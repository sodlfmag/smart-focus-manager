package com.example.helloandroid;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ImageDetailActivity extends AppCompatActivity {
    private static final String TAG = "ImageDetailActivity";
    private ImageView detailImageView;
    private TextView detailTitle;
    private TextView detailText;
    private Button btnShare;
    private Button btnSave;
    private ProgressBar progressBar;
    private Bitmap imageBitmap;
    private String title;
    private String text;
    private String imageUrl;
    
    // Bitmap 임시 저장용 static 변수
    private static Bitmap tempBitmap;
    
    public static void setTempBitmap(Bitmap bitmap) {
        tempBitmap = bitmap;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate 호출됨");
        setContentView(R.layout.activity_image_detail);
        
        detailImageView = findViewById(R.id.detailImageView);
        detailTitle = findViewById(R.id.detailTitle);
        detailText = findViewById(R.id.detailText);
        btnShare = findViewById(R.id.btnShare);
        btnSave = findViewById(R.id.btnSave);
        progressBar = findViewById(R.id.progressBar);
        
        // Intent에서 데이터 받기
        Intent intent = getIntent();
        imageUrl = intent.getStringExtra("imageUrl");
        title = intent.getStringExtra("title");
        text = intent.getStringExtra("text");
        
        // 먼저 static 변수에서 Bitmap 확인
        if (tempBitmap != null) {
            imageBitmap = tempBitmap;
            detailImageView.setImageBitmap(imageBitmap);
            tempBitmap = null; // 메모리 해제
        } else if (imageUrl != null && !imageUrl.isEmpty()) {
            // Bitmap이 없으면 URL에서 다운로드
            progressBar.setVisibility(View.VISIBLE);
            new LoadImageTask().execute(imageUrl);
        } else {
            Toast.makeText(this, "이미지를 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
        }
        
        if (title != null && !title.isEmpty()) {
            detailTitle.setText(title);
        } else {
            detailTitle.setText("제목 없음");
        }
        
        if (text != null && !text.isEmpty()) {
            detailText.setText(text);
        } else {
            detailText.setText("내용 없음");
        }
        
        // 공유 버튼 클릭 리스너
        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareImage();
            }
        });
        
        // 저장 버튼 클릭 리스너
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveToGallery();
            }
        });
    }
    
    // 이미지 다운로드 AsyncTask
    private class LoadImageTask extends AsyncTask<String, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(String... urls) {
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                InputStream inputStream = conn.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                inputStream.close();
                return bitmap;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            progressBar.setVisibility(View.GONE);
            if (bitmap != null) {
                imageBitmap = bitmap;
                detailImageView.setImageBitmap(bitmap);
            } else {
                Toast.makeText(ImageDetailActivity.this, "이미지 로드 실패", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void shareImage() {
        Log.d(TAG, "shareImage 호출됨");
        if (imageBitmap == null) {
            Log.w(TAG, "imageBitmap이 null입니다");
            Toast.makeText(this, "이미지를 공유할 수 없습니다", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            Log.d(TAG, "공유 파일 생성 시작");
            // 임시 파일 생성 (고유한 파일명 사용)
            File cachePath = new File(getCacheDir(), "images");
            cachePath.mkdirs();
            String fileName = "shared_image_" + System.currentTimeMillis() + ".jpg";
            File imageFile = new File(cachePath, fileName);
            
            // Bitmap을 파일로 저장
            OutputStream outputStream = new FileOutputStream(imageFile);
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
            outputStream.flush();
            outputStream.close();
            Log.d(TAG, "파일 저장 완료: " + imageFile.getAbsolutePath());
            
            // FileProvider를 사용하여 Uri 생성
            Uri imageUri = FileProvider.getUriForFile(
                this,
                getPackageName() + ".fileprovider",
                imageFile
            );
            Log.d(TAG, "Uri 생성 완료: " + imageUri);
            
            // 공유 Intent 생성
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/jpeg");
            shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
            
            String shareText = "";
            if (title != null && !title.isEmpty()) {
                shareText += title;
            }
            if (text != null && !text.isEmpty()) {
                if (!shareText.isEmpty()) {
                    shareText += "\n";
                }
                shareText += text;
            }
            if (!shareText.isEmpty()) {
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
            }
            
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            // 공유 선택 다이얼로그 표시
            Intent chooser = Intent.createChooser(shareIntent, "이미지 공유");
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Log.d(TAG, "공유 Intent 실행 전");
            try {
                startActivity(chooser);
                Log.d(TAG, "공유 Intent 실행 완료");
            } catch (android.content.ActivityNotFoundException ex) {
                Log.e(TAG, "공유 앱을 찾을 수 없음", ex);
                Toast.makeText(this, "공유할 수 있는 앱이 없습니다", Toast.LENGTH_SHORT).show();
            }
            
        } catch (IOException e) {
            Log.e(TAG, "공유 파일 생성 실패", e);
            e.printStackTrace();
            Toast.makeText(this, "공유 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause 호출됨");
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume 호출됨");
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop 호출됨");
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy 호출됨");
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // 상태 저장
        if (imageUrl != null) {
            outState.putString("imageUrl", imageUrl);
        }
        if (title != null) {
            outState.putString("title", title);
        }
        if (text != null) {
            outState.putString("text", text);
        }
    }
    
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // 상태 복원
        if (savedInstanceState != null) {
            imageUrl = savedInstanceState.getString("imageUrl");
            title = savedInstanceState.getString("title");
            text = savedInstanceState.getString("text");
            
            // 이미지가 없으면 다시 로드
            if (imageBitmap == null && imageUrl != null && !imageUrl.isEmpty()) {
                progressBar.setVisibility(View.VISIBLE);
                new LoadImageTask().execute(imageUrl);
            }
        }
    }
    
    private void saveToGallery() {
        if (imageBitmap == null) {
            Toast.makeText(this, "이미지를 저장할 수 없습니다", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10 이상: MediaStore API 사용
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, 
                    (title != null && !title.isEmpty() ? title : "image") + "_" + System.currentTimeMillis() + ".jpg");
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PhotoBlog");
                
                Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                
                if (uri != null) {
                    OutputStream outputStream = getContentResolver().openOutputStream(uri);
                    if (outputStream != null) {
                        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                        outputStream.close();
                        Toast.makeText(this, "갤러리에 저장되었습니다", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "저장 실패", Toast.LENGTH_SHORT).show();
                }
            } else {
                // Android 9 이하: 기존 방식
                String imageFileName = (title != null && !title.isEmpty() ? title : "image") + "_" + System.currentTimeMillis() + ".jpg";
                File storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "PhotoBlog");
                if (!storageDir.exists()) {
                    storageDir.mkdirs();
                }
                
                File imageFile = new File(storageDir, imageFileName);
                
                OutputStream outputStream = new FileOutputStream(imageFile);
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                outputStream.close();
                
                // 갤러리에 스캔 요청
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                Uri contentUri = Uri.fromFile(imageFile);
                mediaScanIntent.setData(contentUri);
                sendBroadcast(mediaScanIntent);
                
                Toast.makeText(this, "갤러리에 저장되었습니다", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}

