package com.example.mobileinventory;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class tableview extends AppCompatActivity {

    Connection con = null;
    DataBaseInitial initial;
    InitialSpinnerOwners spinnerOwners;
    AllData allData;
    TextView nameText;
    Spinner postSpinner, divisionSpinner, choiceSpinner;
    ConstraintLayout SerialSearchLayout, nameSearchLayout;
    EditText SerialText;
    AllDataBySerial allDataBySerial;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tableview);

        nameText = findViewById(R.id.nameEditText);
        postSpinner = findViewById(R.id.postSpinner);
        divisionSpinner = findViewById(R.id.divisionSpinner);
        choiceSpinner = findViewById(R.id.choiceSpinner);
        SerialSearchLayout = findViewById(R.id.SerialSearchLayout);
        nameSearchLayout = findViewById(R.id.nameSearchLayout);
        SerialText = findViewById(R.id.SerialText);

        initial = new DataBaseInitial(this);
        initial.execute();

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.choice_array, R.layout.spinner_item);
        adapter.setDropDownViewResource(R.layout.spinner_item);
        choiceSpinner.setAdapter(adapter);
        choiceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String option = parent.getItemAtPosition(position).toString();
                if (option.equalsIgnoreCase("Choice from Serial number")) {
                    SerialSearchLayout.setVisibility(View.VISIBLE);
                    nameSearchLayout.setVisibility(View.GONE);
                    spinnerOwners = new InitialSpinnerOwners(tableview.this);
                    spinnerOwners.execute();

                } else if (option.equalsIgnoreCase("Choice from user")) {
                    SerialSearchLayout.setVisibility(View.GONE);
                    nameSearchLayout.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

    }

    public void Search(View view) {
        String option = choiceSpinner.getSelectedItem().toString();
        if (option.equalsIgnoreCase("Choice from user")) {
            Intent intent = new Intent(tableview.this, tableContent.class);
            String owner_post = postSpinner.getSelectedItem().toString();
            String owner_name = nameText.getText().toString();
            String owner_division = divisionSpinner.getSelectedItem().toString();
            intent.putExtra("option","Choice from user");
            intent.putExtra("owner_post", owner_post);
            intent.putExtra("owner_name", owner_name);
            intent.putExtra("owner_division",owner_division);
            startActivity(intent);
//            allData = new AllData(this);
//            allData.execute(owner_post, owner_name, owner_division);
        } else if (option.equalsIgnoreCase("Choice from Serial number")) {
            Intent intent = new Intent(tableview.this, tableContent.class);

            String Serial_No = SerialText.getText().toString();
            intent.putExtra("option","Choice from Serial number");
            intent.putExtra("Serial_No", Serial_No);
            startActivity(intent);
//            allDataBySerial = new AllDataBySerial(this);
//            allDataBySerial.execute(Serial_No);
        }
    }

    private static class AllDataBySerial extends AsyncTask<String, Void, Void> {
        private final WeakReference<tableview> weakReference;
        List<String> All_data = new ArrayList<>();
        List<InputStream> qr_code_list = new ArrayList<>();

        AllDataBySerial(tableview context) {
            weakReference = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(String... params) {
            tableview activity = weakReference.get();
            String Serial = params[0];
            String sql = "select o.owner_post, o.owner_name, o.owner_division, e.Type, e.Model, e.Serial, i.QR_code from inventory i, owners o , equipment e where " +
                    "e.E_id = i.E_id and o.owner_id = i.owner_id and e.Serial like ?";
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
            super.onPostExecute(unused);
        }
    }

    private static class InitialSpinnerOwners extends AsyncTask<Void, Void, Void> {
        List<String> division_list = new ArrayList<>();
        String Info = "";
        private final WeakReference<tableview> weakReference;

        InitialSpinnerOwners(tableview context) {
            weakReference = new WeakReference<>(context);
        }


        @Override
        protected Void doInBackground(Void... voids) {
            tableview activity = weakReference.get();
            String sql = "Select distinct owner_division as division from owners ";
            PreparedStatement ps = null;
            ResultSet rs = null;

            try {
                ps = activity.con.prepareStatement(sql);
                rs = ps.executeQuery();
                while (rs.next()) {
                    division_list.add(rs.getString(1));
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
            tableview activity = weakReference.get();
            ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, R.layout.spinner_item, division_list);
            adapter.setDropDownViewResource(R.layout.spinner_item);
            activity.divisionSpinner.setAdapter(adapter);
            activity.divisionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    String text = parent.getItemAtPosition(position).toString();
                    new changeSelectionForPost(activity).execute(text);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
            super.onPostExecute(unused);
        }
    }

    private static class changeSelectionForPost extends AsyncTask<String, Void, Void> {

        private final WeakReference<tableview> weakReference;
        List<String> post_list = new ArrayList<>();

        changeSelectionForPost(tableview context) {
            weakReference = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(String... params) {
            tableview activity = weakReference.get();
            String division = params[0];
            String sql = "Select distinct owner_post from owners where owner_division = ?";
            PreparedStatement ps = null;
            ResultSet rs = null;

            try {
                ps = activity.con.prepareStatement(sql);
                ps.setString(1, division);
                rs = ps.executeQuery();
                while (rs.next()) {
                    post_list.add(rs.getString(1));
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
            tableview activity = weakReference.get();
            ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, R.layout.spinner_item, post_list);
            adapter.setDropDownViewResource(R.layout.spinner_item);
            activity.postSpinner.setAdapter(adapter);
            activity.postSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    new changeNameText(activity).execute();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
            super.onPostExecute(unused);
        }
    }

    private static class changeNameText extends AsyncTask<Void, Void, Void> {

        private final WeakReference<tableview> weakReference;
        boolean have_owner = false;
        String owner_name;

        changeNameText(tableview context) {
            weakReference = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            tableview activity = weakReference.get();
            String sql = "select owner_name as name from owners where owner_division=? and owner_post=?";
            String owner_division = activity.divisionSpinner.getSelectedItem().toString();
            String owner_post = activity.postSpinner.getSelectedItem().toString();

            PreparedStatement ps = null;
            ResultSet rs = null;

            try {
                ps = activity.con.prepareStatement(sql);
                ps.setString(1, owner_division);
                ps.setString(2, owner_post);
                rs = ps.executeQuery();
                if (rs.next()) {
                    have_owner = true;
                    owner_name = rs.getString(1);
                }
            } catch (SQLException throwable) {
                throwable.printStackTrace();
                have_owner = false;
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
            tableview activity = weakReference.get();
            if (have_owner) {
                activity.nameText.setText(owner_name);
            }//activity.debug.append("\nget name failed");

            super.onPostExecute(unused);
        }
    }

    private static class DataBaseInitial extends AsyncTask<Void, Void, Void> {
        private final WeakReference<tableview> weakReference;
        String Info = "";

        DataBaseInitial(tableview context) {
            weakReference = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            tableview activity = weakReference.get();
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

    private static class AllData extends AsyncTask<String, Void, Void> {
        private final WeakReference<tableview> weakReference;
        List<String> All_data = new ArrayList<>();
        List<InputStream> qr_code_list = new ArrayList<>();

        AllData(tableview context) {
            weakReference = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(String... params) {
            tableview activity = weakReference.get();
            String sql = "select o.owner_post, o.owner_name, o.owner_division, e.Type, e.Model, e.Serial, i.QR_code from inventory i, owners o, equipment e where e.E_id = i.E_id and " +
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
            tableview activity = weakReference.get();

            if (All_data.size() == 0 && qr_code_list.size() == 0) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(activity);
                dialog.setTitle("No Information");
                dialog.setMessage("The Search requirement does not come up with any result");
                dialog.setPositiveButton("Confirm", (dialog1, which) -> dialog1.dismiss());
                dialog.setCancelable(false);
                dialog.show();
            }
            super.onPostExecute(unused);
        }
    }
}