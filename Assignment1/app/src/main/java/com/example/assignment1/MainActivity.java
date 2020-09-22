package com.example.assignment1;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.FFmpeg;
import com.arthenica.mobileffmpeg.FFprobe;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import org.w3c.dom.Text;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Vector;

public class MainActivity extends AppCompatActivity implements SensorEventListener, Serializable {
    DatabaseHelper db;

    Sensor accelerometer;
    private Uri fileUri;
    private VideoView videoView;
    private SensorManager sensorManager;
    static final int REQUEST_VIDEO_CAPTURE = 1;
    private DecimalFormat df = new DecimalFormat("##");
    private float HR;
    private float RR;

    private Vector<Double> accZaxis = new Vector<>();

    private static String TAG = "MainActivity";

    static {
        if (OpenCVLoader.initDebug()) {
            Log.d(TAG, "OpenCV loaded successfully");
        } else {
            Log.d(TAG, "OpenCV not loaded properly");
        }
    }

    private long start;
    private long end;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = new DatabaseHelper(this);
        Button button = (Button) findViewById(R.id.buttonSymptoms);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(), SymptomsActivity.class);
                startActivity(i);
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode==REQUEST_VIDEO_CAPTURE) {
            if(resultCode==RESULT_OK){
                Toast.makeText(getApplicationContext(), "Video saved to:\n" + data.getData(),
                        Toast.LENGTH_LONG).show();
                VideoView videoView = new VideoView(this);
                videoView.setVideoURI(data.getData());
                processHRVideo();
            } else if (resultCode==RESULT_CANCELED){
                Toast.makeText(getApplicationContext(), "Video recording cancelled", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getApplicationContext(), "Failed to record video",
                        Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(getApplicationContext(), "Video recording failed", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        accZaxis.add((double) event.values[2]);
    }

    private void processRR(){
        double bpm;
        int nPeaks;
        int AVG_WINDOW = 2;
        int AROUND_PEAKS = 3;
        long elapsedTime = (this.end - this.start)/1000;

        TextView textRR = (TextView) findViewById(R.id.textRR);

        // This can be further improved with proximity sensor
        Log.d(TAG, Arrays.toString(accZaxis.toArray()));
        nPeaks = doAverageAndCountPeaks(accZaxis, AVG_WINDOW, AROUND_PEAKS);
        nPeaks -= 2; // Remove the beginning and end peaks of getting close to the chest
        bpm = (nPeaks*60.)/elapsedTime;

        textRR.setText("BPM: "+df.format(bpm));
        Toast.makeText(this, "P: "+nPeaks+" BPM: "+bpm, Toast.LENGTH_LONG).show();
        RR = (float) bpm;
    }

    public void getRR(View view){
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        Button startRR = (Button) findViewById(R.id.startRR);
        Button stopRR = (Button) findViewById(R.id.stopRR);

        switch(view.getId()) {
            case R.id.startRR:
                start = System.currentTimeMillis();
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
                startRR.setVisibility(View.GONE);
                stopRR.setVisibility(View.VISIBLE);
                break;

            case R.id.stopRR:
                end = System.currentTimeMillis();
                sensorManager.unregisterListener(this);
                stopRR.setVisibility(View.GONE);
                startRR.setVisibility(View.VISIBLE);
                processRR();
                break;
        }
    }

    public void getHRVideo(View view) {
        File mediaFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
        + "/HeartRate.mp4");

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        fileUri = Uri.fromFile(mediaFile);
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 45);
        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY,1);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);

        boolean isFlashAvailable = getApplicationContext().getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
        if(!isFlashAvailable)
            Toast.makeText(getApplicationContext(), "There is no flash available", Toast.LENGTH_LONG).show();
        startActivityForResult(intent, REQUEST_VIDEO_CAPTURE);
    }

    private void processHRVideo(){
        Mat frame = new Mat();
        TextView textHR = (TextView) findViewById(R.id.textHR);
        String filePathMP4 = Environment.getExternalStorageDirectory().getAbsolutePath() + "/HeartRate.mp4";
        String filePathMJPEG = Environment.getExternalStorageDirectory().getAbsolutePath() + "/HeartRate.mjpeg";
        String filePathAVI = Environment.getExternalStorageDirectory().getAbsolutePath() + "/HeartRate.avi";

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);

        int rc = FFmpeg.execute("-i " + filePathMP4 + " -vcodec mjpeg -y -t 45 " + filePathMJPEG);
        int rc2 = FFmpeg.execute("-i " + filePathMJPEG+" -vcodec copy -y -t 45 " + filePathAVI);

        VideoCapture HRvideo = new VideoCapture(filePathAVI);

        if (!HRvideo.isOpened()) {
            Log.d(TAG, "Error! Video can't be opened!");
            return;
        }

        double avg_HR = 0.0;
        int frame_total_count = (int) HRvideo.get(Videoio.CV_CAP_PROP_FRAME_COUNT);
        double fps = HRvideo.get(Videoio.CV_CAP_PROP_FPS);
        int chunks = (int) (frame_total_count/fps)/5;
        if(chunks==0) chunks = 1;
        int frames_per_chunk = frame_total_count/chunks;

        for (int i=0; i<chunks; i+=1){
            int start_frame = i*frames_per_chunk;
            int end_frame = (start_frame+frames_per_chunk-1);
            Log.d(TAG, "[+] Chunk "+i+"/"+(chunks-1)+" stt: "+start_frame+" end: "+end_frame);

            processVideoThread foo = new processVideoThread(HRvideo, start_frame, frames_per_chunk);
            Thread thread = new Thread(foo);
            thread.start();
            try { thread.join(); } catch (InterruptedException e) { e.printStackTrace(); }
            avg_HR += foo.getAvgHR();
        }

        textHR.setText("HR: "+df.format(avg_HR/chunks));
        Log.d(TAG, "[+]: Final average Heart Rate "+avg_HR/chunks);
        Toast.makeText(this, "[+]: Final average Heart Rate "+avg_HR/chunks, Toast.LENGTH_LONG).show();
        HR = (float) (avg_HR/chunks);
    }

    private int doAverageAndCountPeaks(Vector<Double> noisyVector, int AVG_WINDOW, int AROUND_NVALS) {
        Vector<Double> meanVector = new Vector<>();
        int n_peaks = 0;
        write("/noisyVector.txt", noisyVector.toArray());

        // Compute the running average with a window
        for (int i = 0; i < noisyVector.size() - AVG_WINDOW; i += 1) {
            double running_avg = 0;
            for (int j = i; j < i + AVG_WINDOW; j += 1) {
                running_avg += noisyVector.get(j);
            }
            meanVector.add(running_avg / AVG_WINDOW);
        }

        write("/meanVector.txt", meanVector.toArray());
        //Log.d(TAG, Arrays.toString(meanVector.toArray()));

        for (int i = 0; i < meanVector.size(); i += 1) {
            double cur_value = meanVector.get(i);
            int max_val = i;
            int left_margin = (i - AROUND_NVALS < 0) ? i : AROUND_NVALS;
            int right_margin = (i + AROUND_NVALS > meanVector.size()) ? meanVector.size() - i - 1 : AROUND_NVALS;

            for (int j = -left_margin; j < right_margin; j += 1) {
                double aux_val = meanVector.get(i + j);
                if (aux_val > cur_value) {
                    max_val = i + j;
                    break;
                }
            }
            if (max_val == i) {
                Log.d(TAG, "[+]: peak: " + i + " val: " + cur_value);
                n_peaks += 1;
            }
        }

        return n_peaks;
    }

    public static void write (String filename, Object[] x) {
        filename = Environment.getExternalStorageDirectory().getAbsolutePath()
                + filename;

        try {
        BufferedWriter outputWriter = null;
        outputWriter = new BufferedWriter(new FileWriter(filename));
        for (Object value : x) {
            outputWriter.write(value + "");
            outputWriter.newLine();
        }
        outputWriter.flush();
        outputWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveDataToDB(View view) {

        float[] readingsArray = new float[12];
        float[] arraySymptoms = (float[]) getIntent().getSerializableExtra("arraySymptoms");

        if(arraySymptoms == null){ arraySymptoms = new float[10]; Arrays.fill(arraySymptoms, 0);}

        readingsArray[0] = Float.parseFloat(df.format(HR));
        readingsArray[1] = Float.parseFloat(df.format(RR));
        System.arraycopy(arraySymptoms, 0, readingsArray, 2, arraySymptoms.length);

        db.saveDataToUserReadings("Alberto", readingsArray);

    }

    public class processVideoThread implements Runnable{
        private volatile int fpc;
        private volatile double avgHR;
        private volatile int startFrame;
        private volatile VideoCapture HRvideo;

        processVideoThread(VideoCapture HRvideo, int startFrame, int fpc){
            this.fpc = fpc;
            this.startFrame = startFrame;
            this.HRvideo = HRvideo;
        }

        @Override
        public void run() {
            Vector<Double> mean_Rs = new Vector<>();

            int AVG_WINDOW = 10;
            int AROUND_NVALS = 15;
            Mat frame = new Mat();
            Mat red_channel = Mat.zeros(frame.rows(), frame.cols(), CvType.CV_32F);
            int fc = 0; // Frame count
            int n_peaks;
            double seconds = fpc/HRvideo.get(Videoio.CV_CAP_PROP_FPS);

            HRvideo.set(Videoio.CV_CAP_PROP_POS_FRAMES, startFrame); // GET starting frame
            Log.d(TAG, "[+]: STARTING ON " + HRvideo.get(Videoio.CV_CAP_PROP_POS_FRAMES));

            // Calculate mean red channel values and store them into a vector
            while (fc < fpc){
                if (HRvideo.read(frame)){
                    Core.extractChannel(frame, red_channel, 0);
                    mean_Rs.add(Core.mean(red_channel).val[0]);
                    fc += 1;
                }
            }

            n_peaks = doAverageAndCountPeaks(mean_Rs, AVG_WINDOW, AROUND_NVALS);

            double avg_HR_rpm = ((n_peaks) * 60) / (seconds);
            Log.d(TAG, "Mean RPM this chunk: "+avg_HR_rpm);
            this.avgHR = avg_HR_rpm;
        }

        public double getAvgHR(){
            return avgHR;
        }

    }

}