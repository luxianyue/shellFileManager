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
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.File;
import java.io.IOException;

/**
 * A terminal session, controlling the process attached to the session (usually
 * a shell). It keeps track of process PID and destroys it's process group
 * upon stopping.
 */
public class ShellTermSession extends GenericTermSession {
    private int mProcId;
    private Thread mWatcherThread;

    private static final int PROCESS_EXITED = 1;
    private Handler mMsgHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (!isRunning()) {
                return;
            }
            if (msg.what == PROCESS_EXITED) {
                onProcessExit((Integer) msg.obj);
            }
        }
    };

    public ShellTermSession() throws IOException {
        super(ParcelFileDescriptor.open(new File("/dev/ptmx"), ParcelFileDescriptor.MODE_READ_WRITE), false);
        //super(ParcelFileDescriptor.open(new File("/dev/tty"), ParcelFileDescriptor.MODE_READ_WRITE), false);
        initializeSession();

        setTermOut(new ParcelFileDescriptor.AutoCloseOutputStream(mTermFd));
        setTermIn(new ParcelFileDescriptor.AutoCloseInputStream(mTermFd));

        mWatcherThread = new Thread() {
            @Override
            public void run() {
                Log.i("shellTermsession", "waiting for: " + mProcId);
                int result = TermExec.waitFor(mProcId);
                Log.i("shellTermSession", "Subprocess exited: " + result);

                mMsgHandler.sendMessage(mMsgHandler.obtainMessage(PROCESS_EXITED, result));
            }
        };
        mWatcherThread.setName("Process watcher");
    }

    private void initializeSession() throws IOException {
        String path = System.getenv("PATH");
        String[] env = new String[3];
        env[0] = "TERM=linux";
        env[1] = "PATH=" + path;
        env[2] = "HOME=/data/data/com.lu.filemanager2";

        mProcId = createSubprocess("/system/bin/sh", env);
        System.out.println("mprocid-->" + mProcId + "  env0--" + env[0] + "  env1--" + env[1] + " env2==" + env[2]);
    }

    @Override
    public void initialize() {
        super.initialize();
        mWatcherThread.start();
        sendInitialCommand("");
    }

    private void sendInitialCommand(String initialCommand) {
        System.out.println("shellTermSession sendInitialCommand--->" + initialCommand);
        //write(initialCommand + '\r');
    }

    private int createSubprocess(String shell, String[] env) throws IOException {
        //System.out.println("-------cmd-->" + cmd + "\nargs--->" + Arrays.toString(args));
        //return TermExec.createSubprocess(mTermFd, cmd, args, env);
        String args[] = {"/system/bin/sh","-"};
        return TermExec.createSubprocess(mTermFd, shell, args, env);
    }

    private void onProcessExit(int result) {
        onProcessExit();
    }

    @Override
    public void finish() {
        hangupProcessGroup();
        super.finish();
    }

    /**
     * Send SIGHUP to a process group, SIGHUP notifies a terminal client, that the terminal have been disconnected,
     * and usually results in client's death, unless it's process is a daemon or have been somehow else detached
     * from the terminal (for example, by the "nohup" utility).
     */
    void hangupProcessGroup() {
        TermExec.sendSignal(-mProcId, 1);
    }
}
