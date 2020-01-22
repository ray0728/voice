package com.ray.lab.voice.util;

public class FFMpegHelper {
    private static FFMpegHelper instance = null;
    static{
        System.loadLibrary("ffmpeg-helper");
    }
    private FFMpegHelper(){

    }

    public static FFMpegHelper self(){
        if(instance == null){
            instance = new FFMpegHelper();
        }
        return instance;
    }

    public native int setRtmpServer(String outUrl);

    public native int pushStream(byte[] buffer,int ylen,byte[] ubuffer,int ulen,byte[] vbuffer,int vlen);

    public native int close();
}
