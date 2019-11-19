package com.heu.esensedemo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.Spinner;
import android.widget.Toast;

import com.heu.esensedemo.io.esense.esenselib.ESenseData;
import com.heu.esensedemo.io.esense.esenselib.ESenseConfig;
import com.heu.esensedemo.io.esense.esenselib.ESenseConnectionListener;
import com.heu.esensedemo.io.esense.esenselib.ESenseEvent;
import com.heu.esensedemo.io.esense.esenselib.ESenseManager;
import com.heu.esensedemo.io.esense.esenselib.ESenseSensorListener;

import java.util.ArrayList;
import java.util.List;

//todo onRequestPermissionsResult处理动态权限回调逻辑，在每个按钮事件前添加权限判断，如果没有权限，则动态申请权限

// 使用AudioRecord来进行录音，需要自己实现文件流读写等细节，但可以直接保存为wav文件格式，参考如下链接
// https://blog.csdn.net/chezi008/article/details/53064604
// https://github.com/dgutkai/BTRecorder
public class MainActivity extends AppCompatActivity implements View.OnClickListener, ESenseConnectionListener, ESenseSensorListener {


    private Spinner userSpinner;
    private Spinner voiceSpinner;
    private ArrayAdapter<String> userListAdapter;
    private ArrayAdapter<String> voiceListAdapter;

    private List<String> userList;
    private List<String> voiceList;

    private String userType;//记录选择的用户
    private String voiceType;//记录选择的录音类型

    private Button leftButton;
    private Button readSettingButton;
    private Button startRecordButton;
    private Button stopRecordButton;
    private Chronometer timer;
    private ESenseManager manager;
    private BluetoothAdapter mBluetoothAdapter;
    private ESenseConfig config;
    private CsvUtil.LooperThread csvThread;
    private long startTime;//记录数据采集开始时间
    private int offsetX, offsetY, offsetZ;//


    private ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        leftButton = findViewById(R.id.left_button);
        readSettingButton = findViewById(R.id.read_settin_button);
        startRecordButton = findViewById(R.id.start_record_button);
        stopRecordButton = findViewById(R.id.stop_record_button);
        timer = (Chronometer) findViewById(R.id.timer);
        userSpinner = (Spinner) findViewById(R.id.user_type);
        voiceSpinner = (Spinner) findViewById(R.id.voice_type);

        userList = new ArrayList<>();
        voiceList = new ArrayList<>();

        userList.add(getString(R.string.user1));
        userList.add(getString(R.string.user2));
        userList.add(getString(R.string.user3));
        userList.add(getString(R.string.user4));
        userList.add(getString(R.string.user5));
        userList.add(getString(R.string.user6));
        userList.add(getString(R.string.user7));
        userList.add(getString(R.string.user8));
        userList.add(getString(R.string.user9));
        userList.add(getString(R.string.user10));

        voiceList.add(getString(R.string.Hey_Siri));
        voiceList.add(getString(R.string.OK_Google));
        voiceList.add(getString(R.string.Alexa));

