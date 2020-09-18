package com.example.wt;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;


//TODOs:
// #1. Make statistics day/week/month
// #2. Make each entry in the CSV file to be editable from the app
// #3. (Optional) Make it possible to add weights of previous days without the app exploding

public class MainActivity extends AppCompatActivity {
    String FILENAME = "weights.csv";
    String W_AVGS_FNAME = "weights_avgs.csv";

    float intWeight;
    String date, time, comments;
    EditText weightInput, timeInput, dateInput, commentsInput;
    TextView dayTextInput, yestCompTextInput, weekCompTextInput, monthCompTextInput;
    Button saveButton;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(!fileExists(this.W_AVGS_FNAME)){createFile(this.W_AVGS_FNAME);}
        if(!fileExists(this.FILENAME)){createFile(this.FILENAME);}

        weightInput = (EditText) findViewById(R.id.weightInput);
        timeInput = (EditText) findViewById(R.id.timeInput);
        dateInput = (EditText) findViewById(R.id.dateInput);
        commentsInput = (EditText) findViewById(R.id.commentsInput);

        dayTextInput = (TextView) findViewById(R.id.dayAvgText);
        yestCompTextInput = (TextView) findViewById(R.id.yestCompText);
        weekCompTextInput = (TextView) findViewById(R.id.weekCompText);
        monthCompTextInput = (TextView) findViewById(R.id.monthCompText);

        saveButton = (Button) findViewById(R.id.saveButton);

        weightInput.setHint(getLastInputWeight());
        dateInput.setText(getCurrentDate("date"));
        timeInput.setText(getCurrentDate("time"));

        computeAndStoreAverages();
        if(getFileLines(W_AVGS_FNAME) > 0) {
            String today = getCurrentDate("date");
            dayTextInput.setText("Today: "+String.format("%.2f", getDayAvg(today)));
            yestCompTextInput.setText("1 day: "+String.format("%.2f", getWeightDiff(1)));
            weekCompTextInput.setText("7 days: "+String.format("%.2f", getWeightDiff(7)));
            monthCompTextInput.setText("30 days: "+String.format("%.2f", getWeightDiff(30)));
        }

