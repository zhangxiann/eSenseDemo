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
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.heu.esensedemo.io.esense.esenselib.app.src.main.java.io.esense.esenselib.ESenseConfig;
import com.heu.esensedemo.io.esense.esenselib.app.src.main.java.io.esense.esenselib.ESenseConnectionListener;
import com.heu.esensedemo.io.esense.esenselib.app.src.main.java.io.esense.esenselib.ESenseEvent;
import com.heu.esensedemo.io.esense.esenselib.app.src.main.java.io.esense.esenselib.ESenseEventListener;
import com.heu.esensedemo.io.esense.esenselib.app.src.main.java.io.esense.esenselib.ESenseManager;
import com.heu.esensedemo.io.esense.esenselib.app.src.main.java.io.esense.esenselib.ESenseSensorListener;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, ESenseConnectionListener, ESenseSensorListener, ESenseEventListener {
    public static final String TAG = "HEU-IOT-eSense";
    public static final int REQUEST_ACCESS_COARSE_LOCATION_PERMISSION = 0x43;
    public static final int REQUEST_WRITE_EXTERNAL_STORAGE = 0x44;


    //left: eSense-0416 ;  right: eSense-1403
    public static final String leftEarbudName = "eSense-0416";
    public static final String rightEarbudName = "eSense-1403";

    private Button leftButton;
    private Button rightButton;
    private Button startRecordButton;
    private Button stopRecordButton;
    private ESenseManager manager;
    private BluetoothAdapter mBluetoothAdapter;
    ESenseConfig config;
    private int index = 0; //记录多少行数据
    private AudioManager audioManager;
    private MediaRecorder mediaRecorder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        leftButton = findViewById(R.id.left_button);
        rightButton = findViewById(R.id.right_button);
        startRecordButton = findViewById(R.id.start_record_button);
        stopRecordButton = findViewById(R.id.stop_record_button);
        leftButton.setOnClickListener(this);
        rightButton.setOnClickListener(this);
        startRecordButton.setOnClickListener(this);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        registerBluetoothReceiver();
        requestPermissions();
        CsvHelper.open();

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.left_button://点击了"扫描左耳机"的按钮
                if (manager!=null && manager.isConnected())
                    manager.disconnect();
                manager = new ESenseManager(leftEarbudName,MainActivity.this.getApplicationContext(), this);
                connect();
                Log.d(TAG, "------ you start to find "+leftEarbudName+" ------");
                break;

            case R.id.right_button://点击了"扫描右耳机"的按钮
                if (manager!=null && manager.isConnected())
                    manager.disconnect();
                manager = new ESenseManager(rightEarbudName,MainActivity.this.getApplicationContext(), this);
                connect();
                Log.d(TAG, "------ you start to find "+rightEarbudName+" ------");
                break;
            case R.id.start_record_button:

                break;

            case R.id.stop_record_button:

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
        //Toast.makeText(this, "------ the eSense earbud is successfully connected ------", Toast.LENGTH_SHORT).show();
        //manager.getSensorConfig();
        //manager.getBatteryVoltage();
        //manager.getAccelerometerOffset();
        //manager.setDeviceName();
        //ESenseConfig config = new ESenseConfig(ESenseConfig.AccRange.G_2, ESenseConfig.GyroRange.DEG_250, ESenseConfig.AccLPF.BW_20, ESenseConfig.GyroLPF.BW_10);
        //manager.setSensorConfig();
        manager.registerSensorListener(this, 100);
        manager.registerEventListener(this);
    }

    @Override
    public void onDisconnected(ESenseManager manager) {
        Log.d(TAG, "------ the eSense earbud is disconnected ------");
    }


    @Override
    public void onSensorChanged(ESenseEvent evt) {
        //evt.convertAccToG(manager.getSensorConfig())
        //manager.get
        Log.d(TAG, "------packet index is :"+ evt.getPacketIndex() +" ------");
        Log.d(TAG, "------ Accel values are :"+ Arrays.toString(evt.getAccel()) +" ------");
        Log.d(TAG, "------ Gyro values are :"+ Arrays.toString(evt.getGyro()) +" ------");
        Log.d(TAG, "------ timesamp is :"+ evt.getTimestamp() +" ------");
        if (index<10){
            CsvHelper.writeCsv(evt.getTimestamp(), evt.getAccel()
            [0], evt.getAccel()[1], evt.getAccel()[2], evt.getGyro()[0], evt.getGyro()[1], evt.getGyro()[2]);
        }
        if (index==10){
            CsvHelper.flush();
            manager.disconnect();
        }

        //evt.convertAccToG(config); //acceleration in g
        //evt.convertGyroToDegPerSecond(config);  rotational speed in degrees/second

        index++;

    }






    @Override
    public void onBatteryRead(double voltage) {
        Log.d(TAG, "------ voltage is "+ String.valueOf(voltage) + "------");
    }

    @Override
    public void onButtonEventChanged(boolean pressed) {
        Log.d(TAG, "------ A button event is triggered ------");
    }

    @Override
    public void onAdvertisementAndConnectionIntervalRead(int minAdvertisementInterval, int maxAdvertisementInterval, int minConnectionInterval, int maxConnectionInterval) {
        Log.d(TAG, "------ AdvertisementInterval range is ( "+ String.valueOf(minAdvertisementInterval) + String.valueOf(maxConnectionInterval) + ") ------");
    }

    @Override
    public void onDeviceNameRead(String deviceName) {
        Log.d(TAG, "------ the device name is "+ deviceName +" ------");
    }

    @Override
    public void onSensorConfigRead(ESenseConfig config) {
        Log.d(TAG, "------ read Sensor config ------");
        this.config = config;
    }

    @Override
    public void onAccelerometerOffsetRead(int offsetX, int offsetY, int offsetZ) {
        Log.d(TAG, "------ read Accelerometer Offset ------");
    }




    /**
     * Android 6.0 动态申请授权定位信息权限
     */
    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

