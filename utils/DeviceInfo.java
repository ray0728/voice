package com.ray.lab.voice.util;

import android.os.Build;
import android.util.Base64;

public class DeviceInfo {
    private static final String TAG = "DeviceInfo";
    private String model;
    private String serial;
    public static DeviceInfo instance = null;
    private DeviceInfo(){
        model = Build.MODEL;
        serial = "35" +
                Build.BOARD.length() % 10 +
                Build.BRAND.length() % 10 +
                Build.CPU_ABI.length() % 10 +
                Build.DEVICE.length() % 10 +
                Build.DISPLAY.length() % 10 +
                Build.HOST.length() % 10 +
                Build.ID.length() % 10 +
                Build.MANUFACTURER.length() % 10 +
                Build.MODEL.length() % 10 +
                Build.PRODUCT.length() % 10 +
                Build.TAGS.length() % 10 +
                Build.TYPE.length() % 10 +
                Build.USER.length() % 10;
    }

    public static DeviceInfo self(){
        if(instance == null){
            instance = new DeviceInfo();
        }
        return instance;
    }

    public String getModel() {
        return model;
    }

    public String getSerial() {
        return serial;
    }

    public String toLocalString(String url, double lon, double lat) {
        return String.format("%s?name=%s&info=%s&lat=%f&lon=%f", url, Base64.encodeToString(model.getBytes(), Base64.DEFAULT), Base64.encodeToString(serial.getBytes(), Base64.DEFAULT), lat, lon);
    }

    public String toRtmpString(String url){
        return url + Base64.encodeToString(model.getBytes(), Base64.DEFAULT);
    }
}
