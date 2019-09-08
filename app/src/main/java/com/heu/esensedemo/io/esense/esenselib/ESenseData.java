package com.heu.esensedemo.io.esense.esenselib;

import android.os.Parcel;
import android.os.Parcelable;

public class ESenseData implements Parcelable {
    public long timestamp;  //phone's timestamp
    public double[] accel;   //3-elements array with X, Y and Z axis for accelerometer
    public double[] gyro;    //3-elements array with X, Y and Z axis for gyroscope

    public ESenseData(long timestamp, double[] accel, double[] gyro) {
        this.timestamp = timestamp;
        this.accel = accel;
        this.gyro = gyro;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public double[] getAccel() {
        return accel;
    }

    public double[] getGyro() {
        return gyro;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.timestamp);
        dest.writeDoubleArray(this.accel);
        dest.writeDoubleArray(this.gyro);
    }

    protected ESenseData(Parcel in) {
        this.timestamp = in.readLong();
        this.accel = in.createDoubleArray();
        this.gyro = in.createDoubleArray();
    }

    public static final Parcelable.Creator<ESenseData> CREATOR = new Parcelable.Creator<ESenseData>() {
        @Override
        public ESenseData createFromParcel(Parcel source) {
            return new ESenseData(source);
        }

        @Override
        public ESenseData[] newArray(int size) {
            return new ESenseData[size];
        }
    };
}
