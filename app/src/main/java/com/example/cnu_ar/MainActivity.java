package com.example.cnu_ar;

import android.content.Intent;
import android.os.Bundle;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import java.util.ArrayList;

// 어플리케이션 첫 화면 클래스. 버튼을 누르면 나올 메뉴 구성.
public class MainActivity extends AppCompatActivity{
    private EditText searchText;
    private ArrayList<String> buildList/*건물명, 건물 번호*/, pointList/*건물 좌표*/, arrayList, point = new ArrayList<>();// 데이터를 넣을 리스트 변수
    private ImageButton map, last, info, set;
    private double [][] maps;
    private GpsTracker gpsTracker;
    private ArrayList<Integer> road, reverse;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {//상단 액션바 메뉴 생성
        getMenuInflater().inflate(R.menu.appbar_action, menu) ;
        return true ;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {//액션바 메뉴 선택시 실행
        switch (item.getItemId()) {
            case R.id.searchLayout:
                Intent intent3 = new Intent(this, SearchBuilding.class);
                Bundle mBundle = new Bundle();
                mBundle.putSerializable("maps", maps);
                intent3.putExtras(mBundle);
                intent3.putStringArrayListExtra("point",point);
                startActivity(intent3);
                return true ;
            default :
                return super.onOptionsItemSelected(item) ;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Intent loading = new Intent(this, LoadingActivity.class);
        startActivity(loading);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle("충남대학교 AR 안내 어플리케이션");

        searchText = findViewById(R.id.searchText);
        map = findViewById(R.id.map);
        last = findViewById(R.id.last);
        info = findViewById(R.id.info);
        set = findViewById(R.id.set);

        buildList = new ArrayList<String>();
        pointList = new ArrayList<String>();

        settingList();
        mapping();

        map.setOnClickListener(new EditText.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent = new Intent(getApplicationContext(), MapsActivity.class);
                Bundle mBundle = new Bundle();
                mBundle.putSerializable("maps", maps);
                intent.putExtras(mBundle);
                startActivity(intent);
            }
        });
        last.setOnClickListener(new EditText.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent = new Intent(getApplicationContext(), RecentActivity.class);
                intent.putExtra("buildList",buildList);
                intent.putExtra("pointList", pointList);
                intent.putExtra("point",point);
                Bundle mBundle = new Bundle();
                mBundle.putSerializable("maps", maps);
                intent.putExtras(mBundle);
                startActivity(intent);
            }
        });
        info.setOnClickListener(new EditText.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent = new Intent(
                        getApplicationContext(), InfoActivity.class);
                startActivity(intent);
            }
        });
        set.setOnClickListener(new EditText.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent = new Intent(
                        getApplicationContext(), MapsActivity.class);/////////////
                startActivity(intent);
            }
        });
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
        finish();
    }
    @Override
    protected  void onStop(){
        super.onStop();
    }

    private void settingList() {
        // FireBase DB 객체
        final FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();
        final DatabaseReference ref = mDatabase.getReference().child("buildInfo").child("0");

        Query query = ref.orderByKey();

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    if(String.valueOf(postSnapshot.child("buildingNum").getValue()).equals("")){
                        buildList.add(String.valueOf(postSnapshot.child("college").getValue()));
                    }else if(String.valueOf(postSnapshot.child("major").getValue()).equals("")){
                        buildList.add(String.valueOf(postSnapshot.child("college").getValue())+" "+ String.valueOf(postSnapshot.child("buildingNum").getValue()));
                    }else{
                        buildList.add(String.valueOf(postSnapshot.child("college").getValue())+" "+ String.valueOf(postSnapshot.child("buildingNum").getValue()) + " "+ String.valueOf(postSnapshot.child("major").getValue()));
                    }
                    pointList.add(String.valueOf(postSnapshot.child("latitude").getValue())+"#"+ String.valueOf(postSnapshot.child("longitude").getValue()));//건물좌표
                }

                arrayList = new ArrayList<>();
                arrayList.addAll(buildList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void mapping() {
        final FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();
        final DatabaseReference PointRef = mDatabase.getReference().child("coordinate");
        Query PointQuery = PointRef.orderByKey();

        PointQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()){
                    point.add(String.valueOf(postSnapshot.child("latitude").getValue()) + "#" + String.valueOf(postSnapshot.child("longitude").getValue()));
                }
                maps = new double[point.size()][point.size()];
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    int children = (int) postSnapshot.child("nodeList").getChildrenCount();

                    for(int c = 0; c < children; c++){
                        String[] a = point.get(Integer.parseInt(String.valueOf(postSnapshot.child("pointNum").getValue()).substring(1))).split("#");
                        String[] b = point.get(Integer.parseInt(String.valueOf(postSnapshot.child("nodeList").child("node" + Integer.toString(c+1)).getValue()).substring(1))).split("#");

                        maps[Integer.parseInt(String.valueOf(postSnapshot.child("pointNum").getValue()).substring(1))][Integer.parseInt(String.valueOf(postSnapshot.child("nodeList").child("node" + Integer.toString(c+1)).getValue()).substring(1))] = Math.sqrt(Math.pow(Double.parseDouble(a[0])-Double.parseDouble(b[0]),2)+ Math.pow(Double.parseDouble(a[1])-Double.parseDouble(b[1]),2));
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}
