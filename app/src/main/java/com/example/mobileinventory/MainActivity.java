package com.example.mobileinventory;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mobileinventory.R;

import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MainActivity extends AppCompatActivity {
    EditText Email, Password;
    TextView ErrorLog;
    Connection con = null;

    @Override
    protected void onStart() {
        grantCameraPermission();
        super.onStart();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Email = findViewById(R.id.EmailAddress);
        Password = findViewById(R.id.Password);
        ErrorLog = findViewById(R.id.ErrorLog);


        new DataBaseInitial(this).execute();

    }


    private static class DataBaseInitial extends AsyncTask<Void, Void, Void> {
        String Info = "";
        private final WeakReference<MainActivity> weakReference;

        DataBaseInitial(MainActivity context){
            weakReference = new WeakReference<>(context);
        }
        @Override
        protected Void doInBackground(Void... voids) {
            MainActivity activity = weakReference.get();
            try {
                Class.forName("com.mysql.jdbc.Driver");
                activity.con = DriverManager.getConnection("jdbc:mysql://175.45.63.58:3306/world?autoReconnect=true&useSSL=false", "tony", "Lunatoys123");
                Info += "Connection Successful \n";
            } catch (ClassNotFoundException | SQLException e) {
                Info += e.toString() + "\n";
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            MainActivity activity = weakReference.get();
            if (!Info.equals("")) {
                activity.ErrorLog.setText(Info);
            }

            super.onPostExecute(unused);
        }
    }


    public void Login(View view) {
        new User_Login(this).execute();
    }


    private static class User_Login extends AsyncTask<Void, Void, Void> {
        boolean Login_success = false;
        String Info = "";
        private final WeakReference<MainActivity> weakReference;

        User_Login(MainActivity context){
            weakReference = new WeakReference<>(context);
        }
        @Override
        protected Void doInBackground(Void... voids) {
            MainActivity activity = weakReference.get();

            String selectStatement = "select * from owners where user_name='" +
                    activity.Email.getText().toString().trim() + "' and user_password='" + activity.Password.getText().toString().trim() + "'";
            Log.i("Login ", selectStatement);
            PreparedStatement statement = null;
            ResultSet rs = null;

            try {
                statement = activity.con.prepareStatement(selectStatement);
                rs = statement.executeQuery();

                Login_success = rs.next();
            } catch (SQLException e) {
                Info += e.toString() + "\n";
            } finally {
                if (statement != null) {
                    try {
                        statement.close();
                    } catch (SQLException e) {
                        Info += e.toString() + "\n";
                    }
                }

                if (rs != null) {
                    try {
                        rs.close();
                    } catch (SQLException e) {
                        Info += e.toString() + "\n";
                    }
                }
            }


            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            MainActivity activity = weakReference.get();
            if (activity.Email.getText().toString().trim().equalsIgnoreCase("")) {
                Toast.makeText(activity, "No Email", Toast.LENGTH_SHORT).show();
                return;
            }

            if (activity.Password.getText().toString().trim().equalsIgnoreCase("")) {
                Toast.makeText(activity, "No Password", Toast.LENGTH_SHORT).show();
                return;
            }
            if(Login_success){
                try {
                    activity.con.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                Intent intent = new Intent(activity, MainMenu.class);
                activity.startActivity(intent);
            }else{
                activity.ErrorLog.append("Login failed");
                activity.ErrorLog.append(Info);
            }
            super.onPostExecute(unused);
        }
    }

    private void grantCameraPermission() {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},1);
        }
    }
}