package com.example.hearingtest.activities;

import androidx.annotation.RawRes;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.example.hearingtest.R;
import com.example.hearingtest.adapters.WaveHeader;
import com.example.hearingtest.databinding.ActivityMainBinding;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
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
            combineWavFile(R.raw.noise_500hz, R.raw.ftm_500hz, gap80ms);
            mediaPlayer = MediaPlayer.create(this, Uri.parse(pathWav));
            mediaPlayer.start();
        } else if (event.getAction() == MotionEvent.ACTION_UP) {

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
    public void main(){
        executor.execute(() -> {
            //Do background work here
        });
        handler.post(() -> {
            //Do UI Thread work here
        });
    }

    /**
     * Will combine noise, gap and tone to one wav file.
     * @param noise The noise file.
     * @param tone The tone file.
     * @param gap The gap that is going to be between noise and tone.
     */
    private void combineWavFile(@RawRes int noise, @RawRes int tone, byte[] gap) {
        try {

            // Read the byte's from wav to byte[] of noise.
            InputStream is = getResources().openRawResource(noise);
            byte[] wavData = new byte[is.available()];
            is.read(wavData);
            is.close();

            // Read the byte's from wav to byte[] of tone.
            InputStream toneIs = getResources().openRawResource(tone);
            byte[] toneArray = new byte[toneIs.available()];
            toneIs.read(toneArray);
            toneIs.close();

            toneArray = adjustVolume(toneArray, 30);

            byte[] combined = new byte[wavData.length - 44 + gap.length + toneArray.length - 46];
            ByteBuffer byteBuffer = ByteBuffer.wrap(combined);
            byteBuffer.put(wavData, 44, wavData.length - 44);
            byteBuffer.put(gap);
            byteBuffer.put(toneArray, 46, toneArray.length - 46);

            // Create new header for combined
            waveHeader.setSubChunk2Size(combined.length);
            waveHeader.setChunkSize();
            byte[] bytes = waveHeader.createWaveHeader();

            ByteArrayOutputStream out = new ByteArrayOutputStream( );
            out.write(bytes);
            out.write(combined);
            byte[] sound = out.toByteArray();
            out.flush();
            out.close();

            // Create new wav file from combined byte[]
            File file = new File(pathWav);

            if (!file.exists()) {
                Boolean value = file.createNewFile();
            }

            FileOutputStream stream = new FileOutputStream(pathWav);
            stream.write(sound);
            stream.close();

        } catch (Exception e){
            e.printStackTrace();
        }

    }

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
}