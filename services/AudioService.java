package com.ray.lab.voice.services;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioFormat;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.ray.lab.voice.util.AudioHelper;
import com.ray.lab.voice.util.RtpRawData;

public class AudioService extends Service implements AudioHelper.Listener {
    private static final String TAG = "AudioService";
    private static final int RECORD_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int RECORD_CHANNEL = android.media.AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORD_SAMPLE_RATE = 32000;
    private IBinder mBinder = new AudioService.LocalBinder();
    private AudioHelper mAudioHelper = null;
    private StreamService mStreamService;
    private ServiceConnection mStreamConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mStreamService = ((StreamService.LocalBinder)service).getService();
            mAudioHelper.startRecord();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mStreamService = null;
        }
    };

    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mAudioHelper == null) {
            mAudioHelper = AudioHelper.self(RECORD_SAMPLE_RATE, RECORD_FORMAT, RECORD_CHANNEL);
            mAudioHelper.setListener(this);
        }
        intent = new Intent(this, StreamService.class);
        bindService(intent, mStreamConnection, Context.BIND_AUTO_CREATE);
        return super.onStartCommand(intent, flags, startId);
    }

    public void onDestroy() {
        if(mStreamService != null){
            unbindService(mStreamConnection);
        }
        if (mAudioHelper != null) {
            mAudioHelper.stopRecord();
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onRecording(int length, byte[] data) {
        mStreamService.append(RtpRawData.TYPE_DATA_AUDIO, data, length);
    }

    public class LocalBinder extends Binder {
        public AudioService getService() {
            return AudioService.this;
        }
    }
}
