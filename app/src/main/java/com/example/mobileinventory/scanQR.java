package com.example.mobileinventory;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.IOException;

public class scanQR extends AppCompatActivity {

    SurfaceView surfaceView;
    TextView textView;
    CameraSource cameraSource;
    BarcodeDetector barcodeDetector;
    boolean toAnotherActivity;

    @Override
    protected void onStart() {
        super.onStart();
        toAnotherActivity = false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_qr);
        surfaceView = findViewById(R.id.surfaceView);
        textView = findViewById(R.id.textView);
        barcodeDetector = new BarcodeDetector.Builder(this).setBarcodeFormats(Barcode.QR_CODE).build();
        cameraSource = new CameraSource.Builder(this, barcodeDetector).setRequestedPreviewSize(300, 300).setAutoFocusEnabled(true).build();

        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }

                try {
                    cameraSource.start(holder);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                cameraSource.stop();
            }
        });

        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {

            }

            @Override
            public void receiveDetections(@NonNull @org.jetbrains.annotations.NotNull Detector.Detections<Barcode> detections) {
                final SparseArray<Barcode> qrCode = detections.getDetectedItems();
                if (qrCode.size() != 0) {
                    //AESEncryption aes = new AESEncryption("audit Commission");
                    try {
                        String DecodeContent = qrCode.valueAt(0).displayValue;

                        if (!toAnotherActivity) {
                            Log.i("ScanQR", String.valueOf(DecodeContent.contains("Audit Commission")));
                            textView.post(() -> textView.setText(DecodeContent));
                            if (DecodeContent.contains("Audit Commission")) {
                                String Action = getIntent().getStringExtra("Action");
                                if (Action.equalsIgnoreCase("update")) {
                                    Intent intent = new Intent(scanQR.this, qr_update.class);
                                    intent.putExtra("content", DecodeContent);
                                    startActivity(intent);
                                } else if (Action.equalsIgnoreCase("Delete")) {
                                    Intent intent = new Intent(scanQR.this, qr_delete.class);
                                    intent.putExtra("content", DecodeContent);
                                    startActivity(intent);
                                }
                                toAnotherActivity = true;
                            }
                        }


                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }


}