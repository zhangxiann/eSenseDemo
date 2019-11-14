package com.heu.esensedemo;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.heu.esensedemo.io.esense.esenselib.ESenseData;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 操作CSV文件的工具类
 */
public class CsvHelper {

    public static final String mComma = ",";
    public static final String TAG = "HEU-IOT-eSense";

    private static String mImuFileName = null;
    private static BufferedOutputStream mImuOutputStream;
    private int i=0;

    public static void open(String userStr, String voiceStr) {
        String folderName = null;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            String path = Environment.getExternalStorageDirectory().getAbsolutePath();
            if (path != null) {
                folderName = path + "/ESenseData1/"+voiceStr+"/";
            }
        }

        File fileRobo = new File(folderName);
        if (!fileRobo.exists()) {
            fileRobo.mkdirs();
        }
        mImuFileName = folderName + userStr + ".csv";
        File csvFile = new File(mImuFileName);
        if (csvFile.exists() && csvFile.isFile()){
            csvFile.delete();
        }
        try {
            mImuOutputStream = new BufferedOutputStream(new FileOutputStream(csvFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }


    public static void writeCsv(long timesamp, double accel_x, double accel_y, double accel_z, double gyro_x, double gyro_y, double gyro_z) {
        StringBuilder mImuStringBuilder = new StringBuilder();
        mImuStringBuilder = new StringBuilder();
        mImuStringBuilder.append(timesamp);
        mImuStringBuilder.append(mComma);
        mImuStringBuilder.append(accel_x);
        mImuStringBuilder.append(mComma);
        mImuStringBuilder.append(accel_y);
        mImuStringBuilder.append(mComma);
        mImuStringBuilder.append(accel_z);
        mImuStringBuilder.append(mComma);

        mImuStringBuilder.append(gyro_x);
        mImuStringBuilder.append(mComma);
        mImuStringBuilder.append(gyro_y);
        mImuStringBuilder.append(mComma);
        mImuStringBuilder.append(gyro_z);
        mImuStringBuilder.append("\n");
        try {
            mImuOutputStream.write(mImuStringBuilder.toString().getBytes());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void flush() {
        if (mImuOutputStream != null) {
            try {
                mImuOutputStream.flush();
                mImuOutputStream.close();
                mImuOutputStream = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static final class LooperThread extends Thread {
        int i=0;
        public Handler handler;
        //private int i = 0;

        @Override
        public void run() {
            super.run();

            Looper.prepare();

            handler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    i = i + 1;
                    //Log.d(TAG, String.valueOf(i));
                    super.handleMessage(msg);
                    switch (msg.what) {
                        case 0:
                            float time = (float) msg.obj;
                            Log.d(TAG, "计时时间 = " + time +"ms; "+" imu 数据采集次数 =  "+i+"; 设置的采样频率 = " + msg.arg1 +"Hz; 实际采集频率 = "+ i/time+"Hz");

                            flush();
                            break;

                        case 1:
                            Bundle bundle = msg.getData();
                            ESenseData data = bundle.getParcelable("ESENSE_DATA");
                            writeCsv(data.getTimestamp(), data.accel[0], data.accel[1], data.accel[2], data.gyro[0], data.gyro[1], data.gyro[2]);
                            //Log.d(TAG, data.accel[0]+"--"+data.gyro[0]);
                            break;
                    }
                }
            };
            Looper.loop();//loop()会调用到handler的handleMessage(Message msg)方法，所以，写在下面；
        }
    }
}
