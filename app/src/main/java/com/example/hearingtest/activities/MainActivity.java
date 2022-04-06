package com.example.hearingtest.activities;

import androidx.annotation.RawRes;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.example.hearingtest.databinding.ActivityMainBinding;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
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
    }

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
     * Will calculate the decibel ...
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
            is.close();

            // Read the byte's from wav to byte[] of tone.
            InputStream toneIs = getResources().openRawResource(tone);
            byte[] toneArray = new byte[toneIs.available()];
            toneIs.close();

            // Combined noise with gap and tone
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
            outputStream.write(wavData, 44, wavData.length);
            outputStream.write(gap, 0, gap.length);
            outputStream.write(toneArray, 46, toneArray.length);


            byte[] combined = outputStream.toByteArray( );
            outputStream.flush();
            outputStream.close();

            // Create new header for combined

            // Create new wav file from combined byte[]
            File file = new File(pathWav);

            if (!file.exists()) {
                Boolean value = file.createNewFile();
            }

            FileOutputStream stream = new FileOutputStream(pathWav);
            stream.write(combined);
            stream.close();

        } catch (Exception e){
            e.printStackTrace();
        }

    }
}