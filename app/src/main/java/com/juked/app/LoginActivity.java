package com.juked.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;


//import com.facebook.FacebookException;
//import com.facebook.LoggingBehavior;
//import com.facebook.Request;
//import com.facebook.Response;
//import com.facebook.Session;
//import com.facebook.SessionDefaultAudience;
//import com.facebook.SessionLoginBehavior;
//import com.facebook.SessionState;
//import com.facebook.Settings;
//import com.facebook.model.GraphObject;
//import com.facebook.model.GraphUser;
//import com.facebook.widget.LoginButton;
import com.juked.app.R;
import com.juked.app.module.JukedUser;
import com.juked.app.utils.Const;
import com.juked.app.utils.JSONParser;
import com.juked.app.utils.Utils;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.UUID;

public class LoginActivity extends Activity{

    public final String KODEFUNFBAPP_ID = "487874921267044";
    //Session session;
    boolean pendingRequest;
    public JSONObject m_user = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_login);

//        this.session = createSession();
//        Settings.addLoggingBehavior(LoggingBehavior.INCLUDE_ACCESS_TOKENS);


        ImageButton btnFB = (ImageButton)findViewById(R.id.btnFBLogin);
//        btnFB.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                connectToFacebook();
//            }
//        });

    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
//    public void connectToFacebook() {
//
//        if (this.session != null && this.session.isOpened()) {
//            sendRequests();
//        } else {
//
//            pendingRequest = true;
//
//            Session.OpenRequest openRequest = new Session.OpenRequest(this);
//
//            if (openRequest != null) {
//                openRequest.setDefaultAudience(SessionDefaultAudience.FRIENDS);
//                openRequest.setPermissions(Arrays.asList("public_profile", "email", "user_birthday"));
//                openRequest.setLoginBehavior(SessionLoginBehavior.SSO_WITH_FALLBACK);
//
//                this.session.openForPublish(openRequest.setCallback(new Session.StatusCallback() {
//
//                    @Override
//                    public void call(Session session, SessionState state, Exception exception) {
//                        // TODO Auto-generated method stub
//                        if (exception != null) {
//                            new AlertDialog.Builder(LoginActivity.this)
//                                    .setTitle("Insuffienct Permissions")
//                                    .setMessage(exception.getMessage())
//                                    .setPositiveButton("OK", null)
//                                    .show();
//                            LoginActivity.this.session = createSession();
//                        } else if (session.isOpened()) {
////                            sendRequests();
//                        }
//                    }
//                }));
//
//            }
//        }
//
//
//    }
//
//    private void sendRequests() {
//
//        if (session != null) {
//            Log.d("FB", "groupToken request!");
//            Request myreq = Request.newGraphPathRequest(session, "me",
//                    new Request.Callback() {
//
//                        @Override
//                        public void onCompleted(Response response) {
//                            Log.d("FB", "received token");
//                            GraphObject obj = response.getGraphObject();
//
//                            m_user = obj.getInnerJSONObject();
//
//                            System.out.println(m_user.toString());
//                            String strURL = Const.kSocialApiServerPreference + "social/v1/users.json";
//                            runloadthread(strURL, m_user);
//                        }
//
//                    });
//            myreq.executeAsync();
//        } else {
//            Log.d("FB", "Session is closed");
//        }
//    }
//
//    private Session createSession() {
//        Session activeSession = Session.getActiveSession();
//        if (activeSession == null || activeSession.getState().isClosed()) {
//            activeSession = new Session.Builder(this).setApplicationId("487874921267044").build();
//            Session.setActiveSession(activeSession);
//        }
//        return activeSession;
//    }

//    public void onActivityResult(int requestCode, int resultCode, Intent data) {
//        if (this.session.onActivityResult(this, requestCode, resultCode, data) &&
//                pendingRequest &&
//                this.session.getState().isOpened()) {
//            sendRequests();
//        }
//    }


