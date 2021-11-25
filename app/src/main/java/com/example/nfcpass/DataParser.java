package com.example.nfcpass;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DataParser {
    String b_data;
    String b_no;
    String b_name;
    String b_date;
    String valid;
    public DataParser(String b_data){
        this.b_data = b_data;
        b_dataParser();
    }


    public void b_dataParser(){
        try{
            JSONObject jsonObj = new JSONObject(b_data);
            JSONArray jsonArray = jsonObj.getJSONArray("data");
            JSONObject jsonObjData = jsonArray.getJSONObject(0);
            JSONObject jsonObjReq = jsonObjData.getJSONObject("request_param");

            this.b_name = jsonObjReq.getString("p_nm");
            this.b_no = jsonObjData.getString("b_no");
            this.valid = jsonObjData.getString("valid");
            this.b_date = jsonObjData.getString("b_date");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String getNo(){
        return this.b_no;
    }

    public String getName(){
        return this.b_name;
    }

    public String getDate(){
        return this.b_date;
    }

    public String getValid(){
        return this.valid;
    }
}