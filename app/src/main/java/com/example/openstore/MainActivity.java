package com.example.openstore;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;

import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.kakao.kakaolink.KakaoLink;
import com.kakao.kakaolink.KakaoTalkLinkMessageBuilder;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;


public class MainActivity extends AppCompatActivity
        implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    MyPlacesAdapter adapter;
    AutoCompleteTextView search;

    Bitmap open;
    Bitmap current;
    Bitmap close;
    BackgroundTask task;

    ArrayList<MyGooglePlaces> places= new ArrayList<MyGooglePlaces>();

    private GoogleApiClient mGoogleApiClient = null;
    private GoogleMap mGoogleMap = null;
    private Marker currentMarker = null;

    private static final String TAG = "googlemap_example";
    private static final int GPS_ENABLE_REQUEST_CODE = 2001;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 2002;
    private static final int UPDATE_INTERVAL_MS = 5000;
    private static final int FASTEST_UPDATE_INTERVAL_MS = 1000;

    private CameraPosition cameraPosition;
    private AppCompatActivity mActivity;
    boolean askPermissionOnceAgain = false;
    boolean mRequestingLocationUpdates = false;
    Location mCurrentLocation;
    boolean mMoveMapByUser = true;
    boolean mMoveMapByAPI = true;
    LatLng currentPosition;
    int Radius = 1000;

    LatLng mPosition;

    LocationRequest locationRequest = new LocationRequest()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY) //배터리소모를 고려하지않으며 정확도 최우선
            .setInterval(UPDATE_INTERVAL_MS)
            .setFastestInterval(FASTEST_UPDATE_INTERVAL_MS);

    List<Marker> previous_marker = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        Intent intent = new Intent(this, LoadingActivity.class);
        startActivity(intent);

        Log.d(TAG, "onCreate");

        previous_marker = new ArrayList<Marker>();

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        mActivity = this;

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        ImageButton btn_timepicker = (ImageButton) findViewById(R.id.btn_timePicker);
        btn_timepicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TimePickerDialog.OnTimeSetListener onTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        String am_pm = "";
                        String mm_precede = "";

                        if(hourOfDay >= 12) {
                            am_pm = "PM";
                            if (hourOfDay >=13 && hourOfDay <24) {
                                hourOfDay -= 12;
                            } else {
                                hourOfDay = 12;
                            }
                        } else if (hourOfDay == 0){
                            hourOfDay = 12;
                        }

                        if(minute < 10) {
                            mm_precede = "0";
                        }

                        TextView selectedTime = (TextView) findViewById(R.id.selectedTime);
                        selectedTime.setText(am_pm+" "+hourOfDay+" : "+mm_precede+minute);
                    }
                };

                Calendar mCalendar = Calendar.getInstance();
                int hour = mCalendar.get(Calendar.HOUR_OF_DAY);
                int min = mCalendar.get(Calendar.MINUTE);

                boolean is24Hour = false;

                TimePickerDialog timePickerDialog = new TimePickerDialog(
                        MainActivity.this, android.R.style.Theme_Holo_Light_Dialog, onTimeSetListener, hour, min, is24Hour);


                timePickerDialog.setTitle("Please select time");

                timePickerDialog.show();
            }
        });

        // 검색 버튼
        ImageButton button1 = (ImageButton) findViewById(R.id.button1);
        ImageButton button2 = (ImageButton) findViewById(R.id.button2);
        ImageButton button3 = (ImageButton) findViewById(R.id.button3);
        ImageButton button4 = (ImageButton) findViewById(R.id.button4);
        ImageButton button5 = (ImageButton) findViewById(R.id.button5);


        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showInformation("restaurant", mPosition);
            }
        });
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showInformation("cafe", mPosition);
            }
        });
        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showInformation("convenience_store", mPosition);
            }
        });
        button4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showInformation("hospital", mPosition);
            }
        });
        button5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showInformation("bar", mPosition);
            }
        });


        BitmapDrawable bitmapdraw1=(BitmapDrawable)getResources().getDrawable(R.drawable.open,null);
        BitmapDrawable bitmapdraw2=(BitmapDrawable)getResources().getDrawable(R.drawable.current,null);
        BitmapDrawable bitmapdraw3=(BitmapDrawable)getResources().getDrawable(R.drawable.close,null);

        Bitmap b1=bitmapdraw1.getBitmap();
        Bitmap b2=bitmapdraw2.getBitmap();
        Bitmap b3=bitmapdraw3.getBitmap();

        open = Bitmap.createScaledBitmap(b1, 100, 100, false);
        current = Bitmap.createScaledBitmap(b2, 120, 120, false);
        close = Bitmap.createScaledBitmap(b3, 100, 100, false);
    }


    @Override
    public void onResume() {
        super.onResume();

        setCurrentTime();

        if (mGoogleApiClient.isConnected()) {

            Log.d(TAG, "onResume : call startLocationUpdates");
            if (!mRequestingLocationUpdates) startLocationUpdates();


        }
        if (askPermissionOnceAgain) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                askPermissionOnceAgain = false;

                checkPermissions();
            }
        }

    }

    private void setCurrentTime() {
        String am_pm = "";
        String mm_precede = "";

        Calendar datetime = Calendar.getInstance();

        if (datetime.get(Calendar.AM_PM) == Calendar.AM)
            am_pm = "AM";
        else if (datetime.get(Calendar.AM_PM) == Calendar.PM)
            am_pm = "PM";

        if(datetime.get(Calendar.MINUTE)<10) {
            mm_precede = "0";
        }
        String strHrs = (datetime.get(Calendar.HOUR) == 0) ?"12":datetime.get(Calendar.HOUR)+"";

        TextView selectedTime = (TextView) findViewById(R.id.selectedTime);
        selectedTime.setText(am_pm+" "+strHrs+" : "+mm_precede+datetime.get(Calendar.MINUTE));
    }

    public void shareKakao(View v) {
        try {
            final KakaoLink kakaoLink = KakaoLink.getKakaoLink(this);
            final KakaoTalkLinkMessageBuilder kakaoBuilder = kakaoLink.createKakaoTalkLinkMessageBuilder();

            kakaoBuilder.addText("카카오링크 테스트");
            String url = "https://Ih3.googleusercontent.com/4FMghyiNYU73ECn5bHOKGOX1Nv_A5J7z2eRjHGIGxtQtK7L-fyNVuqcvyq6C1vIUxgPP=w300-rw";
            kakaoBuilder.addImage(url, 160, 160);

            kakaoBuilder.addAppButton("앱 실행 혹은 다운로드");
            kakaoLink.sendMessage(kakaoBuilder, this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (item.getItemId() == R.id.menu_share) {
            //   shareKakao();
        }
        //noinspection SimplifiableIfStatement
        else if (id == R.id.action_settings) {
            setOptionsDialog();
        } else if (id == R.id.about) {
            // 옵션메뉴에서 about 클릭했을때
            openOptionsDialog();

        } else if (id == R.id.exit) {
            // 옵션메뉴에서 Exit 를 클릭했을때
            exitOptionsDialog();

        }
        return super.onOptionsItemSelected(item);
    }




    private void setOptionsDialog() {
        final List<Integer> ListItems = new ArrayList<>();
        ListItems.add(500);
        ListItems.add(1000);
        ListItems.add(1500);
        ListItems.add(2000);
        final CharSequence[] items =  {"500","1000","1500","2000"};

        final List SelectedItems  = new ArrayList();
        int defaultItem = 1;
        SelectedItems.add(defaultItem);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Radius");
        builder.setSingleChoiceItems(items, defaultItem,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SelectedItems.clear();
                        SelectedItems.add(which);
                    }
                });
        builder.setPositiveButton("Select",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Integer r = Radius;

                        if (!SelectedItems.isEmpty()) {
                            int index = (int) SelectedItems.get(0);
                            Radius = ListItems.get(index);
                        }
                        Toast.makeText(getApplicationContext(),
                                "Radius is changed to "+ Radius , Toast.LENGTH_SHORT)
                                .show();
                    }
                });
        builder.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        builder.show();
    }

    private void exitOptionsDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.exit)
                .setMessage(R.string.app_exit_message)
                .setNegativeButton(R.string.str_no,
                        new DialogInterface.OnClickListener() {
                            public void onClick(
                                    DialogInterface dialoginterface, int i) {
                            }
                        })
                .setPositiveButton(R.string.str_ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(
                                    DialogInterface dialoginterface, int i) {
                                finish();
                            }
                        }).show();
    }

    private void openOptionsDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.about)
                .setMessage(R.string.app_about_message)
                .setPositiveButton(R.string.str_ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(
                                    DialogInterface dialoginterface, int i) {
                            }
                        }).show();

    }

    private void startLocationUpdates() {

        if (!checkLocationServicesStatus()) {

            Log.d(TAG, "startLocationUpdates : call showDialogForLocationServiceSetting");
            showDialogForLocationServiceSetting();
        } else {

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                Log.d(TAG, "startLocationUpdates : 퍼미션 안가지고 있음");
                return;
            }

            Log.d(TAG, "startLocationUpdates : call FusedLocationApi.requestLocationUpdates");
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient
                    , locationRequest, this);
            mRequestingLocationUpdates = true;

            mGoogleMap.setMyLocationEnabled(true);
        }
    }

    private void stopLocationUpdates() {

        Log.d(TAG, "stopLocationUpdates : LocationServices.FusedLocationApi.removeLocationUpdates");
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        mRequestingLocationUpdates = false;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "onMapReady :");
        mGoogleMap = googleMap;
        setDefaultLocation();



        mGoogleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {

                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(marker.getPosition())
                        .zoom(17)
                        .tilt(30)
                        .build();
                mGoogleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

                marker.showInfoWindow();

                return true;
            }
        });


        mGoogleMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {


            @Override
            public void onInfoWindowClick(Marker marker) {

                Intent intent = new Intent(getBaseContext(), information.class);

                String title = marker.getTitle();
                String address = marker.getSnippet();
                String Category= marker.getSnippet();


                intent.putExtra("title",title);
                intent.putExtra("address",address);


                startActivity(intent);

            }

        });

        //
        mGoogleMap.getUiSettings().setMyLocationButtonEnabled(true);
        mGoogleMap.getUiSettings().setZoomControlsEnabled(true);
        mGoogleMap.getUiSettings().setCompassEnabled(true);
        mGoogleMap.animateCamera(CameraUpdateFactory.zoomTo(15));
        mGoogleMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {

            @Override
            public boolean onMyLocationButtonClick() {

                Log.d(TAG, "onMyLocationButtonClick : 위치에 따른 카메라 이동 활성화");
                mMoveMapByAPI = true;
                return true;
            }
        });

        mGoogleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                Log.d(TAG, "onMapClick");
            }
        });
        mGoogleMap.setOnCameraMoveStartedListener(new GoogleMap.OnCameraMoveStartedListener() {

            @Override
            public void onCameraMoveStarted(int i) {

                if (mMoveMapByUser == true && mRequestingLocationUpdates) {

                    Log.d(TAG, "onCameraMove : 위치에 따른 카메라 이동 비활성화");
                    mMoveMapByAPI = false;
                }
                mMoveMapByUser = true;
            }
        });

        mGoogleMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {

                mPosition = mGoogleMap.getCameraPosition().target;
            }
        });


        mGoogleMap.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {

            @Override
            public void onCameraMove() {

            }
        });
    }

    @Override
    public void onLocationChanged(Location location) {

        currentPosition
                = new LatLng(location.getLatitude(), location.getLongitude());

        Log.d(TAG, "onLocationChanged : ");

        final String markerTitle = getCurrentAddress(currentPosition);
        final String markerSnippet = "위도:" + String.valueOf(location.getLatitude())
                + " 경도:" + String.valueOf(location.getLongitude());

        //현재 위치에 마커 생성하고 이동
        setCurrentLocation(location, markerTitle, markerSnippet);

        mCurrentLocation = location;

        search = (AutoCompleteTextView)findViewById(R.id.search);

        if(search != null) {
            adapter = new MyPlacesAdapter(MainActivity.this, currentPosition);
            search.setAdapter(adapter);
            final ImageButton search_button = (ImageButton) findViewById(R.id.btn_search);

            search.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    //calling getfilter to filter the results
                    adapter.getFilter().filter(s);
                    //notify the adapters after results changed
                    adapter.notifyDataSetChanged();
                }

                @Override
                public void afterTextChanged(final Editable s) {
                    search_button.setOnClickListener(new View.OnClickListener(){
                        @Override
                        public void onClick(View v) {
                            adapter.getFilter().filter(s);
                            adapter.notifyDataSetChanged();
                            MyGooglePlaces googlePlaces = (MyGooglePlaces)adapter.getItem(0);
                            search.setText(googlePlaces.getName());

                            LatLng latlng = new LatLng(googlePlaces.getLat(), googlePlaces.getLng());
                            CameraPosition Latlng = new CameraPosition.Builder()
                                    .target(latlng)
                                    .zoom(17)
                                    .tilt(30)
                                    .build();
                            mGoogleMap.animateCamera(CameraUpdateFactory.newCameraPosition(Latlng));

                            if (currentMarker != null) currentMarker.remove();

                            LatLng currentLatLng = new LatLng(googlePlaces.getLat(), googlePlaces.getLng());

                            String markerSnippet = getCurrentAddress(latlng);

                            MarkerOptions markerOptions = new MarkerOptions();
                            markerOptions.position(currentLatLng);
                            markerOptions.title(googlePlaces.getName());



                            if (googlePlaces.getOpen() == "YES") {
                                markerOptions.icon(BitmapDescriptorFactory.fromBitmap(open));
                            } else {
                                markerOptions.icon(BitmapDescriptorFactory.fromBitmap(close));
                            }
                            markerOptions.snippet(markerSnippet);
                            markerOptions.draggable(true);

                            mGoogleMap.clear();  //지도 클리어

                            if(previous_marker != null)
                                previous_marker.clear(); //지역정보 마커 클리어

                            Marker item = mGoogleMap.addMarker(markerOptions);
                            previous_marker.add(item);

                        }
                    });


                }
            });
            // handling click of autotextcompleteview items
            search.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {


                    MyGooglePlaces googlePlaces = (MyGooglePlaces) parent.getItemAtPosition(position);
                    search.setText(googlePlaces.getName());
                    // final AutoCompletePrediction item = MyPlacesAdapter.getPredictions();

                    LatLng latlng = new LatLng(googlePlaces.getLat(), googlePlaces.getLng());
                    CameraPosition Latlng = new CameraPosition.Builder()
                            .target(latlng)
                            .zoom(17)
                            .tilt(30)
                            .build();
                    mGoogleMap.animateCamera(CameraUpdateFactory.newCameraPosition(Latlng));

                    if (currentMarker != null) currentMarker.remove();


                    LatLng currentLatLng = new LatLng(googlePlaces.getLat(), googlePlaces.getLng());

                    MarkerOptions markerOptions = new MarkerOptions();
                    markerOptions.position(currentLatLng);
                    markerOptions.title(googlePlaces.getName());

                    if (googlePlaces.getOpen() == "YES") {
                        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(open));
                    } else {
                        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(close));
                    }
                    markerOptions.snippet(getCurrentAddress(latlng));
                    markerOptions.draggable(true);

                    mGoogleMap.clear();  //지도 클리어

                    if(previous_marker != null)
                        previous_marker.clear(); //지역정보 마커 클리어

                    Marker item = mGoogleMap.addMarker(markerOptions);
                    previous_marker.add(item);
                    ;

                }
            });
            Log.d(TAG, "search is succeed");
        } else {
            Log.d(TAG, "search is failed");
        }
    }

    @Override
    protected void onStart() {

        if (mGoogleApiClient != null && mGoogleApiClient.isConnected() == false) {

            Log.d(TAG, "onStart: mGoogleApiClient connect");
            mGoogleApiClient.connect();
        }

        super.onStart();

    }

    @Override
    protected void onStop() {

        if (mRequestingLocationUpdates) {

            Log.d(TAG, "onStop : call stopLocationUpdates");
            stopLocationUpdates();
        }

        if (mGoogleApiClient.isConnected()) {

            Log.d(TAG, "onStop : mGoogleApiClient disconnect");
            mGoogleApiClient.disconnect();
        }

        super.onStop();

    }

    @Override
    public void onConnected(Bundle connectionHint) {

        if (mRequestingLocationUpdates == false) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                int hasFineLocationPermission = ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);

                if (hasFineLocationPermission == PackageManager.PERMISSION_DENIED) {
                    ActivityCompat.requestPermissions(mActivity,
                            new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);

                } else {

                    Log.d(TAG, "onConnected : 퍼미션 가지고 있음");
                    Log.d(TAG, "onConnected : call startLocationUpdates");
                    startLocationUpdates();
                    mGoogleMap.setMyLocationEnabled(true);
                }

            } else {

                Log.d(TAG, "onConnected : call startLocationUpdates");
                startLocationUpdates();
                mGoogleMap.setMyLocationEnabled(true);
            }
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed");
        setDefaultLocation();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(TAG, "onConnectionSuspended");
        if (cause == CAUSE_NETWORK_LOST)
            Log.e(TAG, "onConnectionSuspended(): Google Play services " +
                    "connection lost. Cause: network lost.");
        else if (cause == CAUSE_SERVICE_DISCONNECTED)
            Log.e(TAG, "onConnectionSuspended(): Google Play services " +
                    "connection lost. Cause: service disconnected");
    }


    public String getCurrentAddress(LatLng latlng) {
        //지오코더... GPS를 주소로 변환
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        List<Address> addresses;

        try {
            addresses = geocoder.getFromLocation(
                    latlng.latitude,
                    latlng.longitude,
                    1);
        } catch (IOException ioException) {
            //네트워크 문제
            Toast.makeText(this, "지오코더 서비스 사용불가", Toast.LENGTH_LONG).show();
            return "지오코더 서비스 사용불가";
        } catch (IllegalArgumentException illegalArgumentException) {
            Toast.makeText(this, "잘못된 GPS 좌표", Toast.LENGTH_LONG).show();
            return "잘못된 GPS 좌표";
        }

        if (addresses == null || addresses.size() == 0) {
            Toast.makeText(this, "주소 미발견", Toast.LENGTH_LONG).show();
            return "주소 미발견";

        } else {
            Address address = addresses.get(0);

            TextView textView = (TextView) findViewById(R.id.textView2);
            if(address.getLocality() != null && address.getLocality().length()>0) {
                textView.setText(address.getAdminArea() + " " + address.getLocality() + " " + address.getThoroughfare());
            } else if (address.getSubLocality() != null && address.getSubLocality().length() >0){
                textView.setText(address.getAdminArea() + " " + address.getSubLocality() + " " + address.getThoroughfare());
            } else {
                textView.setText(address.getAdminArea() + " " + address.getThoroughfare());
            }
            return address.getAddressLine(0);
        }
    }

    public boolean checkLocationServicesStatus() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)  //GPS로 현재위치 확인
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER); //기지국으로부터 현재위치-둘중하나만있어도됨

    }

    public void setCurrentLocation(Location location, String markerTitle, String markerSnippet) {

        if(currentMarker!=null) currentMarker.remove();

        mMoveMapByUser = false;


        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());


        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(currentLatLng);
        markerOptions.title(markerTitle);

        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(current));
        markerOptions.snippet(markerSnippet);
        markerOptions.draggable(true);

        currentMarker = mGoogleMap.addMarker(markerOptions);

        if (mMoveMapByAPI) {

            Log.d(TAG, "setCurrentLocation : mGoogleMap moveCamera "
                    + location.getLatitude() + " " + location.getLongitude());
            // CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(currentLatLng, 15);
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLng(currentLatLng);
            mGoogleMap.moveCamera(cameraUpdate);
        }
    }

    public void setDefaultLocation() {

        mMoveMapByUser = false;

        //디폴트 위치, Seoul
        LatLng DEFAULT_LOCATION = new LatLng(37.56, 126.97);
        String markerTitle = "위치정보 가져올 수 없음";
        String markerSnippet = "위치 퍼미션과 GPS 활성 요부 확인하세요";

        if (currentMarker != null) currentMarker.remove();

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(DEFAULT_LOCATION);
        markerOptions.title(markerTitle);
        markerOptions.snippet(markerSnippet);

        markerOptions.draggable(true);
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(current));;
        currentMarker = mGoogleMap.addMarker(markerOptions);

        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, 15);
        mGoogleMap.moveCamera(cameraUpdate);

    }

    //여기부터는 런타임 퍼미션 처리을 위한 메소드들
    @TargetApi(Build.VERSION_CODES.M)
    private void checkPermissions() {
        boolean fineLocationRationale = ActivityCompat
                .shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);
        int hasFineLocationPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);

        if (hasFineLocationPermission == PackageManager
                .PERMISSION_DENIED && fineLocationRationale)
            showDialogForPermission("앱을 실행하려면 퍼미션을 허가하셔야합니다.");

        else if (hasFineLocationPermission
                == PackageManager.PERMISSION_DENIED && !fineLocationRationale) {
            showDialogForPermissionSetting("퍼미션 거부 + Don't ask again(다시 묻지 않음) " +
                    "체크 박스를 설정한 경우로 설정에서 퍼미션 허가해야합니다.");
        } else if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED) {

            Log.d(TAG, "checkPermissions : 퍼미션 가지고 있음");
            if (mGoogleApiClient.isConnected() == false) {

                Log.d(TAG, "checkPermissions : 퍼미션 가지고 있음");
                mGoogleApiClient.connect();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int permsRequestCode,
                                           String[] permissions,
                                           int[] grantResults) {

        if (permsRequestCode
                == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION && grantResults.length > 0) {

            boolean permissionAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (permissionAccepted) {
                if (mGoogleApiClient.isConnected() == false) {
                    Log.d(TAG, "onRequestPermissionsResult : mGoogleApiClient connect");
                    mGoogleApiClient.connect();
                }
            } else {
                checkPermissions();
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void showDialogForPermission(String msg) {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("알림");
        builder.setMessage(msg);
        builder.setCancelable(false);
        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                ActivityCompat.requestPermissions(mActivity,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            }
        });

        builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        });
        builder.create().show();
    }

    private void showDialogForPermissionSetting(String msg) {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("알림");
        builder.setMessage(msg);
        builder.setCancelable(true);
        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

                askPermissionOnceAgain = true;

                Intent myAppSettings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + mActivity.getPackageName()));
                myAppSettings.addCategory(Intent.CATEGORY_DEFAULT);
                myAppSettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mActivity.startActivity(myAppSettings);
            }
        });
        builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        });
        builder.create().show();
    }

    private void showDialogForLocationServiceSetting() {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("위치 서비스 비활성화");
        builder.setMessage("앱을 사용하기 위해서는 위치 서비스가 필요합니다.\n"
                + "위치 설정을 수정하실래요?");
        builder.setCancelable(true);
        builder.setPositiveButton("설정", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Intent callGPSSettingIntent
                        = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.create().show();
    }

    public void showInformation(String type, LatLng latLng){

        mGoogleMap.clear();  //지도 클리어

        if(previous_marker != null)
            previous_marker.clear(); //지역정보 마커 클리어

        places = getPredictions(type, latLng);

        task = new BackgroundTask();
        task.execute(places);

        for (MyGooglePlaces poi : places) {
            if (poi.getOpen() != "Not Known") {
                LatLng latlng = new LatLng(poi.getLat(), poi.getLng());

                String markerSnippet = getCurrentAddress(latlng)+"\n영업시간 : "+poi.getWeekday()+"\n전화번호 : "+poi.getPhone_number();


                MarkerOptions markerOptions = new MarkerOptions();

                markerOptions.position(latlng);
                markerOptions.title(poi.getName());

                markerOptions.snippet(markerSnippet);

                if (poi.getOpen() == "YES") {
                    markerOptions.icon(BitmapDescriptorFactory.fromBitmap(open));
                } else {
                    markerOptions.icon(BitmapDescriptorFactory.fromBitmap(close));
                }
                Marker item = mGoogleMap.addMarker(markerOptions);
                previous_marker.add(item);
            }
        }

        //중복 마커 제거
        HashSet<Marker> hashSet = new HashSet<Marker>();
        hashSet.addAll(previous_marker);
        previous_marker.clear();
        previous_marker.addAll(hashSet);

    }


    // method to get different places nearby search location
    public ArrayList<MyGooglePlaces> getPredictions(String type, LatLng latLng)
    {
        //pass your current latitude and longitude to find nearby and radius means distances from current position to places
        String API_KEY="AIzaSyCdv5hmYW2q7_AgyaruClyb5iKv1oUQSNw";
        Integer radius = Radius;
        String url= "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location="+latLng.latitude+","+latLng.longitude
                +"&radius="+radius+"&type="+type+"&key="+API_KEY;
        return getPlaces(url);
    }
    private ArrayList<MyGooglePlaces> getPlaces(String constraint)
    {
        StringBuilder sb= new StringBuilder();
        URL url;
        HttpURLConnection urlConnection = null;
        try{
            url = new URL(constraint);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setRequestProperty("Content-length","0");
            urlConnection.setUseCaches(false);
            urlConnection.setAllowUserInteraction(false);
            urlConnection.connect();
            int status = urlConnection.getResponseCode();

            switch(status) {
                case 200:
                case 201:
                    InputStream in = urlConnection.getInputStream();
                    InputStreamReader isw = new InputStreamReader(in);
                    BufferedReader br = new BufferedReader(isw);
                    String data;
                    while ((data = br.readLine()) != null) {
                        sb.append(data + "\n");
                    }
                    br.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null) urlConnection.disconnect();
        }
        return parseGoogleParse(sb.toString());
    }
    // method to parse the json returned by googleplaces api
    private ArrayList<MyGooglePlaces> parseGoogleParse(final String response) {ArrayList<MyGooglePlaces> temp = new ArrayList();
        try {
            // make an jsonObject in order to parse the response
            JSONObject jsonObject = new JSONObject(response);
            // make an jsonObject in order to parse the response
            if (jsonObject.has("results")) {
                JSONArray jsonArray = jsonObject.getJSONArray("results");
                for (int i = 0; i < jsonArray.length(); i++) {
                    MyGooglePlaces poi = new MyGooglePlaces();
                    if (jsonArray.getJSONObject(i).has("name")) {
                        poi.setName(jsonArray.getJSONObject(i).optString("name"));
                        poi.setId(jsonArray.getJSONObject(i).optString("place_id"));
                        if (jsonArray.getJSONObject(i).has("opening_hours")) {
                            if (jsonArray.getJSONObject(i).getJSONObject("opening_hours").has("open_now")) {
                                if (jsonArray.getJSONObject(i).getJSONObject("opening_hours").getString("open_now").equals("true")) {
                                    poi.setOpenNow("YES");
                                } else {
                                    poi.setOpenNow("NO");
                                }
                            }
                        } else {
                            poi.setOpenNow("Not Known");
                        }
                        if (jsonArray.getJSONObject(i).has("geometry")) {
                            if (jsonArray.getJSONObject(i).getJSONObject("geometry").has("location"))
                            {
                                if (jsonArray.getJSONObject(i).getJSONObject("geometry").getJSONObject("location").has("lat"))
                                {
                                    poi.setLatLng(Double.parseDouble(jsonArray.getJSONObject(i).getJSONObject("geometry").getJSONObject("location").getString("lat")), Double.parseDouble(jsonArray.getJSONObject(i).getJSONObject("geometry").getJSONObject("location").getString("lng")));
                                }
                            }
                        }
                        if (jsonArray.getJSONObject(i).has("photos")) {
                            JSONArray photosArray = jsonArray.getJSONObject(i).getJSONArray("photos");
                            for (int j = 0; j < photosArray.length(); j++) {
                                if(photosArray.getJSONObject(j).has("photo_reference")){
                                    poi.setPhoto_ref(photosArray.getJSONObject(j).optString("photo_reference"));
                                }
                            }
                        }
                        if (jsonArray.getJSONObject(i).has("types")) {
                            JSONArray typesArray = jsonArray.getJSONObject(i).getJSONArray("types");
                            for (int j = 0; j < typesArray.length(); j++) {
                                poi.setCategory(typesArray.getString(j) + ", " + poi.getCategory());
                            }
                        }
                    }
                    //if(temp.size()<5)
                    temp.add(poi);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList();
        }
        return temp;
    }
    // API 사용


    private BitmapDescriptor getMarkerIcon(String color) {
        float[] hsv = new float[3];
        Color.colorToHSV(Color.parseColor(color), hsv);
        return BitmapDescriptorFactory.defaultMarker(hsv[0]);
    }

    @Override
    protected void onActivityResult ( int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {

            case GPS_ENABLE_REQUEST_CODE:

                //사용자가 GPS 활성 시켰는지 검사
                if (checkLocationServicesStatus()) {


                    Log.d(TAG, "onActivityResult : 퍼미션 가지고 있음");

                    if (mGoogleApiClient.isConnected() == false) {

                        Log.d(TAG, "onActivityResult : mGoogleApiClient connect ");
                        mGoogleApiClient.connect();
                    }
                    return;
                }
        }
    }


    class BackgroundTask extends AsyncTask<ArrayList<MyGooglePlaces>, Void, Void> {

        @Override
        protected Void doInBackground(ArrayList<MyGooglePlaces>... arrayLists) {
            while(isCancelled()==false){
                for (MyGooglePlaces poi : arrayLists[0]) {
                    getDetails(poi);}
            }
            return null;
        }

        public void getDetails(MyGooglePlaces poi)
        {
            //pass your current latitude and longitude to find nearby and radius means distances from current position to places
            String API_KEY="AIzaSyCdv5hmYW2q7_AgyaruClyb5iKv1oUQSNw";
            String url = "https://maps.googleapis.com/maps/api/place/details/json?place_id="+poi.getId()+"&fields=opening_hours,formatted_phone_number&key="+API_KEY;
            getPlaceDetails(url, poi);
        }
        private void getPlaceDetails(String constraint, MyGooglePlaces poi)
        {
            StringBuilder sb= new StringBuilder();
            URL url;
            HttpURLConnection urlConnection = null;
            try{
                url = new URL(constraint);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setRequestProperty("Content-length","0");
                urlConnection.setUseCaches(false);
                urlConnection.setAllowUserInteraction(false);
                urlConnection.connect();
                int status = urlConnection.getResponseCode();

                switch(status) {
                    case 200:
                    case 201:
                        InputStream in = urlConnection.getInputStream();
                        InputStreamReader isw = new InputStreamReader(in);
                        BufferedReader br = new BufferedReader(isw);
                        String data;
                        while ((data = br.readLine()) != null) {
                            sb.append(data + "\n");
                        }
                        br.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (urlConnection != null) urlConnection.disconnect();
            }
            parseGoogleParse2(sb.toString(), poi);
        }
        // method to parse the json returned by googleplaces api
        private void parseGoogleParse2(final String response, MyGooglePlaces poi) {
            try {
                // make an jsonObject in order to parse the response
                JSONObject jsonObject = new JSONObject(response);
                // make an jsonObject in order to parse the response
                if (jsonObject.has("results")) {
                    JSONArray jsonArray = jsonObject.getJSONArray("results");
                    for (int i = 0; i < jsonArray.length(); i++) {
                        poi.setPhone_number(jsonArray.getJSONObject(i).optString("formatted_phone_number"));
                        if (jsonArray.getJSONObject(i).has("opening_hours")) {
                            if (jsonArray.getJSONObject(i).getJSONObject("opening_hours").has("weekday_text")) {
                                JSONArray textArray = jsonArray.getJSONObject(i).getJSONArray("weekday_text");
                                for (int j = 0; j < textArray.length(); j++) {
                                    poi.setWeekday(textArray.getString(j) + ",\n" + poi.getWeekday());
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
