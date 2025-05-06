package com.example.myapplication;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import org.tensorflow.lite.Interpreter;
import android.content.res.AssetFileDescriptor;
import android.widget.Button;
import android.widget.ImageView;
import android.content.Intent;
import android.provider.MediaStore;
import android.view.View;
import android.graphics.Bitmap;

import android.util.Log;

import java.io.*;
import java.nio.*;
import java.nio.channels.FileChannel;

public class WorkoutActivity extends BottomNavigationActivity {
    private Interpreter tflite;
    Button btnCapture;
    ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout);

        btnCapture = findViewById(R.id.btnCapture);
        imageView = findViewById(R.id.imageView);

        // Setup bottom navigation
        setupBottomNavigation();

        btnCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, 5);
            }
        });
    }

    private MappedByteBuffer loadModelFile(String filename) throws IOException {
        AssetFileDescriptor fileDescriptor = getAssets().openFd(filename);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.getStartOffset(), fileDescriptor.getDeclaredLength());
    }

    private ByteBuffer preprocess(Bitmap bitmap) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(1 * 192 * 192 * 3 * 4);
        buffer.order(ByteOrder.nativeOrder());
        for (int y = 0; y < 192; y++) {
            for (int x = 0; x < 192; x++) {
                int pixel = bitmap.getPixel(x, y);
                buffer.putFloat(((pixel >> 16) & 0xFF));
                buffer.putFloat(((pixel >> 8) & 0xFF));
                buffer.putFloat((pixel & 0xFF));
            }
        }
        buffer.rewind();
        return buffer;
    }

    private void drawKeypoints(Bitmap bitmap, float[][][][] keypoints, float threshold) {
        int height = bitmap.getHeight();
        int width = bitmap.getWidth();

        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.FILL);

        for (int i = 0; i < 17; i++) {
            float y = keypoints[0][0][i][0];
            float x = keypoints[0][0][i][1];
            float confidence = keypoints[0][0][i][2];

            if (confidence > threshold) {
                canvas.drawCircle(x * width, y * height, 6, paint);
            }
        }
    }

    private void squatCorrection(float[][][][] keypoints, float confidenceThreshold) {
        // Get the 17 keypoints: [0][0][i][0] = y, [0][0][i][1] = x, [0][0][i][2] = score
        float[] rightHip = keypoints[0][0][12];
        float[] rightKnee = keypoints[0][0][14];
        float[] rightAnkle = keypoints[0][0][16];

        float[] leftHip = keypoints[0][0][11];
        float[] leftKnee = keypoints[0][0][13];
        float[] leftAnkle = keypoints[0][0][15];

        float[] rightShoulder = keypoints[0][0][6];

        Log.d("Angles", "Keypoints set");

        if (rightHip[2] > confidenceThreshold && rightKnee[2] > confidenceThreshold && rightAnkle[2] > confidenceThreshold) {

            float hx = rightHip[1], hy = rightHip[0], hScore = rightHip[2];
            float kx = rightKnee[1], ky = rightKnee[0], kScore = rightKnee[2];
            float ax = rightAnkle[1], ay = rightAnkle[0], aScore = rightAnkle[2];

            double kh = Math.sqrt(Math.pow(kx - hx, 2) + Math.pow(ky - hy, 2));
            double ka = Math.sqrt(Math.pow(kx - ax, 2) + Math.pow(ky - ay, 2));
            double ha = Math.sqrt(Math.pow(hx - ax, 2) + Math.pow(hy - ay, 2));

            double angle = Math.acos((Math.pow(kh, 2) + Math.pow(ka, 2) - Math.pow(ha, 2)) / (2 * kh * ka));
            double angleDegrees = Math.toDegrees(angle);
            Log.d("Angles", "Right leg angle: " + angleDegrees);
            if (angle >= 100){
                Log.d("Angles", "Tip: Squat a little deeper next time");
            }
            else if (angle <= 70)
            {
                Log.d("Angles", "Tip: You don't need to squat so deep");
            }
            else {
                Log.d("Angles", "Good squat depth!");
            }
        }
        if (leftHip[2] > confidenceThreshold && leftKnee[2] > confidenceThreshold && leftAnkle[2] > confidenceThreshold) {

            float hx = leftHip[1], hy = leftHip[0], hScore = leftHip[2];
            float kx = leftKnee[1], ky = leftKnee[0], kScore = leftKnee[2];
            float ax = leftAnkle[1], ay = leftAnkle[0], aScore = leftAnkle[2];

            double kh = Math.sqrt(Math.pow(kx - hx, 2) + Math.pow(ky - hy, 2));
            double ka = Math.sqrt(Math.pow(kx - ax, 2) + Math.pow(ky - ay, 2));
            double ha = Math.sqrt(Math.pow(hx - ax, 2) + Math.pow(hy - ay, 2));

            double angle = Math.acos((Math.pow(kh, 2) + Math.pow(ka, 2) - Math.pow(ha, 2)) / (2 * kh * ka));
            double angleDegrees = Math.toDegrees(angle);
            Log.d("Angles", "Left leg angle: " + angleDegrees);
            if (angle >= 100){
                Log.d("Angles", "Tip: Squat a little deeper next time");
            }
            else if (angle <= 70)
            {
                Log.d("Angles", "Tip: You don't need to squat so deep");
            }
            else {
                Log.d("Angles", "Good squat depth!");
            }
        }
        if (rightHip[2] > confidenceThreshold && rightKnee[2] > confidenceThreshold && rightShoulder[2] > confidenceThreshold) {

            float hx = rightHip[1], hy = rightHip[0], hScore = rightHip[2];
            float kx = rightKnee[1], ky = rightKnee[0], kScore = rightKnee[2];
            float sx = rightShoulder[1], sy = rightShoulder[0], sScore = rightShoulder[2];

            double kh = Math.sqrt(Math.pow(kx - hx, 2) + Math.pow(ky - hy, 2));
            double ks = Math.sqrt(Math.pow(kx - sx, 2) + Math.pow(ky - sy, 2));
            double hs = Math.sqrt(Math.pow(hx - sx, 2) + Math.pow(hy - sy, 2));

            double angle = Math.acos((Math.pow(kh, 2) + Math.pow(hs, 2) - Math.pow(ks, 2)) / (2 * kh * hs));
            double angleDegrees = Math.toDegrees(angle);
            Log.d("Angles", "Right hip angle: " + angleDegrees);
            if (angle >= 70){
                Log.d("Angles", "Tip: Lean forward a bit");
            }
            else if (angle <= 50)
            {
                Log.d("Angles", "Tip: Lean back a bit");
            }
            else {
                Log.d("Angles", "Good back angle!");
            }
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != 0) {
            Bitmap image = (Bitmap) data.getExtras().get("data");
            imageView.setImageBitmap(image);
            try {

                // Load model
                tflite = new Interpreter(loadModelFile("3.tflite"));
                Log.d("Angles", "Model Loaded");

                // Load image from assets
                Bitmap resized = Bitmap.createScaledBitmap(image, 192, 192, true);

                ByteBuffer input = preprocess(resized);
                float[][][][] output = new float[1][1][17][3];

                // Run inference
                tflite.run(input, output);

                squatCorrection(output, 0.1f);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected int getNavigationMenuItemId(){
        return R.id.navigation_workout;
    };
}