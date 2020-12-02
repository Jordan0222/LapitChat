package com.example.lapitchat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {

    private ImageView mProfileImage;
    private TextView mProfileName, mProfileStatus, mProfileFriendsCount;
    private Button mProfileSendReqBtn, mDeclineBtn;

    private DatabaseReference mUserDatabase;
    private DatabaseReference mFriendReqDatabase;
    private DatabaseReference mFriendDatabase;
    private DatabaseReference mRoofRef;
    private DatabaseReference mUserRef;

    private FirebaseUser mCurrent_user;

    private ProgressDialog mProgressDialog;

    private String mCurrent_state;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        String user_id = getIntent().getStringExtra("user_id");

        mRoofRef = FirebaseDatabase.getInstance().getReference();

        // FirebaseDatabase
        mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(user_id);
        mFriendReqDatabase = FirebaseDatabase.getInstance().getReference().child("Friend_req");
        mFriendDatabase = FirebaseDatabase.getInstance().getReference().child("Friends");

        mCurrent_user = FirebaseAuth.getInstance().getCurrentUser();
        mUserRef = FirebaseDatabase.getInstance().getReference().child("Users").child(mCurrent_user.getUid());

        mProfileName = (TextView) findViewById(R.id.profile_displayname);
        mProfileFriendsCount = (TextView) findViewById(R.id.profile_friends);
        mProfileStatus = (TextView) findViewById(R.id.profile_status);
        mProfileImage = (CircleImageView) findViewById(R.id.profile_image);

        // Button
        mProfileSendReqBtn = (Button) findViewById(R.id.profile_req_btn);
        mDeclineBtn = (Button) findViewById(R.id.profile_dec_btn);

        mCurrent_state = "not_friend";

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle("Loading User");
        mProgressDialog.setMessage("Please wait while we load the user data");
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.show();

        mUserDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String display_name = snapshot.child("name").getValue().toString();
                String status = snapshot.child("status").getValue().toString();
                String image = snapshot.child("image").getValue().toString();

                mProfileName.setText(display_name);
                mProfileStatus.setText(status);

                // mProfileImage.setScaleType(ImageView.ScaleType.FIT_XY);

                Picasso.get().load(image).into(mProfileImage);

                mDeclineBtn.setVisibility(View.INVISIBLE);
                mDeclineBtn.setEnabled(false);

                // -----------------------------FRIENDS LIST / REQUEST FEATURE-----------------------------------
                mFriendReqDatabase.child(mCurrent_user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.hasChild(user_id)) {
                            String req_type = snapshot.child(user_id).child("request_type").getValue().toString();

                            if (req_type.equals("received")) {
                                mCurrent_state = "req_received";
                                mProfileSendReqBtn.setText("Accept Friend Request");

                                mDeclineBtn.setVisibility(View.VISIBLE);
                                mDeclineBtn.setEnabled(true);
                            } else if (req_type.equals("sent")) {
                                mCurrent_state = "req_sent";
                                mProfileSendReqBtn.setText("Cancel Friend Request");

                                mDeclineBtn.setVisibility(View.INVISIBLE);
                                mDeclineBtn.setEnabled(false);
                            }
                        } else {
                            mFriendDatabase.child(mCurrent_user.getUid())
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    if (snapshot.hasChild(user_id)) {
                                        mCurrent_state = "friends";
                                        mProfileSendReqBtn.setText("Unfriend the Person");

                                        mDeclineBtn.setVisibility(View.INVISIBLE);
                                        mDeclineBtn.setEnabled(false);
                                    }

                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });
                        }

                        mProgressDialog.dismiss();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        mProgressDialog.dismiss();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                mProgressDialog.dismiss();
            }
        });

        mProfileSendReqBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mProfileSendReqBtn.setEnabled(false);
                // -----------------------------Not Friend State-----------------------------------
                if (mCurrent_state.equals("not_friend")) {

                    Map requestMap = new HashMap();
                    requestMap.put("Friend_req/" + mCurrent_user.getUid() + "/" + user_id + "/" + "request_type", "sent");
                    requestMap.put("Friend_req/" + user_id + "/" + mCurrent_user.getUid() + "/" + "request_type", "received");

                    mRoofRef.updateChildren(requestMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                            if (error != null) {
                                Toast.makeText(ProfileActivity.this, "Three was some error in sending request", Toast.LENGTH_SHORT).show();
                            }
                            mProfileSendReqBtn.setEnabled(true);

                            mCurrent_state = "req_sent";
                            mProfileSendReqBtn.setText("Cancel Friend Request");

                            //mDeclineBtn.setVisibility(View.INVISIBLE);
                            //mDeclineBtn.setEnabled(false);
                        }
                    });
                }

                // -----------------------------Cancel Request State-----------------------------------

                if (mCurrent_state.equals("req_sent")) {
                    mFriendReqDatabase.child(mCurrent_user.getUid()).child(user_id).removeValue()
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            mFriendReqDatabase.child(user_id).child(mCurrent_user.getUid()).removeValue()
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            mProfileSendReqBtn.setEnabled(true);
                                            mCurrent_state = "not_friend";
                                            mProfileSendReqBtn.setText("Send Friend Request");

                                            mDeclineBtn.setVisibility(View.INVISIBLE);
                                            mDeclineBtn.setEnabled(false);
                                        }
                                    });
                        }
                    });
                }

                // -----------------------------REQ RECEIVED STATE-----------------------------------

                if (mCurrent_state.equals("req_received")) {

                    final String currentDate = DateFormat.getDateTimeInstance().format(new Date());

                    Map friendsMap = new HashMap();
                    friendsMap.put("Friends/" + mCurrent_user.getUid() + "/" + user_id + "/date", currentDate);
                    friendsMap.put("Friends/" + user_id + "/" + mCurrent_user.getUid() + "/date", currentDate);

                    friendsMap.put("Friend_req/" + mCurrent_user.getUid() + "/" + user_id, null);
                    friendsMap.put("Friend_req/" + user_id + "/" + mCurrent_user.getUid(), null);

                    mRoofRef.updateChildren(friendsMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {

                            if (error == null) {
                                mCurrent_state = "friends";
                                mProfileSendReqBtn.setText("Unfriend the Person");

                                mDeclineBtn.setVisibility(View.INVISIBLE);
                                mDeclineBtn.setEnabled(false);
                            } else {
                                String databaseError = error.getMessage();
                                Toast.makeText(ProfileActivity.this, databaseError, Toast.LENGTH_SHORT).show();
                            }
                            mProfileSendReqBtn.setEnabled(true);
                        }
                    });
                }
                // -----------------------------UNFRIEND-----------------------------------

                if (mCurrent_state.equals("friends")) {

                    Map unfriendMap = new HashMap();
                    unfriendMap.put("Friends/" + mCurrent_user.getUid() + "/" + user_id, null);
                    unfriendMap.put("Friends/" + user_id + "/" + mCurrent_user.getUid(), null);

                    mRoofRef.updateChildren(unfriendMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {

                            if (error == null) {
                                mCurrent_state = "not_friend";
                                mProfileSendReqBtn.setText("Send Friend Request");

                                mDeclineBtn.setVisibility(View.INVISIBLE);
                                mDeclineBtn.setEnabled(false);
                            } else {
                                String databaseError = error.getMessage();
                                Toast.makeText(ProfileActivity.this, databaseError, Toast.LENGTH_SHORT).show();
                            }
                            mProfileSendReqBtn.setEnabled(true);
                        }
                    });
                }
            }
        });

        mDeclineBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Map DeclineMap = new HashMap();
                DeclineMap.put("Friend_req/" + mCurrent_user.getUid() + "/" + user_id, null);
                DeclineMap.put("Friend_req/" + user_id + "/" + mCurrent_user.getUid(), null);

                mRoofRef.updateChildren(DeclineMap, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {

                        if (error == null) {
                            mCurrent_state = "not_friend";
                            mProfileSendReqBtn.setText("Send Friend Request");

                            mDeclineBtn.setVisibility(View.INVISIBLE);
                            mDeclineBtn.setEnabled(false);
                        } else {
                            String databaseError = error.getMessage();
                            Toast.makeText(ProfileActivity.this, databaseError, Toast.LENGTH_SHORT).show();
                        }

                    }
                });
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        mUserRef.child("online").setValue(false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mUserRef.child("online").setValue(true);
    }
}