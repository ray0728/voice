package com.ray.lab.voice.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.SparseArray;


import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.ray.lab.voice.MainActivity;
import com.ray.lab.voice.R;
import com.ray.lab.voice.StatusActivity;
import com.ray.lab.voice.services.options.LocationOptions;
import com.ray.lab.voice.storage.LocationStorage;
import com.ray.lab.voice.util.DeviceInfo;
import com.ray.lab.voice.util.HttpHelper;
import com.ray.lab.voice.util.LocationData;
import com.ray.lab.voice.util.Toolkit;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LocationService extends Service implements AMapLocationListener, HttpHelper.HttpListener {
    private static final String TAG = "LocationService";
    public static final String KEY_LOCATION_OPTIONS = "LocationOptions";
    private static final int DEFAULT_NOTIFY_ID = 2;
    private static final String CHANNEL_ID = "com.ray.lab.voice.location";
    private static final int MESSAGE_CALL_PHONE = 1;
    private static final int MESSAGE_CALL_APP = 2;
    private AMapLocationClient mLocationClient = null;
    private final IBinder mBinder = new LocalBinder();
    private HttpHelper mHttpHelper = null;
    private LocationStorage mStorage = null;
    private double[] mLastLocation = new double[2];
    private Intent mMainUIIntent = new Intent();
    private List<ScheduledExecutorService> mExecutorList = new ArrayList<>();

    @Override
    public void onCreate() {
        super.onCreate();
        mLastLocation[0] = 0;
        mLastLocation[1] = 0;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mMainUIIntent.setAction(StatusActivity.ACTION);
        mMainUIIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        final LocationOptions options = (LocationOptions) intent.getSerializableExtra(KEY_LOCATION_OPTIONS);
        initLocation(options.build());
        ScheduledExecutorService executor;
        if (options.hasPhoneOptions()) {
            executor = Executors.newSingleThreadScheduledExecutor();
            executor.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    startActivity(mMainUIIntent);
                    Toolkit.self().autoCallAndEnd(getApplicationContext(), options.getPhoneNumber(), options.getCallDelay());
                }
            }, 1, options.getCallIntervals(), TimeUnit.SECONDS);
            mExecutorList.add(executor);
        }
        if (options.hasThirdAppOptions()) {
            executor = Executors.newSingleThreadScheduledExecutor();
            executor.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    startActivity(mMainUIIntent);
                    Toolkit.self().startThirdApp(getApplicationContext(), options.getThirdAppPkgName());
                }
            }, 1, options.getCallThirdIntervals(), TimeUnit.SECONDS);
        }
//        Toolkit.showForegroundNotifiy(this, DEFAULT_NOTIFY_ID, CHANNEL_ID, TAG, null, R.string.gnss_trace, R.string.gnss_trace);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        if (mStorage != null) {
            mStorage.stop();
        }
        for(ScheduledExecutorService executor: mExecutorList){
            executor.shutdown();
        }
        destroyLocation();
        super.onDestroy();
    }

    private void initStorage() {
        if (mStorage == null) {
            mStorage = new LocationStorage(getApplicationContext(), "Location");
        }
    }

    public SparseArray<LocationData> getAllRecords(String date) {
        initStorage();
        return mStorage.getRecord(date);
    }

    public SparseArray<LocationData> getAllLocationRecords(long start, long end) {
        initStorage();
        return mStorage.getLocationRecord(start, end);
    }

    @Override
    public void onLocationChanged(AMapLocation location) {
        String locationType = "undefine";
        if (null != location && location.getErrorCode() == AMapLocation.LOCATION_SUCCESS) {
            if (location.getLatitude() == mLastLocation[0] && location.getLongitude() == mLastLocation[1]) {
                return;
            }
            mLastLocation[0] = location.getLatitude();
            mLastLocation[1] = location.getLongitude();
            mHttpHelper.put("https://www.iamray.cn/rst/lab/voice/device", DeviceInfo.self().toLocalString(location.getLatitude(), location.getLongitude()));
            initStorage();
            mStorage.record(Toolkit.formateDate(Toolkit.FORMAT_DATE_YMD_HMS, null), location.getLocationType(), location.getLatitude(), location.getLongitude());
        }
    }

    @Override
    public void onHttpSuccess(String data) {

    }

    @Override
    public void onHttpFail() {

    }

    public class LocalBinder extends Binder {
        public LocationService getService() {
            return LocationService.this;
        }
    }

    private void initLocation(AMapLocationClientOption option) {
        mHttpHelper = new HttpHelper(this);
        mLocationClient = new AMapLocationClient(this.getApplicationContext());
        mLocationClient.setLocationOption(option);
        mLocationClient.setLocationListener(this);
        mLocationClient.stopLocation();
        mLocationClient.startLocation();
    }

    private void destroyLocation() {
        if (null != mLocationClient) {
            mLocationClient.unRegisterLocationListener(this);
            mLocationClient.stopLocation();
            mLocationClient.onDestroy();
            mLocationClient = null;
        }
    }
}
