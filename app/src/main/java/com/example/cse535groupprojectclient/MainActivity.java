package com.example.cse535groupprojectclient;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MainActivity extends AppCompatActivity {

    private int battery_level;

    //setting up client properties
    public static final int SERVER_PORT = 8888;
    public static final String SERVER_IP = "192.168.1.6";
    private ClientThread clientThread;
    private Thread thread;

    //client up
    public static String CLIENT_IP = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //get permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_NETWORK_STATE}, 1);
        }

        try{
            CLIENT_IP = getLocalIPaddress();
        } catch (UnknownHostException e){
            e.printStackTrace();
        }

        //starting new client thread
        clientThread = new ClientThread();
        thread = new Thread(clientThread);
        thread.start();

        //testing sending msg to server
        //test();

        //listening batter level check
        this.registerReceiver(this.batterylevelReciver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        //get current location
        location_finder loc_finder= new location_finder(this);
        Log.i("TAG", "longitude: " + loc_finder.get_longitude());
        Log.i("TAG", "latitude: " + loc_finder.get_latitude());
        Log.i("TAG", "country: " + loc_finder.get_country());
        Log.i("TAG", "address: " + loc_finder.get_address());
    }

    // battery level check
    private BroadcastReceiver batterylevelReciver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            battery_level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            Log.i("TAG", "battery level: " + Integer.toString(battery_level));
        }
    };

    //get current location

    private String getLocalIPaddress() throws UnknownHostException{
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        assert wifiManager != null;
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipInt = wifiInfo.getIpAddress();
        return InetAddress.getByAddress(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ipInt).array()).getHostAddress();
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
        while(i<100){
            clientThread.sendMessage(CLIENT_IP + ": " + Integer.toString(i));
            Log.i("TAG", "send :" + CLIENT_IP + ": " + Integer.toString(i));
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