package com.example.helloandroid;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {
    private List<PostData> postList;
    
    public ImageAdapter(List<PostData> postList) {
        // 생성자에서 포스트 목록 입력
        this.postList = postList;
    }
    
    @Override
    public ImageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // 이미지 항목을 나타낼 뷰 생성
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image, parent, false);
        return new ImageViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(ImageViewHolder holder, int position) {
        // 해당 위치의 이미지를 뷰에 설정
        PostData post = postList.get(position);
        holder.imageView.setImageBitmap(post.getBitmap());
        
        // 상태별 색상 구분
        String title = post.getTitle();
        if (title != null) {
            if ("Focus".equals(title)) {
                // Focus: 초록색 테두리
                holder.itemView.setBackgroundColor(Color.parseColor("#E8F5E9")); // 연한 초록색
                holder.itemView.setPadding(8, 8, 8, 8);
                if (holder.statusText != null) {
                    holder.statusText.setText("집중");
                    holder.statusText.setTextColor(Color.parseColor("#388E3C")); // 진한 초록색
                }
            } else if ("Distracted".equals(title)) {
                // Distracted: 빨간색 카드 + 썸네일 강조
                holder.itemView.setBackgroundColor(Color.parseColor("#FFEBEE")); // 연한 빨간색
                holder.itemView.setPadding(12, 12, 12, 12);
                if (holder.statusText != null) {
                    holder.statusText.setText("딴짓");
                    holder.statusText.setTextColor(Color.parseColor("#D32F2F")); // 진한 빨간색
                }
            } else if ("Away".equals(title)) {
                // Away: 회색
                holder.itemView.setBackgroundColor(Color.parseColor("#F5F5F5")); // 회색
                holder.itemView.setPadding(8, 8, 8, 8);
                if (holder.statusText != null) {
                    holder.statusText.setText("부재");
                    holder.statusText.setTextColor(Color.parseColor("#757575")); // 회색
                }
            } else {
                // 기본 색상
                holder.itemView.setBackgroundColor(Color.WHITE);
                holder.itemView.setPadding(8, 8, 8, 8);
                if (holder.statusText != null) {
                    holder.statusText.setText(title);
                    holder.statusText.setTextColor(Color.BLACK);
                }
            }
        }
        
        // 클릭 리스너 추가
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), ImageDetailActivity.class);
                // Bitmap 대신 URL과 메타데이터만 전달 (크기 제한 회피)
                intent.putExtra("imageUrl", post.getImageUrl());
                intent.putExtra("title", post.getTitle());
                intent.putExtra("text", post.getText());
                // Bitmap은 static 변수로 임시 저장 (메모리 상에 있으므로 재다운로드 불필요)
                ImageDetailActivity.setTempBitmap(post.getBitmap());
                v.getContext().startActivity(intent);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return postList.size();
    }
    
    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView statusText;
        
        public ImageViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageViewItem); // item_image.xml에 있는 ImageView
            statusText = itemView.findViewById(R.id.statusText); // 상태 텍스트 (있으면)
        }
    }
}

