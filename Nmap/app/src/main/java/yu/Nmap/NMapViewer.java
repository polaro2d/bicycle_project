package yu.Nmap;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.Button;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.Socket;


import com.nhn.android.maps.NMapActivity;
import com.nhn.android.maps.NMapCompassManager;
import com.nhn.android.maps.NMapController;
import com.nhn.android.maps.NMapLocationManager;
import com.nhn.android.maps.NMapOverlay;
import com.nhn.android.maps.NMapOverlayItem;
import com.nhn.android.maps.NMapView;
import com.nhn.android.maps.maplib.NGeoPoint;
import com.nhn.android.maps.nmapmodel.NMapError;
import com.nhn.android.maps.nmapmodel.NMapPlacemark;
import com.nhn.android.maps.overlay.NMapCircleData;
import com.nhn.android.maps.overlay.NMapCircleStyle;
import com.nhn.android.maps.overlay.NMapPOIdata;
import com.nhn.android.maps.overlay.NMapPOIitem;
import com.nhn.android.maps.overlay.NMapPathData;
import com.nhn.android.maps.overlay.NMapPathLineStyle;
import com.nhn.android.mapviewer.overlay.NMapCalloutCustomOverlay;
import com.nhn.android.mapviewer.overlay.NMapCalloutOverlay;
import com.nhn.android.mapviewer.overlay.NMapMyLocationOverlay;
import com.nhn.android.mapviewer.overlay.NMapOverlayManager;
import com.nhn.android.mapviewer.overlay.NMapPOIdataOverlay;
import com.nhn.android.mapviewer.overlay.NMapPathDataOverlay;

public class NMapViewer extends NMapActivity {
    private static final String LOG_TAG = "NMapViewer";
    private static final boolean DEBUG = false;
    private static String ip;
    // set your Client ID which is registered for NMapViewer library.
    private static final String CLIENT_ID = "69nyp8lepmsK8RrvSyQH";

    private MapContainerView mMapContainerView;

    private NMapView mMapView;
    private NMapController mMapController;

    private int click = 1;

    //예약한 변수
    private int check;
    private int ch_val;

    private static char[] data = {'*','0','0','0','0','0','0','0','0','*'};//주고받을 데이터형식
    //1. * 데이터의 시작과 끝
    // 2. 1~9까지는 점유정보(없음 0, 있음 1)
    private static double la;
    private static double lo;

    //back 버튼 두번 누르면 종료하게 하기
    private final long FINISH_INTERVAL_TIME = 2000;
    private long backPressedTime = 0;


    private static final NGeoPoint NMAP_LOCATION_DEFAULT = new NGeoPoint(126.978371, 37.5666091);
    private static final int NMAP_ZOOMLEVEL_DEFAULT = 3;
    private static final int NMAP_VIEW_MODE_DEFAULT = NMapView.VIEW_MODE_VECTOR;
    private static final boolean NMAP_TRAFFIC_MODE_DEFAULT = false;
    private static final boolean NMAP_BICYCLE_MODE_DEFAULT = false;

    private static final String KEY_ZOOM_LEVEL = "NMapViewer.zoomLevel";
    private static final String KEY_CENTER_LONGITUDE = "NMapViewer.centerLongitudeE6";
    private static final String KEY_CENTER_LATITUDE = "NMapViewer.centerLatitudeE6";
    private static final String KEY_VIEW_MODE = "NMapViewer.viewMode";
    private static final String KEY_TRAFFIC_MODE = "NMapViewer.trafficMode";
    private static final String KEY_BICYCLE_MODE = "NMapViewer.bicycleMode";

    private SharedPreferences mPreferences;

    private NMapOverlayManager mOverlayManager;

    private NMapMyLocationOverlay mMyLocationOverlay;
    private NMapLocationManager mMapLocationManager;
    private NMapCompassManager mMapCompassManager;

    private NMapViewerResourceProvider mMapViewerResourceProvider;

    private NMapPOIdataOverlay mFloatingPOIdataOverlay;
    private NMapPOIitem mFloatingPOIitem;

