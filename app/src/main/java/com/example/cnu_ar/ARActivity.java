package com.example.cnu_ar;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.core.content.ContextCompat;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableException;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.rendering.ViewRenderable;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import uk.co.appoly.arcorelocation.LocationMarker;
import uk.co.appoly.arcorelocation.LocationScene;
import uk.co.appoly.arcorelocation.rendering.LocationNode;
import uk.co.appoly.arcorelocation.rendering.LocationNodeRender;
import uk.co.appoly.arcorelocation.utils.ARLocationPermissionHelper;
import uk.co.appoly.arcorelocation.utils.LocationUtils;

public class ARActivity extends FragmentActivity implements OnMapReadyCallback {
    private boolean installRequested;
    private boolean hasFinishedLoading = false;

    // 카메라 화면 객체
    private ArSceneView arSceneView;

    // View를 AR로 만든 객체
    private ViewRenderable exampleLayoutRenderable;

    // 특정 GPS 좌표에 AR을 표시하기 위한 객체
    private LocationScene locationScene;

    // 화살표 ImageView 객체 및 변수
    private ImageView arrImg;
    private double tarLati, tarLong;
    private float bearing;

    // 건물 명 변수
    private String build;

    //자신의 위치를 가져오는 변수
    private GpsTracker gpsTracker;

    private ArrayList<String> point;
    private ArrayList<Integer> road;
    private int i=0;

    private PolylineOptions polylineOptions;
    private ArrayList<LatLng> arrayPoints;
    private GoogleMap mMap;

