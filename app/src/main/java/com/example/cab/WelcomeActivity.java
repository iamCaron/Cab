package com.example.cab;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class WelcomeActivity extends AppCompatActivity {

    private Button welcomeDriverBtn,welcomeRiderBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        welcomeRiderBtn=(Button)findViewById(R.id.riderButton);
        welcomeDriverBtn=(Button)findViewById(R.id.driverButton);

        welcomeDriverBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(WelcomeActivity.this,DriverLoginRegisterActivity.class);
                startActivity(intent);
            }
        });

        welcomeRiderBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(WelcomeActivity.this,RiderLoginRegisterActivity.class);
                startActivity(intent);
            }
        });

    }
}