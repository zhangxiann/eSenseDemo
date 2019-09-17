package com.heu.esensedemo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.Toast;

import com.heu.esensedemo.io.esense.esenselib.ESenseData;
import com.heu.esensedemo.io.esense.esenselib.app.src.main.java.io.esense.esenselib.ESenseConfig;
import com.heu.esensedemo.io.esense.esenselib.app.src.main.java.io.esense.esenselib.ESenseConnectionListener;
import com.heu.esensedemo.io.esense.esenselib.app.src.main.java.io.esense.esenselib.ESenseEvent;
import com.heu.esensedemo.io.esense.esenselib.app.src.main.java.io.esense.esenselib.ESenseEventListener;
import com.heu.esensedemo.io.esense.esenselib.app.src.main.java.io.esense.esenselib.ESenseManager;
import com.heu.esensedemo.io.esense.esenselib.app.src.main.java.io.esense.esenselib.ESenseSensorListener;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Arrays;

//todo onRequestPermissionsResult处理动态权限回调逻辑，在每个按钮事件前添加权限判断，如果没有权限，则动态申请权限
//todo 使用AudioRecord来进行录音，需要自己实现文件流读写等细节，但可以直接保存为wav文件格式，参考如下链接
// https://blog.csdn.net/chezi008/article/details/53064604
// https://github.com/dgutkai/BTRecorder
public class MainActivity extends AppCompatActivity implements View.OnClickListener, ESenseConnectionListener, ESenseSensorListener, ESenseEventListener {
    public static final String TAG = "HEU-IOT-eSense";
    public static final int REQUEST_ACCESS_COARSE_LOCATION_PERMISSION = 0x43;
    public static final int REQUEST_WRITE_EXTERNAL_STORAGE = 0x44;
    public static final int REQUEST_RECORD_AUDIO = 0x45;

    //left: eSense-0416 ;  right: eSense-1403
    public static final String leftEarbudName = "eSense-0416";

    private Button leftButton;
    private Button setButton;
    private Button readSettingButton;
    private Button startRecordButton;
    private Button stopRecordButton;
    private Chronometer timer;
    private ESenseManager manager;
    private BluetoothAdapter mBluetoothAdapter;
    ESenseConfig config;
    private Boolean recordImuData = false;
    private long timesamp;
    private CsvHelper.LooperThread csvThread;
    long startTime;
    long endTime;
    private int offsetX, offsetY, offsetZ;
    private int minAdvertisementInterval;
    private int maxAdvertisementInterval;
    private float time;
    private int sampleingRate = 100;

    private int frequence = 8000; //录制频率，单位hz.
    private int channelConfig = AudioFormat.CHANNEL_IN_MONO;//单声道
    private int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        leftButton = findViewById(R.id.left_button);
        readSettingButton = findViewById(R.id.read_settin_button);
        setButton = findViewById(R.id.set_button);
        startRecordButton = findViewById(R.id.start_record_button);
        stopRecordButton = findViewById(R.id.stop_record_button);
        timer = (Chronometer) findViewById(R.id.timer);
        leftButton.setOnClickListener(this);
        readSettingButton.setOnClickListener(this);
        setButton.setOnClickListener(this);
        startRecordButton.setOnClickListener(this);
        stopRecordButton.setOnClickListener(this);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        registerBluetoothReceiver();
        requestPermissions();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.left_button://点击了"扫描左耳机"的按钮
                if (null != manager && manager.isConnected())
                    manager.disconnect();
                manager = new ESenseManager(leftEarbudName, MainActivity.this.getApplicationContext(), this);
                connect();
                Log.d(TAG, "------ you start to find " + leftEarbudName + " ------");
                break;
            case R.id.read_settin_button:

                boolean test1 = manager.getSensorConfig();
                Log.d(TAG, "getSensorConfig = " + test1);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                boolean test = manager.getAdvertisementAndConnectionInterval();
                Log.d(TAG, "getAdvertisementAndConnectionInterval = " + test);
                try {
                    Thread.sleep(800);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                manager.getAccelerometerOffset();
                break;
            case R.id.set_button:
                boolean result = manager.setAdvertisementAndConnectiontInterval(1000, 2000, 20, 40);
                Log.d(TAG, "setAdvertisementAndConnectiontInterval = " + result);
                break;

            case R.id.start_record_button:
                timesamp = System.currentTimeMillis();
                CsvHelper.open(timesamp);
                csvThread = new CsvHelper.LooperThread();
                csvThread.start();
                BluetoothUtil.getInstance(this).openSco(new BluetoothUtil.IBluetoothConnectListener() {
                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "openSco onError  error=" + error);
                        Toast.makeText(MainActivity.this, "error：" + error, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onSuccess() {
                        //这里启动录制任务
                        Toast.makeText(MainActivity.this, "开始蓝牙录音", Toast.LENGTH_SHORT).show();
                        startRecording();
                    }
                });
                break;

            case R.id.stop_record_button:
                endTime = System.currentTimeMillis();
                time = (endTime - startTime) / 1000.0f;
                stopRecording();
                timer.stop();
                break;

            default:
                break;
        }
    }

    @Override
    public void onDeviceFound(com.heu.esensedemo.io.esense.esenselib.app.src.main.java.io.esense.esenselib.ESenseManager manager) {
        Log.d(TAG, "------ the eSense earbud is found ------");
        //Toast.makeText(this, "------ the eSense earbud is found ------", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDeviceNotFound(com.heu.esensedemo.io.esense.esenselib.app.src.main.java.io.esense.esenselib.ESenseManager manager) {
        Log.d(TAG, "------ the eSense earbud is not found ------");
    }

    @Override
    public void onConnected(com.heu.esensedemo.io.esense.esenselib.app.src.main.java.io.esense.esenselib.ESenseManager manager) {
        Log.d(TAG, "------ the eSense earbud is successfully connected ------");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "------ the eSense earbud is successfully connected ------", Toast.LENGTH_SHORT).show();

            }
        });
