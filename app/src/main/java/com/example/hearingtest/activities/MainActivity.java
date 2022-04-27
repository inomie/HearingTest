package com.example.hearingtest.activities;

import androidx.annotation.RawRes;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends AppCompatActivity implements View.OnTouchListener {

    private ActivityMainBinding binding;
    private MediaPlayer mediaPlayer;
    private ExecutorService executor;
    private Handler handler;
    private byte[] gap80ms;
    private byte[] gap40ms;
    private byte[] gap20ms;
    private byte[] gap10ms;
    private byte[] gap5ms;
    private String pathWav;
    private WaveHeader waveHeader;
    private AudioManager audioManager;
    private int decibelLevel;
    private boolean start;
    private boolean pressed = false;
    private int clicked = 0;
    private int[] values;
    int sum = 0;

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
            start = true;
            main();

        });
    }

    /**
     * Initialize all the variables.
     */
    private void init() {
        mediaPlayer = new MediaPlayer();
        executor = Executors.newSingleThreadExecutor();
        handler = new Handler(Looper.getMainLooper());
        gap80ms = new byte[7680];
        gap40ms = new byte[3840];
        gap20ms = new byte[1920];
        gap10ms = new byte[960];
        gap5ms = new byte[480];
        pathWav = "/storage/emulated/0/Download/test.wav";
        waveHeader = new WaveHeader();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        setMaxVolume();
        decibelLevel = 80;
        values = new int[4];
    }

    /**
     * Set max volume on music on device
     */
    private void setMaxVolume() {
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
    };

    /**
     * Will release the last mediaPlayer and create a new one and play the sound.
     * @param path take the path of the sound file
     */
    private void playSound(String path) {
        mediaPlayer.release();
        mediaPlayer = MediaPlayer.create(this, Uri.parse(path));
        mediaPlayer.setVolume(0,1);
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
            if (clicked < 4) {
                values[clicked] = decibelLevel;
                clicked++;
            }


        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            pressed = false;
            if (clicked < 4) {
                values[clicked] = decibelLevel;
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
     * The main program
     */
    private void main(){
        executor.execute(() -> {
            //Do background work here
            while (start && clicked < 4) {
                // Check if button is being pressed or not.
                if (pressed) {
                    // Reduce the decibel level.
                    decibelLevel -= 5;
                } else if (decibelLevel < 80) {
                    // Increase the decibel level.
                    decibelLevel += 5;
                }
                // Create the new wav file with correct decibel level on tone (ftm).
                combineWavFile(R.raw.noise_500hz, R.raw.ftm_500hz, gap80ms, decibelLevel);
                playSound(pathWav);
                // Wait for mediaPlayer to finish.
                waitToFinish();
                SystemClock.sleep(80);
            }

            // Calculate the threshold
            for (int i = 0; i < 4; i++) {
                sum += values[i];
            }
            sum = sum/4;

            // Print the threshold.
            MainActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    showToast(Integer.toString(sum));
                }
            });


        });
        handler.post(() -> {
            //Do UI Thread work here

        });
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
    public void combineWavFile(@RawRes int noise, @RawRes int tone, byte[] gap, int dB) {
        try {

            // Read the byte's from noiseWav to byte[].
            InputStream is = getResources().openRawResource(noise);
            byte[] wavData = new byte[is.available()];
            is.read(wavData);
            is.close();

            // Read the byte's from toneWav to byte[].
            InputStream toneIs = getResources().openRawResource(tone);
            byte[] toneArray = new byte[toneIs.available()];
            toneIs.read(toneArray);
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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
