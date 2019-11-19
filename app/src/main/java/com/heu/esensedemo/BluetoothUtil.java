package com.heu.esensedemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.util.Log;


//蓝牙工具类
public class BluetoothUtil {

    private String TAG = "BluetoothUtil";

    private static BluetoothUtil mBluetoothUtil;

    //第一次打开sco没成功的情况，持续连接的次数
    private static final int SCO_CONNECT_TIME = 5;
    private int mConnectIndex = 0;

    private AudioManager mAudioManager = null;
    static Context mContext;

    private BluetoothUtil() {
        if (mAudioManager == null) {
            mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        }
    }

    public static BluetoothUtil getInstance(Context context) {
        mContext = context;
        if (mBluetoothUtil == null) {
            mBluetoothUtil = new BluetoothUtil();
        }
        return mBluetoothUtil;
    }

    public void openSco(final IBluetoothConnectListener listener) {
        if (!mAudioManager.isBluetoothScoAvailableOffCall()) {
            Log.e(TAG, "系统不支持蓝牙录音");
            listener.onError("Your device no support bluetooth record!");
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                //mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
                //蓝牙录音的关键，启动SCO连接，耳机话筒才起作用
                mAudioManager.stopBluetoothSco();
                mAudioManager.startBluetoothSco();
                //蓝牙SCO连接建立需要时间，连接建立后会发出ACTION_SCO_AUDIO_STATE_CHANGED消息，通过接收该消息而进入后续逻辑。
                //也有可能此时SCO已经建立，则不会收到上述消息，可以startBluetoothSco()前先
                //stopBluetoothSco()
                mConnectIndex = 0;
                mContext.registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1);
                        boolean bluetoothScoOn = mAudioManager.isBluetoothScoOn();
                        Log.i(TAG, "onReceive state=" + state + ",bluetoothScoOn=" + bluetoothScoOn);
                        if (AudioManager.SCO_AUDIO_STATE_CONNECTED == state) { // 判断值是否是：1
                            Log.e(TAG, "onReceive success!");
                            mAudioManager.setBluetoothScoOn(true);  //打开SCO
                            listener.onSuccess();
                            mContext.unregisterReceiver(this);  //取消广播，别遗漏
                        } else {//等待一秒后再尝试启动SCO
                            Log.e(TAG, "onReceive failed index=" + mConnectIndex);
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            if (mConnectIndex < SCO_CONNECT_TIME) {
                                mAudioManager.startBluetoothSco();//再次尝试连接
                            } else {
                                listener.onError("open sco failed!");
                                mContext.unregisterReceiver(this);  //取消广播，别遗漏
                            }
                            mConnectIndex++;
                        }
                    }
                }, new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED));
            }
        }).start();

    }


    public void closeSco() {
        boolean bluetoothScoOn = mAudioManager.isBluetoothScoOn();
        Log.i(TAG, "bluetoothScoOn=" + bluetoothScoOn);
        if (bluetoothScoOn) {
            mAudioManager.setBluetoothScoOn(false);
            mAudioManager.stopBluetoothSco();
        }
        mBluetoothConnectListener = null;
    }

    public interface IBluetoothConnectListener {
        void onError(String error);

        void onSuccess();
    }

    IBluetoothConnectListener mBluetoothConnectListener;
}
