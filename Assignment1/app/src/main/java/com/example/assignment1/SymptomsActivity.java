package com.example.assignment1;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.Arrays;


public class SymptomsActivity extends Activity implements PopupMenu.OnMenuItemClickListener, Serializable {
    private DecimalFormat df = new DecimalFormat("0.00");
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

    public SymptomsActivity(){
        Arrays.fill(arraySymptoms, 0);
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.symptoms_logging_page);

        Button back = (Button) findViewById(R.id.buttonBack);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(), MainActivity.class);
                i.putExtra("arraySymptoms", arraySymptoms);
                startActivity(i);
            }
        });

        RatingBar ratingBar = (RatingBar) findViewById(R.id.ratingBar1);
        ratingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            Button upload = (Button) findViewById(R.id.buttonUploadSymptoms);

            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                TextView textRating = (TextView) findViewById(R.id.textRating);
                curStarRating = ratingBar.getRating();
                textRating.setText(String.format("%.1f", curStarRating));
                upload.setEnabled(true);
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
        Button upload = (Button) findViewById(R.id.buttonUploadSymptoms);
        TextView textSymptoms = (TextView) findViewById(R.id.TextSymptom);

        textSymptoms.setText(item.getTitle());
        curSymptomID = Arrays.asList(arraySymptomsNames).indexOf(item.getTitle());
        upload.setEnabled(true);
        return false;
    }

    public void saveCurSymptom(View view) {
        RatingBar ratingBar = (RatingBar) findViewById(R.id.ratingBar1);
        TextView symptomText = (TextView) findViewById(R.id.TextSymptom);

        arraySymptoms[curSymptomID] = curStarRating;
        Toast.makeText(this, "Saved rating on symptom "+arraySymptomsNames[curSymptomID], Toast.LENGTH_LONG).show();
        ratingBar.setRating((float) 3.5);
        symptomText.setText("Nausea");
        curStarRating = (float) 3.5;
        curSymptomID = 0;

    }

    public float[] getArraySymptoms() {
        return arraySymptoms;
    }
}

