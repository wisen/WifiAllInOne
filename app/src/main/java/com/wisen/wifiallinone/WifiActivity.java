package com.wisen.wifiallinone;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class WifiActivity extends AppCompatActivity {

    final private String TAG = "WifiAdmin";
    final private int REQUEST_COARSE_LOCATION = 10;

    /** Called when the activity is first created. */
    private TextView allNetWork;
    private Button scan;
    private Button start;
    private Button stop;
    private Button check;
    private WifiAdmin mWifiAdmin;
    // 扫描结果列表
    private List<ScanResult> list;
    private ScanResult mScanResult;
    private StringBuffer sb=new StringBuffer();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(!has_ACCESS_COARSE_LOCATION_Permission()){
            Log.d(TAG,"request permission fail!");
            //return;
        }

        allNetWork = (TextView) findViewById(R.id.allNetWork);
        scan = (Button) findViewById(R.id.scan);
        start = (Button) findViewById(R.id.start);
        stop = (Button) findViewById(R.id.stop);
        check = (Button) findViewById(R.id.check);

        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getAllNetWorkList();
            }
        });

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWifiAdmin.openWifi();
                Toast.makeText(WifiActivity.this, "当前wifi状态为："+mWifiAdmin.checkState(), Toast.LENGTH_SHORT).show();
            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWifiAdmin.closeWifi();
                Toast.makeText(WifiActivity.this, "当前wifi状态为："+mWifiAdmin.checkState(), Toast.LENGTH_SHORT).show();
            }
        });

        check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(WifiActivity.this, "当前wifi状态为："+mWifiAdmin.checkState(), Toast.LENGTH_SHORT).show();
            }
        });

        mWifiAdmin = new WifiAdmin(WifiActivity.this);
    }

    public void getAllNetWorkList(){
        // 每次点击扫描之前清空上一次的扫描结果
        Log.d(TAG, "getAllNetWorkList");
        if(sb!=null){
            sb=new StringBuffer();
        }
        //开始扫描网络
        mWifiAdmin.startScan();
        list=mWifiAdmin.getWifiList();
        if(list!=null){
            for(int i=0;i<list.size();i++){
                //得到扫描结果
                mScanResult=list.get(i);
                sb=sb.append(mScanResult.BSSID+"  ").append(mScanResult.SSID+"   ")
                        .append(mScanResult.capabilities+"   ").append(mScanResult.frequency+"   ")
                        .append(mScanResult.level+"\n\n");
            }
            allNetWork.setText("扫描到的wifi网络：\n"+sb.toString());
        }
    }

    protected boolean has_ACCESS_COARSE_LOCATION_Permission() {
        Log.d(TAG, "has_ACCESS_COARSE_LOCATION_Permission()");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "requestPermissions Manifest.permission.ACCESS_COARSE_LOCATION");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_COARSE_LOCATION);
            return false;
        }
        return true;
    }

    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult, requestCode = " + requestCode +
                ", permissions = " + permissions + ", grantResults = " + grantResults[0]);
        switch (requestCode) {
            case REQUEST_COARSE_LOCATION: {
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
    }

}
