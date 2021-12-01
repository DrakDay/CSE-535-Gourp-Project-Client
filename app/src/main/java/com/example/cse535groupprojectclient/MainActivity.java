package com.example.cse535groupprojectclient;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity {

    //setting up client properties
    public static final int SERVER_PORT = 8888;
    public static final String SERVER_IP = "192.168.1.6";
    private ClientThread clientThread;
    private Thread thread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //starting new client thread
        clientThread = new ClientThread();
        thread = new Thread(clientThread);
        thread.start();

        //testing sending msg to server
        //test();
    }

    class ClientThread implements Runnable {

        private Socket socket;
        private BufferedReader input;

        @Override
        public void run() {
            try {

                socket = new Socket(SERVER_IP, SERVER_PORT);
                Log.i("TAG", "connect to server :");
                while (!Thread.currentThread().isInterrupted()) {

                    this.input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String message = input.readLine();
                    if (null == message || "Disconnect".contentEquals(message)) {
                        Thread.interrupted();
                        message = "Server Disconnected.";
                        Log.i("TAG", "Client : (most time is server Disconnected )" + message);
                        break;
                    }
                    Log.i("TAG", "Server :" + message);
                }

            } catch (IOException e1) {
                e1.printStackTrace();
                Log.i("TAG", "Starting client failed :");
            }

        }

        void sendMessage(final String message) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (null != socket) {
                            PrintWriter out = new PrintWriter(new BufferedWriter(
                                    new OutputStreamWriter(socket.getOutputStream())),
                                    true);
                            out.println(message);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }

    }

    public void test(){
        int i = 0;
        while(i<200){
            clientThread.sendMessage(Integer.toString(i));
            Log.i("TAG", "send :" + Integer.toString(i));
            i++;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != clientThread) {
            clientThread.sendMessage("Disconnect");
            clientThread = null;
        }
    }
}