package com.example.lapitchat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import de.hdodenhof.circleimageview.CircleImageView;

public class OneTimePadActivity extends AppCompatActivity {

    private static final String TAG = OneTimePadActivity.class.getSimpleName();
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
    private OneTimePadActivity.MessageAdapter mAdapter;

    private static final int TOTAL_ITEMS_TO_LOAD = 10;
    private int mCurrentPage = 1;

    private int itemPos = 0;
    private String mLastKey = "";
    private String mPrevKey = "";


    private static String keyOTP = null;

    private static final int PERMISSION_REQUEST_STORAGE = 1000;
    private static final int READ_REQUEST_CODE = 42;

    private static final String ALGORITHM_MD5 = "MD5";

    private static int dataLength = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_one_time_pad);

        // 取得使用者允許
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission
                (Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_STORAGE);
        }

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

        keyOTP = getSharedPreferences("OTP", MODE_PRIVATE)
                .getString("keyOTP", null);

        mAdapter = new OneTimePadActivity.MessageAdapter(messageList);
        mLinearLayout = new LinearLayoutManager(this);
        mMessageList.setHasFixedSize(true);
        mMessageList.setLayoutManager(mLinearLayout);
        mMessageList.setAdapter(mAdapter);

        if (keyOTP == null) {
        } else {
            mMessageList.setAdapter(mAdapter);
        }

        if (keyOTP == null) {
            new AlertDialog.Builder(OneTimePadActivity.this)
                    .setTitle("Import Key")
                    .setMessage("Do you want to import your key?")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            performFileSearch();
                            keyOTP = getSharedPreferences("OTP", MODE_PRIVATE)
                                    .getString("keyOTP", null);
                        }
                    })
                    .setNeutralButton("Cancel", null)
                    .show();
        } else {
            loadMessages();
        }

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

        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mCurrentPage++;
                // messageList.clear();
                // loadMessages();
                itemPos = 0;
                if (keyOTP == null) {
                    Toast.makeText(OneTimePadActivity.this, "You don't import your key", Toast.LENGTH_SHORT).show();
                } else {
                    loadMoreMessages();
                }
            }
        });
    }

    // read content of the file
    private String readText(String input) {
        // File file = Environment.getExternalStorageDirectory();
        File txtFile = new File("/storage/emulated/0/" + input);
        StringBuilder text = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new FileReader(txtFile));
            String line = br.readLine();
            while (line != null) {
                Log.d(TAG, "readText: " + line);
                text.append(line);
                text.append("\n");
                line = br.readLine();
            }
            br.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return text.toString();
    }

    // select file from storage
    private void performFileSearch() {
        Intent searchIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        searchIntent.addCategory(Intent.CATEGORY_OPENABLE);
        searchIntent.setType("text/*");
        startActivityForResult(searchIntent, READ_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                String path = uri.getPath();
                path = path.substring(path.indexOf(":") + 1);
                Toast.makeText(this, "" + path, Toast.LENGTH_LONG).show();
                String binaryKey = readText(path);

                try {
                    final String keyOTP = MD5_32bit(binaryKey);

                    getSharedPreferences("OTP", MODE_PRIVATE)
                            .edit()
                            .putString("keyOTP", keyOTP)
                            .commit();

                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission Granted!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Permission not Granted!", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadMoreMessages() {

        DatabaseReference encryptMessageRef = mEncryptRoofRef.child("OTPMessages")
                .child(mCurrentUserId).child(mChatUser);
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

        DatabaseReference encryptMessageRef = mEncryptRoofRef.child("OTPMessages")
                .child(mCurrentUserId).child(mChatUser);
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

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void sendMessage() {
        String message = mEncryptChatMessageView.getText().toString();
        dataLength = message.length();

        byte[] encryptByte = encrypt(message.getBytes(StandardCharsets.UTF_8),
                keyOTP.getBytes(StandardCharsets.UTF_8));

        String encryptMessage = Base64.encodeToString(encryptByte, Base64.DEFAULT);

        if (!TextUtils.isEmpty(message)) {
            String current_user_ref = "OTPMessages" + "/" + mCurrentUserId + "/" + mChatUser;
            String chat_user_ref =  "OTPMessages" + "/" + mChatUser + "/" + mCurrentUserId;

            DatabaseReference user_message_push = mEncryptRoofRef.child("OTPMessages")
                    .child(mCurrentUserId).child(mChatUser).push();

            String push_id = user_message_push.getKey();

            Map messageMap = new HashMap();
            messageMap.put("encryptMessage", encryptMessage);
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
        mEncryptChatAddBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performFileSearch();
                keyOTP = getSharedPreferences("OTP", MODE_PRIVATE)
                        .getString("keyOTP", null);
            }
        });

        mEncryptChatSendBtn = findViewById(R.id.encryptChat_send_btn);
        mEncryptChatSendBtn.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onClick(View v) {
                if (keyOTP == null) {
                    Toast.makeText(OneTimePadActivity.this,
                            "You don't import your key", Toast.LENGTH_SHORT).show();
                } else {
                    sendMessage();
                }
            }
        });

        mEncryptChatMessageView = findViewById(R.id.encryptChat_message_view);
        mMessageList = findViewById(R.id.encryptChat_message_list);
        mRefreshLayout = findViewById(R.id.encryptChat_swipe_layout);

    }

    public class MessageAdapter extends
            RecyclerView.Adapter<OneTimePadActivity.MessageAdapter.MessageViewHolder> {

        private List<EncryptMessage> MessageList;
        private FirebaseAuth firebaseAuth;

        public MessageAdapter(List<EncryptMessage> MessageList) {
            super();
            this.MessageList = MessageList;
        }

        @NonNull
        @Override
        public OneTimePadActivity.MessageAdapter.MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.message_single_layout, parent, false);

            return new OneTimePadActivity.MessageAdapter.MessageViewHolder(view);
        }

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public void onBindViewHolder(@NonNull OneTimePadActivity.MessageAdapter.MessageViewHolder holder, int position) {

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
            String encryptMessage = c.getEncryptMessage();

            byte[] TextByte = encrypt(
                    Base64.decode(encryptMessage.getBytes(StandardCharsets.UTF_8), Base64.DEFAULT),
                    keyOTP.getBytes(StandardCharsets.UTF_8));

            if (TextByte != null) {
                String decryptMessage = new String(TextByte, StandardCharsets.UTF_8);
                holder.messageText.setText(decryptMessage);
            }
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

    public static final String MD5_16bit(String readyEncryptStr) throws NoSuchAlgorithmException {
        if (readyEncryptStr != null) {
            return MD5_32bit(readyEncryptStr).substring(8, 24);
        } else {
            return null;
        }
    }

    public static final String MD5_32bit(String readyEncryptStr) throws NoSuchAlgorithmException {
        if (readyEncryptStr != null) {
            // Get MD5 digest algorithm's MessageDigest's instance.
            MessageDigest md = MessageDigest.getInstance(ALGORITHM_MD5);
            // Use specified byte update digest.
            md.update(readyEncryptStr.getBytes());
            // Get cipher text
            byte[] b = md.digest();
            // The cipher text converted to hexadecimal string
            StringBuilder su = new StringBuilder();
            // byte array switch hexadecimal number
            for (int offset = 0, bLen = b.length; offset < bLen; offset++) {
                String haxHex = Integer.toHexString(b[offset] & 0xFF);
                if (haxHex.length() < 2) {
                    su.append("0");
                }
                su.append(haxHex);
            }
            return su.toString();
        } else {
            return null;
        }
    }

    public static byte[] encrypt(byte[] data, byte[] key) {
        if (data == null || data.length == 0 || key == null || key.length == 0) {
            return data;
        }

        byte[] result = new byte[data.length];

        for (int i = 0; i < data.length; i++) {
            result[i] = (byte) (data[i] ^ key[i % key.length] ^ (i & 0xFF));
        }

        return result;
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