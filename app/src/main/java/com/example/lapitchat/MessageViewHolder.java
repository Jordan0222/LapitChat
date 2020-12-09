package com.example.lapitchat;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessageViewHolder extends RecyclerView.ViewHolder {

    public TextView messageText;
    public CircleImageView mProfileImage;

    public MessageViewHolder(@NonNull View itemView) {
        super(itemView);

        messageText = itemView.findViewById(R.id.message_text_layout);
        mProfileImage = itemView.findViewById(R.id.message_image_layout);
    }
}
