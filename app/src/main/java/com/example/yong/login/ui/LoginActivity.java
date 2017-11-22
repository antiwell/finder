package com.example.yong.login.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.yong.houseinfo.ui.HouseInfoActivity;
import com.example.yong.login.network.LoginNetworkUtils;
import com.example.yong.yongshixiong.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import org.apache.log4j.Logger;
import de.mindpipe.android.logging.log4j.LogConfigurator;
import android.os.Environment;
import java.io.File;

import org.apache.log4j.Level;

import static com.example.yong.constants.SPConstants.SP_FILE_NAME;
import static com.example.yong.constants.SPConstants.SP_KEY_TOKEN;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    private final static String TAG = "LoginActivity";

    private EditText mInputUsernameEt;
    private EditText mInputPasswordEt;
    private TextView mLoginButton;
//    public static String sToken = null;
    public static Logger logger = Logger.getLogger(LoginActivity.class);

    //logger是否已经初始化过
    private boolean loggerHasConfigured = false;

    //需要检查的权限
    final static String [] permissionStrArry = new String[]{
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.PROCESS_OUTGOING_CALLS,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
    };

    final static int PERMISSION_REQUEST_CODE = 444;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mInputUsernameEt = (EditText) findViewById(R.id.input_id_et);
        mInputPasswordEt = (EditText) findViewById(R.id.input_pw_et);
        mLoginButton = (TextView) findViewById(R.id.login_btn);
        mLoginButton.setOnClickListener(this);

        if(!checkRequirePermissionGranted(permissionStrArry)){
            ActivityCompat.requestPermissions(this, permissionStrArry, PERMISSION_REQUEST_CODE);
        }else{
            if(!loggerHasConfigured){
                configureLogger();
            }
        }
    }

    private void configureLogger(){
        if(loggerHasConfigured){
            return;
        }
        loggerHasConfigured = true;
        TelephonyManager tm = (TelephonyManager) ((getApplicationContext()).getSystemService(Activity.TELEPHONY_SERVICE));
        String id = tm.getDeviceId();
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/recorder/";
        File pathDir = new File(path);
        if(pathDir.exists() == false){
            pathDir.mkdirs();
        }

        LogConfigurator logConfigurator = new LogConfigurator();
        String filePath = path +id+ "_log4j.txt";
        File file = new File(filePath);
        try{
            if(file.exists() == false){
                file.createNewFile();
            }

            logConfigurator.setFileName(filePath);
            logConfigurator.setRootLevel(Level.DEBUG);
            logConfigurator.setLevel("org.apache", Level.ERROR);
            logConfigurator.setFilePattern("%d %-5p [%c{2}]-[%L] %m%n");
            logConfigurator.setMaxFileSize(1024 * 1024 * 5);
            logConfigurator.setImmediateFlush(true);
            logConfigurator.configure();

            logger.info("system info:"+android.os.Build.VERSION.RELEASE);        //logger.debug("start......");
            logger.info("phone model:"+android.os.Build.MODEL);
            logger.info("phone brand:"+android.os.Build.BRAND);
            logger.info("phone IMIE:"+tm.getDeviceId());

        }catch (IOException e){
            Log.w(TAG, e);
        }
    }

    /**
     * 检查所需的权限是否已经申请
     */
    private boolean checkRequirePermissionGranted(String [] permissions){
        if(permissions == null || permissions.length <= 0){
            return true;
        }

        for(String permission : permissions){
            if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
                Log.w(TAG, " checkRequirePermissionGranted not PERMISSION_GRANTED for " + permission);
                return false;
            }
        }

        return true;

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.login_btn:
                if(checkRequirePermissionGranted(permissionStrArry)){    //检查权限
                  mLoginButton.setClickable(false);
                  onClickLogin();
                }else{
                    ActivityCompat.requestPermissions(this, permissionStrArry, PERMISSION_REQUEST_CODE);
                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == PERMISSION_REQUEST_CODE){
            boolean isAllGranted = true;
            Log.w(TAG, " onRequestPermissionsResult grantResults.length == " + grantResults.length);
            for(int grant : grantResults){
                Log.w(TAG, " onRequestPermissionsResult grant == " + grant);
                if(grant != PackageManager.PERMISSION_GRANTED){
                    isAllGranted = false;
                    break;
                }
            }
            Log.w(TAG, " onRequestPermissionsResult isAllGranted " + isAllGranted);
            if(isAllGranted){
                if(!loggerHasConfigured){
                    configureLogger();
                }
            }else{
                Toast.makeText(this, R.string.permission_granded_failed, Toast.LENGTH_SHORT);
            }
        }
    }

    /**
     * 点击登陆
     */
    private void onClickLogin() {
        class Result {
            public int errorCode;
            public String errorMsg;
            public String data;

            public Result(int code, String msg, String dataIn) {
                errorCode = code;
                errorMsg = msg;
                data = dataIn;
            }
        }
        ;

        Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                String s = LoginNetworkUtils.getTokenSync(getApplicationContext());
                if (TextUtils.isEmpty(s)) {
                    subscriber.onError(new Throwable("getToken fail"));
                    subscriber.onCompleted();
                    mLoginButton.setClickable(true);
                } else {
                    subscriber.onNext(s);
                    subscriber.onCompleted();
                }
            }
        }).map(new Func1<String, Result>() {
            @Override
            public Result call(String s) {
                if (TextUtils.isEmpty(s)) {
                    return new Result(-1, "Get Token null", null);
                } else {
                    try {
                        JSONObject jsonObject = new JSONObject(s);
                        int errorCode = jsonObject.optInt("err_code");
                        String errorMsg = jsonObject.optString("reason");
                        String token = jsonObject.optString("csrf_token");
                        if (errorCode == 0) {     //获取token成功
//                            sToken = token;

                            SharedPreferences sharedPreferences = getSharedPreferences(SP_FILE_NAME, Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString(SP_KEY_TOKEN, token);
                            editor.commit();

                            logger.info(mInputUsernameEt.getText().toString()+" login success!  Version 1.3" );

                            LoginNetworkUtils.Param tokenParam = new LoginNetworkUtils.Param("csrf_token", token);
                            LoginNetworkUtils.Param emailParam = new LoginNetworkUtils.Param("email", mInputUsernameEt.getText().toString());
                            LoginNetworkUtils.Param passwordParam = new LoginNetworkUtils.Param("password", mInputPasswordEt.getText().toString());
                            LoginNetworkUtils.Param submitParam = new LoginNetworkUtils.Param("submit", "Login");
                            LoginNetworkUtils.Param nextParam = new LoginNetworkUtils.Param("next", "/lj/no_use");

                            LoginNetworkUtils.Param[] params = new LoginNetworkUtils.Param[]{tokenParam, emailParam, passwordParam, submitParam, nextParam};
                            String loginReturnJSON = LoginNetworkUtils.loginSync(getApplicationContext(), params);
                            Log.v(TAG, " onClickLogin loginReturnJSON : " + loginReturnJSON);
                            return new Result(0, "", loginReturnJSON);
                        } else {
                            mLoginButton.setClickable(true);
                            return new Result(-1, errorMsg, null);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, " ", e);
                        mLoginButton.setClickable(true);
                        return new Result(-1, e.getMessage(), null);
                    }
                }
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Result>() {
                    @Override
                    public void onCompleted() {
                        mLoginButton.setClickable(true);
                    }

                    @Override
                    public void onError(Throwable e) {
                            mLoginButton.setClickable(true);
                    }

                    @Override
                    public void onNext(Result result) {
                        if (result == null) {
                            Log.w(TAG, " onClickLogin result == null");
                        } else {
                            if (result.errorCode == 0) {

                                String loginReturnJSON = result.data;
                                try {
                                    JSONObject jo = new JSONObject(loginReturnJSON);
                                    JSONObject responseJSON = jo.getJSONObject("response");
                                    if (responseJSON != null) {
                                        if (responseJSON.has("errors")) {       //登陆有错误
                                            JSONObject errorsJSON = responseJSON.getJSONObject("errors");
                                            mLoginButton.setClickable(true);
                                            if (errorsJSON.has("password")) {
                                                Toast.makeText(LoginActivity.this, getString(R.string.login_failed_password_wrong), Toast.LENGTH_SHORT).show();
                                            } else if (errorsJSON.has("email")) {
                                                Toast.makeText(LoginActivity.this, getString(R.string.login_failed_user_noexist), Toast.LENGTH_SHORT).show();
                                            } else {
                                                Toast.makeText(LoginActivity.this, getString(R.string.login_failed), Toast.LENGTH_SHORT).show();
                                            }
                                        } else {
                                            Toast.makeText(LoginActivity.this, getString(R.string.login_success), Toast.LENGTH_SHORT).show();

                                            Intent intent = new Intent(LoginActivity.this, HouseInfoActivity.class);
                                            startActivity(intent);
                                            LoginActivity.this.finish();
                                        }
                                    } else {
                                        mLoginButton.setClickable(true);
                                        Toast.makeText(LoginActivity.this, getString(R.string.login_failed), Toast.LENGTH_SHORT).show();
                                    }
                                } catch (JSONException e) {
                                    Log.w(TAG, e);
                                    mLoginButton.setClickable(true);
                                    Toast.makeText(LoginActivity.this, getString(R.string.login_failed), Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                mLoginButton.setClickable(true);
                                Toast.makeText(LoginActivity.this, getString(R.string.login_failed), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
    }


}
