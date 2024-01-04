package com.gowtham.utils;

public class CompressOption {

    private int frameRate=30;

    private String bitRate="0k";

    private int width=0;

    private int height=0;

    public CompressOption() {
    }

    public int getWidth() {
        return width;
    }
    public int getHeight() {
        return height;
    }
    public int getFrameRate() {
        return frameRate;
    }

    public String getBitRate() {
        return bitRate;
    }

}
