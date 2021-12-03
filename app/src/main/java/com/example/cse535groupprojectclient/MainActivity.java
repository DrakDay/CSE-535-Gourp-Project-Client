package com.example.cse535groupprojectclient;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;

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
    location_finder loc_finder;
    //setting up client properties
    public static final int SERVER_PORT = 8888;
    public static final String SERVER_IP = "192.168.1.6";
    private ClientThread clientThread;
    private Thread thread;
    private Battery_thread battery_thread;
    private Thread bthread;

    //client ip
    public static String CLIENT_IP = "";

    //ui element
    Button connect_to_server;
    Button disconnect_to_server;

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

        connect_to_server = findViewById(R.id.connect);
        disconnect_to_server = findViewById(R.id.disconnect);

        //get ip
        try{
            CLIENT_IP = getLocalIPaddress();
        } catch (UnknownHostException e){
            e.printStackTrace();
        }


        //listening batter level check
        this.registerReceiver(this.batterylevelReciver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        //get current location
        loc_finder = new location_finder(this);
        Log.i("TAG", "longitude: " + loc_finder.get_longitude());
        Log.i("TAG", "latitude: " + loc_finder.get_latitude());
        Log.i("TAG", "country: " + loc_finder.get_country());
        //Log.i("TAG", "address: " + loc_finder.get_address());

        connect_to_server.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clientThread = new ClientThread();
                thread = new Thread(clientThread);
                thread.start();
                SystemClock.sleep(500);
                clientThread.sendMessage("IP:"+CLIENT_IP);
                SystemClock.sleep(500);
                clientThread.sendMessage("Lat:" + loc_finder.get_latitude() + ",Lon:"+loc_finder.get_longitude()+",IP:"+CLIENT_IP);
            }
        });

        disconnect_to_server.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clientThread.sendMessage("Disconnect:"+CLIENT_IP);
                thread.interrupt();
                battery_thread.stop();
            }
        });

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
                        Log.i("TAG", "Client : (most time is server Disconnected )" + message);
                        break;
                    }
                    Log.i("TAG", "Server :" + message);
                    request_handler(message);
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

    //handling msg from server
    public void request_handler(String msg){
        if(msg.equals("participate")){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    confirm();
                }
            });

        }
    }

    //ask user want participate using alter dialog
    public void confirm (){
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        builder.setTitle("Enter Network");
        builder.setMessage("do you want participate");

        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //send yes msg to server
                clientThread.sendMessage("YES:"+CLIENT_IP+",Battery:"+battery_level);

                //start monitoring client battery level
                battery_thread = new Battery_thread();
                bthread = new Thread(battery_thread);
                bthread.start();
                dialog.dismiss();
            }
        });

        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //send no msg to server
                clientThread.sendMessage("NO:"+CLIENT_IP);
                dialog.dismiss();
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }


    //get current battery level
    public void monitoring(){

        this.unregisterReceiver(this.batterylevelReciver);
        this.registerReceiver(this.batterylevelReciver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        loc_finder.update_locations();
        clientThread.sendMessage("Battery:"+battery_level+ ",Lat:"+loc_finder.get_latitude()+",Lon:"+loc_finder.get_longitude()+",IP:"+CLIENT_IP);
        Log.i("TAG","Battery:"+battery_level+ ",Lat:"+loc_finder.get_latitude()+",Lon:"+loc_finder.get_longitude()+",IP:"+CLIENT_IP);
    }

    //battery thread class
    class Battery_thread implements Runnable{
        boolean run = true;
        @Override
        public void run(){
            while(run) {
                monitoring();
                SystemClock.sleep(5000);
            }
        }
        public void stop(){
            run = false;
        }
    }
}


