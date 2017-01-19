package com.example.android.sunshine;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends WearableActivity implements
        DataApi.DataListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private TextView mWeatherTempMax;
    private TextView mWeatherTempMin;
    private ImageView mWeatherIcon;

    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setAmbientEnabled();

        final TextView formattedDate = (TextView) findViewById(R.id.formatted_date);
        mWeatherTempMax = (TextView) findViewById(R.id.weather_temp_max);
        mWeatherTempMin = (TextView) findViewById(R.id.weather_temp_min);
        mWeatherIcon = (ImageView) findViewById(R.id.weather_icon);

        formattedDate.setText(formattedDateFromString());

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Wearable.DataApi.removeListener(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "onConnected: GoogleApiClient");
        Wearable.DataApi.addListener(mGoogleApiClient, this);
        requestForWeatherInfo();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended: GoogleApiClient");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed: GoogleApiClient - " + connectionResult);
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                // DataItem changed
                DataItem item = event.getDataItem();
                if (item.getUri().getPath().compareTo("/wear-data-changed") == 0) {
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    updateDisplay(dataMap);
                }
            }
        }
    }

    private void updateDisplay(final DataMap dataMap) {
        final Asset profileAsset = dataMap.getAsset("WEATHER_ICON");
        loadBitmapFromAsset(profileAsset);
        mWeatherTempMax.setText(dataMap.getString("MAX_TEMP"));
        mWeatherTempMin.setText(dataMap.getString("MIN_TEMP"));
    }


    private void requestForWeatherInfo() {
        final PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/mobile-data-changed");
        putDataMapReq.setUrgent();

        double randomDouble = new Random().nextDouble();
        final DataMap dataMap = putDataMapReq.getDataMap();

        dataMap.putDouble("RENEW_PATH_DATA", randomDouble);
        dataMap.putString("UPDATE_WEATHER", "UPDATE_WEATHER");

        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult =
                Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);
        pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
            @Override
            public void onResult(@NonNull final DataApi.DataItemResult result) {
                if (result.getStatus().isSuccess()) {
                    Log.d(TAG, "Data item set: " + result.getDataItem().getUri());
                }
            }
        });
    }


    private String formattedDateFromString() {
        final Date date = new Date();

        final SimpleDateFormat dateFormat =
                new SimpleDateFormat("EE',' MMM d yyyy", java.util.Locale.getDefault());
        try {
            return dateFormat.format(date).toUpperCase();
        } catch (Exception e) {
            Log.e(TAG, "Exception in formateDateFromstring(): " + e.getMessage());
        }
        return "";
    }

    private void loadBitmapFromAsset(final Asset asset) {
        if (asset == null) {
            throw new IllegalArgumentException("Asset must be non-null");
        }

        final ExecutorService executorService = Executors.newSingleThreadExecutor();

        executorService.execute(new Runnable() {
            @Override
            public void run() {

                // convert asset into a file descriptor and block until it's ready
                final InputStream assetInputStream = Wearable.DataApi.getFdForAsset(
                        mGoogleApiClient, asset).await().getInputStream();

                if (assetInputStream == null) {
                    Log.w(TAG, "Requested an unknown Asset.");
                    return;
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // decode the stream into a bitmap
                        final Bitmap bitmap = BitmapFactory.decodeStream(assetInputStream);
                        mWeatherIcon.setVisibility(View.VISIBLE);
                        mWeatherIcon.setImageBitmap(bitmap);
                    }
                });
            }
        });
    }
}
