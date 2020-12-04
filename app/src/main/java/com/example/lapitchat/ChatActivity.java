package com.example.lapitchat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {

    private String mChatUser;
    private Toolbar mChatToolbar;

    private DatabaseReference mRoofRef;

    private TextView mTitleView;
    private TextView mLastSeenView;
    private CircleImageView mProfileImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mRoofRef = FirebaseDatabase.getInstance().getReference();

        mChatUser = getIntent().getStringExtra("user_id");
        String chat_user_name = getIntent().getStringExtra("user_name");

        mChatToolbar = findViewById(R.id.chat_app_bar);
        setSupportActionBar(mChatToolbar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);

        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View action_bar_view = inflater.inflate(R.layout.chat_custom_bar, null);

        actionBar.setCustomView(action_bar_view);

        // -------------Custom Action bar items-----------------
        mTitleView = findViewById(R.id.custom_name);
        mLastSeenView = findViewById(R.id.custom_seen);
        mProfileImage = findViewById(R.id.custom_bar_image);

        mTitleView.setText(chat_user_name);

        mRoofRef.child("Users").child(mChatUser).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                String online = snapshot.child("online").getValue().toString();
                String image = snapshot.child("image").getValue().toString();

                if (online.equals("true")) {
                    mLastSeenView.setText("online");
                } else {
                    mLastSeenView.setText(online);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }
}