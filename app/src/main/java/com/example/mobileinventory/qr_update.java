package com.example.mobileinventory;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class qr_update extends AppCompatActivity {

    TextView debug, nameText;
    Connection con = null;
    Spinner division_spinner, post_spinner, Type_spinner, model_spinner, Serial_no_spinner;
    String Old_division, Old_post, Old_name, Old_type, Old_model, Old_Serial, Old_ownersId, Old_EquipmentId;
    getOwnersID getOwners;
    getEquipmentID getEquipmentID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_update);

        String[] content = getIntent().getStringExtra("content").split("\n");
        Old_division = content[1].split(":")[1];
        Old_post = content[2].split(":")[1];
        Old_name = content[3].split(":")[1];
        Old_type = content[4].split(":")[1];
        Old_model = content[5].split(":")[1];
        Old_Serial = (content[6].split(":").length == 2) ? content[6].split(":")[1] : "";

        debug = findViewById(R.id.Debug);
        division_spinner = findViewById(R.id.division_Spinner);
        post_spinner = findViewById(R.id.Post_spinner);
        nameText = findViewById(R.id.nameText);
        Type_spinner = findViewById(R.id.Type_Spinner);
        model_spinner = findViewById(R.id.modelSpinner);
        Serial_no_spinner = findViewById(R.id.SerialNoSpinner);

        new DataBaseInitial(this).execute();
        new InitialSpinnerOwners(this).execute();
        new setUpEquipmentSpinner(this).execute();

        try {
            getOwners = new getOwnersID(this);
            getEquipmentID = new getEquipmentID(this);
            Old_ownersId = getOwners.execute(Old_division.trim(), Old_post.trim(), Old_name.trim()).get();
            Old_EquipmentId = getEquipmentID.execute(Old_type.trim(), Old_model.trim(), Old_Serial.trim()).get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }


        try {
            checkQRExists exists = new checkQRExists(this);
            boolean QRExists = exists.execute(Old_ownersId, Old_EquipmentId).get();
            if (!QRExists) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(qr_update.this);
                dialog.setTitle("Exception");
                dialog.setMessage("This QR Code not exists in Database");
                dialog.setCancelable(false);
                dialog.setPositiveButton("Confirm", (dialog1, which) -> {
                    Intent intent = new Intent(qr_update.this, MainMenu.class);
                    startActivity(intent);
                });
                dialog.show();
            }
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        //debug.setText(getIntent().getStringExtra("content"));


    }

    private static class checkQRExists extends AsyncTask<String, Void, Boolean> {
        private final WeakReference<qr_update> weakReference;


        checkQRExists(qr_update context) {
            weakReference = new WeakReference<>(context);
        }

        @Override
        protected Boolean doInBackground(String... params) {
            qr_update activity = weakReference.get();
            String Owner_id = params[0];
            String Equipment_id = params[1];
            String sql = "select * from inventory where E_id =? and owner_id =?";
            PreparedStatement ps = null;
            ResultSet rs = null;

            try {
                ps = activity.con.prepareStatement(sql);
                ps.setString(1, Equipment_id);
                ps.setString(2, Owner_id);
                rs = ps.executeQuery();

                if (rs.next()) {
                    return true;
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
            return false;
        }
    }

    public void update(View view) throws ExecutionException, InterruptedException {
        String division = division_spinner.getSelectedItem().toString();
        String post = post_spinner.getSelectedItem().toString();
        String name = nameText.getText().toString();
        String type = Type_spinner.getSelectedItem().toString();
        String model = model_spinner.getSelectedItem().toString();
        String Serial = (Serial_no_spinner.getSelectedItem() == null) ? "" : Serial_no_spinner.getSelectedItem().toString();

        getOwnersID getOwners2 = new getOwnersID(this);
        getEquipmentID getEquipmentID2 = new getEquipmentID(this);
        String new_ownersId = getOwners2.execute(division, post, name).get();
        String new_equipmentID = getEquipmentID2.execute(type, model, Serial).get();
//      Toast.makeText(qr_delete_and_update.this, String.valueOf(get.getStatus() == AsyncTask.Status.FINISHED), Toast.LENGTH_SHORT).show();
        new updateInfo(this).execute(Old_ownersId, Old_EquipmentId, new_ownersId, new_equipmentID);
    }


    private static class updateInfo extends AsyncTask<String, Void, Void> {
        private final WeakReference<qr_update> weakReference;

        updateInfo(qr_update context) {
            weakReference = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(String... params) {
            qr_update activity = weakReference.get();
            String sql = "update inventory set E_id=?, owner_id=?, QR_code=?, Date=? where E_id=? and owner_id=?";
            PreparedStatement ps = null;

            try {
                ps = activity.con.prepareStatement(sql);
                ps.setString(1, params[3]);
                ps.setString(2, params[2]);


                String division = activity.division_spinner.getSelectedItem().toString();
                String post = activity.post_spinner.getSelectedItem().toString();
                String name = activity.nameText.getText().toString();
                String type = activity.Type_spinner.getSelectedItem().toString();
                String model = activity.model_spinner.getSelectedItem().toString();
                String Serial = (activity.Serial_no_spinner.getSelectedItem() == null) ? "" : activity.Serial_no_spinner.getSelectedItem().toString();

                String QRContent = "Audit Commission \ndivision: " + division + "\n" +
                        "post: " + post + "\n" +
                        "name: " + name + "\n" +
                        "Type: " + type + "\n" +
                        "Model_no: " + model + "\n" +
                        "Serial_no: " + Serial;

                MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
                //AESEncryption aes = new AESEncryption("audit Commission");
                BitMatrix bitMatrix = multiFormatWriter.encode(QRContent, BarcodeFormat.QR_CODE, 500, 500);
                BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
                Bitmap bitmap = barcodeEncoder.createBitmap(bitMatrix);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                byte[] imageInByte = stream.toByteArray();
                ByteArrayInputStream bs = new ByteArrayInputStream(imageInByte);

                ps.setBinaryStream(3, bs);
                ps.setTimestamp(4, new java.sql.Timestamp(new Date().getTime()));
                ps.setString(5, params[1]);
                ps.setString(6, params[0]);
                ps.executeUpdate();
            } catch (Exception throwable) {
                throwable.printStackTrace();
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
            qr_update activity = weakReference.get();
//          activity.debug.append("\n" + "finish update");
            AlertDialog.Builder dialog = new AlertDialog.Builder(activity);
            dialog.setTitle("update successful");
            dialog.setCancelable(false);
            dialog.setPositiveButton("confirm", (dialog1, which) -> {
                Intent intent = new Intent(activity, MainMenu.class);
                activity.startActivity(intent);
            });
            dialog.show();
            super.onPostExecute(unused);
        }
    }

    private static class getEquipmentID extends AsyncTask<String, Void, String> {
        private final WeakReference<qr_update> weakReference;

        getEquipmentID(qr_update context) {
            weakReference = new WeakReference<>(context);
        }

        @Override
        protected String doInBackground(String... params) {
            qr_update activity = weakReference.get();
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
            qr_update activity = weakReference.get();
            activity.debug.append("\n" + "finish getting equipment");
            super.onPostExecute(s);
        }
    }

    private static class getOwnersID extends AsyncTask<String, Void, String> {

        private final WeakReference<qr_update> weakReference;

        getOwnersID(qr_update context) {
            weakReference = new WeakReference<>(context);
        }

        @Override
        protected void onPostExecute(String s) {
            qr_update activity = weakReference.get();
            activity.debug.append("\n" + "finish getting owners");
            super.onPostExecute(s);
        }

        @Override
        protected String doInBackground(String... params) {
            if (isCancelled()) {
                return null;
            } else {
                qr_update activity = weakReference.get();
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

    private static class setUpEquipmentSpinner extends AsyncTask<Void, Void, Void> {

        List<String> typeList = new ArrayList<>();
        private final WeakReference<qr_update> weakReference;

        setUpEquipmentSpinner(qr_update context) {
            weakReference = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            qr_update activity = weakReference.get();
            String sql = "select distinct type from equipment order by type";
            PreparedStatement ps = null;
            ResultSet rs = null;

            try {
                ps = activity.con.prepareStatement(sql);
                rs = ps.executeQuery();
                while (rs.next()) {
                    typeList.add(rs.getString("type"));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                if (ps != null) {
                    try {
                        ps.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }

                if (rs != null) {
                    try {
                        rs.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            qr_update activity = weakReference.get();
            ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, typeList);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            activity.Type_spinner.setAdapter(adapter);
            int position = typeList.indexOf(activity.Old_type.trim());
            activity.Type_spinner.setSelection(position);
            activity.Type_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    new Model(activity).execute();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
            super.onPostExecute(unused);
        }
    }

    private static class Model extends AsyncTask<Void, Void, Void> {
        List<String> model_list = new ArrayList<>();
        private final WeakReference<qr_update> weakReference;

        Model(qr_update context) {
            weakReference = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(Void... params) {
            qr_update activity = weakReference.get();
            String sql = "select distinct model from equipment where type=? order by model";
            PreparedStatement ps = null;
            ResultSet rs = null;

            try {
                ps = activity.con.prepareStatement(sql);
                ps.setString(1, activity.Type_spinner.getSelectedItem().toString());
                rs = ps.executeQuery();

                while (rs.next()) {
                    model_list.add(rs.getString("model"));
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

            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            qr_update activity = weakReference.get();
            ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, model_list);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            activity.model_spinner.setAdapter(adapter);
            int position = model_list.indexOf(activity.Old_model.trim());
            activity.model_spinner.setSelection(position);
            activity.model_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    //String text = parent.getItemAtPosition(position).toString();
                    new Serial(activity).execute();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
            super.onPostExecute(unused);
        }
    }

    private static class Serial extends AsyncTask<Void, Void, Void> {

        List<String> Serial = new ArrayList<>();
        private final WeakReference<qr_update> weakReference;

        Serial(qr_update context) {
            weakReference = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            qr_update activity = weakReference.get();
            String sql = "select distinct Serial from equipment where type=? and model=? " +
                    "and Serial <> '' order by Serial";
            PreparedStatement ps = null;
            ResultSet rs = null;

            try {
                ps = activity.con.prepareStatement(sql);
                ps.setString(1, activity.Type_spinner.getSelectedItem().toString());
                ps.setString(2, activity.model_spinner.getSelectedItem().toString());
                rs = ps.executeQuery();

                while (rs.next()) {
                    Serial.add(rs.getString("Serial"));
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
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            qr_update activity = weakReference.get();
            ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, Serial);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            activity.Serial_no_spinner.setAdapter(adapter);
            if (!activity.Old_Serial.equals("")) {
                int position = Serial.indexOf(activity.Old_Serial.trim());
                activity.Serial_no_spinner.setSelection(position);
            }
            super.onPostExecute(unused);
        }
    }

    private static class DataBaseInitial extends AsyncTask<Void, Void, Void> {
        String Info = "";
        private final WeakReference<qr_update> weakReference;

        DataBaseInitial(qr_update context) {
            weakReference = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            qr_update activity = weakReference.get();
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
            qr_update activity = weakReference.get();
            if (!Info.equals("")) {
                activity.debug.setText(Info);
            }
            activity.debug.append("\n" + activity.getIntent().getStringExtra("content"));

            super.onPostExecute(unused);
        }
    }

    private static class InitialSpinnerOwners extends AsyncTask<Void, Void, Void> {
        List<String> division_list = new ArrayList<>();
        String Info = "";
        private final WeakReference<qr_update> weakReference;

        InitialSpinnerOwners(qr_update context) {
            weakReference = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            qr_update activity = weakReference.get();
            String sql = "select distinct owner_division as division from owners";
            PreparedStatement statement = null;
            ResultSet rs = null;

            try {
                statement = activity.con.prepareStatement(sql);
                rs = statement.executeQuery();

                while (rs.next()) {
                    division_list.add(rs.getString(1));
                }
            } catch (SQLException throwable) {
                throwable.printStackTrace();
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
            qr_update activity = weakReference.get();
            activity.debug.append(Info);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, division_list);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            activity.division_spinner.setAdapter(adapter);
            int position = division_list.indexOf(activity.Old_division.trim());
            activity.division_spinner.setSelection(position);
            activity.division_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    //String text = post_spinner.getSelectedItem().toString();
                    String text = parent.getItemAtPosition(position).toString();
                    //DebugLog.append(text+"\n");
                    //Toast.makeText(generateQRCode.this, text, Toast.LENGTH_SHORT).show();
                    new changeSelectionForPost(activity).execute(text);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    //new qr_delete_and_update.changeSelectionForPost(activity).execute(text);
                }
            });
            super.onPostExecute(unused);
        }
    }

    private static class changeSelectionForPost extends AsyncTask<String, Void, Void> {
        String Info = "";
        List<String> post_list = new ArrayList<>();
        private final WeakReference<qr_update> weakReference;

        changeSelectionForPost(qr_update context) {
            weakReference = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(String... division) {
            qr_update activity = weakReference.get();
            String owner_division = division[0];
            String sql = "select distinct owner_post as post from owners where owner_division = ?";
            PreparedStatement ps = null;
            ResultSet rs = null;

            try {
                ps = activity.con.prepareStatement(sql);
                ps.setString(1, owner_division);
                rs = ps.executeQuery();

                while (rs.next()) {
                    post_list.add(rs.getString(1));
                }
            } catch (SQLException e) {
                Info += e.toString() + "\n";
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
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            qr_update activity = weakReference.get();
            //Toast.makeText(generateQRCode.this, "Selection For Post", Toast.LENGTH_SHORT).show();
            if (!Info.equals("")) {
                activity.debug.append(Info);
            } else {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, post_list);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                activity.post_spinner.setAdapter(adapter);
                int position = post_list.indexOf(activity.Old_post.trim());
                activity.post_spinner.setSelection(position);
                activity.post_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        new ChangeNameText(activity).execute();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });
            }
            super.onPostExecute(unused);
        }
    }

    private static class ChangeNameText extends AsyncTask<Void, Void, Void> {
        String Info = "";
        boolean have_owner = false;
        private final WeakReference<qr_update> weakReference;

        ChangeNameText(qr_update context) {
            weakReference = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            qr_update activity = weakReference.get();
            String sql = "select owner_name as name from owners where owner_division = ? and owner_post=?";

            String owner_division = activity.division_spinner.getSelectedItem().toString();
            String owner_owner = activity.post_spinner.getSelectedItem().toString();

            PreparedStatement ps = null;
            ResultSet rs = null;

            try {
                ps = activity.con.prepareStatement(sql);
                ps.setString(1, owner_division);
                ps.setString(2, owner_owner);
                rs = ps.executeQuery();

                if (rs.next()) {
                    have_owner = true;
                    Info += rs.getString(1);
                } else have_owner = false;
            } catch (SQLException e) {
                Info += e.toString() + "\n";
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
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            qr_update activity = weakReference.get();
            //Toast.makeText(generateQRCode.this, "changeNameText", Toast.LENGTH_LONG).show();
            if (have_owner) {
                activity.nameText.setText(Info);
            } else {
                activity.debug.append(Info + "\n");
            }
            super.onPostExecute(unused);
        }
    }
}