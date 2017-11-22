package com.example.yong.network.cookies;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.List;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

/**
 * Created by yaojian on 2017/8/28.
 */

public class OkHttp3CookieManager implements CookieJar {

    private final PersistentOkHttp3CookieStore cookieStore;

    public OkHttp3CookieManager(Context context){
        cookieStore = new PersistentOkHttp3CookieStore(context);
    }

    @Override
    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        if (cookies != null && cookies.size() > 0) {
            for (Cookie item : cookies) {
                cookieStore.add(url, item);
            }
        }
    }

    @Override
    public List<Cookie> loadForRequest(HttpUrl url) {
        List<Cookie> cookies = cookieStore.get(url);
        return cookies;
    }

    /**
     * 清空所有的cookies
     */
    public boolean removeAllCookies(){
        if(cookieStore != null){
            return cookieStore.removeAll();
        }

        return false;
    }
}
