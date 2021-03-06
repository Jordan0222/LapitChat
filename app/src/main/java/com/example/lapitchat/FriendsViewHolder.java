package com.example.lapitchat;

import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class FriendsViewHolder extends RecyclerView.ViewHolder{

    public LinearLayout root;
    // public TextView mDate;
    public TextView txtDisplayName;
    public TextView txtStatus;
    public CircleImageView mDisplayImage;
    public ImageView userOnlineView;

    public FriendsViewHolder(@NonNull View itemView) {
        super(itemView);

        root = itemView.findViewById(R.id.recycler_root);
        mDisplayImage = itemView.findViewById(R.id.user_single_image);
        txtDisplayName = itemView.findViewById(R.id.user_single_name);
        txtStatus = itemView.findViewById(R.id.user_single_status);
        userOnlineView = itemView.findViewById(R.id.user_single_online);
    }

    public void setTxtDisplayName(String string) {
        txtDisplayName.setText(string);
    }

    /*public void setTxtDate(String string) {
        mDate.setText(string);
    }*/

    public void setImage(String string) {
        Picasso.get().load(string).into(mDisplayImage);
    }

    public void setTxtStatus(String string) {
        txtStatus.setText(string);
    }

    public void setUserOnline(String online_status) {
        if (online_status.equals("true")) {
            userOnlineView.setVisibility(View.VISIBLE);
        } else {
            userOnlineView.setVisibility(View.INVISIBLE);
        }
    }
}
