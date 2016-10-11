package com.wisen.wifiallinone;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class WifiAllInOneMainActivity extends AppCompatActivity {

    private Button ScanTest;
    private Button HotSpotTest;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_all_in_one_main);

        ScanTest = (Button)findViewById(R.id.open_scan);
        ScanTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(WifiAllInOneMainActivity.this, WifiActivity.class);
                startActivity(intent);
            }
        });

        HotSpotTest = (Button)findViewById(R.id.hotspot_test);
        HotSpotTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(WifiAllInOneMainActivity.this, HotSpotTestMainActivity.class);
                startActivity(intent);
            }
        });
    }
}
