package com.wisen.wifiallinone;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.HotspotClient;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class HotSpotTestMainActivity extends AppCompatActivity {

    private static final String VERSION = "1.0";

    private final String TAG = "HotSpotTst";
    protected static final int SHORT_TIMEOUT = 5 * 1000; // 5 seconds
    protected static final long LONG_TIMEOUT = 30 * 1000;  // 2 minutes

    public WifiManager mWifiManager = null;
    private Context mContext;
    WifiConfiguration mConfig = new WifiConfiguration();

    private static String NETWORK_ID = "hotspot_stressTst";
    private static String PASSWD = "1234567890";

    private final String getWifiApStateMethodName = "getWifiApState";
    Method getWifiApStateMethod = null;
    private final String setWifiApEnabledMethodName = "setWifiApEnabled";
    Method setWifiApEnabledMethod = null;
    private final String isWifiApEnabledMethodName = "isWifiApEnabled";
    Method isWifiApEnabledMethod = null;
    private final String getHotspotClientsMethodName = "getHotspotClients";
    Method getHotspotClientsMethod = null;

    private final String WIFI_AP_STATE_CHANGED_ACTION = "android.net.wifi.WIFI_AP_STATE_CHANGED";
    private final String WIFI_HOTSPOT_CLIENTS_CHANGED_ACTION = "android.net.wifi.WIFI_HOTSPOT_CLIENTS_CHANGED";

    private IntentFilter mIntentFilter = null;
    private HotSpotStateReceiver mHotSpotStateReceiver = null;

    private int mTotalIterations = 5;
    private int mLastIteration = 0;

    public static final int WIFI_AP_STATE_DISABLING = 10;
    public static final int WIFI_AP_STATE_DISABLED = 11;
    public static final int WIFI_AP_STATE_ENABLING = 12;
    public static final int WIFI_AP_STATE_ENABLED = 13;
    public static final int WIFI_AP_STATE_FAILED = 14;

    final private int REQUEST_WRITE_SETTINGS = 99;

    private EditText mEditCount = null;
    private EditText mEditOnTime = null;
    private EditText mEditOffTime = null;
    private Button btn_hotspotOnoffTst_start = null;
    private Button btn_hotspotOnoffTst_stop = null;
    private TextView mTextState = null;
    private TextView mTextMessage = null;
    private static final String mTextPrepare = "Edit test count and delay time, then press Start button to begin...";

    protected long mTotalCount;
    protected long mOnTime;
    protected long mOffTime;
    private long mCount;
    private int mCheckNetworkCount;
    private int mClients;
    private static final int MAX_CHECK_NETWORK_COUNT = 60; //60 second timeout

    private int mWifiApState = WIFI_AP_STATE_DISABLED;

    private static final int MSG_TIMER = 1;
    private static final int MSG_DELAY_UPDATE_STATE = 2;
    private static final int MSG_SWITCH_STATUS = 3;
    private static final int MSG_CHECK_NETWORK = 4;
    private static final int MSG_AUTOTEST = 5;
    private static final int MSG_STOP = 6;
    private static final int MSG_START = 7;

    private boolean waitforClientConnect = false;
    private boolean waitforClientDisconn = false;

    List<HotspotClient> clientsList = null;

    private void initValues(){
        mCount = 0;
        mTotalCount = 3000;
        mOnTime = 30000;
        mOffTime = 30000;
        mCheckNetworkCount = 0;
        mClients = 0;
    }

    private void findViews(){
        mEditCount = (EditText) findViewById (R.id.edit_auto_test_count);
        mEditOnTime = (EditText) findViewById (R.id.edit_on_time);
        mEditOffTime = (EditText) findViewById (R.id.edit_off_time);
        btn_hotspotOnoffTst_start = (Button) findViewById (R.id.btn_hotspot_tststart);
        btn_hotspotOnoffTst_stop = (Button) findViewById (R.id.btn_hotspot_tststop);
        mTextState = (TextView) findViewById (R.id.text_state);
        mTextMessage = (TextView) findViewById (R.id.text_msg);
    }

    private void initViews(){
        mEditCount.setText (String.valueOf (mTotalCount));
        mEditOnTime.setText (String.valueOf (mOnTime / 1000));
        mEditOffTime.setText (String.valueOf (mOffTime / 1000));
        mTextMessage.setText (mTextPrepare);
        mTextState.setText ("Current State: ...");

        btn_hotspotOnoffTst_stop.setEnabled (false);

        btn_hotspotOnoffTst_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disableWifiandHotSpot();

                clickStart();
            }
        });

        btn_hotspotOnoffTst_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickStop();
            }
        });
    }

    private boolean disableWifiandHotSpot(){
        if (mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(false);
            // wait for the wifi state to be DISABLED
            if(waitForWifiState(WifiManager.WIFI_STATE_DISABLED, LONG_TIMEOUT)){
                Log.d(TAG, "Disable wifi success, start hotspot test.");
            } else {
                return false;
            }
        }

        boolean isWifiApEnabled = false;
        try {
            isWifiApEnabled = (boolean)isWifiApEnabledMethod.invoke(mWifiManager);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        if (isWifiApEnabled) {
            Log.d(TAG, "hotspot already on, turn off it.");
            try {
                setWifiApEnabledMethod.invoke(mWifiManager, mConfig, false);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
            //SystemClock.sleep(SHORT_TIMEOUT);
        }

        return true;
    }

    private void onTimerFinal()
    {
        mEditCount.setEnabled (true);
        mEditOnTime.setEnabled (true);
        mEditOffTime.setEnabled (true);
        btn_hotspotOnoffTst_start.setEnabled (true);
        btn_hotspotOnoffTst_stop.setEnabled (false);
        updateStateText();
    }

    private void clickStop()
    {
        onTimerFinal();

        mHandler.removeMessages(MSG_TIMER);
        mHandler.removeMessages(MSG_SWITCH_STATUS);
        mHandler.removeMessages(MSG_CHECK_NETWORK);
        Log.d(TAG, "Stop hotspot tst, reason: user terminal!");
    }

    private void testOver(){
        onTimerFinal();

        mHandler.removeMessages(MSG_TIMER);
        mHandler.removeMessages(MSG_SWITCH_STATUS);
        mHandler.removeMessages(MSG_CHECK_NETWORK);
        updateStateText();
    }

    private void clickStart()
    {
        getWifiApState();

        try
        {
            mTotalCount = Long.parseLong (mEditCount.getText ().toString ());
            mOnTime = Long.parseLong (mEditOnTime.getText ().toString ()) * 1000;
            mOffTime = Long.parseLong (mEditOffTime.getText ().toString ()) * 1000;
            Log.d(TAG, "Save paramters: mTotalCount = " + mTotalCount + ", mOnTime = " + mOnTime + ", mOffTime = " + mOffTime);
        }
        catch (Exception e)
        {
            mTotalCount = 0;
            mOnTime = 0;
            mOffTime = 0;
        }

        if ((mTotalCount <= 0) || (mOnTime <= 0) || (mOffTime <= 0))
        {
            mTextMessage.setText ("Invalid value!");
            Log.d(TAG, "Stop hotspot tst, reason: invalid argument!");

            return;
        }


        mEditCount.setEnabled (false);
        mEditOnTime.setEnabled (false);
        mEditOffTime.setEnabled (false);
        btn_hotspotOnoffTst_start.setEnabled (false);
        btn_hotspotOnoffTst_stop.setEnabled (true);
        mCount = 0;
        mHandler.sendMessage (mHandler.obtainMessage(MSG_CHECK_NETWORK));
    }

    protected void updateStateText()
    {
        mTextState.setText ("HotSpot Sts: " + getWifiApStateStr());
    }

    private void updateMessageText(){
        mTextMessage.setText("mClients: " + mClients);
    }

    private String getWifiApStateStr()
    {
        switch (mWifiApState)
        {
            case WIFI_AP_STATE_ENABLED:	return "ENABLED";
            case WIFI_AP_STATE_DISABLED:	return "DISABLED";
            case WIFI_AP_STATE_DISABLING:	return "DISABLING";
            case WIFI_AP_STATE_ENABLING:	return "ENABLING";
            case WIFI_AP_STATE_FAILED:      return "Failed";
        }
        return "Unknown";
    }

    private void initReflectMethod(){
        if(null != mWifiManager){
            try {
                getWifiApStateMethod = mWifiManager.getClass().getMethod(getWifiApStateMethodName);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }

            try {
                setWifiApEnabledMethod = mWifiManager.getClass().getMethod(setWifiApEnabledMethodName, WifiConfiguration.class, boolean.class);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }

            try {
                isWifiApEnabledMethod = mWifiManager.getClass().getMethod(isWifiApEnabledMethodName);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }

            try {
                getHotspotClientsMethod = mWifiManager.getClass().getMethod(getHotspotClientsMethodName);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
    }

    private void initWifiApConfig(){
        mConfig.SSID = NETWORK_ID;
        mConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        mConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        mConfig.preSharedKey = PASSWD;
    }

    private void updateWifiApConfig(){
        //TODO
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hot_spot_test_main);

        setTitle("HotSpotTest Ver: "+VERSION);

        //step1 get permisson///////////////////////////////////
        if(!Settings.System.canWrite(this)){
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_WRITE_SETTINGS);
        }
        ///////////////////////////////////////////////////////

        // Get an instance of WifiManager
        //step2 get wifimanager instance///////////////////////
        mWifiManager =(WifiManager)getSystemService(Context.WIFI_SERVICE);
        if(null == mWifiManager) {
            Log.e(TAG, "get wifimanager fail!");
        }
        /////////////////////////////////////////////////////

        //step3 init values and views///////////////////////
        initValues();
        findViews();
        initViews();
        initReflectMethod();
        initWifiApConfig();
        ///////////////////////////////////////////////////

        //step4 register receiver//////////////////////////
        mHotSpotStateReceiver = new HotSpotStateReceiver();
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WIFI_AP_STATE_CHANGED_ACTION);//WifiManager.WIFI_AP_STATE_CHANGED_ACTION
        mIntentFilter.addAction(WIFI_HOTSPOT_CLIENTS_CHANGED_ACTION);
        registerReceiver(mHotSpotStateReceiver, mIntentFilter);
        ////////////////////////////////////////////////////
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_WRITE_SETTINGS) {
            if (Settings.System.canWrite(this)) {
                //检查返回结果
                Toast.makeText(HotSpotTestMainActivity.this, "WRITE_SETTINGS permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(HotSpotTestMainActivity.this, "WRITE_SETTINGS permission not granted", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean doSwitchStatus()
    {

        if (mWifiApState == WIFI_AP_STATE_DISABLED) {
            mCount++;
            if(mCount > mTotalCount)
                return false;
            Log.d(TAG, "set HotSpot on, times: " + mCount);
            try {
                setWifiApEnabledMethod.invoke(mWifiManager, mConfig, true);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
            mWifiApState = WIFI_AP_STATE_ENABLING;
        } else if (mWifiApState == WIFI_AP_STATE_ENABLED){
            Log.d(TAG, "set HotSpot off, times: " + mCount);
            try {
                setWifiApEnabledMethod.invoke(mWifiManager, mConfig, false);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
            mWifiApState = WIFI_AP_STATE_DISABLING;
        }

        return true;
    }

    private Handler mHandler = new Handler () {
        @Override
        public void handleMessage(Message msg) {

            Log.d(TAG, "Msg = " + msg.what);
            switch (msg.what)
            {
                case MSG_START:
                    SystemClock.sleep(SHORT_TIMEOUT);
                    getWifiApState();
                    mHandler.sendMessage (mHandler.obtainMessage(MSG_CHECK_NETWORK));
                    //mHandler.sendMessage (mHandler.obtainMessage(MSG_SWITCH_STATUS));
                    break;
                case MSG_TIMER:
                    mCheckNetworkCount = 0;

                    if (mCount >= mTotalCount)
                    {
                        testOver();
                        Log.d(TAG, "hotspot test Over!");
                        mTextMessage.setText (mTextPrepare);
                    }
                    else
                    {
                        sendMessage (obtainMessage(MSG_SWITCH_STATUS));
                    }
                    break;
                case MSG_SWITCH_STATUS:
                    doSwitchStatus();
                    sendMessage(obtainMessage(MSG_CHECK_NETWORK));
                    break;
                case MSG_CHECK_NETWORK:
                    updateStateText();
                    //updateMessageText();
                    if (mWifiApState == WIFI_AP_STATE_ENABLED)
                    {
                        waitforClientConnect = true;
                        waitforClientDisconn = false;
                        sendMessageDelayed(obtainMessage(MSG_TIMER), mOnTime);
                    }
                    else if (mWifiApState == WIFI_AP_STATE_DISABLED)
                    {
                        waitforClientConnect = false;
                        waitforClientDisconn = true;
                        sendMessageDelayed(obtainMessage(MSG_TIMER), mOffTime);
                    }
                    else if (mCheckNetworkCount++ < MAX_CHECK_NETWORK_COUNT)
                    {
                        Log.d(TAG, "mCheckNetworkCount = " + mCheckNetworkCount);
                        sendMessageDelayed(obtainMessage(MSG_CHECK_NETWORK), 1000);
                    }
                    else
                    {
                        Log.d(TAG, "get hotspot status timeout, current state = " + getWifiApStateStr());
                    }
                    break;
            }
        }
    };

    private void getWifiApState(){
        try {
            mWifiApState = (int)getWifiApStateMethod.invoke(mWifiManager);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "mWifiApState = " + mWifiApState);
    }

    protected boolean waitForWifiApState(int expectedState, long timeout) {
        long startTime = SystemClock.uptimeMillis();
        while (true) {
            try {
                mWifiApState = (int)getWifiApStateMethod.invoke(mWifiManager);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }

            if (mWifiApState == expectedState) {
                Log.d(TAG, "waitForWifiAPState success: state=" + mWifiApState);
                return true;
            }
            if ((SystemClock.uptimeMillis() - startTime) > timeout) {
                Log.d(TAG, String.format("waitForWifiAPState timeout: expected = %d, actual = %d",
                        expectedState, mWifiApState));
                return false;
            }
            Log.d(TAG, String.format("waitForWifiAPState interim: expected = %d, actual = %d",
                    expectedState, mWifiApState));
            //SystemClock.sleep(SHORT_TIMEOUT);
        }
    }

    protected boolean waitForWifiState(int expectedState, long timeout) {
        long startTime = SystemClock.uptimeMillis();
        while (true) {
            int state = mWifiManager.getWifiState();
            if (state == expectedState) {
                Log.d(TAG,"waitForWifiState success: state=" + state);
                return true;
            }
            if ((SystemClock.uptimeMillis() - startTime) > timeout) {
                Log.d(TAG,"waitForWifiState timeout: expected = %d, actual = %d" + expectedState + state);
                return false;
            }
            Log.d(TAG,"waitForWifiState interim: expected = %d, actual = %d" + expectedState + state);
            SystemClock.sleep(SHORT_TIMEOUT);
        }
    }

    private class HotSpotStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "HotSpotStateReceiver onReceive() is calleld with " + intent);
            if (action.equals(WIFI_AP_STATE_CHANGED_ACTION)){
                getWifiApState();
            }else if (action.equals(WIFI_HOTSPOT_CLIENTS_CHANGED_ACTION)) {
                try {
                    clientsList = (List<HotspotClient>)getHotspotClientsMethod.invoke(mWifiManager);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }

                mTextMessage.setText("Clients list:\n" + clientsList.toString());
                Log.d(TAG, "Clients list:\n" + clientsList.toString());
            }
        }
    }

    public void testHotSpot(){
        int i;
        for (i = 0; i < mTotalIterations; i++) {
            Log.d(TAG, "Hotspot OnOff Test start, times: " + i);
            mLastIteration = i;
            // enable Wifi tethering
            try {
                //need permission: android.permission.WRITE_SETTINGS
                setWifiApEnabledMethod.invoke(mWifiManager, mConfig, true);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }

            // wait for wifi ap state to be ENABLED
            if(!waitForWifiApState(WIFI_AP_STATE_ENABLED, 2 * LONG_TIMEOUT)){
                Log.d(TAG, "Hotspot OnOff Test Over at times: " + i + " reason: enable fail.");
                break;
            }

            /*
            // wait for wifi tethering result
            waitForTetherStateChange(LONG_TIMEOUT);
            // allow the wifi tethering to be enabled for 10 seconds
            try {
                Thread.sleep(2 * SHORT_TIMEOUT);
            } catch (Exception e) {
                // ignore
            }
            //test whether there have uplink data connection after Wi-Fi tethering
            pingTest();
            */
            // disable wifi hotspot
            try {
                setWifiApEnabledMethod.invoke(mWifiManager, mConfig, false);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }

            if(!waitForWifiApState(WIFI_AP_STATE_DISABLED, 2 * LONG_TIMEOUT)){
                Log.d(TAG, "Hotspot OnOff Test Over at times: " + i + "reason: disable fail.");
                break;
            }

            //mWifiManager.isWifiApEnabled();
            Log.d(TAG, "Hotspot OnOff Test end, times: " + i);
        }
    }

    /*
    protected boolean has_WRITE_SETTINGS_Permission() {
        Log.d(TAG, "has_WRITE_SETTINGS_Permission()");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_SETTINGS)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "requestPermissions Manifest.permission.WRITE_SETTINGS");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_SETTINGS},
                    REQUEST_WRITE_SETTINGS);
            return false;
        }
        return true;
    }

    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult, requestCode = " + requestCode +
                ", permissions = " + permissions + ", grantResults = " + grantResults[0]);
        switch (requestCode) {
            case REQUEST_WRITE_SETTINGS: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //init_recorder_and_player();
                    Log.d(TAG, "grant success!");
                } else {
                    //TODO re-request
                    Log.d(TAG, "grant fail!");
                }
                break;
            }
        }
    }*/
}
