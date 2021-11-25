package com.example.nfcpass;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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
class person{
    private String name,time,phonenum;

    person(String time, String name, String phonenum){
        this.time = time;
        this.name = name;
        this.phonenum = phonenum;
    }

    String getName(){return name;}
    String getTime(){return time;}
    String getPhonenum(){return phonenum;}

}
public class history extends AppCompatActivity {

    TextView date;
    Date day;
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    String today;
    long time;
    ListView listview;
    ArrayList<person> list = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        date = findViewById(R.id.date);
        listview = findViewById(R.id.list);

        time = System.currentTimeMillis();
        day = new Date(time);
        today = simpleDateFormat.format(day);
        date.setText(today);

        list.add(new person("2021-11-26-02:21","정재영","010-2342-1232"));
        list.add(new person("2021-11-26-02:32","조주민","010-2321-5463"));

        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, list) ;

        listview.setAdapter(adapter);



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

        for(int i=0; i<list.size();i++ ){
            row = sheet.createRow(i+1);
            cell = row.createCell(0);
            cell.setCellValue(list.get(i).getTime());

            cell=row.createCell(1);
            cell.setCellValue(list.get(i).getName());

            cell=row.createCell(2);
            cell.setCellValue(list.get(i).getPhonenum());
        }

        File xlsFile = new File(getExternalFilesDir(null), "test.xls");
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