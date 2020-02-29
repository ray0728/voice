package com.ray.lab.voice.services;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Point;
import android.hardware.camera2.CameraDevice;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.util.Size;
import android.view.WindowManager;

import androidx.annotation.Nullable;

import com.ray.lab.voice.util.CameraHelper;
import com.ray.lab.voice.util.RtpRawData;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraService extends Service implements CameraHelper.CameraListener {
    private static final String TAG = "CameraService";
    private CameraHelper mCameraHelper = null;
    private IBinder mBinder = new LocalBinder();
    private ExecutorService mExecutorService;
    private StreamService mStreamService;
    private ServiceConnection mStreamConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mStreamService = ((StreamService.LocalBinder)service).getService();
            mCameraHelper.start();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mStreamService = null;
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mCameraHelper == null) {
            int width = intent.getIntExtra("WIDTH", 1080);
            int height = intent.getIntExtra("HEIGHT", 720);
            mExecutorService = Executors.newSingleThreadExecutor();
            mCameraHelper = new CameraHelper.Builder()
                    .cameraListener(this)
                    .maxPreviewSize(new Point(1920, 1080))
                    .minPreviewSize(new Point(640, 480))
                    .specificCameraId(CameraHelper.CAMERA_ID_FRONT)
                    .context(getApplicationContext())
                    .previewViewSize(new Point(width, height))
                    .rotation(((WindowManager) (this.getSystemService(Context.WINDOW_SERVICE))).getDefaultDisplay().getRotation())
                    .build();
        }
        intent = new Intent(this, StreamService.class);
        bindService(intent, mStreamConnection, Context.BIND_AUTO_CREATE);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        if(mStreamService != null){
            unbindService(mStreamConnection);
        }
        if (mExecutorService != null) {
            mExecutorService.shutdown();
            mExecutorService = null;
        }
        if (mCameraHelper != null) {
            mCameraHelper.release();
        }
        super.onDestroy();
    }

    @Override
    public void onCameraOpened(CameraDevice cameraDevice, String cameraId, Size previewSize, int displayOrientation) {

    }

    @Override
    public void onPreview(byte[] nv12) {
        mStreamService.append(RtpRawData.TYPE_DATA_VIDEO, nv12, nv12.length);
    }

    @Override
    public void onSizeChanged(int width, int height) {
        mStreamService.setVideoParams(width, height);
    }

    @Override
    public void onCameraClosed() {

    }

    @Override
    public void onCameraError(Exception e) {
        Log.e(TAG, "onCameraError " + e.getMessage());
    }

    public class LocalBinder extends Binder {
        public CameraService getService() {
            return CameraService.this;
        }
    }
}
