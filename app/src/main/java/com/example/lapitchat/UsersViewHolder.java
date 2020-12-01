package com.example.lapitchat;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class UsersViewHolder extends RecyclerView.ViewHolder {

    public CircleImageView mDisplayImage;
    public TextView txtDisplayName;
    public TextView txtStatus;
    public LinearLayout root;

    public UsersViewHolder(@NonNull View itemView) {
        super(itemView);

        root = itemView.findViewById(R.id.recycler_root);
        mDisplayImage = itemView.findViewById(R.id.user_single_image);
        txtDisplayName = itemView.findViewById(R.id.user_single_name);
        txtStatus = itemView.findViewById(R.id.user_single_status);
    }

    public void setTxtDisplayName(String string) {
        txtDisplayName.setText(string);
    }


    public void setTxtStatus(String string) {
        txtStatus.setText(string);
    }

    public void setImage(String string) {
        Picasso.get().load(string).into(mDisplayImage);
    }
}