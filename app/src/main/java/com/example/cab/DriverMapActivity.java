package com.example.cab;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.LocationCallback;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class DriverMapActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    private GoogleApiClient googleApiClient;
    Location lastLocation;
    LocationRequest locationRequest;
    final int PERMISSIONS_REQUEST_CODE=0;

    final String[] PERMISSIONS_STRING_ARRAY=new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION};

    private Button settingsButton,logoutButton;
    FirebaseAuth mAuth;
    FirebaseUser currentUser;
    private boolean currentLogoutDriverStatus=false;
    private DatabaseReference assignedRiderReference,assignedRiderPickupLocationRef;
    private String driverId,riderId="";
    Marker pickUpMarker;
    private ValueEventListener assignedRiderPickupRefListener;

    TextView txtName,txtPhone;
    CircleImageView riderProfile;
    ImageButton callRiderBtn;
    RelativeLayout riderInfoLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_map);

        mAuth=FirebaseAuth.getInstance();
        currentUser=mAuth.getCurrentUser();
        driverId=currentUser.getUid();

        logoutButton=(Button)findViewById(R.id.logoutBtn);
        settingsButton=(Button)findViewById(R.id.settingsBtn);

        txtName=(TextView)findViewById(R.id.rider_name);
        txtPhone=(TextView)findViewById(R.id.rider_phone);


       riderInfoLayout=(RelativeLayout)findViewById(R.id.rider_details);
       riderProfile=(CircleImageView)findViewById(R.id.profile_image_rider);
        callRiderBtn=(ImageButton)findViewById(R.id.call_rider);


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                currentLogoutDriverStatus=true;
                disconnectTheDriver();
                mAuth.signOut();
                logoutDriver();
            }
        });

        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(DriverMapActivity.this,SettingsActivity.class);
                intent.putExtra("type","Drivers");
                startActivity(intent);
            }
        });



        getAssignedRiderRequest();

    }

    private void getAssignedRiderRequest() {
        assignedRiderReference =FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverId).child("RidersRideId");
        assignedRiderReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                        riderId=snapshot.getValue().toString();
                        getAssignedRiderPickupLocation();
                        riderInfoLayout.setVisibility(View.VISIBLE);
                        getAssignedRiderInformation();
                }else{
                    riderId="";
                    if(pickUpMarker!=null){
                        pickUpMarker.remove();
                    }

                    if(assignedRiderPickupRefListener!=null){
                        assignedRiderPickupLocationRef.removeEventListener(assignedRiderPickupRefListener);
                    }
                    riderInfoLayout.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void getAssignedRiderPickupLocation() {
       assignedRiderPickupLocationRef=FirebaseDatabase.getInstance().getReference().child("Riders Requests").child(riderId).child("l");
       assignedRiderPickupRefListener=assignedRiderPickupLocationRef.addValueEventListener(new ValueEventListener() {
           @Override
           public void onDataChange(@NonNull DataSnapshot snapshot) {
               List<Object> riderLocationMap=(List<Object>)snapshot.getValue();
               double locationLat=0;
               double locationLng=0;

               if(riderLocationMap.get(0)!=null){
                   locationLat=Double.parseDouble(riderLocationMap.get(0).toString());
               }
               if(riderLocationMap.get(1)!=null){
                   locationLng=Double.parseDouble(riderLocationMap.get(1).toString());
               }

               LatLng riderLatLng=new LatLng(locationLat,locationLng);
               pickUpMarker=mMap.addMarker(new MarkerOptions().position(riderLatLng).title("Your Passenger").icon(BitmapDescriptorFactory.fromResource(R.drawable.rider)));
           }

           @Override
           public void onCancelled(@NonNull DatabaseError error) {

           }
       });
    }


    private void logoutDriver() {
        Intent welcomeIntent=new Intent(DriverMapActivity.this,WelcomeActivity.class);
        welcomeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(welcomeIntent);
        finish();
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        buildGoogleApiClient();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(PERMISSIONS_STRING_ARRAY,PERMISSIONS_REQUEST_CODE);
            }else{
                Toast.makeText(DriverMapActivity.this,
                        "Permission Denied, please grant the permission",
                        Toast.LENGTH_SHORT)
                        .show();
            }

            return;
        }
        mMap.setMyLocationEnabled(true);

        }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(locationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermission(PERMISSIONS_STRING_ARRAY,PERMISSIONS_REQUEST_CODE);
              return;
        }else {
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);

        }

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        if(getApplicationContext()!=null){
            lastLocation=location;
            LatLng latLng=new LatLng(lastLocation.getLatitude(),lastLocation.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(13));
            String userId= FirebaseAuth.getInstance().getCurrentUser().getUid();

            DatabaseReference driverAvailablityRef = FirebaseDatabase.getInstance().getReference().child("Drivers Available");
            GeoFire geoFireAvailability=new GeoFire(driverAvailablityRef);

            DatabaseReference driverWorkingRef=FirebaseDatabase.getInstance().getReference().child("Drivers Working");
            GeoFire geoFireWorking=new GeoFire(driverWorkingRef);

            switch (riderId)
            {

                case "":
                    geoFireWorking.removeLocation(userId);
                    geoFireAvailability.setLocation(userId,new GeoLocation(location.getLatitude(),location.getLongitude()));

                break;
                default:
                    geoFireAvailability.removeLocation(userId);
                    geoFireWorking.setLocation(userId,new GeoLocation(location.getLatitude(),location.getLongitude()));

                break;
            }

        }
    }

    protected synchronized void buildGoogleApiClient(){
        googleApiClient=new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();

        googleApiClient.connect();

    }

    // Function to check and request permission.
    public void requestPermission(String[] permission, int requestCode) {

        ActivityCompat.requestPermissions(DriverMapActivity.this,
                PERMISSIONS_STRING_ARRAY,PERMISSIONS_REQUEST_CODE);

    }

        @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode,
                        permissions,
                        grantResults);

        if (requestCode == PERMISSIONS_REQUEST_CODE){
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED&& grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
            }
            else {
                Toast.makeText(DriverMapActivity.this,
                        "Permission Denied, please grant the permission",
                        Toast.LENGTH_SHORT)
                        .show();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermission(PERMISSIONS_STRING_ARRAY,PERMISSIONS_REQUEST_CODE);
                }
            }
        }


    }

    @Override
    protected void onStop() {
        super.onStop();

        if(!currentLogoutDriverStatus){
            disconnectTheDriver();
        }

    }

    private void disconnectTheDriver() {
        String userId= FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference driverAvailablityRef = FirebaseDatabase.getInstance().getReference().child("Drivers Available");
        GeoFire geoFire=new GeoFire(driverAvailablityRef);
        geoFire.removeLocation(userId);
    }

    private void getAssignedRiderInformation(){
        DatabaseReference reference=FirebaseDatabase.getInstance().getReference().child("Users").child("Riders").child(riderId);
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()&&snapshot.getChildrenCount()>0){

                    if(snapshot.hasChild("name")){
                        String name=snapshot.child("name").getValue().toString();
                        txtName.setText(name);
                    }

                    if(snapshot.hasChild("phone")){
                        String phone=snapshot.child("phone").getValue().toString();
                        txtPhone.setText(phone);
                    }


                    if(snapshot.hasChild("image")){
                        String image = snapshot.child("image").getValue().toString();
                        Picasso.get().load(image).into(riderProfile);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }
}