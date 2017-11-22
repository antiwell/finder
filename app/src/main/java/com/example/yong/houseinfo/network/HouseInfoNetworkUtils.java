package com.example.yong.houseinfo.network;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.example.yong.constants.SPConstants;
import com.example.yong.login.ui.LoginActivity;
import com.example.yong.network.cookies.OkHttp3CookieManager;
import com.example.yong.network.cookies.PersistentCookieStore;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URLEncoder;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.example.yong.constants.URLConstants.URL_HOST;
import static com.example.yong.constants.URLConstants.URL_PATH_GET_HOUSE_INFO;

public class HouseInfoNetworkUtils {

    private final static String TAG = "HouseInfoNetworkUtils";

    /**
     * 获取 房屋信息
     *
     * @return
     */
    public static String getHouseInfoSync(Context context, long lastId) {
        if (lastId < 0) {
            lastId = 0;
        }
        Log.i(TAG, "getHouseInfoSync lastId == " + lastId);
        try {

            String token = context.getSharedPreferences(SPConstants.SP_FILE_NAME, Context.MODE_PRIVATE).getString(SPConstants.SP_KEY_TOKEN, "");
            if(TextUtils.isEmpty(token)){
                LoginActivity.logger.info("token is empty ");
                return null;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("?");
            sb.append(String.format("%s=%d", "last_id", lastId));
            sb.append("&");
            sb.append(String.format("%s=%s", "csrf_token", URLEncoder.encode(token, "utf-8")));

            String requestUrl = URL_HOST + URL_PATH_GET_HOUSE_INFO + sb.toString();

            Log.v(TAG, " getHouseInfoSync requestUrl : " + requestUrl);

            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                                .cookieJar(new OkHttp3CookieManager(context))
                                .build();
            final Request request = new Request.Builder().url(requestUrl).build();
            Call call = okHttpClient.newCall(request);
            Response response = call.execute();
            if (response != null) {
                return response.body().string();
            }
        } catch (Exception e) {
            Log.e(TAG, " ", e);
            LoginActivity.logger.info("get house interface failed "+e.getMessage());
        }

        return null;
    }

}
