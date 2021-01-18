package com.example.application;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.UUID;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static android.os.Build.*;

public class ControlActivity extends AppCompatActivity {

    private BluetoothSocket btSocket = null;
    private static final int REQUEST_PERMISSION_ACCESS_LOCATION = 1;
    private BluetoothAdapter mBluetoothAdapter;
    private TextView text2;
    private Button botton;
    UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    String pin = "1234";  //此处为你要连接的蓝牙设备的初始密钥，一般为1234或0000
    private static String address = "20:20:08:25:10:41"; // <==要连接的目标蓝牙设备MAC地址
    private OutputStream outStream = null;
    private Button mbotton;
    boolean IsConnected = false;
    private TextView responseText;
    String text;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);


        text2 =this.findViewById(R.id.textView2); //状态信息
        botton=this.findViewById(R.id.button);
        mbotton=this.findViewById(R.id.button_Tx);
        responseText=this.findViewById(R.id.tx_weather);

        sendRequestWithOkHttp();

        mBluetoothAdapter=BluetoothAdapter.getDefaultAdapter();

        IntentFilter filter=new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver,filter);

        mbotton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new SendInfoTask().execute("Control");
            }
        });
        botton.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View arg0) {
                requestPermission();
                if(!mBluetoothAdapter.isEnabled())
                {
                    mBluetoothAdapter.enable();

                }
                mBluetoothAdapter.startDiscovery();
                new ConnectTask().execute(address);
                text2.setText("正在连接...");

            }


        });

    }

    private void sendRequestWithOkHttp(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                BufferedReader reader = null;
                try {
                    OkHttpClient client = new OkHttpClient();
                    Request request = new Request.Builder().url("https://api.seniverse.com/v3/weather/now.json?key=SG_ckZ26xPfp8E2EK&location=chengdu&language=zh-Hans&unit=c").build();
                    Response response = client.newCall(request).execute();
                    String responseData = response.body().string();
                    parseJSONWithJSONObject(responseData);
                    showResponse("今日天气:"+text);

                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    private void showResponse(final String response){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                responseText.setText(response);
            }
        });
    }
    private void parseJSONWithJSONObject(String jsonData) throws JSONException {
        JSONObject jsonObject = new JSONObject(jsonData);
        JSONArray jsonArray = jsonObject.getJSONArray("results");
        JSONObject Weather = jsonArray.getJSONObject(0);

        JSONObject location = Weather.getJSONObject("location");
        JSONObject now = Weather.getJSONObject("now");
        text = now.getString("text");
    }

    public void onDestroy() {

        super.onDestroy();
        //解除注册
        unregisterReceiver(mReceiver);
        try {
            btSocket.close();
            btSocket=null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.finish();
        Log.e("destory","解除注册");
    }



    //定义广播接收
    private BroadcastReceiver mReceiver=new BroadcastReceiver(){



        @RequiresApi(api = VERSION_CODES.KITKAT)
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction(); //得到action
            Log.e("action1=", action);
            BluetoothDevice btDevice=null;  //创建一个蓝牙device对象
            // 从Intent中获取设备对象
            btDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if(BluetoothDevice.ACTION_FOUND.equals(action)){  //发现设备
                Log.e("发现设备:", "["+btDevice.getName()+"]"+":"+btDevice.getAddress());

                if(btDevice.getName().contains("HC-05"))//HC-05设备如果有多个，第一个搜到的那个会被尝试。
                {
                    if (btDevice.getBondState() == BluetoothDevice.BOND_NONE) {

                        Log.e("ywq", "attemp to bond:"+"["+btDevice.getName()+"]");
                        try {
                            //通过工具类ClsUtils,调用createBond方法
                            com.ywq.tools.ClsUtils.createBond(btDevice.getClass(), btDevice);
                            com.ywq.tools.ClsUtils.setPairingConfirmation(btDevice.getClass(), btDevice, true);
                            abortBroadcast();//如果没有将广播终止，则会出现一个一闪而过的配对框。
                            //调用setPin方法进行配对...
                            boolean ret = com.ywq.tools.ClsUtils.setPin(btDevice.getClass(), btDevice, pin);
                        } catch (Exception e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                } else
                    Log.e("error", "Is faild");
            }
        }



    };

    class SendInfoTask extends AsyncTask<String,String,String>
    {

        @Override
        protected void onPostExecute(String result) {
            // TODO Auto-generated method stub
            super.onPostExecute(result);

        }

        @Override
        protected String doInBackground(String... arg0) {
            // TODO Auto-generated method stub

            if(btSocket==null)
            {
                return "还没有创建连接";
            }



            if(arg0[0].length()>0)//不是空白串
            {
                //String target=arg0[0];

                byte[] msgBuffer = arg0[0].getBytes();

                try {
                    //  将msgBuffer中的数据写到outStream对象中
                    outStream.write(msgBuffer);

                } catch (IOException e) {
                    Log.e("error", "ON RESUME: Exception during write.", e);
                    return "发送失败";
                }

            }

            return "发送成功";
        }

    }


    //连接蓝牙设备的异步任务
    class ConnectTask extends AsyncTask<String,String,String> {

        @Override
        protected String doInBackground(String... params) {
            // TODO Auto-generated method stub
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(params[0]);

            try {

                btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);


                btSocket.connect();

                Log.e("error", "ON RESUME: BT connection established, data transfer link open.");

            } catch (IOException e) {

                try {
                    btSocket.close();
                    return "Socket 创建失败";

                } catch (IOException e2) {
                    IsConnected = false;
                    Log.e("error", "ON RESUME: Unable to close socket during connection failure", e2);
                    return "Socket 关闭失败";
                }

            }
            //取消搜索
            mBluetoothAdapter.cancelDiscovery();

            try {
                outStream = btSocket.getOutputStream();

            } catch (IOException e) {
                Log.e("error", "ON RESUME: Output stream creation failed.", e);
                IsConnected = false;
                return "Socket 流创建失败";
            }

            IsConnected = true;
            return "蓝牙连接正常,Socket 创建成功";
        }


        @Override    //这个方法是在主线程中运行的，所以可以更新界面
        protected void onPostExecute(String result) {
            // TODO Auto-generated method stub
            if (IsConnected == true){
                text2.setText("成功连接上蓝牙HC-05");
            }else {
                text2.setText("连接失败，请重试");
            }
        }

    }

    private void requestPermission() {
        if (VERSION.SDK_INT >= 23) {
            int checkAccessFinePermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
            if (checkAccessFinePermission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_PERMISSION_ACCESS_LOCATION);
                Log.e(getPackageName(), "没有权限，请求权限");
                return;
            }
            Log.e(getPackageName(), "已有定位权限");
            //这里可以开始搜索操作
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_ACCESS_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.e(getPackageName(), "开启权限permission granted!");
                    //这里可以开始搜索操作
                } else {
                    Log.e(getPackageName(), "没有定位权限，请先开启!");
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}