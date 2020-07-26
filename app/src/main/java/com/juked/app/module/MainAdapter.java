package com.juked.app.module;

/**
 * Created by imac on 7/22/14.
 */
import android.app.Activity;
import android.content.Context;
import android.media.Image;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.juked.app.R;
import com.juked.app.utils.ImageLoader;
import com.juked.app.utils.Utils;
import com.juked.app.utils.smartimageview.SmartImageView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class MainAdapter extends BaseAdapter {
    final String strImageUrl = "http://mapavape.com/pictures/";



    private Activity activity;
    private static LayoutInflater inflater=null;
    public ImageLoader imageLoader;
    ArrayList<JSONObject> mArray;

    public MainAdapter(Context context, ArrayList<JSONObject> arrayData) {

        inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        imageLoader=new ImageLoader(context.getApplicationContext());
        mArray = arrayData;
    }

    public int getCount() {
        return mArray.size();
    }

    public JSONObject getItem(int position) {
        return mArray.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View vi=convertView;
        if(convertView==null)
            vi = inflater.inflate(R.layout.list_row, null);

        TextView tv = (TextView)vi.findViewById(R.id.title);
        SmartImageView img = (SmartImageView)vi.findViewById(R.id.list_image);
        TextView artist = (TextView)vi.findViewById(R.id.artist);
        TextView album = (TextView)vi.findViewById(R.id.album);
        TextView txtDate = (TextView)vi.findViewById(R.id.txtDate);
        try {
            String currentTitle = (mArray.get(position).getJSONObject("track")).getString("title");
            Date currentMatchTime = getDatefromString(((JSONObject) mArray.get(position)).getString("created_at"));
            try {
                txtDate.setText(Utils.formatToYesterdayOrToday(currentMatchTime));
            } catch (ParseException e) {
                e.printStackTrace();
            }
            tv.setText(currentTitle);
            JSONObject objArtist = (mArray.get(position).getJSONObject("track")).getJSONObject("artist");
            artist.setText(objArtist.getString("name"));


            JSONObject obj = (mArray.get(position).getJSONObject("track")).getJSONObject("album");
            album.setText(obj.getString("title"));
            String strImgURL = obj.getString("image_album150x150");
            img.setImageUrl(strImgURL);
//            imageLoader.DisplayImage(strImgURL, img);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return vi;
    }
    Date getDatefromString(String strDate){

        Calendar cal = Calendar.getInstance();
//        TimeZone tz = cal.getTimeZone();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'+00:00'", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("Etc/UTC"));
//        format.setTimeZone(tz);
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




}