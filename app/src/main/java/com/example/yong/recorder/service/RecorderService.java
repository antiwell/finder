package com.example.yong.recorder.service;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.example.yong.recorder.upload.RecorderUploader;
import com.example.yong.report.ReportHouseInfoUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.FileInputStream;
import java.io.FileOutputStream;


import com.example.yong.login.ui.LoginActivity;

/**
 * 实现录音的service
 * Created by yaojian on 2017/8/15.
 */
public class RecorderService extends Service {

    private final static String TAG = "RecorderService";

    private MediaRecorder recorder; //录音的一个实例

    private MyListener mMyListener;
    private String data_id = "10000";
    private String phone_number = "";

    @Override
    public IBinder onBind(Intent intent) {
        return new Binder();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, " yaoTest onCreate ");

        mMyListener = new MyListener();

        //监听电话
        TelephonyManager tm= (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        tm.listen(mMyListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.i(TAG, " yaoTest onStartCommand");
        data_id = intent.getStringExtra("data_id");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.i(TAG, " yaoTest onDestroy");
        if (mMyListener != null) {
            TelephonyManager telephony = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            telephony.listen(mMyListener, PhoneStateListener.LISTEN_NONE);
        }
    }

    class  MyListener extends PhoneStateListener{

        private boolean isRecording = false;

        private String mPhoneNumber = "";

        //保存录音文件path
        private String mSaveFilePath = "";

        //在电话状态改变的时候调用
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            switch (state){
                case TelephonyManager.CALL_STATE_IDLE:
                    Log.i(TAG, "yaoTest MyListener TelephonyManager.CALL_STATE_IDLE ");
                    LoginActivity.logger.info("Recorder RecorderService CALL_STATE_IDLE");
                    if(TextUtils.isEmpty(data_id))
                    {
                        LoginActivity.logger.info("data_id is empty,CALL_STATE_IDLE");
                        return;
                    }
                    //空闲状态
                    try{
                        if (recorder != null && isRecording) {
                            recorder.stop();//停止录音
                            recorder.release();//释放资源
                        }
                        LoginActivity.logger.info("Recorder RecorderService report after,id="+data_id);
                        //结束电话后, report房间信息
                        ReportHouseInfoUtils.reportHouseInfoAfterCallAsync(RecorderService.this, data_id);

                        //开始上传录音
                        RecorderUploader mUploader = new RecorderUploader(new RecorderUploader.RecorderUploadListener() {
                            @Override
                            public void onUploadFailed(String fileName, int errorCode, String errorStr, IOException e) {
                                Log.w(TAG, e);
                                Log.i(TAG, " yaoTest onUploadFailed " + fileName + " errorCode == " + errorCode + " errorStr " + errorStr);
                                LoginActivity.logger.info(" yaoTest onUploadFailed " + fileName + " errorCode == " + errorCode + " errorStr " + errorStr+" exception:"+e.toString());
                            }

                            @Override
                            public void onUploadSuccess(String fileName) {
                                Log.i(TAG, " yaoTest onUploadSuccess " + fileName);
                                LoginActivity.logger.info(" yaoTest onUploadSuccess " + fileName);
                                if(fileName.indexOf("txt")>0)
                                {
                                    File file = new File(fileName);
                                    if (file.isFile() && file.exists()) {
                                         file.delete();
                                    }
                                }
                            }
                        });
                        LoginActivity.logger.info("Recorder RecorderService post file,name="+mSaveFilePath);

                        TelephonyManager tm = (TelephonyManager) ((getApplicationContext()).getSystemService(Activity.TELEPHONY_SERVICE));
                        String id = tm.getDeviceId();
                        String currentTime = getCurrentTime();
                        String logFilePath=Environment.getExternalStorageDirectory().getAbsolutePath() + "/recorder/"+id+"_log4j.txt";

                        mUploader.startPostFileAsync(RecorderService.this, RecorderUploader.RECORDER_POST_URL, mSaveFilePath);
                        Log.i(TAG, " yaoTest upload file: " + mSaveFilePath);
                        //fileCopy(logFilePath,logFilePath+"_1");
                        //mUploader.startPostFileAsync(RecorderService.this,RecorderUploader.RECORDER_POST_URL,logFilePath+"_1");
                        mUploader.startPostFileAsync(RecorderService.this,RecorderUploader.RECORDER_POST_URL,logFilePath);
                        Log.i(TAG, " yaoTest upload file: " + logFilePath);


                    }catch(Throwable e){
                        Log.w(TAG, e);
                    }finally {
                        recorder = null;
                        isRecording = false;
                        mPhoneNumber = "";
                        mSaveFilePath = "";
                        data_id="";
                    }
                    break;

                case TelephonyManager.CALL_STATE_RINGING:
                    LoginActivity.logger.info("Recorder RecorderService CALL_STATE_RINGING");
                    Log.i(TAG, " yaoTest MyListener TelephonyManager.CALL_STATE_RINGING");
//                    prepareRecorder(incomingNumber);
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    Log.i(TAG, " yaoTest MyListener TelephonyManager.CALL_STATE_OFFHOOK");
                    LoginActivity.logger.info("Recorder RecorderService CALL_STATE_OFFHOOK,phone num:"+incomingNumber);
                    if(TextUtils.isEmpty(data_id))
                    {
                        LoginActivity.logger.info("data_id is empty,CALL_STATE_OFFHOOK");
                        return;
                    }
                    prepareRecorder(incomingNumber);


                    if(!isRecording){
                        try{
                            if (recorder != null){
                                recorder.prepare();//准备录音
                                recorder.start(); //接听的时候开始录音
                                isRecording = true;
                                LoginActivity.logger.info("Recorder RecorderService begin recording");
                            }
                        }catch (Throwable e){
                            Log.w(TAG, e);
                        }
                    }

                    break;
            }
        }

        /**
         * 准备录音
         */
        private void prepareRecorder(String phoneNumber){

            if(TextUtils.isEmpty(mPhoneNumber)) {
                mPhoneNumber = phoneNumber;
            }

            mSaveFilePath = getFilePathToSaveRecorder(phoneNumber);

            if (recorder == null && !isRecording){
                try{
                    recorder=new MediaRecorder();//初始化录音对象
                    recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION);//设置录音的输入源(麦克)
                    recorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);//设置音频格式(3gp)
                    recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);//设置音频编码
                    recorder.setOutputFile(mSaveFilePath); //设置录音保存的文件
                }catch (Throwable e) {
                    Log.w(TAG, e);
                }
            }
        }
        public  boolean fileCopy(String oldFilePath,String newFilePath) throws IOException {
            //如果原文件不存在
            if(fileExists(oldFilePath) == false){
                return false;
            }
            //获得原文件流
            FileInputStream inputStream = new FileInputStream(new File(oldFilePath));
            byte[] data = new byte[1024];
            //输出流
            FileOutputStream outputStream =new FileOutputStream(new File(newFilePath));
            //开始处理流
            while (inputStream.read(data) != -1) {
                outputStream.write(data);
            }
            inputStream.close();
            outputStream.close();
            return true;
        }
        public  boolean fileExists(String filePath) {
            File file = new File(filePath);
            return file.exists();
        }

        /**
         * 得到 录音文件
         * @return
         */
        private String getFilePathToSaveRecorder(String phonenumber){
            createRecorderFile();
            String currentTime = getCurrentTime();
          //  return DIR_PATH + "/" + phonenumber + "_" + currentTime + ".amr";
            return DIR_PATH + "/" + data_id + "_" +phonenumber+"_"+ currentTime + ".amr";
        }

        private final String DIR_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/recorder";

        //创建保存录音的目录
        private void createRecorderFile() {
            File file=new File(DIR_PATH);
            if (!file.exists()){
                file.mkdir();
            }
        }
        //获取当前时间，以其为名来保存录音
        private String getCurrentTime(){
            SimpleDateFormat format=new SimpleDateFormat("yyyyMMddHHmmss");
            Date date=new Date();
            String str=format.format(date);
            return str;
        }

    }
    public class Binder extends android.os.Binder{
        public void setData(String data, String phonenumber){
            RecorderService.this.data_id = data;
            RecorderService.this.phone_number = phonenumber;
        }
        public RecorderService getService(){
            return RecorderService.this;
        }
    }
    private CallBack callback = null;

    public void setCallback(CallBack callback) {
        this.callback = callback;
    }

    public CallBack getCallback() {
        return callback;
    }

    public static interface CallBack{
        void onDataChange(String data);
    }
}