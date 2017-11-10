package com.lu.filemanager2;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

/**
 * Created by bulefin on 2017/11/1.
 */

public class LocalSocketDemo {
    private final String SOCKET_NAME = "socket_test0";
    private LocalSocket mClient;
    private LocalSocketAddress mAddress;
    private boolean isConnected = false;

    public LocalSocketDemo() {
        mClient = new LocalSocket();
        mAddress = new LocalSocketAddress(SOCKET_NAME, LocalSocketAddress.Namespace.RESERVED);
        new ConnectSocketThread().start();
    }

    public String sendMsg(String msg) {
        if (!isConnected) {
            return "Connect fail";
        }
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(mClient.getInputStream()));
            PrintWriter out = new PrintWriter(mClient.getOutputStream());
            out.println(msg);
            out.flush();
            return in.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Nothing return";
    }


    private class ConnectSocketThread extends Thread {
        @Override
        public void run() {
            try {
                mClient.connect(mAddress);
            } catch (Exception e) {
                Log.i("SocketClient","Connect fai:" +  e.toString());
            }
        }
    }

    public void closeSocket() {
        try {
            mClient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
