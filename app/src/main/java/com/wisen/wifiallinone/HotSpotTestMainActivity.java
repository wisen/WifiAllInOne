package com.wisen.wifiallinone;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class HotSpotTestMainActivity extends AppCompatActivity {

    private Button btn_hotspotOnoffTst;

    public WifiManager mWifiManager;
    private Context mContext;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hot_spot_test_main);

        // Get an instance of WifiManager
        mWifiManager =(WifiManager)getSystemService(Context.WIFI_SERVICE);

        btn_hotspotOnoffTst = (Button)findViewById(R.id.btn_hotspot_tststart);
        btn_hotspotOnoffTst.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }

    private Handler mHandler = new Handler () {
        @Override
        public void handleMessage(Message msg) {

        }
    };
}