//                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
//                        Manifest.permission.ACCESS_COARSE_LOCATION)) {
//                    Toast.makeText(this, "使用蓝牙需要授权定位信息", Toast.LENGTH_LONG).show();
//                }
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

        manager.disconnect();
        super.onDestroy();
    }


    private void startRecording(){
        String fileName = Environment.getExternalStorageDirectory().getAbsolutePath()+"/record.3gp";
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setOutputFile(fileName);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        try {
            mediaRecorder.prepare();
        } catch (Exception e) {
            // TODO: handle exception
            Log.i(TAG, "prepare() failed!");
        }
        if (!audioManager.isBluetoothScoAvailableOffCall()) {
            Log.i(TAG, "系统不支持蓝牙录音");
            return;
        }
        Log.i(TAG, "系统支持蓝牙录音");
        audioManager.stopBluetoothSco();
        audioManager.startBluetoothSco();
        registerReceiver(new ESenseBroadcastReceiver(), new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED));
    }


    private void stopRecording(){
        mediaRecorder.stop();
        mediaRecorder.release();
        mediaRecorder=null;
        if(audioManager.isBluetoothScoOn()){
            audioManager.setBluetoothScoOn(false);
            audioManager.stopBluetoothSco();
        }
    }




    class ESenseBroadcastReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1);
            if (state == AudioManager.SCO_AUDIO_STATE_CONNECTED){
                Log.i(TAG, "AudioManager.SCO_AUDIO_STATE_CONNECTED");
                audioManager.setBluetoothScoOn(true);
                Log.i(TAG, "Routing:" + audioManager.isBluetoothScoOn());
                audioManager.setMode(AudioManager.STREAM_MUSIC);
                mediaRecorder.start();
                unregisterReceiver(this);
            }else{
                try{
                    Thread.sleep(10000);
                }catch (Exception e){
                    e.printStackTrace();
                }
                audioManager.startBluetoothSco();
                Log.i(TAG, "再次startBluetoothSco()");
            }
        }
    }

}
