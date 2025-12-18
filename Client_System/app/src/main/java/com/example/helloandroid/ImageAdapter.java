package com.example.helloandroid;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.lang.ref.WeakReference;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {
    private static final String TAG = "ImageAdapter";
    private static final int MAX_CONCURRENT_LOADS = 5; // 동시에 로드할 수 있는 최대 이미지 수
    private static int activeLoadCount = 0; // 현재 로딩 중인 이미지 수
    
    private List<PostData> postList;
    private String siteUrl;
    
    public ImageAdapter(List<PostData> postList) {
        this(postList, "https://sodlfmag.pythonanywhere.com");
    }
    
    public ImageAdapter(List<PostData> postList, String siteUrl) {
        this.postList = postList;
        this.siteUrl = siteUrl;
    }
    
    @Override
    public ImageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image, parent, false);
        return new ImageViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(ImageViewHolder holder, int position) {
        PostData post = postList.get(position);
        String title = post.getTitle();
        String text = post.getText();
        String imageUrl = post.getImageUrl();
        Date createdDate = post.getCreatedDate();
        
        // 상태별 아이콘 및 색상 설정
        if (title != null) {
            if ("Focus".equals(title)) {
                holder.statusIcon.setText("🎯");
                if (holder.statusText != null) {
                    holder.statusText.setText("집중");
                    holder.statusText.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.focus_dark));
                }
            } else if ("Distracted".equals(title)) {
                holder.statusIcon.setText("📱");
                if (holder.statusText != null) {
                    holder.statusText.setText("딴짓");
                    holder.statusText.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.distracted_dark));
                }
            } else if ("Away".equals(title)) {
                holder.statusIcon.setText("🚶");
                if (holder.statusText != null) {
                    holder.statusText.setText("부재");
                    holder.statusText.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.away_dark));
                }
            } else {
                holder.statusIcon.setText("📋");
                if (holder.statusText != null) {
                    holder.statusText.setText(title);
                    holder.statusText.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.text_primary));
                }
            }
        }
        
        // 상대 시간 표시
        if (holder.timeText != null && createdDate != null) {
            holder.timeText.setText(getRelativeTime(createdDate));
        } else if (holder.timeText != null) {
            holder.timeText.setText("");
        }
        
        // 텍스트 미리보기
        if (holder.textPreview != null) {
            if (text != null && !text.isEmpty()) {
                holder.textPreview.setText(text);
                holder.textPreview.setVisibility(View.VISIBLE);
            } else {
                holder.textPreview.setVisibility(View.GONE);
            }
        }
        
        // 이미지 표시
        Log.d(TAG, String.format("onBindViewHolder[%d] - imageUrl: %s", position, imageUrl));
        if (imageUrl != null && !imageUrl.isEmpty()) {
            // 이미지 인디케이터 표시
            if (holder.imageIndicator != null) {
                holder.imageIndicator.setVisibility(View.VISIBLE);
            }
            
            // 썸네일 이미지 로드
            if (holder.imageView != null) {
                holder.imageView.setVisibility(View.VISIBLE);
                // 전체 URL 생성
                String fullUrl = getFullImageUrl(imageUrl);
                Log.d(TAG, String.format("onBindViewHolder[%d] - fullUrl: %s", position, fullUrl));
                if (fullUrl != null) {
                    // URL이 변경되었는지 확인
                    String currentUrl = (String) holder.imageView.getTag();
                    if (currentUrl != null && !currentUrl.equals(fullUrl)) {
                        // URL이 변경되었으므로 이전 이미지 제거 및 태스크 취소
                        Log.d(TAG, String.format("onBindViewHolder[%d] - URL 변경: %s -> %s", position, currentUrl, fullUrl));
                        holder.cancelImageLoading();
                        holder.imageView.setImageDrawable(null);
                        holder.imageView.setTag(null);
                    }
                    // 이미지 로드 (이미 로드되었으면 스킵됨)
                    loadThumbnailImage(holder, imageUrl);
                } else {
                    Log.w(TAG, "이미지 URL 변환 실패: " + imageUrl);
                    holder.imageView.setVisibility(View.GONE);
                    holder.imageView.setImageDrawable(null);
                    holder.imageView.setTag(null);
                }
            }
        } else {
            if (holder.imageIndicator != null) {
                holder.imageIndicator.setVisibility(View.GONE);
            }
            if (holder.imageView != null) {
                holder.cancelImageLoading();
                holder.imageView.setVisibility(View.GONE);
                holder.imageView.setTag(null);
                holder.imageView.setImageDrawable(null);
            }
        }
        
        // 클릭 리스너 설정
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), ImageDetailActivity.class);
                intent.putExtra("imageUrl", imageUrl);
                intent.putExtra("title", title);
                intent.putExtra("text", text);
                if (createdDate != null) {
                    intent.putExtra("createdDate", createdDate.getTime());
                }
                v.getContext().startActivity(intent);
            }
        });
    }
    
    private String getRelativeTime(Date date) {
        if (date == null) return "";
        
        Calendar now = Calendar.getInstance();
        Calendar then = Calendar.getInstance();
        then.setTime(date);
        
        long diffInMillis = now.getTimeInMillis() - then.getTimeInMillis();
        long diffInSeconds = diffInMillis / 1000;
        long diffInMinutes = diffInSeconds / 60;
        long diffInHours = diffInMinutes / 60;
        long diffInDays = diffInHours / 24;
        
        // 같은 날인지 확인
        boolean isSameDay = (now.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
                            now.get(Calendar.MONTH) == then.get(Calendar.MONTH) &&
                            now.get(Calendar.DAY_OF_MONTH) == then.get(Calendar.DAY_OF_MONTH));
        
        if (isSameDay) {
            if (diffInMinutes < 1) {
                return "방금 전";
            } else if (diffInMinutes < 60) {
                return diffInMinutes + "분 전";
            } else {
                return diffInHours + "시간 전";
            }
        } else if (diffInDays == 1) {
            return "어제";
        } else if (diffInDays < 7) {
            return diffInDays + "일 전";
        } else {
            // 일주일 이상이면 날짜 표시
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm", Locale.getDefault());
            return sdf.format(date);
        }
    }
    
    private String getFullImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return null;
        }
        
        // 이미 절대 URL인 경우
        if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
            return imageUrl;
        }
        
        // 상대 경로인 경우 siteUrl과 결합
        String baseUrl = siteUrl;
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        
        if (imageUrl.startsWith("/")) {
            return baseUrl + imageUrl;
        } else {
            return baseUrl + "/" + imageUrl;
        }
    }
    
    private void loadThumbnailImage(ImageViewHolder holder, String imageUrl) {
        ImageView imageView = holder.imageView;
        if (imageView == null) return;
        
        String fullUrl = getFullImageUrl(imageUrl);
        if (fullUrl == null) {
            Log.w(TAG, "이미지 URL이 유효하지 않습니다: " + imageUrl);
            return;
        }
        
        // 이미지가 이미 로드 중이거나 같은 URL이면 스킵
        String currentUrl = (String) imageView.getTag();
        if (currentUrl != null && currentUrl.equals(fullUrl)) {
            // 같은 URL이면 다시 로드하지 않음 (이미 로드되었거나 로딩 중)
            // 이미지가 실제로 있는지 확인
            android.graphics.drawable.Drawable drawable = imageView.getDrawable();
            if (drawable != null && drawable.getConstantState() != null) {
                // 이미지가 이미 로드되어 있음
                Log.d(TAG, "이미지 로딩 스킵 (이미 로드됨): " + fullUrl);
                return;
            }
            // 태스크가 실행 중인지 확인
            if (holder.currentTask != null && holder.currentTask.getStatus() == AsyncTask.Status.RUNNING) {
                Log.d(TAG, "이미지 로딩 스킵 (로딩 중): " + fullUrl);
                return;
            }
            // 이미지가 없고 태스크도 없으면 다시 로드
            Log.d(TAG, "이미지가 없어서 다시 로드: " + fullUrl);
        }
        
        // 동시 로딩 수 제한
        if (activeLoadCount >= MAX_CONCURRENT_LOADS) {
            Log.d(TAG, "동시 로딩 수 제한으로 인해 이미지 로딩 지연: " + fullUrl);
            // 나중에 다시 시도하도록 태그만 설정
            imageView.setTag(fullUrl);
            return;
        }
        
        // 이전 태스크 취소
        holder.cancelImageLoading();
        // 태그를 설정하여 중복 로딩 방지
        imageView.setTag(fullUrl);
        Log.d(TAG, "이미지 로딩 시작: " + fullUrl);
        activeLoadCount++;
        holder.currentTask = new LoadThumbnailTask(imageView, holder, fullUrl);
        holder.currentTask.execute(fullUrl);
    }
    
    private static class LoadThumbnailTask extends AsyncTask<String, Void, Bitmap> {
        private WeakReference<ImageViewHolder> holderRef;
        private String imageUrl;
        
        public LoadThumbnailTask(ImageView imageView, ImageViewHolder holder, String imageUrl) {
            this.holderRef = holder != null ? new WeakReference<>(holder) : null;
            this.imageUrl = imageUrl;
        }
        
        @Override
        protected Bitmap doInBackground(String... urls) {
            if (urls == null || urls.length == 0 || urls[0] == null) {
                Log.e(TAG, "이미지 URL이 null입니다");
                return null;
            }
            
            String imageUrl = urls[0];
            Log.d(TAG, "이미지 로딩 시작: " + imageUrl);
            
            try {
                URL url = new URL(imageUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setRequestMethod("GET");
                
                int responseCode = conn.getResponseCode();
                Log.d(TAG, "HTTP 응답 코드: " + responseCode);
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream inputStream = conn.getInputStream();
                    
                    // 스트림을 먼저 ByteArray로 읽기 (스트림을 두 번 읽을 수 없으므로)
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    byte[] data = new byte[8192];
                    int nRead;
                    while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                        buffer.write(data, 0, nRead);
                    }
                    buffer.flush();
                    byte[] imageBytes = buffer.toByteArray();
                    inputStream.close();
                    conn.disconnect();
                    
                    // 이미지 크기 확인
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, options);
                    
                    // 적절한 샘플 크기 계산
                    int reqWidth = 200;
                    int reqHeight = 200;
                    int inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
                    
                    // 실제 디코딩
                    options.inJustDecodeBounds = false;
                    options.inSampleSize = inSampleSize;
                    Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, options);
                    
                    if (bitmap != null) {
                        Log.d(TAG, "이미지 로딩 성공: " + bitmap.getWidth() + "x" + bitmap.getHeight());
                    } else {
                        Log.e(TAG, "비트맵 디코딩 실패");
                    }
                    return bitmap;
                } else {
                    Log.e(TAG, "HTTP 오류: " + responseCode);
                    conn.disconnect();
                    return null;
                }
            } catch (Exception e) {
                Log.e(TAG, "이미지 로딩 중 예외 발생: " + imageUrl, e);
                e.printStackTrace();
                return null;
            }
        }
        
        private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
            final int height = options.outHeight;
            final int width = options.outWidth;
            int inSampleSize = 1;
            
            if (height > reqHeight || width > reqWidth) {
                final int halfHeight = height / 2;
                final int halfWidth = width / 2;
                
                while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                    inSampleSize *= 2;
                }
            }
            
            return inSampleSize;
        }
        
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            synchronized (ImageAdapter.class) {
                activeLoadCount = Math.max(0, activeLoadCount - 1);
            }
            
            ImageViewHolder holder = holderRef != null ? holderRef.get() : null;
            
            // 태스크 완료 시 holder의 currentTask를 null로 설정
            if (holder != null && holder.currentTask == this) {
                holder.currentTask = null;
            }
            
            if (holder == null) {
                Log.d(TAG, "ViewHolder가 null입니다 (재사용됨): " + imageUrl);
                return;
            }
            
            ImageView imageView = holder.imageView;
            if (imageView == null) {
                Log.d(TAG, "ImageView가 null입니다: " + imageUrl);
                return;
            }
            
            if (bitmap == null) {
                Log.w(TAG, "비트맵이 null입니다. URL: " + imageUrl);
                return;
            }
            
            // ImageView가 여전히 같은 URL을 표시하는지 확인
            String currentUrl = (String) imageView.getTag();
            if (currentUrl != null && currentUrl.equals(imageUrl)) {
                imageView.setImageBitmap(bitmap);
                Log.d(TAG, "이미지 표시 완료: " + imageUrl + " (" + bitmap.getWidth() + "x" + bitmap.getHeight() + ")");
            } else {
                Log.d(TAG, "ImageView URL이 변경되어 이미지 표시 취소. current=" + currentUrl + ", expected=" + imageUrl);
            }
        }
        
        @Override
        protected void onCancelled() {
            synchronized (ImageAdapter.class) {
                activeLoadCount = Math.max(0, activeLoadCount - 1);
            }
            
            ImageViewHolder holder = holderRef != null ? holderRef.get() : null;
            if (holder != null && holder.currentTask == this) {
                holder.currentTask = null;
            }
            Log.d(TAG, "이미지 로딩 태스크 취소됨: " + imageUrl);
        }
        
        @Override
        protected void onCancelled(Bitmap bitmap) {
            onCancelled();
        }
    }
    
    @Override
    public int getItemCount() {
        return postList.size();
    }
    
    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        TextView statusIcon;
        ImageView imageView;
        TextView statusText;
        TextView timeText;
        TextView textPreview;
        TextView imageIndicator;
        LoadThumbnailTask currentTask; // 현재 실행 중인 이미지 로딩 태스크
        
        public ImageViewHolder(View itemView) {
            super(itemView);
            statusIcon = itemView.findViewById(R.id.statusIcon);
            imageView = itemView.findViewById(R.id.imageViewItem);
            statusText = itemView.findViewById(R.id.statusText);
            timeText = itemView.findViewById(R.id.timeText);
            textPreview = itemView.findViewById(R.id.textPreview);
            imageIndicator = itemView.findViewById(R.id.imageIndicator);
        }
        
        public void cancelImageLoading() {
            if (currentTask != null && currentTask.getStatus() != AsyncTask.Status.FINISHED) {
                currentTask.cancel(true);
                currentTask = null;
            }
        }
    }
}
