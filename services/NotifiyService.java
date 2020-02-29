package com.ray.lab.voice.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.util.SparseArray;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.ray.lab.voice.MainActivity;
import com.ray.lab.voice.R;
import com.ray.lab.voice.util.Toolkit;

public class NotifiyService extends Service {
    private static final String TAG = "NotifiyService";
    private static final int DEFAULT_NOTIFY_ID = 1;
    private static final String CHANNEL_ID = "com.ray.lab.voice.notifiy";
    private NotificationChannel mNotificationChannel = null;
    private NotificationCompat.Builder mBuilder = null;
    private Binder mBinder = new LocalBinder();
    private SparseArray<Intent> mServiceIntent;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        RemoteViews views = new RemoteViews(getPackageName(), R.layout.notifiy_info);
        views.setTextViewText(R.id.tv_notifiy_title, getResources().getText(R.string.app_name));
        Toolkit.showForegroundNotifiy(this, DEFAULT_NOTIFY_ID, CHANNEL_ID, TAG, views, 0, 0);
        return super.onStartCommand(intent, flags, startId);
    }

    public void addService(Intent intent){
        startService(intent);
        if(mServiceIntent == null){
            mServiceIntent = new SparseArray<>();
        }
        mServiceIntent.append(mServiceIntent.size(), intent);
    }

    public void removeService(Intent intent){
        if(mServiceIntent != null && mServiceIntent.indexOfValue(intent) != -1){
            stopService(intent);
            mServiceIntent.remove(mServiceIntent.indexOfValue(intent));
        }
    }

    public void removeAllService(){
        if(mServiceIntent != null){
            for(int i = mServiceIntent.size() - 1; i >= 0; i--){
                stopService(mServiceIntent.valueAt(i));
                mServiceIntent.remove(i);
            }
        }
    }


    public class LocalBinder extends Binder {
        public NotifiyService getService() {
            return NotifiyService.this;
        }
    }
}
