package com.example.chat;

public class Message {
    private String content;
    private String name;
    private String imgUrl;

    public Message () {}

    public Message (String content, String name, String imgUrl) {
        this.content = content;
        this.name = name;
        this.imgUrl = imgUrl;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }
}
