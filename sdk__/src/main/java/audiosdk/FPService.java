package audiosdk;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.util.Pair;

import java.util.Properties;

import audiosdk.test.interfaces.InfoReceivedListener;
import audiosdk.test.utilities.Utils;


public class FPService extends Service {
    // Binder given to clients
    private final IBinder mBinder = new FPServiceBinder();

    private FPController fpController ;

    private boolean isDebugging;

    private Thread thread;

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class FPServiceBinder extends Binder {
        public FPService getService() {
            Log.d("AUDIO_SDK", "getService() returns: " + FPService.this);
            // Return this instance of LocalService so clients can call public methods
            return FPService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d("AUDIO_SDK", "onBind mbinder: " + mBinder);
        // A client is binding to the service with bindService()

        return mBinder;
    }



    @Override
    public void onCreate() {
        Log.d("AUDIO_SDK", "onCreated()");
        super.onCreate();
        // The service is being created

        Properties properties = Utils.loadProperties(this);
        isDebugging = Boolean.parseBoolean(properties.getProperty("isDebugging"));

    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // The service is starting, due to a call to startService()
        Log.d("AUDIO_SDK", "onStartCommand executed");
        super.onStartCommand(intent, flags, startId);

        return START_STICKY;
    }




    public Pair<Boolean,String> initialize(Context ctx, FPController.MatchRate matchRate, int onDuration, int offDuration, boolean repeatMode,
                                           FPController.UploadInterval uploadInterval, int uploadIntervalHours, int uploadIntervalMinutes ,
                           int notificationResponse ){
        fpController = FPController.getInstance(this.getApplicationContext());
        if (fpController.isRunning()){
            fpController.cancel();
        }
        return fpController.initialize(matchRate, onDuration, offDuration,repeatMode,uploadInterval,uploadIntervalHours,uploadIntervalMinutes,notificationResponse);

    }

    public void setAudioRecordConfiguration(int audioSource,int sampleRateInHz, int channelConfig, int recorderAudioEncoding){

        fpController.setAudioRecordConfiguration(audioSource,sampleRateInHz,channelConfig,recorderAudioEncoding);
    }

    public void start()  {
        if (fpController.isRunning()){
            fpController.cancel();
        }
        thread= new Thread(new Runnable() {
            String exception;
            @Override
            public void run() {
                 fpController.start();
            }
        });
        thread.start();

    }

    public void cancel(){

        fpController.cancel();
        thread.interrupt();

    }
    /**
     *
     * @param listener
     */
    public void addOnResponseListener(InfoReceivedListener listener){
        fpController = FPController.getInstance(this.getApplicationContext());
        fpController.addOnResponseListener(listener);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d("AUDIO_SDK", "FPService.unUnbind");
        boolean ret =  super.onUnbind(intent);
        return  ret;
    }

    @Override
    public void onDestroy() {
        Log.d("AUDIO_SDK", "FPService.ondestroy");
        super.onDestroy();
        // The service is no longer used and is being destroyed
        fpController.cancel();
        thread.interrupt();

    }

}
