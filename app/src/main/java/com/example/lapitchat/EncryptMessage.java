package com.example.lapitchat;

public class EncryptMessage {

    private String encryptMessage, type;
    private long time;
    private boolean seen;
    private String from;

    public EncryptMessage() {
    }

    public EncryptMessage(String from) {
        this.from = from;
    }

    public EncryptMessage(String encryptMessage, String type, long time, boolean seen) {
        this.encryptMessage = encryptMessage;
        this.type = type;
        this.time = time;
        this.seen = seen;
    }

    public String getEncryptMessage() {
        return encryptMessage;
    }

    public void setEncryptMessage(String encryptMessage) {
        this.encryptMessage = encryptMessage;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public boolean isSeen() {
        return seen;
    }

    public void setSeen(boolean seen) {
        this.seen = seen;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }
}
