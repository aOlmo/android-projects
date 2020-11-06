package com.example.assignment1;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.os.Environment;
import android.os.FileUtils;
import android.os.StrictMode;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.security.cert.CRL;
import java.util.Arrays;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static SQLiteDatabase db;
    private static String TAG = "Database";
    public static final String USER_READ_TABLE = "UserReadings";
    public static final String USER_GPS_TABLE = "UserGPSReadings";
    public static final String DATABASE_NAME = "Assignment2.db";
    public static String[] COLS = {"NAME","HR","BR","NAUSEA","HEADACHE","DIARRHEA", "SOAR_THROAT",
            "FEVER","MUSCLE_ACHE","LOSS_SMELL_TASTE","COUGH","SHORTNESS_BREATH","TIRED"};

    public static String[] GPS_COLS = {"NAME", "LATITUDE", "LONGITUDE", "TIMESTAMP"};
    public static String GPS_CREATE_TABLE = "(NAME varchar(255), LATITUDE double, LONGITUDE double, TIMESTAMP timestamp)";

    public DatabaseHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, 1);
        this.db = this.getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String command = "";
        String type_INT = "int,";
        String type_VARCHAR = "varchar(255),";
        String type_FLOAT ="float,";
        this.db = db;

        for (String s : COLS){
            command += " "+s+" ";
            if(s.equals("NAME")){
                command += type_VARCHAR;
            } else {
                command += type_FLOAT;
            }
        }

        if ((command != null) && (command.length() > 0)) {
            command = command.substring(0, command.length() - 1);
        }

        Log.d(TAG, "Command: "+command);
        String final_command = String.format("CREATE TABLE %s (%s)", USER_READ_TABLE, command);
        db.execSQL(final_command);

        // Create GPS table
        db.execSQL(String.format("CREATE TABLE %s %s", USER_GPS_TABLE, GPS_CREATE_TABLE));

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onCreate(db);
    }

    public void saveDataToUserReadings(String name, float[] d){
        String cmd;
        String sd = Arrays.toString(d);
        String strData = String.format("%s", sd.substring(1, sd.length()-1));
        cmd = String.format("INSERT INTO UserReadings(%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)", COLS);
        cmd += String.format(" VALUES ('%s', %s)", name, strData);
        Log.d(TAG, cmd);
        db.execSQL(cmd);
    }

    public void saveGPSData(String name, double lat, double lon){
        Long tsLong = System.currentTimeMillis()/1000;
        String ts = tsLong.toString();
        String cmd;
        cmd = String.format("INSERT INTO UserGPSReadings(%s, %s, %s, %s)", GPS_COLS);
        cmd += String.format(" VALUES ('%s', %s, %s, %s)", name, lat, lon, ts);
        Log.d(TAG, cmd);
        db.execSQL(cmd);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public boolean sendDBToServer(File dbFile){
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        String boundary = Long.toHexString(System.currentTimeMillis());
        String CRLF = "\r\n";
        String charset = "UTF-8";

        try {
            String upload_file_var = "uploaded_file";
            HttpURLConnection httpUrlConnection;
            URL url = new URL("http://192.168.1.32/upload_video.php");
            httpUrlConnection = (HttpURLConnection) url.openConnection();
            httpUrlConnection.setUseCaches(false);
            httpUrlConnection.setDoOutput(true);

            httpUrlConnection.setRequestMethod("POST");
            httpUrlConnection.setRequestProperty("Connection", "Keep-Alive");
            httpUrlConnection.setRequestProperty("Cache-Control", "no-cache");
            httpUrlConnection.setRequestProperty(
                    "Content-Type", "multipart/form-data;boundary=" + boundary);

            DataOutputStream request = new DataOutputStream(httpUrlConnection.getOutputStream());

            request.writeBytes("--" + boundary + CRLF);
            request.writeBytes("Content-Disposition: form-data; name=\"" +
                    upload_file_var + "\";filename=\"" +
                    dbFile.getName() + "\"" + CRLF);
            request.writeBytes(CRLF);

            byte[] fileContent = new byte[0];
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                fileContent = Files.readAllBytes(dbFile.toPath());
            }

            request.write(fileContent);
            request.writeBytes(CRLF);
            request.writeBytes("--"+ boundary + "--" + CRLF);

            int responseCode = httpUrlConnection.getResponseCode();

            BufferedReader br = new BufferedReader(new InputStreamReader((httpUrlConnection.getInputStream())));
            StringBuilder sb = new StringBuilder();
            String out;
            while ((out = br.readLine()) != null) {
                sb.append(out);
            }

            Log.d(TAG, "Al-Message: "+sb.toString());
            Log.d(TAG, "The response code is: "+responseCode);

            if(responseCode == 200 && sb.toString().contains("Uploaded")){
                return true;
            } else {
                return false;
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }
}
