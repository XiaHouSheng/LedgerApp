package com.xiahousheng.ledger;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class loginActivity extends AppCompatActivity {
    private EditText ledgerLoginUsn,ledgerLoginPsw;
    private Button ledgerLoginBtn;
    private static String url="http://82.156.201.153/ledgerdb/login";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        //判断是否未登录
        isLogin();
        Toast.makeText(loginActivity.this,"测试启动",Toast.LENGTH_LONG).show();
        //初始化
        ledgerLoginPsw=findViewById(R.id.ledger_login_psw);
        ledgerLoginUsn=findViewById(R.id.ledger_login_usn);
        ledgerLoginBtn=findViewById(R.id.ledger_btn_login);
        //设置按钮监听
        ledgerLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String username = ledgerLoginUsn.getText().toString();
                String password = ledgerLoginPsw.getText().toString();
                //判断username是否合法
                System.out.println(username);
                System.out.println(password);
                if(username.isEmpty()||password.isEmpty()){
                    Toast.makeText(loginActivity.this,"Button|不能为空",Toast.LENGTH_SHORT).show();
                }else{
                    login(username,md5enc(password));
                    Toast.makeText(loginActivity.this,"Button|合法",Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    //用于登录
    private void login(String username,String password){
        OkHttpClient client = new OkHttpClient();
        MediaType JSON = MediaType.get("application/json");
        String json=String.format("{\"username\":\"%s\",\"password\":\"%s\"}",username,password);
        RequestBody body = RequestBody.create(json,JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        String text = null;
        try (Response response = client.newCall(request).execute()){
            assert response.body() != null;
            text=response.body().string();
            System.out.println(text);
        }catch (IOException e){
            System.out.println(e);
        }
        Gson gson = new Gson();
        LoginResponse data = gson.fromJson(text, LoginResponse.class);
        if (data.code==0){
            Toast.makeText(loginActivity.this,"Success|检查账号和密码",Toast.LENGTH_SHORT).show();
        }else{
            try {
                saveToken(data.token);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Intent intent = new Intent(loginActivity.this, MainActivity.class);
            startActivity(intent);
        }

    }
    //保存Token
    private void saveToken(String token) throws IOException {
        File internalStorageDir = getFilesDir();
        File file = new File(internalStorageDir,"token.txt");
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(token.getBytes());
    }

    //md5加密
    private String md5enc(String input){
        try {
            // 创建 MessageDigest 实例并指定算法为 MD5
            MessageDigest md = MessageDigest.getInstance("MD5");
            // 将字符串转换为字节数组并更新 MessageDigest
            md.update(input.getBytes());
            // 计算 MD5 哈希值
            byte[] digest = md.digest();
            // 将字节数组转换为十六进制字符串
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            String md5Hash = sb.toString();
            return md5Hash;
            } catch (NoSuchAlgorithmException e) {
                System.err.println("没有这种算法");
            }
        return "";
    }
    //response structure
    static class LoginResponse{
        int code;
        String token;
    }

    //判断是否为第一次登录
    private void isLogin(){
        File internalStorageDir = getFilesDir();
        File file = new File(internalStorageDir,"token.txt");
        if (file.exists()){
            Intent intent = new Intent(loginActivity.this,MainActivity.class);
            startActivity(intent);
        }
    }

}