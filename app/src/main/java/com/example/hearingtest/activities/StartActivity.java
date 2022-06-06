package com.example.hearingtest.activities;

import androidx.annotation.NonNull;
import androidx.annotation.RawRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.example.hearingtest.R;
import com.example.hearingtest.adapters.WaveHeader;
import com.example.hearingtest.databinding.ActivityStartBinding;

import java.util.concurrent.Executors;

public class StartActivity extends AppCompatActivity {

    private ActivityStartBinding binding;
    private MediaPlayer mediaPlayer;
    private AudioManager audioManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStartBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        init();
        setListeners();
        checkPermission();
    }

    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(StartActivity.this, new String[]{
                    Manifest.permission.BLUETOOTH_CONNECT}, 1);
        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("DEBUG_MA", "permission granted");
            } else {
                Log.e("DEBUG_MA", "permission denied");
            }
        }
    }

    /**
     * Initialize all the variables.
     */
    private void init() {
        mediaPlayer = new MediaPlayer();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        setMaxVolume();
    }

    /**
     * Will show the Toast message on screen.
     * @param message the message that will be printed
     */
    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Initialize listeners.
     */
    private void setListeners() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        binding.noiseButton.setOnClickListener(v -> {
            if (!mediaPlayer.isPlaying()) {
                playSound(R.raw.noise_500hz);
            }
        });
        binding.toneButton.setOnClickListener(v -> {
            if (!mediaPlayer.isPlaying()) {
                playSound(R.raw.tone_500hz);
            }
        });
        binding.ftmButton.setOnClickListener(v -> {
            if (!mediaPlayer.isPlaying()) {
                playSound(R.raw.ftm_4000hz);
            }
        });
        binding.nextButton.setOnClickListener(v -> {
            if (mBluetoothAdapter == null) {
                showToast("Device don't have bluetooth");
                finish();
            } else if (!mBluetoothAdapter.isEnabled()) {
                showToast("Start Bluetooth before going to next page");
            } else {
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
            }

        });

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
     * @param sound the sound that will be played.
     */
    private void playSound(@RawRes int sound) {
        mediaPlayer.release();
        mediaPlayer = MediaPlayer.create(this, sound);
        mediaPlayer.start();
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