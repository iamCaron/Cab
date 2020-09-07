package com.example.cab;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
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

import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class RiderMapActivity extends FragmentActivity implements OnMapReadyCallback,
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

    Button logoutBtn,settingsBtn;
    Button callCabBtn;

    FirebaseAuth mAuth;
    FirebaseUser currentUser;

    private String uId;
    private DatabaseReference riderDatabaseRef;
    private DatabaseReference driverAvailableRef;
    private DatabaseReference driverRef;
    private DatabaseReference driverLocationRef;
    LatLng riderPickupLocation;
    private int radius=1;
    private Boolean driverFound=false;
    private String driverFoundId;
    private Boolean requestType=false;
    private Marker driverMarker,pickupMarker;
    private GeoQuery geoQuery;

    private ValueEventListener driverLocationRefListener;

    TextView txtName,txtPhone,txtCar;
    CircleImageView driverProfile;
    ImageButton callDriverBtn;
    RelativeLayout driverInfoLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rider_map);

        mAuth=FirebaseAuth.getInstance();
        currentUser=mAuth.getCurrentUser();
        uId=mAuth.getCurrentUser().getUid();
        logoutBtn=(Button)findViewById(R.id.logout_btn);
        settingsBtn=(Button)findViewById(R.id.settingsBtn);
        callCabBtn =(Button)findViewById(R.id.call_a_cab_btn);

        riderDatabaseRef=FirebaseDatabase.getInstance().getReference().child("Riders Requests");
        driverAvailableRef =FirebaseDatabase.getInstance().getReference().child("Drivers Available");
        driverLocationRef=FirebaseDatabase.getInstance().getReference().child("Drivers Working");

        txtName=(TextView)findViewById(R.id.driver_name);
        txtPhone=(TextView)findViewById(R.id.driver_phone);
        txtCar=(TextView)findViewById(R.id.driver_car);

        driverInfoLayout=(RelativeLayout)findViewById(R.id.driver_details);
        driverProfile=(CircleImageView)findViewById(R.id.profile_image_driver);
        callDriverBtn=(ImageButton)findViewById(R.id.call_driver);


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        logoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                mAuth.signOut();
                logoutRider();
            }
        });

        settingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(RiderMapActivity.this,SettingsActivity.class);
                intent.putExtra("type","Riders");
                startActivity(intent);
            }
        });


        callCabBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(requestType){
                    requestType=false;
                    geoQuery.removeAllListeners();
                    driverLocationRef.removeEventListener(driverLocationRefListener);

                    if(driverFound!=null) {
                        driverRef = FirebaseDatabase.getInstance().getReference()
                                .child("Users").child("Drivers").child(driverFoundId).child("RidersRideId");
                        driverRef.removeValue();
                        driverFoundId = null;
                    }
                        driverFound=false;
                        radius=1;

                        GeoFire geoFire=new GeoFire(riderDatabaseRef);
                        geoFire.removeLocation(currentUser.getUid().toString());
                        if(pickupMarker!=null) {
                            pickupMarker.remove();
                        }
                        if(driverMarker!=null){
                            driverMarker.remove();

                        callCabBtn.setText("Call a Cab");
                        driverInfoLayout.setVisibility(View.GONE);
                    }
                }else{

                    requestType=true;
                    GeoFire geoFire=new GeoFire(riderDatabaseRef);
                    geoFire.setLocation(uId,new GeoLocation(lastLocation.getLatitude(),lastLocation.getLongitude()));

                    riderPickupLocation =new LatLng(lastLocation.getLatitude(),lastLocation.getLongitude());
                    pickupMarker=mMap.addMarker(new MarkerOptions().position(riderPickupLocation).title("My Pickup Location").icon(BitmapDescriptorFactory.fromResource(R.drawable.rider)));
                    callCabBtn.setText("Calling a Cab...");

                    getClosestDriver();
                }

            }
        });
    }

    private void getClosestDriver() {
        GeoFire geoFire=new GeoFire(driverAvailableRef);
        geoQuery=geoFire.queryAtLocation(new GeoLocation(riderPickupLocation.latitude, riderPickupLocation.longitude),radius);
        geoQuery.removeAllListeners();
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {

                if(!driverFound&&requestType){
                    driverFound=true;
                    driverFoundId=key;

                    driverRef=FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundId);
                    HashMap driversMap=new HashMap();
                    driversMap.put("RidersRideId",uId);
                    driverRef.updateChildren(driversMap); 

                    callCabBtn.setText("Looking for drivers location...");
                    getDriversLocation();

                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if(!driverFound){
                    radius++;
                    getClosestDriver();

                }

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    private void getDriversLocation() {
        driverLocationRefListener=driverLocationRef.child(driverFoundId).child("l").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists() && requestType){
                    List<Object> driverLocationMap=(List<Object>)snapshot.getValue();
                                      double locationLat=0;
                    double locationLng=0;
                    callCabBtn.setText("Driver Found");


                    driverInfoLayout.setVisibility(View.VISIBLE);
                    getAssignedDriverInformation();
                    if(driverLocationMap.get(0)!=null){
                        locationLat=Double.parseDouble(driverLocationMap.get(0).toString());
                    }
                    if(driverLocationMap.get(1)!=null){
                        locationLng=Double.parseDouble(driverLocationMap.get(1).toString());
                    }

                    LatLng driverLatLng=new LatLng(locationLat,locationLng);
                    if(driverMarker!=null){
                        driverMarker.remove();
                    }

                    Location location1=new Location("");
                    location1.setLatitude(riderPickupLocation.latitude);
                    location1.setLongitude(riderPickupLocation.longitude);

                    Location location2=new Location("");
                    location2.setLatitude(driverLatLng.latitude);
                    location2.setLongitude(driverLatLng.longitude);

                    float distance=location1.distanceTo(location2);
                    if(distance<90){
                        callCabBtn.setText("Driver Arrived");
                    }else
                    callCabBtn.setText("Driver Found at"+String.valueOf(distance));

                    driverMarker=mMap.addMarker(new MarkerOptions().position(driverLatLng).title("Your Driver").icon(BitmapDescriptorFactory.fromResource(R.drawable.car)));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void logoutRider() {
        Intent welcomeIntent=new Intent(RiderMapActivity.this,WelcomeActivity.class);
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
                Toast.makeText(RiderMapActivity.this,
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
        }else
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {

          lastLocation=location;
          LatLng latLng=new LatLng(lastLocation.getLatitude(),lastLocation.getLongitude());
          mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
          mMap.animateCamera(CameraUpdateFactory.zoomTo(13));



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

        ActivityCompat.requestPermissions(RiderMapActivity.this,
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
                Toast.makeText(RiderMapActivity.this,
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
    }

    private void getAssignedDriverInformation(){
        DatabaseReference reference=FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundId);
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()&&snapshot.getChildrenCount()>0){

                    if(snapshot.hasChild("car")){
                    String car=snapshot.child("car").getValue().toString();
                    txtCar.setText(car);
                    }

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
                        Picasso.get().load(image).into(driverProfile);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }
}