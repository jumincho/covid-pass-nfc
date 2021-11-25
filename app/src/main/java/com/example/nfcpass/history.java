package com.example.nfcpass;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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
    ArrayList<String> timelist = new ArrayList<>(); // time
    ArrayList<String> namelist = new ArrayList<>(); // name
    ArrayList<String> phlist = new ArrayList<>(); // ph
    Dialog dialog;
    String shopinfo;

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

        time = System.currentTimeMillis();
        day = new Date(time);
        today = simpleDateFormat.format(day);
        date.setText(today);

        dialog = new Dialog(history.this);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("사업장").document(shopinfo).collection(today).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                for(QueryDocumentSnapshot doc : task.getResult()){
                    namelist.add(doc.getId().substring(0,3));
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

        int size = namelist.size();
        for(int i =0; i < size; i++) {
            db.collection("사업장").document(shopinfo).collection(today).document(namelist.get(i).toString()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    Map<String, Object> map = new HashMap<>();
                    map = task.getResult().getData();


                    timelist.add(map.get("입장시간").toString());
                    phlist.add(map.get("전화번호").toString());
                }
            });
        }



    }

    public void showDialog01(int num) {

        TextView name = dialog.findViewById(R.id.name);
        TextView ph = dialog.findViewById(R.id.phone);
        TextView time = dialog.findViewById(R.id.time);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("사업장").document(shopinfo).collection(today).document(namelist.get(num).toString()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                Map<String,Object> map = new HashMap<>();
                map = task.getResult().getData();

                name.setText(namelist.get(num).substring(0,3));
                time.setText(map.get("입장시간").toString());
                ph.setText(map.get("전화번호").toString());
            }
        });


        dialog.show();
    }



    private void saveExcel(){

        Workbook workbook = new HSSFWorkbook();

        Sheet sheet = workbook.createSheet();//새로운 시트생성

        Row row = sheet.createRow(0); // 새로운 행 생성
        Cell cell;

        cell = row.createCell(0); //1번 셀 생성
        cell.setCellValue("시간"); // 1번 셀 값 입력

        cell = row.createCell(1);
        cell.setCellValue("이름");

        cell = row.createCell(2);
        cell.setCellValue("전화번호");

        int namelistsize = namelist.size();

        for(int i=0; i<namelistsize;i++ ){
            row = sheet.createRow(i+1);
            cell = row.createCell(0);
            cell.setCellValue(timelist.get(i));//시간값

            cell=row.createCell(1);
            cell.setCellValue(namelist.get(i).substring(0,3)); //이름

            cell=row.createCell(2);
            cell.setCellValue(phlist.get(i)); //전화번호
        }

        File xlsFile = new File(getExternalFilesDir(null), "History.xls");
        try{
            FileOutputStream os = new FileOutputStream(xlsFile);
            workbook.write(os);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Toast.makeText(getApplicationContext(),xlsFile.getAbsolutePath()+"에 저장되었습니다",Toast.LENGTH_SHORT).show();

        Uri path = Uri.fromFile(xlsFile);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/excel");
        shareIntent.putExtra(Intent.EXTRA_STREAM,path);
        startActivity(Intent.createChooser(shareIntent,"엑셀내보내기기"));
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

}