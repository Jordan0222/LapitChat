package com.example.lapitchat;

import android.app.Application;

import com.google.firebase.database.FirebaseDatabase;

public class LapiChat extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }
}
