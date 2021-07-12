package com.example.mobileinventory;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;

import java.io.InputStream;
import java.util.List;

public class MyAdapter extends ArrayAdapter<String> {
    private final Activity context;
    private final List<String> resource;
    private final List<InputStream> qrcode;

    public MyAdapter(@NonNull Activity context, List<String> resource, List<InputStream> qrcode) {
        super(context, R.layout.mylist, resource);
        this.context = context;
        this.resource = resource;
        this.qrcode = qrcode;
    }

    public View getView(int position, View view, ViewGroup parent) {
        View singleItem = view;
        ViewHolder holder;
        if (singleItem == null) {
            LayoutInflater layoutInflater = context.getLayoutInflater();
            singleItem = layoutInflater.inflate(R.layout.mylist, parent, false);
            holder = new ViewHolder(singleItem);
            singleItem.setTag(holder);
        } else {
            holder = (ViewHolder) singleItem.getTag();
        }

        String[] content = resource.get(position).split("\n");
        String post = content[0];
        String name = content[1];
        String division = content[2];
        String type = content[3];
        String model = content[4];
        String Serial = content[5];

        holder.post.setText(post);
        holder.name.setText(name);
        holder.division.setText(division);
        holder.type.setText(type);
        holder.model.setText(model);
        holder.Serial.setText(Serial);

        InputStream stream = qrcode.get(position);
        Bitmap bitmap = BitmapFactory.decodeStream(stream);
        holder.icon.setImageBitmap(bitmap);
        return singleItem;
    }
}
