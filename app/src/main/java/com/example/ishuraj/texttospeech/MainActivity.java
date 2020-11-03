package com.example.ishuraj.texttospeech;

import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    Button b1;
    EditText e1;
    TextToSpeech ts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        b1 = (Button)findViewById(R.id.button);
        e1 = (EditText)findViewById(R.id.editText);
        ts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                ts.setLanguage(Locale.ENGLISH);
                ts.setSpeechRate(2f);
            }
        });

        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String s = e1.getText().toString();
                ts.speak(s, TextToSpeech.QUEUE_FLUSH, null); //queue_flush flushes the text from memory after speaking
            }
        });
    }
}
