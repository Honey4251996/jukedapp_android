package com.juked.app.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import android.content.Context;
import android.content.SharedPreferences;

import com.juked.app.module.JukedUser;

public class Utils {
	
	static String kPreferenceName = "SMELT";
	
    public static void CopyStream(InputStream is, OutputStream os)
    {
        final int buffer_size=1024;
        try
        {
            byte[] bytes=new byte[buffer_size];
            for(;;)
            {
              int count=is.read(bytes, 0, buffer_size);
              if(count==-1)
                  break;
              os.write(bytes, 0, count);
            }
        }
        catch(Exception ex){}
    }

    public static void saveTimeStamp(Context con, String strTime){
        SharedPreferences sp =  con.getSharedPreferences(kPreferenceName,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("time", strTime);

        editor.commit();
    }
    public static String getTimeStamp(Context con){
        SharedPreferences sp =  con.getSharedPreferences(kPreferenceName,
                Context.MODE_PRIVATE);
        return sp.getString("time", "");
    }
    public static void saveUserInfo(Context con, boolean status, JukedUser user){
		SharedPreferences sp =  con.getSharedPreferences(kPreferenceName,
				Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sp.edit();
		editor.putBoolean("logged", status);
        try {
            editor.putString("userinfo", ObjectSerializer.serialize(user));
        } catch (IOException e) {
            e.printStackTrace();
        }

		editor.commit();
	}
	
	public static Boolean getUserStatus(Context con){
		SharedPreferences sp =  con.getSharedPreferences(kPreferenceName,
				Context.MODE_PRIVATE);
		return sp.getBoolean("logged", false);
	}

	public static JukedUser getUserinfo(Context con) throws IOException {
		SharedPreferences sp =  con.getSharedPreferences(kPreferenceName,
				Context.MODE_PRIVATE);

		return (JukedUser) ObjectSerializer
                .deserialize(sp.getString("userinfo", ""));
		
	}

    public static String formatToYesterdayOrToday(Date dateTime) throws ParseException {
//        Date dateTime = new SimpleDateFormat("EEE hh:mma MMM d, yyyy").parse(date);

        Calendar cal = Calendar.getInstance();
        TimeZone tz = cal.getTimeZone();
        String strLocal = tz.getDisplayName();

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(tz);
        calendar.setTime(dateTime);
        Calendar today = Calendar.getInstance();
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DATE, -1);
        DateFormat timeFormatter = new SimpleDateFormat("hh:mma");
        timeFormatter.setTimeZone(tz);
        if (calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) && calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
            return "Today " + timeFormatter.format(dateTime);
        } else if (calendar.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) && calendar.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR)) {
            return "Yesterday " + timeFormatter.format(dateTime);
        } else {
            SimpleDateFormat simpleDate = new SimpleDateFormat("yyyy/MM/dd hh:mma");
            simpleDate.setTimeZone(tz);
            String strDt = simpleDate.format(dateTime);
            return strDt;
        }
    }
}