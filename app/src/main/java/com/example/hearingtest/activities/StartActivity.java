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
     * Initialize listeners.
     */
    private void setListeners() {
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
                playSound(R.raw.ftm_500hz);
            }
        });
        binding.nextButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
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

}