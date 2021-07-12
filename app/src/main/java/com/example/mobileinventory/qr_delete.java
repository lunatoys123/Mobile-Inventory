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
    String[] Info;
    TextView debug;
    Connection con = null;
    DataBaseInitial dataBaseInitial;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_delete);
        debug = findViewById(R.id.debug);

        String[] content = getIntent().getStringExtra("content").split("\n");
        String item_id = content[1];
        /*Old_division = content[1].split(":")[1];
        Old_post = content[2].split(":")[1];
        Old_name = content[3].split(":")[1];
        Old_type = content[4].split(":")[1];
        Old_model = content[5].split(":")[1];
        Old_Serial = (content[6].split(":").length == 2) ? content[6].split(":")[1] : "";*/

        dataBaseInitial = new DataBaseInitial(this);
        dataBaseInitial.execute();

        try {
            getInfoFromItemId getInfoFromItemId = new getInfoFromItemId(this);
            Info = getInfoFromItemId.execute(item_id).get();
            new DeleteInventory(this).execute(item_id);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }


    }

    private static class getInfoFromItemId extends AsyncTask<String, Void, String[]> {
        private final WeakReference<qr_delete> weakReference;

        getInfoFromItemId(qr_delete context) {
            weakReference = new WeakReference<>(context);
        }

        @Override
        protected String[] doInBackground(String... params) {
            String[] result = new String[6];
            qr_delete activity = weakReference.get();
            String item_id = params[0];
            String sql = "select o.owner_division,o.owner_post, o.owner_name, e.Type, e.Model, e.Serial " +
                    "from inventory i ,owners o,equipment e where i.items_id = ? " +
                    "and i.E_id = e.E_id and i.owner_id = o.owner_id";
            PreparedStatement ps = null;
            ResultSet rs = null;

            try {
                ps = activity.con.prepareStatement(sql);
                ps.setString(1, item_id);
                rs = ps.executeQuery();
                if (rs.next()) {
                    result[0] = rs.getString("owner_division");
                    result[1] = rs.getString("owner_post");
                    result[2] = rs.getString("owner_name");
                    result[3] = rs.getString("Type");
                    result[4] = rs.getString("Model");
                    result[5] = rs.getString("Serial");

                    return result;
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
    }

    private static class DeleteInventory extends AsyncTask<String, Void, Void> {
        private final WeakReference<qr_delete> weakReference;

        DeleteInventory(qr_delete context) {
            weakReference = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(String... params) {
            qr_delete activity = weakReference.get();
            String item_id = params[0];
            String sql = "Delete from Inventory where Items_ID=?";
            PreparedStatement ps = null;

            try {
                ps = activity.con.prepareStatement(sql);
                ps.setString(1, item_id);
                ps.executeUpdate();
            } catch (SQLException throwable) {
                throwable.printStackTrace();
            } finally {

                try {
                    if (ps != null) ps.close();
                } catch (SQLException throwable) {
                    throwable.printStackTrace();
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

            String message = "";
            for(int i =0;i<activity.Info.length;i++){
                message+=activity.Info[i]+"\n";
            }
            dialog.setMessage(message);

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
                activity.con = DriverManager.getConnection("jdbc:mysql://175.45.63.58:3306/world?autoReconnect=true&useSSL=false", "tony", "Lunatoys123");
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
                {
                    try {
                        if (ps != null) ps.close();
                        if (rs != null) rs.close();
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
                        {
                            try {
                                if (ps != null) ps.close();
                                if (rs != null) rs.close();
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