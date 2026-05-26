package com.example.nfcpass;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class Check_shop_doc extends AppCompatActivity {

    String resultName;
    String resultNumber;


    EditText et_BusinessNumber_1;
    EditText et_BusinessNumber_2;
    EditText et_BusinessNumber_3;
    EditText et_BusinessName;
    EditText et_BusinessDate;
    Button btn_BusinessData;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_shop_doc);

        et_BusinessNumber_1 = (EditText) findViewById(R.id.et_BusinessNumber_1);
        et_BusinessNumber_2 = (EditText) findViewById(R.id.et_BusinessNumber_2);
        et_BusinessNumber_3 = (EditText) findViewById(R.id.et_BusinessNumber_3);
        et_BusinessName = (EditText) findViewById(R.id.et_BusinessName);
        et_BusinessDate = (EditText) findViewById(R.id.et_BusinessDate);
        btn_BusinessData = (Button) findViewById(R.id.btn_BusinessData);

        btn_BusinessData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String numberData = (et_BusinessNumber_1.getText().toString().trim() + et_BusinessNumber_2.getText().toString().trim() + et_BusinessNumber_3.getText().toString().trim());
                String nameData = et_BusinessName.getText().toString().trim();
                String dateData = et_BusinessDate.getText().toString().trim();

                if(numberData.equals("") || nameData.equals("") || dateData.equals("")) {
                    Toast.makeText(Check_shop_doc.this,"올바른 값을 입력해주세요.", Toast.LENGTH_SHORT).show();
                }else {


                    String url = "https://api.odcloud.kr/api/nts-businessman/v1/validate?serviceKey="
                            + com.example.nfcpass.BuildConfig.BUSINESS_API_KEY;
                    String jsonData = "{  \"businesses\": [    {      \"b_no\": \""
                            + numberData + "\",      \"start_dt\": \""
                            + dateData + "\",      \"p_nm\": \""
                            + nameData + "\",      \"p_nm2\": \"\",      \"b_nm\": \"\",      \"corp_no\": \"\",      \"b_sector\": \"\",      \"b_type\": \"\"    }  ]}";


                    // AsyncTask를 통해 HttpURLConnection 수행.
                    NetworkTask networkTask = new NetworkTask(url, jsonData);
                    networkTask.execute();
                }

            }
        });
    }


    public class NetworkTask extends AsyncTask<Void, Void, String> {

        private String url;
        private String jsonData;

        public NetworkTask(String url, String jsonData) {

            this.url = url;
            this.jsonData = jsonData;
        }

        @Override
        protected String doInBackground(Void... params) {
            String result; // 요청 결과를 저장할 변수.

            RequestHttpURLConnection requestHttpURLConnection = new RequestHttpURLConnection();
            result = requestHttpURLConnection.request(url, jsonData);

            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            if (s == null) {
                Toast.makeText(Check_shop_doc.this,"확인할 수 없습니다.", Toast.LENGTH_SHORT).show();
                return;
            }
            DataParser dataParser = new DataParser(s);
            if("02".equals(dataParser.getValid())){
                Toast.makeText(Check_shop_doc.this,"확인할 수 없습니다.", Toast.LENGTH_SHORT).show();
            }else {
                resultName = dataParser.getName();
                resultNumber = dataParser.getNo();

                Intent intent = new Intent(Check_shop_doc.this,Nfc_pass_check.class);
                intent.putExtra("값","1");
                intent.putExtra("사업자번호",resultNumber);
                intent.putExtra("사업자이름",resultName);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);
                finish();
            }
         }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);

    }

    private long backKeyPressedTime = 0;

    //뒤로 가기 키를 누르면 입력을 종료 시킨다.
    @Override
    public void onBackPressed() {

        if (System.currentTimeMillis() > backKeyPressedTime + 500) {
            backKeyPressedTime = System.currentTimeMillis();
            return;
        }

        if (System.currentTimeMillis() <= backKeyPressedTime + 500) {

            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setTitle("종료").setMessage("종료 하시겠습니까?");
            AlertDialog.Builder builder1 = builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finishAndRemoveTask();
                }
            });

            builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });

            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        }

    }

}