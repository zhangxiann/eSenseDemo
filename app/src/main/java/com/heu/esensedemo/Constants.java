package com.heu.esensedemo;

import android.media.AudioFormat;

public class Constants {
    //这个类存放的都是静态变量，使用私有构造器避免类初始化
    private Constants(){

    }

    public static final String TAG = "HEU-IOT-eSense";
    //动态权限 request code
    public static final int REQUEST_ACCESS_COARSE_LOCATION_PERMISSION = 0x43;
    public static final int REQUEST_WRITE_EXTERNAL_STORAGE = 0x44;
    public static final int REQUEST_RECORD_AUDIO = 0x45;

    //left: eSense-0416 ;  right: eSense-1403
    // 关于耳机的ID 可使用nRF Connect(一个APP)，只有使用对应耳机的id，才能连接上耳机的BLE（详情请访问 esense.io 查阅官方文档）。
    // 左耳机是带有IMU传感器的，使用BLE(低功耗蓝牙)，右耳是采集麦克风数据的(使用传统蓝牙)，因此这里连接左耳机即可。
    // 实际使用 先打开手机蓝牙连接右耳机1403，然后使用本APP连接左耳机。按照这个顺序即可使用右耳机采集麦克风数据，而左耳机采集IMU数据。
    // 否则左耳机可能会同时采集IMU和麦克风，这样会导致IMU采集速率远比设置值低。
    public static final String leftEarbudName = "eSense-0416";

    // 设置数据文件存放的目录名
    public static final String dirname="/ESenseData1/";

    // 设置 IMU 采样频率
    public static final int sampleingRate = 100;

    //handler 消息type: 停止采集数据
    public static final int MSG_TYPE_STOP=0;

    //handler 消息type: 接收数据，写进文件
    public static final int MSG_TYPE_RECEIVE_DATA=1;

    public static final int frequence = 8000; //声音录制频率，单位hz.
    public static final int channelConfig = AudioFormat.CHANNEL_IN_MONO;//单声道
    public static final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;//保存格式

    //是否录制数据
    public static boolean recordImuData= false;





}
