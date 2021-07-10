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
            String division = getIntent().getStringExtra("division");
            String post = getIntent().getStringExtra("post");
            String name = getIntent().getStringExtra("name");
            String Type = getIntent().getStringExtra("Type");
            String Model = getIntent().getStringExtra("Model");
            String SerialNo = getIntent().getStringExtra("Serial");

            String QRContent = "Audit Commission \ndivision: " + division + "\n" +
                    "post: " + post + "\n" +
                    "name: " + name + "\n" +
                    "Type: " + Type + "\n" +
                    "Model_no: " + Model + "\n" +
                    "Serial_no: " + SerialNo;

            MultiFormatWriter multiFormatWriter = new MultiFormatWriter();

            try {
                // AESEncryption aes = new AESEncryption("audit Commission");
                // QRContent = aes.encrypt(QRContent);
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

    public void send(View view) throws ExecutionException, InterruptedException {
        String division = getIntent().getStringExtra("division");
        String post = getIntent().getStringExtra("post");
        String name = getIntent().getStringExtra("name");
        String Type = getIntent().getStringExtra("Type");
        String Model = getIntent().getStringExtra("Model");
        String SerialNo = getIntent().getStringExtra("Serial");
        String ownersId = new getOwnersID(this).execute(division, post, name).get();
        String EquipmentId = new getEquipmentID(this).execute(Type, Model, SerialNo).get();
        new SendImageToDataBase(this).execute(ownersId, EquipmentId);
    }

    private static class getEquipmentID extends AsyncTask<String, Void, String> {
        private final WeakReference<ViewQRCode> weakReference;

        getEquipmentID(ViewQRCode context) {
            weakReference = new WeakReference<>(context);
        }

        @Override
        protected String doInBackground(String... params) {
            ViewQRCode activity = weakReference.get();
            String EquipmentID = null;
            String sql = "select E_id as id from equipment where Type=? and Model=? and Serial =?";
            PreparedStatement ps = null;
            ResultSet rs = null;

            try {
                ps = activity.con.prepareStatement(sql);
                ps.setString(1, params[0]);
                ps.setString(2, params[1]);
                ps.setString(3, params[2]);
                rs = ps.executeQuery();

                if(rs.next()){
                    EquipmentID = rs.getString("id");
                }
            } catch (SQLException throwable) {
                throwable.printStackTrace();
            } finally{
                if(ps!=null) {
                    try {
                        ps.close();
                    } catch (SQLException throwable) {
                        throwable.printStackTrace();
                    }
                }

                if(rs!=null){
                    try {
                        rs.close();
                    } catch (SQLException throwable) {
                        throwable.printStackTrace();
                    }
                }
            }

            return EquipmentID;
        }
    }

    private static class getOwnersID extends AsyncTask<String, Void, String> {

        private final WeakReference<ViewQRCode> weakReference;

        getOwnersID(ViewQRCode context) {
            weakReference = new WeakReference<>(context);
        }

        @Override
        protected String doInBackground(String... params) {
            ViewQRCode activity = weakReference.get();
            String ownerID = null;

            String sql = "SELECT owner_id as id FROM owners where owner_post=? and owner_name=? and owner_division=?";
            PreparedStatement ps = null;
            ResultSet rs = null;

            try {
                ps = activity.con.prepareStatement(sql);
                ps.setString(1, params[1]);
                ps.setString(2, params[2]);
                ps.setString(3, params[0]);
                rs = ps.executeQuery();

                if (rs.next()) {
                    ownerID = rs.getString("id");
                }
            } catch (SQLException throwable) {
                throwable.printStackTrace();
            } finally {
                if (ps != null) {
                    try {
                        ps.close();
                    } catch (SQLException throwable) {
                        throwable.printStackTrace();
                    }
                }

                if (rs != null) {
                    try {
                        rs.close();
                    } catch (SQLException throwable) {
                        throwable.printStackTrace();
                    }
                }
            }

            return ownerID;
        }
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
            String sql = "insert into inventory values (null,?,?,?,?)";
            PreparedStatement ps = null;
            try {
                ps = activity.con.prepareStatement(sql);
                ps.setString(1, params[1]);
                ps.setString(2, params[0]);
                ps.setTimestamp(3, new java.sql.Timestamp(new Date().getTime()));
                BitmapDrawable bitmapDrawable = (BitmapDrawable) activity.QRCodeImageView.getDrawable();
                Bitmap bitmap1 = bitmapDrawable.getBitmap();
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap1.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                byte[] imageInByte = stream.toByteArray();
                ByteArrayInputStream bs = new ByteArrayInputStream(imageInByte);
                ps.setBinaryStream(4, bs);
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