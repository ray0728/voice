package com.ray.lab.voice.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.ray.lab.voice.util.DeviceInfo;
import com.ray.lab.voice.util.FFMpegHelper;
import com.ray.lab.voice.util.RtpRawData;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StreamService extends Service {
    private static final String TAG = "StreamService";
    private static final String RTMP_URL = "rtmp://www.iamray.cn/stream/";
    private static final int VIDEO_PARAMS_CONFIGED = 1;
    private static final int AUDIO_PARAMS_CONFIGED = 2;
    private static final int ALL_PARAMS_CONFIGED = 3;
    private IBinder mBinder = new LocalBinder();
    private ArrayBlockingQueue<RtpRawData> mRawDataQueue;
    private ExecutorService mProcessExecutor;
    private int isReady = 0;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class LocalBinder extends Binder {
        public StreamService getService() {
            return StreamService.this;
        }
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        mProcessExecutor = Executors.newSingleThreadExecutor();
        mRawDataQueue = new ArrayBlockingQueue<>(1024);
        FFMpegHelper.self().setRtmpServer(DeviceInfo.self().toRtmpString(RTMP_URL));
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        if (mProcessExecutor != null) {
            isReady = 0;
            mProcessExecutor.shutdown();
            mProcessExecutor = null;
        }
        FFMpegHelper.self().dispose();
        super.onDestroy();
    }

    public void setVideoParams(int width, int height) {
        FFMpegHelper.self().initVideoEncode(width, height);
        isReady &= VIDEO_PARAMS_CONFIGED;
        if (isReady == ALL_PARAMS_CONFIGED) {
            startPush();
        }
    }

    public void setAudioParams(int sample, int channel, int rate) {
        FFMpegHelper.self().initAudioEncode(sample, channel, rate);
        isReady &= AUDIO_PARAMS_CONFIGED;
        if (isReady == ALL_PARAMS_CONFIGED) {
            startPush();
        }
    }

    public void append(int type, byte[] data, int size) {
        try {
            mRawDataQueue.put(new RtpRawData(type, data, size));
        } catch (InterruptedException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private void startPush() {
        mProcessExecutor.execute(new Runnable() {
            @Override
            public void run() {
                RtpRawData rawData;
                while (isReady == ALL_PARAMS_CONFIGED) {
                    try {
                        rawData = mRawDataQueue.take();
                        if (rawData == null) {
                            continue;
                        }
                        if(rawData.isVideoData()) {
                            FFMpegHelper.self().pushVideo(rawData.getData(), rawData.getSize());
                        }else{
                            FFMpegHelper.self().pushAudio(rawData.getData(), rawData.getSize());
                        }
                    } catch (InterruptedException e) {
                        Log.e(TAG, e.getMessage());
                    }
                }
            }
        });
    }
}
