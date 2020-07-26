package com.juked.app;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.juked.app.module.MainAdapter;
import com.juked.app.utils.Const;
import com.juked.app.utils.Foreground;
import com.juked.app.utils.ImageLoader;
import com.juked.app.utils.JSONParser;
import com.juked.app.utils.Utils;
import com.juked.app.utils.smartimageview.SmartImageView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import audiosdk.test.AudioSDK;
import audiosdk.test.FPController;
import audiosdk.test.interfaces.IAudioSDK;

public class MainActivity extends Activity {

    int nBullets;
    String strBannerURL;
    String strBannerTargetURL;

    FrameLayout layout;
    SmartImageView imgBanner;
    TextView txtBallote;
    JukedApplication app;

    ImageLoader imageLoader;
    JSONArray arrayJSON;
    ListView listview;
    MainAdapter adapter;

    TextView txtStatic;

    private IAudioSDK audioSDK ;

    TextView txtLoading;

    int nLoading = 0;

    boolean bFirst = true;

    int firstDedupeRunLocal = 0;


    int matchCountoldLocal = 0;

    ArrayList<JSONObject> mArray;

    ArrayList<JSONObject> mutMatchesLocal;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Foreground.init(getApplication());

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_mainactivity);

        layout = (FrameLayout) findViewById(R.id.bannerlayout);
        imgBanner = (SmartImageView) findViewById(R.id.banner);
        txtBallote = (TextView) findViewById(R.id.txtBallot);

        listview = (ListView)findViewById(R.id.listView);
        listview.setVisibility(View.GONE);
        txtStatic = (TextView)findViewById(R.id.txtStatic);

        LayoutInflater inflater = getLayoutInflater();
        ViewGroup header = (ViewGroup)inflater.inflate(R.layout.list_emptyrow, listview, false);
        listview.addHeaderView(header, null, false);

        mArray = new ArrayList<JSONObject>();
        adapter = new MainAdapter(MainActivity.this, mArray);
//        Log.e("MyString", Integer.toString(arrayJSON.length()));
        listview.setAdapter(adapter);

        imageLoader = new ImageLoader(this);

        app = (JukedApplication) getApplication();

        if(app.bFirstLaunch){
            Utils.saveTimeStamp(this,"2014-01-01 00:00:00");
        }

//        MyTimerTask myTask = new MyTimerTask();
//        Timer myTimer = new Timer();
//        myTimer.schedule(myTask, app.nInterval*1000);

        txtLoading = (TextView)findViewById(R.id.textView);

//        Button btnBanner = (Button)findViewById(R.id.btnBanner);
//        btnBanner.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Intent i = new Intent(Intent.ACTION_VIEW);
//                i.setData(Uri.parse(strBannerTargetURL));
//                startActivity(i);
//            }
//        });
//
//        getBanner();

//        String strURL = Const.kSocialApiServerPreference + "social/v1/matches/users/103.json?"+String.format("udid=%s&access_token=%s&since=%s","719F0FD4-6A00-4BF9-BB99-91E5F46EE715","CAAG7uDtgf2QBANdKCkT5pKjZBZCXqACsHk0auSlxnXfylk9L1wkcQPWIhTUQZBLJZAo6JkiyAjzCgH6edR5hP86MsqvbvX69KFH7MOhyzwvqOIusICwfhyMLvHiFmb3YJdrSnC9qXI6ChXZBDcBsQ9dszJ18Vzeu28Hr2tevy6x4NeHdwwhsSB37kGtrnUZAzSsbiZAX0alGYOzsuZAqIrlksbzJqZAOvHSsZD","2014-01-15 17:40:15");
//      String strURL = Const.kSocialApiServerPreference + String.format("social/v1/matches/users/%s.json?udid=%s&access_token=%s&since=%s",app.jukedUser.id,app.UDID, app.jukedUser.access_token,Utils.getTimeStamp(this));
//        runMatchloadthread(strURL);

        loadinghandler.sendEmptyMessage(0);
    }

    /**/


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
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

//    public void getBanner() {
//        JukedApplication app = (JukedApplication) getApplication();
//
//        String strURL = Const.kSocialApiServerPreference + "social/v1/promotions/current/" + app.jukedUser.id;
//        runBannerloadthread(strURL);
//    }

//    class MyTimerTask extends TimerTask {
//
//        @Override
//        public void run() {
//            JukedApplication app = (JukedApplication) getApplication();
//            MyTimerTask myTask = new MyTimerTask();
//            Timer myTimer = new Timer();
//            myTimer.schedule(myTask, app.nInterval*1000);
//
//            getBanner();
//        }
//    }

