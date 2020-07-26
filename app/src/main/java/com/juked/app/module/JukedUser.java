package com.juked.app.module;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * Created by imac on 7/21/14.
 */
public class JukedUser implements Serializable {
    public String id;
    public String facebookid;
    public String first_name;
    public String last_name;
    public String email;
    public String sex;
    public String birth_date;
    public String year_born;
    public String access_token;


    public JukedUser(JSONObject obj){
        try {
            id = obj.getString("id");
            facebookid = obj.getString("facebook_id");
            first_name = obj.getString("first_name");
            last_name = obj.getString("last_name");
            email = obj.getString("email");
            sex = obj.getString("sex");
            birth_date = obj.getString("birth_date");
            year_born = obj.getString("year_born");
            access_token = obj.getString("access_token");
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
}
