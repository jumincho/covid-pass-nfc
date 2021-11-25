package com.example.nfcpass;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequest;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class Check_user_doc extends AppCompatActivity {
    private static final String CLOUD_VISION_API_KEY = BuildConfig.API_KEY;
    public static final String FILE_NAME = "temp.jpg";
    private static final String ANDROID_CERT_HEADER = "X-Android-Cert";
    private static final String ANDROID_PACKAGE_HEADER = "X-Android-Package";
    private static final int MAX_LABEL_RESULTS = 10;
    private static final int MAX_DIMENSION = 1200;

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int GALLERY_PERMISSIONS_REQUEST = 0;
    private static final int GALLERY_IMAGE_REQUEST = 1;
    public static final int CAMERA_PERMISSIONS_REQUEST = 2;
    public static final int CAMERA_IMAGE_REQUEST = 3;
    private static Context context;
    private static TextView noti;
    ProgressDialog dialog2;

    ImageView mMainImage;
    Button user_check;
    EditText check_edit_name , openday1;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_user_doc);

        mMainImage = findViewById(R.id.imageView);
        user_check = findViewById(R.id.user_check);
        check_edit_name = findViewById(R.id.check_edit_name);
        openday1 = findViewById(R.id.openday1);
        noti = findViewById(R.id.noti);

        user_check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!(check_edit_name.getText().toString().equals("")) && !(openday1.getText().toString().equals("")) && openday1.getText().toString().length() == 11) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(Check_user_doc.this);
                    builder
                            .setMessage("인증서 사진을 불러와주세요.")
                            .setPositiveButton("사진 선택", (dialog, which) -> startGalleryChooser());
                    builder.create().show();
                    try {
                        saveUserDate(check_edit_name.getText().toString(),Long.parseLong(String.valueOf(openday1.getText())));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                else if(openday1.getText().toString().length() != 11){
                    Toast.makeText(Check_user_doc.this,"올바른 번호를 입력해주세요.",Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(Check_user_doc.this,"이름과 번호를 입력해주세요.",Toast.LENGTH_SHORT).show();
                }
            }
        });


        context = this;

    }

    public void startGalleryChooser(){
        if(PermissionUtils.requestPermission(this,GALLERY_PERMISSIONS_REQUEST, Manifest.permission.READ_EXTERNAL_STORAGE)){
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select a photo"),
                    GALLERY_IMAGE_REQUEST);
        }
    }


    public File getCameraFile(){
        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return new File(dir, FILE_NAME);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            uploadImage(data.getData());
        } else if (requestCode == CAMERA_IMAGE_REQUEST && resultCode == RESULT_OK) {
            Uri photoUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", getCameraFile());
            uploadImage(photoUri);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case GALLERY_PERMISSIONS_REQUEST:
                if (PermissionUtils.permissionGranted(requestCode, GALLERY_PERMISSIONS_REQUEST, grantResults)) {
                    startGalleryChooser();
                }
                break;
        }
    }

    public void uploadImage(Uri uri) {
        if (uri != null) {
            dialog2 = new ProgressDialog(Check_user_doc.this);
            dialog2.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            dialog2.setCancelable(false);
            dialog2.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog2.setMessage("확인중");
            dialog2.show();

            try {
                // scale the image to save on bandwidth
                Bitmap bitmap =
                        scaleBitmapDown(
                                MediaStore.Images.Media.getBitmap(getContentResolver(), uri),
                                MAX_DIMENSION);

                callCloudVision(bitmap);
                mMainImage.setImageBitmap(bitmap);//올려놓은 사진의 bitmap 설정해줌
            } catch (IOException e) {
                Log.d(TAG, "Image picking failed because " + e.getMessage());
                Toast.makeText(this, "Something is wrong with that image. Pick a different one please.", Toast.LENGTH_LONG).show();
            }
        } else {
            Log.d(TAG, "Image picker gave us a null image.");
            Toast.makeText(this, "Something is wrong with that image. Pick a different one please.", Toast.LENGTH_LONG).show();
        }
    }

    private Vision.Images.Annotate prepareAnnotationRequest(Bitmap bitmap) throws IOException {
        HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

        VisionRequestInitializer requestInitializer =
                new VisionRequestInitializer(CLOUD_VISION_API_KEY) {
                    /**
                     * We override this so we can inject important identifying fields into the HTTP
                     * headers. This enables use of a restricted cloud platform API key.
                     */
                    @Override
                    protected void initializeVisionRequest(VisionRequest<?> visionRequest)
                            throws IOException {
                        super.initializeVisionRequest(visionRequest);

                        String packageName = getPackageName();
                        visionRequest.getRequestHeaders().set(ANDROID_PACKAGE_HEADER, packageName);

                        String sig = PackageManagerUtils.getSignature(getPackageManager(), packageName);

                        visionRequest.getRequestHeaders().set(ANDROID_CERT_HEADER, sig);
                    }
                };

        Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);
        builder.setVisionRequestInitializer(requestInitializer);

        Vision vision = builder.build();

        BatchAnnotateImagesRequest batchAnnotateImagesRequest =
                new BatchAnnotateImagesRequest();
        batchAnnotateImagesRequest.setRequests(new ArrayList<AnnotateImageRequest>() {{
            AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();

            // Add the image
            Image base64EncodedImage = new Image();
            // Convert the bitmap to a JPEG
            // Just in case it's a format that Android understands but Cloud Vision
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
            byte[] imageBytes = byteArrayOutputStream.toByteArray();

            // Base64 encode the JPEG
            base64EncodedImage.encodeContent(imageBytes);
            annotateImageRequest.setImage(base64EncodedImage);

            // add the features we want
            annotateImageRequest.setFeatures(new ArrayList<Feature>() {{
                Feature labelDetection = new Feature();
                labelDetection.setType("TEXT_DETECTION");
                labelDetection.setMaxResults(MAX_LABEL_RESULTS);
                add(labelDetection);
            }});

            // Add the list of one thing to the request
            add(annotateImageRequest);
        }});

        Vision.Images.Annotate annotateRequest =
                vision.images().annotate(batchAnnotateImagesRequest);
        // Due to a bug: requests to Vision API containing large images fail when GZipped.
        annotateRequest.setDisableGZipContent(true);
        Log.d(TAG, "created Cloud Vision request object, sending request");

        return annotateRequest;
    }

    private void callCloudVision(final Bitmap bitmap) {
        // Switch text to loading


        // Do the real work in an async task, because we need to use the network anyway
        try {
            AsyncTask<Object, Void, String> labelDetectionTask = new LableDetectionTask(this, prepareAnnotationRequest(bitmap));
            labelDetectionTask.execute();
        } catch (IOException e) {
            Log.d(TAG, "failed to make API request because of other IOException " +
                    e.getMessage());
        }
    }

    private class LableDetectionTask extends AsyncTask<Object, Void, String> {
        private final WeakReference<Check_user_doc> Check_UserWeakReference;
        private Vision.Images.Annotate mRequest;



        LableDetectionTask(Check_user_doc activity, Vision.Images.Annotate annotate) {
            Check_UserWeakReference = new WeakReference<>(activity);
            mRequest = annotate;
        }

        @Override
        protected String doInBackground(Object... params) {
            try {
                Log.d(TAG, "created Cloud Vision request object, sending request");
                BatchAnnotateImagesResponse response = mRequest.execute();
                return convertResponseToString(response);

            } catch (GoogleJsonResponseException e) {
                Log.d(TAG, "failed to make API request because " + e.getContent());
            } catch (IOException e) {
                Log.d(TAG, "failed to make API request because of other IOException " +
                        e.getMessage());
            }
            return "Cloud Vision API request failed. Check logs for details.";
        }

        protected void onPostExecute(String result) {
            Check_user_doc activity = Check_UserWeakReference.get();
            if (activity != null && !activity.isFinishing()) {
            }
        }
    }


    private Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {

        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int resizedWidth = maxDimension;
        int resizedHeight = maxDimension;

        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension;
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = maxDimension;
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }

    private String convertResponseToString(BatchAnnotateImagesResponse response) throws IOException {

        StringBuilder message = new StringBuilder("I found these things:\n\n");
        int cnt = 0;
        String Vtry = "3", Vday = "3";
        List<EntityAnnotation> labels = response.getResponses().get(0).getTextAnnotations();
        if (labels != null) {
            for (EntityAnnotation label : labels) {
                cnt++;
                if(label.getDescription().equals("1")||label.getDescription().equals("2")){ // 접종 횟수 여부
                    if(labels.get(cnt).getDescription().equals("차")){
                        Vtry = label.getDescription() + labels.get(cnt).getDescription();
                    }
                }
                else if(label.getDescription().equals("추가")){ //부스터샷 추가접종 여부
                    if(labels.get(cnt).getDescription().equals("접종")){
                        Vtry = label.getDescription() + labels.get(cnt).getDescription();
                    }
                }
                else if(label.getDescription().equals("일자")){
                    Vday = labels.get(cnt).getDescription().substring(0,labels.get(cnt).getDescription().length()-1); //1차접종시 및 부스터샷
                    if(Vday.equals("접")){//2차 접종시
                        Vday = labels.get(cnt+2).getDescription().substring(0,labels.get(cnt+2).getDescription().length()-1);
                        if(Vday.equals("1")){//2차 접종 후 14일 경과시
                            Vday = labels.get(cnt+5).getDescription().substring(0,labels.get(cnt+5).getDescription().length()-1);
                        }
                    }


                }

            }

            if(!(Vtry.equals("3")) && !(Vday.equals("3"))) {
                dialog2.dismiss();
                Intent intent = new Intent(context, Nfc_pass_check.class);
                intent.putExtra("백신", Vtry);
                intent.putExtra("인증", Vday);
                saveUserDatevac(Vtry,Vday);
                context.startActivity(intent);
                finish();
            }else {
                        dialog2.dismiss();
                        notifi notifis = new notifi();
                        notifis.start();

            }

        } else {
            message.append("nothing");
        }

        return message.toString();
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


    class notifi extends Thread {

        @Override
        public void run(){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, "사진을 다시 입력해주세요. (COOV 인증서)", Toast.LENGTH_SHORT).show();
                    mMainImage.setImageBitmap(null);
                }
            });
        }
    }

    public void saveUserDate(String name, long number) throws IOException {
        FileOutputStream fos = openFileOutput("UserDate.dat",MODE_PRIVATE);
        DataOutputStream dos = new DataOutputStream(fos); //데이터를 쓴다.
        dos.writeLong(number);
        dos.writeUTF(name);
        dos.flush();
        dos.close();


    }

    public void saveUserDatevac(String vtry, String vname) throws IOException {
        FileOutputStream fos = openFileOutput("UserDatevac.dat",MODE_PRIVATE);
        DataOutputStream dos = new DataOutputStream(fos); //데이터를 쓴다.
        dos.writeUTF(vtry);
        dos.writeUTF(vname);
        dos.flush();
        dos.close();


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