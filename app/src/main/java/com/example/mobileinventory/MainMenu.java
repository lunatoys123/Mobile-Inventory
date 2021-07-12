package com.example.mobileinventory;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainMenu extends AppCompatActivity {

    Button GenerateQRCode, updateQRBtn, showData;
    final String[] option = {"update", "Delete"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        GenerateQRCode = findViewById(R.id.GenQRCode);
        updateQRBtn = findViewById(R.id.updateQRBtn);
        showData = findViewById(R.id.showData);


        GenerateQRCode.setOnClickListener(v -> {
            Intent intent = new Intent(MainMenu.this, generateQRCode.class);
            startActivity(intent);
        });

        updateQRBtn.setOnClickListener(v -> {

            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle("choose operation");
            dialog.setItems(option, (dialog1, which) -> {
                Intent intent = new Intent(MainMenu.this, scanQR.class);
                intent.putExtra("Action", option[which]);
                startActivity(intent);
            });
            dialog.setCancelable(false);
            dialog.show();
        });

        showData.setOnClickListener(v -> {
            Intent intent = new Intent(MainMenu.this, tableview.class);
            startActivity(intent);
        });


    }
}