//    void runloadthread(final String strURL, final JSONObject facebook) {
//        final ProgressDialog pDialog;
//        pDialog = new ProgressDialog(this);
//        pDialog.setMessage("Loading...");
//        pDialog.setIndeterminate(false);
//        pDialog.setCancelable(true);
//        pDialog.show();
//
//        new Thread(new Runnable() {
//
//            @SuppressWarnings("deprecation")
//            @Override
//            public void run() {
//                Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
//                JukedApplication app = (JukedApplication)LoginActivity.this.getApplication();
//                try {
//                    String strBirthday = facebook.getString("birthday");
//                    String strEmail = facebook.getString("email");
//                    String fbID = facebook.getString("id");
//                    String strfirstname = facebook.getString("first_name");
//                    String strlastname = facebook.getString("last_name");
//                    String strGender = facebook.getString("gender");
//
//
//
//                    HttpClient client = new DefaultHttpClient();
//
//                    HttpPost post = new HttpPost(strURL);
//                    JSONObject json = null;
//
//                    @SuppressWarnings("deprecation")
//                    MultipartEntity reqEntity = new MultipartEntity(
//                            HttpMultipartMode.BROWSER_COMPATIBLE);
//
//                    String strAccessToken = session.getAccessToken();
//
//                    reqEntity.addPart("access_token",
//                            new StringBody(strAccessToken));
//                    reqEntity.addPart("email",new StringBody(strEmail));
//                    reqEntity.addPart("birth_date",
//                            new StringBody(strBirthday));
//                    reqEntity.addPart("facebook_id",
//                            new StringBody(fbID));
//                    reqEntity.addPart("first_name",
//                            new StringBody(strfirstname));
//                    reqEntity.addPart("last_name",
//                            new StringBody(strlastname));
//                    reqEntity.addPart("sex",
//                            new StringBody(strGender));
//                    reqEntity.addPart("udid",
//                            new StringBody(app.UDID));
//
//                    post.setEntity(reqEntity);
//                    HttpResponse response = client.execute(post);
//                    HttpEntity resEntity = response.getEntity();
//
//                    String str = EntityUtils.toString(resEntity);
//
//                StringBuilder builder = new StringBuilder();
//                json = new JSONObject(str);
//
//
//                app.jukedUser = new JukedUser(json);
//
//                Utils.saveUserInfo(LoginActivity.this, true, app.jukedUser);
//
//
//
//                String strURL = Const.kSocialApiServerPreference + String.format("social/v1/friends/users/%s.json?access_token=%s&udid=%s",app.jukedUser.id,app.jukedUser.access_token, app.UDID);
//
//                JSONParser jParser = new JSONParser();
//
//                jParser.getJSONFromUrl(strURL);
////                HttpClient client1 = new DefaultHttpClient();
//
////                HttpPost post1 = new HttpPost(strURL);
////                JSONObject json1 = null;
////
////                @SuppressWarnings("deprecation")
////                MultipartEntity reqEntity1 = new MultipartEntity(
////                        HttpMultipartMode.BROWSER_COMPATIBLE);
////
////
////                reqEntity1.addPart("access_token",
////                        new StringBody(app.jukedUser.access_token));
////                reqEntity1.addPart("udid",
////                        new StringBody(UDID));
////
////                post1.setEntity(reqEntity1);
////                HttpResponse response1 = client1.execute(post1);
////                HttpEntity resEntity1 = response1.getEntity();
////
////                String str1 = EntityUtils.toString(resEntity1);
////
////                StringBuilder builder1 = new StringBuilder();
////                json = new JSONObject(str1);
//
//
//
//                strURL = Const.kSocialApiServerPreference + "social/v1/devices/token";
//                HttpClient client2 = new DefaultHttpClient();
//
//                HttpPost post2 = new HttpPost(strURL);
//                JSONObject json2 = null;
//
//                @SuppressWarnings("deprecation")
//                MultipartEntity reqEntity2 = new MultipartEntity(
//                        HttpMultipartMode.BROWSER_COMPATIBLE);
//
//
//                reqEntity2.addPart("device",
//                        new StringBody(app.UDID));
//                reqEntity2.addPart("token",
//                        new StringBody(app.strDeviceToken));
//
//                post2.setEntity(reqEntity2);
//                HttpResponse response2 = client2.execute(post2);
//                HttpEntity resEntity2 = response2.getEntity();
//
//                String str2 = EntityUtils.toString(resEntity2);
//
//                StringBuilder builder2 = new StringBuilder();
//                json = new JSONObject(str2);
//
//
//
//
//            } catch (Exception e) {
//
//                }
//                handler.sendEmptyMessage(0);
//                pDialog.dismiss();
//            }
//        }).start();
//
//    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
            } else {

            }
        }

    };


}
