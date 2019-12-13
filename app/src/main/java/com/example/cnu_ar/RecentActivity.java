package com.example.cnu_ar;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

// 최근 기록 표시 클래스
public class RecentActivity extends AppCompatActivity {

    private ListView listView;          // 검색을 보여줄 리스트변수
    private ListAdapter adapter;      // 리스트뷰에 연결할 아답터
    private ArrayList<String> buildList/*건물명, 건물 번호*/, pointList/*건물 좌표*/, arrayList, point = new ArrayList<>();// 데이터를 넣을 리스트 변수
    private Button delBtn;
    private SharedPreferences pref;
    private GpsTracker gpsTracker;
    private ArrayList<Integer> road, reverse;
    private double [][] maps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recent_list);
        setTitle("최근 기록");
        listView = (ListView) findViewById(R.id.recentList);
        delBtn = (Button) findViewById(R.id.recentDelBtn);

        pref = getSharedPreferences("list", 0);

        arrayList = new ArrayList<String>();

        settingList();

        // 인텐트로 건물 이름 및 좌표를 받아옴
        Intent intent = getIntent();
        buildList = intent.getExtras().getStringArrayList("buildList");
        pointList = intent.getExtras().getStringArrayList("pointList");
        point = intent.getExtras().getStringArrayList("point");
        maps = (double[][]) getIntent().getSerializableExtra("maps");

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            int PointNum, tempPointNum, endPointNum,tempEndPointNum;
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String tempBuildList = arrayList.get(i).split(" ")[0];//tempBuildList == 건물명
                int j;
                DijkstraGraph dijkstraGraph = new DijkstraGraph(point.size(), maps);
                double lat,log,mlat,mlog, tempDistance = Double.MAX_VALUE,tempDistance2 = Double.MAX_VALUE;
                gpsTracker = new GpsTracker(RecentActivity.this);

                mlat = gpsTracker.getLatitude();
                mlog = gpsTracker.getLongitude();

                for (j = 0; j < buildList.size(); j++) {
                    if (buildList.get(j).contains(tempBuildList)) {
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

        // 최근 기록 삭제 버튼 클릭시 저장된 기록을 모두 지우고 액티비티 재실행
        delBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = pref.edit();
                editor.clear();
                editor.commit();

                Intent intent = getIntent();
                finish();
                startActivity(intent);
            }
        });
    }

    // 데이터를 쉐어드프리퍼런스에서 받아와 리스트에 넣음
    private void settingList() {

        String recent = pref.getString("recent", "최근기록이 없습니다!");

        String[] tempBuilding = recent.split("#");
        for (int i = tempBuilding.length - 1; i >= 0; i--) {
            arrayList.add(tempBuilding[i]);
        }

        adapter = new ListAdapter(arrayList, getApplicationContext());
        listView.setAdapter(adapter);
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
        finish();
    }
    @Override
    protected void onStop(){
        super.onStop();
        finish();
    }
}
