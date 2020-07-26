package com.audiosdktest.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Pair;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import com.juked.app.R;

import java.util.Map;

import audiosdk.test.AudioSDK;
import audiosdk.test.FPController;
import audiosdk.test.interfaces.IAudioSDK;
import audiosdk.test.interfaces.InfoReceivedListener;

public class MainActivity extends Activity implements InfoReceivedListener {


    /**
     * ********Constants***************
     */
    private static final int sizeButtonsAndUpperBar = 150;


    /**
     * ********Fields***************
     */

    private FPController.MatchRate mMatchRate;
    private int mOnDuration;
    private int mOffDuration;
    private int mUploadIntervalHourValue;
    private int mUploadIntervalMinuteValue;
    private Boolean mRepeat;
    private FPController.UploadInterval mUploadInterval;
    private int mNotificationResponse;
    private TextView matchRateTV;
    private TextView onDurationTV;
    private TextView offDurationTV;
    private TextView uploadIntervalTV;
    private Switch repeatModeTv;
    private TextView notificationResponseTV;
    private SharedPreferences mSharedPreferences;
    private Context context;
    private IAudioSDK audioSDK ;
    private Boolean alreadyInitialized = false;
    private Exception exception;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        Boolean init;

        super.onCreate(savedInstanceState);

        if(savedInstanceState == null) {

        }
        setContentView(R.layout.activity_main);
        this.context = this;
        matchRateTV = (TextView) findViewById(R.id.match_rate);
        onDurationTV = (TextView) findViewById(R.id.on_duration);
        offDurationTV = (TextView) findViewById(R.id.off_duration);
        uploadIntervalTV = (TextView) findViewById(R.id.upload_interval);
        repeatModeTv = (Switch) findViewById(R.id.repeat_mode);
        notificationResponseTV = (TextView) findViewById(R.id.notification_response);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        repeatModeTv.setEnabled(false);
        setButtonHandlers();

        Log.d("AUDIO_SDK","MainActivity.onCreate");
        loadPref();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("Initialize", alreadyInitialized);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

		/*
		 * Because it's onlt ONE option in the menu.
		 * In order to make it simple, We always start SetPreferenceActivity
		 * without checking.
		 */

        Intent intent = new Intent();
        intent.setClass(MainActivity.this, SetPreferenceActivity.class);
        startActivityForResult(intent, 0);

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		/*
		 * To make it simple, always re-load Preference setting.		 */

        loadPref();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus) {
            setScrollViewSize();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        audioSDK.unbindService();
    }

    private void loadPref() {

        Map<String, ?> all = mSharedPreferences.getAll();
        // only for testing
        //mSharedPreferences.edit().clear().commit();

        String matchRate = mSharedPreferences.getString("match_rate", FPController.MatchRate.ONE_PER_SECOND.toString());
        mMatchRate = FPController.MatchRate.valueOf(matchRate);
        matchRateTV.setText(matchRate);

        String onDuration = mSharedPreferences.getString("on_duration", "1");
        mOnDuration = Integer.valueOf(onDuration);
        onDurationTV.setText(String.valueOf(onDuration));

        String offDuration = mSharedPreferences.getString("off_duration", "0");
        mOffDuration = Integer.valueOf(offDuration);
        offDurationTV.setText(String.valueOf(offDuration));

        String uploadIntervalHourValue = mSharedPreferences.getString("editHourValue", "1");
        mUploadIntervalHourValue = Integer.valueOf(uploadIntervalHourValue);

        String uploadIntervalMinuteValue = mSharedPreferences.getString("editMinuteValue", "1");
        mUploadIntervalMinuteValue = Integer.valueOf(uploadIntervalMinuteValue);


        boolean repeat = mSharedPreferences.getBoolean("repeat", true);
        mRepeat = repeat;
        repeatModeTv.setChecked(repeat);

        String uploadIntervalStr = mSharedPreferences.getString("upload_interval", FPController.UploadInterval.IMMEDIATELY.toString());
        mUploadInterval = FPController.UploadInterval.valueOf(uploadIntervalStr);
        uploadIntervalTV.setText(uploadIntervalStr);

        String notificationResponse = mSharedPreferences.getString("notification_response", "0");
        mNotificationResponse = Integer.valueOf(notificationResponse);
        notificationResponseTV.setText(notificationResponse);

    }

    private void setButtonHandlers() {
        findViewById(R.id.buttonStart).setOnClickListener(buttonClick);
        findViewById(R.id.buttonStop).setOnClickListener(buttonClick);
    }

    /**
     * ******************** Callbacks & Event Handlers *******************
     */
    private View.OnClickListener buttonClick = new View.OnClickListener() {
        Exception ex;
        public void onClick(View v) {


            switch (v.getId()) {
                case R.id.buttonStart: {
                    new Thread(new Runnable() {

                        Pair<Boolean, String> resultInit;

                        @Override
                        public void run() {
                            try {
                                audioSDK = new AudioSDK(MainActivity.this.context);
                                audioSDK.addOnResponseListener(MainActivity.this);
                                resultInit = audioSDK.initialize(mMatchRate, mOnDuration, mOffDuration, mRepeat, mUploadInterval, mUploadIntervalHourValue, mUploadIntervalMinuteValue, mNotificationResponse);
                                if (resultInit.first) {
                                    audioSDK.setAudioRecordConfiguration(MediaRecorder.AudioSource.MIC, 8000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
                                    audioSDK.start();
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(context, "Recording...", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                } else {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(context, resultInit.second, Toast.LENGTH_LONG).show();
                                        }
                                    });
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                ex = e;
                                Log.d("MainActivity", e.getMessage() + "");
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(context, ex.getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                        }
                    }).start();

                    break;
                }
                case R.id.buttonStop: {
                    audioSDK.cancel();
                    Toast.makeText(context, "Cancelled", Toast.LENGTH_SHORT).show();
                    break;
                }

            }
        }
    };

    private void setScrollViewSize() {
        Display display;
        Point size;
        int width, height;
        ViewGroup.LayoutParams paramsLinear;
        ViewGroup.LayoutParams paramsButtons;
        LinearLayout linearView;
        LinearLayout buttonsView;


        display = getWindowManager().getDefaultDisplay();
        size = new Point();
        display.getSize(size);
        width = size.x;
        height = size.y;

        // set the size of the layout parameters in function of the screen size

        // get the linear layout
        linearView = ((LinearLayout) findViewById(R.id.linearSwitchContent));

        // get the parameters
        paramsLinear = (ViewGroup.LayoutParams) linearView.getLayoutParams();
        // modify them
        paramsLinear.height = height - sizeButtonsAndUpperBar;

        linearView.setLayoutParams(paramsLinear);
    }


    @Override
    public void dataReceived(String xml) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, "Data Received", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void errorReceived(Exception e) {
        exception = e;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, exception.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

    }

}
