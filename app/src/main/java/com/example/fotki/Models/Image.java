package com.example.fotki.Models;

public class Image {
    private String imageName;
    private String imageUrl;
    private String username;
    private String email;

    public Image() {

    }

    public Image(String imageName, String imageUrl, String username, String email) {
        this.imageName = imageName;
        this.imageUrl = imageUrl;
        this.username = username;
        this.email = email;
    }

    public String getImageUrl() {
        return imageUrl;
    }
    public String getUsername() {
        return username;
    }
    public String getEmail() {
        return email;
    }
    public String getImageName() {
        return imageUrl;
    }
}

