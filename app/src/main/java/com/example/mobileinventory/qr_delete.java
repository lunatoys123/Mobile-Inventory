package com.example.mobileinventory;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;

public class qr_delete extends AppCompatActivity {
    String Old_division, Old_post, Old_name, Old_type, Old_model, Old_Serial;
    TextView debug;
    Connection con = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_delete);
        debug = findViewById(R.id.debug);

        String[] content = getIntent().getStringExtra("content").split("\n");
        Old_division = content[1].split(":")[1];
        Old_post = content[2].split(":")[1];
        Old_name = content[3].split(":")[1];
        Old_type = content[4].split(":")[1];
        Old_model = content[5].split(":")[1];
        Old_Serial = (content[6].split(":").length == 2) ? content[6].split(":")[1] : "";
        new DataBaseInitial(this).execute();

        try {
            getOwnersID getOwner = new getOwnersID(this);
            String ownersId = getOwner.execute(Old_division.trim(), Old_post.trim(), Old_name.trim()).get();

            getEquipmentID getEquipment = new getEquipmentID(this);
            String equipmentId = getEquipment.execute(Old_type.trim(), Old_model.trim(), Old_Serial.trim()).get();

            new DeleteInventory(this).execute(ownersId, equipmentId);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }

//        AlertDialog.Builder dialog = new AlertDialog.Builder(qr_delete.this);
//        dialog.setTitle("Welcome");
//        dialog.setMessage(getIntent().getStringExtra("content"));
//        dialog.setPositiveButton("Confirm", (dialog1, which) -> dialog1.cancel());
//        dialog.show();
    }

    private static class DeleteInventory extends AsyncTask<String, Void, Void> {
        private final WeakReference<qr_delete> weakReference;

        DeleteInventory(qr_delete context) {
            weakReference = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(String... params) {
            qr_delete activity = weakReference.get();
            String ownersId = params[0];
            String equipmentId = params[1];
            String sql = "Delete from Inventory where E_id=? and owner_id=?";
            PreparedStatement ps = null;

            try {
                ps = activity.con.prepareStatement(sql);
                ps.setString(1, equipmentId);
                ps.setString(2, ownersId);
                ps.executeUpdate();
            } catch (SQLException throwable) {
                throwable.printStackTrace();
            } finally {
                if(ps!=null){
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
            qr_delete activity = weakReference.get();
            AlertDialog.Builder dialog = new AlertDialog.Builder(activity);
            dialog.setTitle("Delete successful");
            dialog.setCancelable(false);
            dialog.setMessage(activity.getIntent().getStringExtra("content"));
            dialog.setPositiveButton("Confirm", (dialog1, which) -> {
                Intent intent = new Intent(activity, MainMenu.class);
                activity.startActivity(intent);
            });
            dialog.show();
            super.onPostExecute(unused);
        }
    }

    private static class DataBaseInitial extends AsyncTask<Void, Void, Void> {
        String Info = "";
        private final WeakReference<qr_delete> weakReference;

        DataBaseInitial(qr_delete context) {
            weakReference = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            qr_delete activity = weakReference.get();
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
            qr_delete activity = weakReference.get();
            if (!Info.equals("")) {
                activity.debug.setText(Info);
            }
            activity.debug.append("\n" + activity.getIntent().getStringExtra("content"));

            super.onPostExecute(unused);
        }
    }

    private static class getEquipmentID extends AsyncTask<String, Void, String> {
        private final WeakReference<qr_delete> weakReference;

        getEquipmentID(qr_delete context) {
            weakReference = new WeakReference<>(context);
        }

        @Override
        protected String doInBackground(String... params) {
            qr_delete activity = weakReference.get();
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

                if (rs.next()) {
                    EquipmentID = rs.getString("id");
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

            return EquipmentID;
        }

        @Override
        protected void onPostExecute(String s) {
            qr_delete activity = weakReference.get();
            activity.debug.append("\nfinish getEquipment ID");
            super.onPostExecute(s);
        }
    }

    private static class getOwnersID extends AsyncTask<String, Void, String> {

        private final WeakReference<qr_delete> weakReference;

        getOwnersID(qr_delete context) {
            weakReference = new WeakReference<>(context);
        }

        @Override
        protected void onPostExecute(String s) {
            qr_delete activity = weakReference.get();
            activity.debug.append("\nfinish getOwners");
            super.onPostExecute(s);
        }

        @Override
        protected String doInBackground(String... params) {
            if (isCancelled()) {
                return null;
            } else {
                qr_delete activity = weakReference.get();
                String ownerID = null;

                String sql = "SELECT owner_id as id FROM owners where owner_post=? and owner_name=? and owner_division=?";
                PreparedStatement ps = null;
                ResultSet rs = null;

                if (!isCancelled()) {
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

                }
                return ownerID;
            }

        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }
    }
}