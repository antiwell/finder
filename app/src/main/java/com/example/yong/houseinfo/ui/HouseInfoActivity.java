package com.example.yong.houseinfo.ui;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.example.yong.callstate.PhoneStateReceiver;
import com.example.yong.houseinfo.network.HouseInfoNetworkUtils;
import com.example.yong.recorder.service.RecorderService;
import com.example.yong.report.ReportHouseInfoUtils;
import com.example.yong.yongshixiong.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.SoftReference;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import android.content.ComponentName;
import android.os.IBinder;
import android.content.ServiceConnection;
import de.mindpipe.android.logging.log4j.LogConfigurator;
import com.example.yong.login.ui.LoginActivity;

public class HouseInfoActivity extends AppCompatActivity implements ServiceConnection  {
    private final static String TAG = "HouseInfoActivity";

    private MainHandler mMainHander;

    private long mLastId = 0;
    private final static String SP_FILE = "YongShiXiong";
    private final static String SP_KEY_LAST_ID = "lastId";
    private double mLatency = 0;

    private HouseData mHouseData = new HouseData();

    private boolean isDialing = false;      //是否正在 呼出电话
    private int mCallTimes = 0;

    private TextView mStatusTv;
    private TextView mTitleTv;
    private TextView mNameTv;
    private TextView mPriceTv;
    private TextView mPhoneNumberTv;
    private TextView mTimeTv;

    private TextView mRequestTimesTv;

    private CallPhoneStateListener mCallPhoneStateListener = null;

    private final static int CALL_STATUS_IDLEING_INT = 0;
    private final static String CALL_STATUS_IDLEING_STR = "空闲中...";
    private final static int CALL_STATUS_UPDATING_DATA_INT = 1;
    private final static String CALL_STATUS_UPDATING_DATA_STR = "数据更新中...";
    private final static int CALL_STATUS_DIALING_INT = 2;
    private final static String CALL_STATUS_DIALING_STR = "拨号中...";
    private final static int CALL_STATUS_NO_NEWEST_INFO_INT = 3;
    private final static String CALL_STATUS_NO_NEWEST_INFO_STR = "没有新数据返回...";

    private PhoneStateReceiver mPhoneStateReceiver;

    private RecorderService.Binder binder = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //LogConfigurator logConfigurator = new LogConfigurator();

        setContentView(R.layout.activity_autodial);
        Log.i(TAG, " onCreate");
        mStatusTv = (TextView) findViewById(R.id.status);
        mTitleTv = (TextView) findViewById(R.id.title);
        mNameTv = (TextView) findViewById(R.id.name);
        mPriceTv = (TextView) findViewById(R.id.price);
        mPhoneNumberTv = (TextView) findViewById(R.id.phone_number);
        mTimeTv = (TextView) findViewById(R.id.time);

        mRequestTimesTv = (TextView) findViewById(R.id.times);

        //监听电话
       // TelephonyManager tm= (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
       // tm.listen(CallPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);

        mCallPhoneStateListener = new CallPhoneStateListener(this);

        TelephonyManager telephony = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        telephony.listen(mCallPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);


        Log.i(TAG, " onCreate startService begin");
        Intent intent = new Intent(this, RecorderService.class);
        startService(intent);
        Log.i(TAG, " onCreate startService end");

        //bindService(intent,myServiceConn,Context.BIND_AUTO_CREATE);
        bindService(new Intent(this,RecorderService.class),this, Context.BIND_AUTO_CREATE);

