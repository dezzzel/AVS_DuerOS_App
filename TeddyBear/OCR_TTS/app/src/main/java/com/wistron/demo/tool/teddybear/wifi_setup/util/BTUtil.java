package com.wistron.demo.tool.teddybear.wifi_setup.util;

import android.bluetooth.BluetoothManager;
import android.content.Context;

/**
 * Created by aaron on 16-9-9.
 */
public class BTUtil {

    /**
     * get BluetoothManager
     */
    public static BluetoothManager getManager(Context context) {
        return (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
    }

}
