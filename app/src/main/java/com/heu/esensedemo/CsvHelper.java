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

    private static String mAccelFileName = null;
    private static String mGyroFileName = null;
    private static BufferedOutputStream accelOutputStream;
    private static BufferedOutputStream gyroOutputStream;


    public static void open(long timesamp) {
        String folderName = null;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            String path = Environment.getExternalStorageDirectory().getAbsolutePath();
            if (path != null) {
                folderName = path + "/ESenseData/";
            }
        }

        File fileRobo = new File(folderName);
        if (!fileRobo.exists()) {
            fileRobo.mkdir();
        }
        mAccelFileName = folderName + timesamp + "eSense-Accel.csv";
        mGyroFileName = folderName + timesamp + "eSense-Gyro.csv";
        try {
            accelOutputStream = new BufferedOutputStream(new FileOutputStream(
                    new File(mAccelFileName)));
            gyroOutputStream = new BufferedOutputStream(new FileOutputStream(
                    new File(mGyroFileName)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
//        mAccelStringBuilder.append("timesamp");
//        mAccelStringBuilder.append(mComma);
//        mAccelStringBuilder.append("x");
//        mAccelStringBuilder.append(mComma);
//        mAccelStringBuilder.append("y");
//        mAccelStringBuilder.append(mComma);
//        mAccelStringBuilder.append("z");
//        mAccelStringBuilder.append("\n");
//
//        mGyroStringBuilder = new StringBuilder(10241024);
//        mGyroStringBuilder.append("timesamp");
//        mGyroStringBuilder.append(mComma);
//        mGyroStringBuilder.append("x");
//        mGyroStringBuilder.append(mComma);
//        mGyroStringBuilder.append("y");
//        mGyroStringBuilder.append(mComma);
//        mGyroStringBuilder.append("z");
//        mGyroStringBuilder.append("\n");

    }


    public static void writeCsv(long timesamp, double accel_x, double accel_y, double accel_z, double gyro_x, double gyro_y, double gyro_z) {
        StringBuilder mAccelStringBuilder = new StringBuilder();
        StringBuilder mGyroStringBuilder = new StringBuilder();
        mAccelStringBuilder = new StringBuilder();
        mAccelStringBuilder.append(timesamp);
        mAccelStringBuilder.append(mComma);
        mAccelStringBuilder.append(accel_x);
        mAccelStringBuilder.append(mComma);
        mAccelStringBuilder.append(accel_y);
        mAccelStringBuilder.append(mComma);
        mAccelStringBuilder.append(accel_z);
        mAccelStringBuilder.append("\n");
        try {
            accelOutputStream.write(mAccelStringBuilder.toString().getBytes());

        } catch (IOException e) {
            e.printStackTrace();
        }

        mGyroStringBuilder.append(timesamp);
        mGyroStringBuilder.append(mComma);
        mGyroStringBuilder.append(gyro_x);
        mGyroStringBuilder.append(mComma);
        mGyroStringBuilder.append(gyro_y);
        mGyroStringBuilder.append(mComma);
        mGyroStringBuilder.append(gyro_z);
        mGyroStringBuilder.append("\n");
        try {
            gyroOutputStream.write(mGyroStringBuilder.toString().getBytes());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void flush() {
        if (accelOutputStream != null) {
            try {
                accelOutputStream.flush();
                accelOutputStream.close();
                accelOutputStream = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (gyroOutputStream != null) {
            try {
                gyroOutputStream.flush();
                gyroOutputStream.close();
                gyroOutputStream = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static final class LooperThread extends Thread {

        public Handler handler;
        //private int i = 0;

        @Override
        public void run() {
            super.run();

            Looper.prepare();

            handler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    //i = i + 1;
                    //Log.d(TAG, String.valueOf(i));
                    super.handleMessage(msg);
                    switch (msg.what) {
                        case 0:
                            Log.d(TAG, "写入完成！");
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
