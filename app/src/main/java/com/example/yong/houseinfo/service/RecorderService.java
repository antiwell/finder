package com.example.yong.houseinfo.service;

import android.app.Service;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import com.example.yong.login.ui.LoginActivity;

/**
 * 实现录音的service
 * Created by yaojian on 2017/8/15.
 */
public class RecorderService extends Service {

    private final static String TAG = "RecorderService";

    private MediaRecorder recorder; //录音的一个实例

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        //监听电话
        TelephonyManager tm= (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        tm.listen(new MyListener(), PhoneStateListener.LISTEN_CALL_STATE);

    }
    class  MyListener extends PhoneStateListener{

        //在电话状态改变的时候调用
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            Log.i(TAG, " MyListener onCallStateChanged " + state);
            switch (state){
                case TelephonyManager.CALL_STATE_IDLE:
                    Log.i(TAG, " MyListener TelephonyManager.CALL_STATE_IDLE ");
                    LoginActivity.logger.info("recorderservice begin CALL_STATE_IDLE");
                    //空闲状态
                    if (recorder!=null){
                        recorder.stop();//停止录音
                        recorder.release();//释放资源
                        recorder=null;
                    }
                    break;

                case TelephonyManager.CALL_STATE_RINGING:
                    Log.i(TAG, " MyListener TelephonyManager.CALL_STATE_RINGING");
                    //响铃状态  需要在响铃状态的时候初始化录音服务
                    if (recorder==null){
                        LoginActivity.logger.info("recorderservice begin rining");
                        recorder=new MediaRecorder();//初始化录音对象
                        recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL);//设置录音的输入源(麦克)
                        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);//设置音频格式(3gp)
                        createRecorderFile();//创建保存录音的文件夹

                        recorder.setOutputFile(DIR_PATH + "/" + incomingNumber + "_" + getCurrentTime() + ".3gp"); //设置录音保存的文件
                        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);//设置音频编码
                        try {
                            recorder.prepare();//准备录音
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    Log.i(TAG, " MyListener TelephonyManager.CALL_STATE_OFFHOOK");
                    //摘机状态（接听）

                    if (recorder==null){
                        LoginActivity.logger.info("recorderservice begin CALL_STATE_OFFHOOK");

                        recorder=new MediaRecorder();//初始化录音对象
                        recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL);//设置录音的输入源(麦克)
                        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);//设置音频格式(3gp)
                        createRecorderFile();//创建保存录音的文件夹

                        recorder.setOutputFile(DIR_PATH + "/" + incomingNumber + "_" + getCurrentTime() + ".3gp"); //设置录音保存的文件
                        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);//设置音频编码
                        try {
                            recorder.prepare();//准备录音
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    if (recorder!=null){
                        recorder.start(); //接听的时候开始录音
                        LoginActivity.logger.info("recorderservice begin record");
                    }
                    break;
            }
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
}