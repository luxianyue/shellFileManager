/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lu.shell;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A terminal session, consisting of a VT100 terminal emulator and its
 * input and output streams.
 * <p>
 * You need to supply an {@link InputStream} and {@link OutputStream} to
 * provide input and output to the terminal.  For a locally running
 * program, these would typically point to a tty; for a telnet program
 * they might point to a network socket.  Reader and writer threads will be
 * spawned to do I/O to these streams.  All other operations, including
 * <p>
 * Call {@link #setTermIn} and {@link #setTermOut} to connect the input and
 * output streams to the emulator.  When all of your initialization is
 * complete, your initial screen size is known, and you're ready to
 * start VT100 emulation, call {@link #initialize} or
 * with the number of rows and columns the terminal should
 * initially have.  (If you attach the session to an ,
 * the view will take care of setting the screen size and initializing the
 * emulator for you.)
 * <p>
 * When you're done with the session, you should call {@link #finish} on it.
 * This frees emulator data from memory, stops the reader and writer threads,
 * and closes the attached I/O streams.
 */
public class TermSession {

    private OutputStream mTermOut;
    private InputStream mTermIn;

    private Thread mReaderThread;
    private Thread mWriterThread;
    private ByteQueue mWriteQueue;
    private Handler mWriterHandler;

    private CharsetEncoder mUTF8Encoder;

    private OnContentListener mContentListener;

    private static final int NEW_INPUT = 1;
    private static final int NEW_OUTPUT = 2;
    private static final int NEW_REQUEST_ROOT = 3;
    private static final int FINISH = 4;
    private static final int EOF = 5;

    private boolean mIsReqRoot;
    private boolean mIsRunning = false;
    private Handler mMsgHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (!mIsRunning) {
                return;
            }

            if (msg.what == NEW_INPUT) {
                if (mContentListener != null) {
                    mContentListener.onContent(msg.obj.toString());
                }
            }
            if (msg.what == NEW_REQUEST_ROOT) {
                if (mContentListener != null) {
                    mContentListener.onRequestRoot(msg.obj.toString());
                }
            }
            if (msg.what == EOF) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        onProcessExit();
                    }
                });
            }
        }
    };

    public TermSession() {
        this(false);
    }

    public TermSession(final boolean exitOnEOF) {
        mUTF8Encoder = Charset.forName("UTF-8").newEncoder();
        mUTF8Encoder.onMalformedInput(CodingErrorAction.REPLACE);
        mUTF8Encoder.onUnmappableCharacter(CodingErrorAction.REPLACE);

        //mReceiveBuffer = new byte[4 * 1024];
        //mByteQueue = new ByteQueue(4 * 1024);
        mReaderThread = new Thread() {
            @Override
            public void run() {
                try {
                    int len = 0;
                    byte buf[] = new byte[4096];
                    StringBuffer sb = new StringBuffer();
                    Pattern linePattern = Pattern.compile("[^\n]+[\n]");
                    Matcher lineMatcher = null;
                    String str = null;
                    int start = -1, end = -1;
                    String name = null;
                    boolean isRequestRoot = false;
                    while((len = mTermIn.read(buf)) != -1) {
                        sb.append(new String(buf, 0, len));
                        lineMatcher = linePattern.matcher(sb.toString());
                        if (name == null) {
                            name = sb.toString();
                        }
                        if (isRequestRoot) {
                            if (sb.toString().contains("@")) {
                                isRequestRoot = false;
                                System.out.println("=============>root-------->" + sb.toString());
                                mMsgHandler.obtainMessage(NEW_REQUEST_ROOT, sb.toString()).sendToTarget();
                            }
                        }
                        while (lineMatcher.find()) {
                            if (start == -1) {
                                start = lineMatcher.start();
                            }
                            end = lineMatcher.end();
                            str = lineMatcher.group();
                            mMsgHandler.obtainMessage(NEW_INPUT, str).sendToTarget();
                            if (mIsReqRoot) {
                                mIsReqRoot = false;
                                isRequestRoot = true;
                            }
                            //System.out.println(str);
                        }
                        if (start != -1) {
                            sb.delete(start, end);
                            start = -1;
                        }
                        //System.out.println(sb.toString()+"------------------------------------------>"+ name);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("exception-->"+e.getMessage());
                }

                if (exitOnEOF) mMsgHandler.sendMessage(mMsgHandler.obtainMessage(EOF));
            }
        };
        mReaderThread.setName("TermSession input reader");

        mWriteQueue = new ByteQueue(4096);
        mWriterThread = new Thread() {
            private byte[] mBuffer = new byte[4096];

            @Override
            public void run() {
                Looper.prepare();

                mWriterHandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        if (msg.what == NEW_OUTPUT) {
                            writeToOutput();
                        } else if (msg.what == FINISH) {
                            Looper.myLooper().quit();
                        }
                    }
                };

                // Drain anything in the queue from before we started
                writeToOutput();

                Looper.loop();
            }

            private void writeToOutput() {
                ByteQueue writeQueue = mWriteQueue;
                byte[] buffer = mBuffer;
                OutputStream termOut = mTermOut;

                int bytesAvailable = writeQueue.getBytesAvailable();
                int bytesToWrite = Math.min(bytesAvailable, buffer.length);

                if (bytesToWrite == 0) {
                    return;
                }

                try {
                    writeQueue.read(buffer, 0, bytesToWrite);
                    termOut.write(buffer, 0, bytesToWrite);
                    termOut.flush();
                } catch (IOException e) {
                    // Ignore exception
                    // We don't really care if the receiver isn't listening.
                    // We just make a best effort to answer the query.
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        mWriterThread.setName("TermSession output writer");
    }

    protected void onProcessExit() {
        finish();
    }

    /**
     * Set the terminal emulator's window size and start terminal emulation.
     *
     */
    public void initialize() {
        mIsRunning = true;
        mReaderThread.start();
        mWriterThread.start();
        System.out.println("mReaderThread.start(); mWriterThread.start();");
    }

    /**
     * Write the UTF-8 representation of a String to the terminal output.  The
     * written data will be consumed by the emulation client as input.
     * <p>
     * This implementation encodes the String and then calls
     * therefore usually be unnecessary to override this method; override
     *
     * @param data The String to write to the terminal.
     */
    public void write(String data) {
        try {
            //byte[] bytes = data.getBytes("UTF-8");
            //write(bytes, 0, bytes.length);
            if ("su".equals(data.trim())) {
                mIsReqRoot = true;
            }
            mTermOut.write(data.getBytes("UTF-8"));
            mTermOut.flush();
        } catch (Exception e) {
        }
    }

    /**
     * Get the {@link OutputStream} associated with this session.
     *
     * @return This session's {@link OutputStream}.
     */
    public OutputStream getTermOut() {
        return mTermOut;
    }

    /**
     * Set the {@link OutputStream} associated with this session.
     *
     * @param termOut This session's {@link OutputStream}.
     */
    public void setTermOut(OutputStream termOut) {
        mTermOut = termOut;
    }

    /**
     * Get the {@link InputStream} associated with this session.
     *
     * @return This session's {@link InputStream}.
     */
    public InputStream getTermIn() {
        return mTermIn;
    }

    /**
     * Set the {@link InputStream} associated with this session.
     *
     * @param termIn This session's {@link InputStream}.
     */
    public void setTermIn(InputStream termIn) {
        mTermIn = termIn;
    }

    /**
     * @return Whether the terminal emulation is currently running.
     */
    public boolean isRunning() {
        return mIsRunning;
    }

    /**
     * Finish this terminal session.  Frees resources used by the terminal
     * emulator and closes the attached <code>InputStream</code> and
     * <code>OutputStream</code>.
     */
    public void finish() {
        mIsRunning = false;
        // Stop the reader and writer threads, and close the I/O streams
        if (mWriterHandler != null) {
            mWriterHandler.sendEmptyMessage(FINISH);
        }
        try {
            mTermIn.close();
            mTermOut.close();
        } catch (IOException e) {
            // We don't care if this fails
        } catch (NullPointerException e) {
        }
    }

    public void setContentListener(OnContentListener listener) {
        this.mContentListener = listener;
    }

    public interface OnContentListener {
        void onContent(String content);
        void onRequestRoot(String str);
    }
}
