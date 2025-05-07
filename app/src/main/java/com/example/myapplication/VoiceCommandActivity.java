package com.example.myapplication;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Base activity that handles voice commands for all activities in the app
 */
public abstract class VoiceCommandActivity extends AppCompatActivity {
    private static final String TAG = "VoiceCommandActivity";
    private static final int PERMISSION_REQUEST_CODE = 200;

    // Voice recording variables
    private MediaRecorder mediaRecorder;
    private String audioFilePath;
    private boolean isRecording = false;
    private File audioFile;

    // Volume button control variables
    private static final long DOUBLE_TAP_TIMEOUT = 500; // milliseconds
    private long lastVolumeButtonTime = 0;
    private boolean isPendingDoubleTap = false;
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean isVolumeButtonHeld = false;
    private static final long LONG_PRESS_DURATION = 800; // milliseconds for long press
    private Runnable longPressRunnable;

    // Voice command maps - key is command phrase, value is command ID
    private Map<String, String> globalCommands = new HashMap<>();
    private Map<String, String> activitySpecificCommands = new HashMap<>();

    // Interface to be implemented by specific activities
    public interface VoiceCommandListener {
        void onVoiceCommand(String commandId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            checkPermissions();
            setupAudioFilePath();
            setupLongPressHandler();
            setupDefaultCommands();

            // Let specific activities set up their commands
            setupActivityCommands();

            // Announce app is ready for voice commands
            announceAccessibility("App ready. Press volume button to record voice commands.");
        } catch (Exception e) {
            Log.e(TAG, "Error during onCreate", e);
            Toast.makeText(this, "Failed to initialize voice commands: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Abstract method to be implemented by specific activities to add their commands
     */
    protected abstract void setupActivityCommands();

    /**
     * Method for activities to register their voice commands
     * @param command The voice command phrase (lowercase)
     * @param commandId An identifier for the command action
     */
    protected void registerCommand(String command, String commandId) {
        activitySpecificCommands.put(command.toLowerCase(Locale.US), commandId);
    }

    private void setupDefaultCommands() {
        // Common navigation commands across all activities
        globalCommands.put("go home", "action_home");
        globalCommands.put("main screen", "action_home");
        globalCommands.put("diet screen", "action_diet");
        globalCommands.put("nutrition", "action_diet");
        globalCommands.put("workout screen", "action_workout");
        globalCommands.put("exercise", "action_workout");
        globalCommands.put("profile", "action_profile");
        globalCommands.put("settings", "action_settings");
        globalCommands.put("next screen", "action_next");
        globalCommands.put("previous screen", "action_previous");
    }

    private void setupLongPressHandler() {
        longPressRunnable = () -> {
            if (isVolumeButtonHeld) {
                isVolumeButtonHeld = false;
                handleRecordingToggle();
                announceAccessibility(isRecording ? "Recording started" : "Recording stopped");
            }
        };
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    PERMISSION_REQUEST_CODE);
        }
    }

    private void setupAudioFilePath() {
        try {
            File filesDir = getFilesDir();
            if (!filesDir.exists() && !filesDir.mkdirs()) {
                throw new IOException("Failed to create directory for audio files");
            }

            audioFilePath = new File(filesDir, "VoiceCommand.m4a").getAbsolutePath();
            audioFile = new File(audioFilePath);
        } catch (Exception e) {
            Log.e(TAG, "Error setting up audio file path", e);
            Toast.makeText(this, "Failed to set up recording storage",
                    Toast.LENGTH_LONG).show();
            throw new RuntimeException("Failed to set up audio storage", e);
        }
    }

