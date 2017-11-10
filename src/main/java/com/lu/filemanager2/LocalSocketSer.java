package com.lu.filemanager2;

import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.os.Bundle;
import android.util.Log;

import com.lu.activity.BasedActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by bulefin on 2017/11/1.
 */

public class LocalSocketSer extends BasedActivity{
    /* 数据段begin */
    private final String TAG = "server";

    private ServerSocketThread mServerSocketThread;
    /* 数据段end */

    /* 函数段begin */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        mServerSocketThread = new ServerSocketThread();
        mServerSocketThread.start();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        mServerSocketThread.stopRun();
    }
    /* 函数段end */

    /* 内部类begin */
    private class ServerSocketThread extends Thread
    {
        private boolean keepRunning = true;
        private LocalServerSocket mServerSocket;

        private void stopRun() {
            keepRunning = false;
        }

        @Override
        public void run() {
            try {
                mServerSocket = new LocalServerSocket("pym_local_socket");
                while (keepRunning) {
                    LocalSocket clientSocket = mServerSocket.accept();
                    //由于accept()在阻塞时，可能Activity已经finish掉了，所以再次检查keepRunning
                    System.out.println("accept: has client connected");
                    if (keepRunning) {
                        Log.d(TAG, "new client coming !");
                        new ClientSocket(clientSocket).start();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                keepRunning = false;
            }

            if (mServerSocket != null) {
                try {
                    mServerSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class ClientSocket extends Thread {
        private LocalSocket clientSocket;

        public ClientSocket(LocalSocket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            StringBuilder sb = new StringBuilder();
            InputStream inputStream = null;
            try {
                inputStream = clientSocket.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                char[] buf = new char[4096];
                int readBytes = -1;
                while ((readBytes = inputStreamReader.read(buf)) != -1) {
                    String tempStr = new String(buf, 0, readBytes);
                    sb.append(tempStr);
                    System.out.println(sb.toString());
                }
                System.out.println("run method is over");
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "resolve data error !");
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}

