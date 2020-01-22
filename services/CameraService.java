package com.ray.lab.voice.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.hardware.camera2.CameraDevice;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.util.Size;
import android.view.WindowManager;

import androidx.annotation.Nullable;

import com.ray.lab.voice.util.CameraHelper;
import com.ray.lab.voice.util.DeviceInfo;
import com.ray.lab.voice.util.FFMpegHelper;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraService extends Service implements CameraHelper.CameraListener {
    private static final String TAG = "CameraService";
    private static final String RTMP_URL = "rtmp://139.9.147.147:1935/stream/";
    private CameraHelper mCameraHelper = null;
    private IBinder mBinder = new LocalBinder();
    private byte[] mNv21ByteArray;
    private ExecutorService mExecutorService;



    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mCameraHelper == null) {
            FFMpegHelper.self().setRtmpServer(DeviceInfo.self().toRtmpString(RTMP_URL));
            mExecutorService = Executors.newSingleThreadExecutor();
            mCameraHelper = new CameraHelper.Builder()
                    .cameraListener(this)
                    .maxPreviewSize(new Point(1920, 1080))
                    .minPreviewSize(new Point(640, 480))
                    .specificCameraId(CameraHelper.CAMERA_ID_BACK)
                    .context(getApplicationContext())
                    .previewViewSize(new Point(1280, 720))
                    .rotation(((WindowManager) (this.getSystemService(Context.WINDOW_SERVICE))).getDefaultDisplay().getRotation())
                    .build();
            mCameraHelper.start();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        if (mExecutorService != null) {
            mExecutorService.shutdown();
            mExecutorService = null;
        }
        if(mCameraHelper != null) {
            mCameraHelper.release();
        }
        super.onDestroy();
    }

    @Override
    public void onCameraOpened(CameraDevice cameraDevice, String cameraId, Size previewSize, int displayOrientation, boolean isMirror) {
        Log.d(TAG, "onCameraOpened");
    }

    @Override
    public void onPreview(byte[] y, byte[] u, byte[] v) {
        Log.d(TAG, "onPreview");
        FFMpegHelper.self().pushStream(y, y.length, u, u.length, v, v.length);
    }

    @Override
    public void onCameraClosed() {
        Log.d(TAG, "onCameraClose");
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
