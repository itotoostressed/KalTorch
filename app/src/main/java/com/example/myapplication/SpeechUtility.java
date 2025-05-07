package com.example.myapplication;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Utility class for text-to-speech functionality using OpenAI's API
 */
public class SpeechUtility {
    private static final String TAG = "SpeechUtility";
    private static final String TTS_MODEL = "gpt-4o-mini-tts";
    private static final String TTS_VOICE = "nova"; // OpenAI voices: alloy, echo, fable, onyx, nova, shimmer

    private Context context;
    private MediaPlayer mediaPlayer;
    private File audioFile;
    private String apiKey;

    /**
     * Create a new SpeechUtility instance
     * @param context Application context
     * @param apiKey OpenAI API key
     */
    public SpeechUtility(Context context, String apiKey) {
        this.context = context;
        this.apiKey = apiKey;
    }

    /**
     * Speak the provided text using OpenAI's TTS API
     * @param textToSpeak The text to be spoken
     */
    public void speak(String textToSpeak) {
        if (textToSpeak == null || textToSpeak.trim().isEmpty()) {
            Log.e(TAG, "Cannot generate speech: text is empty");
            return;
        }

        try {
            // Create temporary file for storing audio
            audioFile = File.createTempFile("tts_audio", ".mp3", context.getCacheDir());

            // Configure OkHttp client with timeout
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();

            // Create JSON request body
            JSONObject requestJson = new JSONObject();
            requestJson.put("model", TTS_MODEL);
            requestJson.put("input", textToSpeak);
            requestJson.put("voice", TTS_VOICE);

            RequestBody requestBody = RequestBody.create(
                    requestJson.toString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url("https://api.openai.com/v1/audio/speech")
                    .post(requestBody)
                    .addHeader("Authorization", apiKey.trim())
                    .addHeader("Content-Type", "application/json")
                    .build();

            // Show loading indicator if context is an activity
            if (context instanceof AppCompatActivity) {
                ((AppCompatActivity) context).runOnUiThread(() ->
                        Toast.makeText(context, "Generating speech...", Toast.LENGTH_SHORT).show()
                );
            }

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "TTS API call failed", e);
                    String errorMessage = "Speech generation failed: " + e.getMessage();

                    if (context instanceof AppCompatActivity) {
                        ((AppCompatActivity) context).runOnUiThread(() ->
                                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                        );
                    }
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    try {
                        if (!response.isSuccessful()) {
                            handleUnsuccessfulResponse(response);
                            return;
                        }

                        ResponseBody responseBody = response.body();
                        if (responseBody == null) {
                            if (context instanceof AppCompatActivity) {
                                ((AppCompatActivity) context).runOnUiThread(() ->
                                        Toast.makeText(context, "Empty response from server", Toast.LENGTH_LONG).show()
                                );
                            }
                            return;
                        }

                        // Write audio data to file
                        try (FileOutputStream fos = new FileOutputStream(audioFile)) {
                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            while ((bytesRead = responseBody.byteStream().read(buffer)) != -1) {
                                fos.write(buffer, 0, bytesRead);
                            }
                            fos.flush();
                        }

                        // Play the audio file
                        playAudio(audioFile);

                    } catch (Exception e) {
                        Log.e(TAG, "Error processing TTS response", e);
                        if (context instanceof AppCompatActivity) {
                            ((AppCompatActivity) context).runOnUiThread(() ->
                                    Toast.makeText(context, "Error processing speech: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show()
                            );
                        }
                    } finally {
                        response.close();
                    }
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "Error creating JSON request", e);
            Toast.makeText(context, "Error preparing speech request", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e(TAG, "Error creating temp file", e);
            Toast.makeText(context, "Error creating audio file", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error in generateSpeech", e);
            Toast.makeText(context, "Speech generation error: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void handleUnsuccessfulResponse(Response response) {
        try {
            String errorBody = response.body() != null ? response.body().string() : "No error details";
            Log.e(TAG, "TTS API error response: " + errorBody);

            String errorMessage = "Speech generation failed. Status: " + response.code();

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
            if (context instanceof AppCompatActivity) {
                ((AppCompatActivity) context).runOnUiThread(() ->
                        Toast.makeText(context, finalErrorMessage, Toast.LENGTH_LONG).show()
                );
            }

        } catch (Exception e) {
            Log.e(TAG, "Error handling unsuccessful response", e);
            if (context instanceof AppCompatActivity) {
                ((AppCompatActivity) context).runOnUiThread(() ->
                        Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        }
    }

    private void playAudio(File audioFile) {
        try {
            // Stop any existing audio playback
            stopAudio();

            // Create new MediaPlayer
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build());

            // Set data source and prepare
            mediaPlayer.setDataSource(audioFile.getPath());
            mediaPlayer.prepare();

            // Add completion listener
            mediaPlayer.setOnCompletionListener(mp -> {
                Log.d(TAG, "Audio playback completed");
                // Clean up resources
                stopAudio();
            });

            // Start playback
            mediaPlayer.start();

            Log.d(TAG, "Started audio playback");

        } catch (IOException e) {
            Log.e(TAG, "Error playing audio", e);
            Toast.makeText(context, "Error playing instructions audio",
                    Toast.LENGTH_SHORT).show();
            stopAudio();
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error playing audio", e);
            Toast.makeText(context, "Audio playback error: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            stopAudio();
        }
    }

    /**
     * Stop any currently playing audio and release resources
     */
    public void stopAudio() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.reset();
                mediaPlayer.release();
            } catch (Exception e) {
                Log.w(TAG, "Error stopping media player", e);
            } finally {
                mediaPlayer = null;
            }
        }
    }

    /**
     * Clean up resources. Call this method in your activity's onDestroy()
     */
    public void cleanup() {
        // Stop audio playback
        stopAudio();

        // Delete temporary audio file
        if (audioFile != null && audioFile.exists()) {
            if (!audioFile.delete()) {
                Log.w(TAG, "Failed to delete temporary audio file: " + audioFile.getPath());
            }
        }
    }
}