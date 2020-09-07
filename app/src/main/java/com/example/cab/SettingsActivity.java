package com.example.cab;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsActivity extends AppCompatActivity {

    private CircleImageView profileImageView;
    private EditText nameEditText, phoneEditText,driverCarName;

    private TextView profileChangeBtn;
    private ImageView closeBtn,saveBtn;

    private String getType;
    private String checker="";
    private Uri imageUri;
    private String myUrl="";
    private StorageTask uploadTask;
    private StorageReference storageProfilePicsRef;
    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;

    final int PERMISSIONS_REQUEST_CODE=100;
    final String[] PERMISSIONS_STRING_ARRAY=new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getType=getIntent().getStringExtra("type");
        mAuth=FirebaseAuth.getInstance();
        databaseReference= FirebaseDatabase.getInstance().getReference().child("Users").child(getType);
        storageProfilePicsRef= FirebaseStorage.getInstance().getReference().child("Profile Pictures");

        profileImageView=(CircleImageView)findViewById(R.id.profile_image);
        nameEditText=(EditText)findViewById(R.id.name);
        phoneEditText=(EditText)findViewById(R.id.phone);
        driverCarName=(EditText)findViewById(R.id.driver_car_name);
        if(getType.equals("Drivers")){
            driverCarName.setVisibility(View.VISIBLE);
        }else{
            driverCarName.setVisibility(View.GONE);
        }

        closeBtn=(ImageView) findViewById(R.id.close_btn);
        saveBtn=(ImageView)findViewById(R.id.save_btn);

        profileChangeBtn=(TextView) findViewById(R.id.change_picture_btn);

        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(getType.equals("Drivers")){

                    startActivity(new Intent(SettingsActivity.this,DriverMapActivity.class)
                            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
                    finish();
                }else {
                    startActivity(new Intent(SettingsActivity.this,RiderMapActivity.class)
                            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
                    finish();
                }
            }
        });

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(checker.equals("clicked")){
                    validateControllers();
                }else{
                     validateAndSaveOnlyInformaion();
                }
            }
        });

        profileChangeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ActivityCompat.checkSelfPermission(SettingsActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(SettingsActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        requestPermissions(PERMISSIONS_STRING_ARRAY,PERMISSIONS_REQUEST_CODE);
                    }else{
                        Toast.makeText(SettingsActivity.this,
                                "Permission Denied, please grant the permission",
                                Toast.LENGTH_SHORT)
                                .show();
                    }

                    return;
                }else {
                    checker = "clicked";
                    CropImage.activity()
                            .setAspectRatio(1, 1)
                            .start(SettingsActivity.this);
                }

            }
        });
        getUserInformation();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE && resultCode==RESULT_OK&&data!=null){
            CropImage.ActivityResult result=CropImage.getActivityResult(data);
            imageUri=result.getUri();
            profileImageView.setImageURI(imageUri);
        }else {
            if(getType.equals("Drivers")){
                driverCarName.setVisibility(View.VISIBLE);
                Toast.makeText(this,"Error try again",Toast.LENGTH_SHORT);
            }else{
                driverCarName.setVisibility(View.GONE);
                Toast.makeText(this,"Error try again",Toast.LENGTH_SHORT);
            }

        }
    }

    private void validateControllers(){
        if(TextUtils.isEmpty(nameEditText.getText().toString())){
            Toast.makeText(this,"Please Enter Your Name",Toast.LENGTH_SHORT);
        }else if(TextUtils.isEmpty(phoneEditText.getText().toString())){
            Toast.makeText(this,"Please Enter Your Phone Number",Toast.LENGTH_SHORT);
        }else if(getType.equals("Drivers" ) && TextUtils.isEmpty(driverCarName.getText().toString())){
            Toast.makeText(this,"Please Enter Your Car Name",Toast.LENGTH_SHORT);
        }else if(checker.equals("clicked")){
            updateProfilePicture();

        }
    }

    private void updateProfilePicture() {

        final ProgressDialog progressDialog=new ProgressDialog(this);
        progressDialog.setTitle("Settings Account Information");
        progressDialog.setMessage("Please wait while we are setting your account information");
        progressDialog.show();
        if(imageUri!=null){
            final StorageReference fileRef=storageProfilePicsRef
                    .child(mAuth.getCurrentUser().getUid() + ".jpg");
              uploadTask=fileRef.putFile(imageUri);
              uploadTask.continueWithTask(new Continuation() {
                  @Override
                  public Object then(@NonNull Task task) throws Exception {
                      if(!task.isSuccessful()){
                          throw task.getException();
                      }
                      return fileRef.getDownloadUrl();
                  }
              }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                  @Override
                  public void onComplete(@NonNull Task<Uri> task) {
                      if(task.isSuccessful()){
                          Uri downloadUri=task.getResult();
                          myUrl=downloadUri.toString();

                          HashMap<String,Object> userMap =new HashMap<>();
                          userMap.put("uid",mAuth.getCurrentUser().getUid());
                          userMap.put("name",nameEditText.getText().toString());
                          userMap.put("phone",phoneEditText.getText().toString());
                          userMap.put("image",myUrl);
                          if (getType.equals("Drivers")){
                              userMap.put("car",driverCarName.getText().toString());
                          }

                          databaseReference.child(mAuth.getCurrentUser().getUid()).updateChildren(userMap);
                          progressDialog.dismiss();
                          getUserInformation();

                      }
                  }
              });
        }
    }

    private void validateAndSaveOnlyInformaion(){

        if(TextUtils.isEmpty(nameEditText.getText().toString())){
            Toast.makeText(this,"Please Enter Your Name",Toast.LENGTH_SHORT);
        }else if(TextUtils.isEmpty(phoneEditText.getText().toString())){
            Toast.makeText(this,"Please Enter Your Phone Number",Toast.LENGTH_SHORT);
        }else if(getType.equals("Drivers" ) && TextUtils.isEmpty(driverCarName.getText().toString())){
            Toast.makeText(this,"Please Enter Your Car Name",Toast.LENGTH_SHORT);
        }else {
            HashMap<String, Object> userMap = new HashMap<>();
            userMap.put("uid", mAuth.getCurrentUser().getUid());
            userMap.put("name", nameEditText.getText().toString());
            userMap.put("phone", phoneEditText.getText().toString());

            if (getType.equals("Drivers")) {
                userMap.put("car", driverCarName.getText().toString());
            }

            databaseReference.child(mAuth.getCurrentUser().getUid()).updateChildren(userMap);
        }
    }

    private void getUserInformation(){
        databaseReference.child(mAuth.getCurrentUser().getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()&&snapshot.getChildrenCount()>0){
                    if(snapshot.hasChild("name")){
                        String name=snapshot.child("name").getValue().toString();
                        nameEditText.setText(name);
                    }

                    if(snapshot.hasChild("phone")){
                        String phone=snapshot.child("phone").getValue().toString();
                        phoneEditText.setText(phone);
                    }



                    if (getType.equals("Drivers")) {
                        if(snapshot.hasChild("car")) {
                            String car = snapshot.child("car").getValue().toString();
                            driverCarName.setText(car);
                        }
                    }
                    if(snapshot.hasChild("image")){
                        String image = snapshot.child("image").getValue().toString();
                        Picasso.get().load(image).into(profileImageView);
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    // Function to check and request permission.
    public void requestPermission(String[] permission, int requestCode) {

        ActivityCompat.requestPermissions(SettingsActivity.this,
                PERMISSIONS_STRING_ARRAY,PERMISSIONS_REQUEST_CODE);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode,
                permissions,
                grantResults);

        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                profileChangeBtn.performClick();
            } else {
                Toast.makeText(this,
                        "Permission Denied, please grant the Read Write permission",
                        Toast.LENGTH_SHORT)
                        .show();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermission(PERMISSIONS_STRING_ARRAY, PERMISSIONS_REQUEST_CODE);
                }
            }
        }

     }
    }