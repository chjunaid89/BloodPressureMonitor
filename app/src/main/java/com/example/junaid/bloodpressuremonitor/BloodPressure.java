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
    TextView txtString, txtStringLength, sensorView0, sensorView1, sensorView2;
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
                Intent deviceIntent = new Intent(BloodPressure.this, DeviceListActivity.class);
                startActivity(deviceIntent);
            }
        });


        startButton = (Button) findViewById(R.id.startbutton);
        stopButton = (Button) findViewById(R.id.stopbutton);
        txtString = (TextView) findViewById(R.id.txtString);
        txtStringLength = (TextView) findViewById(R.id.txtLength);
        sensorView0 = (TextView) findViewById(R.id.map_value);
        sensorView1 = (TextView) findViewById(R.id.sys_value);
        sensorView2 = (TextView) findViewById(R.id.dia_value);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        bluetoothIn = new Handler() {
            public void handleMessage(android.os.Message msg) {
                if (msg.what == handlerState) {										//if message is what we want
                    String readMessage = (String) msg.obj;                                                                // msg.arg1 = bytes from connect thread
                    recDataString.append(readMessage);      								//keep appending to string until ~
                    int endOfLineIndex = recDataString.indexOf("~");                    // determine the end-of-line
                    if (endOfLineIndex > 0) {                                           // make sure there data before ~
                        String dataInPrint = recDataString.substring(0, endOfLineIndex+1);    // extract string
                        txtString.setText("Data Received = " + dataInPrint);
                        int dataLength = dataInPrint.length();							//get length of data received
                        txtStringLength.setText("Data Length = " + String.valueOf(dataLength));

                        if (recDataString.charAt(0) == '#')								//if it starts with # we know it is what we are looking for
                        {
                            String sensor0 = recDataString.substring(1, 3);
                            String sensor1 = recDataString.substring(4, 7);
                            String sensor2 = recDataString.substring(8, 10);

                            sensorView0.setText(sensor0);	//update the textviews with sensor values
                            sensorView1.setText(sensor1);
                            sensorView2.setText(sensor2);
                            int progressValue = Integer.parseInt(sensor1) - 80;
                            progressBar.setProgress(progressValue);
                        }
                        recDataString.delete(0, recDataString.length()); 					//clear all string data
                    }
                }
            }
        };

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        checkBluetoothState();

        stopButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mConnectedThread.write("0");
                Toast.makeText(getBaseContext(), "Reset", Toast.LENGTH_SHORT).show();
                sensorView0.setText("---");
                sensorView1.setText("---");
                sensorView2.setText("---");
                txtString.setText("");
                txtStringLength.setText("");
                progressBar.setProgress(0);
            }
        });

        startButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mConnectedThread.write("1");
                Toast.makeText(getBaseContext(), "Measurement starts! Please stay still", Toast.LENGTH_LONG).show();
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();
        Intent intent = getIntent();
        address = intent.getStringExtra("MAC_ADDRESS");

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice mDevice : pairedDevices) {
                if (mDevice.getAddress().equals(address)) {
                    try {
                        btSocket = mDevice.createRfcommSocketToServiceRecord(BTMODULEUUID);
                    } catch (IOException e) {
                        Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_LONG).show();
                    }
                    try {
                        btSocket.connect();
                        mConnectedThread = new ConnectedThread(btSocket);
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
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
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
                    bytes = mmInStream.read(buffer);
                    String readMessage = new String(buffer, 0, bytes);
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }
        public void write(String input) {
            byte[] msgBuffer = input.getBytes();
            try {
                mmOutStream.write(msgBuffer);
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
            Intent helpIntent = new Intent(BloodPressure.this, HowToMeasureBP.class);
            startActivity(helpIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
