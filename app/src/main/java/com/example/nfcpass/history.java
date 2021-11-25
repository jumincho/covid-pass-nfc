package com.example.nfcpass;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public class history extends AppCompatActivity {

    TextView date;
    Date day;
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    String today;
    long time;
    ListView listview;
    public static ArrayList<String> namelist = new ArrayList<>(); // name
    Dialog dialog;
    String shopinfo;
    TextView total;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        Intent intent = getIntent();
        try {
            shopinfo = intent.getStringExtra("정보");
        } catch (Exception e) {
            e.printStackTrace();
        }

        date = findViewById(R.id.date);
        listview = findViewById(R.id.list);
        total = findViewById(R.id.total);

        time = System.currentTimeMillis();
        day = new Date(time);
        today = simpleDateFormat.format(day);
        date.setText(today);

        dialog = new Dialog(history.this);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("사업장").document(shopinfo).collection(today).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                for (QueryDocumentSnapshot doc : task.getResult()) {

                    namelist.add(doc.getId().substring(0, 3));
                    total.setText("총 입장 인원 : " + namelist.size() + " 명");
                }
                ArrayAdapter adapter = new ArrayAdapter(history.this, android.R.layout.simple_list_item_1, namelist);
                listview.setAdapter(adapter);
            }
        });



        dialog.setContentView(R.layout.user_info);
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                showDialog01(i);
            }
        });


    }

    public void showDialog01(int num) {

        TextView name = dialog.findViewById(R.id.name);
        TextView ph = dialog.findViewById(R.id.phone);
        TextView time = dialog.findViewById(R.id.time);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("사업장").document(shopinfo).collection(today).document(namelist.get(num).toString()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                Map<String, Object> map = new HashMap<>();
                map = task.getResult().getData();

                name.setText(namelist.get(num).substring(0, 3));
                time.setText(map.get("입장시간").toString());
                ph.setText(map.get("전화번호").toString());
            }
        });


        dialog.show();
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