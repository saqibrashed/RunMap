package com.stdnull.runmap.modules.map;

import android.Manifest;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.utils.overlay.SmoothMoveMarker;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeAddress;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.stdnull.baselib.common.CFLog;
import com.stdnull.baselib.GlobalApplication;
import com.stdnull.baselib.common.RMConfiguration;
import com.stdnull.runmap.model.BuildingPoint;
import com.stdnull.runmap.model.TrackPoint;
import com.stdnull.runmap.modules.map.filter.ILocationFilter;
import com.stdnull.runmap.modules.map.listenter.IGpsPowerListener;
import com.stdnull.runmap.modules.map.listenter.IGpsSwicthListener;
import com.stdnull.runmap.modules.map.listenter.IMapCaptureFinished;
import com.stdnull.runmap.modules.map.listenter.IOnNewLocation;
import com.stdnull.baselib.modules.permission.PermissionCallBack;
import com.stdnull.baselib.modules.permission.PermissionManager;
import com.stdnull.baselib.utils.SystemUtils;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by chen on 2017/6/1.
 */

public class AmapWrapper implements IMap,AMapStateListener {
    private AMap mAmap;
    private AMapStateListenerImpl mAmapStateListener;

    private MapDrawer mMapDrawer;

    /**
     * 用于进行逆地理位置编码
     */
    private GeocodeSearch mGeocodeSearch;

    private IGpsPowerListener mGpsPowerListener;
    private IGpsSwicthListener mGpsSwitchListener;
    private IOnNewLocation mOnNewLocationListener;
    /**
     * 定位服务类实例，提供单次定位、持续定位、最后位置相关功能。
     */
    private AMapLocationClient mAmapLocationClient;
    /**
     * 定位参数设置，通过该实例可以对定位的相关参数进行设置
     */
    private AMapLocationClientOption mAmapLocationOption;
    /**
     * 定位间隔时长
     */
    private final long mLocationInterval = 5 * RMConfiguration.SECOND;

    private List<ILocationFilter> mLocationFilterList = new ArrayList<>();
    /**
     * 上一次定位结果
     */
    private AMapLocation mPrevoiusLocation;

