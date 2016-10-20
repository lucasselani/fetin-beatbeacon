package br.inatel.beatbeacon;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Parcel;
import android.os.ParcelUuid;
import android.support.design.widget.Snackbar;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.security.Permissions;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Delayed;

import static br.inatel.beatbeacon.R.id.app_color;


public class FullscreenActivity extends AppCompatActivity implements ScanConfigurations {
    private static final boolean AUTO_HIDE = true;

    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    private static final int REQUEST_ENABLE_BT = 1;
    private static final String TAG = FullscreenActivity.class.getName();
    private static final String UUID_BEACON = "88c4649c-9875-4b8f-b2e6-5d06ae55f38c";
    private static final int INTERVAL = 1000;
    private Handler handler;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private FrameLayout mFrameLayout;

    private final int COLORS[] = {Color.BLACK, Color.GREEN, Color.BLUE, Color.YELLOW, Color.RED,
                                Color.GRAY, Color.CYAN, Color.MAGENTA, Color.WHITE};
    private final String POSITIONS[] = {"1","2","3","4","5","6","7","8","9"};
    private int previousColor = 0;
    private double totalRssi = 0;
    private int totalSamples = 0;
    private boolean alreadyScanned = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen);
        mFrameLayout = (FrameLayout) findViewById(R.id.fullscreen_layout);

        mVisible = true;

        handler = new Handler();
        // Set up the user interaction to manually show or hide the system UI.
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        askEnableBluetooth();
        askPermissions();
        enableBluetoothScanner();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    private void changeAppColor(final int currentColor){
        if(currentColor == previousColor) return;

        while(mFrameLayout.getChildCount() > 0) {
            mFrameLayout.removeView(findViewById(R.id.app_color));
        }

        LayoutInflater layoutInflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View inflatedLayout= layoutInflater.inflate(R.layout.color_layout, null, false);
        mFrameLayout.addView(inflatedLayout);
        final Animation fadeOut = new AlphaAnimation(1.00f, 0.00f);
        final Animation fadeIn = new AlphaAnimation(0.00f, 1.00f);

        fadeIn.setDuration(1000);
        fadeIn.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                mFrameLayout.findViewById(R.id.app_color).setBackgroundColor(COLORS[currentColor]);
                ((TextView) mFrameLayout.findViewById(R.id.app_color)
                        .findViewById(R.id.position_content))
                        .setText(POSITIONS[currentColor]);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                previousColor = currentColor;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        fadeOut.setDuration(1000);
        fadeOut.setAnimationListener(new Animation.AnimationListener(){
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                mFrameLayout.findViewById(R.id.app_color).startAnimation(fadeIn);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        mFrameLayout.findViewById(R.id.app_color).setBackgroundColor(COLORS[previousColor]);

        ((TextView) mFrameLayout.findViewById(R.id.app_color)
                .findViewById(R.id.position_content))
                .setText(POSITIONS[previousColor]);

        mFrameLayout.findViewById(R.id.app_color).startAnimation(fadeOut);
    }

    private void askPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED){
            String[] permissions = {Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION};
            ActivityCompat.requestPermissions(this, permissions, 3030);
        }
    }

    private void askEnableBluetooth(){
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null)
            Toast.makeText(this, "Seu dispositivo não suporta Bluetooth!", Toast.LENGTH_LONG);
        else if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBTIntent, REQUEST_ENABLE_BT);
        }
    }

    private void enableBluetoothScanner(){
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Log.v(TAG, "Bluetooth Adapter Enabled");
        if(!mBluetoothAdapter.isEnabled()) return;
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        if(mBluetoothLeScanner != null) {
            Log.v(TAG, "Bluetooth Scanner Enabled");
            mBluetoothLeScanner.startScan(SCAN_FILTERS, SCAN_SETTINGS, scanCallback);
        }
        else{
            Toast.makeText(FullscreenActivity.this,
                    "Seu dispositivo não suporta leitor de BLE!",
                    Toast.LENGTH_LONG).show();
        }
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.i("MainActivity", "Callback: Success");
            ScanRecord scanRecord = result.getScanRecord();
            if (scanRecord == null) {
                Log.w(TAG, "Null ScanRecord for device " + result.getDevice().getAddress());
                return;
            } else {
                String uuid = null;
                final String mac = result.getDevice().getAddress();

                List<ParcelUuid> serviceUuids = scanRecord.getServiceUuids();
                if(serviceUuids != null){
                    if(!serviceUuids.isEmpty()){
                        uuid = scanRecord.getServiceUuids().get(0).getUuid().toString();
                    } else return;
                }
/*                else return;
                //SAINDO CASO SEJA DIFERENTE
                try{
                    if(!uuid.equals(UUID_BEACON)) return;
                }catch (NullPointerException e){
                    e.printStackTrace();
                    return;
                }*/

                totalRssi += result.getRssi();
                totalSamples++;

                Log.v(TAG, "RSSI: " + totalRssi + " Samples: " + totalSamples);

                if(!alreadyScanned){
                    alreadyScanned = true;
                    handler.postDelayed(new Runnable(){
                        public void run(){
                            double rssi = totalRssi/totalSamples;
                            if(!Double.isNaN(rssi)) {
                                //DEBUGING OPTIONS
                                Snackbar.make(findViewById(R.id.fullscreen_layout),
                                        "MAC: " + mac + " - RSSI: " + rssi,
                                        Snackbar.LENGTH_INDEFINITE).show();
                                Log.v(TAG,"MAC: " + mac + " - RSSI: " + rssi);

                                if(rssi < -80){
                                    changeAppColor(0);
                                } else if(rssi < -70){
                                    changeAppColor(1);
                                } else if(rssi < -60){
                                    changeAppColor(2);
                                } else if(rssi < -50){
                                    changeAppColor(3);
                                } else if(rssi < -40){
                                    changeAppColor(4);
                                } else if(rssi < -30){
                                    changeAppColor(5);
                                } else if(rssi < -20){
                                    changeAppColor(6);
                                } else{
                                    changeAppColor(7);
                                }

                                totalRssi = 0;
                                totalSamples = 0;
                            }
                            handler.postDelayed(this, INTERVAL);
                        }
                    }, INTERVAL);
                }
            }
        }
        @Override
        public void onScanFailed(int errorCode) {
            Log.e(TAG, "onScanFailed errorCode " + errorCode);
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                switch (state) {
                    case BluetoothAdapter.STATE_TURNING_ON:
                        //Indicates the local Bluetooth adapter is turning on.
                        // However local clients should wait for STATE_ON before attempting to use the adapter.
                        Log.v(TAG, "Bluetooth Adapter Enabling");
                        break;

                    case BluetoothAdapter.STATE_ON:
                        //Indicates the local Bluetooth adapter is on, and ready for use.
                        enableBluetoothScanner();
                        break;

                    case BluetoothAdapter.STATE_OFF:
                        Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBTIntent, REQUEST_ENABLE_BT);
                        break;
                }

            }
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode,@NonNull String permissions[], @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        try {
            enableBluetoothScanner();
        }
        catch (SecurityException se){
            Toast.makeText(FullscreenActivity.this,
                    "Não é possível usar o aplicativo sem conceder permissões!",
                    Toast.LENGTH_LONG).show();
            askPermissions();
        }

        catch (Exception e){
            e.printStackTrace();
            if(!mBluetoothAdapter.isEnabled()){
                Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBTIntent, REQUEST_ENABLE_BT);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == REQUEST_ENABLE_BT) enableBluetoothScanner();
    }

    public static String getGuidFromByteArray(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        long high = bb.getLong();
        long low = bb.getLong();
        UUID uuid = new UUID(high, low);
        return uuid.toString();
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
