package com.example.junaid.bloodpressuremonitor;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class BloodPressure extends AppCompatActivity {

    Button startButton, stopButton;
    TextView txtString, txtStringLength, map, sys, dia;
    ProgressBar progressBar;
    String address;
    Handler bluetoothIn;

    final int handlerState = 0;
    private BluetoothAdapter bluetoothAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder recDataString = new StringBuilder();

    private ConnectedThread mConnectedThread;

    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blood_pressure);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_new));
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent deviceIntent = new Intent(BloodPressure.this, DeviceListActivity.class); // start new activity to connect the paired bluetooth device
                startActivity(deviceIntent);
            }
        });


        startButton = (Button) findViewById(R.id.startbutton);
        stopButton = (Button) findViewById(R.id.stopbutton);
        txtString = (TextView) findViewById(R.id.txtString);
        txtStringLength = (TextView) findViewById(R.id.txtLength);
        map = (TextView) findViewById(R.id.map_value);
        sys = (TextView) findViewById(R.id.sys_value);
        dia = (TextView) findViewById(R.id.dia_value);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        bluetoothIn = new Handler() {
            public void handleMessage(android.os.Message msg) {
                if (msg.what == handlerState) {										            // if msg is what we expect
                    String readMessage = (String) msg.obj;
                    recDataString.append(readMessage);      								    // keep appending to msg until ~
                    int endOfLineIndex = recDataString.indexOf("~");                            // end of the msg
                    if (endOfLineIndex > 0) {                                                   // make sure there is a msg before ~
                        String dataInPrint = recDataString.substring(0, endOfLineIndex+1);      // extract msg
                        txtString.setText("Data Received = " + dataInPrint);
                        int dataLength = dataInPrint.length();							        // length of msg received
                        txtStringLength.setText("Data Length = " + String.valueOf(dataLength));

                        if (recDataString.charAt(0) == '#')								        // if msg starts with # , guarantees what we are expecting
                        {
                            String map_val = recDataString.substring(1, 3);
                            String sys_val = recDataString.substring(4, 7);
                            String dia_val = recDataString.substring(8, 10);

                            map.setText(map_val);	                                            // update the textviews with the blood pressure values
                            sys.setText(sys_val);
                            dia.setText(dia_val);
                            int progressValue = Integer.parseInt(sys_val) - 80;
                            progressBar.setProgress(progressValue);
                        }
                        recDataString.delete(0, recDataString.length()); 					    // clear all string data
                    }
                }
            }
        };

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        checkBluetoothState();                                                                  // method to check the bluetooth status

        stopButton.setOnClickListener(new View.OnClickListener() {                              // reset the main activity and update the textviews
            public void onClick(View v) {
                mConnectedThread.write("0");
                Toast.makeText(getBaseContext(), "Reset", Toast.LENGTH_SHORT).show();
                map.setText("---");
                sys.setText("---");
                dia.setText("---");
                txtString.setText("");
                txtStringLength.setText("");
                progressBar.setProgress(0);
            }
        });

        startButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {                                                 // start the blood pressure measurement
                mConnectedThread.write("1");
                Toast.makeText(getBaseContext(), "Measurement starts! Please stay still", Toast.LENGTH_LONG).show();
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();
        Intent intent = getIntent();
        address = intent.getStringExtra("MAC_ADDRESS");                                         // get the MAC address of the bluetooth device

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice mDevice : pairedDevices) {
                if (mDevice.getAddress().equals(address)) {
                    try {
                        btSocket = mDevice.createRfcommSocketToServiceRecord(BTMODULEUUID);     // create RFCOMM bluetooth socket connection with the device
                    } catch (IOException e) {
                        Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_LONG).show();
                    }
                    try {
                        btSocket.connect();
                        mConnectedThread = new ConnectedThread(btSocket);                       // thread to communicate with the bluetooh device
                        mConnectedThread.start();
                    } catch (IOException e) {
                        try {
                            btSocket.close();
                        } catch (IOException e2) {

                        }
                    }

                }

            }
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();
        try
        {
            if(btSocket != null) {
                btSocket.close();
            }
        } catch (IOException e2) {

        }
    }


    private void checkBluetoothState(){

        if(!bluetoothAdapter.isEnabled())
        {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);                 // ask permission to turn-ON the bluetooth if it is turned-OFF
            startActivity(intent);

        }
    }

    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }


        public void run() {
            byte[] buffer = new byte[256];
            int bytes;

            while (true) {
                try {
                    bytes = mmInStream.read(buffer);                                                    // read msg from the bluetooth device
                    String readMessage = new String(buffer, 0, bytes);
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();     // update the main thread with the msg received
                } catch (IOException e) {
                    break;
                }
            }
        }
        public void write(String input) {
            byte[] msgBuffer = input.getBytes();
            try {
                mmOutStream.write(msgBuffer);                                                           // write msg to the blutooth device
            } catch (IOException e) {
                Toast.makeText(getBaseContext(), "Connection Failure", Toast.LENGTH_LONG).show();
                finish();

            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_blood_pressure, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        if (id == R.id.help) {
            Intent helpIntent = new Intent(BloodPressure.this, HowToMeasureBP.class);                   // new activity for the help on how to measure the blood pressure
            startActivity(helpIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
