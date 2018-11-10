package com.kaya.asli.listen;

import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private static final String MIME_TYPE_MP3 = "audio/mpeg";
    private static final int AUDIO_PICK_REQUEST_CODE = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Button button = findViewById(R.id.activity_main_button_choose_file);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final Intent requestFileIntent = new Intent(Intent.ACTION_GET_CONTENT);
                requestFileIntent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                requestFileIntent.setType(MIME_TYPE_MP3);
                startActivityForResult(requestFileIntent, AUDIO_PICK_REQUEST_CODE);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        if (requestCode == AUDIO_PICK_REQUEST_CODE
                && resultCode == RESULT_OK
                && data != null
                && data.getData() != null) {

            Log.d(MainActivity.class.getSimpleName(), data.getData().toString());
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}
