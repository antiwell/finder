package com.example.yong.report;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.example.yong.houseinfo.network.HouseInfoNetworkUtils;
import com.example.yong.houseinfo.ui.HouseInfoActivity;
import com.example.yong.network.cookies.OkHttp3CookieManager;
import com.example.yong.network.cookies.PersistentCookieStore;
import com.example.yong.yongshixiong.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.CookieManager;
import java.net.CookiePolicy;

import okhttp3.Call;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import static com.example.yong.constants.URLConstants.URL_HOST;
import static com.example.yong.constants.URLConstants.REPORT_URL_HOST;
import static com.example.yong.constants.URLConstants.URL_PATH_GET_TOKEN;
import static com.example.yong.constants.URLConstants.URL_PATH_REPORT_HOUSE_INFO;
import com.example.yong.login.ui.LoginActivity;

/**
 * Created by yaojian on 2017/7/23.
 */
public class ReportHouseInfoUtils {

    private final static String TAG = "ReportHouseInfoUtils";
    //private static Context  new_context = null;
    /**
     * 呼叫电话前 上报信息, 异步的
     *
     * @param phoneNumber
     */
    public static void reportHouseInfoBeforeCallAsync(final Context context,final String phoneNumber) {
        //new_context = context;
        if (TextUtils.isEmpty(phoneNumber)) {
            Log.w(TAG, " reportHouseInfoBeforeCallAsync phoneNumber isEmpty");

            return;
        }

        Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                try {
                    MultipartBody.Builder builder = new MultipartBody.Builder();
                    builder.setType(MultipartBody.FORM);
                    builder.addFormDataPart("data_id", phoneNumber);
                    builder.addFormDataPart("content", "andriod_pre_recall");

                    //OkHttpClient okHttpClient = new OkHttpClient();
                    OkHttpClient okHttpClient = new OkHttpClient.Builder().cookieJar(new OkHttp3CookieManager(context)).build();
                    final Request request = new Request.Builder().url(REPORT_URL_HOST + URL_PATH_REPORT_HOUSE_INFO).post(builder.build()).build();
                    Call call = okHttpClient.newCall(request);
                    Response response = call.execute();
                    if (response != null) {
                        subscriber.onNext(response.body().toString());
                    }
                } catch (Exception e) {
                    Log.e(TAG, " ", e);
                    subscriber.onError(e);
                    LoginActivity.logger.info("send id fail:"+phoneNumber);
                }

                subscriber.onCompleted();
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<String>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.w(TAG, e);
                    }

                    @Override
                    public void onNext(String result) {
                        if (TextUtils.isEmpty(result)) {
                            Log.w(TAG, " reportHouseInfoBeforeCallAsync onNext result is empty");
                        } else {
                            Log.d(TAG, " reportHouseInfoBeforeCallAsync result : " + result);
                        }
                    }
                });
    }

    /**
     * 呼叫电话前 上报信息, 异步的
     *
     * @param phoneNumber
     */
    public static void reportHouseInfoAfterCallAsync(final Context context, final String phoneNumber) {
        if (TextUtils.isEmpty(phoneNumber)) {
            Log.w(TAG, " reportHouseInfoBeforeCallAsync data_id isEmpty");
            LoginActivity.logger.info("get data id exception,id="+phoneNumber);
            return;
        }

        Observable.create(new Observable.OnSubscribe<Response>() {
            @Override
            public void call(Subscriber<? super Response> subscriber) {
                try {
                    MultipartBody.Builder builder = new MultipartBody.Builder();
                    builder.setType(MultipartBody.FORM);
                    builder.addFormDataPart("data_id", phoneNumber);
                    builder.addFormDataPart("content", "andriod_after_recall");

                    OkHttpClient okHttpClient = new OkHttpClient.Builder().cookieJar(new OkHttp3CookieManager(context)).build();
                    final Request request = new Request.Builder().url(REPORT_URL_HOST + URL_PATH_REPORT_HOUSE_INFO).post(builder.build()).build();
                    Call call = okHttpClient.newCall(request);
                    Response response = call.execute();
                    subscriber.onNext(response);
                    LoginActivity.logger.info("post success,id="+phoneNumber);
                } catch (Exception e) {
                    LoginActivity.logger.info("post exception,id="+phoneNumber);
                    Log.w(TAG, e);
                    subscriber.onError(e);
                }

                subscriber.onCompleted();
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Response>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.w(TAG, e);
                    }

                    @Override
                    public void onNext(Response result) {
                        if(result == null){
                            Log.w(TAG, " reportHouseInfoBeforeCallAsync onNext result == null");
                        }else{
                            if(result.isSuccessful()){
                                Log.w(TAG, " reportHouseInfoBeforeCallAsync onNext success");
                            }else{
                                Log.w(TAG, " reportHouseInfoAfterCallAsync onNext failed : " + result.message());
                                LoginActivity.logger.info("post after exception,err="+result.message());
                            }
                        }
                    }
                });
    }

}
