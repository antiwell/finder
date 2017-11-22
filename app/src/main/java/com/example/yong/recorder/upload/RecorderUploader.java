package com.example.yong.recorder.upload;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.example.yong.login.ui.LoginActivity;
import com.example.yong.network.cookies.OkHttp3CookieManager;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import static com.example.yong.constants.URLConstants.URL_HOST;
import static com.example.yong.constants.URLConstants.URL_PATH_REPORT_HOUSE_INFO;

/**
 * 用于上传文件
 * Created by yaojian on 2017/8/27.
 */
public class RecorderUploader {

    private final static String TAG = "RecorderUploader";

    public final static String RECORDER_POST_URL = "http://pay.antiwell.com/lj/upload_voice";

    private  RecorderUploadListener mListener = null;

    public RecorderUploader(RecorderUploadListener listener){
        mListener = listener;
    }

    /**
     * 异步的 post文件
     * @param url
     * @param filePath
     */
    public void startPostFileAsync(final Context context, final String url, final String filePath){
        if(TextUtils.isEmpty(url)){
            Log.i(TAG, " yaoTest startPostFileAsync url is null");
            LoginActivity.logger.info("url is emtpty");
            return;
        }

        if(TextUtils.isEmpty(filePath) || !(new File(filePath).exists())){
            Log.i(TAG, " yaoTest startPostFileAsync filePath is null");
            return;
        }

        Log.i(TAG, " yaoTest startPostFileAsync filePath : " + filePath);

        Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                startPostFileSync(context, url, filePath);
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
                    }
                });
    }


    /**
     * 同步的 post文件
     */
    public void startPostFileSync(Context context, String url, final String filePath){
        if(TextUtils.isEmpty(url)){
            Log.w(TAG, " startPostFileSync url is null");
            return;
        }
        //创建File
        File file = new File(filePath);
        //创建RequestBody
//        RequestBody fileBody = RequestBody.create(MediaType.parse("application/octet-stream"), file);
//        RequestBody requestBody = new MultipartBody.Builder().addFormDataPart("filename", file.getName(), fileBody).build();
//        Log.i(TAG, " startPostFileSync " + requestBody.toString());

        MultipartBody.Builder builder = new MultipartBody.Builder();
        builder.setType(MultipartBody.FORM);
        Log.i(TAG, " startPostFileSync file.getName() : " + file.getName());
        String fileName = filePath.substring(filePath.lastIndexOf("/"));
        Log.i(TAG, " startPostFileSync fileName " + fileName);
        builder.addFormDataPart("file", fileName, RequestBody.create(MediaType.parse("multipart/form-data"), file));
        RequestBody requestBody = builder.build();

        //创建Request
        final Request request = new Request.Builder().url(url).post(requestBody).build();
        OkHttpClient okHttpClient = new OkHttpClient.Builder().cookieJar(new OkHttp3CookieManager(context)).connectTimeout(10000, TimeUnit.MILLISECONDS)
                .readTimeout(10000,TimeUnit.MILLISECONDS)
                .writeTimeout(10000,TimeUnit.MILLISECONDS).build();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.w(TAG, e);
                if(mListener != null){
                    Log.i(TAG, " yaoTest callback onFailure");
                    mListener.onUploadFailed(filePath, -1, "", e);

                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(response.isSuccessful()){    //上传成功
                    if(mListener != null){
                        mListener.onUploadSuccess(filePath);
                    }
                }else{
                    if(mListener != null){
                        Log.i(TAG, " yaoTest onResponse " + response.toString());
                        mListener.onUploadFailed(filePath, -1, response.message(), null);
                    }
                }
            }
        });
    }

    public static interface RecorderUploadListener{

        public void onUploadFailed(String fileName, int errorCode, String errorStr, IOException e);

        public void onUploadSuccess(String fileName);
    }
}