    // Override key event to handle volume buttons
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP)) {
            // Handle volume button press

            // Method 1: Double tap detection
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastVolumeButtonTime < DOUBLE_TAP_TIMEOUT) {
                // Double tap detected
                isPendingDoubleTap = false;
                handler.removeCallbacksAndMessages(null);
                handleRecordingToggle();
                announceAccessibility(isRecording ? "Recording started" : "Recording stopped");
                return true;
            } else {
                // First tap
                isPendingDoubleTap = true;
                lastVolumeButtonTime = currentTime;

                // Set a timer to check if this is just a single tap
                handler.postDelayed(() -> {
                    if (isPendingDoubleTap) {
                        isPendingDoubleTap = false;
                        // Single tap actions (if any)
                    }
                }, DOUBLE_TAP_TIMEOUT);
            }

            // Method 2: Long press detection
            isVolumeButtonHeld = true;
            handler.postDelayed(longPressRunnable, LONG_PRESS_DURATION);

            return true; // Consume the event
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP)) {
            // Cancel long press detection
            handler.removeCallbacks(longPressRunnable);
            isVolumeButtonHeld = false;
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    protected void announceAccessibility(String message) {
        // Show a toast but also could integrate with TalkBack
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void handleRecordingToggle() {
        try {
            if (!isRecording) {
                startRecording();
            } else {
                stopRecording();
            }
            isRecording = !isRecording;
        } catch (Exception e) {
            Log.e(TAG, "Error with recording", e);
            Toast.makeText(this, "Recording error: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            // Reset recording state if error occurs
            isRecording = false;
        }
    }

    private void startRecording() {
        try {
            // Clean up any existing recorder
            if (mediaRecorder != null) {
                try {
                    mediaRecorder.release();
                } catch (Exception e) {
                    Log.w(TAG, "Error releasing previous recorder", e);
                }
            }

            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setOutputFile(audioFilePath);

            mediaRecorder.prepare();
            mediaRecorder.start();
            announceAccessibility("Recording started");
        } catch (IOException e) {
            Log.e(TAG, "Failed to prepare media recorder", e);
            Toast.makeText(this, "Recording failed: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            resetRecorder();
            throw new RuntimeException("Recording failed", e);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error during recording", e);
            Toast.makeText(this, "Recording error: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            resetRecorder();
            throw new RuntimeException("Recording failed", e);
        }
    }

    private void stopRecording() {
        if (mediaRecorder == null) {
            Log.w(TAG, "Attempted to stop null media recorder");
            return;
        }

        try {
            mediaRecorder.stop();
            announceAccessibility("Recording saved. Processing audio...");

            if (audioFile.exists() && audioFile.length() > 0) {
                sendAudioToWhisper(audioFile);
            } else {
                Log.e(TAG, "Audio file is empty or doesn't exist");
                Toast.makeText(this, "Recording failed: Empty file",
                        Toast.LENGTH_SHORT).show();
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "Error stopping recording", e);
            Toast.makeText(this, "Error stopping recording: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        } finally {
            resetRecorder();
        }
    }

    private void resetRecorder() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.release();
            } catch (Exception e) {
                Log.w(TAG, "Error releasing media recorder", e);
            } finally {
                mediaRecorder = null;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission denied. Cannot record audio.",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void sendAudioToWhisper(File audioFile) {
        // Validate audio file
        if (!audioFile.exists()) {
            Log.e(TAG, "Audio file doesn't exist: " + audioFile.getAbsolutePath());
            Toast.makeText(this, "Audio file doesn't exist", Toast.LENGTH_SHORT).show();
            return;
        }

        if (audioFile.length() == 0) {
            Log.e(TAG, "Audio file is empty: " + audioFile.getAbsolutePath());
            Toast.makeText(this, "Audio file is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Configure OkHttp client with timeout
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();

            // Use the correct MIME type for m4a files
            MediaType mediaType = MediaType.parse("audio/m4a");

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("model", "whisper-1")
                    .addFormDataPart("file", audioFile.getName(),
                            RequestBody.create(audioFile, mediaType))
                    .build();

            // Use your actual API key (store it securely!)
            String apiKey = BuildConfig.OPENAI_API_KEY;

            Request request = new Request.Builder()
                    .url("https://api.openai.com/v1/audio/transcriptions")
                    .post(requestBody)
                    .addHeader("Authorization", apiKey.trim())
                    .build();

            // Show loading indicator
            runOnUiThread(() -> Toast.makeText(this,
                    "Processing audio...",
                    Toast.LENGTH_SHORT).show());

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "API call failed", e);
                    String errorMessage = "API call failed";

                    if (e instanceof SocketTimeoutException) {
                        errorMessage = "Connection timed out. Please try again.";
                    } else {
                        errorMessage += ": " + e.getMessage();
                    }

                    final String finalErrorMessage = errorMessage;
                    runOnUiThread(() -> {
                        Toast.makeText(VoiceCommandActivity.this,
                                finalErrorMessage, Toast.LENGTH_LONG).show();
                        announceAccessibility("Error: " + finalErrorMessage);
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    try {
                        if (!response.isSuccessful()) {
                            handleUnsuccessfulResponse(response);
                            return;
                        }

                        if (response.body() == null) {
                            runOnUiThread(() -> {
                                Toast.makeText(VoiceCommandActivity.this,
                                        "Empty response from server", Toast.LENGTH_LONG).show();
                                announceAccessibility("Error: Empty response from server");
                            });
                            return;
                        }

                        String responseBody = response.body().string();
                        handleSuccessfulResponse(responseBody);

                    } catch (Exception e) {
                        Log.e(TAG, "Error processing response", e);
                        runOnUiThread(() -> {
                            Toast.makeText(VoiceCommandActivity.this,
                                    "Error processing response: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                            announceAccessibility("Error processing response");
                        });
                    } finally {
                        response.close();
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error sending audio to Whisper API", e);
            Toast.makeText(this, "Failed to send audio: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            announceAccessibility("Failed to send audio");
        }
    }

    private void handleUnsuccessfulResponse(Response response) {
        try {
            String errorBody = response.body() != null ? response.body().string() : "No error details";
            Log.e(TAG, "Error response: " + errorBody);

            String errorMessage = "Transcription failed. Status: " + response.code();

            // Try to parse error message from response
            try {
                JSONObject errorJson = new JSONObject(errorBody);
                if (errorJson.has("error")) {
                    JSONObject error = errorJson.getJSONObject("error");
                    if (error.has("message")) {
                        errorMessage = error.getString("message");
                    }
                }
            } catch (JSONException e) {
                Log.w(TAG, "Failed to parse error JSON", e);
            }

            final String finalErrorMessage = errorMessage;
            runOnUiThread(() -> {
                Toast.makeText(VoiceCommandActivity.this,
                        finalErrorMessage, Toast.LENGTH_LONG).show();
                announceAccessibility("Error: " + finalErrorMessage);
            });

        } catch (Exception e) {
            Log.e(TAG, "Error handling unsuccessful response", e);
            runOnUiThread(() -> {
                Toast.makeText(VoiceCommandActivity.this,
                        "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                announceAccessibility("Error occurred");
            });
        }
    }

    private void handleSuccessfulResponse(String responseBody) {
        try {
            JSONObject json = new JSONObject(responseBody);
            String transcript = json.getString("text").trim().toLowerCase(Locale.US);

            Log.d(TAG, "Transcript: " + transcript);

            // Process the voice command
            String commandId = processVoiceCommand(transcript);

            runOnUiThread(() -> {
                // Display the recognized text
                String message = "Transcribed: " + transcript;
                Toast.makeText(VoiceCommandActivity.this, message, Toast.LENGTH_LONG).show();

                // Handle the command if found
                if (commandId != null) {
                    announceAccessibility("Command recognized: " + commandId);
                    executeCommand(commandId);
                } else {
                    announceAccessibility("No command recognized");
                }
            });

        } catch (JSONException e) {
            Log.e(TAG, "JSON parsing error", e);
            runOnUiThread(() -> {
                Toast.makeText(VoiceCommandActivity.this,
                        "Failed to parse response: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
                announceAccessibility("Failed to understand audio");
            });
        } catch (Exception e) {
            Log.e(TAG, "Error processing transcript", e);
            runOnUiThread(() -> {
                Toast.makeText(VoiceCommandActivity.this,
                        "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                announceAccessibility("Error processing transcript");
            });
        }
    }

    /**
     * Process voice command by matching against registered commands
     * @param transcript The transcript text to check for commands
     * @return Command ID if matched, null otherwise
     */
    private String processVoiceCommand(String transcript) {
        // First check activity-specific commands
        for (Map.Entry<String, String> entry : activitySpecificCommands.entrySet()) {
            if (transcript.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        // Then check global commands
        for (Map.Entry<String, String> entry : globalCommands.entrySet()) {
            if (transcript.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        return null; // No match found
    }

    /**
     * Execute the recognized command
     * @param commandId The ID of the command to execute
     */
    private void executeCommand(String commandId) {
        if (this instanceof VoiceCommandListener) {
            ((VoiceCommandListener) this).onVoiceCommand(commandId);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Make sure to release resources when activity stops
        if (isRecording) {
            try{
                stopRecording();
            } catch (Exception e) {
                Log.e(TAG, "Error stopping recording on app stop", e);
            }
            isRecording = false;
        }

        // Remove any pending callbacks
        handler.removeCallbacksAndMessages(null);
    }
}