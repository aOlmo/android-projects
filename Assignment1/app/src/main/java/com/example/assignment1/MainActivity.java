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
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Paths;

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

        process_HR_video();
        // TODO: Uncomment this!
//        startActivityForResult(intent, REQUEST_VIDEO_CAPTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode==REQUEST_VIDEO_CAPTURE) {
            if(resultCode==RESULT_OK){
                Toast.makeText(getApplicationContext(), "Video saved to:\n" + data.getData(),
                        Toast.LENGTH_LONG).show();
                VideoView videoView = new VideoView(this);
                videoView.setVideoURI(data.getData());

                process_HR_video();

                //AlertDialog.Builder builder = new AlertDialog.Builder(this);
                //builder.setView(videoView).show();
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

    private void process_HR_video() {
        Mat frame = new Mat();
        String filePathMP4 = Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/HeartRate.mp4";

        String filePathMJPEG = Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/HeartRate.mjpeg";

        String filePathAVI = Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/HeartRate.avi";

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);

        //TODO: Uncomment these!
//        int rc = FFmpeg.execute("-i " + filePathMP4 + " -vcodec mjpeg -y -t 45 " + filePathMJPEG);
//        int rc2 = FFmpeg.execute("-i " + filePathMJPEG+" -vcodec copy -y -t 45 " + filePathAVI);

        VideoCapture HRvideo = new VideoCapture(filePathAVI);

        if (!HRvideo.isOpened()) {
            Log.d(TAG, "Error! Video can't be opened!");
            return;
        }

        int chunks = 9;
        int frame_total_count = (int) HRvideo.get(Videoio.CV_CAP_PROP_FRAME_COUNT);
        int fpc = frame_total_count/chunks;
        int frame_count = 0;

//        int frame_no = (int) HRvideo.get(Videoio.CV_CAP_PROP_POS_FRAMES, percent); // GET starting frame

        for (int i=0; i<9; i+=1){
            int start_frame = i*fpc;
            int end_frame = (start_frame+fpc-1);
            Log.d(TAG, "[+] Chunk "+i+" s: "+start_frame+" e: "+end_frame);
//            get_chunk_readings_vector(HRvideo, start_frame, end_frame);
        }

        return;

//        while (true){
//            if(HRvideo.read(frame)){
//
//
//                Mat red_channel = Mat.zeros(frame.rows(), frame.cols(), CvType.CV_32F);
//                Core.extractChannel(frame, red_channel, 0);
//                double mu = Core.mean(red_channel).val[0];
//                // Get all mean frame values
//                // Do zero crossing extraction
//                frame_count += 1;
//            } if (frame.empty()){
//                break;
//            }
//        }
//        Log.d(TAG, "[+]: Frame count " + frame_total_count);
    }


}