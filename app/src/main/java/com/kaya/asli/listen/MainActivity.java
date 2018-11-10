package com.kaya.asli.listen;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import java.io.IOException;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements
        View.OnClickListener,
        MediaPlayer.OnPreparedListener {

    private static final String MIME_TYPE_MP3 = "audio/mpeg";
    private static final int PERMISSIONS_REQUEST_CODE_READ_EXTERNAL_STORAGE = 5;

    private Button buttonPickRandomAudioFile;
    private ImageButton buttonPlay;
    private ImageButton buttonStop;
    private MediaPlayer mediaPlayer;

    private Uri audioUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeUiElements();
    }

    private void initializeUiElements() {
        buttonPickRandomAudioFile = findViewById(R.id.activity_main_button_pick_random_audio_file);
        buttonPickRandomAudioFile.setOnClickListener(this);
        buttonPlay = findViewById(R.id.activity_main_button_play);
        buttonPlay.setOnClickListener(this);
        buttonStop = findViewById(R.id.activity_main_button_stop);
        buttonStop.setOnClickListener(this);

    }

    private void pickRandomAudioFileFromDevice() {

        ContentResolver contentResolver = this.getContentResolver();
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = contentResolver.query(musicUri, null, null, null, null, null);

        if(musicCursor != null && musicCursor.moveToFirst())
        {
            int titleColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int durationColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
            int audioFileCount = musicCursor.getCount();

            final Random randomIndex  = new Random();
            int randomNumber = randomIndex.nextInt(audioFileCount - 1);


            int i = 0;
            do
            {
                if (i == randomNumber) {
                    break;
                }
                i++;
            }
            while (musicCursor.moveToNext());

            // TODO show info in UI
            String title = musicCursor.getString(titleColumn);
            long id = musicCursor.getLong(idColumn);
            long duration = musicCursor.getLong(durationColumn);

            audioUri = ContentUris.withAppendedId(
                    android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);

            musicCursor.close();
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == buttonPickRandomAudioFile.getId()) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                pickRandomAudioFileFromDevice();
            } else {
                requestPermissionAccessDeviceStorage();
            }


        } else if (v.getId() == buttonPlay.getId() && audioUri != null) {
            if (mediaPlayer == null) {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mediaPlayer.setOnPreparedListener(this);
                //todo handle permission wake_lock
                //mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
            } else {
                mediaPlayer.reset();
            }

            try {
                mediaPlayer.setDataSource(getApplicationContext(), audioUri);
            } catch (IOException e) {
                e.printStackTrace();
            }

            mediaPlayer.prepareAsync();

        } else if (v.getId() == buttonStop.getId() && mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CODE_READ_EXTERNAL_STORAGE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    pickRandomAudioFileFromDevice();
                }
                break;
            default:
                break;
        }
    }

    private void requestPermissionAccessDeviceStorage() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                PERMISSIONS_REQUEST_CODE_READ_EXTERNAL_STORAGE);
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mediaPlayer.start();
    }

    @Override
    protected void onStop() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        super.onStop();
    }
}
