package com.juked.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;

import java.io.IOException;
import java.util.Timer;

import com.juked.app.R;
import com.juked.app.utils.Utils;

import java.util.TimerTask;

public class SplashActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_splash);

        MyTimerTask myTask = new MyTimerTask();
        Timer myTimer = new Timer();
        myTimer.schedule(myTask, 1000);
    }



    class MyTimerTask extends TimerTask{

        @Override
        public void run() {
            JukedApplication app = (JukedApplication) getApplication();
            boolean bStatus = Utils.getUserStatus(SplashActivity.this);
//            if(bStatus){

                app.bFirstLaunch = false;
                try {
                    app.jukedUser = Utils.getUserinfo(SplashActivity.this);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Intent intent = new Intent(SplashActivity.this, com.audiosdktest.app.MainActivity.class);
                startActivity(intent);


//            }else {
//                app.bFirstLaunch = true;
//                Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
//                startActivity(intent);
//            }
        }
    }
}
