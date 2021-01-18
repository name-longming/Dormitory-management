package com.example.application;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private Button mBtnLogin;
    private EditText name;  //用户名
    private EditText pass;  //密码

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        name=(EditText) findViewById(R.id.et_1);  //获取用户名
        pass=(EditText) findViewById(R.id.et_2);  //获取密码

        mBtnLogin = findViewById(R.id.btn_login);
        mBtnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String mname = "admin";
                String mpass = "4022";
                String user = name.getText().toString().trim();
                String pwd = pass.getText().toString().trim();
                if (user.equals(mname) && pwd.equals(mpass)) {
                    Toast.makeText(MainActivity.this,"登录成功！",Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(MainActivity.this,ControlActivity.class);
                    startActivity(intent);
                }
                else{
                    Toast.makeText(MainActivity.this, "密码或是账号错误！", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}