        //设置选择用户下拉菜单
        userListAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, userList);
        //设置选择录音下拉菜单
        voiceListAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, voiceList);

        userListAdapter.setDropDownViewResource(android.R.layout.simple_list_item_1);
        voiceListAdapter.setDropDownViewResource(android.R.layout.simple_list_item_1);

        userSpinner.setAdapter(userListAdapter);
        voiceSpinner.setAdapter(voiceListAdapter);

        userSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                userType = parent.getItemAtPosition(position).toString();
                Log.d("user type:", userType);

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Log.d("user type:", userType);

            }
        });

        voiceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                voiceType = parent.getItemAtPosition(position).toString();
                Log.e("voice type", voiceType);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Log.e("voice type", voiceType);
            }
        });

        userSpinner.setEnabled(true);
        voiceSpinner.setEnabled(true);


        leftButton.setOnClickListener(this);
        readSettingButton.setOnClickListener(this);
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
                showDialog();
                manager = new ESenseManager(Constants.leftEarbudName, MainActivity.this.getApplicationContext(), this);
                connect();
                Log.d(Constants.TAG, "------ you start to find " + Constants.leftEarbudName + " ------");
                break;
            case R.id.read_settin_button://读取配置信息，开始记录数据前，务必读取配置信息保存到 config 对象，用于对后面采集到的IMU数据进行转换
                readSetting();
                break;

            case R.id.start_record_button:
                if (null == this.config) {
                    Toast.makeText(MainActivity.this, "请先点击\"读取和设置配置信息\"", Toast.LENGTH_SHORT).show();
                    return;
                }
                CsvUtil.open(userType, voiceType);
                csvThread = new CsvUtil.LooperThread();
                csvThread.start();
                BluetoothUtil.getInstance(this).openSco(new BluetoothUtil.IBluetoothConnectListener() {
                    @Override
                    public void onError(String error) {
                        Log.e(Constants.TAG, "openSco onError  error=" + error);
                        Toast.makeText(MainActivity.this, "打开蓝牙录音失败，是不是没连接右耳机？", Toast.LENGTH_LONG).show();
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
                long endTime = System.currentTimeMillis();
                float time = (endTime - startTime) / 1000.0f;//计算采集时间
                stopRecording(time);
                timer.stop();
                break;

            default:
                break;
        }
    }

    private void showDialog() {
        if (null == dialog) {
            dialog = new ProgressDialog(this);
            dialog.setCancelable(false);
        }
        dialog.show();
    }

    //读取耳机配置信息
    private void readSetting() {
        dialog.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean test1 = manager.getSensorConfig();//读取配置信息，开始记录数据前，务必读取配置信息保存到 config 对象，用于对后面采集到的IMU数据进行转换
                Log.d(Constants.TAG, "getSensorConfig = " + test1);
/*                //暂停400ms再调用manager.getAdvertisementAndConnectionInterval()，否则可能会get失败。
                // 因为需要等待上一次 get 操作返回数据，才能和 eSense 进行下一次数据交换，否则下面的 get 可能会失败。
                // 详情请访问 esense.io 查阅官方文档
                try {
                    Thread.sleep(400);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                boolean test = manager.getAdvertisementAndConnectionInterval();//可删除
                Log.d(Constants.TAG, "getAdvertisementAndConnectionInterval = " + test);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (null != dialog && dialog.isShowing()) {
                            dialog.dismiss();
                        }

                    }
                });*/
            }
        }).start();
    }

    //调用 connect() 方法连接左耳机失败的回调
    @Override
    public void onDeviceFound(ESenseManager manager) {
        Log.d(Constants.TAG, "------ the eSense earbud is found ------");
        //Toast.makeText(this, "------ the eSense earbud is found ------", Toast.LENGTH_SHORT).show();
    }

    //调用 connect() 方法连接左耳机成功的回调
    @Override
    public void onDeviceNotFound(ESenseManager manager) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (null != dialog && dialog.isShowing()) {
                    dialog.dismiss();
                }
                Toast.makeText(MainActivity.this, "没有扫描到左耳机", Toast.LENGTH_SHORT).show();
            }
        });
        Log.d(Constants.TAG, "------ the eSense earbud is not found ------");
    }


    //调用 connect() 方法连接左耳机成功的回调
    @Override
    public void onConnected(ESenseManager manager) {
        Log.d(Constants.TAG, "------ the eSense earbud is successfully connected ------");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (null != dialog && dialog.isShowing()) {
                    dialog.dismiss();
                }
                Toast.makeText(MainActivity.this, "成功连接到左耳机", Toast.LENGTH_SHORT).show();
            }
        });

        manager.registerSensorListener(this, Constants.sampleingRate);//设置监听器监听IMU数据的回调

        manager.registerEventListener(new ESenseEventListenerImpl(this));//设置其他方法的回调

        //manager.getSensorConfig();
        unregisterReceiver(bluetoothReceiver);
    }

    //调用 registerSensorListener()方法成功后，耳机断开连接时，会自动触发此回调
    @Override
    public void onDisconnected(ESenseManager manager) {
        Log.d(Constants.TAG, "------ the eSense earbud is disconnected ------");
    }

    //调用 registerSensorListener()方法成功后，会根据设置的采样率回调此方法。
    @Override
    public void onSensorChanged(ESenseEvent evt) {

        //IO读写很慢，如果直接把IMU的数据写进文件，会有数据丢失。因此使用子线程来读写数据，使用handler把数据发给子线程写进文件
        if (Constants.recordImuData) {
            Message msg = Message.obtain();
            Bundle data = new Bundle();
            data.putParcelable("ESENSE_DATA", new ESenseData(evt.getTimestamp(), evt.convertAccToG(config, new int[]{offsetX, offsetY, offsetZ}), evt.convertGyroToDegPerSecond(config)));
            msg.setData(data);
            msg.what = Constants.MSG_TYPE_RECEIVE_DATA;
            csvThread.handler.sendMessage(msg);
        }


    }

    /**
     * Android 6.0以上 动态申请授权信息权限
     */
    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // BLE连接 需要申请定位权限
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                //请求权限
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        Constants.REQUEST_ACCESS_COARSE_LOCATION_PERMISSION);
            }

            // 保存数据到CSV 需要申请写权限
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                //请求权限
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        Constants.REQUEST_WRITE_EXTERNAL_STORAGE);
            }

            // 录音需要 需要申请录音权限
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

                //请求权限
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        Constants.REQUEST_RECORD_AUDIO);
            }

        }
    }


    //todo
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == Constants.REQUEST_ACCESS_COARSE_LOCATION_PERMISSION) {
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


    //点击 开始 按钮触发
    private void startRecording() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                RecordUtil.recordTask(MainActivity.this, voiceType, userType);
            }
        }).start();

    }

    private void stopRecording(float time) {
        Message msg = Message.obtain();
        msg.what = Constants.MSG_TYPE_STOP;
        msg.obj = time;
        msg.arg1 = Constants.sampleingRate;
        csvThread.handler.sendMessage(msg);
        Constants.recordImuData = false;
    }

    public void setConfig(ESenseConfig config) {
        this.config = config;
        boolean result = manager.setAdvertisementAndConnectiontInterval(1000, 2000, 20, 40);
        Log.d(Constants.TAG, "setAdvertisementAndConnectiontInterval = " + result);
        if (result) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    if (null != dialog && dialog.isShowing()) {
                        dialog.dismiss();
                    }
                    Toast.makeText(MainActivity.this, "读取和设置配置信息成功", Toast.LENGTH_SHORT).show();
                }
            });
        } else
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "读取和设置配置信息失败", Toast.LENGTH_SHORT).show();
                }
            });

    }

    public void setOffSet(int offsetX, int offsetY, int offsetZ) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
    }


    public void notifyRecording() {

        startTime = System.currentTimeMillis();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                timer.setBase(SystemClock.elapsedRealtime());//计时器清零
                timer.start();
            }
        });
    }
}
