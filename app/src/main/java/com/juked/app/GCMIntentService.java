//package com.juked.app;
//
//import android.app.Activity;
//import android.app.Notification;
//import android.app.NotificationManager;
//import android.app.PendingIntent;
//import android.content.Context;
//import android.content.Intent;
//import android.util.Log;
//
//import com.google.android.gcm.GCMBaseIntentService;
//
///**
// * User: Oleg Soroka
// * Date: 20.09.12
// * Time: 21:37
// */
//public class GCMIntentService extends GCMBaseIntentService {
//
//    private static final String TAG = "GCMIntentService";
//
//    public GCMIntentService() {
//        super(Config.SENDER_ID);
//    }
//
//    @Override
//    protected void onMessage(Context ctx, Intent intent) {
//        Log.i(TAG, "onMessage");
//
//        final String message = intent.getExtras().getString("message");
//
//        NotificationManager notificationManager = (NotificationManager)ctx.getSystemService(Activity.NOTIFICATION_SERVICE);
//
//	    Intent notificationIntent = new Intent(ctx.getApplicationContext(), LoginActivity.class);
//
//	    PendingIntent pendingIntent = PendingIntent.getActivity(ctx, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
//
//	    Notification notification = new Notification();
//
//	    notification.icon = R.drawable.ic_launcher;
//	    notification.tickerText = message;
//	    notification.when = System.currentTimeMillis();
//	    notification.defaults = Notification.DEFAULT_ALL;
//	    notification.flags = Notification.FLAG_AUTO_CANCEL;
//	    //notification.setLatestEventInfo(ctx, ctx.getString(R.string.app_name), message, pendingIntent);
//
//	    notificationManager.notify(0, notification);
//    }
//
//    @Override
//    protected void onError(Context context, String errorId) {
//        Log.i(TAG, "onError, errorId=" + errorId);
//    }
//
//    @Override
//    protected void onRegistered(Context context, String registrationId) {
//
//    	JukedApplication app = (JukedApplication) getApplication();
//    	app.strDeviceToken = registrationId;
//        Log.i(TAG, "onRegistered, registrationId=" + registrationId);
//    }
//
//    @Override
//    protected void onUnregistered(Context context, String registrationId) {
//        Log.i(TAG, "onUnregistered, registrationId=" + registrationId);
//    }
//}