        saveButton.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v){
                ContextWrapper contextWrapper = new ContextWrapper(getApplicationContext());
                File directory = contextWrapper.getDir(getFilesDir().getName(), Context.MODE_PRIVATE);

                String strWeight = weightInput.getText().toString();
                if(strWeight.equals("")){showToast("Please input weight value"); return;}

                intWeight = Float.parseFloat(strWeight);
                date = dateInput.getText().toString();
                time = timeInput.getText().toString();
                comments = commentsInput.getText().toString();

                if(comments.equals("")){comments = "No comments";}

                try {
                    File file = new File(directory, FILENAME);
                    if (!file.exists()) {
                        file.createNewFile(); showToast("File created");
                    }

                    FileWriter fw =  new FileWriter(file.getAbsoluteFile(), true);
                    BufferedWriter bw = new BufferedWriter(fw);

                    bw.write(intWeight+","+date+","+time+","+comments);
                    bw.newLine();
                    bw.close();

                    weightInput.getText().clear();
                    weightInput.setHint(getLastInputWeight());
                    dateInput.setText(getCurrentDate("date"));
                    timeInput.setText(getCurrentDate("time"));

                } catch (IOException e) {
                    e.printStackTrace();
                }

                showToast("Saved ! Entry #"+getFileLines(FILENAME));

                computeAndStoreAverages();
                String today = getCurrentDate("date");
                dayTextInput.setText("Today: "+getDayAvg(today));
                yestCompTextInput.setText("1 day: "+getWeightDiff(1));
                weekCompTextInput.setText("7 days: "+getWeightDiff(7));
                monthCompTextInput.setText("30 days: "+getWeightDiff(30));
            }
        });
    }

    protected void onResume(){
        super.onResume();
        dateInput.setText(getCurrentDate("date"));
        timeInput.setText(getCurrentDate("time"));
    }

    // ================================= HELPERS =============================== //
    public void appendNewEntryTo(String entry, String fname){
        ContextWrapper contextWrapper = new ContextWrapper(getApplicationContext());
        File directory = contextWrapper.getDir(getFilesDir().getName(), Context.MODE_PRIVATE);

        if(!fileExists(fname)){return;}

        FileWriter fw = null;
        try {
            fw = new FileWriter(directory+"/"+fname, true);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(entry);
            bw.newLine();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createFile(String fname){
        ContextWrapper contextWrapper = new ContextWrapper(getApplicationContext());
        File directory = contextWrapper.getDir(getFilesDir().getName(), Context.MODE_PRIVATE);
        File file = new File(directory, fname);
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean fileExists(String fname){
        ContextWrapper contextWrapper = new ContextWrapper(getApplicationContext());
        File directory = contextWrapper.getDir(getFilesDir().getName(), Context.MODE_PRIVATE);

        File file = new File(directory, fname);
        return file.exists();
    }

    public void removeFile(String fname){
        ContextWrapper contextWrapper = new ContextWrapper(getApplicationContext());
        File directory = contextWrapper.getDir(getFilesDir().getName(), Context.MODE_PRIVATE);

        File file = new File(directory, fname);
        file.delete();
    }

    // TODO: This is assuming that each entry is ordered by date
    public void computeAndStoreAverages(){
        ArrayList<String> weightCSV = readCSVFile(this.FILENAME);
        String cur_date, prev_date="";
        Float avg = 0f;
        int count_days = 0;
        String new_entry = "";
        Float sum_weights = 0f;

        // Remove previous avg file
        removeFile(this.W_AVGS_FNAME);
        createFile(this.W_AVGS_FNAME);

        // WEIGHT, DATE, TIME, COMMENT
        String[] aux;
        Integer n_entries = getFileLines(this.FILENAME);
        for(String entry : weightCSV){
            n_entries--;
            aux = entry.split(",");
            cur_date = aux[1];
            if(prev_date.equals(cur_date)){
                sum_weights += Float.parseFloat(aux[0]);
                count_days++;
            } else {
                // Save current stats into file after changing to another date
                if (count_days != 0) {
                    avg = sum_weights/count_days;
                    new_entry = String.format("%.2f",avg)+","+count_days+","+sum_weights+","+prev_date;
                    appendNewEntryTo(new_entry, this.W_AVGS_FNAME);
                }
                sum_weights = Float.parseFloat(aux[0]);
                count_days = 1;
            }
            if (n_entries == 1){
                avg = sum_weights/count_days;
                new_entry = String.format("%.2f",avg)+","+count_days+","+sum_weights+","+cur_date;
                appendNewEntryTo(new_entry, this.W_AVGS_FNAME);
            }
            prev_date = cur_date;
        }
    }

    public ArrayList<String> readCSVFile(String fname){
        ContextWrapper contextWrapper = new ContextWrapper(getApplicationContext());
        File directory = contextWrapper.getDir(getFilesDir().getName(), Context.MODE_PRIVATE);
        ArrayList<String> ret = new ArrayList<String>();
        String line;

        try {
            BufferedReader br = new BufferedReader(new FileReader(directory+"/"+fname));
            while ((line = br.readLine()) != null){
                ret.add(line);
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return ret;
    }

    public Float getDayAvg(String date_of_day){
        SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");
        ArrayList<String> fileEntries;
        String[] aux;
        Float dayAvg = -1f;

        String today = getCurrentDate("date");
        fileEntries = readCSVFile(this.W_AVGS_FNAME);

        // WEIGHT, DATE, TIME, COMMENTS
        String cur_d;
        Float cur_w;
        for(String entry : fileEntries){
            aux = entry.split(",");
            cur_d = aux[3];
            if (cur_d.equals(date_of_day)) {
                dayAvg = Float.parseFloat(aux[0]);
//                showToast("Average of " + date_of_day + ": " + Float.toString(dayAvg) + "kg.");
            }
        }

        return dayAvg;
    }

    public Float getWeightDiff(int days){
        SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");
        Integer aux = getFileLines(this.W_AVGS_FNAME)-days;
        if (aux < 0) return 0f;

        Calendar c = Calendar.getInstance(); // starts with today's date and time
        c.add(Calendar.DAY_OF_YEAR, -days);
        String dateUntil = df.format(c.getTime());
        String today = getCurrentDate("date");

        Float todaysAvg = getDayAvg(today);
        Float dayAvg = getDayAvg(dateUntil);

        if(todaysAvg < 0 || dayAvg < 0) return 0f;

        Float t = todaysAvg - dayAvg;
        showToast("RESTA: "+t);

        return t;
    }

    public String getCurrentDate(String type) {
        Calendar c = Calendar.getInstance();
        System.out.println("Current time => " + c.getTime());
        String formattedDate = "";

        if(type.equals("date")) {
            SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");
             formattedDate = df.format(c.getTime());
        } else if(type.equals("time")){
            SimpleDateFormat df = new SimpleDateFormat("HH:mm");
            formattedDate = df.format(c.getTime());
        }

        return formattedDate;
    }

    public String getLastInputWeight(){
        ContextWrapper contextWrapper = new ContextWrapper(getApplicationContext());
        File directory = contextWrapper.getDir(getFilesDir().getName(), Context.MODE_PRIVATE);

        File file = new File(directory, FILENAME);
        if (!file.exists()) {return "0";}

        FileReader fr = null;
        try {
            fr = new FileReader(file.getAbsoluteFile());

            BufferedReader br = new BufferedReader(fr);
            String last, line;
            last = "";
            while ((line = br.readLine()) != null) {
                last = line;
            }

            return last.split(",")[0] +"kg\t|\t" + last.split(",")[1];

        } catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }

    // TODO: Returns 1 when the file does not exist in fact
    public Integer getFileLines(String fname){
        ContextWrapper contextWrapper = new ContextWrapper(getApplicationContext());
        File directory = contextWrapper.getDir(getFilesDir().getName(), Context.MODE_PRIVATE);
        int c=1;

        File file = new File(directory, fname);
        // if file doesnt exists, then create it
        if (!file.exists()) {return 1;}

        FileReader fr = null;
        try {
            fr = new FileReader(file.getAbsoluteFile());

            BufferedReader br = new BufferedReader(fr);
            String line;
            while ((line = br.readLine()) != null) {
                c += 1;
            }

        } catch (Exception e){
            e.printStackTrace();
        }

        return c;
    }

    private void showToast(String text){
        Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
    }

}