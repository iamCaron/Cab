package com.example.cab;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RiderLoginRegisterActivity extends AppCompatActivity {

    Button riderLoginButton,riderRegisterButton;
    TextView riderRegisterLink,title;
    EditText emailEditText,passwordEditText;
    FirebaseAuth mAuth;
    ProgressDialog loadingBar;
    private DatabaseReference riderDatabaseReference;
    private String onlineRiderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rider_login_register);

        mAuth=FirebaseAuth.getInstance();

        riderLoginButton=(Button)findViewById(R.id.LoginBtn);
        riderRegisterButton=(Button)findViewById(R.id.RegisterBtn);
        riderRegisterLink=(TextView)findViewById(R.id.registerLink);
        title=(TextView)findViewById(R.id.title);
        loadingBar=new ProgressDialog(this);

        emailEditText=(EditText)findViewById(R.id.EmailAddress);
        passwordEditText=(EditText)findViewById(R.id.Password) ;

        riderRegisterButton.setVisibility(View.INVISIBLE);
        riderRegisterButton.setEnabled(false);

        riderRegisterLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                riderLoginButton.setVisibility(View.INVISIBLE);
                riderRegisterLink.setVisibility(View.INVISIBLE);

                riderRegisterButton.setVisibility(View.VISIBLE);
                riderRegisterButton.setEnabled(true);
                title.setText("Rider Register");
            }
        });

        riderRegisterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email=emailEditText.getText().toString();
                String password=passwordEditText.getText().toString();
                registerRider(email,password);
            }
        });

        riderLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String email=emailEditText.getText().toString();
                String password=passwordEditText.getText().toString();
                SignInRider(email,password);
            }
        });

    }

    private void SignInRider(String email, String password) {
        if(TextUtils.isEmpty(email)){
            Toast.makeText(RiderLoginRegisterActivity.this,"Please Enter Valid Email.",Toast.LENGTH_SHORT).show();
        }else

        if(TextUtils.isEmpty(password)){
            Toast.makeText(RiderLoginRegisterActivity.this,"Please Enter Valid Password.",Toast.LENGTH_SHORT).show();
        }else {

            loadingBar.setTitle("Rider SignIn");
            loadingBar.setMessage("Please wait while we are checking your credentials...");
            loadingBar.show();
            mAuth.signInWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if(task.isSuccessful()){

                        loadingBar.dismiss();  Intent riderIntent =new Intent(RiderLoginRegisterActivity.this,RiderMapActivity.class);
                        startActivity(riderIntent);

                        Toast.makeText(RiderLoginRegisterActivity.this,"Rider Login Successfully.",Toast.LENGTH_SHORT).show();

                    }else {
                        loadingBar.dismiss();
                        Toast.makeText(RiderLoginRegisterActivity.this,"Rider Login Unsuccessful, please try again",Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

    }

    private void registerRider(String email, String password) {
        if(TextUtils.isEmpty(email)){
            Toast.makeText(RiderLoginRegisterActivity.this,"Please Enter Valid Email.",Toast.LENGTH_SHORT).show();
        }else

        if(TextUtils.isEmpty(password)){
            Toast.makeText(RiderLoginRegisterActivity.this,"Please Enter Valid Password.",Toast.LENGTH_SHORT).show();
        }else
        {
            loadingBar.setTitle("Rider Registration");
            loadingBar.setMessage("Please wait while we are registering your data...");
            loadingBar.show();
            mAuth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if(task.isSuccessful()){
                        onlineRiderId=mAuth.getCurrentUser().getUid();
                        riderDatabaseReference= FirebaseDatabase.getInstance().getReference().child("Users").child("Riders").child(onlineRiderId);

                        riderDatabaseReference.setValue(true);
                        loadingBar.dismiss();
                        Intent intent=new Intent(RiderLoginRegisterActivity.this,RiderMapActivity.class);
                        startActivity(intent);
                        Toast.makeText(RiderLoginRegisterActivity.this,"Driver Registered Successfully.",Toast.LENGTH_SHORT).show();
                    }else {
                        loadingBar.dismiss();
                        Toast.makeText(RiderLoginRegisterActivity.this,"Driver Registration Unsuccessful",Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }
}