//    void runBannerloadthread(final String strURL) {
//
//        new Thread(new Runnable() {
//
//            @Override
//            public void run() {
//                Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
//
//                JSONParser jParser = new JSONParser();
//
//                JSONObject json = jParser.getJSONFromUrl(strURL);
//
//                try {
//                    JSONObject obj = (JSONObject) json.getJSONArray("promotions").get(0);
//                    strBannerURL = obj.getString("banner");
//                    strBannerTargetURL = obj.getString("click_url");
//
//
//                    JSONObject prizes = (JSONObject) obj.getJSONArray("prizes").get(0);
//                    if (prizes != null) {
//                        nBullets = prizes.getInt("ballots");
//                        if (app.nInterval != 0) {
//                            handler.sendEmptyMessage(0);
//                        }
//                    } else {
//                        layout.setVisibility(View.GONE);
//                    }
//
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    layout.setVisibility(View.GONE);
//                }
//
//            }
//        }).start();
//
//    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                layout.setVisibility(View.VISIBLE);
                imgBanner.setImageUrl(strBannerURL);
                txtBallote.setText(Integer.toString(nBullets));

            }
        }

    };

    void runMatchloadthread(final String strURL) {
        ProgressDialog pDialog = null;
        if(bFirst) {

            pDialog = new ProgressDialog(this);
            pDialog.setMessage("Loading...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(true);
            pDialog.show();
            bFirst = false;
        }
        final ProgressDialog finalPDialog = pDialog;
        new Thread(new Runnable() {

            @Override
            public void run() {
                Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

                JSONParser jParser = new JSONParser();

                try {
                    String url = strURL.replaceAll(" ","%20");
                    arrayJSON = jParser.getJSONFromUrl(url, true);
                    if(arrayJSON == null)
                        arrayJSON = new JSONArray();
//                    if(arrayJSON != null)
                    matchhandler.sendEmptyMessage(0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if(finalPDialog != null)
                    finalPDialog.dismiss();
            }
        }).start();
    }

    private Handler matchhandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            final JukedApplication app = (JukedApplication)MainActivity.this.getApplication();
            if(arrayJSON.length() > 0){
                listview.setVisibility(View.VISIBLE);
                txtStatic.setVisibility(View.GONE);
            }else{
                listview.setVisibility(View.GONE);
                txtStatic.setVisibility(View.VISIBLE);
            }

            if(arrayJSON.length() > 0){
                try {
                    firstDedupeRunLocal = 0;
                    matchCountoldLocal = 0;

                    ArrayList<JSONObject> array = new ArrayList<JSONObject>();
                    for(int i = 0; i < arrayJSON.length(); i++){
                        if(arrayJSON.getJSONObject(i).getJSONObject("track").getInt("id") > 10){
//                    int j = 0;
                        }
                        array.add(arrayJSON.getJSONObject(i));
                    }
                    deDupeMatchesLocal(array);

                    Date currentMatchTime = getDatefromString(((JSONObject) mArray.get(mArray.size()-1)).getString("created_at"));
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                    String formattedString = formatter.format(currentMatchTime);
                    Utils.saveTimeStamp(MainActivity.this, formattedString);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }else{
                mArray.clear();
            }
            Log.e("MyString", Integer.toString(mArray.size()));
            adapter.notifyDataSetChanged();

            if(app.ranOnce == 0){
                app.ranOnce = 1;
                CallMatchTask myTask = new CallMatchTask();
                Timer myTimer = new Timer();
                myTimer.schedule(myTask, app.nRefreshInterval);
            }
        }

    };

    class CallMatchTask extends TimerTask {

        @Override
        public void run() {


            CallMatchTask myTask = new CallMatchTask();
            Timer myTimer = new Timer();
            myTimer.schedule(myTask, app.nRefreshInterval);

            if(app.inForeground == 1) {
//          String strURL = Const.kSocialApiServerPreference + "social/v1/matches/users/103.json?"+String.format("udid=%s&access_token=%s&since=%s","719F0FD4-6A00-4BF9-BB99-91E5F46EE715","CAAG7uDtgf2QBANdKCkT5pKjZBZCXqACsHk0auSlxnXfylk9L1wkcQPWIhTUQZBLJZAo6JkiyAjzCgH6edR5hP86MsqvbvX69KFH7MOhyzwvqOIusICwfhyMLvHiFmb3YJdrSnC9qXI6ChXZBDcBsQ9dszJ18Vzeu28Hr2tevy6x4NeHdwwhsSB37kGtrnUZAzSsbiZAX0alGYOzsuZAqIrlksbzJqZAOvHSsZD","2014-01-15 17:40:15");
                String strURL = Const.kSocialApiServerPreference + String.format("social/v1/matches/users/%s.json?udid=%s&access_token=%s&since=%s", app.jukedUser.id, app.UDID, app.jukedUser.access_token, Utils.getTimeStamp(MainActivity.this));

                runMatchloadthread(strURL);
                Log.i("MyString", "call matchapi again");
            }


        }
    }


    private Handler loadinghandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            loadinghandler.postDelayed(new Runnable(){
                public void run(){
                    if(nLoading == 0){
                        txtLoading.setText("Listening");
                    }else if(nLoading == 1){
                        txtLoading.setText("Listening.");
                    }else if(nLoading == 2){
                        txtLoading.setText("Listening..");
                    }else if(nLoading == 3){
                        txtLoading.setText("Listening...");
                    }else if(nLoading == 4){
                        txtLoading.setText("Listening....");
                    }else if(nLoading == 5){
                        txtLoading.setText("Listening.....");
                    }else if(nLoading == 6){
                        txtLoading.setText("Listening......");
                    }else if(nLoading == 7){
                        txtLoading.setText("Listening.......");
                    }else if(nLoading == 8){
                        txtLoading.setText("Listening........");
                    }else if(nLoading == 9){
                        txtLoading.setText("Sending.");
                    }else if(nLoading == 10){
                        txtLoading.setText("Sending..");
                    }else if(nLoading == 11){
                        txtLoading.setText("Sending...");
                    }else if(nLoading == 12){
                        txtLoading.setText("Recieving.");
                    }else if(nLoading == 13){
                        txtLoading.setText("Recieving..");
                    }else if(nLoading == 14)

                    {
                        txtLoading.setText("Recieving...");
                    }
                    nLoading++;
                    if(nLoading > 14)
                        nLoading = 0;
                        loadinghandler.postDelayed(this, 1000);
                }
            }, 1000);

        }

    };


    Date getDatefromString(String strDate){
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'+00:00'");
        try {
            Date date = format.parse(strDate);
            System.out.println(date);
            return date;
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    ArrayList<JSONObject> deDupeMatchesLocal(ArrayList<JSONObject> matches) throws JSONException {



        //Below de-dupes matches based on 6 four characters, in an array of the last 5 matches. If a dupe is found in last 5 matches, it is checked to see if dupe was within last 4 minutes. If so, it removes from array of matches. This removal is only for display purposes, the data persists as was originally captured.


        mutMatchesLocal = new ArrayList<JSONObject>(matches);

        matchCountoldLocal = matches.size();
        for (int i = matches.size()-1; i >0; --i) {


            String currentTitle = ((JSONObject)matches.get(i-1)).getJSONObject("track").getString("title");

            Date currentMatchTime = getDatefromString(((JSONObject) matches.get(i-1)).getString("created_at"));

            int trackId = ((JSONObject)matches.get(i-1)).getJSONObject("track").getInt("id");

            if (trackId > 10) {



                String prevTitle = ((JSONObject)matches.get(i)).getJSONObject("track").getString("title");
                Date prevMatchTime = getDatefromString(((JSONObject) matches.get(i)).getString("created_at"));




                boolean dupe;
                dupe = fuzzyStringMatcher(currentTitle,prevTitle);

                if (currentTitle.length()>6) {
                    currentTitle = currentTitle.substring(6);

                }

                if (prevTitle.length()>6) {
                    prevTitle = prevTitle.substring(6);

                }


                if ((currentTitle.equals(prevTitle)  && (currentMatchTime.getTime()/1000 - prevMatchTime.getTime()/1000) < 240) || (dupe)) {
                    mutMatchesLocal.remove(i);

                }
            }else{
                mutMatchesLocal.remove(i);
            }
            firstDedupeRunLocal = 1;

        }
        mArray.clear();
        mArray.addAll(mutMatchesLocal);
        return mArray;



    }


    boolean fuzzyStringMatcher(String stringNumOne,String stringNumTwo)

    {

        String[] piecesOfStringOne = stringNumOne.split(" ");
        String[] piecesOfStringTwo = stringNumTwo.split(" ");

        int sameWords = 0;

        int countStringOne = piecesOfStringOne.length;
        int countStringTwo = piecesOfStringTwo.length;

        float totalPercentSameOne = 0;
        float totalPercentsameTwo = 0;

        for(int i=0;i<countStringOne;i++)
        {
            String tempPhraseOne = piecesOfStringOne[i];
            for(int j=0;j<countStringTwo;j++)
            {
                String tempPhraseTwo = piecesOfStringTwo[j];

                if (tempPhraseOne.contains(tempPhraseTwo) == false) {
                }
                else
                {
                    sameWords = sameWords+1;
                    break;

                }

            }
        }

        if(countStringOne!=0 && countStringTwo !=0)
        {
            totalPercentSameOne = (sameWords/countStringOne)*100;
            totalPercentsameTwo = (sameWords/countStringTwo)*100;

            if (totalPercentSameOne >= 75 || totalPercentsameTwo >= 75)
            {
                return true;
            }
            else if (!(totalPercentSameOne >= 75 || totalPercentsameTwo >= 75))
            {
                return false;
            }
        }
        else
        {
            return true;
        }
        return false;
    }
}
