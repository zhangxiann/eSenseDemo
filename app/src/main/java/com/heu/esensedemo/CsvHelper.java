package com.heu.esensedemo;

import android.os.Environment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 操作CSV文件的工具类
 */
//todo StringBuilder有可能内存溢出，优化一下，每多少条数据保存一次, 考虑多线程读写IO
public class CsvHelper {

    public static final String mComma = ",";
    private static StringBuilder mAccelStringBuilder = null;
    private static StringBuilder mGyroStringBuilder = null;
    private static String mAccelFileName = null;
    private static String mGyroFileName = null;

    public static void open() {
        String folderName = null;
        if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
            String path = Environment.getExternalStorageDirectory().getAbsolutePath();
            if (path != null) {
                folderName = path +"/CSV/";
            }
        }

        File fileRobo = new File(folderName);
        if(!fileRobo.exists()){
            fileRobo.mkdir();
        }
        mAccelFileName = folderName + "eSense-Accel.csv";
        mGyroFileName = folderName + "eSense-Gyro.csv";
        mAccelStringBuilder = new StringBuilder(10241024);
        mAccelStringBuilder.append("timesamp");
        mAccelStringBuilder.append(mComma);
        mAccelStringBuilder.append("x");
        mAccelStringBuilder.append(mComma);
        mAccelStringBuilder.append("y");
        mAccelStringBuilder.append(mComma);
        mAccelStringBuilder.append("z");
        mAccelStringBuilder.append("\n");

        mGyroStringBuilder = new StringBuilder(10241024);
        mGyroStringBuilder.append("timesamp");
        mGyroStringBuilder.append(mComma);
        mGyroStringBuilder.append("x");
        mGyroStringBuilder.append(mComma);
        mGyroStringBuilder.append("y");
        mGyroStringBuilder.append(mComma);
        mGyroStringBuilder.append("z");
        mGyroStringBuilder.append("\n");
        
        
    }


    public static void writeCsv(long timesamp, int accel_x, int accel_y, int accel_z, int gyro_x, int gyro_y, int gyro_z) {
        mAccelStringBuilder.append(timesamp);
        mAccelStringBuilder.append(mComma);
        mAccelStringBuilder.append(accel_x);
        mAccelStringBuilder.append(mComma);
        mAccelStringBuilder.append(accel_y);
        mAccelStringBuilder.append(mComma);
        mAccelStringBuilder.append(accel_z);
        mAccelStringBuilder.append("\n");


        mGyroStringBuilder.append(timesamp);
        mGyroStringBuilder.append(mComma);
        mGyroStringBuilder.append(gyro_x);
        mGyroStringBuilder.append(mComma);
        mGyroStringBuilder.append(gyro_y);
        mGyroStringBuilder.append(mComma);
        mGyroStringBuilder.append(gyro_z);
        mGyroStringBuilder.append("\n");

    }


    public static void flush() {
        
            try {
                if (mAccelFileName != null) {
                File file = new File(mAccelFileName);
                FileOutputStream fos = new FileOutputStream(file, true);
                fos.write(mAccelStringBuilder.toString().getBytes());//如果是追加保存，使用offset参数
                fos.flush();
                fos.close();
                } else {
                    throw new RuntimeException("You should call open() before flush()");
                }

                if (mGyroFileName != null) {
                    File file = new File(mGyroFileName);
                    FileOutputStream fos = new FileOutputStream(file, true);
                    fos.write(mGyroStringBuilder.toString().getBytes());
                    fos.flush();
                    fos.close();
                } else {
                    throw new RuntimeException("You should call open() before flush()");
                }
                
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
    }
}
