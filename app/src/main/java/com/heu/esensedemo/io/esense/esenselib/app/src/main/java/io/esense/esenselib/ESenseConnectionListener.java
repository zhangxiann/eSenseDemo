package com.heu.esensedemo.io.esense.esenselib.app.src.main.java.io.esense.esenselib;

import com.heu.esensedemo.io.esense.esenselib.app.src.main.java.io.esense.esenselib.ESenseManager;

public interface ESenseConnectionListener {
    /**
     * Called when the device with the specified name has been found during a scan
     * @param manager device manager
     */
    void onDeviceFound(ESenseManager manager);

    /**
     * Called when the device with the specified name has not been found during a scan
     * @param manager device manager
     */
    void onDeviceNotFound(ESenseManager manager);

    /**
     * Called when the connection has been successfully made
     * @param manager device manager
     */
    void onConnected(ESenseManager manager);

    /**
     * Called when the device has been disconnected
     * @param manager device manager
     */
    void onDisconnected(ESenseManager manager);
}
