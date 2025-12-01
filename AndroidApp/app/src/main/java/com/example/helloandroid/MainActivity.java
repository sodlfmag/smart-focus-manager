package com.example.helloandroid;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    private static final int REQUEST_IMAGE_PICK = 2;
    
    ImageView imgView;
    TextView textView;
    ProgressBar progressBar;
    SwipeRefreshLayout swipeRefreshLayout;
    Switch switchSystemControl;
    TextView tvDistractedCount;
    TextView tvFocusTime;
    // 에뮬레이터: 10.0.2.2, 실제 기기: PC의 IP 주소로 변경 필요 (예: 192.168.0.5)
    // String site_url = "http://10.0.2.2:8000";
    String site_url = "https://sodlfmag.pythonanywhere.com";
    JSONObject post_json;
    String imageUrl = null;
    Bitmap bmImg = null;
    CloadImage taskDownload;
    PutPost taskUpload;
    
    // Upload에 사용할 변수
    private Uri selectedImageUri;
    private Bitmap selectedBitmap;
    
    // Post 데이터 리스트
    private List<PostData> postDataList;
    private List<PostData> allPostDataList;  // 필터링 전 전체 데이터
    private Date filterStartDate = null;
    private Date filterEndDate = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate 호출됨");
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.textView);
        progressBar = findViewById(R.id.progressBar);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        switchSystemControl = findViewById(R.id.switchSystemControl);
        tvDistractedCount = findViewById(R.id.tvDistractedCount);
        tvFocusTime = findViewById(R.id.tvFocusTime);
        
        // Pull-to-Refresh 설정
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                onClickDownload(null);
            }
        });
        
        // 권한 확인
        checkPermissions();
        
        // 앱 시작 시 서버 상태 확인
        checkSystemStatus();
        
        // 저장된 데이터가 있으면 복원
        if (savedInstanceState != null && postDataList != null && !postDataList.isEmpty()) {
            Log.d(TAG, "onCreate: 저장된 데이터 복원 시도");
            restoreRecyclerView();
        } else {
            Log.d(TAG, "onCreate: 저장된 데이터 없음. postDataList=" + (postDataList == null ? "null" : "size=" + postDataList.size()));
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume 호출됨. postDataList=" + (postDataList == null ? "null" : "size=" + postDataList.size()));
        // 다른 Activity에서 돌아왔을 때 데이터 복원
        if (postDataList != null && !postDataList.isEmpty()) {
            Log.d(TAG, "onResume: 데이터 복원 시도");
            restoreRecyclerView();
        } else {
            Log.w(TAG, "onResume: postDataList가 null이거나 비어있음");
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause 호출됨");
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
    
    private void restoreRecyclerView() {
        Log.d(TAG, "restoreRecyclerView 호출됨");
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        if (recyclerView != null && postDataList != null && !postDataList.isEmpty()) {
            Log.d(TAG, "RecyclerView 복원 중. 데이터 개수: " + postDataList.size());
            ImageAdapter adapter = new ImageAdapter(postDataList);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(adapter);
            textView.setText("이미지 로드 성공! (" + postDataList.size() + "개)");
            Log.d(TAG, "RecyclerView 복원 완료");
        } else {
            Log.w(TAG, "RecyclerView 복원 실패. recyclerView=" + (recyclerView == null ? "null" : "not null") + 
                    ", postDataList=" + (postDataList == null ? "null" : "size=" + postDataList.size()));
        }
    }
    
    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
            }
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "권한이 승인되었습니다", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "권한이 필요합니다", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    public void onClickDownload(View v) {
        if (taskDownload != null && taskDownload.getStatus() == AsyncTask.Status.RUNNING) {
            taskDownload.cancel(true);
        }
        taskDownload = new CloadImage();
        taskDownload.execute(site_url + "/api_root/Post/");
        Toast.makeText(getApplicationContext(), "Download", Toast.LENGTH_LONG).show();
    }
    
    public void onClickUpload(View v) {
        // Post에 따른 UI 제공 방식
        // 1단계: 이미지 선택
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }
    
    public void onClickFilter(View v) {
        showDateFilterDialog();
    }
    
    private void showDateFilterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("날짜 범위 선택");
        
        View view = getLayoutInflater().inflate(R.layout.dialog_date_filter, null);
        
        // 날짜 선택을 위한 Calendar 초기화
        final Calendar startCalendar = Calendar.getInstance();
        final Calendar endCalendar = Calendar.getInstance();
        startCalendar.add(Calendar.DAY_OF_MONTH, -7); // 기본값: 7일 전
        
        // 날짜 표시용 TextView (간단한 구현)
        TextView tvStartDate = view.findViewById(R.id.tvStartDate);
        TextView tvEndDate = view.findViewById(R.id.tvEndDate);
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        tvStartDate.setText("시작: " + sdf.format(startCalendar.getTime()));
        tvEndDate.setText("종료: " + sdf.format(endCalendar.getTime()));
        
        // 시작 날짜 선택
        tvStartDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new android.app.DatePickerDialog(MainActivity.this,
                    new android.app.DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(android.widget.DatePicker view, int year, int month, int dayOfMonth) {
                            startCalendar.set(year, month, dayOfMonth);
                            tvStartDate.setText("시작: " + sdf.format(startCalendar.getTime()));
                        }
                    },
                    startCalendar.get(Calendar.YEAR),
                    startCalendar.get(Calendar.MONTH),
                    startCalendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });
        
        // 종료 날짜 선택
        tvEndDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new android.app.DatePickerDialog(MainActivity.this,
                    new android.app.DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(android.widget.DatePicker view, int year, int month, int dayOfMonth) {
                            endCalendar.set(year, month, dayOfMonth);
                            tvEndDate.setText("종료: " + sdf.format(endCalendar.getTime()));
                        }
                    },
                    endCalendar.get(Calendar.YEAR),
                    endCalendar.get(Calendar.MONTH),
                    endCalendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });
        
        builder.setView(view);
        builder.setPositiveButton("적용", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                filterStartDate = startCalendar.getTime();
                filterEndDate = endCalendar.getTime();
                // 종료일은 하루 끝까지 포함
                filterEndDate.setHours(23);
                filterEndDate.setMinutes(59);
                filterEndDate.setSeconds(59);
                applyDateFilter();
            }
        });
        builder.setNeutralButton("초기화", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                filterStartDate = null;
                filterEndDate = null;
                applyDateFilter();
            }
        });
        builder.setNegativeButton("취소", null);
        builder.show();
    }
    
    private void applyDateFilter() {
        if (allPostDataList == null || allPostDataList.isEmpty()) {
            allPostDataList = postDataList != null ? new ArrayList<>(postDataList) : new ArrayList<>();
        }
        
        if (filterStartDate == null && filterEndDate == null) {
            // 필터 없음 - 전체 표시
            postDataList = new ArrayList<>(allPostDataList);
        } else {
            // 날짜 필터 적용
            postDataList = new ArrayList<>();
            for (PostData post : allPostDataList) {
                Date postDate = post.getCreatedDate();
                if (postDate == null) continue;
                
                boolean matches = true;
                if (filterStartDate != null && postDate.before(filterStartDate)) {
                    matches = false;
                }
                if (filterEndDate != null && postDate.after(filterEndDate)) {
                    matches = false;
                }
                
                if (matches) {
                    postDataList.add(post);
                }
            }
        }
        
        // RecyclerView 업데이트
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        if (recyclerView != null && postDataList != null) {
            ImageAdapter adapter = new ImageAdapter(postDataList);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(adapter);
            textView.setText("필터링된 이미지: " + postDataList.size() + "개");
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            try {
                InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
                selectedBitmap = BitmapFactory.decodeStream(inputStream);
                inputStream.close();
                
                // 2단계: 제목과 텍스트 입력 다이얼로그
                showUploadDialog();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "이미지 로드 실패", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void showUploadDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("새 이미지 게시");
        
        View view = getLayoutInflater().inflate(R.layout.dialog_upload, null);
        EditText editTitle = view.findViewById(R.id.editTitle);
        EditText editText = view.findViewById(R.id.editText);
        ImageView previewImage = view.findViewById(R.id.previewImage);
        
        previewImage.setImageBitmap(selectedBitmap);
        
        builder.setView(view);
        builder.setPositiveButton("업로드", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String title = editTitle.getText().toString();
                String text = editText.getText().toString();
                
                if (title.isEmpty() || text.isEmpty()) {
                    Toast.makeText(MainActivity.this, "제목과 내용을 입력하세요", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // 3단계: 업로드 실행
                taskUpload = new PutPost();
                taskUpload.execute(title, text);
            }
        });
        builder.setNegativeButton("취소", null);
        builder.show();
    }
    
    private class CloadImage extends AsyncTask<String, Integer, List<PostData>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
            textView.setText("이미지를 불러오는 중...");
        }
        
        @Override
        protected List<PostData> doInBackground(String... urls) {
            List<PostData> postList = new ArrayList<>();
            try {
                String apiUrl = urls[0];
                String token = "e79ef213eae997b907ae570486118e9486e51662";
                URL urlAPI = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) urlAPI.openConnection();
                conn.setRequestProperty("Authorization", "Token " + token);
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream is = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                    is.close();
                    String strJson = result.toString();
                    
                    // JSON 응답 파싱 (results 배열 또는 직접 배열)
                    JSONObject jsonResponse = new JSONObject(strJson);
                    JSONArray aryJson;
                    if (jsonResponse.has("results")) {
                        aryJson = jsonResponse.getJSONArray("results");
                    } else {
                        aryJson = new JSONArray(strJson);
                    }
                    
                    // 배열 내 모든 이미지 다운로드 및 메타데이터 저장
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                    for (int i = 0; i < aryJson.length(); i++) {
                        post_json = (JSONObject) aryJson.get(i);
                        imageUrl = post_json.optString("image", null);
                        String title = post_json.optString("title", "");
                        String text = post_json.optString("text", "");
                        String createdDateStr = post_json.optString("created_date", "");
                        
                        Date createdDate = null;
                        if (!createdDateStr.isEmpty()) {
                            try {
                                // ISO 8601 형식 파싱 (예: "2025-11-17T23:27:30.369366+09:00")
                                String dateStr = createdDateStr.split("\\+")[0].split("\\.")[0]; // 타임존 제거
                                createdDate = sdf.parse(dateStr);
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                        }
                        
                        if (imageUrl != null && !imageUrl.equals("null") && !imageUrl.isEmpty()) {
                            URL myImageUrl = new URL(imageUrl);
                            conn = (HttpURLConnection) myImageUrl.openConnection();
                            InputStream imgStream = conn.getInputStream();
                            Bitmap imageBitmap = BitmapFactory.decodeStream(imgStream);
                            if (imageBitmap != null) {
                                PostData postData = new PostData(imageBitmap, title, text, imageUrl, createdDate);
                                postList.add(postData); // 포스트 데이터 리스트에 추가
                            }
                            imgStream.close();
                        }
                    }
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return postList;
        }
        
        @Override
        protected void onPostExecute(List<PostData> posts) {
            progressBar.setVisibility(View.GONE);
            swipeRefreshLayout.setRefreshing(false);
            allPostDataList = posts;  // 전체 데이터 저장
            postDataList = posts;
            Log.d(TAG, "CloadImage 완료. 데이터 개수: " + (posts != null ? posts.size() : 0));
            if (posts == null || posts.isEmpty()) {
                textView.setText("불러올 이미지가 없습니다.");
                Log.w(TAG, "이미지 데이터가 비어있음");
            } else {
                textView.setText("이미지 로드 성공! (" + posts.size() + "개)");
                RecyclerView recyclerView = findViewById(R.id.recyclerView);
                if (recyclerView != null) {
                    ImageAdapter adapter = new ImageAdapter(posts);
                    recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
                    recyclerView.setAdapter(adapter);
                    Log.d(TAG, "RecyclerView 어댑터 설정 완료");
                } else {
                    Log.e(TAG, "RecyclerView가 null입니다!");
                }
                // 대시보드 업데이트
                updateDashboard();
            }
        }
    }
    
    private class PutPost extends AsyncTask<String, Void, Boolean> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
            textView.setText("업로드 중...");
        }
        
        @Override
        protected Boolean doInBackground(String... params) {
            String title = params[0];
            String text = params[1];
            
            try {
                String token = "e79ef213eae997b907ae570486118e9486e51662";
                String apiUrl = site_url + "/api_root/Post/";
                
                URL url = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Token " + token);
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW");
                conn.setDoOutput(true);
                conn.setDoInput(true);
                
                // Multipart 요청 본문 생성
                String boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW";
                String lineEnd = "\r\n";
                String twoHyphens = "--";
                
                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
                
                // Title 필드
                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"title\"" + lineEnd);
                dos.writeBytes(lineEnd);
                dos.writeBytes(title + lineEnd);
                
                // Text 필드
                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"text\"" + lineEnd);
                dos.writeBytes(lineEnd);
                dos.writeBytes(text + lineEnd);
                
                // Author 필드
                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"author\"" + lineEnd);
                dos.writeBytes(lineEnd);
                dos.writeBytes("1" + lineEnd); // 관리자 ID
                
                // Image 필드
                if (selectedBitmap != null) {
                    dos.writeBytes(twoHyphens + boundary + lineEnd);
                    dos.writeBytes("Content-Disposition: form-data; name=\"image\"; filename=\"image.jpg\"" + lineEnd);
                    dos.writeBytes("Content-Type: image/jpeg" + lineEnd);
                    dos.writeBytes(lineEnd);
                    
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    selectedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                    byte[] imageBytes = baos.toByteArray();
                    dos.write(imageBytes);
                    dos.writeBytes(lineEnd);
                }
                
                // 마지막 boundary
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
                dos.flush();
                dos.close();
                
                int responseCode = conn.getResponseCode();
                return responseCode == HttpURLConnection.HTTP_CREATED || responseCode == HttpURLConnection.HTTP_OK;
                
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        
        @Override
        protected void onPostExecute(Boolean success) {
            progressBar.setVisibility(View.GONE);
            if (success) {
                Toast.makeText(getApplicationContext(), "Upload 성공!", Toast.LENGTH_LONG).show();
                selectedBitmap = null;
                selectedImageUri = null;
                textView.setText("업로드 완료! 동기화 버튼을 눌러 확인하세요.");
            } else {
                Toast.makeText(getApplicationContext(), "Upload 실패", Toast.LENGTH_LONG).show();
                textView.setText("업로드 실패. 다시 시도해주세요.");
            }
        }
    }
    
    // 원격 제어 관련 메서드들
    public void onSystemControlToggle(View view) {
        Switch switchControl = (Switch) view;
        boolean isChecked = switchControl.isChecked();
        
        // 서버에 명령 전송
        ControlSystemTask task = new ControlSystemTask();
        task.execute(isChecked ? "START" : "STOP");
    }
    
    private void checkSystemStatus() {
        // 서버에서 현재 상태 확인
        CheckStatusTask task = new CheckStatusTask();
        task.execute();
    }
    
    private void updateDashboard() {
        // 대시보드 업데이트: 오늘 날짜 기준 Distracted 카운트
        if (allPostDataList == null) {
            allPostDataList = postDataList != null ? new ArrayList<>(postDataList) : new ArrayList<>();
        }
        
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        
        int distractedCount = 0;
        int focusCount = 0;
        int totalCount = 0;
        
        for (PostData post : allPostDataList) {
            Date postDate = post.getCreatedDate();
            if (postDate != null && postDate.after(today.getTime())) {
                totalCount++;
                String title = post.getTitle();
                if ("Distracted".equals(title)) {
                    distractedCount++;
                } else if ("Focus".equals(title)) {
                    focusCount++;
                }
            }
        }
        
        tvDistractedCount.setText(String.valueOf(distractedCount));
        
        // 집중 시간 비율 계산
        if (totalCount > 0) {
            int focusPercent = (focusCount * 100) / totalCount;
            tvFocusTime.setText(focusPercent + "%");
        } else {
            tvFocusTime.setText("0%");
        }
    }
    
    private class CheckStatusTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                URL url = new URL(site_url + "/api/status/");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
                
                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream is = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                    is.close();
                    
                    JSONObject jsonResponse = new JSONObject(result.toString());
                    return jsonResponse.getBoolean("is_running");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }
        
        @Override
        protected void onPostExecute(Boolean isRunning) {
            if (switchSystemControl != null) {
                switchSystemControl.setChecked(isRunning);
            }
        }
    }
    
    private class ControlSystemTask extends AsyncTask<String, Void, Boolean> {
        @Override
        protected Boolean doInBackground(String... commands) {
            String command = commands[0];
            try {
                URL url = new URL(site_url + "/api/status/");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
                
                // JSON 데이터 전송
                JSONObject jsonData = new JSONObject();
                jsonData.put("command", command);
                
                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
                dos.writeBytes(jsonData.toString());
                dos.flush();
                dos.close();
                
                int responseCode = conn.getResponseCode();
                return responseCode == HttpURLConnection.HTTP_OK;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        
        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                Toast.makeText(getApplicationContext(), 
                    switchSystemControl.isChecked() ? "시스템 시작됨" : "시스템 중지됨", 
                    Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "명령 전송 실패", Toast.LENGTH_SHORT).show();
                // 실패 시 스위치 상태 되돌리기
                switchSystemControl.setChecked(!switchSystemControl.isChecked());
            }
        }
    }
}