//        Looper.prepare();
//        Looper.loop();
        //manager.getSensorConfig();
        //manager.getBatteryVoltage();
        //manager.getAccelerometerOffset();
        //manager.setDeviceName();
        //ESenseConfig config = new ESenseConfig(ESenseConfig.AccRange.G_2, ESenseConfig.GyroRange.DEG_250, ESenseConfig.AccLPF.BW_20, ESenseConfig.GyroLPF.BW_10);
        //manager.setSensorConfig();
        manager.registerSensorListener(this, sampleingRate);
        manager.registerEventListener(this);
        //manager.getSensorConfig();
        unregisterReceiver(bluetoothReceiver);
    }

    @Override
    public void onDisconnected(ESenseManager manager) {
        Log.d(TAG, "------ the eSense earbud is disconnected ------");
    }


    @Override
    public void onSensorChanged(ESenseEvent evt) {
        //evt.convertAccToG(manager.getSensorConfig())
        //manager.get
//        Log.d(TAG, "------packet index is :" + evt.getPacketIndex() + " ------");
        //Log.d(TAG, "------ Accel values are :" + Arrays.toString(evt.getAccel()) + " ------");
        // Log.d(TAG, "------ Gyro values are :" + Arrays.toString(evt.getGyro()) + " ------");
        //Log.d(TAG, "------ timesamp is :" + evt.getTimestamp() + " ------");
        if (recordImuData) {
            Message msg = Message.obtain();
            Bundle data = new Bundle();
            data.putParcelable("ESENSE_DATA", new ESenseData(evt.getTimestamp(), evt.convertAccToG(config, new int[]{offsetX, offsetY, offsetZ}), evt.convertGyroToDegPerSecond(config)));
            msg.setData(data);
            msg.what = 1;
            csvThread.handler.sendMessage(msg);
//            CsvHelper.writeCsv(evt.getTimestamp(), evt.convertAccToG(config)
//                    [0], evt.convertAccToG(config)[1], evt.convertAccToG(config)[2], evt.convertAccToG(config)[0], evt.convertAccToG(config)[1], evt.convertAccToG(config)[2]);
        }

        //evt.convertAccToG(config); //acceleration in g
        //evt.convertGyroToDegPerSecond(config);  rotational speed in degrees/second
    }


    @Override
    public void onBatteryRead(double voltage) {
        Log.d(TAG, "------ voltage is " + String.valueOf(voltage) + "------");
    }

    @Override
    public void onButtonEventChanged(boolean pressed) {
        //耳机的点击事件有时传不回来，或者延迟很大,几秒
        Log.d(TAG, "------ A button event is triggered ------" + (pressed == true));
    }

    @Override
    public void onAdvertisementAndConnectionIntervalRead(int minAdvertisementInterval, int maxAdvertisementInterval, int minConnectionInterval, int maxConnectionInterval) {
        Log.d(TAG, "------ AdvertisementInterval range is ( " + String.valueOf(minAdvertisementInterval) + "---" + String.valueOf(maxAdvertisementInterval) + ") ------");
        Log.d(TAG, "------ ConnectionInterval range is ( " + String.valueOf(minConnectionInterval) + "---" + String.valueOf(maxConnectionInterval) + ") ------");
        this.minAdvertisementInterval = minAdvertisementInterval;
        this.maxAdvertisementInterval = maxAdvertisementInterval;
        //manager.setAdvertisementAndConnectiontInterval(minAdvertisementInterval, maxAdvertisementInterval, 20, 100);
    }

    @Override
    public void onDeviceNameRead(String deviceName) {
        Log.d(TAG, "------ the device name is " + deviceName + " ------");
    }

    @Override
    public void onSensorConfigRead(ESenseConfig config) {
        Log.d(TAG, "------ read Sensor config ------");
        this.config = config;

        //manager.setSensorConfig(config);
    }

    @Override
    public void onAccelerometerOffsetRead(int offsetX, int offsetY, int offsetZ) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;

        Log.d(TAG, "offsetX = " + offsetX / config.getAccSensitivityFactor() + "; offsetY = " + offsetY / config.getAccSensitivityFactor() + "; offsetZ = " + offsetZ / config.getAccSensitivityFactor());
    }

    /**
     * Android 6.0 动态申请授权定位信息权限
     */
    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                //请求权限
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        REQUEST_ACCESS_COARSE_LOCATION_PERMISSION);
            }

            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                //请求权限
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_WRITE_EXTERNAL_STORAGE);
            }

            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

                //请求权限
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        REQUEST_RECORD_AUDIO);
            }

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_ACCESS_COARSE_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //用户授权
            } else {
                finish();
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    //开启蓝牙
    private void connect() {
        if (mBluetoothAdapter.isEnabled()) {
            manager.connect(10000);
        } else {
            mBluetoothAdapter.enable();
        }
    }

    //注册监听蓝牙状态变化广播
    private void registerBluetoothReceiver() {
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothReceiver, filter);
    }

    BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                int state = mBluetoothAdapter.getState();
                if (state == BluetoothAdapter.STATE_ON) {
                    manager.connect(10000);
                }
            }
        }
    };

    @Override
    protected void onDestroy() {
        manager.unregisterEventListener();
        manager.unregisterSensorListener();
        if (manager.isConnected())
            manager.disconnect();
        super.onDestroy();
    }


    private void startRecording() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                recordTask();
            }
        }).start();

    }

    private AudioDeviceInfo findAudioDevice(int deviceFlag, int deviceType) {
        AudioManager manager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        AudioDeviceInfo[] adis = manager.getDevices(deviceFlag);
        Log.i(TAG, "findAudioDevice: adis = " + adis);
        for (AudioDeviceInfo adi : adis) {
            Log.i(TAG, "findAudioDevice: adi.getType() = " + adi.getType());
            if (adi.getType() == deviceType) {
                return adi;
            }
        }
        return null;
    }


    //录音线程
    private void recordTask() {
        Log.i(TAG, "RecordTask2: AsyncTask");

        try {
            final String fileName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/ESenseData/" + timesamp + "voice.pcm";
            File file = new File(fileName);
            //开通输出流到指定的文件
            DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
            //根据定义好的几个配置，来获取合适的缓冲大小
            final int bufferSize = AudioRecord.getMinBufferSize(frequence, channelConfig, audioEncoding);
//                int bufferSize = 640;
            Log.i(TAG, "RecordTask: dataSize=" + bufferSize);//1280
            //实例化AudioRecord//MediaRecorder.AudioSource.VOICE_COMMUNICATION
//            AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, frequence, channelConfig, audioEncoding, bufferSize);
//            AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, frequence, channelConfig, audioEncoding, bufferSize);
            AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, frequence, channelConfig, audioEncoding, bufferSize);
            AudioDeviceInfo audioDevice = findAudioDevice(AudioManager.GET_DEVICES_INPUTS, AudioDeviceInfo.TYPE_BLUETOOTH_SCO);
            if (null != audioDevice) {
                Log.i(TAG, "RecordTask: audioDevice = " + audioDevice.getType());
                record.setPreferredDevice(audioDevice);
            }

            //开始录制
            record.startRecording();
            recordImuData = true;
            startTime = System.currentTimeMillis();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    timer.setBase(SystemClock.elapsedRealtime());//计时器清零
                    timer.start();
                }
            });
            if (record.getPreferredDevice() != null) {
                Log.i(TAG, "RecordTask2: getPreferredDevice = " + record.getPreferredDevice().getType());
            }
            Log.i(TAG, "RecordTask2: getRoutedDevice = " + record.getRoutedDevice().getType());

            byte audioData[] = new byte[bufferSize];

            int r = 0; //存储录制进度
            //定义循环，根据isRecording的值来判断是否继续录制
            long beforeTime = 0;
            while (recordImuData) {
                //从bufferSize中读取字节，返回读取的short个数
                int number = record.read(audioData, 0, bufferSize);
                dos.write(audioData, 0, number);
                //Log.i(TAG, "audioData number=" + number);
//                    System.out.println(Arrays.toString(audioData));

                r++; //自增进度值
//                    long curTime = System.currentTimeMillis();
//                    Log.i(TAG, "r=" + r + "   time over:" + (curTime - beforeTime)); //循环一次的时间是40毫秒，获取1280个自己音频数据
//                    beforeTime = curTime;
            }
            //录制结束
            record.stop();
            record.release();
            BluetoothUtil.getInstance(this).closeSco();
            dos.flush();
            dos.close();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            final String wavFileName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/ESenseData/" + timesamp + "voice.wav";

            new Thread(new Runnable() {
                @Override
                public void run() {
                    WavUtils.convertWaveFile(fileName, wavFileName, frequence, bufferSize);
                }
            }).start();

            Log.i(TAG, "RecordTask2: over");
            Log.v(TAG, "The DOS available:" + file.getAbsolutePath());
            Log.v(TAG, "The DOS available:" + file.length());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void stopRecording() {


        Message msg = Message.obtain();
        msg.what = 0;
        msg.obj = time;
        msg.arg1 = sampleingRate;
        csvThread.handler.sendMessage(msg);
        recordImuData = false;
    }

}
