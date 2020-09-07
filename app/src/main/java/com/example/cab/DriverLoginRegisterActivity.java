package com.example.cab;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
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

public class DriverLoginRegisterActivity extends AppCompatActivity {


    Button driverLoginButton,driverRegisterButton;
    TextView driverRegisterLink,title;
    EditText emailEditText, passwordEditText;
    FirebaseAuth mAuth;
    ProgressDialog loadingBar;

    private DatabaseReference driverDatabaseReference;
    private String onlineDriverId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_login_register);

        mAuth=FirebaseAuth.getInstance();



        driverLoginButton=(Button)findViewById(R.id.LoginBtn);
        driverRegisterButton=(Button)findViewById(R.id.RegisterBtn);
        driverRegisterLink=(TextView)findViewById(R.id.registerLink);
        title=(TextView)findViewById(R.id.title);

        emailEditText =(EditText)findViewById(R.id.EmailAddress);
        passwordEditText =(EditText)findViewById(R.id.Password) ;
        loadingBar=new ProgressDialog(this);

        driverRegisterButton.setVisibility(View.INVISIBLE);
        driverRegisterButton.setEnabled(false);

        driverRegisterLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                driverLoginButton.setVisibility(View.INVISIBLE);
                driverRegisterLink.setVisibility(View.INVISIBLE);

                driverRegisterButton.setVisibility(View.VISIBLE);
                driverRegisterButton.setEnabled(true);
                title.setText("Driver Register");
            }
        });

        driverRegisterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email=emailEditText.getText().toString();
                String password=passwordEditText.getText().toString();
                registerDriver(email,password);
            }
        });

        driverLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email=emailEditText.getText().toString();
                String password=passwordEditText.getText().toString();
                SignInDriver(email,password);
            }
        });


    }

    private void SignInDriver(String email, String password) {
        if(TextUtils.isEmpty(email)){
            Toast.makeText(DriverLoginRegisterActivity.this,"Please Enter Valid Email.",Toast.LENGTH_SHORT).show();
        }else

        if(TextUtils.isEmpty(password)){
            Toast.makeText(DriverLoginRegisterActivity.this,"Please Enter Valid Password.",Toast.LENGTH_SHORT).show();
        }else {

            loadingBar.setTitle("Driver SignIn");
            loadingBar.setMessage("Please wait while we are checking your credentials...");
            loadingBar.show();
            mAuth.signInWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if(task.isSuccessful()){
                        loadingBar.dismiss();
                        Intent driverIntent =new Intent(DriverLoginRegisterActivity.this,DriverMapActivity.class);
                        startActivity(driverIntent);

                        Toast.makeText(DriverLoginRegisterActivity.this,"Driver Login Successfully.",Toast.LENGTH_SHORT).show();


                    }else {
                        loadingBar.dismiss();
                        Toast.makeText(DriverLoginRegisterActivity.this,"Driver Login Unsuccessful, please try again",Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

    }


    private void registerDriver(String email, String password) {
        if(TextUtils.isEmpty(email)){
            Toast.makeText(DriverLoginRegisterActivity.this,"Please Enter Valid Email.",Toast.LENGTH_SHORT).show();
        }else

        if(TextUtils.isEmpty(password)){
            Toast.makeText(DriverLoginRegisterActivity.this,"Please Enter Valid Password.",Toast.LENGTH_SHORT).show();
        }else
        {
            loadingBar.setTitle("Driver Registration");
            loadingBar.setMessage("Please wait while we are registering your data...");
            loadingBar.show();
            mAuth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if(task.isSuccessful()){
                        onlineDriverId=mAuth.getCurrentUser().getUid();
                        driverDatabaseReference= FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(onlineDriverId);

                        driverDatabaseReference.setValue(true);
                        loadingBar.dismiss();
                        Intent intent=new Intent(DriverLoginRegisterActivity.this,DriverMapActivity.class);
                        startActivity(intent);
                        Toast.makeText(DriverLoginRegisterActivity.this,"Driver Registered Successfully.",Toast.LENGTH_SHORT).show();
                    }else {
                        loadingBar.dismiss();
                        Toast.makeText(DriverLoginRegisterActivity.this,"Driver Registration Unsuccessful",Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }
}