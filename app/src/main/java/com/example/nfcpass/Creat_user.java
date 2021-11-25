package com.example.nfcpass;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Creat_user extends AppCompatActivity {

    Button go_shop,go_user,go_history;
    Dialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_creat_user);

        go_user = (Button) findViewById(R.id.go_user);
        go_shop = (Button) findViewById(R.id.go_shop);
        go_history = findViewById(R.id.go_history);

        dialog = new Dialog(Creat_user.this);
        dialog.setContentView(R.layout.user_info_pass);

        go_history.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialog01();
            }
        });


        go_user.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Creat_user.this,Check_user_doc.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
            }
        });

        go_shop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Creat_user.this,Check_shop_doc.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
            }
        });
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

    public void showDialog01() {

        dialog.show();

        EditText shop_info = dialog.findViewById(R.id.shop_info);
        EditText check_code = dialog.findViewById(R.id.check_code);
        Button go_list = dialog.findViewById(R.id.go_list);


        go_list.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(check_code.getText().toString().equals("112255")){
                    Intent intent = new Intent(Creat_user.this,history.class);
                    intent.putExtra("정보",shop_info.getText().toString());
                    startActivity(intent);
                    overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);
                    finish();
                }
                else{
                    Toast.makeText(Creat_user.this,"올바른 조사관 코드를 입력해주세요.",Toast.LENGTH_SHORT).show();
                }
            }
        });


    }
}