package com.example.hearingtest.activities;


import androidx.annotation.NonNull;
import androidx.annotation.RawRes;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.util.Xml;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.example.hearingtest.R;
import com.example.hearingtest.adapters.WaveHeader;
import com.example.hearingtest.databinding.ActivityMainBinding;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends AppCompatActivity implements View.OnTouchListener {

    private static final String Tag = "DEBUG_BT";

    private ActivityMainBinding binding;
    private MediaPlayer mediaPlayer;
    private ExecutorService executor;
    private String pathWav;
    private WaveHeader waveHeader;
    private AudioManager audioManager;
    private int decibelLevel;
    private boolean pressed = false;
    private int clicked = 0;
    private int[] values;
    private int[] noiseSounds;
    private int[] ftmTones;
    private int[] tones;
    private byte[][] gaps;
    private int[] gapTimes;
    private byte[] gap1s;
    private static int[][] thresholdTemporalValuesRightEar;
    private static int[][] thresholdTemporalValuesLeftEar;
    private static int[] thresholdAudiometryRightEar;
    private static int[] thresholdAudiometryLeftEar;

    static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    BluetoothSocket BTSocket = null;
    BluetoothAdapter BTAdapter = null;
    Set<BluetoothDevice> BTPairedDevices = null;
    boolean bBTConnected = false;
    BluetoothDevice BTDevice = null;
    classBTInitDataCommunication cBTInitSendReceive = null;

    static public final int BT_CON_STATUS_NOT_CONNECTED = 0;
    static public final int BT_CON_STATUS_CONNECTING = 1;
    static public final int BT_CON_STATUS_CONNECTED = 2;
    static public final int BT_CON_STATUS_FAILED = 3;
    static public final int BT_CON_STATUS_CONNECTION_LOST = 4;
    static public int iBTConnectionStatus = BT_CON_STATUS_NOT_CONNECTED;

    static final int BT_STATE_LISTENING = 1;
    static final int BT_STATE_CONNECTING = 2;
    static final int BT_STATE_CONNECTED = 3;
    static final int BT_STATE_CONNECTION_FAILED = 4;
    static final int BT_STATE_MESSAGE_RECEIVED = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        init();
        setListeners();

        getBTPairedDevices();
        populateSpinnerWithBTPairedDevices();
    }


    /**
     * Initialize listeners.
     */
    private void setListeners() {
        binding.pressButton.setOnTouchListener(this);
        binding.StartButton.setOnClickListener(v -> MainActivity.this.runOnUiThread(() -> {
            binding.StartButton.setVisibility(View.INVISIBLE);
            binding.pressButton.setVisibility(View.VISIBLE);
            temporalMaskingRightEar();
        }));
        binding.StartAudioButton.setOnClickListener(v -> MainActivity.this.runOnUiThread(() -> {
            binding.StartAudioButton.setVisibility(View.GONE);
            binding.pressButton.setVisibility(View.VISIBLE);
            audiometryRightEar();
        }));
        binding.StartTemporalButton.setOnClickListener(v -> MainActivity.this.runOnUiThread(() -> {
            binding.StartTemporalButton.setVisibility(View.GONE);
            binding.pressButton.setVisibility(View.VISIBLE);
            temporalMaskingLeftEar();
        }));
        binding.StartLeftAudioButton.setOnClickListener(v -> MainActivity.this.runOnUiThread(() -> {
            binding.StartLeftAudioButton.setVisibility(View.GONE);
            binding.pressButton.setVisibility(View.VISIBLE);
            audiometryLeftEar();
        }));
        binding.idButtonConnect.setOnClickListener(V -> {
            Log.d(Tag, "Button Clicked buttonConnect");
            if (bBTConnected == false) {
                if (binding.idSpinnerBTPairedDevices.getSelectedItemPosition() == 0) {
                    Log.d(Tag, "Select a device");
                    showToast("Select a device");
                    return;
                }

                String selectedDevice = binding.idSpinnerBTPairedDevices.getSelectedItem().toString();
                Log.d(Tag, "Selected device = " + selectedDevice);

                for (BluetoothDevice BTDev : BTPairedDevices) {
                    if (selectedDevice.equals(BTDev.getName())) {
                        BTDevice = BTDev;
                        Log.d(Tag, "Selected device UUID = " + BTDevice.getAddress());

                        cBluetoothConnect cBTConnect = new cBluetoothConnect(BTDevice);
                        cBTConnect.start();
                    }
                }
            } else {
                Log.d(Tag, "Disconnecting BTConnection");
                try {
                    BTSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(Tag, e.getMessage());
                }
                binding.idButtonConnect.setText("Connect");
                bBTConnected = false;
                binding.SendData.setVisibility(View.GONE);
            }
        });
        binding.SendData.setOnClickListener(v -> {

            try {
                sendMessage();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Will check if the phone has Bluetooth and if it is enabled.
     * Then it will get all the paired devices.
     */
    private void getBTPairedDevices() {
        Log.d(Tag, "getBTPairedDevices - Start");

        // Get default adapter.
        BTAdapter = BluetoothAdapter.getDefaultAdapter();

        // Control if the device have Bluetooth or is enable.
        if (BTAdapter == null) {
            Log.e(Tag, "getBTPairedDevices - BTAdapter null");
            showToast("No bluetooth on this device");
            return;
        } else if (!BTAdapter.isEnabled()) {
            Log.e(Tag, "getBTPairedDevices - BT not enable");
            showToast("Turn on Bluetooth");
            return;
        }

        // Get the paired devices.
        BTPairedDevices = BTAdapter.getBondedDevices();
        Log.d(Tag, "getBTPairedDevices - Paired devices count " + BTPairedDevices.size());

        for (BluetoothDevice BTDevice : BTPairedDevices) {
            Log.d(Tag, BTDevice.getName() + ", " + BTDevice.getAddress());
        }
    }

    /**
     * The function will add the paired devices to the dropdown list.
     */
    private void populateSpinnerWithBTPairedDevices() {
        ArrayList<String> alPairedDevices = new ArrayList<>();
        alPairedDevices.add("Select");

        // Add all the paired devices on the phone.
        for (BluetoothDevice BTDev : BTPairedDevices) {
            alPairedDevices.add(BTDev.getName());
        }

        // Add the devices to the dropdown list.
        final ArrayAdapter<String> aaPairedDevices = new ArrayAdapter<String>(this,
                androidx.appcompat.R.layout.support_simple_spinner_dropdown_item, alPairedDevices);
        aaPairedDevices.setDropDownViewResource(androidx.appcompat.R.layout.support_simple_spinner_dropdown_item);
        binding.idSpinnerBTPairedDevices.setAdapter(aaPairedDevices);
    }

    /**
     * The class take care of the connection to the device.
     */
    public class cBluetoothConnect extends Thread {
        private BluetoothDevice device;

        /**
         * Constructor
         * @param BTDevice the device that Bluetooth going to connect to.
         */
        public cBluetoothConnect(BluetoothDevice BTDevice) {
            Log.i(Tag, "classBTConnect - start");
            device = BTDevice;

            try {
                BTSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(Tag, "classBTConnect - e = " + e.getMessage());
            }
            Log.i(Tag, "classBTConnect - got socket");
        }

        /**
         * Main program for Bluetooth connect thread.
         * The program will connect to the Bluetooth socket.
         */
        public void run() {
            Log.i(Tag, "classBTConnect - run");
            try {
                BTSocket.connect();
                Message message = Message.obtain();
                message.what = BT_STATE_CONNECTED;
                handler.sendMessage(message);
                Log.i(Tag, "classBTConnect - run : " + message.what);
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(Tag, "classBTConnect - run, e = " + e.getMessage());
                Message message = Message.obtain();
                message.what = BT_STATE_CONNECTION_FAILED;
                handler.sendMessage(message);
            }
        }
    }

    /**
     * The class will take care of receiving and sending data over Bluetooth.
     */
    public class classBTInitDataCommunication extends Thread {
        private final BluetoothSocket bluetoothSocket;
        private InputStream inputStream = null;
        private OutputStream outputStream = null;

        /**
         * Constructor
         * @param socket the socket for the Bluetooth device.
         */
        public classBTInitDataCommunication(BluetoothSocket socket) {
            Log.i(Tag, "classBTInitDataCommunication - start");

            bluetoothSocket = socket;

            try {
                inputStream = bluetoothSocket.getInputStream();
                outputStream = bluetoothSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(Tag, "classBTInitDataCommunication - start, e = " + e.getMessage());
            }
        }

        /**
         * Main program for data communication thread.
         * This thread will listening for data over Bluetooth.
         */
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            // Receiving data from Bluetooth.
            while (BTSocket.isConnected()) {
                try {
                    bytes = inputStream.read(buffer);
                    handler.obtainMessage(BT_STATE_MESSAGE_RECEIVED, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(Tag, "BT disconnect from device end, e = " + e.getMessage());
                    iBTConnectionStatus = BT_CON_STATUS_CONNECTION_LOST;
                    try {
                        Log.d(Tag, "Disconnecting BTConnection");
                        if (BTSocket != null && BTSocket.isConnected()) {
                            BTSocket.close();
                        }
                        binding.idButtonConnect.setText("Connect");
                        bBTConnected = false;
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                        Log.e(Tag, "classBTInitDataCommunication - run, e = " + e.getMessage());
                    }
                }
            }
        }

        /**
         * Sending the bytes over Bluetooth.
         * @param bytes the data that is going to be sent.
         */
        public void write(byte[] bytes) {
            try {
                outputStream.write(bytes);
                Log.d(Tag, "Sending message");
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(Tag, "sending failed = " + e.getMessage());
            }
        }
    }

    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case BT_STATE_LISTENING:
                    Log.d(Tag, "BT_STATE_ LISTENING");
                    break;
                case BT_STATE_CONNECTING:
                    iBTConnectionStatus = BT_CON_STATUS_CONNECTING;
                    binding.idButtonConnect.setText("Connecting..");
                    Log.d(Tag, "BT_STATE_CONNECTING");
                    break;
                case BT_STATE_CONNECTED:
                    iBTConnectionStatus = BT_CON_STATUS_CONNECTED;
                    binding.idButtonConnect.setText("Disconnect");

                    cBTInitSendReceive = new classBTInitDataCommunication(BTSocket);
                    cBTInitSendReceive.start();

                    bBTConnected = true;
                    binding.SendData.setVisibility(View.VISIBLE);
                    break;
                case BT_STATE_CONNECTION_FAILED:
                    iBTConnectionStatus = BT_CON_STATUS_FAILED;
                    Log.d(Tag, "BT_STATE_CONNECTION_FAILED");
                    bBTConnected = false;
                    break;
                case BT_STATE_MESSAGE_RECEIVED:
                    byte[] readBuffer = (byte[])msg.obj;
                    String tempMsg = new String(readBuffer, 0, msg.arg1);
                    Log.d(Tag, "Message received ( " + tempMsg.length() + " ) Data : " + tempMsg);
                    break;
            }

            return true;
        }
    });

    /**
     * The function will send all of the thresholds from the tests.
     * @throws IOException
     */
    public void sendMessage() throws IOException {
        Log.i(Tag, "Sending message");
        // Control so the application is connected.
        if (BTSocket != null && iBTConnectionStatus == BT_CON_STATUS_CONNECTED) {
            if (BTSocket.isConnected()) {

                // Convert temporal masking test thresholds to byte array.
                byte[] Data = temporalDataToBytes(thresholdTemporalValuesRightEar);

                // Send the data.
                cBTInitSendReceive.write(Data);

                // Convert temporal masking test thresholds to byte array.
                Data = temporalDataToBytes(thresholdTemporalValuesLeftEar);

                // Send the data.
                cBTInitSendReceive.write(Data);

                // Convert audiometry test thresholds to byte array.
                Data = audiometryDataToBytes(thresholdAudiometryRightEar);

                // Send the data
                cBTInitSendReceive.write(Data);

                // Convert audiometry test thresholds to byte array.
                Data = audiometryDataToBytes(thresholdAudiometryLeftEar);

                // Send the data
                cBTInitSendReceive.write(Data);

            } else {
                showToast("Connect to device");
            }
        }
    }

    /**
     * The function will convert the integer array to an byte array loaded with frame work data.
     * @param Data The array of data from audiometry test.
     * @return A byte array with the frame work.
     * @throws IOException
     */
    private byte[] audiometryDataToBytes(int[] Data) throws IOException {
        // Data that is being stored in the byte array
        byte[] start = "AA".getBytes();
        // Tell what type it is.
        byte[] data = "DA".getBytes();
        // Length of the payload.
        byte[] length = "9".getBytes();
        // Tell the end of frame work.
        byte[] stop = "EE".getBytes();

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        // Add to outputStream.
        output.write(start);
        output.write(data);
        output.write(length);

        // Calculate the sum of the values and add the byte to outputStream.
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            sum += Data[i];
            output.write((byte)Data[i]);
        }

        // Get the high bytes from the sum and add it to outputStream.
        int HI = ((sum & 0xff) >> 8);
        String sHI = Integer.toString(HI);
        byte[] CSR_HI = sHI.getBytes();
        output.write(CSR_HI);

        // Get the low bytes from the sum and add it to outputStream.
        int LOW = (sum & 0xff);
        String sLOW = Integer.toString(LOW);
        byte[] CSR_LOW = sLOW.getBytes();
        output.write(CSR_LOW);

        // Add the stop bytes.
        output.write(stop);

        // Convert outputStream to byteArray.
        byte[] out = output.toByteArray();

        return out;
    }

    /**
     * The function will convert the integer array to an byte array loaded with frame work data.
     * @param Data The array of data from temporal masking test.
     * @return A byte array with the frame work.
     * @throws IOException
     */
    private byte[] temporalDataToBytes(int[][] Data) throws IOException {
        // Data that is being stored in the byte array
        byte[] start = "AA".getBytes();
        // Tell what type it is.
        byte[] data = "DA".getBytes();
        // Length of the payload.
        byte[] length = "15".getBytes();
        // Tell the end of frame work.
        byte[] stop = "EE".getBytes();

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        // Add to outputStream.
        output.write(start);
        output.write(data);
        output.write(length);

        // Calculate the sum of the values and add the byte to outputStream.
        int sum = 0;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 5; j++) {
                sum += Data[i][j];
                output.write((byte)Data[i][j]);
            }
        }

        // Get the high bytes from the sum and add it to outputStream.
        int HI = ((sum & 0xff) >> 8);
        String sHI = Integer.toString(HI);
        byte[] CSR_HI = sHI.getBytes();
        output.write(CSR_HI);

        // Get the low bytes from the sum and add it to outputStream.
        int LOW = (sum & 0xff);
        String sLOW = Integer.toString(LOW);
        byte[] CSR_LOW = sLOW.getBytes();
        output.write(CSR_LOW);

        // Add the stop bytes.
        output.write(stop);

        // Convert outputStream to byteArray.
        byte[] out = output.toByteArray();

        return out;
    }

    /**
     * Initialize all the variables.
     */
    private void init() {
        mediaPlayer = new MediaPlayer();
        executor = Executors.newSingleThreadExecutor();
        pathWav = "/storage/emulated/0/Download/HearingTestSound.wav";
        waveHeader = new WaveHeader();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        setMaxVolume();
        decibelLevel = 50;
        values = new int[5];
        noiseSounds = new int[]{R.raw.noise_500hz, R.raw.noise_2000hz, R.raw.noise_4000hz};
        ftmTones = new int[]{R.raw.ftm_500hz, R.raw.ftm_2000hz, R.raw.ftm_4000hz};
        tones = new int[]{R.raw.tone_1000hz, R.raw.tone_2000hz, R.raw.tone_3000hz,
                R.raw.tone_4000hz, R.raw.tone_6000hz, R.raw.tone_8000hz, R.raw.tone_1000hz,
                R.raw.tone_500hz, R.raw.tone_250hz
        };
        gaps = new byte[5][];
        gaps[0] = new byte[7680];
        gaps[1] = new byte[3840];
        gaps[2] = new byte[1920];
        gaps[3] = new byte[960];
        gaps[4] = new byte[480];
        gapTimes = new int[]{80, 40, 20, 10, 5};
        gap1s = new byte[96000];
        thresholdTemporalValuesRightEar = new int[3][5];
        thresholdTemporalValuesLeftEar = new int[3][5];
        thresholdAudiometryRightEar = new int[9];
        thresholdAudiometryLeftEar = new int[9];


    }

    /**
     * Set max volume on music on device
     */
    private void setMaxVolume() {
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
    }

    /**
     * Will release the last mediaPlayer and create a new one and play the sound.
     * Will play the sound on the right ear.
     * @param path take the path of the sound file
     */
    private void playSoundRightEar(String path) {
        mediaPlayer.release();
        mediaPlayer = MediaPlayer.create(this, Uri.parse(path));
        mediaPlayer.setVolume(0,1);
        mediaPlayer.start();
    }

    /**
     * Will release the last mediaPlayer and create a new one and play the sound.
     * Will play the sound on the left ear.
     * @param path take the path of the sound file
     */
    private void playSoundLeftEar(String path) {
        mediaPlayer.release();
        mediaPlayer = MediaPlayer.create(this, Uri.parse(path));
        mediaPlayer.setVolume(1,0);
        mediaPlayer.start();
    }

    /**
     * Will calculate the decibel
     * @param L_out The value of the new decibel level
     * @return decibel ...
     */
    private float calculateDB(int L_out) {
        int L_max = 90;
        float L_diff = L_out - L_max;

        return (float)(Math.pow(10, (L_diff/20)));
    }

    /**
     * When the user is pressing or releasing the button.
     * @param v The view.
     * @param event the event from the button.
     * @return will just return true.
     */
    @Override
    public boolean onTouch(View v, MotionEvent event) {

        // Check if the user pressed or released the button
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            pressed = true;
            // Control so it don't save values outside the bound of array
            if (clicked < 6) {
                // If it is the first click don't save it.
                if (clicked != 0) {
                    values[clicked - 1] = decibelLevel;
                }
                clicked++;
            }

        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            v.performClick();
            pressed = false;
            // Control so it don't save values outside the bound of array
            if (clicked < 6) {
                // If it is the first click don't save it. Should not happen.
                if (clicked != 0) {
                    values[clicked - 1] = decibelLevel;
                }
                clicked++;
            }
        }
        return true;
    }

    /**
     * Will show the Toast message on screen.
     * @param message the message that will be printed
     */
    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    /**
     * The main program for temporalMasking right ear
     */
    @SuppressLint("SetTextI18n")
    private void temporalMaskingRightEar(){
        executor.execute(() -> {
            int sum = 0;
            // Loop through gaps
            for (int i = 0; i < 5; i++) {

                // Loop through sounds
                for (int j = 0; j < 3; ) {


                    // Stop thread if application is tabbed or backed out of
                    if (Thread.currentThread().isInterrupted()){
                        return;
                    }

                    // Check if user have pressed/released the button six times.
                    if (clicked == 6) {

                        // Calculate the threshold
                        for (int k = 0; k < 5; k++) {
                            sum += values[k];
                        }
                        sum = sum/5;

                        // Save threshold value to array.
                        thresholdTemporalValuesRightEar[j][i] = sum;

                        j++;
                        clicked = 0;
                        decibelLevel = 50;
                        sum = 0;
                        continue;
                    }

                    // Control if the user is pressing the button or not
                    if (pressed && clicked < 2) {
                        // Reduce the decibel level.
                        decibelLevel -= 5;
                    } else if (pressed) {
                        decibelLevel -= 2;
                    } else if (decibelLevel < 80 && clicked >= 2) {
                        decibelLevel += 2;
                    } else if (decibelLevel < 80) {
                        // Increase the decibel level.
                        decibelLevel += 5;
                    }

                    // Create the new wav file with correct decibel level on tone (ftm).
                    combineWavFileTemporalMasking(noiseSounds[j], ftmTones[j], gaps[i], decibelLevel);
                    playSoundRightEar(pathWav);

                    // Wait for mediaPlayer to finish.
                    waitToFinish();
                    // Sleep for the same time as gap length.
                    SystemClock.sleep(gapTimes[i]);
                }
            }

            // Will print out all the thresholds values.
            MainActivity.this.runOnUiThread(() -> {

                binding.pressButton.setVisibility(View.GONE);
                binding.StartAudioButton.setVisibility(View.VISIBLE);
                binding.textField.setText("Audiometry, right ear");

            });



        });
        handler.post(() -> {
            //Do UI Thread work here

        });
    }

    /**
     * The main program for temporalMasking left ear
     */
    @SuppressLint("SetTextI18n")
    private void temporalMaskingLeftEar(){
        executor.execute(() -> {
            int sum = 0;
            decibelLevel = 50;
            // Loop through gaps
            for (int i = 0; i < 5; i++) {
                // Loop through sounds
                for (int j = 0; j < 3; ) {

                    // Stop thread if application is tabbed or backed out of
                    if (Thread.currentThread().isInterrupted()){
                        return;
                    }

                    // Check if user have pressed/released the button six times.
                    if (clicked == 6) {

                        // Calculate the threshold
                        for (int k = 0; k < 5; k++) {
                            sum += values[k];
                        }
                        sum = sum/5;

                        // Save threshold value to array.
                        thresholdTemporalValuesLeftEar[j][i] = sum;

                        j++;
                        clicked = 0;
                        decibelLevel = 50;
                        sum = 0;
                        continue;
                    }

                    // Control if the user is pressing the button or not
                    if (pressed && clicked < 2) {
                        // Reduce the decibel level.
                        decibelLevel -= 5;
                    } else if (pressed) {
                        decibelLevel -= 2;
                    } else if (decibelLevel < 80 && clicked >= 2) {
                        decibelLevel += 2;
                    } else if (decibelLevel < 80) {
                        // Increase the decibel level.
                        decibelLevel += 5;
                    }

                    // Create the new wav file with correct decibel level on tone (ftm).
                    combineWavFileTemporalMasking(noiseSounds[j], ftmTones[j], gaps[i], decibelLevel);
                    playSoundLeftEar(pathWav);

                    // Wait for mediaPlayer to finish.
                    waitToFinish();
                    // Sleep for the same time as gap length.
                    SystemClock.sleep(gapTimes[i]);
                }
            }

            // Will print out all the thresholds values.
            MainActivity.this.runOnUiThread(() -> {

                binding.pressButton.setVisibility(View.GONE);
                binding.StartLeftAudioButton.setVisibility(View.VISIBLE);
                binding.textField.setText("Audiometry, left ear");

            });

        });
        handler.post(() -> {
            //Do UI Thread work here

        });
    }


    /**
     * The main program for audiometry right ear
     */
    @SuppressLint("SetTextI18n")
    private void audiometryRightEar(){
        executor.execute(() -> {
            int sum = 0;
            decibelLevel = 30;
            // Loop through tones
            for (int i = 0; i < 9; ) {

                // Stop thread if application is tabbed or backed out of
                if (Thread.currentThread().isInterrupted()){
                    return;
                }
                
                // Check if user have pressed/released the button six times.
                if (clicked == 6) {

                    // Calculate the threshold
                    for (int k = 0; k < 5; k++) {
                        sum += values[k];
                    }
                    sum = sum/5;

                    // Save threshold value to array.
                    thresholdAudiometryRightEar[i] = sum;

                    i++;
                    clicked = 0;
                    decibelLevel = 0;
                    sum = 0;
                    continue;
                }

                // Control if the user is pressing the button or not
                if (pressed && clicked < 2) {
                    // Reduce the decibel level.
                    decibelLevel -= 5;
                } else if (pressed) {
                    decibelLevel -= 2;
                } else if (decibelLevel < 80 && clicked >= 2) {
                    decibelLevel += 2;
                } else if (decibelLevel < 80) {
                    // Increase the decibel level.
                    decibelLevel += 5;
                }

                // Create the new wav file with correct decibel level on tone (ftm).
                combineWavFileAudiometry(tones[i], gap1s, decibelLevel);
                playSoundRightEar(pathWav);

                // Wait for mediaPlayer to finish.
                waitToFinish();

            }

            // Will print out all the thresholds values.
            MainActivity.this.runOnUiThread(() -> {

                binding.pressButton.setVisibility(View.GONE);
                binding.StartTemporalButton.setVisibility(View.VISIBLE);
                binding.textField.setText("Temporal masking, left ear");

            });


        });
        handler.post(() -> {
            //Do UI Thread work here

        });
    }

    /**
     * The main program for audiometry left ear
     */
    @SuppressLint("SetTextI18n")
    private void audiometryLeftEar(){
        executor.execute(() -> {
            decibelLevel = 0;
            int sum = 0;
            // Loop through tones
            for (int i = 0; i < 9; ) {

                // Stop thread if application is tabbed or backed out of
                if (Thread.currentThread().isInterrupted()){
                    return;
                }

                // Check if user have pressed/released the button six times.
                if (clicked == 6) {

                    // Calculate the threshold
                    for (int k = 0; k < 5; k++) {
                        sum += values[k];
                    }
                    sum = sum/5;

                    // Save threshold value to array.
                    thresholdAudiometryLeftEar[i] = sum;

                    i++;
                    clicked = 0;
                    decibelLevel = 0;
                    sum = 0;
                    continue;
                }

                // Control if the user is pressing the button or not
                if (pressed && clicked < 2) {
                    // Reduce the decibel level.
                    decibelLevel -= 5;
                } else if (pressed) {
                    decibelLevel -= 2;
                } else if (decibelLevel < 80 && clicked >= 2) {
                    decibelLevel += 2;
                } else if (decibelLevel < 80) {
                    // Increase the decibel level.
                    decibelLevel += 5;
                }

                // Create the new wav file with correct decibel level on tone (ftm).
                combineWavFileAudiometry(tones[i], gap1s, decibelLevel);
                playSoundLeftEar(pathWav);

                // Wait for mediaPlayer to finish.
                waitToFinish();

            }

            // Will print out all the thresholds values.
            MainActivity.this.runOnUiThread(() -> {
                binding.pressButton.setVisibility(View.GONE);
                binding.textField.setText("Connect to the deice and then send data");
                binding.Connection.setVisibility(View.VISIBLE);

            });

            try {
                saveArraysToFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

        });
        handler.post(() -> {
            //Do UI Thread work here

        });
    }

    /**
     * Will save down all the arrays to text file to be able to study the results from test persons.
     * @throws IOException Will throw a IOException if it fails.
     */
    private void saveArraysToFile() throws IOException {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 5; j++) {
                builder.append(thresholdTemporalValuesRightEar[i][j]);
                builder.append(",");
            }
        }
        BufferedWriter writer = new BufferedWriter(new FileWriter("/storage/emulated/0/Download/tempRightEar.txt"));
        writer.write(builder.toString());
        writer.close();

        builder.setLength(0);

        for (int i = 0; i < 9; i++) {
            builder.append(thresholdAudiometryRightEar[i]);
            builder.append(",");
        }
        writer = new BufferedWriter(new FileWriter("/storage/emulated/0/Download/audioRightEar.txt"));
        writer.write(builder.toString());
        writer.close();

        builder.setLength(0);

        for (int i = 0; i < 9; i++) {
            builder.append(thresholdAudiometryLeftEar[i]);
            builder.append(",");
        }
        writer = new BufferedWriter(new FileWriter("/storage/emulated/0/Download/audioLeftEar.txt"));
        writer.write(builder.toString());
        writer.close();

        builder.setLength(0);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 5; j++) {
                builder.append(thresholdTemporalValuesLeftEar[i][j]);
                builder.append(",");
            }
        }
        writer = new BufferedWriter(new FileWriter("/storage/emulated/0/Download/tempLeftEar.txt"));
        writer.write(builder.toString());
        writer.close();
    }

    /**
     * A dummy function to make the program wait for mediaPlayer to finish.
     */
    private void waitToFinish() {
        while (mediaPlayer.isPlaying()) {

        }
    }

    /**
     * Will combine noise, gap and tone to one wav file. Will also set the intensity of the ftm tone.
     * @param noise The noise file.
     * @param tone The ftm tone file.
     * @param gap The gap that is going to be between noise and tone.
     * @param dB The new intensity of the ftm tone.
     */
    public void combineWavFileTemporalMasking(@RawRes int noise, @RawRes int tone, byte[] gap, int dB) {
        try {
            int read;
            // Read the byte's from noiseWav to byte[].
            InputStream is = getResources().openRawResource(noise);
            byte[] wavData = new byte[is.available()];
            read = is.read(wavData);
            Log.i("MyActivity " + "wavData", String.valueOf(read));
            is.close();

            // Read the byte's from toneWav to byte[].
            InputStream toneIs = getResources().openRawResource(tone);
            byte[] toneArray = new byte[toneIs.available()];
            read = toneIs.read(toneArray);
            Log.i("MyActivity " + "toneArray", String.valueOf(read));
            toneIs.close();

            // Adjust the intensity of the tone with the correct decibel value.
            toneArray = adjustVolume(toneArray, dB);

            // Create a new byte[] and store noise, gap and tone in it without a header.
            byte[] combined = new byte[wavData.length - 44 + gap.length + toneArray.length - 46];
            ByteBuffer byteBuffer = ByteBuffer.wrap(combined);
            byteBuffer.put(wavData, 44, wavData.length - 44);
            byteBuffer.put(gap);
            byteBuffer.put(toneArray, 46, toneArray.length - 46);

            // Set the size of the data chunk and size of the full size of the file.
            waveHeader.setSubChunk2Size(combined.length);
            waveHeader.setChunkSize();

            // Create a new header and store it in a byte[].
            byte[] bytes = waveHeader.createWaveHeader();

            // Write header and the combined sounds to one byte[].
            ByteArrayOutputStream out = new ByteArrayOutputStream( );
            out.write(bytes);
            out.write(combined);
            byte[] sound = out.toByteArray();
            out.flush();
            out.close();

            File file = new File(pathWav);

            // Check if file exists otherwise create file.
            if (!file.exists()) {
                Boolean value = file.createNewFile();
                Log.i("MyActivity " + "createFile", String.valueOf(value));
            }

            // Write to file.
            FileOutputStream stream = new FileOutputStream(pathWav);
            stream.write(sound);
            stream.close();



        } catch (Exception e){
            e.printStackTrace();
        }

    }

    /**
     * Will combine tone and gap to one wav file and setting the intensity of tone.
     * @param tone The tone file
     * @param gap The gap that is going to be after the tone. Same length as tone 1s.
     * @param dB The intensity of the tone.
     */
    public void combineWavFileAudiometry(@RawRes int tone, byte[] gap, int dB) {
        try {
            int read;

            // Read the byte's from toneWav to byte[].
            InputStream toneIs = getResources().openRawResource(tone);
            byte[] toneArray = new byte[toneIs.available()];
            read = toneIs.read(toneArray);
            Log.i("MyActivity " + "toneArray", String.valueOf(read));
            toneIs.close();

            // Adjust the intensity of the tone with the correct decibel value.
            toneArray = adjustVolume(toneArray, dB);

            // Create a new byte[] and store noise, gap and tone in it without a header.
            byte[] combined = new byte[gap.length + toneArray.length - 46];
            ByteBuffer byteBuffer = ByteBuffer.wrap(combined);
            byteBuffer.put(toneArray, 46, toneArray.length - 46);
            byteBuffer.put(gap);


            // Set the size of the data chunk and size of the full size of the file.
            waveHeader.setSubChunk2Size(combined.length);
            waveHeader.setChunkSize();

            // Create a new header and store it in a byte[].
            byte[] bytes = waveHeader.createWaveHeader();

            // Write header and the combined sounds to one byte[].
            ByteArrayOutputStream out = new ByteArrayOutputStream( );
            out.write(bytes);
            out.write(combined);
            byte[] sound = out.toByteArray();
            out.flush();
            out.close();

            File file = new File(pathWav);

            // Check if file exists otherwise create file.
            if (!file.exists()) {
                Boolean value = file.createNewFile();
                Log.i("MyActivity " + "createFile", String.valueOf(value));
            }

            // Write to file.
            FileOutputStream stream = new FileOutputStream(pathWav);
            stream.write(sound);
            stream.close();

        } catch (Exception e){
            e.printStackTrace();
        }

    }

    /**
     * Will multiply all the samples with the audio coefficient.
     * @param audioSamples Byte array with all the audio samples in 2 bytes.
     * @param volume The requested volume in decibel.
     * @return a byte array with all the audio samples but with the new volume.
     */
    private byte[] adjustVolume(byte[] audioSamples, int volume) {
        byte[] newAudio = new byte[audioSamples.length];
        for (int i = 0; i < newAudio.length; i += 2) {
            short buf1 = audioSamples[i + 1];
            short buf2 = audioSamples[i];

            buf1 = (short) ((buf1 & 0xff) << 8);
            buf2 = (short) (buf2 & 0xff);

            float newVolume = calculateDB(volume);

            short res = (short) (buf1 | buf2);
            res = (short) (res * newVolume);

            newAudio[i] = (byte) res;
            newAudio[i + 1] = (byte) (res >> 8);

        }
        return newAudio;
    }

    @Override
    protected void onResume() {
        super.onResume();
        setMaxVolume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        executor.shutdownNow();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
