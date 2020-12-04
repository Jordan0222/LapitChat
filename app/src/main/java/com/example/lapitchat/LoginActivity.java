package com.example.lapitchat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.rengwuxian.materialedittext.MaterialEditText;

public class LoginActivity extends AppCompatActivity {

    private Toolbar mToolbar;

    private MaterialEditText mLoginEmail;
    private MaterialEditText mLoginPassword;

    private Button mLogin_btn;

    private ProgressDialog mLoginProgress;

    private FirebaseAuth mAuth;

    private DatabaseReference mUserDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        mToolbar = (Toolbar) findViewById(R.id.login_page_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Login");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users");

        mLoginEmail = findViewById(R.id.message_input);
        mLoginPassword = findViewById(R.id.login_password);

        mLoginProgress = new ProgressDialog(this);

        mLogin_btn = (Button) findViewById(R.id.login_button);

        mLogin_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = mLoginEmail.getText().toString();
                String password = mLoginPassword.getText().toString();
                
                if (!TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {

                    mLoginProgress.setTitle("Logging In");
                    mLoginProgress.setMessage("Please wait while we check your credential");
                    mLoginProgress.setCanceledOnTouchOutside(false);
                    mLoginProgress.show();

                    loginUser(email, password);
                }
            }
        });
    }

    private void loginUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            mLoginProgress.dismiss();

                            String current_user_id = mAuth.getCurrentUser().getUid();

                            FirebaseUser mUser = FirebaseAuth.getInstance().getCurrentUser();

                            mUser.getIdToken(true)
                                    .addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
                                        public void onComplete(@NonNull Task<GetTokenResult> task) {
                                            if (task.isSuccessful()) {
                                                String idToken = task.getResult().getToken();
                                                // Send token to your backend via HTTPS
                                                mUserDatabase.child(current_user_id).child("device_token").setValue(idToken)
                                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                            @Override
                                                            public void onSuccess(Void aVoid) {
                                                                Intent mainIntent = new Intent(LoginActivity.this, MainActivity.class);
                                                                mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                                startActivity(mainIntent);
                                                                finish();
                                                            }
                                                        });
                                            } else {
                                                // Handle error -> task.getException();
                                            }
                                        }
                                    });

                            /*Intent mainIntent = new Intent(LoginActivity.this, MainActivity.class);
                            mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(mainIntent);
                            finish();*/
                        } else {
                            mLoginProgress.hide();
                            Toast.makeText(LoginActivity.this, "Your email or password is wrong!", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}