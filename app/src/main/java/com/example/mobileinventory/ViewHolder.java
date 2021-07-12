package com.example.mobileinventory;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class ViewHolder {
    TextView post, name, division, type, model, Serial;
    ImageView icon;

    public ViewHolder(View v){
        post = v.findViewById(R.id.post);
        name = v.findViewById(R.id.name);
        division = v.findViewById(R.id.division);
        type = v.findViewById(R.id.Type);
        model = v.findViewById(R.id.model);
        Serial = v.findViewById(R.id.Serial);
        icon = v.findViewById(R.id.icon);
    }
}
