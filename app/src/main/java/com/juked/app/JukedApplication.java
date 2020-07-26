package com.juked.app;

import android.app.Application;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

//import com.google.android.gcm.GCMRegistrar;
import com.juked.app.module.JukedUser;
import com.juked.app.utils.Const;
import com.juked.app.utils.Foreground;
import com.juked.app.utils.JSONParser;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by imac on 7/21/14.
 */
public class JukedApplication extends Application {
    public int nInterval;
    public JukedUser jukedUser;
    public String strDeviceToken;
    public String UDID;
    public int nRefreshInterval;
    public int inForeground = 1;
    public int ranOnce = 0;
    public boolean bFirstLaunch;
    @Override
    public void onCreate(){
        super.onCreate();

//        UDID = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
//
//        GCMRegistrar.checkDevice(this);
//        GCMRegistrar.checkManifest(this);
//
//        final String regId = GCMRegistrar.getRegistrationId(this);
//        Log.v("MyString", "Run GCMRegistrar.getRegistrationId(), regId=" + regId);
//        if (regId.equals("")) {
//            register();
//        } else {
//            Log.v("MyString", "Already registered, try to unregister");
//            GCMRegistrar.unregister(this);
//            register();
//        }


        String strURL = Const.kSocialApiServerPreference + "/social/v1/fingerprints.json";
        runloadthread(strURL);
    }

//    private void register() {
//        Log.v("MyString", "Run GCMRegistrar.register(), SENDER_ID=" + Config.SENDER_ID);
//        GCMRegistrar.register(this, Config.SENDER_ID);
//    }


    void runloadthread(final String strURL){

        new Thread(new Runnable() {

            @Override
            public void run() {
                Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

                JSONParser jParser = new JSONParser();

                JSONObject json = jParser.getJSONFromUrl(strURL);

                try {
                    nInterval = ((JSONObject)json.getJSONObject("promotions")).getInt("polling_interval");
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }).start();

    }
}
