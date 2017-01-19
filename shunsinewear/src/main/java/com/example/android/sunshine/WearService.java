package com.example.android.sunshine;

import android.util.Log;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;

public class WearService extends WearableListenerService {

    private static final String COUNT_KEY = "com.example.key.count";

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d("WearService", "onDataChanged ");
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                // DataItem changed
                DataItem item = event.getDataItem();
                if (item.getUri().getPath().compareTo("/wear-data-changed") == 0) {
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    dataMap.getInt(COUNT_KEY);
                    Log.d("WearService", "dataMap.getInt(COUNT_KEY): " + dataMap.getInt(COUNT_KEY));
                }
            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                // DataItem deleted
            }
        }
    }
}
