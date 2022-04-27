package com.example.hearingtest.adapters;

import android.content.Context;
import android.media.AudioManager;

import androidx.annotation.RawRes;
import androidx.appcompat.app.AppCompatActivity;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class Sound extends AppCompatActivity {

    private AudioManager audioManager;
    private WaveHeader waveHeader;
    private String pathWav;

    public Sound() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        waveHeader = new WaveHeader();
        pathWav = "/storage/emulated/0/Download/test.wav";
    }

    /**
     * Set max volume on music on device
     */
    public void setMaxVolume() {
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
    };

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
     * Will combine noise, gap and tone to one wav file.
     * @param noise The noise file.
     * @param tone The tone file.
     * @param gap The gap that is going to be between noise and tone.
     */
    public void combineWavFile(@RawRes int noise, @RawRes int tone, byte[] gap) {
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

}
