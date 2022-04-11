package com.example.hearingtest.adapters;

import java.nio.ByteBuffer;

public class WaveHeader {

    private String chunkId; //4 bytes
    private int chunkSize; //4 bytes
    private String format; //4 bytes
    private String subChunk1Id; //4 bytes
    private int subChunk1Size; //4 bytes
    private short audioFormat; //2 bytes
    private short channels; //2 bytes
    private int sampleRate; //4 bytes
    private int byteRate; //4 bytes
    private short blockAlign; //2 bytes
    private short bitsPerSample; //2 bytes
    private String subChunk2Id; //4 bytes
    private int subChunk2Size; //4 bytes

    private int headerLengthInBytes;

    public WaveHeader() {
        this.chunkId = "RIFF";
        this.chunkSize = 36;
        this.format = "WAVE";
        this.subChunk1Id = "fmt ";
        this.subChunk1Size = 16;
        this.audioFormat = 1;
        this.channels = 1;
        this.sampleRate = 48000;
        this.byteRate = 96000;
        this.blockAlign = 2;
        this.bitsPerSample = 16;
        this.subChunk2Id = "data";

        this.headerLengthInBytes = 44;
    }

    /**
     * Set the size of the data chunk.
     * @param dataSize the size of data.
     */
    public void setSubChunk2Size(short dataSize) {
        this.subChunk2Size = dataSize;
    }

    /**
     * Get the length of the header.
     * @return the length of the header.
     */
    public int getHeaderLengthInBytes() {
        return this.headerLengthInBytes;
    }

    /**
     * Will convert a short to an byte array with the value from the short.
     * @param s is the short that's need's to be converted to bytes.
     * @return the byte array with the value of the short.
     */
    public byte[] shortToByte(short s) {
        //Allocate bytes on the heap
        ByteBuffer byteBuffer = ByteBuffer.allocate(2);
        //Load buffer with bytes
        byteBuffer.putShort(s);
        return byteBuffer.array();
    }

    /**
     * Will convert a int to an byte array with the value from the int.
     * @param i is the int that's need's to be converted to bytes.
     * @return the array with the value of the int.
     */
    public byte[] integerToByte(int i) {
        //Allocate bytes on the heap
        ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        //Load buffer with byes
        byteBuffer.putInt(i);
        return byteBuffer.array();
    }

    /**
     * Will convert a string to an byte array with the value from the string.
     * @param s is the string with that need's to be converted to bytes.
     * @return the array with the value of the string.
     */
    public byte[] stringToByte(String s) {
        return s.getBytes();
    }

    /**
     * Creating the new wave header for the wave file.
     * @return a byte array filled with the new values.
     */
    public byte[] createWaveHeader() {
        byte[] bytes = stringToByte(this.chunkId);

        return bytes;
    }



}
