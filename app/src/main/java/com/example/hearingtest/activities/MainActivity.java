package com.example.hearingtest.activities;

import androidx.annotation.RawRes;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
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
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends AppCompatActivity implements View.OnTouchListener {

    private ActivityMainBinding binding;
    private MediaPlayer mediaPlayer;
    private ExecutorService executor;
    private Handler handler;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        init();
        setListeners();
    }

    /**
     * Initialize listeners.
     */
    private void setListeners() {
        binding.pressButton.setOnTouchListener(this);
        binding.StartButton.setOnClickListener(v -> {
            binding.StartButton.setVisibility(View.INVISIBLE);
            binding.pressButton.setVisibility(View.VISIBLE);
            temporalMaskingRightEar();
        });
        binding.StartAudioButton.setOnClickListener(v -> {
            binding.StartAudioButton.setVisibility(View.GONE);
            binding.pressButton.setVisibility(View.VISIBLE);
            audiometryRightEar();
        });
        binding.StartTemporalButton.setOnClickListener(v -> {
            binding.StartTemporalButton.setVisibility(View.GONE);
            binding.pressButton.setVisibility(View.VISIBLE);
            temporalMaskingLeftEar();
        });
        binding.StartLeftAudioButton.setOnClickListener(v -> {
            binding.StartLeftAudioButton.setVisibility(View.GONE);
            binding.pressButton.setVisibility(View.VISIBLE);
            audiometryLeftEar();
        });
    }

    /**
     * Initialize all the variables.
     */
    private void init() {
        mediaPlayer = new MediaPlayer();
        executor = Executors.newSingleThreadExecutor();
        handler = new Handler(Looper.getMainLooper());
        pathWav = "/storage/emulated/0/Download/test.wav";
        waveHeader = new WaveHeader();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        setMaxVolume();
        decibelLevel = 50;
        values = new int[5];
        noiseSounds = new int[]{R.raw.noise_500hz, R.raw.noise_2000hz, R.raw.noise_4000hz};
        ftmTones = new int[]{R.raw.ftm_500hz, R.raw.ftm_2000hz, R.raw.noise_4000hz};
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
     * @param path take the path of the sound file
     */
    private void playSoundRightEar(String path) {
        mediaPlayer.release();
        mediaPlayer = MediaPlayer.create(this, Uri.parse(path));
        mediaPlayer.setVolume(0,1);
        mediaPlayer.start();
    }

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

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            pressed = true;
            if (clicked < 6) {
                if (clicked != 0) {
                    values[clicked - 1] = decibelLevel;
                }
                clicked++;
            }


        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            v.performClick();
            pressed = false;
            if (clicked < 6) {
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

                    // Check if user have pressed/released the button four times.
                    if (clicked == 6) {

                        // Calculate the threshold
                        for (int k = 0; k < 5; k++) {
                            sum += values[k];
                        }
                        sum = sum/5;

                        // Add threshold to array to be saved.
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
                    } else if (pressed && clicked >= 2) {
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
                for (int i = 0; i < 3 ; i++) {
                    for (int j = 0; j < 5; j++) {
                        showToast(Integer.toString(thresholdTemporalValuesRightEar[i][j]));
                    }
                }

                binding.pressButton.setVisibility(View.GONE);
                binding.StartAudioButton.setVisibility(View.VISIBLE);
                binding.textField.setText("Audiometry, right ear");

            });



        });
        handler.post(() -> {
            //Do UI Thread work here

        });
    }

    private void temporalMaskingLeftEar(){
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

                    // Check if user have pressed/released the button four times.
                    if (clicked == 6) {

                        // Calculate the threshold
                        for (int k = 0; k < 5; k++) {
                            sum += values[k];
                        }
                        sum = sum/5;

                        // Add threshold to array to be saved.
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
                    } else if (pressed && clicked >= 2) {
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
                for (int i = 0; i < 3 ; i++) {
                    for (int j = 0; j < 5; j++) {
                        showToast(Integer.toString(thresholdTemporalValuesRightEar[i][j]));
                    }
                }

                binding.pressButton.setVisibility(View.GONE);
                binding.StartLeftAudioButton.setVisibility(View.VISIBLE);
                binding.textField.setText("Audiometry, left ear");

            });

        });
        handler.post(() -> {
            //Do UI Thread work here

        });
    }

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
                
                // Check if user have pressed/released the button four times.
                if (clicked == 6) {

                    // Calculate the threshold
                    for (int k = 0; k < 5; k++) {
                        sum += values[k];
                    }
                    sum = sum/5;

                    // Add threshold to array to be saved.
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
                } else if (pressed && clicked >= 2) {
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
                for (int i = 0; i < 9 ; i++) {
                    showToast(Integer.toString(thresholdAudiometryRightEar[i]));
                }

                binding.pressButton.setVisibility(View.GONE);
                binding.StartTemporalButton.setVisibility(View.VISIBLE);
                binding.textField.setText("Temporal masking, left ear");

            });


        });
        handler.post(() -> {
            //Do UI Thread work here

        });
    }

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

                // Check if user have pressed/released the button four times.
                if (clicked == 6) {

                    // Calculate the threshold
                    for (int k = 0; k < 5; k++) {
                        sum += values[k];
                    }
                    sum = sum/5;

                    // Add threshold to array to be saved.
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
                } else if (pressed && clicked >= 2) {
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
                for (int i = 0; i < 9 ; i++) {
                    showToast(Integer.toString(thresholdAudiometryRightEar[i]));
                }

                binding.pressButton.setVisibility(View.GONE);

                binding.textField.setText("Done");

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

    private void saveArraysToFile() throws IOException {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 5; j++) {
                builder.append(thresholdTemporalValuesRightEar[i][j]+"");
                if (j < 5) {
                    builder.append(",");
                }
            }
        }
        BufferedWriter writer = new BufferedWriter(new FileWriter("/storage/emulated/0/Download/tempRightEar.txt"));
        writer.write(builder.toString());
        writer.close();

        builder.setLength(0);

        for (int i = 0; i < 9; i++) {
            builder.append(thresholdAudiometryRightEar[i]+"");
            if (i < 9) {
                builder.append(",");
            }
        }
        writer = new BufferedWriter(new FileWriter("/storage/emulated/0/Download/audioRightEar.txt"));
        writer.write(builder.toString());
        writer.close();

        builder.setLength(0);

        for (int i = 0; i < 9; i++) {
            builder.append(thresholdAudiometryLeftEar[i]+"");
            if (i < 9) {
                builder.append(",");
            }
        }
        writer = new BufferedWriter(new FileWriter("/storage/emulated/0/Download/audioLeftEar.txt"));
        writer.write(builder.toString());
        writer.close();

        builder.setLength(0);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 5; j++) {
                builder.append(thresholdTemporalValuesLeftEar[i][j]+"");
                if (j < 5) {
                    builder.append(",");
                }
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
     * Will combine noise, gap and tone to one wav file.
     * @param noise The noise file.
     * @param tone The tone file.
     * @param gap The gap that is going to be between noise and tone.
     */
    public void combineWavFileTemporalMasking(@RawRes int noise, @RawRes int tone, byte[] gap, int dB) {
        try {
            int read = 0;
            // Read the byte's from noiseWav to byte[].
            InputStream is = getResources().openRawResource(noise);
            byte[] wavData = new byte[is.available()];
            read = is.read(wavData);
            is.close();

            // Read the byte's from toneWav to byte[].
            InputStream toneIs = getResources().openRawResource(tone);
            byte[] toneArray = new byte[toneIs.available()];
            read = toneIs.read(toneArray);
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
            }

            // Write to file.
            FileOutputStream stream = new FileOutputStream(pathWav);
            stream.write(sound);
            stream.close();

        } catch (Exception e){
            e.printStackTrace();
        }

    }

    public void combineWavFileAudiometry(@RawRes int tone, byte[] gap, int dB) {
        try {
            int read = 0;

            // Read the byte's from toneWav to byte[].
            InputStream toneIs = getResources().openRawResource(tone);
            byte[] toneArray = new byte[toneIs.available()];
            read = toneIs.read(toneArray);
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
