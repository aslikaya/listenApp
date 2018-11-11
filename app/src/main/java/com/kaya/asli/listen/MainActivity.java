package com.kaya.asli.listen;

import android.Manifest;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements
        View.OnClickListener,
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener {

    private static final int PERMISSIONS_REQUEST_CODE_READ_EXTERNAL_STORAGE = 5;
    private static final int REFRESH_ELAPSED_TIME_PERIOD_MS = 1000;

    private Button buttonPickRandomAudioFile;
    private ImageButton buttonPlay;
    private ImageButton buttonStop;
    private MediaPlayer mediaPlayer;
    private TextView textViewTitle;
    private TextView textViewDuration;
    private TextView textViewElapsedTime;

    private LocalService localService;
    private boolean bound = false;

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LocalService.LocalBinder binder = (LocalService.LocalBinder) service;
            localService = binder.getService();
            bound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bound = false;
        }
    };
    private Uri audioUri;
    private ScheduledExecutorService scheduledExecutorService;
    private Runnable updateElapsedTimeRunnable;
    private String title;
    private Long duration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeUiElements();
    }

    @Override
    protected void onStart() {
        super.onStart();
        final Intent intent = new Intent(this, LocalService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    private void initializeUiElements() {
        buttonPickRandomAudioFile = findViewById(R.id.activity_main_button_pick_random_audio_file);
        buttonPickRandomAudioFile.setOnClickListener(this);
        buttonPlay = findViewById(R.id.activity_main_button_play);
        buttonPlay.setOnClickListener(this);
        buttonStop = findViewById(R.id.activity_main_button_stop);
        buttonStop.setOnClickListener(this);
        textViewTitle = findViewById(R.id.activity_main_text_view_title);
        textViewDuration = findViewById(R.id.activity_main_text_view_duration);
        textViewElapsedTime = findViewById(R.id.activity_main_text_view_elapsed_time);

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

            title = musicCursor.getString(titleColumn);
            long id = musicCursor.getLong(idColumn);
            duration = musicCursor.getLong(durationColumn);

            audioUri = ContentUris.withAppendedId(
                    android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);

            musicCursor.close();

            arrangeWidgetVisibility();
        }
    }

    private void arrangeWidgetVisibility() {
        buttonPlay.setVisibility(View.VISIBLE);
        buttonStop.setVisibility(View.VISIBLE);
        textViewTitle.setVisibility(title != null ? View.VISIBLE : View.GONE);
        textViewDuration.setVisibility(duration != null ? View.VISIBLE : View.GONE);
        updateMetaData();
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

            if (bound) {
                localService.stop();
            }

        } else if (v.getId() == buttonPlay.getId() && audioUri != null) {
            if (bound) {
                localService.play(audioUri);
            }

        } else if (v.getId() == buttonStop.getId() ) {
            if (bound) {
                localService.stop();
            }
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
        startUpdatingCallbackWithPosition();
    }

    private void updateMetaData() {
        textViewTitle.setText(getString(R.string.title, title));
        textViewDuration.setText(getString(R.string.duration, TimeUtil.millisecondsToFormattedTime(duration)));
    }

    private void startUpdatingCallbackWithPosition() {
        textViewElapsedTime.setVisibility(View.VISIBLE);

        if (scheduledExecutorService == null) {
            scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        }

        if (updateElapsedTimeRunnable == null) {
            updateElapsedTimeRunnable = new Runnable() {
                @Override
                public void run() {
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                        textViewElapsedTime.setText(
                                getString(R.string.elapsed_time, mediaPlayer.getCurrentPosition()));
                        Log.d(MainActivity.class.getSimpleName(), Integer.toString(mediaPlayer.getCurrentPosition()));
                    }

                }
            };
        }

        scheduledExecutorService.scheduleAtFixedRate(
                updateElapsedTimeRunnable,
                0,
                REFRESH_ELAPSED_TIME_PERIOD_MS,
                TimeUnit.MILLISECONDS
        );
    }

    @Override
    protected void onStop() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        super.onStop();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (scheduledExecutorService != null) {
            scheduledExecutorService.shutdownNow();
            scheduledExecutorService = null;
            updateElapsedTimeRunnable = null;
        }
    }

    @Override
    protected void onDestroy() {
        unbindService(connection);
        super.onDestroy();
    }
}
