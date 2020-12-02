package com.example.lapitchat;

public class Friends {

    public String date;

    public Friends(String date) {
    }

    public Friends(String name, String date) {
        this.date = date;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}