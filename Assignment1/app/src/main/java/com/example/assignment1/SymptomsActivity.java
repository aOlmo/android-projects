package com.example.assignment1;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Arrays;


public class SymptomsActivity extends Activity implements PopupMenu.OnMenuItemClickListener {

    DatabaseHelper db;
    float[] arraySymptoms = new float[symptoms.values().length];
    String[] arraySymptomsNames = {"Nausea", "Headache", "Diarrhea", "Soar Throat", "Fever",
            "Muscle Ache", "Loss of Smell or Taste", "Cough", "Shortness of Breath", "Feeling Tired"};



    enum symptoms {
        NAUSEA,HEADACHE,DIARRHEA,
        SOAR_THROAT,FEVER,MUSCLE_ACHE,
        LOSS_SMELL_TASTE,COUGH,SHORTNESS_BREATH,TIRED
    }

    private int curSymptomID;
    private float curStarRating;
    private boolean changed; // Flag to know if there was any user input

    public SymptomsActivity(){
        Arrays.fill(arraySymptoms, 0);
        changed = false;
    }

    protected void onCreate(Bundle savedInstanceState) {
        db = new DatabaseHelper(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.symptoms_logging_page);

        Button button = (Button) findViewById(R.id.buttonBack);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(i);
            }
        });

        Button buttonSigns = (Button) findViewById(R.id.buttonUploadSymptoms);
        buttonSigns.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPopup(v);
            }
        });

    }

    public void showPopup(View v){
        PopupMenu popup = new PopupMenu(this, v);
        popup.setOnMenuItemClickListener(this);
        popup.inflate(R.menu.popup_menu_symptoms);
        popup.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item){
        TextView textSymptoms = (TextView) findViewById(R.id.TextSymptom);
        textSymptoms.setText(item.getTitle());
        curSymptomID = Arrays.asList(arraySymptomsNames).indexOf(item.getTitle());
        changed = true;
        return false;
    }

    public void changeCurStarRating(View view) {
        RatingBar ratingBar = (RatingBar) findViewById(R.id.ratingBar1);
        changed = true;
        curStarRating = ratingBar.getRating();
    }

    public void saveSymptomsToDB(View view) {
        return;
    }

}

