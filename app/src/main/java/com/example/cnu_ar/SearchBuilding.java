package com.example.cnu_ar;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import androidx.annotation.NonNull;

public class SearchBuilding extends Activity {

    private ListView listView;          // 검색을 보여줄 리스트변수
    private EditText editSearch;        // 검색어를 입력할 Input 창
    private ListAdapter adapter;      // 리스트뷰에 연결할 아답터
    private ArrayList<String> arrayList, buildList, pointList, point = new ArrayList<>();
    private ArrayList<Integer> road, reverse;
    private double [][] maps;
    private GpsTracker gpsTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_building);
        setTitle("장소 검색");

        editSearch = findViewById(R.id.searchText);
        listView = findViewById(R.id.buildingList);

        buildList = new ArrayList<>();
        pointList = new ArrayList<>();

        settingList();

        Intent intent = getIntent();
        point = intent.getStringArrayListExtra("point");
        maps = (double[][]) getIntent().getSerializableExtra("maps");

        // input창에 글자를 입력할 때 리스너
        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // input창에 문자를 입력하면 search 메소드를 호출
                String text = editSearch.getText().toString();
                search(text);
            }
        });

        // 리스트뷰 클릭 리스너. 리스트를 누르면 건물명과 좌표로 AR을 실행한다.
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            int PointNum, tempPointNum, endPointNum,tempEndPointNum;
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String tempBuildList = buildList.get(i).split(" ")[0];
                int j;
                DijkstraGraph dijkstraGraph = new DijkstraGraph(point.size(), maps);
                double lat,log,mlat,mlog, tempDistance = Double.MAX_VALUE,tempDistance2 = Double.MAX_VALUE;
                gpsTracker = new GpsTracker(SearchBuilding.this);

                mlat = gpsTracker.getLatitude();
                mlog = gpsTracker.getLongitude();

                for (j = 0; j < arrayList.size(); j++) {
                    if (arrayList.get(j).contains(tempBuildList)) {
                        Log.d("검색좌표", String.valueOf(j));
                        break;
                    }
                }

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
                    if (tempDistance2 >=  Math.sqrt(Math.pow(Double.parseDouble(pointList.get(j).split("#")[0])-lat,2) + Math.pow(Double.parseDouble(pointList.get(j).split("#")[1])-log,2))) {
                        tempDistance2 = Math.sqrt(Math.pow(Double.parseDouble(pointList.get(j).split("#")[0]) - lat, 2) + Math.pow(Double.parseDouble(pointList.get(j).split("#")[1]) - log, 2));
                        endPointNum = tempEndPointNum;
                    }
                }

                road = dijkstraGraph.dijkstra(PointNum, endPointNum);
                road.remove(road.size()-1);
                road.add(road.size(), PointNum);
                reverse = new ArrayList<Integer>();
                for(int q = road.size()-1; q >= 0; q--){
                    reverse.add(road.get(q));
                }
                String tempPointList = pointList.get(j);
                Log.d("검색좌표", tempPointList);

                SharedPreferences pref = getSharedPreferences("list", 0);
                SharedPreferences.Editor editor = pref.edit();

                String temp = pref.getString("recent", "");
                temp += tempBuildList + "#";
                editor.putString("recent", temp);

                String tempPoint = pref.getString("point", "");
                tempPoint += tempPointList + "/";
                editor.putString("point", tempPoint);
                editor.commit();


                Intent intent = new Intent(getApplicationContext(), ARActivity.class);
                intent.putExtra("buildName", tempBuildList);
                intent.putExtra("lati", Double.valueOf(pointList.get(j).split("#")[0]));
                intent.putExtra("long", Double.valueOf(pointList.get(j).split("#")[1]));
                intent.putExtra("road", reverse);
                intent.putExtra("buildLilst",buildList);
                intent.putExtra("pointList", pointList);
                intent.putExtra("point",point);
                startActivity(intent);
            }
        });
    }

    // 검색을 수행하는 메소드
    public void search(String charText) {

        buildList.clear();

        // 문자 입력이 없을때는 모든 데이터를 보여줌
        if (charText.length() == 0) {
            buildList.clear();
        }
        else {
            // 리스트의 모든 데이터를 검색
            for (int i = 0; i < arrayList.size(); i++) {
                // arraylist의 모든 데이터에 입력받은 char가 포함되어 있으면 true를 반환
                if (arrayList.get(i).contains(charText)) {
                    // 검색된 데이터를 리스트에 추가한다.
                    buildList.add(arrayList.get(i));
                }
            }
        }
        // 리스트 데이터가 새로고침
        adapter.notifyDataSetChanged();
    }

    // 리스트뷰에 표시할 데이터를 DB에서 받아와 리스트에 넣음
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

                adapter = new ListAdapter(buildList, getApplicationContext());
                listView.setAdapter(adapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }
}