    /**
     * 初始化地图显示样式
     * @param map
     */
    private void initAmap(AMap map){
        this.mAmap = map;
        //照传入的CameraUpdate参数移动可视区域。
        map.animateCamera(CameraUpdateFactory.zoomTo(19));
        //设置定位源
        map.setLocationSource(mAmapStateListener);
        //显示室内地图
        map.showIndoorMap(true);
        //设置是否打开定位图层
        map.setMyLocationEnabled(true);
        MyLocationStyle style = new MyLocationStyle();
        //去除定位中心圆圈
        style.strokeWidth(0);
        style.radiusFillColor(Color.TRANSPARENT);
        //定位、且将视角移动到地图中心点，定位点依照设备方向旋转，并且会跟随设备移动
        style.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE);
        map.setMyLocationStyle(style);
        //去除缩放按钮
        UiSettings settings = map.getUiSettings();
        settings.setZoomControlsEnabled(false);
    }

    private void initLocationOptions() {
        mAmapLocationOption = new AMapLocationClientOption();
        //设置定位模式为AMapLocationMode.Hight_Accuracy，高精度模式。
        mAmapLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        //设置定位间隔,单位毫秒,默认为2000ms，最低1000ms。
        mAmapLocationOption.setInterval(mLocationInterval);
        //单位是毫秒，默认30000毫秒，建议超时时间不要低于8000毫秒。
        mAmapLocationOption.setHttpTimeOut(RMConfiguration.HTTP_OUT_TIME);
        //开启缓存机制
        mAmapLocationOption.setLocationCacheEnable(false);
        //需要返回地址
        mAmapLocationOption.setNeedAddress(true);
        //当设置为true时，网络定位可以返回海拔、角度和速度
        mAmapLocationOption.setSensorEnable(true);
    }

    private void initLocationClient() {
        mAmapLocationClient = new AMapLocationClient(GlobalApplication.getAppContext());
        mAmapLocationClient.setLocationListener(mAmapStateListener);
        mAmapLocationClient.setLocationOption(mAmapLocationOption);
    }

    public AmapWrapper(AMap aMap){
        this.mAmap = aMap;
        mAmapStateListener = new AMapStateListenerImpl();
        mGeocodeSearch = new GeocodeSearch(GlobalApplication.getAppContext());
        mMapDrawer = new MapDrawer(mAmap);
        initLocationOptions();
        initLocationClient();
    }

    @Override
    public void initMap() {
        initAmap(mAmap);
    }

    @Override
    public void startLocation(Activity context) {
        PermissionManager.getInstance().requestPermission(context,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                new PermissionCallBack() {
                    @Override
                    public void onAllPermissionGranted() {
                        if (mAmapLocationClient != null) {
                            mAmapStateListener.setStateListener(AmapWrapper.this);
                            mAmapLocationClient.startLocation();
                        }
                    }

                    @Override
                    public void onDenied() {

                    }
                });
    }

    @Override
    public void setMinZoomLevel(int level) {
        mAmap.setMinZoomLevel(level);
    }

    @Override
    public void addLocationFilter(ILocationFilter filter) {
        mLocationFilterList.add(filter);
    }

    @Override
    public boolean removeLocationFilter(ILocationFilter filter) {
        return mLocationFilterList.remove(filter);
    }

    @Override
    public void setOnGpsPowerListener(IGpsPowerListener listener) {
        this.mGpsPowerListener = listener;
    }

    @Override
    public void setOnGpsSwitchListener(IGpsSwicthListener listener) {
        this.mGpsSwitchListener = listener;
    }

    @Override
    public void setOnNewLocationListener(IOnNewLocation listener) {
        this.mOnNewLocationListener = listener;
    }

    @Override
    public void notifyServiceActive() {
        CFLog.i("AmapWrapper","service active");

    }

    @Override
    public void notifyServiceDeactivate() {

    }

    @Override
    public void notifyLocationChanged(LocationSource.OnLocationChangedListener amListener, AMapLocation aMapLocation) {
        if(mGpsPowerListener != null){
            mGpsPowerListener.onGpsPower(aMapLocation.getGpsAccuracyStatus());
        }
        for(ILocationFilter locationFilter : mLocationFilterList) {
            if(!locationFilter.accept(mPrevoiusLocation, aMapLocation)){
                return;
            }
        }
        mPrevoiusLocation = aMapLocation;
        amListener.onLocationChanged(aMapLocation);
        if(mOnNewLocationListener != null){
            mOnNewLocationListener.onNewLocation(aMapLocation);
        }
    }

    @Override
    public void notifyMapLoaded() {
    }

    @Override
    public void notifyGPSSwitchChanged() {
        if(mGpsSwitchListener != null){
            mGpsSwitchListener.onGPSSwitchChanged();
        }
    }

    @Override
    public void requestRegeoAddress(AMapLocation aMapLocation, final TrackPoint trackPoint) {
        Observable.just(aMapLocation)
                .subscribeOn(Schedulers.io())
                .map(new Func1<AMapLocation, LatLonPoint>() {
                    @Override
                    public LatLonPoint call(AMapLocation aMapLocation) {
                        return new LatLonPoint(aMapLocation.getLatitude(), aMapLocation.getLongitude());
                    }
                })
                .filter(new Func1<LatLonPoint, Boolean>() {
                    @Override
                    public Boolean call(LatLonPoint latLonPoint) {
                        if(!SystemUtils.isNetworkEnable(GlobalApplication.getAppContext())){
                            return false;
                        }
                        return true;
                    }
                })
                .map(new Func1<LatLonPoint, RegeocodeAddress>() {
                    @Override
                    public RegeocodeAddress call(LatLonPoint latLonPoint) {
                        RegeocodeQuery query = new RegeocodeQuery(latLonPoint, 200,GeocodeSearch.AMAP);
                        try {
                            return mGeocodeSearch.getFromLocation(query);
                        } catch (AMapException e) {
                            CFLog.e(this.getClass().getName(),"Regeocode failed");
                        }
                        return null;
                    }
                })
                .subscribe(new Action1<RegeocodeAddress>() {
                    @Override
                    public void call(RegeocodeAddress result) {
                        if (result == null) {
                            return;
                        }
                        trackPoint.setBuildName(result.getBuilding());
                        CFLog.e("TAG", "regeo building =" + result.getBuilding()
                                + "district = " + result.getDistrict() + " neiberhood = " + result.getNeighborhood()
                                + "street = " + result.getStreetNumber());
                    }
                });
    }

    @Override
    public AMap getController() {
        return mAmap;
    }

    @Override
    public int getMapType() {
        return mAmap.getMapType();
    }

    @Override
    public void changeMapType() {
        if (mAmap.getMapType() == AMap.MAP_TYPE_NORMAL) {
            mAmap.setMapType(AMap.MAP_TYPE_SATELLITE);
        } else {
            mAmap.setMapType(AMap.MAP_TYPE_NORMAL);
        }
    }

    @Override
    public void moveToSpecficCamera(CameraUpdate cameraUpdate) {
        mAmap.moveCamera(cameraUpdate);
    }

    @Override
    public void captureMap(final IMapCaptureFinished callback) {
        mAmap.getMapScreenShot(new AMap.OnMapScreenShotListener() {
            @Override
            public void onMapScreenShot(Bitmap bitmap) {

            }

            @Override
            public void onMapScreenShot(Bitmap bitmap, int i) {
                if (callback != null) {
                    callback.onMapCaptureFinished(bitmap, i);
                }
                StringBuilder buffer = new StringBuilder();
                if (i != 0)
                    buffer.append("地图渲染完成，截屏无网格");
                else {
                    buffer.append("地图未渲染完成，截屏有网格");
                }
                CFLog.e("TAG",buffer.toString());
            }
        });
    }

    @Override
    public void drawPolyLine(float speed, LatLng... latLngs) {
        mMapDrawer.drawPolyLine(speed, latLngs);
    }

    @Override
    public void drawPolyLine(List<LatLng> latLngs, int color) {
        mMapDrawer.drawPolyLine(latLngs, color);
    }

    @Override
    public void drawTrackAnimation(List<LatLng> drawSource, int currentIndex, SmoothMoveMarker.MoveListener moveListener) {
        mMapDrawer.drawTrackAnimation(drawSource, currentIndex, moveListener);
    }

    @Override
    public void clear() {
        mAmap.clear();
    }

    @Override
    public void drawMarkers(List<BuildingPoint> buildingPointList) {
        mMapDrawer.drawMarkers(buildingPointList);
    }
}
