package com.example.cab;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;

import gr.net.maroulis.library.EasySplashScreen;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        View easySplashScreenView = new EasySplashScreen(SplashActivity.this)
                .withFullScreen()
                .withTargetActivity(WelcomeActivity.class)
                .withSplashTimeOut(1600)
                .withBackgroundResource(android.R.color.holo_purple)
                .withHeaderText("CAB")
                .withFooterText("Copyright 2020")

                .withLogo(R.drawable.splash)
                .withAfterLogoText("Ride for you...")
                .create();



        setContentView(easySplashScreenView);

    }
}