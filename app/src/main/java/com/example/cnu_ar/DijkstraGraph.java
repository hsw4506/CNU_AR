package com.example.cnu_ar;

import java.util.ArrayList;

public class DijkstraGraph {
    private int n;//포인트의 개수
    private double [][] maps;// 포인트 간의 거리 저장
    private ArrayList<Integer> road;// 최단 경로 저장

    public DijkstraGraph(int n, double[][] b){
        this.n = n;
        this.maps = b;
        road = new ArrayList<Integer>();
    }

    public ArrayList<Integer> dijkstra(int v, int e) {//v: 시작점, e: 도착점
        double distance[] = new double[n]; // 최단 거리를 저장할 변수
        boolean[] check = new boolean[n]; // 해당 노드를 방문했는지 체크할 변수
        int temproad[] = new int[n];//최단 경로를 임시로 저장할 변수

        // distance값, temporad 초기화
        for (int i = 0; i < n; i++) {
            distance[i] = Double.MAX_VALUE;
            temproad[i] = 0;////////////////////
        }

        // 시작노드값 초기화.
        distance[v] = 0;
        check[v] = true;

        // 연결노드 distance갱신
        for (int i = 1; i < n; i++) {
            if (!check[i] && maps[v][i] != 0) {
                distance[i] = maps[v][i];
            }
        }
       if (maps[v][e] != 0 || v==e){//시작점과 바로 연결되어 있는 점이거나 시작점과 도착점이 일치하는경우
           road.add(0,e);
           return road;
       }else {
           for (int a = 1; a < n; a++) {
               double min = Double.MAX_VALUE;
               int min_index = -1;

               if(temproad[e] != 0){
                   road.add(0,e);
                   for(int i = 0; i<temproad.length-1;i++){
                       if (road.get(i) == 0){
                           break;
                       }
                       road.add(i+1,temproad[road.get(i)]);
                   }
                   return road;
               }

               for (int i = 1; i < n; i++) {
                   if (!check[i] && distance[i] != Double.MAX_VALUE) {
                       if (distance[i] < min) {
                           min = distance[i];
                           min_index = i;
                       }
                   }
               }
               check[min_index] = true;//min_index가 -1인 경우는 위의 for문에서 최소값을 찾지 못한 경우

               for (int i = 1; i < n; i++) {
                   if (!check[i] && maps[min_index][i] != 0) {// 체크하지 않았고 연결되어있는 점이면
                       if (distance[i] > distance[min_index] + maps[min_index][i]) {
                           distance[i] = distance[min_index] + maps[min_index][i];
                           temproad[i] = min_index;
                       }
                   }
               }
           }
           // 경로 출력
           road.add(0,e);
           for(int i = 0; i<temproad.length-1;i++) {
               if (road.get(i)==0){
                   break;
               }
               road.add(i+1,temproad[road.get(i)]);
           }
           return road;
       }
    }
}
