package com.example.lapitchat;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.SnapshotParser;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;


public class FriendsFragment extends Fragment {

    private static final String TAG = FriendsFragment.class.getSimpleName();
    private RecyclerView mFriendsList;

    private DatabaseReference mFriendsDatabase;
    private DatabaseReference mUserDatabase;
    private FirebaseAuth mAuth;

    private String mCurrent_user_id;

    private View mMainView;

    private FirebaseRecyclerAdapter FriendsAdapter;

    public FriendsFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mMainView = inflater.inflate(R.layout.fragment_friends, container, false);

        mFriendsList = (RecyclerView) mMainView.findViewById(R.id.friends_list);
        mAuth = FirebaseAuth.getInstance();

        mCurrent_user_id = mAuth.getCurrentUser().getUid();

        mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users");
        mUserDatabase.keepSynced(true);

        mFriendsList.setHasFixedSize(true);
        mFriendsList.setLayoutManager(new LinearLayoutManager(getContext()));

        Load();

        return mMainView;
    }

    private void Load() {
        Query query = FirebaseDatabase.getInstance().getReference().child("Friends").child(mCurrent_user_id);
        FirebaseRecyclerOptions<Friends> options = new FirebaseRecyclerOptions.Builder<Friends>()
                .setQuery(query, new SnapshotParser<Friends>() {
                    @NonNull
                    @Override
                    public Friends parseSnapshot(@NonNull DataSnapshot snapshot) {
                        return new Friends(snapshot.child("date").getValue().toString());
                    }
                })
                .build();

        FriendsAdapter = new FirebaseRecyclerAdapter<Friends, FriendsViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull FriendsViewHolder holder, int position, @NonNull Friends friends) {

                // holder.setTxtDate(friends.getDate());

                String user_id = getRef(position).getKey();
                mUserDatabase.child(user_id).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        String userName = snapshot.child("name").getValue().toString();
                        String userImage = snapshot.child("image").getValue().toString();
                        String userStatus = snapshot.child("status").getValue().toString();
                        String userOnline = snapshot.child("online").getValue().toString();

                        Log.d(TAG, "onDataChange: " + userOnline);

                        holder.setTxtDisplayName(userName);
                        holder.setImage(userImage);
                        holder.setTxtStatus(userStatus);
                        holder.setUserOnline(userOnline);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

            }

            @NonNull
            @Override
            public FriendsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.users_singlelayout, parent, false);

                return new FriendsViewHolder(view);
            }
        };
        mFriendsList.setAdapter(FriendsAdapter);
        FriendsAdapter.startListening();
    }
}