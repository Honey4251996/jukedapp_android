package audiosdk;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import android.util.Pair;

import java.util.Properties;

import audiosdk.test.interfaces.InfoReceivedListener;
import audiosdk.test.interfaces.IAudioSDK;
import audiosdk.test.utilities.Utils;

public class AudioSDK implements IAudioSDK {



    private FPService fpService;
    private boolean bound = false;
    private Context ctx;
    private Intent intent;
    private boolean isDebugging;
    private boolean isInitialized;
    private boolean isBinding;


    public AudioSDK(Context ctx) {
        Log.d("AUDIO_SDK", "AudioSDK contructor");
        isInitialized = false;
        this.ctx = ctx;
        this.intent = new Intent(ctx, FPService.class);
        // check with bind if service is running.. add a variable to the service. Then unbind and startService only if not started
        Properties properties = Utils.loadProperties(ctx);
        isDebugging = Boolean.parseBoolean(properties.getProperty("isDebugging"));

        isBinding = true;
        this.ctx.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        this.ctx.startService(AudioSDK.this.intent);
        waitUntilConnected();
        isBinding = false;



    }


    @Override
    public Pair<Boolean,String> start() {
        Pair<Boolean, String>  ret;
        if (isInitialized) {
            Log.d("AUDIO_SDK", "AudioSDK.start");
            fpService.start();
            ret = new Pair<Boolean, String>(false, "OK");
        }
        else
        {
            ret = new Pair<Boolean, String>(false, "You must call initialize before start");
        }

        return ret;
    }


    @Override
    public Pair<Boolean,String> initialize(FPController.MatchRate matchRate, int onDuration, int offDuration, boolean repeatMode,
                                           FPController.UploadInterval uploadInterval, int uploadIntervalHours, int uploadIntervalMinutes,
                           int notificationResponse ){

        if (Utils.hasActiveInternetConnection(ctx)) {

            waitUntilConnected();
            if (bound) {
                Log.d("AUDIO_SDK", "initialize fpService: " + fpService);

                Pair<Boolean, String> ret = fpService.initialize(this.ctx, matchRate, onDuration, offDuration, repeatMode, uploadInterval, uploadIntervalHours, uploadIntervalMinutes, notificationResponse);
                isInitialized = true;
                return ret;
            } else {
                String msg = "No more recording sessions may be started with this AudioSDK. Subsequent calls do nothing. You must create a new Instance";
                Log.d("AUDIO_SDK",msg);
                return new Pair<Boolean, String>(false, msg);
            }
        }
        else{
            String msg = "No internet access";
            Log.d("AUDIO_SDK",msg);
            return new Pair<Boolean, String>(false, msg);
        }


    }

    @Override
    public void setAudioRecordConfiguration(int audioSource, int sampleRateInHz, int channelConfig, int recorderAudioEncoding){

        waitUntilConnected();
        fpService.setAudioRecordConfiguration(audioSource, sampleRateInHz, channelConfig, recorderAudioEncoding);
    }

    @Override
    public void cancel() {
        waitUntilConnected();
        Log.d("AUDIO_SDK", "audioSDK.Cancel");
        if (bound) {
            if (!isInitialized){
                waitUntilInitialized();
            }

            bound = false;
            fpService.cancel();
            this.ctx.unbindService(mConnection);
            ctx.stopService(intent);
        }
    }


    @Override
    public void unbindService() {
        Log.d("AUDIO_SDK", "audioSDK.onUnbindService" );
        if (bound) {
            this.ctx.unbindService(mConnection);
            bound = false;
        }
    }

    /**
     *
     * @param listener
     */
    @Override
    public void addOnResponseListener(InfoReceivedListener listener){
        waitUntilConnected();
        Log.d("AUDIO_SDK", "audioSDK.addOnResponseListener");
        fpService.addOnResponseListener(listener);
    }

    private void waitUntilConnected() {
        try {
            mConnection.waitUntilConnected();
        } catch (InterruptedException e) {
            if (isDebugging) {
                e.printStackTrace();
            }
        }
    }

    private void waitUntilInitialized() {
        try {
            while(!isInitialized){
                Thread.sleep(10);
            }
        } catch (InterruptedException e) {
            if (isDebugging) {
                e.printStackTrace();
            }
        }
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private MyServiceConnection mConnection = new MyServiceConnection();


    public class MyServiceConnection implements ServiceConnection {


        @Override
        public void onServiceConnected(ComponentName className,IBinder service) {
            // We've bound to LocalService, cast the IBinder and get FPService instance
            FPService.FPServiceBinder binder = (FPService.FPServiceBinder) service;
            fpService = binder.getService();
            bound = true;
            isBinding = false;
            Log.d("AUDIO_SDK","onServiceConnected bound: " + bound);

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            bound = false;
        }

        public void waitUntilConnected() throws InterruptedException {
            while(!bound && isBinding){
                Thread.sleep(10);
            }
        }

    }





}


