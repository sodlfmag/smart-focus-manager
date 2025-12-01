package com.example.helloandroid;

import android.graphics.Bitmap;
import java.util.Date;

public class PostData {
    private Bitmap bitmap;
    private String title;
    private String text;
    private String imageUrl;
    private Date createdDate;
    
    public PostData(Bitmap bitmap, String title, String text, String imageUrl) {
        this(bitmap, title, text, imageUrl, null);
    }
    
    public PostData(Bitmap bitmap, String title, String text, String imageUrl, Date createdDate) {
        this.bitmap = bitmap;
        this.title = title;
        this.text = text;
        this.imageUrl = imageUrl;
        this.createdDate = createdDate;
    }
    
    public Bitmap getBitmap() {
        return bitmap;
    }
    
    public String getTitle() {
        return title;
    }
    
    public String getText() {
        return text;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public Date getCreatedDate() {
        return createdDate;
    }
    
    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }
}


