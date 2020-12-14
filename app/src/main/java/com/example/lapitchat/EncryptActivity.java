package com.example.lapitchat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.rengwuxian.materialedittext.MaterialEditText;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class EncryptActivity extends AppCompatActivity {

    private static final String TAG = EncryptActivity.class.getSimpleName();
    private TextView mTitleView, mLastSeenView;
    private CircleImageView mEncryptChatImage;
    private ImageButton mEncryptChatAddBtn, mEncryptChatSendBtn;
    private MaterialEditText mEncryptChatMessageView;
    private RecyclerView mMessageList;
    private SwipeRefreshLayout mRefreshLayout;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseUser mCurrent_user;
    private DatabaseReference mUserRef, mEncryptRoofRef;

    private String mChatUser, mCurrentUserId;

    private Toolbar mEncryptChatToolbar;

    private final List<EncryptMessage> messageList = new ArrayList<>();
    private LinearLayoutManager mLinearLayout;
    private EncryptActivity.MessageAdapter mAdapter;

    private static final int TOTAL_ITEMS_TO_LOAD = 10;
    private int mCurrentPage = 1;

    private int itemPos = 0;
    private String mLastKey = "";
    private String mPrevKey = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_encrypt);

        mAuth = FirebaseAuth.getInstance();
        mCurrent_user = mAuth.getCurrentUser();

        if (mCurrent_user != null) {
            mUserRef = FirebaseDatabase.getInstance().getReference().child("Users").child(mCurrent_user.getUid());
        }

        mCurrentUserId = mCurrent_user.getUid();

        mEncryptRoofRef = FirebaseDatabase.getInstance().getReference();

        mChatUser = getIntent().getStringExtra("user_id");
        String encryptChat_user_name = getIntent().getStringExtra("user_name");
        String encryptChat_user_image = getIntent().getStringExtra("user_image");

        mEncryptChatToolbar = findViewById(R.id.encryptChat_app_bar);
        setSupportActionBar(mEncryptChatToolbar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);

        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View action_bar_view = inflater.inflate(R.layout.chat_custom_bar, null);

        actionBar.setCustomView(action_bar_view);

        findViews();

        mAdapter = new MessageAdapter(messageList);

        mLinearLayout = new LinearLayoutManager(this);

        mMessageList.setHasFixedSize(true);
        mMessageList.setLayoutManager(mLinearLayout);
        mMessageList.setAdapter(mAdapter);

        loadMessages();

        // -------------Custom Action bar items-----------------

        mTitleView.setText(encryptChat_user_name);
        Picasso.get().load(encryptChat_user_image).into(mEncryptChatImage);

        mEncryptRoofRef.child("Users").child(mChatUser).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                String online = snapshot.child("online").getValue().toString();

                if (online.equals("true")) {
                    mLastSeenView.setText("online");
                } else {

                    GetTimeAgo getTimeAgo = new GetTimeAgo();
                    long lastTime = Long.parseLong(online);
                    String lastSeenTime = getTimeAgo.getTimeAgo(lastTime, getApplicationContext());
                    mLastSeenView.setText(lastSeenTime);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        mEncryptRoofRef.child("Chat").child(mCurrentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (!snapshot.hasChild(mChatUser)) {

                    Map chatAddMap = new HashMap();
                    chatAddMap.put("seen", false);
                    chatAddMap.put("timestamp", ServerValue.TIMESTAMP);

                    Map chatUserMap = new HashMap();
                    chatUserMap.put("Chat" + "/" + mCurrentUserId + "/" + mChatUser, chatAddMap);
                    chatUserMap.put("Chat" + "/" + mChatUser + "/" + mCurrentUserId, chatAddMap);

                    mEncryptRoofRef.updateChildren(chatUserMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                            if (error != null) {
                                Log.d("Chat_LOG", "onComplete: " + error.getMessage().toString());
                            }
                        }
                    });
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        mEncryptChatSendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });

        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mCurrentPage++;
                // messageList.clear();
                // loadMessages();
                itemPos = 0;
                loadMoreMessages();
            }
        });
    }

    private void loadMoreMessages() {

        DatabaseReference encryptMessageRef = mEncryptRoofRef.child("encryptMessages").child(mCurrentUserId).child(mChatUser);
        Query messageQuery = encryptMessageRef.orderByKey().endAt(mLastKey).limitToLast(10);

        messageQuery.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                EncryptMessage encryptMessage = snapshot.getValue(EncryptMessage.class);
                String messageKey = snapshot.getKey();
                // messageList.add(itemPos++, message);

                if (!mPrevKey.equals(messageKey)) {
                    messageList.add(itemPos++, encryptMessage);
                } else {
                    mPrevKey = mLastKey;
                }

                if (itemPos == 1) {
                    mLastKey = messageKey;
                }

                mAdapter.notifyDataSetChanged();

                mRefreshLayout.setRefreshing(false);

                mLinearLayout.scrollToPositionWithOffset(10, 0);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void loadMessages() {

        DatabaseReference encryptMessageRef = mEncryptRoofRef.child("encryptMessages").child(mCurrentUserId).child(mChatUser);
        Query messageQuery = encryptMessageRef.limitToLast(mCurrentPage * TOTAL_ITEMS_TO_LOAD);

        messageQuery.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                EncryptMessage encryptMessage = snapshot.getValue(EncryptMessage.class);

                itemPos++;

                if (itemPos == 1) {
                    String messageKey = snapshot.getKey();
                    mLastKey = messageKey;
                    mPrevKey = mLastKey;
                }

                messageList.add(encryptMessage);
                mAdapter.notifyDataSetChanged();

                mMessageList.scrollToPosition(messageList.size() -1);

                mRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void sendMessage() {
        String message = mEncryptChatMessageView.getText().toString();

        if (!TextUtils.isEmpty(message)) {
            String current_user_ref = "encryptMessages" + "/" + mCurrentUserId + "/" + mChatUser;
            String chat_user_ref =  "encryptMessages" + "/" + mChatUser + "/" + mCurrentUserId;

            DatabaseReference user_message_push = mEncryptRoofRef.child("encryptMessages")
                    .child(mCurrentUserId).child(mChatUser).push();

            String push_id = user_message_push.getKey();

            Map messageMap = new HashMap();
            messageMap.put("encryptMessage", message);
            messageMap.put("seen", false);
            messageMap.put("type", "text");
            messageMap.put("time", ServerValue.TIMESTAMP);
            messageMap.put("from", mCurrentUserId);

            Map messageUserMap = new HashMap();
            messageUserMap.put(current_user_ref + "/" + push_id, messageMap);
            messageUserMap.put(chat_user_ref + "/" + push_id, messageMap);

            mEncryptChatMessageView.setText("");

            mEncryptRoofRef.updateChildren(messageUserMap, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                    if (error != null) {
                        Log.d("Chat_LOG", "onComplete: " + error.getMessage().toString());
                    }
                }
            });
        }
    }

    private void findViews() {

        mTitleView = findViewById(R.id.custom_name);
        mLastSeenView = findViewById(R.id.custom_seen);
        mEncryptChatImage = findViewById(R.id.chat_custom_image_layout);

        mEncryptChatAddBtn = findViewById(R.id.encryptChat_add_btn);
        mEncryptChatSendBtn = findViewById(R.id.encryptChat_send_btn);
        mEncryptChatMessageView = findViewById(R.id.encryptChat_message_view);

        mMessageList = findViewById(R.id.encryptChat_message_list);

        mRefreshLayout = findViewById(R.id.encryptChat_swipe_layout);

    }

    public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

        private List<EncryptMessage> MessageList;
        private FirebaseAuth firebaseAuth;

        public MessageAdapter(List<EncryptMessage> MessageList) {
            super();
            this.MessageList = MessageList;
        }

        @NonNull
        @Override
        public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.message_single_layout, parent, false);

            return new EncryptActivity.MessageAdapter.MessageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull EncryptActivity.MessageAdapter.MessageViewHolder holder, int position) {

            firebaseAuth = FirebaseAuth.getInstance();
            String current_user_id = mAuth.getCurrentUser().getUid();

            EncryptMessage c = MessageList.get(position);
            String from_user = c.getFrom();

            if (from_user != null) {
                if (from_user.equals(current_user_id)) {
                    holder.messageText.setBackgroundColor(Color.WHITE);
                    holder.messageText.setTextColor(Color.BLACK);
                } else {
                    holder.messageText.setBackgroundResource(R.drawable.message_text_background);
                    holder.messageText.setTextColor(Color.WHITE);
                }
            }
            String message = c.getEncryptMessage();
            holder.messageText.setText(c.getEncryptMessage());
        }

        @Override
        public int getItemCount() {
            return MessageList.size();
        }

        public class MessageViewHolder extends RecyclerView.ViewHolder {

            public TextView messageText;
            public CircleImageView mProfileImage;

            public MessageViewHolder(@NonNull View itemView) {
                super(itemView);

                messageText = itemView.findViewById(R.id.message_text_layout);
                mProfileImage = itemView.findViewById(R.id.chat_custom_image_layout);
            }
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        mUserRef.child("online").setValue(ServerValue.TIMESTAMP);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mUserRef.child("online").setValue(true);
    }
}