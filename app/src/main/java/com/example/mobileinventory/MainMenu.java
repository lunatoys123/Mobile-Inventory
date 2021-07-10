package com.example.mobileinventory;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainMenu extends AppCompatActivity {

    Button GenerateQRCode, updateQRBtn, DeleteQRBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        GenerateQRCode = findViewById(R.id.GenQRCode);
        updateQRBtn = findViewById(R.id.updateQRBtn);
        DeleteQRBtn = findViewById(R.id.DeleteQRBtn);


        GenerateQRCode.setOnClickListener(v -> {
            Intent intent = new Intent(MainMenu.this, generateQRCode.class);
            startActivity(intent);
        });

        updateQRBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainMenu.this, scanQR.class);
            intent.putExtra("Action", "update");
            startActivity(intent);
        });

        DeleteQRBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainMenu.this, scanQR.class);
            intent.putExtra("Action","Delete");
            startActivity(intent);
        });
    }
}