package com.heu.esensedemo;

import android.util.Log;

import com.heu.esensedemo.io.esense.esenselib.ESenseConfig;
import com.heu.esensedemo.io.esense.esenselib.ESenseEventListener;

//ESenseEventListener的实现类
public class ESenseEventListenerImpl implements ESenseEventListener {
    private MainActivity activity;
    public ESenseEventListenerImpl(MainActivity activity){
        this.activity=activity;
    }
    //调用 getBatteryVoltage() 成功的回调
    @Override
    public void onBatteryRead(double voltage) {
        Log.d(Constants.TAG, "------ voltage is " + String.valueOf(voltage) + "------");
    }

    // 当使用registerEventListener()方法监听耳机按钮事件，如果点击了耳机按钮，会回调此方法
    @Override
    public void onButtonEventChanged(boolean pressed) {
        //耳机的按钮点击事件有时传不回来，或者延迟很大,几秒
        Log.d(Constants.TAG, "------ A button event is triggered ------" + (pressed == true));
    }

    // 调用 getAdvertisementAndConnectionInterval() 成功的回调
    @Override
    public void onAdvertisementAndConnectionIntervalRead(int minAdvertisementInterval, int maxAdvertisementInterval, int minConnectionInterval, int maxConnectionInterval) {
        Log.d(Constants.TAG, "------ AdvertisementInterval range is ( " + String.valueOf(minAdvertisementInterval) + "---" + String.valueOf(maxAdvertisementInterval) + ") ------");
        Log.d(Constants.TAG, "------ ConnectionInterval range is ( " + String.valueOf(minConnectionInterval) + "---" + String.valueOf(maxConnectionInterval) + ") ------");
    }

    // 调用 getDeviceName() 成功的回调
    @Override
    public void onDeviceNameRead(String deviceName) {
        Log.d(Constants.TAG, "------ the device name is " + deviceName + " ------");
    }

    //
    @Override
    public void onSensorConfigRead(ESenseConfig config) {
        Log.d(Constants.TAG, "------ read Sensor config ------");
        activity.setConfig(config);
        //manager.setSensorConfig(config);
    }

    @Override
    public void onAccelerometerOffsetRead(int offsetX, int offsetY, int offsetZ) {
        activity.setOffSet(offsetX, offsetY, offsetZ);


        //Log.d(Constants.TAG, "offsetX = " + offsetX / config.getAccSensitivityFactor() + "; offsetY = " + offsetY / config.getAccSensitivityFactor() + "; offsetZ = " + offsetZ / config.getAccSensitivityFactor());
    }
}
