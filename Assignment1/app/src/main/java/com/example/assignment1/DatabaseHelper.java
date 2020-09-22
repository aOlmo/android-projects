package com.example.assignment1;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.util.Arrays;

public class DatabaseHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "Assignment1.db";
    public static final String TABLE_NAME = "UserReadings";
    public static String[] COLS = {"NAME","HR","BR","NAUSEA","HEADACHE","DIARRHEA",
            "SOAR_THROAT","FEVER","MUSCLE_ACHE","LOSS_SMELL_TASTE","COUGH","SHORTNESS_BREATH","TIRED"};
    private static String TAG = "Database";
    private static SQLiteDatabase db;


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
        String final_command = String.format("CREATE TABLE %s (%s)", TABLE_NAME, command);
        db.execSQL(final_command);
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

}