        SharedPreferences sharedPreferences = getSharedPreferences(SP_FILE, Context.MODE_PRIVATE);
        mLastId = sharedPreferences.getLong(SP_KEY_LAST_ID, 0);
        mMainHander = new MainHandler(Looper.getMainLooper(), this);
        mMainHander.sendEmptyMessage(MSG_START_GET_HOUSE_INFO);
        Message msg = mMainHander.obtainMessage(MSG_FRESH_CALL_STATUS);
        msg.arg1 = CALL_STATUS_IDLEING_INT;
        mMainHander.sendMessage(msg);
    }

    private void handleFreshViews() {
        if (mHouseData != null) {
            mTitleTv.setText(getResources().getString(R.string.dial_title) + mHouseData.title);
            mNameTv.setText(getResources().getString(R.string.dial_name) + mHouseData.peopleName);
            mPriceTv.setText(getResources().getString(R.string.dial_price) + mHouseData.price);
            mPhoneNumberTv.setText(getResources().getString(R.string.dial_phone_number) + mHouseData.phoneNumber);
            mTimeTv.setText(getResources().getString(R.string.dial_time) + mHouseData.createTime);
        }
    }

    private void handleFreshCallTimes() {
        if (mCallTimes < 0) {
            mCallTimes = 0;
        }
        mRequestTimesTv.setText(getResources().getString(R.string.request_times) + mCallTimes);
    }

    private void handleFreshCallStatus(int status) {
        Log.i(TAG, " handleFreshCallStatus status == " + status);
        String str = "";
        switch (status) {
            case CALL_STATUS_IDLEING_INT:
                str = CALL_STATUS_IDLEING_STR;
                break;

            case CALL_STATUS_DIALING_INT:
                str = CALL_STATUS_DIALING_STR;
                break;

            case CALL_STATUS_UPDATING_DATA_INT:
                str = CALL_STATUS_UPDATING_DATA_STR;
                break;

            case CALL_STATUS_NO_NEWEST_INFO_INT:
                str = CALL_STATUS_NO_NEWEST_INFO_STR;
                break;
        }

        if (mStatusTv != null) {
            mStatusTv.setText(str);
        }
    }

    private void handleMsgStartGetHouseInfo() {
        Observable.create(new Observable.OnSubscribe<HouseData>() {
            @Override
            public void call(Subscriber<? super HouseData> subscriber) {
                {
                    if(mMainHander != null) {
                        Message msg = mMainHander.obtainMessage(MSG_FRESH_CALL_STATUS);
                        msg.arg1 = CALL_STATUS_UPDATING_DATA_INT;
                        mMainHander.sendMessage(msg);
                        //LoginActivity.logger.info("call handleMsgStartGetHouseInfo and send CALL_STATUS_UPDATING_DATA_INT");
                    }
                }
                try {
                    String s = HouseInfoNetworkUtils.getHouseInfoSync(getApplicationContext(), mLastId);
                    if (TextUtils.isEmpty(s)) {
                        subscriber.onError(new Throwable("getHouseInfo error"));
                        subscriber.onCompleted();
                        LoginActivity.logger.info("get houser info error");
                        return;
                    } else {
                        Log.v(TAG, s);
                        HouseData houseData = null;
                        JSONObject jsonObject = new JSONObject(s);
                        int errorCode = jsonObject.optInt("err_code");
                        String msg = jsonObject.optString("reason");
                        if (errorCode == 0) {
                            mLatency = jsonObject.optDouble("latency");
                            JSONArray data = jsonObject.optJSONArray("data");
                            int length = data.length();
                            long maxId = -1;
                            //查找最大的id

                            for (int i = 0; i < length; ++i) {
                                JSONObject job = data.getJSONObject(i);
                                long idTmp = job.optLong("id", -1);
                                if (idTmp > maxId) {
                                    Log.i(TAG, " idTmp==maxId");
                                    maxId = idTmp;
                                    houseData = new HouseData();
                                    houseData.title = job.optString("title");
                                    houseData.id = idTmp;
                                    houseData.createTime = job.optString("create_time");
                                    houseData.peopleName = job.optString("people_name");
                                    houseData.price = job.optString("price");
                                    houseData.url = job.optString("url");
                                    houseData.phoneNumber = job.optString("phone");
                                }
                            }

                            if (maxId <= mLastId) {
                                if (mMainHander != null) {
                                    Message mes = mMainHander.obtainMessage(MSG_FRESH_CALL_STATUS);
                                    mes.arg1 = CALL_STATUS_NO_NEWEST_INFO_INT;
                                    mMainHander.sendMessage(mes);
                                }
                                Log.i(TAG, " handleMsgStartGetHouseInfo maxId <= mLastId, maxId == " + maxId + " mLastId == " + mLastId);
                                subscriber.onError(new Throwable("maxId <= mLastId, maxId == " + maxId + " mLastId == " + mLastId));
                                subscriber.onCompleted();
                                return;
                            } else {
                                mLastId = maxId;
                                SharedPreferences.Editor editor = getSharedPreferences(SP_FILE, Context.MODE_PRIVATE).edit();
                                editor.putLong(SP_KEY_LAST_ID, mLastId);
                                editor.commit();

                                subscriber.onNext(houseData);
                                subscriber.onCompleted();
                                Log.i(TAG, " idTmp==maxId==error");

                                return;
                            }
                        } else {
                            subscriber.onError(new Throwable(msg));
                            subscriber.onCompleted();
                            Log.i(TAG, " idTmp==maxId==return");
                            return;
                        }
                    }
                }catch(JSONException e){
                    subscriber.onError(e);
                    subscriber.onCompleted();
                    Log.i(TAG, " idTmp==maxId==exception");
                    return;
                }
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<HouseData>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        if (mMainHander != null) {
                            mMainHander.sendEmptyMessageDelayed(MSG_START_GET_HOUSE_INFO, 500);
                        }
                    }

                    @Override
                    public void onNext(HouseData result) {
                        if (result == null) {     //一秒后重试
                            Log.i(TAG, "result==null");
                            if (mMainHander != null) {
                                mMainHander.sendEmptyMessageDelayed(MSG_START_GET_HOUSE_INFO, 500);
                            }
                        } else {
                            Log.i(TAG, "resul==not==null");
                            if (mHouseData.equals(result)) {    //没有新数据， 一秒后重试
                                if (mMainHander != null) {
                                    mMainHander.sendEmptyMessageDelayed(MSG_START_GET_HOUSE_INFO, (int) (mLatency) * 1000);
                                    Log.i(TAG, "MSG_START_GET_HOUSE_INFO");
                                }
                            } else {      //有新数据
                                mHouseData = result;
                                Log.i(TAG, "new==data");
                                if (mMainHander != null) {
                                    mMainHander.sendEmptyMessage(MSG_FRESH_HOUSE_INFO);
                                }

                                if (isDialing) {      //正在呼叫
                                    Log.i(TAG, " onNext=isDialing");
                                } else {
                                    Log.i(TAG, "getApplicationContext");
                                    if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
                                    {
                                        LoginActivity.logger.info("get new data,id="+mHouseData.id+",tel="+mHouseData.phoneNumber);
                                        Log.i(TAG, " if getApplicationContext");

                                    //呼叫
                                    final Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + mHouseData.phoneNumber));
                                    // intent.putExtra("data_id",result.id.toString());
                                    //  final Intent new_Intent = new Intent(HouseInfoActivity.this,RecorderService.class);
                                    //  new_Intent.putExtra("data_id",String.valueOf(result.id));
                                    //  startService(new_Intent);
                                    binder.setData(String.valueOf(result.id), result.phoneNumber);
                                    // intent.putExtra("data_id",String.valueOf(result.id));
                                    if (intent.resolveActivity(getPackageManager()) != null) {
                                        mCallTimes++;
                                        if (mMainHander != null) {
                                            mMainHander.sendEmptyMessage(MSG_FRESH_CALL_TIMES);
                                        }

                                        if (mMainHander != null) {
                                            Message msg = mMainHander.obtainMessage(MSG_FRESH_CALL_STATUS);
                                            msg.arg1 = CALL_STATUS_DIALING_INT;
                                            mMainHander.sendMessage(msg);
                                        }

                                        try {
                                            startActivity(intent);
                                            isDialing = true;
                                            LoginActivity.logger.info("send report id:"+mHouseData.id);
                                            //发送report信息
                                            ReportHouseInfoUtils.reportHouseInfoBeforeCallAsync(getApplicationContext(),String.valueOf(mHouseData.id));

                                        } catch (Throwable e) {
//                                            Log.w(TAG, e);
                                            Log.i(TAG, " handleMsgStartGetHouseInfo " + e.getMessage());
                                            isDialing = false;
                                            Toast.makeText(HouseInfoActivity.this, R.string.can_not_dial, Toast.LENGTH_SHORT).show();
                                        }
                                    } else {
                                        Log.w(TAG, "can not resolveActivity intent : " + intent);
                                        Toast.makeText(HouseInfoActivity.this, R.string.can_not_dial, Toast.LENGTH_SHORT).show();
                                    }
                                }
                                }

                                //继续拉取数据
                                if (mMainHander != null) {
                                    mMainHander.sendEmptyMessageDelayed(MSG_START_GET_HOUSE_INFO, (int) (mLatency) * 1000);
                                }
                            }
                        }
                    }
                });
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        binder = (RecorderService.Binder) iBinder;
        binder.getService().setCallback(new RecorderService.CallBack() {
            @Override
            public void onDataChange(String data) {
                Message msg = new Message();
                Bundle b = new Bundle();
                b.putString("data_id",data);
                msg.setData(b);
                handler.sendMessage(msg);
            }
        });
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {

    }

    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            //tvOut.setText(msg.getData().getString("data"));


        }
    };




    @Override
    public void onStart() {
        super.onStart();

    }

    @Override
    public void onStop() {
        super.onStop();

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.v(TAG, " yaoTest onPause");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mCallPhoneStateListener != null) {
            TelephonyManager telephony = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            telephony.listen(mCallPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        }

        Intent intent = new Intent(this, RecorderService.class);
        stopService(intent);

        if (mMainHander != null) {
            mMainHander.removeCallbacksAndMessages(null);
            mMainHander = null;
        }
    }

    private final static int MSG_START_GET_HOUSE_INFO = 101;
    private final static int MSG_FRESH_HOUSE_INFO = 102;
    private final static int MSG_FRESH_CALL_TIMES = 103;
    private final static int MSG_FRESH_CALL_STATUS = 104;

    private final static class MainHandler extends Handler {
        private SoftReference<Context> mContextRef;

        public MainHandler(Looper looper, Context context) {
            super(looper);
            if (context != null) {
                mContextRef = new SoftReference<Context>(context);
            }
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_START_GET_HOUSE_INFO:
                    //LoginActivity.logger.info("MSG_START_GET_HOUSE_INFO");
                    if (mContextRef != null && mContextRef.get() != null && mContextRef.get() instanceof HouseInfoActivity) {
                        HouseInfoActivity activity = (HouseInfoActivity) mContextRef.get();
                        activity.handleMsgStartGetHouseInfo();
                       // LoginActivity.logger.info("MSG_START_GET_HOUSE_INFO 1");
                    }
                    break;

                case MSG_FRESH_HOUSE_INFO:
                    //LoginActivity.logger.info("MSG_FRESH_HOUSE_INFO");
                    if (mContextRef != null && mContextRef.get() != null && mContextRef.get() instanceof HouseInfoActivity) {
                        HouseInfoActivity activity = (HouseInfoActivity) mContextRef.get();
                        activity.handleFreshViews();
                      //  LoginActivity.logger.info("MSG_FRESH_HOUSE_INFO 1");
                    }
                    break;

                case MSG_FRESH_CALL_TIMES:
                    //LoginActivity.logger.info("MSG_FRESH_CALL_TIMES");
                    if (mContextRef != null && mContextRef.get() != null && mContextRef.get() instanceof HouseInfoActivity) {
                        HouseInfoActivity activity = (HouseInfoActivity) mContextRef.get();
                        activity.handleFreshCallTimes();
                      //  LoginActivity.logger.info("MSG_FRESH_CALL_TIMES 1");
                    }
                    break;

                case MSG_FRESH_CALL_STATUS:
                    //LoginActivity.logger.info("MSG_FRESH_CALL_STATUS");
                    if (mContextRef != null && mContextRef.get() != null && mContextRef.get() instanceof HouseInfoActivity) {
                        HouseInfoActivity activity = (HouseInfoActivity) mContextRef.get();
                        activity.handleFreshCallStatus(msg.arg1);
                        //LoginActivity.logger.info("MSG_FRESH_CALL_STATUS 1");
                    }
                    break;

            }
        }
    }

    private final static class HouseData {
        public String createTime;
        public long id;
        public String peopleName;
        public String phoneNumber;
        public String price;
        public String title;
        public String url;

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }

            if (this == obj) {
                return true;
            }

            if (!(obj instanceof HouseData)) {
                return false;
            }

            HouseData tmp = (HouseData) obj;
            if (id != tmp.id) {
                return false;
            }

            return true;

        }
    }

    private static   class CallPhoneStateListener extends PhoneStateListener {

        private SoftReference<Context> mContextRef;

        private CallPhoneStateListener(Context context) {
            if (context != null) {
                mContextRef = new SoftReference<Context>(context);
            }
        }

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            Log.i(TAG, "onCallStateChanged_Fun");
            HouseInfoActivity activity = null;
            if (mContextRef != null && mContextRef.get() != null && mContextRef.get() instanceof HouseInfoActivity) {
                activity = (HouseInfoActivity) mContextRef.get();
            }


            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:

                    Log.i(TAG, " yaoTest TelephonyManager.CALL_STATE_RINGING : " + incomingNumber);
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    if (activity != null) {
                        Log.i(TAG, " yaoTest TelephonyManager.CALL_STATE_IDLE activity.mCallTimes : " + activity.mCallTimes);
                        Log.i(TAG, " yaoTest TelephonyManager.CALL_STATE_IDLE : " + incomingNumber);
                        activity.isDialing = false;
                        Log.w(TAG, " isDialing==false");
                        //结束电话后, report房间信息
//                        ReportHouseInfoUtils.reportHouseInfoAfterCallAsync(activity, activity.mHouseData.phoneNumber);
                    } else {
                        Log.w(TAG, " activity == null");
                    }
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    Log.i(TAG, " yaoTest TelephonyManager.CALL_STATE_OFFHOOK : " + incomingNumber);
                    break;
                default:
                    Log.i(TAG,"default_result");
            }

        }
    }


}
