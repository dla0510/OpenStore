package com.example.openstore;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class information extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.information);


        String title = "";
        String address = "";


        Bundle extras = getIntent().getExtras();

        if (extras == null) {
            title = "error";
        }
        else {

            title = extras.getString("title");
            address = extras.getString("address");

        }

        TextView textView = (TextView) findViewById(R.id.textView_information_contentString);

        String str = title + '\n' + address + '\n';
        textView.setText(str);

    }
}