    @Override
    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ar);
        arSceneView = findViewById(R.id.ar_scene_view);
        // 지도를 비동기적으로 실행하기 위한 메소드. 지도 로딩 완료시 콜백이 실행된다.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // 화살표 ImageView
        arrImg = findViewById(R.id.arrImg);

        // 인텐트로 건물 이름 및 좌표를 받아옴
        Intent intent = getIntent();
        build = intent.getExtras().getString("buildName");
        tarLati = Double.valueOf(intent.getExtras().getDouble("lati"));
        tarLong = Double.valueOf(intent.getExtras().getDouble("long"));
        road = intent.getExtras().getIntegerArrayList("road");
        point = intent.getExtras().getStringArrayList("point");


        // View를 3D AR로 구성하기 위한 메소드
        CompletableFuture<ViewRenderable> exampleLayout = ViewRenderable.builder()
                .setView(this, R.layout.ar_layout)
                .build();

        // 화면에 AR을 렌더링 하도록 로드하는 메소드
        CompletableFuture.allOf(exampleLayout).handle((notUsed, throwable) -> {
            if (throwable != null) {
                Utils.displayError(this, "이미지 받아오기 실패", throwable);
                return null;
            }

            try {
                exampleLayoutRenderable = exampleLayout.get();
                hasFinishedLoading = true;

            } catch (InterruptedException | ExecutionException ex) {
                Utils.displayError(this, "이미지 받아오기 실패", ex);
            }
            return null;
        });
        // 화면을 인식하면 매 프레임 새로 화면을 그리는 리스너를 지정
        arSceneView.getScene().addOnUpdateListener(frameTime -> {
            if (!hasFinishedLoading) {
                return;
            }

            Log.d("실행중", "리스너 실행중");
            // 처음 화면이 실행시 초기 설정을 실행한다
            if (locationScene == null) {
                // 화면 객체 지정
                locationScene = new LocationScene(this, arSceneView);
                locationScene.setAnchorRefreshInterval(10000);

                // 지정한 좌표(tarLong, tarLati)에 AR을 추가한다.
                LocationMarker layoutLocationMarker = new LocationMarker(tarLong, tarLati, getView());

                Timer timer = new Timer();

                // 매 프레임 AR을 업데이트 하는 이벤트.
                layoutLocationMarker.setRenderEvent(new LocationNodeRender() {
                    @Override
                    public void render(LocationNode node) {
                        View eView = exampleLayoutRenderable.getView();
                        TextView buildName = eView.findViewById(R.id.textView1);
                        buildName.setText(build); // 건물 이름 지정
                        TextView distanceTextView = eView.findViewById(R.id.textView2); // 건물과의 거리 표시
                        distanceTextView.setText(node.getDistance() + "M"); // getDistance() 메소드로 거리를 계산한다.

                        Log.d("실행중", "실행중");
                        Handler handler = new Handler() {
                            public void handleMessage(Message msg) {
                                super.handleMessage(msg);
                                String[] a = msg.obj.toString().split("#");
                                double c1 = Double.parseDouble(a[0]);
                                double d1 = Double.parseDouble(a[1]);

                                bearing = (float) LocationUtils.bearing(locationScene.deviceLocation.currentBestLocation.getLatitude(),
                                        locationScene.deviceLocation.currentBestLocation.getLongitude(), c1, d1);
                                //
                                // getOrientation() 메소드로 현재 카메라의 방향을 구한다.
                                // 방위각과 현재 카메라가 회전한 방향의 차이만큼 화살표를 회전시킨다.
                                //
                                float markerBearing = bearing - locationScene.deviceOrientation.getOrientation();
                                markerBearing = markerBearing + 360;
                                markerBearing = markerBearing % 360;
                                double rotation = Math.floor(markerBearing);
                                arrImg.setRotation((float) rotation - 90);
                                arrImg.setRotationX(60);
                            }
                        };

                        String[] a = point.get(road.get(i)).split("#");
                        // 화살표 네비게이션의 회전을 위해 현재 지점과 AR이 표시된 지점의 방위각을 구한다.
                        double a1 = locationScene.deviceLocation.currentBestLocation.getLatitude();//현재 위도
                        double b1 = locationScene.deviceLocation.currentBestLocation.getLongitude();//현재 경도
                        double c1 = Double.parseDouble(a[0]);//경유지점 위도
                        double d1 = Double.parseDouble(a[1]);//경유지점 경도
                        Message message = handler.obtainMessage();

                        if (7 >= Math.sqrt(Math.pow((a1 - c1) * 110972, 2) + Math.pow((b1 - d1) * 89431, 2))) {
                            i++;
                            if (i == road.size()) {
                                message.obj = Double.toString(tarLati)+"#"+Double.toString(tarLong);
                                timer.cancel();
                            } else {
                                message.obj = point.get(road.get(i));
                                handler.sendMessage(message);
                            }
                        }else{
                            message.obj = point.get(road.get(i));
                            handler.sendMessage(message);
                        }
                    }
                });
                locationScene.mLocationMarkers.add(layoutLocationMarker);
            }

            Frame frame = arSceneView.getArFrame();
            if (frame == null) {
                return;
            }

            // ARcore는 AR 표시를 위해 평면 인식과 화면 밝기등을 확인한다.
            if (frame.getCamera().getTrackingState() != TrackingState.TRACKING) {
                Log.d("트래킹 상태", String.valueOf(frame.getCamera().getTrackingState()));
                Log.d("실패 이유", String.valueOf(frame.getCamera().getTrackingFailureReason()));
                return;
            }

            if (locationScene != null) {
                locationScene.processFrame(frame);
            }

        });

        // GPS와 카메라의 권한이 없을 경우 요청한다.
        ARLocationPermissionHelper.requestPermission(this);
        Toast toast = Toast.makeText(getApplicationContext(), "지도에 보이는 빨간선이 시작되는 곳으로 이동해 주세요", Toast.LENGTH_LONG);//AR시작시 시작점 안내
        toast.show();
    }

    private Node getView() {
        Node base = new Node();
        base.setRenderable(exampleLayoutRenderable);
        Context c = this;
        View eView = exampleLayoutRenderable.getView();

        return base;
    }

    // 어플리케이션이 중지, 다시 실행됐을 때 실행할 메소드
    @Override
    protected void onResume() {
        super.onResume();

        if (locationScene != null) {
            locationScene.resume();
        }

        if (arSceneView.getSession() == null) {
            try {
                Session session = Utils.createArSession(this, installRequested);
                if (session == null) {
                    installRequested = ARLocationPermissionHelper.hasPermission(this);
                    return;
                } else {
                    arSceneView.setupSession(session);
                }
            } catch (UnavailableException e) {
                Utils.handleSessionException(this, e);
            }
        }

        try {
            arSceneView.resume();
        } catch (CameraNotAvailableException ex) {
            Utils.displayError(this, "카메라 실행 불가", ex);
            finish();
            return;
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        if (locationScene != null) {
            locationScene.pause();
        }

        arSceneView.pause();
    }

    @Override
    protected void onStop(){
        super.onStop();
        finish();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        arSceneView.destroy();
        finish();
    }

    // 어플리케이션 권한 설정 메소드. 권한이 없으면 요청한다.
    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] results) {
        if (!ARLocationPermissionHelper.hasPermission(this)) {
            if (!ARLocationPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                ARLocationPermissionHelper.launchPermissionSettings(this);
            } else {
                Toast.makeText(
                        this, "카메라 권한이 필요합니다.", Toast.LENGTH_LONG)
                        .show();
            }
            finish();
        }
    }

    // 전체화면을 설정하기 위한 메소드
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow()
                    .getDecorView()
                    .setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    // 지도 로딩시 콜백할 메소드. 충남대 중앙을 기준으로 지도를 띄우고 건물에 마커를 생성한다.
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        double latitude = 0, longitude = 0;//사용자의 현재 위치
        gpsTracker = new GpsTracker(ARActivity.this);
        latitude = gpsTracker.getLatitude();
        longitude = gpsTracker.getLongitude();

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 17));//충대 중심 lat:36.3666232, lng:127.3432415

        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
        LatLng target = new LatLng(tarLati, tarLong);
        mMap.addMarker(new MarkerOptions().position(target).title(build));//지도에 선택한 건물 위치를 마커로 표시
        drawPath(road);// 최단 경로를 지도위에 표시
    }
    public void drawPath(ArrayList<Integer> road){
        arrayPoints = new ArrayList<LatLng>();
        for(int z = 0; z<road.size();z++){
            String[] poly = point.get(road.get(z)).split("#");
            LatLng latlng = new LatLng(Double.parseDouble(poly[0]), Double.parseDouble(poly[1]));
            arrayPoints.add(latlng);
        }
        LatLng e = new LatLng(tarLati,tarLong);
        arrayPoints.add(e);
        polylineOptions = new PolylineOptions();
        polylineOptions.color(Color.RED);
        polylineOptions.width(5);
        polylineOptions.addAll(arrayPoints);
        mMap.addPolyline(polylineOptions);
    }
}
