package com.example.hearingtest.adapters;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class WaveHeader {

    private static String chunkId; //4 bytes
    private static int chunkSize; //4 bytes
    private static String format; //4 bytes
    private static String subChunk1Id; //4 bytes
    private static int subChunk1Size; //4 bytes
    private static short audioFormat; //2 bytes
    private static short channels; //2 bytes
    private static int sampleRate; //4 bytes
    private static int byteRate; //4 bytes
    private static short blockAlign; //2 bytes
    private static short bitsPerSample; //2 bytes
    private static String subChunk2Id; //4 bytes
    private static int subChunk2Size; //4 bytes

    private static int headerLengthInBytes;

    public WaveHeader() {
        this.chunkId = "RIFF";
        this.chunkSize = 0;
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
    public void setSubChunk2Size(int dataSize) {
        this.subChunk2Size = dataSize;
    }

    /**
     * Get the length of the header.
     * @return the length of the header.
     */
    public int getHeaderLengthInBytes() {
        return this.headerLengthInBytes;
    }

    public void setChunkSize() {
        this.chunkSize = (36 + this.subChunk2Size);
    }

    /**
     * Will convert a short to an byte array with the value from the short.
     * @param s is the short that's need's to be converted to bytes.
     * @return the byte array with the value of the short.
     */
    public byte[] shortToByte(short s) {
        //Allocate bytes on the heap
        ByteBuffer byteBuffer = ByteBuffer.allocate(2);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
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
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
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
        ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        byteBuffer = ByteBuffer.wrap(s.getBytes(StandardCharsets.UTF_8));
        return byteBuffer.array();
    }

    /**
     * Creating the new wave header for the wave file.
     * @return a byte array filled with the new values.
     */
    public byte[] createWaveHeader() {
        byte[] bytes = new byte[this.headerLengthInBytes];
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        byteBuffer.put(stringToByte(this.chunkId));
        byteBuffer.put(integerToByte(this.chunkSize));
        byteBuffer.put(stringToByte(this.format));
        byteBuffer.put(stringToByte(this.subChunk1Id));
        byteBuffer.put(integerToByte(this.subChunk1Size));
        byteBuffer.put(shortToByte(this.audioFormat));
        byteBuffer.put(shortToByte(this.channels));
        byteBuffer.put(integerToByte(this.sampleRate));
        byteBuffer.put(integerToByte(this.byteRate));
        byteBuffer.put(shortToByte(this.blockAlign));
        byteBuffer.put(shortToByte(this.bitsPerSample));
        byteBuffer.put(stringToByte(this.subChunk2Id));
        byteBuffer.put(integerToByte(this.subChunk2Size));

        return bytes;
    }



}
