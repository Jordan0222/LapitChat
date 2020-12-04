package com.example.lapitchat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;

import java.util.Random;

import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsActivity extends AppCompatActivity {

    private static final int MAX_LENGTH = 10;

    private DatabaseReference mUserDatabase;
    private FirebaseUser mCurrentUser;
    private DatabaseReference mUserRef;

    private CircleImageView mDisplayImage;
    private TextView mName;
    private TextView mStatus;

    private Button mStatusBtn;
    private Button mImageBtn;

    private static final int GALLERY_PICK = 1;

    private StorageReference mImageStorage;

    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        mUserRef = FirebaseDatabase.getInstance().getReference().child("Users").child(mCurrentUser.getUid());

        mImageStorage = FirebaseStorage.getInstance().getReference();

        mDisplayImage = (CircleImageView) findViewById(R.id.custom_bar_image);
        mName = (TextView) findViewById(R.id.setting_display_name);
        mStatus = (TextView) findViewById(R.id.setting_status);

        mStatusBtn = (Button) findViewById(R.id.setting_stat_button);
        mImageBtn = (Button) findViewById(R.id.setting_img_button);

        String current_uid = mCurrentUser.getUid();

        mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(current_uid);
        mUserDatabase.keepSynced(true);

        mUserDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                String name = snapshot.child("name").getValue().toString();
                String image = snapshot.child("image").getValue().toString();
                String status = snapshot.child("status").getValue().toString();
                String thumb_image = snapshot.child("thumb_image").getValue().toString();

                mName.setText(name);
                mStatus.setText(status);

                if (!image.equals("default")) {
                    // Picasso.get().load(image).into(mDisplayImage);
                    Picasso.get().load(image).networkPolicy(NetworkPolicy.OFFLINE).into(mDisplayImage, new Callback() {
                        @Override
                        public void onSuccess() {

                        }

                        @Override
                        public void onError(Exception e) {
                            // if failure load image online
                            Picasso.get().load(image).into(mDisplayImage);
                        }
                    });
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        mStatusBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String status_value = mStatus.getText().toString();
                Intent status_intent = new Intent(SettingsActivity.this, StatusActivity.class);
                status_intent.putExtra("status_value", status_value);
                startActivity(status_intent);
            }
        });

        mImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent galleryIntent = new Intent();
                galleryIntent.setType("image/*");
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);

                startActivityForResult(Intent.createChooser(galleryIntent, "SELECT IMAGE"), GALLERY_PICK);

                // start picker to get image for cropping and then use the image in cropping activity
                /*CropImage.activity()
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .start(SettingsActivity.this);*/
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_PICK && resultCode == RESULT_OK) {

            Uri imageUri = data.getData();
            // start cropping activity for pre-acquired image saved on the device
            CropImage.activity(imageUri)
                    .setAspectRatio(1, 1)
                    .start(this);

            /*String imageUri = data.getDataString();
            Toast.makeText(SettingsActivity.this, imageUri, Toast.LENGTH_SHORT).show();*/
        }

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            if (resultCode == RESULT_OK) {

                mProgressDialog = new ProgressDialog(SettingsActivity.this);
                mProgressDialog.setTitle("Uploading Image...");
                mProgressDialog.setMessage("Please wait we upload your image");
                mProgressDialog.setCanceledOnTouchOutside(false);
                mProgressDialog.show();

                Uri resultUri = result.getUri();
                String current_user_uid = mCurrentUser.getUid();


                StorageReference filepath = mImageStorage.child("profile_images").child(current_user_uid + ".jpg");

                filepath.putFile(resultUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if (task.isSuccessful()) {
                            filepath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    String download_url = uri.toString();
                                    mUserDatabase.child("image").setValue(download_url).addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                mProgressDialog.dismiss();
                                                Toast.makeText(SettingsActivity.this, "Success Uploading", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                                }
                            });
                        }

                    }
                });
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
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

    public static String random() {
        Random generator = new Random();
        StringBuilder randomStringBuilder = new StringBuilder();
        int randomLength = generator.nextInt(MAX_LENGTH);
        char tempChar;
        for (int i = 0; i < MAX_LENGTH; i++) {
            tempChar = (char) (generator.nextInt(96) + 32);
            randomStringBuilder.append(tempChar);
        }
        return randomStringBuilder.toString();
    }
}