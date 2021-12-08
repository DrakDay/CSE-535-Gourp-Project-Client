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
import android.os.Handler;
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
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private int battery_level;
    location_finder loc_finder;
    //setting up client properties
    public static final int SERVER_PORT = 8888;
    public static final String SERVER_IP = "192.168.0.47";
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
                if (battery_thread != null){
                    battery_thread.stop();
                }

            }
        });

    }

    // battery level check
    private BroadcastReceiver batterylevelReciver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            battery_level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            //Log.i("TAG", "battery level: " + Integer.toString(battery_level));
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
        else if (msg.startsWith("IP")){
            String[] splitMessages = msg.split(",");
            if (splitMessages[0].split(":")[1].equals(CLIENT_IP)) {
                Log.i("SPLIT", Arrays.toString(splitMessages));
                String splitIndex = splitMessages[3];

                // Code for failure, set to true for failure on second client
                boolean fail = false;
                if (fail == true) {

                    if(splitIndex.equals("1")) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                SystemClock.sleep(5000);
                                String message = "FAILURE";
                                Log.i("TEST", message);
                                clientThread.sendMessage(message);
                            }
                        }).run();
                    }
                }
                else {
                    if(splitIndex.equals("2"))
                        splitIndex = "1";

                    int m1RowSize = 0;
                    int m1ColumnSize = 0;
                    int m2RowSize = 0;
                    int m2ColumnSize = 0;
                    String[] m1String;
                    String[] m2String;

                    if(splitMessages[1].contains(".")) {
                        m1String = splitMessages[1].split("\\.");
                    }
                    else {
                        m1String = new String[1];
                        m1String[0] = splitMessages[1];
                    }
                    Log.i("SPLIT", Arrays.toString(m1String));

                    if(splitMessages[2].contains(".")) {
                        m2String = splitMessages[2].split("\\.");
                    }
                    else {
                        m2String = new String[1];
                        m2String[0] = splitMessages[2];
                    }
                    Log.i("SPLIT", Arrays.toString(m2String));
                    m1RowSize = m1String.length;
                    m1ColumnSize = m1String[0].split("@").length;

                    m2RowSize = m2String.length;
                    m2ColumnSize = m2String[0].split("@").length;

                    int[][] matrix1 = new int[m1RowSize][m1ColumnSize];
                    int[][] matrix2 = new int[m2RowSize][m2ColumnSize];

                    for (int i = 0; i < m1RowSize; i++) {
                        for (int j = 0; j < m1ColumnSize; j++) {
                            matrix1[i][j] = Integer.valueOf(m1String[i].split("@")[j]);
                        }
                    }

                    for (int i = 0; i < m2RowSize; i++) {
                        for (int j = 0; j < m2ColumnSize; j++) {
                            matrix2[i][j] = Integer.valueOf(m2String[i].split("@")[j]);
                        }
                    }

                    int[][] multResult = multiplyMatrices(matrix1, matrix2);

                    String message = "RESULT, IP:" + CLIENT_IP + ",";

                    // Copy first matrix
                    for(int rowIndex = 0; rowIndex < multResult.length; rowIndex++) {
                        for(int columnIndex = 0; columnIndex < multResult[0].length; columnIndex++) {
                            message += Integer.toString(multResult[rowIndex][columnIndex]);
                            if (columnIndex != multResult[0].length-1)
                                message += "@";
                        }
                        if (rowIndex != multResult.length-1)
                            message += ".";
                    }
                    message += "," + splitIndex;

                    clientThread.sendMessage(message);
                    Log.i("TEST", message);
                }
            }
        }
    }

    //ask user want participate using alter dialog
    public void confirm (){
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        builder.setTitle("Enter Network");
        builder.setMessage("Do you want participate?");

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

    // Method for multiplying two matrices together
    public static int[][] multiplyMatrices(int[][] matrix1, int[][] matrix2) {
        int[][] result = new int[matrix1.length][matrix2[0].length];
        if (matrix1[0].length != matrix2.length)
            System.out.println("ERROR: Dimensions of matrices do not match!");
        else {
            for (int i = 0; i < matrix1.length; i++) {
                for (int j = 0; j < matrix2[0].length; j++) {
                    result[i][j] = 0;
                    for (int k = 0; k < matrix1[0].length; k++) {
                        result[i][j] += matrix1[i][k] * matrix2[k][j];
                    }
                }
            }
        }
        return result;
    }
}


