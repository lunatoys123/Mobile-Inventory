package com.example.mobileinventory;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.concurrent.ExecutionException;

public class ViewQRCode extends AppCompatActivity {

    ImageView QRCodeImageView;
    Button sendImage;
    TextView textView;
    Connection con = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_qrcode);

        QRCodeImageView = findViewById(R.id.QRCodeImageView);
        sendImage = findViewById(R.id.sendImage);
        textView = findViewById(R.id.textView4);

        Bundle extras = getIntent().getExtras();
        new DataBaseInitial(this).execute();
        if (extras != null) {
            String item_id = getIntent().getStringExtra("item_id");


            String QRContent = "Audit Commission \n" + item_id;

            MultiFormatWriter multiFormatWriter = new MultiFormatWriter();

            try {

                BitMatrix bitMatrix = multiFormatWriter.encode(QRContent, BarcodeFormat.QR_CODE, 500, 500);
                BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
                Bitmap bitmap = barcodeEncoder.createBitmap(bitMatrix);
                QRCodeImageView.setImageBitmap(bitmap);
                textView.append(QRContent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public void send(View view) {
        String item_id = getIntent().getStringExtra("item_id");
        new SendImageToDataBase(this).execute(item_id);
    }



    private static class SendImageToDataBase extends AsyncTask<String, Void, Void> {
        String info = "";
        boolean insert = false;
        private final WeakReference<ViewQRCode> weakReference;

        SendImageToDataBase(ViewQRCode context) {
            weakReference = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(String... params) {
            ViewQRCode activity = weakReference.get();
            String sql = "update inventory set QR_Code = ? where Items_Id = ?";
            PreparedStatement ps = null;
            try {
                ps = activity.con.prepareStatement(sql);
                BitmapDrawable bitmapDrawable = (BitmapDrawable) activity.QRCodeImageView.getDrawable();
                Bitmap bitmap1 = bitmapDrawable.getBitmap();
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap1.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                byte[] imageInByte = stream.toByteArray();
                ByteArrayInputStream bs = new ByteArrayInputStream(imageInByte);
                ps.setBinaryStream(1, bs);
                ps.setString(2, params[0]);

                ps.executeUpdate();
                insert = true;
            } catch (SQLException e) {
                //debug.setText(e.toString());
                info += e.toString() + "\n";
                insert = false;
            } finally {
                if (ps != null) {
                    try {
                        ps.close();
                    } catch (SQLException throwable) {
                        throwable.printStackTrace();
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            ViewQRCode activity = weakReference.get();
            if (insert) {
                activity.textView.append("\nInsert success");
                AlertDialog.Builder dialog = new AlertDialog.Builder(activity);
                dialog.setTitle("Insert success");
                dialog.setCancelable(false);
                dialog.setPositiveButton("Confirm", (dialog1, which) -> {
                    Intent intent = new Intent(activity, MainMenu.class);
                    activity.startActivity(intent);
                });
                dialog.show();
            } else {
                activity.textView.append("\n" + info);
            }

            super.onPostExecute(unused);
        }
    }

    private static class DataBaseInitial extends AsyncTask<Void, Void, Void> {
        String Info = "";
        private final WeakReference<ViewQRCode> weakReference;

        DataBaseInitial(ViewQRCode context) {
            weakReference = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            ViewQRCode activity = weakReference.get();
            try {
                Class.forName("com.mysql.jdbc.Driver");
                activity.con = DriverManager.getConnection("jdbc:mysql://192.168.1.140:3306/world?autoReconnect=true&useSSL=false", "tony", "Lunatoys123");
                Info += "Connection Successful \n";
            } catch (ClassNotFoundException | SQLException e) {
                Info += e.toString() + "\n";
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            ViewQRCode activity = weakReference.get();
            if (!Info.equals("")) {
                activity.textView.append(Info);
            }

            super.onPostExecute(unused);
        }
    }
}