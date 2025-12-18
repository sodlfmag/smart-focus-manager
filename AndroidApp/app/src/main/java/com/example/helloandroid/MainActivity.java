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
import java.util.Collections;
import java.util.Comparator;
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
            Log.w(TAG, "applyDateFilter: allPostDataList가 null이거나 비어있음");
            allPostDataList = postDataList != null ? new ArrayList<>(postDataList) : new ArrayList<>();
        }
        
        if (filterStartDate == null && filterEndDate == null) {
            // 필터 없음 - 전체 표시
            postDataList = new ArrayList<>(allPostDataList);
            textView.setText("데이터 로드 성공! (" + postDataList.size() + "개)");
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
            textView.setText("필터링된 데이터: " + postDataList.size() + "개");
        }
        
        // RecyclerView 업데이트
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        if (recyclerView != null && postDataList != null) {
            ImageAdapter adapter = new ImageAdapter(postDataList);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(adapter);
            Log.d(TAG, "applyDateFilter: RecyclerView 업데이트 완료. 데이터 개수: " + postDataList.size());
        } else {
            Log.w(TAG, "applyDateFilter: RecyclerView가 null이거나 postDataList가 null");
        }
        
        // 대시보드 업데이트 (필터 적용 후)
        updateDashboard();
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
            textView.setText("데이터를 불러오는 중...");
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
                    Log.d(TAG, "서버 응답 받음. 길이: " + strJson.length());
                    
                    // JSON 응답 파싱 (results 배열 또는 직접 배열)
                    JSONObject jsonResponse = new JSONObject(strJson);
                    JSONArray aryJson;
                    
                    // 페이지네이션 확인
                    if (jsonResponse.has("results")) {
                        // 페이지네이션된 응답
                        aryJson = jsonResponse.getJSONArray("results");
                        int count = jsonResponse.optInt("count", aryJson.length());
                        String nextUrl = null;
                        if (!jsonResponse.isNull("next")) {
                            nextUrl = jsonResponse.optString("next", null);
                        }
                        Log.d(TAG, String.format("페이지네이션 응답 - 현재 페이지: %d개, 전체: %d개, 다음 페이지: %s", 
                            aryJson.length(), count, nextUrl != null && !nextUrl.equals("null") ? "있음" : "없음"));
                        
                        // 다음 페이지가 있으면 모든 페이지 가져오기
                        while (nextUrl != null && !nextUrl.isEmpty() && !nextUrl.equals("null")) {
                            Log.d(TAG, "다음 페이지 가져오기: " + nextUrl);
                            try {
                                URL nextPageUrl = new URL(nextUrl);
                                HttpURLConnection nextConn = (HttpURLConnection) nextPageUrl.openConnection();
                                nextConn.setRequestProperty("Authorization", "Token " + token);
                                nextConn.setRequestMethod("GET");
                                nextConn.setConnectTimeout(5000);
                                nextConn.setReadTimeout(5000);
                                
                                int nextResponseCode = nextConn.getResponseCode();
                                if (nextResponseCode == HttpURLConnection.HTTP_OK) {
                                    InputStream nextIs = nextConn.getInputStream();
                                    BufferedReader nextReader = new BufferedReader(new InputStreamReader(nextIs));
                                    StringBuilder nextResult = new StringBuilder();
                                    String nextLine;
                                    while ((nextLine = nextReader.readLine()) != null) {
                                        nextResult.append(nextLine);
                                    }
                                    nextIs.close();
                                    
                                    JSONObject nextJsonResponse = new JSONObject(nextResult.toString());
                                    JSONArray nextAryJson = nextJsonResponse.getJSONArray("results");
                                    
                                    // 현재 배열에 추가
                                    for (int j = 0; j < nextAryJson.length(); j++) {
                                        aryJson.put(nextAryJson.get(j));
                                    }
                                    
                                    // 다음 페이지 URL 확인
                                    if (nextJsonResponse.isNull("next")) {
                                        nextUrl = null;
                                    } else {
                                        nextUrl = nextJsonResponse.optString("next", null);
                                        if (nextUrl != null && nextUrl.equals("null")) {
                                            nextUrl = null;
                                        }
                                    }
                                    Log.d(TAG, String.format("다음 페이지 추가 완료. 현재 총: %d개, 다음 페이지: %s", 
                                        aryJson.length(), nextUrl != null ? "있음" : "없음"));
                                } else {
                                    Log.e(TAG, "다음 페이지 가져오기 실패: " + nextResponseCode);
                                    break;
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "다음 페이지 가져오기 중 예외 발생", e);
                                break;
                            }
                        }
                    } else {
                        // 직접 배열
                        aryJson = new JSONArray(strJson);
                        Log.d(TAG, "직접 배열 응답: " + aryJson.length() + "개");
                    }
                    
                    // 배열 내 메타데이터만 파싱 (이미지 다운로드 제거)
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                    Log.d(TAG, "데이터 로딩 시작. 총 " + aryJson.length() + "개 항목");
                    for (int i = 0; i < aryJson.length(); i++) {
                        post_json = (JSONObject) aryJson.get(i);
                        imageUrl = post_json.optString("image", null);
                        String title = post_json.optString("title", "");
                        String text = post_json.optString("text", "");
                        String createdDateStr = post_json.optString("created_date", "");
                        
                        Date createdDate = null;
                        if (!createdDateStr.isEmpty()) {
                            try {
                                // ISO 8601 형식 파싱 (예: "2025-11-17T23:27:30.369366+09:00" 또는 "2025-12-18T10:30:00Z")
                                String dateStr = createdDateStr;
                                // 타임존 제거 (+, -, Z 제거)
                                if (dateStr.contains("+")) {
                                    dateStr = dateStr.split("\\+")[0];
                                } else if (dateStr.contains("-") && dateStr.lastIndexOf("-") > 10) {
                                    // 타임존이 -05:00 형식인 경우
                                    int lastDashIndex = dateStr.lastIndexOf("-");
                                    if (lastDashIndex > 10) {
                                        dateStr = dateStr.substring(0, lastDashIndex);
                                    }
                                } else if (dateStr.endsWith("Z")) {
                                    dateStr = dateStr.substring(0, dateStr.length() - 1);
                                }
                                // 밀리초 제거
                                if (dateStr.contains(".")) {
                                    dateStr = dateStr.split("\\.")[0];
                                }
                                createdDate = sdf.parse(dateStr);
                                Log.d(TAG, String.format("Post 파싱 성공 - title: %s, 원본: %s, 파싱: %s", title, createdDateStr, dateStr));
                            } catch (ParseException e) {
                                Log.e(TAG, "날짜 파싱 실패: " + createdDateStr, e);
                                e.printStackTrace();
                            }
                        } else {
                            Log.w(TAG, "created_date가 비어있음 - title: " + title);
                        }
                        
                        // 이미지 다운로드 없이 메타데이터만으로 PostData 생성 (Bitmap은 null)
                        PostData postData = new PostData(null, title, text, imageUrl, createdDate);
                        postList.add(postData);
                    }
                    
                    // 날짜순으로 정렬 (최신순 - 내림차순)
                    Collections.sort(postList, new Comparator<PostData>() {
                        @Override
                        public int compare(PostData p1, PostData p2) {
                            Date d1 = p1.getCreatedDate();
                            Date d2 = p2.getCreatedDate();
                            if (d1 == null && d2 == null) return 0;
                            if (d1 == null) return 1;  // null은 뒤로
                            if (d2 == null) return -1; // null은 뒤로
                            return d2.compareTo(d1); // 최신순 (내림차순)
                        }
                    });
                    
                    // 로드된 데이터의 날짜 범위 로그
                    if (!postList.isEmpty()) {
                        Date firstDate = postList.get(0).getCreatedDate();
                        Date lastDate = postList.get(postList.size() - 1).getCreatedDate();
                        SimpleDateFormat logSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                        Log.d(TAG, String.format("데이터 로딩 완료. 총 %d개 Post 생성. 날짜 범위: %s ~ %s", 
                            postList.size(),
                            firstDate != null ? logSdf.format(firstDate) : "null",
                            lastDate != null ? logSdf.format(lastDate) : "null"));
                    } else {
                        Log.d(TAG, "데이터 로딩 완료. 총 0개 Post 생성");
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
            
            Log.d(TAG, "CloadImage 완료. 데이터 개수: " + (posts != null ? posts.size() : 0));
            
            // 로드된 데이터의 날짜 범위 로그
            if (posts != null && !posts.isEmpty()) {
                Date firstDate = posts.get(0).getCreatedDate();
                Date lastDate = posts.get(posts.size() - 1).getCreatedDate();
                SimpleDateFormat logSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                Log.d(TAG, String.format("동기화 완료 - 첫 번째 데이터: %s, 마지막 데이터: %s", 
                    firstDate != null ? logSdf.format(firstDate) : "null",
                    lastDate != null ? logSdf.format(lastDate) : "null"));
            }
            
            if (posts == null || posts.isEmpty()) {
                textView.setText("불러올 데이터가 없습니다.");
                Log.w(TAG, "데이터가 비어있음");
                allPostDataList = new ArrayList<>();
                postDataList = new ArrayList<>();
                // 대시보드 업데이트 (빈 데이터)
                updateDashboard();
            } else {
                // 전체 데이터 저장 (새 리스트로 복사하여 참조 문제 방지)
                allPostDataList = new ArrayList<>(posts);
                Log.d(TAG, "allPostDataList 업데이트 완료. 개수: " + allPostDataList.size());
                
                // 필터가 적용되어 있는지 확인
                boolean hasFilter = filterStartDate != null || filterEndDate != null;
                
                if (hasFilter) {
                    // 필터가 적용되어 있으면 필터를 다시 적용
                    Log.d(TAG, "동기화 후 필터 재적용");
                    applyDateFilter();
                } else {
                    // 필터가 없으면 전체 데이터 표시 (이미 정렬됨)
                    postDataList = new ArrayList<>(posts);
                    textView.setText("데이터 로드 성공! (" + posts.size() + "개)");
                    RecyclerView recyclerView = findViewById(R.id.recyclerView);
                    if (recyclerView != null) {
                        ImageAdapter adapter = new ImageAdapter(postDataList);
                        recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
                        recyclerView.setAdapter(adapter);
                        Log.d(TAG, "RecyclerView 어댑터 설정 완료. 표시할 데이터 개수: " + postDataList.size());
                    } else {
                        Log.e(TAG, "RecyclerView가 null입니다!");
                    }
                    // 대시보드 업데이트 (allPostDataList가 설정된 후)
                    Log.d(TAG, "대시보드 업데이트 호출 전. allPostDataList 개수: " + (allPostDataList != null ? allPostDataList.size() : 0));
                    updateDashboard();
                }
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
        Log.d(TAG, "checkSystemStatus: 서버 상태 확인 시작");
        CheckStatusTask task = new CheckStatusTask();
        task.execute();
    }
    
    private void updateDashboard() {
        // 대시보드 업데이트: 필터 적용 시 필터 범위, 필터 없으면 오늘 날짜 기준
        // allPostDataList가 null이거나 비어있으면 postDataList에서 복사
        if (allPostDataList == null || allPostDataList.isEmpty()) {
            Log.w(TAG, "updateDashboard: allPostDataList가 null이거나 비어있음. postDataList에서 복사");
            allPostDataList = postDataList != null ? new ArrayList<>(postDataList) : new ArrayList<>();
        }
        
        List<PostData> dataToAnalyze;
        SimpleDateFormat logSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        
        // 필터가 적용된 경우 필터링된 데이터 사용, 없으면 오늘 날짜 기준
        if (filterStartDate != null || filterEndDate != null) {
            // 필터 적용된 데이터 기준
            dataToAnalyze = postDataList != null ? postDataList : new ArrayList<>();
            Log.d(TAG, "대시보드: 필터 범위 기준으로 통계 계산. 데이터 개수: " + dataToAnalyze.size());
        } else {
            // 오늘 날짜 기준
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);
            Date todayStart = today.getTime();
            
            Calendar tomorrow = Calendar.getInstance();
            tomorrow.set(Calendar.HOUR_OF_DAY, 0);
            tomorrow.set(Calendar.MINUTE, 0);
            tomorrow.set(Calendar.SECOND, 0);
            tomorrow.set(Calendar.MILLISECOND, 0);
            tomorrow.add(Calendar.DAY_OF_MONTH, 1);
            Date tomorrowStart = tomorrow.getTime();
            
            Log.d(TAG, String.format("대시보드: 오늘 날짜 기준 필터링. 오늘 시작: %s, 내일 시작: %s", 
                logSdf.format(todayStart), logSdf.format(tomorrowStart)));
            Log.d(TAG, "대시보드: 전체 데이터 개수: " + (allPostDataList != null ? allPostDataList.size() : 0));
            
            dataToAnalyze = new ArrayList<>();
            if (allPostDataList != null && !allPostDataList.isEmpty()) {
                for (PostData post : allPostDataList) {
                    Date postDate = post.getCreatedDate();
                    // 오늘 포함: postDate >= todayStart && postDate < tomorrowStart
                    if (postDate != null) {
                        // 날짜만 비교 (시간 무시)
                        Calendar postCal = Calendar.getInstance();
                        postCal.setTime(postDate);
                        Calendar todayCal = Calendar.getInstance();
                        todayCal.setTime(todayStart);
                        
                        // 같은 년, 월, 일인지 확인
                        boolean isToday = (postCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                                          postCal.get(Calendar.MONTH) == todayCal.get(Calendar.MONTH) &&
                                          postCal.get(Calendar.DAY_OF_MONTH) == todayCal.get(Calendar.DAY_OF_MONTH));
                        
                        if (isToday) {
                            dataToAnalyze.add(post);
                            Log.d(TAG, String.format("대시보드: 오늘 데이터 포함 - title: %s, date: %s", 
                                post.getTitle(), logSdf.format(postDate)));
                        } else {
                            Log.d(TAG, String.format("대시보드: 오늘 데이터 제외 - title: %s, date: %s", 
                                post.getTitle(), logSdf.format(postDate)));
                        }
                    } else {
                        Log.w(TAG, "대시보드: 날짜가 null인 데이터 - title: " + post.getTitle());
                    }
                }
            } else {
                Log.w(TAG, "대시보드: allPostDataList가 null이거나 비어있음");
            }
            Log.d(TAG, "대시보드: 오늘 날짜 기준으로 통계 계산. 데이터 개수: " + dataToAnalyze.size());
        }
        
        int distractedCount = 0;
        int focusCount = 0;
        int totalCount = 0;
        
        for (PostData post : dataToAnalyze) {
            totalCount++;
            String title = post.getTitle();
            if ("Distracted".equals(title)) {
                distractedCount++;
            } else if ("Focus".equals(title)) {
                focusCount++;
            }
        }
        
        Log.d(TAG, String.format("대시보드 통계 - 전체: %d, 딴짓: %d, 집중: %d", totalCount, distractedCount, focusCount));
        
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
            Log.d(TAG, "CheckStatusTask: 서버 상태 조회 시작");
            try {
                URL url = new URL(site_url + "/api/status/");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                
                int responseCode = conn.getResponseCode();
                Log.d(TAG, "CheckStatusTask: 응답 코드 - " + responseCode);
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream is = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                    is.close();
                    
                    String responseText = result.toString();
                    Log.d(TAG, "CheckStatusTask: 응답 본문 - " + responseText);
                    
                    JSONObject jsonResponse = new JSONObject(responseText);
                    boolean isRunning = jsonResponse.getBoolean("is_running");
                    Log.d(TAG, "CheckStatusTask: is_running = " + isRunning);
                    return isRunning;
                } else {
                    Log.e(TAG, "CheckStatusTask: HTTP 오류 - " + responseCode);
                }
            } catch (Exception e) {
                Log.e(TAG, "CheckStatusTask: 예외 발생", e);
                e.printStackTrace();
            }
            return false;
        }
        
        @Override
        protected void onPostExecute(Boolean isRunning) {
            Log.d(TAG, "CheckStatusTask: 완료 - isRunning = " + isRunning);
            if (switchSystemControl != null) {
                switchSystemControl.setChecked(isRunning);
                Log.d(TAG, "CheckStatusTask: 스위치 상태 업데이트 - " + isRunning);
            } else {
                Log.w(TAG, "CheckStatusTask: switchSystemControl이 null입니다");
            }
        }
    }
    
    private class ControlSystemTask extends AsyncTask<String, Void, Boolean> {
        private String command;
        
        @Override
        protected Boolean doInBackground(String... commands) {
            command = commands[0];
            Log.d(TAG, "ControlSystemTask: 명령 전송 시작 - " + command);
            try {
                URL url = new URL(site_url + "/api/status/");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                
                // JSON 데이터 전송
                JSONObject jsonData = new JSONObject();
                jsonData.put("command", command);
                String jsonString = jsonData.toString();
                Log.d(TAG, "ControlSystemTask: 전송할 데이터 - " + jsonString);
                
                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
                dos.writeBytes(jsonString);
                dos.flush();
                dos.close();
                
                int responseCode = conn.getResponseCode();
                Log.d(TAG, "ControlSystemTask: 응답 코드 - " + responseCode);
                
                // 응답 본문 읽기
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream is = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                    is.close();
                    Log.d(TAG, "ControlSystemTask: 응답 본문 - " + result.toString());
                    return true;
                } else {
                    // 에러 응답 읽기
                    InputStream is = conn.getErrorStream();
                    if (is != null) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                        StringBuilder result = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            result.append(line);
                        }
                        is.close();
                        Log.e(TAG, "ControlSystemTask: 에러 응답 - " + result.toString());
                    }
                    return false;
                }
            } catch (Exception e) {
                Log.e(TAG, "ControlSystemTask: 예외 발생", e);
                e.printStackTrace();
                return false;
            }
        }
        
        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                String message = switchSystemControl.isChecked() ? "시스템 시작됨" : "시스템 중지됨";
                Log.d(TAG, "ControlSystemTask: 성공 - " + message);
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
            } else {
                Log.e(TAG, "ControlSystemTask: 실패 - 명령 전송 실패");
                Toast.makeText(getApplicationContext(), "명령 전송 실패", Toast.LENGTH_SHORT).show();
                // 실패 시 스위치 상태 되돌리기
                switchSystemControl.setChecked(!switchSystemControl.isChecked());
            }
        }
    }
}
