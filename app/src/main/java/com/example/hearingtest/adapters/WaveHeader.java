package com.example.hearingtest.adapters;

public class WaveHeader {

    private String chunkId;
    private short chunkSize;
    private String format;
    private String subChunk1Id;
    private short subChunk1Size;
    private int audioFormat;
    private int channels;
    private int sampleRate;
    private short byteRate;
    private int blockAlign;
    private int bitsPerSample;
    private String subChunk2Id;
    private short subChunk2Size;
    private int headerLengthInBytes;
    private byte[] bytes;

    public WaveHeader() {
        this.chunkId = "RIFF";
        this.chunkSize = 36;
        this.format = "WAVE";
        this.subChunk1Id = "fmt";
        this.subChunk1Size = 16;
        this.audioFormat = 1;
        this.channels = 1;
        this.sampleRate = 48000;
        this.byteRate = 16000;
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

}
