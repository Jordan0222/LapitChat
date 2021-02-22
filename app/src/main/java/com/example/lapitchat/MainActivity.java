package com.example.lapitchat;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private Toolbar mToolbar;

    private ViewPager mViewPager;
    private SectionPagerAdapter mSectionPagerAdapter;

    private DatabaseReference mUserRef;

    private TabLayout mTabLayout;

    private FirebaseUser mCurrent_user;

    String filepath = "";
    String fileContent = "";
    String filename = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        filename = "test.txt";
        String test = "test";
        fileContent = test.trim();

        if (!fileContent.equals("")) {

            File myExternalFile = new File(getExternalFilesDir(filepath), filename);
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(myExternalFile);
                fos.write(fileContent.getBytes());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {

        }


        mToolbar = (Toolbar) findViewById(R.id.main_page_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Lapit Chat");

        mAuth = FirebaseAuth.getInstance();
        mCurrent_user = mAuth.getCurrentUser();

        if (mCurrent_user != null) {
            mUserRef = FirebaseDatabase.getInstance().getReference().child("Users").child(mCurrent_user.getUid());
        }


        mViewPager = findViewById(R.id.main_tabPager);
        mSectionPagerAdapter = new SectionPagerAdapter(getSupportFragmentManager());

        mViewPager.setAdapter(mSectionPagerAdapter);

        mTabLayout = (TabLayout) findViewById(R.id.main_tabs);
        mTabLayout.setupWithViewPager(mViewPager);
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // check user if is null
        if (currentUser == null) {
            startActivity(new Intent(this, StartActivity.class));
            finish();
        } else {
            mUserRef.child("online").setValue(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mUserRef.child("online").setValue(ServerValue.TIMESTAMP);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        if (item.getItemId() == R.id.main_logout) {
            mUserRef.child("online").setValue(ServerValue.TIMESTAMP);
            FirebaseAuth.getInstance().signOut();
            sentToStart();
        }

        if (item.getItemId() == R.id.main_setting_btn) {
            Intent settingIntent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(settingIntent);
        }

        if (item.getItemId() == R.id.main_all_btn) {
            Intent settingIntent = new Intent(MainActivity.this, UserActivity.class);
            startActivity(settingIntent);
        }
        return true;
    }

    private void sentToStart() {
        Intent startIntent = new Intent(MainActivity.this, StartActivity.class);
        startActivity(startIntent);
        finish();
    }
}