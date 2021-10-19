package com.example.mobileinventory;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;


public class generateQRCode extends AppCompatActivity {

    Connection con = null;
    TextView DebugLog, nameText;
    Spinner division_spinner, post_spinner, Type_spinner, model_spinner;
    Button previewBtn;
    EditText SerialNoTextView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_qrcode);

        DebugLog = findViewById(R.id.Debug);
        division_spinner = findViewById(R.id.division_Spinner);
        post_spinner = findViewById(R.id.Post_spinner);
        nameText = findViewById(R.id.nameText);
        Type_spinner = findViewById(R.id.Type_Spinner);
        model_spinner = findViewById(R.id.modelSpinner);
        SerialNoTextView = findViewById(R.id.SerialNoTextView);
        previewBtn = findViewById(R.id.UpdateBtn);

        new DataBaseInitial(this).execute();
        new InitialSpinnerOwners(this).execute();
        new setUpEquipmentSpinner(this).execute();
    }


    private static class setUpEquipmentSpinner extends AsyncTask<Void, Void, Void> {

        List<String> typeList = new ArrayList<>();
        private final WeakReference<generateQRCode> weakReference;

        setUpEquipmentSpinner(generateQRCode context) {
            weakReference = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            generateQRCode activity = weakReference.get();
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
                try {
                    if (ps != null) ps.close();
                    if (rs != null) rs.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }

            }

            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            generateQRCode activity = weakReference.get();
            ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, typeList);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            activity.Type_spinner.setAdapter(adapter);
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
        private final WeakReference<generateQRCode> weakReference;

        Model(generateQRCode context) {
            weakReference = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(Void... params) {
            generateQRCode activity = weakReference.get();
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
            generateQRCode activity = weakReference.get();
            ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, model_list);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            activity.model_spinner.setAdapter(adapter);
            super.onPostExecute(unused);
        }
    }



    public void preview(View view) {
        Intent intent = new Intent(generateQRCode.this, ViewQRCode.class);
        String division = division_spinner.getSelectedItem().toString();
        String post = post_spinner.getSelectedItem().toString();
        String name = nameText.getText().toString();
        String Type = Type_spinner.getSelectedItem().toString();
        String Model = model_spinner.getSelectedItem().toString();
        String Serial =SerialNoTextView.getText().toString();
//        intent.putExtra("division", division);
//        intent.putExtra("post", post);
//        intent.putExtra("name", name);
//        intent.putExtra("Type", Type);
//        intent.putExtra("Model", Model);
//        intent.putExtra("Serial", Serial);
        try {
            getOwnerID getOwnerID = new getOwnerID(this);
            String owner_id = getOwnerID.execute(division, post, name).get();


            PreInsert insert = new PreInsert(this);
            String insertLastkey = insert.execute(owner_id, Type, Model, Serial).get();

            Log.i("LastKey", insertLastkey);

          intent.putExtra("item_id", insertLastkey);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
       startActivity(intent);
    }

    private static class getOwnerID extends AsyncTask<String, Void, String> {
        private final WeakReference<generateQRCode> weakReference;

        getOwnerID(generateQRCode context) {
            weakReference = new WeakReference<>(context);
        }

        @Override
        protected String doInBackground(String... params) {
            generateQRCode activity = weakReference.get();
            String division = params[0];
            String post = params[1];
            String name = params[2];
            String sql = "select owner_id as id from owners where owner_division = ? and owner_post=? and owner_name=?";
            PreparedStatement ps = null;
            ResultSet rs = null;

            try {
                ps = activity.con.prepareStatement(sql);
                ps.setString(1, division);
                ps.setString(2, post);
                ps.setString(3, name);
                rs = ps.executeQuery();
                if (rs.next()) {
                    return rs.getString("id");
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

    private static class PreInsert extends AsyncTask<String, Void, String> {
        private final WeakReference<generateQRCode> weakReference;

        PreInsert(generateQRCode context) {
            weakReference = new WeakReference<>(context);
        }


        @Override
        protected String doInBackground(String... params) {
            generateQRCode activity = weakReference.get();
            String owner_id = params[0];
            String Type = params[1];
            String Model = params[2];
            String Serial = params[3];

            String sql = "insert into inventory values (null,?,?,?,?,'','',null)";
            PreparedStatement ps = null;

            try {
                ps = activity.con.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, owner_id);
                ps.setString(2, Type);
                ps.setString(3, Model);
                ps.setString(4, Serial);
                ps.executeUpdate();

                ResultSet rs = ps.getGeneratedKeys();
                if(rs.next()){
                   return String.valueOf(rs.getInt(1));
                }


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

    }


    private static class DataBaseInitial extends AsyncTask<Void, Void, Void> {
        String Info = "";
        private final WeakReference<generateQRCode> weakReference;

        DataBaseInitial(generateQRCode context) {
            weakReference = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            generateQRCode activity = weakReference.get();
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
            generateQRCode activity = weakReference.get();
            if (!Info.equals("")) {
                activity.DebugLog.setText(Info);
            }

            super.onPostExecute(unused);
        }
    }

    private static class InitialSpinnerOwners extends AsyncTask<Void, Void, Void> {
        List<String> division_list = new ArrayList<>();
        String Info = "";
        private final WeakReference<generateQRCode> weakReference;

        InitialSpinnerOwners(generateQRCode context) {
            weakReference = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            generateQRCode activity = weakReference.get();
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
            generateQRCode activity = weakReference.get();
            activity.DebugLog.append(Info);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, division_list);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            activity.division_spinner.setAdapter(adapter);

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

                }
            });
            super.onPostExecute(unused);
        }
    }

    private static class changeSelectionForPost extends AsyncTask<String, Void, Void> {
        String Info = "";
        List<String> post_list = new ArrayList<>();
        private final WeakReference<generateQRCode> weakReference;

        changeSelectionForPost(generateQRCode context) {
            weakReference = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(String... division) {
            generateQRCode activity = weakReference.get();
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
            generateQRCode activity = weakReference.get();
            //Toast.makeText(generateQRCode.this, "Selection For Post", Toast.LENGTH_SHORT).show();
            if (!Info.equals("")) {
                activity.DebugLog.append(Info);
            } else {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, post_list);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                activity.post_spinner.setAdapter(adapter);
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
        private final WeakReference<generateQRCode> weakReference;

        ChangeNameText(generateQRCode context) {
            weakReference = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            generateQRCode activity = weakReference.get();
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
            generateQRCode activity = weakReference.get();
            //Toast.makeText(generateQRCode.this, "changeNameText", Toast.LENGTH_LONG).show();
            if (have_owner) {
                activity.nameText.setText(Info);
            } else {
                activity.DebugLog.append(Info + "\n");
            }
            super.onPostExecute(unused);
        }
    }
}