package com.example.cnu_ar;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

// 지도에서 건물을 선택하는 클래스
public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    // 지도 객체
    private GoogleMap mMap;
    private ArrayList<String> point = new ArrayList<>();
    private double[][] maps;
    private DijkstraGraph dijkstraGraph;
    private GpsTracker gpsTracker;
    private ArrayList<Integer> road;
    private ArrayList<Integer> reverse;

    // FireBase DB를 사용하기 위한 객체
    private final FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();
    private final DatabaseReference buildRef = mDatabase.getReference().child("buildInfo").child("0");
    private final DatabaseReference pointRef = mDatabase.getReference().child("coordinate");
    // 액티비티가 시작되면 지도를 비 동기적으로 로딩후 콜백 실행
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        maps = (double[][]) getIntent().getSerializableExtra("maps");
        Query query = pointRef.orderByKey();
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot postSnapshot : dataSnapshot.getChildren()){
                    point.add(String.valueOf(postSnapshot.child("latitude").getValue()) + "#" + String.valueOf(postSnapshot.child("longitude").getValue()));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    // 지도 생성 콜백 메소드
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // 충남대 중앙으로 지도 이동
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(36.368679, 127.345120), 16));

        // DB에서 건물명과 좌표를 받아오고, 받아온 좌표에 지도 마커를 생성한다.
        Query query = buildRef.orderByKey();
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot data : dataSnapshot.getChildren()) {
                    LatLng temp = new LatLng(Double.parseDouble(String.valueOf(data.child("latitude").getValue())), Double.parseDouble(String.valueOf(data.child("longitude").getValue())));
                    mMap.addMarker(new MarkerOptions().position(temp).title(String.valueOf(data.child("college").getValue())));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        //마커 클릭시 실행되는 이벤트
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                Toast toast = Toast.makeText(getApplicationContext(), "건물명을 클릭하면 안내를 시작합니다.", Toast.LENGTH_LONG);
                toast.show();
                return false;
            }
        });

        // 마커 위의 말풍선 클릭시 실행되는 이벤트
        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            int PointNum, tempPointNum, endPointNum,tempEndPointNum;

            @Override
            public void onInfoWindowClick(Marker marker) {
                double lat,log,mlat,mlog, tempDistance = Double.MAX_VALUE,tempDistance2 = Double.MAX_VALUE;
                gpsTracker = new GpsTracker(MapsActivity.this);

                mlat = gpsTracker.getLatitude();
                mlog = gpsTracker.getLongitude();

                for (int k = 0; k< point.size(); k++){//현재 자신의 위치에서 가장 가까운 점과 목표에서 가장 가까운 점 찾기
                    String[] tempLocation = point.get(k).split("#");
                    tempPointNum = k;
                    tempEndPointNum = k;
                    lat = Double.parseDouble(tempLocation[0]);
                    log = Double.parseDouble(tempLocation[1]);
                    if (tempDistance >=  Math.sqrt(Math.pow(mlat-lat,2) + Math.pow(mlog-log,2))) {
                        tempDistance = Math.sqrt(Math.pow(mlat - lat, 2) + Math.pow(mlog - log, 2));
                        PointNum = tempPointNum;
                    }
                    if (tempDistance2 >=  Math.sqrt(Math.pow(marker.getPosition().latitude-lat,2) + Math.pow(marker.getPosition().longitude-log,2))) {
                        tempDistance2 = Math.sqrt(Math.pow(marker.getPosition().latitude - lat, 2) + Math.pow(marker.getPosition().longitude - log, 2));
                        endPointNum = tempEndPointNum;
                    }
                }
                dijkstraGraph = new DijkstraGraph(point.size(), maps);
                road = dijkstraGraph.dijkstra(PointNum, endPointNum);
                road.remove(road.size()-1);
                road.add(road.size(),PointNum);
                reverse = new ArrayList<Integer>();
                for(int q = road.size()-1; q >= 0; q--){
                    reverse.add(road.get(q));
                }

                // SharedPreferences를 사용해 최근 기록을 String으로 저장하고 구분자로 구분한다.
                String tempBuildList = marker.getTitle();
                String tempPointList = marker.getPosition().latitude + "#" + marker.getPosition().longitude;

                SharedPreferences pref = getSharedPreferences("list", 0);
                SharedPreferences.Editor editor = pref.edit();

                String temp = pref.getString("recent", "");
                temp += tempBuildList + "#";
                editor.putString("recent", temp);

                String tempPoint = pref.getString("point", "");
                tempPoint += tempPointList + "/";
                editor.putString("point", tempPoint);
                editor.commit();

                // 목적지 건물 이름과 좌표, 경로를 인텐트를 활용해 AR이 실행될 정보로 넘긴다.
                Intent intent = new Intent(getApplicationContext(), ARActivity.class);
                intent.putExtra("buildName", marker.getTitle());
                intent.putExtra("lati", marker.getPosition().latitude);
                intent.putExtra("long", marker.getPosition().longitude);
                intent.putExtra("road", reverse);
                intent.putExtra("point", point);
                startActivity(intent);
            }
        });

        // 현재 위치를 표시하는 메소드. GPS 권한이 필요함
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
    }

    @Override
    public void onStop(){
        super.onStop();
        finish();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        finish();
    }
}
