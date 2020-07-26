package com.juked.app.utils;

/**
 * Created by imac on 7/23/14.
 */
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import com.juked.app.JukedApplication;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import audiosdk.test.AudioSDK;
import audiosdk.test.FPController;

/**
 * Usage:
 *
 * 1. Get the Foreground Singleton, passing a Context or Application object unless you
 * are sure that the Singleton has definitely already been initialised elsewhere.
 *
 * 2.a) Perform a direct, synchronous check: Foreground.isForeground() / .isBackground()
 *
 * or
 *
 * 2.b) Register to be notified (useful in Service or other non-UI components):
 *
 * Foreground.Listener myListener = new Foreground.Listener(){
 * public void onBecameForeground(){
 * // ... whatever you want to do
 * }
 * public void onBecameBackground(){
 * // ... whatever you want to do
 * }
 * }
 *
 * public void onCreate(){
 * super.onCreate();
 * Foreground.get(this).addListener(listener);
 * }
 *
 * public void onDestroy(){
 * super.onCreate();
 * Foreground.get(this).removeListener(listener);
 * }
 */
public class Foreground implements Application.ActivityLifecycleCallbacks {

    public static final long CHECK_DELAY = 500;
    public static final String TAG = Foreground.class.getName();

    public interface Listener {

        public void onBecameForeground();

        public void onBecameBackground();

    }

    private static Foreground instance;

    private boolean foreground = false, paused = true;
    private Handler handler = new Handler();
    private List<Listener> listeners = new CopyOnWriteArrayList<Listener>();
    private Runnable check;
    private AudioSDK audioSDK;

    /**
     * Its not strictly necessary to use this method - _usually_ invoking
     * get with a Context gives us a path to retrieve the Application and
     * initialise, but sometimes (e.g. in test harness) the ApplicationContext
     * is != the Application, and the docs make no guarantees.
     *
     * @param application
     * @return an initialised Foreground instance
     */
    public static Foreground init(Application application){
        if (instance == null) {
            instance = new Foreground();
            application.registerActivityLifecycleCallbacks(instance);
        }
        return instance;
    }

    public static Foreground get(Application application){
        if (instance == null) {
            init(application);
        }
        return instance;
    }

    public static Foreground get(Context ctx){
        if (instance == null) {
            Context appCtx = ctx.getApplicationContext();
            if (appCtx instanceof Application) {
                init((Application)appCtx);
            }
            throw new IllegalStateException(
                    "Foreground is not initialised and " +
                            "cannot obtain the Application object");
        }
        return instance;
    }

    public static Foreground get(){
        if (instance == null) {
            throw new IllegalStateException(
                    "Foreground is not initialised - invoke " +
                            "at least once with parameterised init/get");
        }
        return instance;
    }

    public boolean isForeground(){
        return foreground;
    }

    public boolean isBackground(){
        return !foreground;
    }

    public void addListener(Listener listener){
        listeners.add(listener);
    }

    public void removeListener(Listener listener){
        listeners.remove(listener);
    }

    @Override
    public void onActivityResumed(final Activity activity) {
        paused = false;
        boolean wasBackground = !foreground;
        foreground = true;

        if (check != null)
            handler.removeCallbacks(check);

        if (wasBackground){
            Log.i(TAG, "went foreground");
            JukedApplication app = (JukedApplication)activity.getApplication();
            app.inForeground = 1;
            app.nRefreshInterval = 4*1000;

            runAudioSDK(activity,  true);
            for (Listener l : listeners) {
                try {
                    l.onBecameForeground();
                } catch (Exception exc) {
                    Log.e(TAG, "Listener threw exception!", exc);
                }
            }
        } else {
            Log.i(TAG, "still foreground");
        }
    }

    @Override
    public void onActivityPaused(final Activity activity) {
        paused = true;

        if (check != null)
            handler.removeCallbacks(check);

        handler.postDelayed(check = new Runnable(){
            @Override
            public void run() {
                if (foreground && paused) {
                    foreground = false;
                    Log.i(TAG, "went background");
                    JukedApplication app = (JukedApplication)activity.getApplication();
                    app.inForeground = 0;
                    app.nRefreshInterval = 4*1000;
                    runAudioSDK(activity, false);
                    for (Listener l : listeners) {
                        try {
                            l.onBecameBackground();
                        } catch (Exception exc) {
                            Log.e(TAG, "Listener threw exception!", exc);
                        }
                    }
                } else {
                    Log.i(TAG, "still foreground");
                }
            }
        }, CHECK_DELAY);
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}

    @Override
    public void onActivityStarted(Activity activity) {}

    @Override
    public void onActivityStopped(Activity activity) {}

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

    @Override
    public void onActivityDestroyed(Activity activity) {}

    void runAudioSDK(final Activity act, final boolean bForground){
        boolean bStatus = Utils.getUserStatus(act);
        if(!bStatus){
            return;
        }

        new Thread(new Runnable() {

            Pair<Boolean, String> resultInit;

            @Override
            public void run() {
                try {
                    if(audioSDK != null){
                        audioSDK.cancel();
                    }
                    audioSDK = new AudioSDK(act);

//                    audioSDK.addOnResponseListener(MainActivity.this);
                    if(bForground){
                        resultInit = audioSDK.initialize(FPController.MatchRate.FOUR_PER_SECOND, 60, 1, true, FPController.UploadInterval.IMMEDIATELY, 1, 1, 0);
                    }else {
                        resultInit = audioSDK.initialize(FPController.MatchRate.FOUR_PER_SECOND, 60, 1, true, FPController.UploadInterval.MINUTE_VALUE, 0, 1, 0);
                    }
                    if (resultInit.first) {
                        audioSDK.setAudioRecordConfiguration(MediaRecorder.AudioSource.MIC, 8000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
                        audioSDK.start();
                        act.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
//                                Toast.makeText(act, "Recording...", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        act.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
//                                Toast.makeText(act, resultInit.second, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
//                    ex = e;
                    Log.d("MainActivity", e.getMessage() + "");
                    act.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
//                            Toast.makeText(context, ex.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }
}