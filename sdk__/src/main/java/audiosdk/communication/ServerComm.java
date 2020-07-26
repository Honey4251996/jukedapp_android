package audiosdk.test.communication;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;
import android.util.Pair;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import audiosdk.test.interfaces.InfoReceivedListener;
import audiosdk.test.utilities.Utils;

public class ServerComm {


    /**********************Constants***********************/
    private static final String BASE_FILE_NAME = "filename";
    private static final String BASE_DATA_FILE_NAME = "data";


    /***********************Fields*************************/
    private String urlGetUser;
    private String urlSendBundle;
    private Context context;
    private String userId;
    private String androidId;
    private String urlGetResponse;
    private Date lastGetResponseRequest;
    private List<InfoReceivedListener> listeners;
    private boolean isDebugging;


    /***********************Constructors*******************/
    public ServerComm(Context context) {
        this.context = context;
        listeners = new ArrayList<InfoReceivedListener>();
        Properties properties = Utils.loadProperties(context);
        urlGetUser = properties.getProperty("urlGetUser");
        urlSendBundle = properties.getProperty("urlSendBundle");
        urlGetResponse = properties.getProperty("urlGetResponse");
        isDebugging = Boolean.parseBoolean(properties.getProperty("isDebugging"));
        userId = null;
        lastGetResponseRequest = null;
    }


    /***********************Event Raisers*******************/
    private void dataReceived(String data) {
        // Notify everybody that may be interested.
        for (InfoReceivedListener hl : listeners) {
            hl.dataReceived(data);
        }
    }

    private void errorReceived(Exception e){
        // Notify everybody that may be interested.
        for (InfoReceivedListener hl : listeners) {
            hl.errorReceived(e);
        }
    }


    /***********************Public Methods*******************/
    public void initialize() {

        Object jsonUserID;
        List<BasicNameValuePair> nameValuePairs;
        JSONObject jsonObject;
        String postResponse;

        try {
            // get the android ID
            this.androidId = Settings.Secure.getString(this.context.getContentResolver(), Settings.Secure.ANDROID_ID);

            // create a list to store HTTP variables and their values
            nameValuePairs = new ArrayList<BasicNameValuePair>();
            // add an HTTP variable and value pair
            nameValuePairs.add(new BasicNameValuePair("udid", androidId));

            // Do POST
            postResponse = POST(urlGetUser, nameValuePairs);

            // process the response
            jsonObject = new JSONObject(postResponse);
            jsonUserID = jsonObject.get("id");

            this.userId = jsonUserID.toString();
            this.urlGetResponse = this.urlGetResponse.replace("XXX", this.userId);

        } catch (JSONException e) {
            errorReceived(e);
        }
    }

    public void getResponseFromServer(){
        Date now;
        String sinceDate;
        String getResponse;
        List<BasicNameValuePair> nameValuePairs;

        if (this.lastGetResponseRequest != null) {
            try {

                // create a list to store HTTP variables and their values
                nameValuePairs = new ArrayList<BasicNameValuePair>();
                // add an HTTP variable and value pair
                nameValuePairs.add(new BasicNameValuePair("udid", androidId));

                now = Utils.localDateToGMT(this.lastGetResponseRequest);
                sinceDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss +S").format(now);
                nameValuePairs.add(new BasicNameValuePair("since", sinceDate));

                // Do GET
                getResponse = GET(this.urlGetResponse, nameValuePairs);
                this.lastGetResponseRequest = Calendar.getInstance().getTime();
                dataReceived(getResponse);
            }
            catch (Exception e){
                errorReceived(e);
            }
        }
    }

    public void sendPackage(List<Pair<String, String[]>> bundle) throws Exception {

        List<File> files;
        String fileContent;

        try {
            files = new ArrayList<File>();
        for (Pair<String, String[]> fpPackage : bundle) {
            fileContent = Utils.concatString(fpPackage.second,"\n");

            byte[] bytesCompressed = Utils.compressByteArray(fileContent.getBytes());
            files.add(Utils.createByteArrayFile(fpPackage.first, bytesCompressed, null));
            Utils.createStringFile(fpPackage.first+".txt",Utils.byteArrayToBinaryString(bytesCompressed),null);
        }

            multiPartPOST(this.urlSendBundle, files);

            if (lastGetResponseRequest == null) {
                lastGetResponseRequest = Calendar.getInstance().getTime();
            }
        }
        catch (Exception e){
            errorReceived(e);
        }
    }

    public void addOnResponseListener(InfoReceivedListener toAdd) {
        listeners.add(toAdd);
    }

    /***********************Private Methods*******************/
    private void multiPartPOST(String url, List<File> files) {

        try {

            HttpClient client;
            HttpPost post;
            MultipartEntityBuilder entityBuilder;
            HttpEntity entity;
            HttpResponse response;
            HttpEntity httpEntity;
            String result;
            String decodedFileName;


            client = new DefaultHttpClient();
            post = new HttpPost(url);
            entityBuilder = MultipartEntityBuilder.create();
            entityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);


            // set body text parameters
            entityBuilder.addTextBody("id", this.userId);
            entityBuilder.addTextBody("udid", this.androidId);
            entityBuilder.addTextBody("file-count", String.valueOf(files.size()));


            for (int i = 0; i < files.size(); i++) {
                decodedFileName = URLDecoder.decode(files.get(i).getName(),"UTF-8");
                entityBuilder.addTextBody(BASE_FILE_NAME + i, decodedFileName);
                entityBuilder.addBinaryBody(BASE_DATA_FILE_NAME + i, files.get(i));
            }


            entity = entityBuilder.build();
            post.setEntity(entity);
            response = client.execute(post);

            httpEntity = response.getEntity();

            result = EntityUtils.toString(httpEntity);

            Log.v("AUDIO_SDK","Result: "+ result);

            if (!isDebugging){
                for (int i = 0; i < files.size(); i++) {
                    files.get(i).delete();
                }
            }

        } catch (Exception e) {
            errorReceived(e);
        }

    }

    private String GET(String url, List<BasicNameValuePair> nameValuePairs) {
        String ret = "";

        HttpClient httpclient = new DefaultHttpClient();
        // specify the URL you want to post to

        url += "?" + URLEncodedUtils.format(nameValuePairs, "utf-8");
        HttpGet httpGet = new HttpGet(url);

        try {
            // send the variable and value, in other words post, to the URL
            HttpResponse response = httpclient.execute(httpGet);

            ret = EntityUtils.toString(response.getEntity());
        } catch (Exception e) {
            e.printStackTrace();
            errorReceived(e);
        }

        return ret;
    }

    private String POST(String url, List<BasicNameValuePair> nameValuePairs) {

        String ret = "";

        HttpClient httpclient = new DefaultHttpClient();
        // specify the URL you want to post to
        HttpPost httppost = new HttpPost(url);
        try {
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            // send the variable and value, in other words post, to the URL
            HttpResponse response = httpclient.execute(httppost);

            ret = EntityUtils.toString(response.getEntity());
        } catch (Exception e) {
            e.printStackTrace();
            errorReceived(e);
        }

        return ret;

    }




}