    private static boolean USE_XML_LAYOUT = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //ConnectThread first = new ConnectThread();
        //first.ConnectThread("");
        //first.start();
        super.onCreate(savedInstanceState);
        if(USE_XML_LAYOUT) {
            setContentView(R.layout.main);
            LayoutInflater inflater = (LayoutInflater)getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            RelativeLayout relative = (RelativeLayout) inflater.inflate(R.layout.button, null);

            RelativeLayout.LayoutParams paramlinear = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.FILL_PARENT,
                    RelativeLayout.LayoutParams.FILL_PARENT);
            addContentView(relative, paramlinear);
                    mMapView = (NMapView) findViewById(R.id.mapView);
        }else {
            // create map view
            mMapView = new NMapView(this);

            // create parent view to rotate map view
            mMapContainerView = new MapContainerView(this);
            mMapContainerView.addView(mMapView);

            // set the activity content to the parent view
            setContentView(mMapContainerView);
            LayoutInflater inflater = (LayoutInflater)getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            RelativeLayout relative = (RelativeLayout) inflater.inflate(R.layout.button, null);

            RelativeLayout.LayoutParams paramlinear = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.FILL_PARENT,
                    RelativeLayout.LayoutParams.FILL_PARENT);
            addContentView(relative, paramlinear);
        }
        // set a registered Client Id for Open MapViewer Library
        mMapView.setClientId(CLIENT_ID);

        // initialize map view
        mMapView.setClickable(true);
        mMapView.setEnabled(true);
        mMapView.setFocusable(true);
        mMapView.setFocusableInTouchMode(true);
        mMapView.requestFocus();

        // register listener for map state changes
        mMapView.setOnMapStateChangeListener(onMapViewStateChangeListener);
        mMapView.setOnMapViewTouchEventListener(onMapViewTouchEventListener);
        mMapView.setOnMapViewDelegate(onMapViewTouchDelegate);

        // use map controller to zoom in/out, pan and set map center, zoom level etc.
        mMapController = mMapView.getMapController();

        // use built in zoom controls
        NMapView.LayoutParams lp = new NMapView.LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT, NMapView.LayoutParams.BOTTOM_RIGHT);
        mMapView.setBuiltInZoomControls(true, lp);

        // create resource provider
        mMapViewerResourceProvider = new NMapViewerResourceProvider(this);
        // set data provider listener
        super.setMapDataProviderListener(onDataProviderListener);

        // create overlay manager
        mOverlayManager = new NMapOverlayManager(this, mMapView, mMapViewerResourceProvider);
        // register callout overlay listener to customize it.
        mOverlayManager.setOnCalloutOverlayListener(onCalloutOverlayListener);
        // register callout overlay view listener to customize it.
        mOverlayManager.setOnCalloutOverlayViewListener(onCalloutOverlayViewListener);

        // location manager
        mMapLocationManager = new NMapLocationManager(this);
        mMapLocationManager.setOnLocationChangeListener(onMyLocationChangeListener);

        // compass manager
        mMapCompassManager = new NMapCompassManager(this);

        // create my location overlay
        mMyLocationOverlay = mOverlayManager.createMyLocationOverlay(mMapLocationManager, mMapCompassManager);
        testPOIdataOverlay();
        System.out.println(data);
        System.out.println("la : "+la);
        System.out.println("lo : "+lo);
        //버튼 클래스 정의
        Button button02 = (Button) findViewById(R.id.button02) ;
        //좌석 버튼

        final Button button1 = (Button) findViewById(R.id.button1);
        final Button button2 = (Button) findViewById(R.id.button2) ;
        final Button button3 = (Button) findViewById(R.id.button3) ;
        final Button button4 = (Button) findViewById(R.id.button4) ;
        final Button button5 = (Button) findViewById(R.id.button5) ;
        final Button button6 = (Button) findViewById(R.id.button6) ;
        final Button button7 = (Button) findViewById(R.id.button7) ;
        final Button button8 = (Button) findViewById(R.id.button8) ;

        //버튼색 바로 적용
        //데이터 반영이 된다. 바로 버튼을 세팅하자
        for(int i=0; i<10; i++){
            if(data[i]=='1'){
                switch (i){
                    case 1:{
                        button1.setEnabled(true);
                        button1.setBackgroundColor(Color.rgb(255,0,0));
                        break;
                    }
                    case 2:{
                        button2.setEnabled(true);
                        button2.setBackgroundColor(Color.rgb(255,0,0));
                        break;
                    }
                    case 3:{
                        button3.setEnabled(true);
                        button3.setBackgroundColor(Color.rgb(255,0,0));
                        break;
                    }
                    case 4:{
                        button4.setEnabled(true);
                        button4.setBackgroundColor(Color.rgb(255,0,0));
                        break;
                    }
                    case 5:{
                        button5.setEnabled(true);
                        button5.setBackgroundColor(Color.rgb(255,0,0));
                        break;
                    }
                    case 6:{
                        button6.setEnabled(true);
                        button6.setBackgroundColor(Color.rgb(255,0,0));
                        break;
                    }
                    case 7:{
                        button7.setEnabled(true);
                        button7.setBackgroundColor(Color.rgb(255,0,0));
                        break;
                    }
                    case 8:{
                        button8.setEnabled(true);
                        button8.setBackgroundColor(Color.rgb(255,0,0));
                        break;
                    }
                }//switch
            }//if
            else{
                switch (i){
                    case 1:{
                        button1.setEnabled(false);
                        button1.setBackgroundColor(Color.rgb(0,255,0));
                        break;
                    }
                    case 2:{
                        button2.setEnabled(false);
                        button2.setBackgroundColor(Color.rgb(0,255,0));
                        break;
                    }
                    case 3:{
                        button3.setEnabled(false);
                        button3.setBackgroundColor(Color.rgb(0,255,0));
                        break;
                    }
                    case 4:{
                        button4.setEnabled(false);
                        button4.setBackgroundColor(Color.rgb(0,255,0));
                        break;
                    }
                    case 5:{
                        button5.setEnabled(false);
                        button5.setBackgroundColor(Color.rgb(0,255,0));
                        break;
                    }
                    case 6:{
                        button6.setEnabled(false);
                        button6.setBackgroundColor(Color.rgb(0,255,0));
                        break;
                    }
                    case 7:{
                        button7.setEnabled(false);
                        button7.setBackgroundColor(Color.rgb(0,255,0));
                        break;
                    }
                    case 8:{
                        button8.setEnabled(false);
                        button8.setBackgroundColor(Color.rgb(0,255,0));
                        break;
                    }
                }//switch
            }//else
        }//for

        // 버튼 이벤트 처리
            Button.OnClickListener onClickListener = new Button.OnClickListener(){
            public void onClick(View view){

                if(click >= 1){//두번째 클릭부터 정지함
                    button1.setEnabled(false);

                    button2.setEnabled(false);

                    button3.setEnabled(false);

                    button4.setEnabled(false);

                    button5.setEnabled(false);

                    button6.setEnabled(false);

                    button7.setEnabled(false);

                    button8.setEnabled(false);
                }

                switch(view.getId()){

                    case R.id.button02:{//새로고침
                        ConnectThread a = new ConnectThread();
                        a.start();
                        //data = {'*','1','0','0','0','1','0','1','0','*'};//주고받을 데이터형식
                        //data[1] = '0';
                        //data[5] = '0';
                        //data[7] = '0';
                        for(int i=0; i<10; i++){
                            if(data[i]=='1'){
                                switch (i){
                                    case 1:{
                                        button1.setEnabled(true);
                                        button1.setBackgroundColor(Color.rgb(255,0,0));
                                        break;
                                    }
                                    case 2:{
                                        button2.setEnabled(true);
                                        button2.setBackgroundColor(Color.rgb(255,0,0));
                                        break;
                                    }
                                    case 3:{
                                        button3.setEnabled(true);
                                        button3.setBackgroundColor(Color.rgb(255,0,0));
                                        break;
                                    }
                                    case 4:{
                                        button4.setEnabled(true);
                                        button4.setBackgroundColor(Color.rgb(255,0,0));
                                        break;
                                    }
                                    case 5:{
                                        button5.setEnabled(true);
                                        button5.setBackgroundColor(Color.rgb(255,0,0));
                                        break;
                                    }
                                    case 6:{
                                        button6.setEnabled(true);
                                        button6.setBackgroundColor(Color.rgb(255,0,0));
                                        break;
                                    }
                                    case 7:{
                                        button7.setEnabled(true);
                                        button7.setBackgroundColor(Color.rgb(255,0,0));
                                        break;
                                    }
                                    case 8:{
                                        button8.setEnabled(true);
                                        button8.setBackgroundColor(Color.rgb(255,0,0));
                                        break;
                                    }
                                }//switch
                            }//if
                            else{
                                switch (i){
                                    case 1:{
                                        button1.setEnabled(false);
                                        button1.setBackgroundColor(Color.rgb(0,255,0));
                                        break;
                                    }
                                    case 2:{
                                        button2.setEnabled(false);
                                        button2.setBackgroundColor(Color.rgb(0,255,0));
                                        break;
                                    }
                                    case 3:{
                                        button3.setEnabled(false);
                                        button3.setBackgroundColor(Color.rgb(0,255,0));
                                        break;
                                    }
                                    case 4:{
                                        button4.setEnabled(false);
                                        button4.setBackgroundColor(Color.rgb(0,255,0));
                                        break;
                                    }
                                    case 5:{
                                        button5.setEnabled(false);
                                        button5.setBackgroundColor(Color.rgb(0,255,0));
                                        break;
                                    }
                                    case 6:{
                                        button6.setEnabled(false);
                                        button6.setBackgroundColor(Color.rgb(0,255,0));
                                        break;
                                    }
                                    case 7:{
                                        button7.setEnabled(false);
                                        button7.setBackgroundColor(Color.rgb(0,255,0));
                                        break;
                                    }
                                    case 8:{
                                        button8.setEnabled(false);
                                        button8.setBackgroundColor(Color.rgb(0,255,0));
                                        break;
                                    }
                                }//switch
                            }//else
                        }//for
                        break;
                    }

                    //1~8번 data를 수정하고 Theread 실행
                    case R.id.button1:{//클릭시 각각 색변환, 클릭수 증가, 전달할 데이터 변경, thread 실행
                        button1.setBackgroundColor(Color.rgb(255,0,0));
                        click++;
                        data[1] = '1';
                        ConnectThread a = new ConnectThread();
                        a.start();
                        break;
                    }
                    case R.id.button2:{
                        button2.setBackgroundColor(Color.rgb(255,0,0));
                        click++;
                        data[2] = '1';
                        ConnectThread a = new ConnectThread();
                        a.start();
                        break;
                    }
                    case R.id.button3:{
                        button3.setBackgroundColor(Color.rgb(255,0,0));
                        click++;
                        data[3] = '1';
                        ConnectThread a = new ConnectThread();
                        a.start();
                        break;
                    }
                    case R.id.button4:{
                        button4.setBackgroundColor(Color.rgb(255,0,0));
                        click++;
                        data[4] = '1';
                        ConnectThread a = new ConnectThread();
                        a.start();
                        break;
                    }
                    case R.id.button5:{
                        button5.setBackgroundColor(Color.rgb(255,0,0));
                        click++;
                        data[5] = '1';
                        ConnectThread a = new ConnectThread();
                        a.start();
                        break;
                    }
                    case R.id.button6:{
                        button6.setBackgroundColor(Color.rgb(255,0,0));
                        click++;
                        data[6] = '1';
                        ConnectThread a = new ConnectThread();
                        a.start();
                        break;
                    }
                    case R.id.button7:{
                        button7.setBackgroundColor(Color.rgb(255,0,0));
                        click++;
                        data[7] = '1';
                        ConnectThread a = new ConnectThread();
                        a.start();
                        break;
                    }
                    case R.id.button8:{
                        button8.setBackgroundColor(Color.rgb(255,0,0));
                        click++;
                        data[8] = '1';
                        ConnectThread a = new ConnectThread();
                        a.start();
                        break;
                    }


                }//switch
            }
        };

        //button01.setOnClickListener(onClickListener);
        button02.setOnClickListener(onClickListener);

        button1.setOnClickListener(onClickListener);
        button2.setOnClickListener(onClickListener);
        button3.setOnClickListener(onClickListener);
        button4.setOnClickListener(onClickListener);
        button5.setOnClickListener(onClickListener);
        button6.setOnClickListener(onClickListener);
        button7.setOnClickListener(onClickListener);
        button8.setOnClickListener(onClickListener);


    }

    @Override
          protected void onStart() {
            super.onStart();
        }

        @Override
        protected void onResume() {
            super.onResume();
        }

        @Override
        protected void onStop() {

        stopMyLocation();

        super.onStop();
    }

    @Override
    protected void onDestroy() {

        // save map view state such as map center position and zoom level.
        saveInstanceState();

        super.onDestroy();
    }

	/* Test Functions */

    private void startMyLocation() {

        if (mMyLocationOverlay != null) {
            if (!mOverlayManager.hasOverlay(mMyLocationOverlay)) {
                mOverlayManager.addOverlay(mMyLocationOverlay);
            }

            if (mMapLocationManager.isMyLocationEnabled()) {

                if (!mMapView.isAutoRotateEnabled()) {
                    mMyLocationOverlay.setCompassHeadingVisible(true);

                    mMapCompassManager.enableCompass();

                    mMapView.setAutoRotateEnabled(true, false);

                    mMapContainerView.requestLayout();
                } else {
                    stopMyLocation();
                }

                mMapView.postInvalidate();
            } else {
                boolean isMyLocationEnabled = mMapLocationManager.enableMyLocation(true);
                if (!isMyLocationEnabled) {
                    Toast.makeText(NMapViewer.this, "위치정보가 켜저있지 않아, 설정으로 들어갑니다.",
                            Toast.LENGTH_LONG).show();

                    Intent goToSettings = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(goToSettings);

                    //return;
                }
            }
        }
    }

    private void stopMyLocation() {
        if (mMyLocationOverlay != null) {
            mMapLocationManager.disableMyLocation();

            if (mMapView.isAutoRotateEnabled()) {
                mMyLocationOverlay.setCompassHeadingVisible(false);

                mMapCompassManager.disableCompass();

                mMapView.setAutoRotateEnabled(false, false);

                mMapContainerView.requestLayout();
            }
        }
    }

    private void testPathDataOverlay() {

        // set path data points
        NMapPathData pathData = new NMapPathData(9);

        pathData.initPathData();
        pathData.addPathPoint(127.108099, 37.366034, NMapPathLineStyle.TYPE_SOLID);
        pathData.addPathPoint(127.108088, 37.366043, 0);
        pathData.addPathPoint(127.108079, 37.365619, 0);
        pathData.addPathPoint(127.107458, 37.365608, 0);
        pathData.addPathPoint(127.107232, 37.365608, 0);
        pathData.addPathPoint(127.106904, 37.365624, 0);
        pathData.addPathPoint(127.105933, 37.365621, NMapPathLineStyle.TYPE_DASH);
        pathData.addPathPoint(127.105929, 37.366378, 0);
        pathData.addPathPoint(127.106279, 37.366380, 0);
        pathData.endPathData();

        NMapPathDataOverlay pathDataOverlay = mOverlayManager.createPathDataOverlay(pathData);
        if (pathDataOverlay != null) {

            // add path data with polygon type
            NMapPathData pathData2 = new NMapPathData(4);
            pathData2.initPathData();
            pathData2.addPathPoint(127.106, 37.367, NMapPathLineStyle.TYPE_SOLID);
            pathData2.addPathPoint(127.107, 37.367, 0);
            pathData2.addPathPoint(127.107, 37.368, 0);
            pathData2.addPathPoint(127.106, 37.368, 0);
            pathData2.endPathData();
            pathDataOverlay.addPathData(pathData2);
            // set path line style
            NMapPathLineStyle pathLineStyle = new NMapPathLineStyle(mMapView.getContext());
            pathLineStyle.setPataDataType(NMapPathLineStyle.DATA_TYPE_POLYGON);
            pathLineStyle.setLineColor(0xA04DD2, 0xff);
            pathLineStyle.setFillColor(0xFFFFFF, 0x00);
            pathData2.setPathLineStyle(pathLineStyle);

            // add circle data
            NMapCircleData circleData = new NMapCircleData(1);
            circleData.initCircleData();
            circleData.addCirclePoint(127.1075, 37.3675, 50.0F);
            circleData.endCircleData();
            pathDataOverlay.addCircleData(circleData);
            // set circle style
            NMapCircleStyle circleStyle = new NMapCircleStyle(mMapView.getContext());
            circleStyle.setLineType(NMapPathLineStyle.TYPE_DASH);
            circleStyle.setFillColor(0x000000, 0x00);
            circleData.setCircleStyle(circleStyle);

            // show all path data
            pathDataOverlay.showAllPathData(0);
        }
    }

    private void testPOIdataOverlay() {

        // Markers for POI item
        int markerId = NMapPOIflagType.PIN;

        // set POI data
        NMapPOIdata poiData = new NMapPOIdata(2, mMapViewerResourceProvider);
        poiData.beginPOIdata(2);
        NMapPOIitem item = poiData.addPOIitem(128.754275, 35.830828, "IT관", markerId, 0);
        NMapPOIitem item2 = poiData.addPOIitem(128.753439, 35.832593, "천마아트센터", markerId, 1);
        item.setRightAccessory(true, NMapPOIflagType.CLICKABLE_ARROW);
        item2.setRightAccessory(true, NMapPOIflagType.CLICKABLE_ARROW);
        item.setSnippet("192.168.1.6");
       // poiData.addPOIitem(128.754, 35.83, "IT관", markerId, 0);
       // poiData.addPOIitem(128.753, 35.83, "천마아트센터", markerId, 1);

        // create POI data overlay
        NMapPOIdataOverlay poiDataOverlay = mOverlayManager.createPOIdataOverlay(poiData, null);

        // set event listener to the overlay
        poiDataOverlay.setOnStateChangeListener(onPOIdataStateChangeListener);

        // select an item
        poiDataOverlay.selectPOIitem(0, true);
       // poiDataOverlay.selectPOIitem(1, true);

        // show all POI data
        //poiDataOverlay.showAllPOIdata(0);
    }

    /* NMapDataProvider Listener */
    private final OnDataProviderListener onDataProviderListener = new OnDataProviderListener() {

        @Override
        public void onReverseGeocoderResponse(NMapPlacemark placeMark, NMapError errInfo) {

            if (DEBUG) {
                Log.i(LOG_TAG, "onReverseGeocoderResponse: placeMark="
                        + ((placeMark != null) ? placeMark.toString() : null));
            }

            if (errInfo != null) {
                Log.e(LOG_TAG, "Failed to findPlacemarkAtLocation: error=" + errInfo.toString());

                Toast.makeText(NMapViewer.this, errInfo.toString(), Toast.LENGTH_LONG).show();
                return;
            }

            if (mFloatingPOIitem != null && mFloatingPOIdataOverlay != null) {
                mFloatingPOIdataOverlay.deselectFocusedPOIitem();

                if (placeMark != null) {
                    mFloatingPOIitem.setTitle(placeMark.toString());
                }
                mFloatingPOIdataOverlay.selectPOIitemBy(mFloatingPOIitem.getId(), false);
            }
        }

    };

    /* MyLocation Listener */
    private final NMapLocationManager.OnLocationChangeListener onMyLocationChangeListener = new NMapLocationManager.OnLocationChangeListener() {

        @Override
        public boolean onLocationChanged(NMapLocationManager locationManager, NGeoPoint myLocation) {

            if (mMapController != null) {
                mMapController.animateTo(myLocation);
            }

            return true;
        }

        @Override
        public void onLocationUpdateTimeout(NMapLocationManager locationManager) {

            // stop location updating
            //			Runnable runnable = new Runnable() {
            //				public void run() {
            //					stopMyLocation();
            //				}
            //			};
            //			runnable.run();

            Toast.makeText(NMapViewer.this, "Your current location is temporarily unavailable.", Toast.LENGTH_LONG).show();
        }

        @Override
        public void onLocationUnavailableArea(NMapLocationManager locationManager, NGeoPoint myLocation) {

            Toast.makeText(NMapViewer.this, "Your current location is unavailable area.", Toast.LENGTH_LONG).show();

            stopMyLocation();
        }

    };

    /* MapView State Change Listener*/
    private final NMapView.OnMapStateChangeListener onMapViewStateChangeListener = new NMapView.OnMapStateChangeListener() {

        @Override
        public void onMapInitHandler(NMapView mapView, NMapError errorInfo) {

            if (errorInfo == null) { // success
                // restore map view state such as map center position and zoom level.

                //restoreInstanceState();

                onZoomLevelChange(mapView, 1);


            } else { // fail
                Log.e(LOG_TAG, "onFailedToInitializeWithError: " + errorInfo.toString());

                Toast.makeText(NMapViewer.this, errorInfo.toString(), Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onAnimationStateChange(NMapView mapView, int animType, int animState) {
            if (DEBUG) {
                Log.i(LOG_TAG, "onAnimationStateChange: animType=" + animType + ", animState=" + animState);
            }
        }

        @Override
        public void onMapCenterChange(NMapView mapView, NGeoPoint center) {
            if (DEBUG) {
                Log.i(LOG_TAG, "onMapCenterChange: center=" + center.toString());
            }
        }

        @Override
        public void onZoomLevelChange(NMapView mapView, int level) {
            if (DEBUG) {
                Log.i(LOG_TAG, "onZoomLevelChange: level=" + level);
            }
        }

        @Override
        public void onMapCenterChangeFine(NMapView mapView) {

        }
    };

    private final NMapView.OnMapViewTouchEventListener onMapViewTouchEventListener = new NMapView.OnMapViewTouchEventListener() {

        @Override
        public void onLongPress(NMapView mapView, MotionEvent ev) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onLongPressCanceled(NMapView mapView) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onSingleTapUp(NMapView mapView, MotionEvent ev) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onTouchDown(NMapView mapView, MotionEvent ev) {

        }

        @Override
        public void onScroll(NMapView mapView, MotionEvent e1, MotionEvent e2) {
        }

        @Override
        public void onTouchUp(NMapView mapView, MotionEvent ev) {
            // TODO Auto-generated method stub

        }

    };

    private final NMapView.OnMapViewDelegate onMapViewTouchDelegate = new NMapView.OnMapViewDelegate() {

        @Override
        public boolean isLocationTracking() {
            if (mMapLocationManager != null) {
                if (mMapLocationManager.isMyLocationEnabled()) {
                    return mMapLocationManager.isMyLocationFixed();
                }
            }
            return false;
        }

    };

    /* POI data State Change Listener*/
    private final NMapPOIdataOverlay.OnStateChangeListener onPOIdataStateChangeListener = new NMapPOIdataOverlay.OnStateChangeListener() {

        @Override
        public void onCalloutClick(NMapPOIdataOverlay poiDataOverlay, NMapPOIitem item) {
            if (DEBUG) {
                Log.i(LOG_TAG, "onCalloutClick: title=" + item.getTitle());
            }

            // [[TEMP]] handle a click event of the callout
            Toast.makeText(NMapViewer.this, "주차장: " + item.getTitle(), Toast.LENGTH_LONG).show();
            String ip = item.getSnippet();
            Button b1 = (Button) findViewById(R.id.button1);
            Button b2 = (Button) findViewById(R.id.button2);
            Button b3 = (Button) findViewById(R.id.button3);
            Button b4 = (Button) findViewById(R.id.button4);
            Button b5 = (Button) findViewById(R.id.button5);
            Button b6 = (Button) findViewById(R.id.button6);
            Button b7 = (Button) findViewById(R.id.button7);
            Button b8 = (Button) findViewById(R.id.button8);
            ConnectThread first = new ConnectThread();
            first.ConnectThread(ip);
            first.start();
            b1.setVisibility(View.VISIBLE);
            b2.setVisibility(View.VISIBLE);
            b3.setVisibility(View.VISIBLE);
            b4.setVisibility(View.VISIBLE);
            b5.setVisibility(View.VISIBLE);
            b6.setVisibility(View.VISIBLE);
            b7.setVisibility(View.VISIBLE);
            b8.setVisibility(View.VISIBLE);
        }




        @Override
        public void onFocusChanged(NMapPOIdataOverlay poiDataOverlay, NMapPOIitem item) {
            if (DEBUG) {
                if (item != null) {
                    Log.i(LOG_TAG, "onFocusChanged: " + item.toString());
                } else {
                    Log.i(LOG_TAG, "onFocusChanged: ");
                }
            }
        }
    };

    private final NMapPOIdataOverlay.OnFloatingItemChangeListener onPOIdataFloatingItemChangeListener = new NMapPOIdataOverlay.OnFloatingItemChangeListener() {

        @Override
        public void onPointChanged(NMapPOIdataOverlay poiDataOverlay, NMapPOIitem item) {
            NGeoPoint point = item.getPoint();

            if (DEBUG) {
                Log.i(LOG_TAG, "onPointChanged: point=" + point.toString());
            }

            findPlacemarkAtLocation(point.longitude, point.latitude);

            item.setTitle(null);

        }
    };

    private final NMapOverlayManager.OnCalloutOverlayListener onCalloutOverlayListener = new NMapOverlayManager.OnCalloutOverlayListener() {

        @Override
        public NMapCalloutOverlay onCreateCalloutOverlay(NMapOverlay itemOverlay, NMapOverlayItem overlayItem,
                                                         Rect itemBounds) {

            // handle overlapped items
            if (itemOverlay instanceof NMapPOIdataOverlay) {
                NMapPOIdataOverlay poiDataOverlay = (NMapPOIdataOverlay)itemOverlay;

                // check if it is selected by touch event
                if (!poiDataOverlay.isFocusedBySelectItem()) {
                    int countOfOverlappedItems = 1;

                    NMapPOIdata poiData = poiDataOverlay.getPOIdata();
                    for (int i = 0; i < poiData.count(); i++) {
                        NMapPOIitem poiItem = poiData.getPOIitem(i);

                        // skip selected item
                        if (poiItem == overlayItem) {
                            continue;
                        }

                        // check if overlapped or not
                        if (Rect.intersects(poiItem.getBoundsInScreen(), overlayItem.getBoundsInScreen())) {
                            countOfOverlappedItems++;
                        }
                    }

                    if (countOfOverlappedItems > 1) {
                        String text = countOfOverlappedItems + " overlapped items for " + overlayItem.getTitle();
                        Toast.makeText(NMapViewer.this, text, Toast.LENGTH_LONG).show();
                        return null;
                    }
                }
            }

            // use custom old callout overlay
            if (overlayItem instanceof NMapPOIitem) {
                NMapPOIitem poiItem = (NMapPOIitem)overlayItem;

                if (poiItem.showRightButton()) {
                    return new NMapCalloutCustomOldOverlay(itemOverlay, overlayItem, itemBounds,
                            mMapViewerResourceProvider);
                }
            }

            // use custom callout overlay
            return new NMapCalloutCustomOverlay(itemOverlay, overlayItem, itemBounds, mMapViewerResourceProvider);

            // set basic callout overlay
            //return new NMapCalloutBasicOverlay(itemOverlay, overlayItem, itemBounds);
        }

    };

    private final NMapOverlayManager.OnCalloutOverlayViewListener onCalloutOverlayViewListener = new NMapOverlayManager.OnCalloutOverlayViewListener() {

        @Override
        public View onCreateCalloutOverlayView(NMapOverlay itemOverlay, NMapOverlayItem overlayItem, Rect itemBounds) {

            if (overlayItem != null) {
                // [TEST] 말풍선 오버레이를 뷰로 설정함
                String title = overlayItem.getTitle();
                if (title != null && title.length() > 5) {
                    return new NMapCalloutCustomOverlayView(NMapViewer.this, itemOverlay, overlayItem, itemBounds);
                }
            }

            // null을 반환하면 말풍선 오버레이를 표시하지 않음
            return null;
        }

    };

    /* Local Functions */
    private static boolean mIsMapEnlared = false;

    private void restoreInstanceState() {
        mPreferences = getPreferences(MODE_PRIVATE);

        int longitudeE6 = mPreferences.getInt(KEY_CENTER_LONGITUDE, NMAP_LOCATION_DEFAULT.getLongitudeE6());
        int latitudeE6 = mPreferences.getInt(KEY_CENTER_LATITUDE, NMAP_LOCATION_DEFAULT.getLatitudeE6());
        int level = mPreferences.getInt(KEY_ZOOM_LEVEL, NMAP_ZOOMLEVEL_DEFAULT);
        int viewMode = mPreferences.getInt(KEY_VIEW_MODE, NMAP_VIEW_MODE_DEFAULT);
        boolean trafficMode = mPreferences.getBoolean(KEY_TRAFFIC_MODE, NMAP_TRAFFIC_MODE_DEFAULT);
        boolean bicycleMode = mPreferences.getBoolean(KEY_BICYCLE_MODE, NMAP_BICYCLE_MODE_DEFAULT);

        mMapController.setMapViewMode(viewMode);
        mMapController.setMapViewTrafficMode(trafficMode);
        mMapController.setMapViewBicycleMode(bicycleMode);
        mMapController.setMapCenter(new NGeoPoint(longitudeE6, latitudeE6), level);

        if (mIsMapEnlared) {
            mMapView.setScalingFactor(2.0F);
        } else {
            mMapView.setScalingFactor(1.0F);
        }
    }

    private void saveInstanceState() {
        if (mPreferences == null) {
            return;
        }

        NGeoPoint center = mMapController.getMapCenter();
        int level = mMapController.getZoomLevel();
        int viewMode = mMapController.getMapViewMode();
        boolean trafficMode = mMapController.getMapViewTrafficMode();
        boolean bicycleMode = mMapController.getMapViewBicycleMode();

        SharedPreferences.Editor edit = mPreferences.edit();

        edit.putInt(KEY_CENTER_LONGITUDE, center.getLongitudeE6());
        edit.putInt(KEY_CENTER_LATITUDE, center.getLatitudeE6());
        edit.putInt(KEY_ZOOM_LEVEL, level);
        edit.putInt(KEY_VIEW_MODE, viewMode);
        edit.putBoolean(KEY_TRAFFIC_MODE, trafficMode);
        edit.putBoolean(KEY_BICYCLE_MODE, bicycleMode);

        edit.commit();

    }

    /**
     * Invoked during init to give the Activity a chance to set up its Menu.
     *
     * @param menu the Menu to which entries may be added
     * @return true
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        int viewMode = mMapController.getMapViewMode();
        boolean isTraffic = mMapController.getMapViewTrafficMode();
        boolean isBicycle = mMapController.getMapViewBicycleMode();

        menu.findItem(R.id.action_revert).setEnabled((viewMode != NMapView.VIEW_MODE_VECTOR) || isTraffic || mOverlayManager.sizeofOverlays() > 0);
        menu.findItem(R.id.action_vector).setChecked(viewMode == NMapView.VIEW_MODE_VECTOR);
        menu.findItem(R.id.action_satellite).setChecked(viewMode == NMapView.VIEW_MODE_HYBRID);
        menu.findItem(R.id.action_traffic).setChecked(isTraffic);
        menu.findItem(R.id.action_bicycle).setChecked(isBicycle);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_revert:
                if (mMyLocationOverlay != null) {
                    stopMyLocation();
                    mOverlayManager.removeOverlay(mMyLocationOverlay);
                }

                mMapController.setMapViewMode(NMapView.VIEW_MODE_VECTOR);
                mMapController.setMapViewTrafficMode(false);
                mMapController.setMapViewBicycleMode(false);
                mOverlayManager.clearOverlays();
                return true;
            case R.id.action_vector:
                invalidateMenu();
                mMapController.setMapViewMode(NMapView.VIEW_MODE_VECTOR);
                return true;

            case R.id.action_satellite:
                invalidateMenu();
                mMapController.setMapViewMode(NMapView.VIEW_MODE_HYBRID);
                return true;

            case R.id.action_traffic:
                invalidateMenu();
                mMapController.setMapViewTrafficMode(!mMapController.getMapViewTrafficMode());
                return true;

            case R.id.action_bicycle:
                invalidateMenu();
                mMapController.setMapViewBicycleMode(!mMapController.getMapViewBicycleMode());
                return true;

            case R.id.action_zoom:
                mMapView.displayZoomControls(true);
                return true;

            case R.id.action_my_location:
                startMyLocation();
                return true;
/*
            case R.id.action_poi_data:
                mOverlayManager.clearOverlays();

                // add POI data overlay
                testPOIdataOverlay();
                return true;

            case R.id.action_path_data:
                mOverlayManager.clearOverlays();

                // add path data overlay
                testPathDataOverlay();

                // add path POI data overlay
                testPathPOIdataOverlay();
                return true;

            case R.id.action_floating_data:
                mOverlayManager.clearOverlays();
                testFloatingPOIdataOverlay();
                return true;

            case R.id.action_visible_bounds:
                // test visible bounds
                Rect viewFrame = mMapView.getMapController().getViewFrameVisible();
                mMapController.setBoundsVisible(0, 0, viewFrame.width(), viewFrame.height() - 200);

                // add POI data overlay
                mOverlayManager.clearOverlays();

                testPathDataOverlay();
                return true;

            case R.id.action_scale_factor:
                if (mMapView.getMapProjection().isProjectionScaled()) {
                    if (mMapView.getMapProjection().isMapHD()) {
                        mMapView.setScalingFactor(2.0F, false);
                    } else {
                        mMapView.setScalingFactor(1.0F, false);
                    }
                } else {
                    mMapView.setScalingFactor(2.0F, true);
                }
                mIsMapEnlared = mMapView.getMapProjection().isProjectionScaled();
                return true;

            case R.id.action_auto_rotate:
                if (mMapView.isAutoRotateEnabled()) {
                    mMapView.setAutoRotateEnabled(false, false);

                    mMapContainerView.requestLayout();

                    mHnadler.removeCallbacks(mTestAutoRotation);
                } else {

                    mMapView.setAutoRotateEnabled(true, false);

                    mMapView.setRotateAngle(30);
                    mHnadler.postDelayed(mTestAutoRotation, AUTO_ROTATE_INTERVAL);

                    mMapContainerView.requestLayout();
                }
                return true;

            case R.id.action_navermap:
                mMapView.executeNaverMap();
                return true;
            */
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        long tempTime = System.currentTimeMillis();
        long intervalTime = tempTime - backPressedTime;
        Button b1 = (Button) findViewById(R.id.button1);
        Button b2 = (Button) findViewById(R.id.button2);
        Button b3 = (Button) findViewById(R.id.button3);
        Button b4 = (Button) findViewById(R.id.button4);
        Button b5 = (Button) findViewById(R.id.button5);
        Button b6 = (Button) findViewById(R.id.button6);
        Button b7 = (Button) findViewById(R.id.button7);
        Button b8 = (Button) findViewById(R.id.button8);
        if (0 <= intervalTime && FINISH_INTERVAL_TIME >= intervalTime)
        {
            super.onBackPressed();
        }
        else
        {

            backPressedTime = tempTime;
            b1.setVisibility(View.VISIBLE);
            if(b1.getVisibility() == View.VISIBLE){
                b1.setVisibility(View.GONE);
                b2.setVisibility(View.GONE);
                b3.setVisibility(View.GONE);
                b4.setVisibility(View.GONE);
                b5.setVisibility(View.GONE);
                b6.setVisibility(View.GONE);
                b7.setVisibility(View.GONE);
                b8.setVisibility(View.GONE);
            }
            Toast.makeText(getApplicationContext(), "한번 더 뒤로가기 누르면 앱이 종료됩니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private void invalidateMenu() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            invalidateOptionsMenu();
        }
    }

    private static final long AUTO_ROTATE_INTERVAL = 2000;
    private final Handler mHnadler = new Handler();
    private final Runnable mTestAutoRotation = new Runnable() {
        @Override
        public void run() {
//        	if (mMapView.isAutoRotateEnabled()) {
//    			float degree = (float)Math.random()*360;
//
//    			degree = mMapView.getRoateAngle() + 30;
//
//    			mMapView.setRotateAngle(degree);
//
//            	mHnadler.postDelayed(mTestAutoRotation, AUTO_ROTATE_INTERVAL);
//        	}
        }
    };

    /**
     * Container view class to rotate map view.
     */
    private class MapContainerView extends ViewGroup {

        public MapContainerView(Context context) {
            super(context);
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            final int width = getWidth();
            final int height = getHeight();
            final int count = getChildCount();
            for (int i = 0; i < count; i++) {
                final View view = getChildAt(i);
                final int childWidth = view.getMeasuredWidth();
                final int childHeight = view.getMeasuredHeight();
                final int childLeft = (width - childWidth) / 2;
                final int childTop = (height - childHeight) / 2;
                view.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
            }

            if (changed) {
                mOverlayManager.onSizeChanged(width, height);
            }
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int w = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
            int h = getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec);
            int sizeSpecWidth = widthMeasureSpec;
            int sizeSpecHeight = heightMeasureSpec;

            final int count = getChildCount();
            for (int i = 0; i < count; i++) {
                final View view = getChildAt(i);

                if (view instanceof NMapView) {
                    if (mMapView.isAutoRotateEnabled()) {
                        int diag = (((int)(Math.sqrt(w * w + h * h)) + 1) / 2 * 2);
                        sizeSpecWidth = MeasureSpec.makeMeasureSpec(diag, MeasureSpec.EXACTLY);
                        sizeSpecHeight = sizeSpecWidth;
                    }
                }

                view.measure(sizeSpecWidth, sizeSpecHeight);
            }
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    /**
     * 소켓 연결할 스레드 정의
     */
    static class ConnectThread extends Thread {

        static String toHost ;//= "165.229.125.59";  //"165.229.125.59";//ifconfig로 host주소 확인필
        // 수정필요
        //그대로
        int port = 15000;

        //스트링으로 받아오는 데이터 변수 임의로 저장해놓는 부분 없에도 됨
        String result;

        //MainActivity에서 선언한 data가 정확히 전달된다.
        public static void ConnectThread(String ip){
            ConnectThread.toHost = ip;
        }

        public void run(){
            try {
                System.out.println("서버에 연결중입니다. 서버 IP : " + toHost);

                // 소켓을 생성하여 연결을 요청한다.
                Socket socket = new Socket(toHost, port);

                // 소켓에서 나갈 데이터 생성
                OutputStream out = socket.getOutputStream();
                DataOutputStream dos = new DataOutputStream(out);  // 기본형 단위로 처리하는 보조스트림
                //출력 데이터 생성
                String out_data = new String(data,0,data.length);
                //데이터 전송
                dos.writeUTF(out_data);


                //입력받는 데이터
                InputStream in = socket.getInputStream();
                DataInputStream dis = new DataInputStream(in); // 기본형 단위로 처리하는 보조스트림
                String in_data = dis.readUTF();
                String split_d[] = new String(in_data).split("/");
                //데이터 위치정보 쪼개서 저장
                data = split_d[0].toCharArray();
                //위도 경도
//                la = Double.valueOf(split_d[1]);
//                lo = Double.valueOf(split_d[2]);

                // 스트림과 소켓을 닫는다.
                System.out.println("연결을 종료합니다.");
                dos.close();
                dis.close();
                socket.close();

            } catch (ConnectException ce) {
                ce.printStackTrace();
            } catch (IOException ie) {
                ie.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            } // try - catch
        }//run
    }//thread class
}


