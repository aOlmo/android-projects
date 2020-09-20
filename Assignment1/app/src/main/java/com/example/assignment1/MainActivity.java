package com.example.assignment1;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Vector;

public class MainActivity extends AppCompatActivity {

    static final int REQUEST_VIDEO_CAPTURE = 1;
    private Uri fileUri;
    private VideoView videoView;

    private static String TAG = "MainActivity";

    static {
        if (OpenCVLoader.initDebug()) {
            Log.d(TAG, "OpenCV loaded successfully");
        } else {
            Log.d(TAG, "OpenCV not loaded properly");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Button button = (Button)findViewById(R.id.buttonUploadSigns);
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
        processHRVideo();
//        startActivityForResult(intent, REQUEST_VIDEO_CAPTURE);
    }

    private void processHRVideo() {
        Mat frame = new Mat();
        String filePathMP4 = Environment.getExternalStorageDirectory().getAbsolutePath() + "/HeartRate.mp4";
        String filePathMJPEG = Environment.getExternalStorageDirectory().getAbsolutePath() + "/HeartRate.mjpeg";
        String filePathAVI = Environment.getExternalStorageDirectory().getAbsolutePath() + "/HeartRate.avi";

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        // TODO: Remove this
//        int rc = FFmpeg.execute("-i " + filePathMP4 + " -vcodec mjpeg -y -t 45 " + filePathMJPEG);
//        int rc2 = FFmpeg.execute("-i " + filePathMJPEG+" -vcodec copy -y -t 45 " + filePathAVI);

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
            avg_HR += getAvgHR(HRvideo, start_frame, frames_per_chunk, frame_total_count);
        }

        Log.d(TAG, "[+]: Final average Heart Rate "+avg_HR/chunks);
        Toast.makeText(this, "[+]: Final average Heart Rate "+avg_HR/chunks, Toast.LENGTH_LONG).show();
    }

    private double getAvgHR(VideoCapture HRvideo, int start_frame, int fpc, int frames_total) {

        Vector<Double> mean_Rs = new Vector<>();
        Vector<Double> avg_vector = new Vector<>();

        double THRESHOLD = 4;
        int AVG_WINDOW = 15;
        Mat frame = new Mat();
        Mat red_channel = Mat.zeros(frame.rows(), frame.cols(), CvType.CV_32F);
        int fc = 0; // Frame count
        double n_peaks = 0.0;
        double seconds = fpc/HRvideo.get(Videoio.CV_CAP_PROP_FPS);

        HRvideo.set(Videoio.CV_CAP_PROP_POS_FRAMES, start_frame); // GET starting frame
        Log.d(TAG, "[+]: STARTING ON " + HRvideo.get(Videoio.CV_CAP_PROP_POS_FRAMES));

        // Calculate mean red channel values and store them into a vector
        while (fc < fpc){
            if (HRvideo.read(frame)){
                Core.extractChannel(frame, red_channel, 0);
                mean_Rs.add(Core.mean(red_channel).val[0]);
                fc += 1;
            }
        }

        write("/mean_Rs.txt", mean_Rs.toArray());

        // Compute the running average with a window
        for(int i=0; i<mean_Rs.size()-AVG_WINDOW; i+=1) {
            double running_avg = 0;
            for(int j=i; j<i+AVG_WINDOW; j+=1){
                running_avg += mean_Rs.get(j);
            }
            avg_vector.add(running_avg/AVG_WINDOW);
            //Log.d(TAG,"Running avg: "+running_avg/AVG_WINDOW);
        }

        Object[] test = avg_vector.toArray();
        write("/avg_HR.txt", avg_vector.toArray());

        Log.d(TAG, Arrays.toString(avg_vector.toArray()));

        int AROUND_NVALS = 15;
        for(int i=0; i<avg_vector.size(); i+=1) {
            double cur_value = avg_vector.get(i);
            int max_val = i;
            int left_margin = (i-AROUND_NVALS < 0) ? i : AROUND_NVALS;
            int right_margin = (i+AROUND_NVALS > avg_vector.size()) ? avg_vector.size()-i-1 : AROUND_NVALS;

            for(int j=-left_margin; j<right_margin; j+=1){
                double aux_val = avg_vector.get(i+j);
                if(aux_val > cur_value){
                    max_val = i+j;
                    break;
                }
            }
            if(max_val == i){
                Log.d(TAG, "[+]: peak: "+i+" val: "+ cur_value);
                n_peaks += 1;
            }
        }

        double avg_HR_rpm = ((n_peaks-1) * 60) / (seconds);
        Log.d(TAG, "Mean RPM this chunk: "+avg_HR_rpm);
        return avg_HR_rpm;
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



}