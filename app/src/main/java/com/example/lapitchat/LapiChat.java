package com.example.lapitchat;

import android.app.Application;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;

public class LapiChat extends Application {

    private DatabaseReference mUserDatabase;
    private FirebaseAuth mAuth;
    private FirebaseUser mCurrent_user;

    @Override
    public void onCreate() {
        super.onCreate();

        FirebaseDatabase.getInstance().setPersistenceEnabled(true);

        // Picasso

        Picasso.Builder builder = new Picasso.Builder(this);
        builder.downloader(new OkHttp3Downloader(this, Integer.MAX_VALUE));
        Picasso built = builder.build();
        built.setIndicatorsEnabled(true);
        built.setLoggingEnabled(true);
        Picasso.setSingletonInstance(built);

        mAuth = FirebaseAuth.getInstance();
        mCurrent_user = mAuth.getCurrentUser();
        if (mCurrent_user != null) {
            mUserDatabase = FirebaseDatabase.getInstance()
                    .getReference().child("Users").child(mAuth.getCurrentUser().getUid());

            mUserDatabase.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot != null) {
                        mUserDatabase.child("online").onDisconnect().setValue(false);
                        mUserDatabase.child("lastSeen").setValue(ServerValue.TIMESTAMP);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }
    }
}
