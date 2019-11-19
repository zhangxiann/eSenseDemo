package com.heu.esensedemo;

import android.content.Context;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.SystemClock;
import android.os.Trace;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;

//数据录制工具类
public class RecordUtil {



    //这个类存放的都是静态变量，使用私有构造器避免类初始化
    private RecordUtil(){

    }

    public static AudioDeviceInfo findAudioDevice(Context context,int deviceFlag, int deviceType) {
        AudioManager manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        AudioDeviceInfo[] adis = manager.getDevices(deviceFlag);
        Log.i(Constants.TAG, "findAudioDevice: adis = " + adis);
        for (AudioDeviceInfo adi : adis) {
            Log.i(Constants.TAG, "findAudioDevice: adi.getType() = " + adi.getType());
            if (adi.getType() == deviceType) {
                return adi;
            }
        }
        return null;
    }

    //录音线程
    public static void recordTask(MainActivity activity, String voiceType, String userType) {
        Log.i(Constants.TAG, "RecordTask2: AsyncTask");

        try {
            final String fileName = Environment.getExternalStorageDirectory().getAbsolutePath() + Constants.dirname + voiceType + "/" + userType + ".pcm";
            File file = new File(fileName);
            if (file.exists() && file.isFile()){
                file.delete();
            }
            //开通输出流到指定的文件
            DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
            //根据定义好的几个配置，来获取合适的缓冲大小
            final int bufferSize = AudioRecord.getMinBufferSize(Constants.frequence, Constants.channelConfig, Constants.audioEncoding);
//                int bufferSize = 640;
            Log.i(Constants.TAG, "RecordTask: dataSize=" + bufferSize);//1280
            //实例化AudioRecord//MediaRecorder.AudioSource.VOICE_COMMUNICATION
//            AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, frequence, channelConfig, audioEncoding, bufferSize);
//            AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, frequence, channelConfig, audioEncoding, bufferSize);
            AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, Constants.frequence, Constants.channelConfig, Constants.audioEncoding, bufferSize);
            AudioDeviceInfo audioDevice = RecordUtil.findAudioDevice(activity, AudioManager.GET_DEVICES_INPUTS, AudioDeviceInfo.TYPE_BLUETOOTH_SCO);
            if (null != audioDevice) {
                Log.i(Constants.TAG, "RecordTask: audioDevice = " + audioDevice.getType());
                record.setPreferredDevice(audioDevice);
            }

            //开始录音
            record.startRecording();
            Constants.recordImuData = true;
            activity.notifyRecording();//通知activity设置相关的变量

            byte audioData[] = new byte[bufferSize];

            //定义循环，根据recordImuData的值来判断是否继续录制
            while (Constants.recordImuData) {
                //从bufferSize中读取字节，返回读取的short个数
                int number = record.read(audioData, 0, bufferSize);
                dos.write(audioData, 0, number);
                //Log.i(Constants.TAG, "audioData number=" + number);
//                    System.out.println(Arrays.toString(audioData));
            }

            //录制结束
            record.stop();
            record.release();
            BluetoothUtil.getInstance(activity).closeSco();
            dos.flush();
            dos.close();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            final String wavFileName = Environment.getExternalStorageDirectory().getAbsolutePath() + Constants.dirname + voiceType + "/" + userType + ".wav";

            new Thread(new Runnable() {
                @Override
                public void run() {
                    WavUtils.convertWaveFile(fileName, wavFileName, Constants.frequence, bufferSize);
                }
            }).start();

            Log.i(Constants.TAG, "RecordTask2: over");
            Log.v(Constants.TAG, "The DOS available:" + file.getAbsolutePath());
            Log.v(Constants.TAG, "The DOS available:" + file.length());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
