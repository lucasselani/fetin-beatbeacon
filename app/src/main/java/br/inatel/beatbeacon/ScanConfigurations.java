package br.inatel.beatbeacon;

import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by lucasselani on 27/09/2016.
 */

public interface ScanConfigurations {
    //BLE SCAN SETTINGS
    ScanSettings SCAN_SETTINGS =
            new ScanSettings.Builder().
                    setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setReportDelay(0)
                    .build();

    //BLE SCAN FILTER
    List<ScanFilter> SCAN_FILTERS =
            new ArrayList<>(Collections.singletonList(
                    new ScanFilter.Builder()
                            .build()));
}
