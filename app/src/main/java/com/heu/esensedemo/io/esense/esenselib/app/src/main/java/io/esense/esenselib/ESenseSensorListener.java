package com.heu.esensedemo.io.esense.esenselib.app.src.main.java.io.esense.esenselib;

import com.heu.esensedemo.io.esense.esenselib.app.src.main.java.io.esense.esenselib.ESenseEvent;

public interface ESenseSensorListener {
    /**
     * Called when there is new sensor data available
     * @param evt object containing the sensor samples received
     */
    void onSensorChanged(ESenseEvent evt);
}
