package com.example.nfcpass;

import static android.net.wifi.p2p.WifiP2pManager.ERROR;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import androidx.appcompat.app.AlertDialog;

import com.google.firebase.firestore.FirebaseFirestore;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class Nfc_pass_check extends Activity {

    NfcAdapter nfcAdapter;
    PendingIntent pendingIntent;
    IntentFilter writeTagFilters[];
    boolean writeMode;
    Tag myTag;
    Context context;
    private TextToSpeech tts;
    String check;
    LinearLayout shop_mode,user_mode;
    long timenow,todaynow;
    Date mtime,mtoday;
    SimpleDateFormat mtimeformat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
    SimpleDateFormat todaynowformat = new SimpleDateFormat("yyyy-MM-dd");
    String time,todaytime;
    Long number;
    String name,shop_num,shop_name,Vtry,Vday;
    TextView va_count,user_check_day;
    ImageButton imageButton;




    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc_pass_check);
        context = this;


        shop_mode = findViewById(R.id.shop_mode);
        user_mode = findViewById(R.id.user_mode);
        va_count = findViewById(R.id.va_count);
        user_check_day = findViewById(R.id.user_check_day);
        imageButton = findViewById(R.id.changebutton);

        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != ERROR) {
                    // 언어를 선택한다.
                    tts.setLanguage(Locale.KOREAN);
                }
            }
        });

        Intent intent = getIntent();
        if(intent.getStringExtra("값") != null) { //사업자 모드인지 / 유저 모드인지 구별 해줌
            check = intent.getStringExtra("값");
        }else{
            check = "3";
        }

        if(check.equals("1")){ // 상인 모드
            shop_mode.setVisibility(View.VISIBLE);
            user_mode.setVisibility(View.GONE);
            shop_name = intent.getStringExtra("사업자이름");
            shop_num = intent.getStringExtra("사업자번호");
        } else if (check.equals("2")) { // 사용자 실행 모드
            shop_mode.setVisibility(View.GONE);
            user_mode.setVisibility(View.VISIBLE);
            Vtry = intent.getStringExtra("백신");
            Vday = intent.getStringExtra("인증");
            va_count.setText(Vtry);
            user_check_day.setText(Vday);

        }
        else{ // 빠른 실행모드
            shop_mode.setVisibility(View.GONE);
            user_mode.setVisibility(View.VISIBLE);
            try {
                readUserDatevac();
            } catch (IOException e) {
                e.printStackTrace();
            }
            va_count.setText(Vtry);
            user_check_day.setText(Vday);
        }


        if(check.equals("1")) {
            writeTag wt = new writeTag(shop_num+shop_name); // 이곳에 사업자 정보 입력 "사업자 번호" + "사장님 이름"
            wt.start();
            shop_mode.setVisibility(View.VISIBLE);
        }else{}




        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            // Stop here, we definitely need NFC
            Toast.makeText(this, "NFC를 지원하지 않는 기기 입니다. QR체크인을 진행해 주세요.", Toast.LENGTH_LONG).show();
            finish();
        }

        timenow = System.currentTimeMillis();
        mtime = new Date(timenow);
        time = mtimeformat.format(mtime);


        todaynow = System.currentTimeMillis();
        mtoday = new Date(todaynow);
        todaytime = todaynowformat.format(mtoday);

        try {
            readUserDate();
        } catch (IOException e) {
            e.printStackTrace();
        }
        readFromIntent(getIntent());


        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
        writeTagFilters = new IntentFilter[] { tagDetected };


        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                vibrator.cancel();
                Intent intent1 = new Intent(Nfc_pass_check.this,Creat_user.class);
                startActivity(intent1);
                overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);
                finish();
            }
        });
    }


    private void readFromIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage[] msgs = null;
            if (rawMsgs != null) {
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                }
            }
            buildTagViews(msgs);
        }
    }



    private void buildTagViews(NdefMessage[] msgs) {




        if (msgs == null || msgs.length == 0) return;

        String text = "";
        byte[] payload = msgs[0].getRecords()[0].getPayload();
        String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";
        int languageCodeLength = payload[0] & 0063;

        try {
            // 받아온 Tag 값
            text = new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
        } catch (UnsupportedEncodingException e) {
        }


        if(check.equals("2")||check.equals("3")) {
            tts.setPitch(0.3f);
            tts.setSpeechRate(2.5f);
            tts.speak("입장이 완료 되었습니다.", TextToSpeech.QUEUE_FLUSH, null);
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            long[] a = {300,100,300,100};
            vibrator.vibrate(a,-1); // 0.5초간 진동
            Toast.makeText(this,"입장이 완료 되었습니다.",Toast.LENGTH_LONG).show();
            String user_name = name;
            Map<String,Object> userdate = new HashMap<>();
            userdate.put("전화번호","0"+String.valueOf(number));
            userdate.put("시간",time);
            String rand = UUID.randomUUID().toString();
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("사업장").document(text).collection(todaytime).document(user_name+rand).set(userdate);
            //UUID를 사용하여 이름에 더하여 동명이인 방지 및 여러번 입장 체크



        }else{
            tts.setPitch(0.3f);
            tts.setSpeechRate(2.5f);
            tts.speak("기록이 완료 되었습니다.", TextToSpeech.QUEUE_FLUSH, null);
            Toast.makeText(this,"기록이 완료 되었습니다",Toast.LENGTH_LONG).show();


        }
    }


    /******************************************************************************
     **********************************Write to NFC Tag****************************
     ******************************************************************************/
    private void write(String text, Tag tag) throws IOException, FormatException {
        NdefRecord[] records = { createRecord(text) };
        NdefMessage message = new NdefMessage(records);
        Ndef ndef = Ndef.get(tag);
        ndef.connect();
        ndef.writeNdefMessage(message);
        ndef.close();
    }
    private NdefRecord createRecord(String text) throws UnsupportedEncodingException {
        String lang       = "en";
        byte[] textBytes  = text.getBytes();
        byte[] langBytes  = lang.getBytes("US-ASCII");
        int    langLength = langBytes.length;
        int    textLength = textBytes.length;
        byte[] payload    = new byte[1 + langLength + textLength];


        payload[0] = (byte) langLength;


        System.arraycopy(langBytes, 0, payload, 1,              langLength);
        System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength);

        NdefRecord recordNFC = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,  NdefRecord.RTD_TEXT,  new byte[0], payload);

        return recordNFC;
    }



    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        readFromIntent(intent);
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())){
            myTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        }
    }

    @Override
    public void onPause(){
        super.onPause();
        WriteModeOff();
    }

    @Override
    public void onResume(){
        super.onResume();
        WriteModeOn();
    }

    private void WriteModeOn(){
        writeMode = true;
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, writeTagFilters, null);
    }
    private void WriteModeOff(){
        writeMode = false;
        nfcAdapter.disableForegroundDispatch(this);
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


    class writeTag extends Thread {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        String shopname;


        public writeTag(String name) {
            this.shopname = name;
        }

        @Override
        public void run() {
            long[] a = {1000,100,1000,100};
            vibrator.vibrate(a,0);
            while (true) {
                try {
                    if(myTag != null) {
                        write(shopname, myTag);
                        vibrator.cancel();
                        break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (FormatException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public void readUserDate() throws IOException {
        FileInputStream fis = openFileInput("UserDate.dat");
        DataInputStream dis = new DataInputStream(fis);
        number = dis.readLong();
        name = dis.readUTF();
        dis.close();
    }

    public void readUserDatevac() throws IOException {
        FileInputStream fis = openFileInput("UserDatevac.dat");
        DataInputStream dis = new DataInputStream(fis);
        Vtry = dis.readUTF();
        Vday = dis.readUTF();
        dis.close();
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
