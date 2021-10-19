package com.example.mobileinventory;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class tableContent extends AppCompatActivity {
    Connection con = null;
    ListView lv;
    Button MainMenuBtn, SearchingBtn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_table_content);

        lv = findViewById(R.id.tableview);
        MainMenuBtn = findViewById(R.id.MainMenuBtn);
        SearchingBtn = findViewById(R.id.SearchingBtn);
        new DataBaseInitial(this).execute();

        Bundle extras = getIntent().getExtras();
        if(extras!=null){
            String option = getIntent().getStringExtra("option");
            if(option.equalsIgnoreCase("Choice from user")){
                String owner_post = getIntent().getStringExtra("owner_post");
                String owner_name = getIntent().getStringExtra("owner_name");
                String owner_division = getIntent().getStringExtra("owner_division");
                AllData allData = new AllData(this);
                allData.execute(owner_post, owner_name, owner_division);
            }else if(option.equalsIgnoreCase("Choice from Serial number")){
                String Serial = getIntent().getStringExtra("Serial_No");
                AllDataBySerial allDataBySerial = new AllDataBySerial(this);
                allDataBySerial.execute(Serial);
            }
        }

        MainMenuBtn.setOnClickListener(v -> {
            Intent intent = new Intent(tableContent.this, MainMenu.class);
            startActivity(intent);
        });

        SearchingBtn.setOnClickListener(v -> {
            Intent intent = new Intent(tableContent.this, tableview.class);
            startActivity(intent);
        });


    }

    private static class AllDataBySerial extends AsyncTask<String, Void, Void> {
        private final WeakReference<tableContent> weakReference;
        List<String> All_data = new ArrayList<>();
        List<InputStream> qr_code_list = new ArrayList<>();

        AllDataBySerial(tableContent context) {
            weakReference = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(String... params) {
            tableContent activity = weakReference.get();
            String Serial = params[0];
            String sql = "select o.owner_post, o.owner_name, o.owner_division, i.Type, i.Model, i.Serial, i.QR_code from inventory i, owners o where " +
                    " o.owner_id = i.owner_id and i.Serial like ?";
            PreparedStatement ps = null;
            ResultSet rs = null;

            try {
                ps = activity.con.prepareStatement(sql);
                ps.setString(1, "%" + Serial + "%");
                rs = ps.executeQuery();
                while (rs.next()) {
                    String content = rs.getString("owner_post") + "\n" + rs.getString("owner_name") + "\n" + rs.getString("owner_division") + "\n" +
                            rs.getString("Type") + "\n" + rs.getString("Model") + "\n" + rs.getString("Serial");
                    All_data.add(content);
                    InputStream qrCode = rs.getBinaryStream("QR_code");
                    qr_code_list.add(qrCode);
                }
            } catch (SQLException throwable) {
                throwable.printStackTrace();
            } finally {
                try {
                    if (ps != null) ps.close();
                    if (rs != null) rs.close();
                } catch (SQLException throwable) {
                    throwable.printStackTrace();
                }
            }


            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            tableContent activity = weakReference.get();
            MyAdapter adapter = new MyAdapter(activity, All_data, qr_code_list);
            activity.lv.setAdapter(adapter);
            super.onPostExecute(unused);
        }
    }

    private static class AllData extends AsyncTask<String, Void, Void> {
        private final WeakReference<tableContent> weakReference;
        List<String> All_data = new ArrayList<>();
        List<InputStream> qr_code_list = new ArrayList<>();

        AllData(tableContent context) {
            weakReference = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(String... params) {
            tableContent activity = weakReference.get();
            String sql = "select o.owner_post, o.owner_name, o.owner_division, i.Type, i.Model, i.Serial, i.QR_code from inventory i, owners o where " +
                    "o.owner_id=i.owner_id and o.owner_post=? and o.owner_name=? and o.owner_division=?";
            PreparedStatement ps = null;
            ResultSet rs = null;

            try {
                ps = activity.con.prepareStatement(sql);
                ps.setString(1, params[0]);
                ps.setString(2, params[1]);
                ps.setString(3, params[2]);
                rs = ps.executeQuery();

                while (rs.next()) {
                    String content = rs.getString("owner_post") + "\n" + rs.getString("owner_name") + "\n" + rs.getString("owner_division") + "\n" +
                            rs.getString("Type") + "\n" + rs.getString("Model") + "\n" + rs.getString("Serial");
                    InputStream qrcode = rs.getBinaryStream("QR_code");
                    All_data.add(content);
                    qr_code_list.add(qrcode);
                }
            } catch (SQLException throwable) {
                throwable.printStackTrace();
            } finally {
                try {
                    if (ps != null) ps.close();
                    if (rs != null) rs.close();
                } catch (SQLException throwable) {
                    throwable.printStackTrace();
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            tableContent activity = weakReference.get();

            if (All_data.size() == 0 && qr_code_list.size() == 0) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(activity);
                dialog.setTitle("No Information");
                dialog.setMessage("The Search requirement does not come up with any result");
                dialog.setPositiveButton("Confirm", (dialog1, which) -> dialog1.dismiss());
                dialog.setCancelable(false);
                dialog.show();
            }else{
                MyAdapter adapter = new MyAdapter(activity, All_data, qr_code_list);
                activity.lv.setAdapter(adapter);
            }
            super.onPostExecute(unused);
        }
    }

    private static class DataBaseInitial extends AsyncTask<Void, Void, Void> {
        private final WeakReference<tableContent> weakReference;
        String Info = "";

        DataBaseInitial(tableContent context) {
            weakReference = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            tableContent activity = weakReference.get();
            try {
                Class.forName("com.mysql.jdbc.Driver");
                activity.con = DriverManager.getConnection("jdbc:mysql://175.45.63.58:3306/world?autoReconnect=true&useSSL=false", "tony", "Lunatoys123");
                Info += "\nConnection Successful";
            } catch (SQLException | ClassNotFoundException throwable) {
                Info += "\n" + throwable.toString();
            }
            return null;
        }


        @Override
        protected void onPostExecute(Void unused) {

            super.onPostExecute(unused);
        }
    }
}