package com.example.yong.login.network;

import android.content.Context;
import android.util.Log;

import com.example.yong.login.ui.LoginActivity;
import com.example.yong.network.cookies.OkHttp3CookieManager;
import com.example.yong.network.cookies.PersistentCookieStore;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.net.URLEncoder;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.example.yong.constants.URLConstants.URL_HOST;
import static com.example.yong.constants.URLConstants.URL_PATH_GET_TOKEN;
import static com.example.yong.constants.URLConstants.URL_PATH_LOGIN;


public class LoginNetworkUtils {

    private final static String TAG = "LoginNetworkUtils";

    /**
     * 同步获取token
     * @return
     */
    public static String getTokenSync(Context context){

        //先清空cookie
        OkHttp3CookieManager okHttp3CookieManager = new OkHttp3CookieManager(context);
        okHttp3CookieManager.removeAllCookies();

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                                .cookieJar(new OkHttp3CookieManager(context))
                                .build();

        final Request request = new Request.Builder().url(URL_HOST + URL_PATH_GET_TOKEN).header("Accept-Encoding", "identity").build();
        Call call = okHttpClient.newCall(request);
        try{
            Response response = call.execute();
            if(response != null){
                return response.body().string();
            }
        }catch (Exception e){
            Log.e(TAG, " ", e);
        }


        return null;
    }

    public static String loginSyncByGetMethod(Context context, String token, String email, String passord, String submit){
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("?");
            sb.append(String.format("%s=%s", "csrf_token", URLEncoder.encode(token, "utf-8")));
            sb.append("&");
            sb.append(String.format("%s=%s", "email", URLEncoder.encode(email, "utf-8")));
            sb.append("&");
            sb.append(String.format("%s=%s", "password", URLEncoder.encode(passord, "utf-8")));
            sb.append("&");
            sb.append(String.format("%s=%s", "submit", URLEncoder.encode(submit, "utf-8")));
            sb.append("&");
            sb.append(String.format("%s=%s", "next", URLEncoder.encode("/lj/no_use", "utf-8")));

            String requestUrl = URL_HOST + URL_PATH_LOGIN + sb.toString();

            Log.v(TAG, " loginSyncByGetMethod requestUrl : " + requestUrl);

            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                                .cookieJar(new OkHttp3CookieManager(context))
                                .build();
            final Request request = new Request.Builder().url(requestUrl).build();
            Call call = okHttpClient.newCall(request);
            Response response = call.execute();
            if (response != null) {
                return response.body().string();
            }
        }catch (Exception e){
            Log.e(TAG, " ", e);
        }

        return null;
    }

    /**
     * 同步 登陆
     * @return
     */
    public static String loginSync(Context context, Param[] params){
//        FormEncodingBuilder builder = new FormEncodingBuilder();
//        if(params != null){
//            for(int i = 0; i < params.length; ++i){
//                Log.v(TAG, " key==" + params[i].key + " value==" + params[i].value);
//                builder.add(params[i].key, params[i].value);
//            }
//        }

        JSONObject jsonObject = new JSONObject();
        try{
            for(int i = 0; i < params.length; ++i){
                Log.v(TAG, " key==" + params[i].key + " value==" + params[i].value);
                jsonObject.put(params[i].key, params[i].value);
            }
        }catch (JSONException e){
            Log.w(TAG, e);
        }

        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonObject.toString());

        Request request = new Request.Builder()
                .url(URL_HOST + URL_PATH_LOGIN)
                .header("Accept-Encoding", "identity")
                .post(requestBody)
                .build();

        Log.v(TAG, " loginSync " + request.toString());

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                        .cookieJar(new OkHttp3CookieManager(context))
                        .build();
        Call call = okHttpClient.newCall(request);
        try{
            Response response = call.execute();
            if(response != null){
                return response.body().string();
            }
        }catch (Exception e){
            Log.e(TAG, " ", e);
        }

        return null;
    }

    private static Param[] validateParam(Param[] params)
    {
        if (params == null)
            return new Param[0];
        else return params;
    }

    private static String guessMimeType(String path)
    {
        FileNameMap fileNameMap = URLConnection.getFileNameMap();
        String contentTypeFor = fileNameMap.getContentTypeFor(path);
        if (contentTypeFor == null)
        {
            contentTypeFor = "application/octet-stream";
        }
        return contentTypeFor;
    }

    public static class Param
    {
        public Param()
        {
        }

        public Param(String key, String value)
        {
            this.key = key;
            this.value = value;
        }

        String key;
        String value;
    }

}
