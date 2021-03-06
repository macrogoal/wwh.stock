package com.bm.wanma.ui.fragment;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.location.LocationManagerProxy;
import com.amap.api.location.LocationProviderProxy;
import com.amap.api.maps.AMap;
import com.amap.api.maps.AMap.OnCameraChangeListener;
import com.amap.api.maps.AMap.OnMapClickListener;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.bm.wanma.R;
import com.bm.wanma.broadcast.BroadcastUtil;
import com.bm.wanma.entity.MapModeBean;
import com.bm.wanma.entity.MarkerCityBean;
import com.bm.wanma.net.GetDataPost;
import com.bm.wanma.net.Protocol;
import com.bm.wanma.popup.AnchorPopupWindow;
import com.bm.wanma.socket.SocketConstant;
import com.bm.wanma.socket.StreamUtil;
import com.bm.wanma.socket.TCPSocketManager;
import com.bm.wanma.ui.activity.ChargeListActivity;
import com.bm.wanma.ui.activity.HomeActivity;
import com.bm.wanma.ui.activity.HomeActivity.MapAwaitChange;
import com.bm.wanma.ui.activity.ITcpCallBack;
import com.bm.wanma.ui.activity.LoginActivity;
import com.bm.wanma.ui.activity.RealTimeChargeActivity;
import com.bm.wanma.ui.activity.SearchPointActivity;
import com.bm.wanma.ui.scan.CaptureActivity;
import com.bm.wanma.utils.DistanceUtil;
import com.bm.wanma.utils.LogUtil;
import com.bm.wanma.utils.PreferencesUtil;
import com.bm.wanma.utils.Tools;
import com.bm.wanma.view.CustomPullLinearLayout;
import com.bm.wanma.view.CustomPullLinearLayout.CustomPullHiddenListener;
import com.bm.wanma.widget.RequstDataClipLoading;
import com.umeng.analytics.MobclickAgent;

public class MapModeFragment extends BaseFragment implements LocationSource,AMapLocationListener
	,AMap.OnMarkerClickListener,OnMapClickListener,OnCameraChangeListener,
	OnClickListener,CustomPullHiddenListener,ITcpCallBack,MapAwaitChange{
	 
	private AMap aMap;
	private MapView mapView;
	private CustomPullLinearLayout customLayout;
	private LocationManagerProxy mAMapLocationManager;
	private OnLocationChangedListener mListener;
	private MarkerOptions markerOption;
	private String currentlat,currentlng,location;
	private LatLng currentLatLng;
	private String condition_ac,condition_dc,condition_park,condition_idel;
	private Bundle locBundle;
	// 登录实体，记录是否登录
	private String pkUserinfo;
	private RelativeLayout loading;
	private RequstDataClipLoading ccl;
	private BitmapDescriptor marker_icon,marker_select_icon;
	// 获取Map模式下的实体
	private ArrayList<MapModeBean> allMapBean;
	private ArrayList<MarkerCityBean> markerCitybeans,markerProvinceBeans;
	private String cityCode;
	private boolean hasLocationSuccess,isFirst,isArea,isCity,hasProvinceRequesting;
	private ImageButton home_location_btn,map_scan;
	private View mapModeFragment;
	private Marker currentMarker;
	private MyLocationStyle myLocationStyle;
	private static final int MAP_LEVEL = 14;
	private AnchorPopupWindow popWindow;
	private TextView iv_charging_animation;
//	private FrameLayout fl_charging_confirm;
	private ImageButton switchBtn;
	private TextView tv_search;
	private String pilenum,headnum;
	private TCPSocketManager mTcpSocketManager;
//	private AlphaAnimation animation = new AlphaAnimation(1, 0);
	private RelativeLayout.LayoutParams lp = null;
	private boolean isCurrentFragment = true;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//		registerBoradcastReceiver();
//		mPageName = "MapModeFragment";
		HomeActivity.setmapAwaitChangeSize(this);
		allMapBean = new ArrayList<MapModeBean>();
		markerCitybeans = new ArrayList<MarkerCityBean>();
		
	}

	
	@SuppressLint("NewApi")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// 高德地图view
		mapModeFragment = inflater.inflate(R.layout.fragment_map_and_list,
				container, false);
		customLayout = (CustomPullLinearLayout) mapModeFragment.findViewById(R.id.map_custom_pull_ll);
		customLayout.setCustomPullHiddenListener(this);
//		fl_charging_confirm = (FrameLayout) mapModeFragment.findViewById(R.id.charging_confirm);
		iv_charging_animation= (TextView)mapModeFragment.findViewById(R.id.nav_charging);
		iv_charging_animation.setOnClickListener(this);
		switchBtn = (ImageButton)mapModeFragment.findViewById(R.id.swith_button);
		switchBtn.setOnClickListener(this);
		tv_search = (TextView) mapModeFragment.findViewById(R.id.nav_search);
		tv_search.setOnClickListener(this);
		
		mapView = (MapView) mapModeFragment.findViewById(R.id.map);
		mapView.onCreate(savedInstanceState);// 必须要写
		
		lp = (android.widget.RelativeLayout.LayoutParams) tv_search.getLayoutParams();  
		init();
		initView();
		
		return mapModeFragment;
	}
	
/**
 * 初始化值view
 */
	private void initView(){

		home_location_btn = (ImageButton) mapModeFragment
				.findViewById(R.id.home_location);
		home_location_btn.setOnClickListener(this);
		map_scan = (ImageButton) mapModeFragment
				.findViewById(R.id.map_scan);
		map_scan.setOnClickListener(this);
		loading = (RelativeLayout) mapModeFragment
				.findViewById(R.id.mapmode_fragment_loading);
		ccl = (RequstDataClipLoading) loading
				.findViewById(R.id.customClipLoading);
	}

	/**
	 * 初始化
	 */
	private void init() {
		if (aMap == null) {
			aMap = mapView.getMap();
			marker_icon = BitmapDescriptorFactory.fromResource(R.drawable.marker_icon);
			marker_select_icon = BitmapDescriptorFactory.fromResource(R.drawable.marker_select_icon);
			setUpMap();
		}
	}
	
	public ArrayList<MapModeBean> getData(){
		return allMapBean;
	}

	/**
	 * 设置一些amap的属性
	 */
	private void setUpMap() {
		myLocationStyle = new MyLocationStyle();
		myLocationStyle.strokeColor(Color.argb(0, 0, 0, 0));// 设置圆形的边框颜色
		myLocationStyle.radiusFillColor(Color.argb(0, 0, 0, 0));// 设置圆形的填充颜色
		myLocationStyle.myLocationIcon(BitmapDescriptorFactory
				.fromResource(R.drawable.location_marker));// 设置小蓝点的图标
		aMap.setMyLocationStyle(myLocationStyle);
		
		aMap.setOnMarkerClickListener(this);// 设置点击marker事件监听器
		aMap.setOnMapClickListener(this); 
		aMap.setOnCameraChangeListener(this);
		aMap.setLocationSource(this);// 设置定位监听
		aMap.getUiSettings().setMyLocationButtonEnabled(false);// 设置默认定位按钮是否显示
		aMap.getUiSettings().setZoomControlsEnabled(false);//设置默认缩放按钮是否显示
		aMap.getUiSettings().setRotateGesturesEnabled(false);//禁用手势旋转地图
		aMap.setMyLocationEnabled(true);// 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false
		// 设置定位的类型为定位模式 ，可以由定位、跟随或地图根据面向方向旋转几种
		aMap.setMyLocationType(AMap.LOCATION_TYPE_LOCATE);
		//aMap.setMyLocationType(AMap.LOCATION_TYPE_MAP_ROTATE);
	}

	@SuppressLint("NewApi")
	@SuppressWarnings("static-access")
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.home_location: //定位按钮点击事件
			
			String geoLat = PreferencesUtil.getStringPreferences(getActivity()
					.getApplicationContext(), "currentlat");
			String geoLng = PreferencesUtil.getStringPreferences(getActivity()
					.getApplicationContext(), "currentlng");
			if (!geoLat.equals("") && !geoLng.equals("")) {
				aMap.animateCamera( new CameraUpdateFactory().newLatLngZoom (new
						  LatLng(Double.parseDouble(geoLat), Double.parseDouble(geoLng)),
						  MAP_LEVEL));
			} else {
				showToast("正在定位，请稍等...");
				startLocation();
			}
			break;
		case R.id.map_scan://扫描按钮点击事件
			MobclickAgent.onEvent(mContext, "chongdian_saoyisao");
			clickScanBtn();
			break;
		case R.id.nav_charging:
			clickChargingBtn();//充电中点击事件
			break;
		case R.id.swith_button:
			//进入充电点界面
			if(popWindow != null && popWindow.isShowing()){
				popWindow.dismiss();
			}
			MobclickAgent.onEvent(mContext, "chongdian_liebiao");
			Intent chargelistIn = new Intent();
			chargelistIn.setClass(getActivity(),ChargeListActivity.class);
			chargelistIn.putExtra("allMapBean", allMapBean);
			startActivity(chargelistIn);
			break;
		case R.id.nav_search:
			//进入搜索界面
			
			Intent searchIn = new Intent();
		    searchIn.setClass(getActivity(),SearchPointActivity.class);
			startActivity(searchIn);
			break;
		default:
			break;
		}
	}  

	/**
	 * 点击扫描按钮
	 * 
	 */
	private void clickScanBtn() {
		pkUserinfo = PreferencesUtil.getStringPreferences(getActivity(), "pkUserinfo");
		if(!Tools.isEmptyString(pkUserinfo)){
			Intent scanIntent = new Intent();
			scanIntent.setClass(getActivity(),
					CaptureActivity.class);
			startActivity(scanIntent);
		}else {
			Intent in = new Intent();  //跳转登录界面
			in.setClass(getActivity(), LoginActivity.class);
			in.putExtra("source_inteface", "captureactivity");
			startActivity(in);
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		
	}
	@Override
	public void onHiddenChanged(boolean hidden) {
		super.onHiddenChanged(hidden);
		if(!hidden){
			//重新加载数据
			isCurrentFragment = true;
			onResume();
		}else {
			isCurrentFragment = false;
			onPause();
		}
	}
	/**
	 * 方法必须重写
	 */
	@Override
	public void onResume() {
		super.onResume();
		if(!isCurrentFragment){
			return;
		}
//		MobclickAgent.onResume(mContext);
//		MobclickAgent.onPageStart(mPageName);
//		MobclickAgent.onEvent(mContext, "anniu_chogndian");
		mapView.onResume();
		startLocation();//重新启动定位
		pilenum = PreferencesUtil.getStringPreferences(getActivity(),"chargepilenum");
		headnum = PreferencesUtil.getStringPreferences(getActivity(),"chargeheadnum");
		if(!Tools.isEmptyString(pilenum) && !Tools.isEmptyString(headnum)){
			lp.setMargins(141, 0, 180, 0);
			tv_search.setLayoutParams(lp);  
//			iv_charging_animation.setVisibility(View.VISIBLE);
//			fl_charging_confirm.setVisibility(View.VISIBLE);
//			animation.setDuration(3000);
//			animation.setRepeatCount(-1);
//			iv_charging_animation.startAnimation(animation);
	    }else {
//	    	iv_charging_animation.setVisibility(View.GONE);
	    	lp.setMargins(141, 0, 45, 0); 
	    	tv_search.setLayoutParams(lp);  
//	    	fl_charging_confirm.setVisibility(View.GONE);
	    }
		pkUserinfo = PreferencesUtil.getStringPreferences(getActivity().getApplicationContext(),"pkUserinfo");
	}
	
	/**
	 * 在地图上添加marker
	 */
	private void addMarkersToMap(ArrayList<MapModeBean> mMapBean) {
		aMap.clear(true);
		if (mMapBean != null && !mMapBean.isEmpty()) { 
				for (MapModeBean bean : mMapBean) {
						try {
							if(bean.getPoStLatitude() != null && bean.getPoStLongitude() != null){
								markerOption = new MarkerOptions();
								markerOption.position(new LatLng(Double.parseDouble(bean.getPoStLatitude()),
										Double.parseDouble(bean.getPoStLongitude())));
								markerOption.icon(marker_icon);
								//markerOption.icon(BitmapDescriptorFactory.fromBitmap(Tools.getMarkerIcon(getActivity(), String.valueOf(num))));
								Marker marker = aMap.addMarker(markerOption);
								marker.setObject(bean);// 这里可以存储数据信息
							}else {
								LogUtil.i("cm_socket", "MapModeBean=============为空");
							}
						} catch (NumberFormatException e) {
							e.printStackTrace();
						}
					}
				}
		}
	/**
	 * 在地图上添加省市级充电点聚合
	 */
	private void addCityMarkersToMap(ArrayList<MarkerCityBean> mMapBean) {
		aMap.clear(true);
		if (mMapBean != null && !mMapBean.isEmpty()) { 
				for (MarkerCityBean bean : mMapBean) {
					try {
						String[] lngLat = bean.getLocation().split(",");
						String count = bean.getCount();
						markerOption = new MarkerOptions();
						markerOption.position(new LatLng(Double.parseDouble(lngLat[1]),
									Double.parseDouble(lngLat[0])));
						markerOption.icon(BitmapDescriptorFactory.fromBitmap(Tools.getMarkerIcon(getActivity(),count)));
						aMap.addMarker(markerOption);
					} catch (NumberFormatException e) {
						e.printStackTrace();
					}
						
					}
				}
		}
	
	//点击非marker区域，将显示的InfoWindow隐藏  
	@Override
	public void onMapClick(LatLng arg0) {
		LogUtil.i("cm_socket", "onMapClick");
	}
	
	/**
	 * 对marker标注点点击响应事件
	 */
	@Override
	public boolean onMarkerClick(Marker marker) {
		if (aMap != null) {
				MapModeBean markerbean = (MapModeBean) marker.getObject();
				if(markerbean != null){
					if(currentMarker != null){
						currentMarker.setIcon(marker_icon);
					}
					marker.setIcon(marker_select_icon);
					currentMarker = marker;
					if(popWindow == null){
						popWindow = new AnchorPopupWindow(getActivity(),markerbean);
						popWindow.setOnDismissListener(new OnDismissListener() {
							@Override
							public void onDismiss() {
								// TODO 处理marker变换事件
								currentMarker.setIcon(marker_icon);
							}
						});
					}else {
						popWindow.initData(markerbean);
					}
					popWindow.showAtLocation(mapModeFragment.findViewById(R.id.mapmode_fragment), 
							Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL, 0, 0);
				}
			
		}

		return false;//返回:true 表示点击marker后marker 不会移动到地图中心；返回false 表示点击marker后marker 会自动移动到地图中心
	}

	
	/**
	 * 方法必须重写
	 */
	@Override
	public void onPause() {
		super.onPause();
		if(isCurrentFragment){
			return;
		}
//		MobclickAgent.onPause(mContext);
//		MobclickAgent.onPageEnd(mPageName);
		mapView.onPause();
		stopLocation();//停止定位

	}

	/**
	 * 方法必须重写
	 */
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		mapView.onSaveInstanceState(outState);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mapView.onDestroy();
		deactivate();
		getActivity().unregisterReceiver(mBroadcastReceiver);
	}
	
	/**
	 * 此方法已经废弃
	 */
	@Override
	public void onLocationChanged(Location location) {
		
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	}
	
	//处理定位成功事件
	private void handleLocationSuccess(AMapLocation location){
		LogUtil.i("cm_socket", "定位成功");
		// 保存当前经纬度
		currentlat = String.valueOf(location.getLatitude());
		currentlng = String.valueOf(location.getLongitude());
		PreferencesUtil.setPreferences(getActivity()
				.getApplicationContext(), "currentlat", currentlat);
		PreferencesUtil.setPreferences(getActivity()
				.getApplicationContext(), "currentlng", currentlng);
		String province = location.getProvince();
		String city = location.getCity();
		String distric = location.getDistrict();
		PreferencesUtil.setPreferences(getActivity(), "province", province);
		PreferencesUtil.setPreferences(getActivity(), "city", city);
		PreferencesUtil.setPreferences(getActivity(), "distric", distric);
		
		locBundle = location.getExtras();
		if (locBundle != null) {
			String desc = locBundle.getString("desc");
			// adcode=330106, citycode=0571, desc=浙江
			PreferencesUtil.setPreferences(getActivity(), "desc", desc);
			String adcode = locBundle.getString("adcode");
			if (adcode.isEmpty()) {
				showToast("请开启定位权限或检查网络连接");
				return;
			}
			PreferencesUtil.setPreferences(getActivity(), "adcode", adcode);
			adcode = adcode.substring(0, 4);
			StringBuilder sb = new StringBuilder();
			sb.append(adcode).append("00");
			cityCode = sb.toString();
			PreferencesUtil.setPreferences(getActivity(), "cityCode", cityCode);
			PreferencesUtil.setPreferences(getActivity()
					.getApplicationContext(), "currentcitycode", sb
					.toString());
			PreferencesUtil.setPreferences(getActivity()
					.getApplicationContext(), "currentaddres", desc);

		}
		
	}
	
	//处理定位失败事件
	@SuppressWarnings("static-access")
	private void handleLocationError(){
		String geoLat = PreferencesUtil.getStringPreferences(getActivity()
				.getApplicationContext(), "currentlat");
		String geoLng = PreferencesUtil.getStringPreferences(getActivity()
				.getApplicationContext(), "currentlng");
		if (!Tools.isEmptyString(geoLat) && !Tools.isEmptyString(geoLng) && !hasLocationSuccess) {
			/*aMap.animateCamera( new CameraUpdateFactory().newLatLngZoom (new
					  LatLng(Double.parseDouble(geoLat), Double.parseDouble(geoLng)),
					  MAP_LEVEL));*/
		} else if(!hasLocationSuccess){
			showToast("正在定位，请稍等...");
			Double initLat = 30.270695;
			Double initLng = 120.129251;
			PreferencesUtil.setPreferences(getActivity()
					.getApplicationContext(), "currentlat", String.valueOf(initLat));
			PreferencesUtil.setPreferences(getActivity()
					.getApplicationContext(), "currentlng", String.valueOf(initLng));
			aMap.animateCamera( new CameraUpdateFactory().newLatLngZoom (new
					  LatLng(initLat,initLng),
					  MAP_LEVEL));
			startLocation();
		}
	}
	
	/**
	 * 定位成功后回调函数
	 */
	@Override
	public void onLocationChanged(AMapLocation amapLocation) {
		if (mListener != null && amapLocation != null) {
			if (amapLocation != null
					&& amapLocation.getAMapException().getErrorCode() == 0) {
				hasLocationSuccess = true ;
				mListener.onLocationChanged(amapLocation);// 显示系统小蓝点
				handleLocationSuccess(amapLocation);
			} else {
				handleLocationError();
			}
		}
	}

	/**
	 * 激活定位
	 */
	@Override
	public void activate(OnLocationChangedListener listener) {
		mListener = listener;
		if (mAMapLocationManager == null) {
			mAMapLocationManager = LocationManagerProxy.getInstance(getActivity());
			// 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
			// 注意设置合适的定位时间的间隔（最小间隔支持为2000ms），并且在合适时间调用removeUpdates()方法来取消定位请求
			// 在定位结束后，在合适的生命周期调用destroy()方法
			// 其中如果间隔时间为-1，则定位只定一次
			// 在单次定位情况下，定位无论成功与否，都无需调用removeUpdates()方法移除请求，定位sdk内部会移除
			mAMapLocationManager.requestLocationData(
					LocationProviderProxy.AMapNetwork, 60 * 1000 * 5, 20, this);
			mAMapLocationManager.setGpsEnable(true);
		}
		
	}
	
	/**
	 * 重新启动定位
	 * 
	 */
	private void startLocation(){
		if (mAMapLocationManager == null) {
			LogUtil.i("cm_location","重新定位" );
			mAMapLocationManager = LocationManagerProxy
					.getInstance(getActivity().getApplicationContext());
			mAMapLocationManager.requestLocationData(
					LocationProviderProxy.AMapNetwork, 60 * 1000 * 5 , 20, this);
			mAMapLocationManager.setGpsEnable(true);
		}
	}
	/**
	 * 暂停定位
	 */
	@SuppressWarnings("deprecation")
	private void stopLocation() {
	    if (mAMapLocationManager != null) {
	    	LogUtil.i("cm_location","停止 定位" );
	    	mAMapLocationManager.removeUpdates(this);
	    	mAMapLocationManager.destory();
	    }
	    mAMapLocationManager = null;
	}
	
	/**
	 * 停止定位
	 */
	@Override
	public void deactivate() {
		mListener = null;
		if (mAMapLocationManager != null) {
			mAMapLocationManager.removeUpdates(this);
			mAMapLocationManager.destroy();
		}
		mAMapLocationManager = null;
		
	}
 
	@SuppressWarnings("unchecked")
	@Override
	public void onSuccess(String sign, Bundle bundle) {
		if(sign.equals(Protocol.GET_MAP_LIST)) {
			allMapBean.clear();
			allMapBean = (ArrayList<MapModeBean>) bundle
					.getSerializable(Protocol.DATA);
			addMarkersToMap(allMapBean);
			ccl.stop();
			loading.setVisibility(View.GONE);
			
		}else if(sign.equals(Protocol.GET_MARKER_CITY)) {
			markerCitybeans.clear();
			markerCitybeans = (ArrayList<MarkerCityBean>) bundle
					.getSerializable(Protocol.DATA);
			addCityMarkersToMap(markerCitybeans);
			ccl.stop();
			loading.setVisibility(View.GONE);
			
		}else if(sign.equals(Protocol.GET_MARKER_PROVINCE)) {
			markerProvinceBeans = (ArrayList<MarkerCityBean>) bundle
					.getSerializable(Protocol.DATA);
			addCityMarkersToMap(markerProvinceBeans);
			ccl.stop();
			loading.setVisibility(View.GONE);
		}else if(sign.equals(Protocol.GET_API_TOKEN)){//获取token
			String apiToken = (String) bundle.getSerializable(Protocol.DATA);
			PreferencesUtil.setPreferences(getActivity(), "apiToken",
					apiToken);
		}
	}
	
	@Override
	public void onFaile(String sign, Bundle bundle) {

		ccl.stop();
		loading.setVisibility(View.GONE);
		showToast(bundle.getString(Protocol.MSG));
		if(sign.equals(Protocol.GET_MARKER_PROVINCE)) {
			hasProvinceRequesting = false;
		}
		
		
		if(currentMarker != null){
			currentMarker.setIcon(marker_icon);
		}
		if ("5000".equals(bundle.getString(Protocol.MSG))) {
			GetDataPost.getInstance(getActivity()).getApiToken(handler);
		}
	}
	
	
	
	public void registerBoradcastReceiver(){  
        IntentFilter myIntentFilter = new IntentFilter();  
        myIntentFilter.addAction(BroadcastUtil.BROADCAST_Charge_CANCLE);
        myIntentFilter.addAction(BroadcastUtil.BROADCAST_Charge_Ing);
        myIntentFilter.addAction(BroadcastUtil.BROADCAST_Force_Offline);
        myIntentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        //注册广播        
        getActivity().registerReceiver(mBroadcastReceiver, myIntentFilter);  
    }  
	//更新地图点
	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver(){  
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if(action.equals(BroadcastUtil.BROADCAST_Charge_CANCLE)){
	        	 //充电结束
	        	 PreferencesUtil.setPreferences(getActivity(),"chargepilenum","");
	        	 PreferencesUtil.setPreferences(getActivity(),"chargeheadnum","");
	        	 iv_charging_animation.setVisibility(View.GONE);
//	        	 fl_charging_confirm.setVisibility(View.GONE);
	         } else if(action.equals(BroadcastUtil.BROADCAST_Charge_Ing)){
	        	 //充电进行中
	        	 iv_charging_animation.setVisibility(View.VISIBLE);
//	        	 fl_charging_confirm.setVisibility(View.VISIBLE);
	        	 pilenum = intent.getStringExtra("chargepilenum");
	        	 headnum = intent.getStringExtra("chargeheadnum");
	        	 PreferencesUtil.setPreferences(getActivity(),"chargepilenum",pilenum);
	        	 PreferencesUtil.setPreferences(getActivity(),"chargeheadnum",headnum);
//	        	 animation.setDuration(3000);
//	 			 animation.setRepeatCount(-1);
//	 			 iv_charging_animation.startAnimation(animation);
	        	  
	         } else if(action.equals(BroadcastUtil.BROADCAST_Force_Offline)){
	        	 //强制下线
	        	 iv_charging_animation.setVisibility(View.GONE);
//	        	 fl_charging_confirm.setVisibility(View.GONE);
	        	 PreferencesUtil.setPreferences(getActivity(),"chargepilenum","");
	        	 PreferencesUtil.setPreferences(getActivity(),"chargeheadnum","");
	         }
		}  
   };

	@Override
	public void onCameraChange(CameraPosition arg0) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void onCameraChangeFinish(CameraPosition arg0) {
		// 获取当前地图的缩放级别
		float mZoom = aMap.getCameraPosition().zoom;	
		LogUtil.i("cm_socket", "mZoom"+mZoom);
		if(currentLatLng == null){
			currentLatLng = arg0.target;
		}
		if(!isFirst){
			isFirst = true;
			new CameraUpdateFactory();
			aMap.animateCamera( CameraUpdateFactory.newLatLngZoom (new
					  LatLng(arg0.target.latitude, arg0.target.longitude),
					  MAP_LEVEL));
		}else {
			if(mZoom <= 6){
				isCity = false;
				isArea = false;
				requestProvinceMarkers();
			}else if(mZoom <= 10){
				isArea = false;
				if(!isCity){
					isCity = true;
					currentLatLng = arg0.target;
					requestCityMarkers(arg0.target);
				}else {
					boolean calc= calculateDistance(arg0.target,currentLatLng);
					if(calc){
						currentLatLng = arg0.target;
						requestCityMarkers(arg0.target);
					}
				}
			}else{
				isCity = false;
				if(!isArea){
					isArea = true;
					currentLatLng = arg0.target;
					LogUtil.i("cm_", "isArea");
					requestFordata(arg0.target);
				}else {
					boolean calc= calculateDistance(arg0.target,currentLatLng);
					if(calc){
						currentLatLng = arg0.target;
						LogUtil.i("cm_", "isArea  calc");
						requestFordata(arg0.target);
					}
				}
			}
		}
	}
	/*	
	 //聚合方法
	private void clusterMarkers(ArrayList<MapModeBean> list){
		LogUtil.i("cm_socket", "开始聚合");	
		// 自定义的聚合类MarkerCluster
		ArrayList<MarkerCluster> clustersMarker = new ArrayList<MarkerCluster>();
		for(MapModeBean bean : list){
			if(!Tools.isEmptyString(bean.getCityCode())){
				if (clustersMarker.size() == 0) {
					clustersMarker.add(new MarkerCluster(bean.getCityCode(), bean,getActivity()));
				}else {
					boolean isIn = false;
					for (MarkerCluster cluster : clustersMarker) {
						if(bean.getCityCode().equals(cluster.getCityCode())){
							cluster.addMarker(bean);
							isIn = true;
							break;
						}
					}
					if (!isIn) {
						clustersMarker.add(new MarkerCluster(bean.getCityCode(), bean,getActivity()));
					}
				}
				
			}
		}
	}*/

	@Override
	public void onHidden() {
		requestFordata(currentLatLng);
	}

	/*获取当前定位点50公里数据*/
	private void requestFordata(LatLng latlng){
		loading.setVisibility(View.VISIBLE);
		ccl.start();
		if(PreferencesUtil.getBooleanPreferences(getActivity(), "isAc", false)){
			condition_ac = "1";
		}else {
			condition_ac = "0";
		}
		if(PreferencesUtil.getBooleanPreferences(getActivity(), "isDc", false)){
			condition_dc = "1";
		}else {
			condition_dc = "0";
		}
		if(PreferencesUtil.getBooleanPreferences(getActivity(), "isPark", false)){
			condition_park = "1";
		}else {
			condition_park = "0";
		}
		if(PreferencesUtil.getBooleanPreferences(getActivity(), "isIdle", false)){
			condition_idel = "1";
		}else {
			condition_idel = "0";
		}

		if (Tools.isEmptyString(currentlat)
				||Tools.isEmptyString(currentlng)
				) {		
			if (!Tools.isEmptyString(PreferencesUtil.getStringPreferences(getActivity(), "currentlat"))
					&&!Tools.isEmptyString(PreferencesUtil.getStringPreferences(getActivity(), "currentlng"))
					) {
				currentlat = PreferencesUtil.getStringPreferences(getActivity(), "currentlat");
				currentlng = PreferencesUtil.getStringPreferences(getActivity(), "currentlng");
				
			}else {
				currentlat = String.valueOf(latlng.latitude);
				currentlng = String.valueOf(latlng.longitude);
			}
		}
		
		GetDataPost.getInstance(getActivity()).getElectricPileMapList(handler,currentlat,currentlng ,String.valueOf(latlng.latitude), String.valueOf(latlng.longitude),
				condition_dc, condition_ac, condition_idel, condition_park,pkUserinfo,"1000");
		PreferencesUtil.setPreferences(getActivity(), "targetLat",String.valueOf(latlng.latitude));
		PreferencesUtil.setPreferences(getActivity(), "targetLng",String.valueOf(latlng.longitude));
	}
	/*获取市级充电点聚合*/
	private void requestCityMarkers(LatLng latlng){
		loading.setVisibility(View.VISIBLE);
		ccl.start();
		location = latlng.longitude + "," +latlng.latitude;
		GetDataPost.getInstance(getActivity()).getMarkerCity(handler, location,"1000");
		PreferencesUtil.setPreferences(getActivity(), "targetLat",String.valueOf(latlng.latitude));
		PreferencesUtil.setPreferences(getActivity(), "targetLng",String.valueOf(latlng.longitude));
	}
	
	/*获取省级充电点聚合*/
	private void requestProvinceMarkers(){
		if(markerProvinceBeans != null && markerProvinceBeans.size() > 0){
			addCityMarkersToMap(markerProvinceBeans);
		}else {
			if(!hasProvinceRequesting){
				loading.setVisibility(View.VISIBLE);
				ccl.start();
				GetDataPost.getInstance(getActivity()).getMarkerProvince(handler,"1000");
				hasProvinceRequesting = true;
			}
			
		}
	}
	
	private boolean calculateDistance(LatLng latlng1,LatLng latlng2){
		if(latlng1 == null || latlng2 == null){
			return false;
		}
		if(DistanceUtil.getDistance(latlng1.latitude, latlng1.longitude, 
				latlng2.latitude, latlng2.longitude) <= 15 * 1000){
			return false;
		}else {
			return true;
		}
	}
	 private void clickChargingBtn(){
		 MobclickAgent.onEvent(mContext, "ditu_zhengzaichongdian");
			showPD("正在获取充电信息...");
			mTcpSocketManager = TCPSocketManager.getInstance(getActivity());
			mTcpSocketManager.setTcpCallback(this);
			if(!mTcpSocketManager.hasTcpConnection()&&!Tools.isEmptyString(pilenum)
					&& !Tools.isEmptyString(headnum)){
				mTcpSocketManager.conn(pilenum, 
						Byte.parseByte(headnum));
			}else {
				cancelPD();
				showErrorCode(110);
			}
		  }
	 
	//处理tcp报文
			@Override
			public void handleTcpPacket(final ByteArrayInputStream result) {
				//收到实时数据，进入实时数据界面
				getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						cancelPD();
					    try {
							StreamUtil.readByte(result);//int reason = 
							short cmdtype = StreamUtil.readShort(result);
							switch (cmdtype) {
							case SocketConstant.CMD_TYPE_REAL_DATA:
								handleRealDataEvent(result);
								break;
							case SocketConstant.CMD_TYPE_CONNECT:
								handleConnectEvent(result);
								break;
							default:
								break;
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				});
			}
			//处理socket-实时数据事件
			private void handleRealDataEvent(ByteArrayInputStream result) throws IOException{
				int state = StreamUtil.readByte(result);
				short chargeTime = StreamUtil.readShort(result);
				StreamUtil.readShort(result);//short dianya = 
				StreamUtil.readShort(result);//short dianliu = 
				int diandu = StreamUtil.readInt(result);
				short feilv = StreamUtil.readShort(result);
				int yuchong = StreamUtil.readInt(result);
				int yichong = StreamUtil.readInt(result);
				int soc = StreamUtil.readByte(result);
				StreamUtil.readInt(result);//int fushu = 
				StreamUtil.readInt(result);//int gaojing = 
				Intent realIn = new Intent(getActivity(),
						RealTimeChargeActivity.class);
				realIn.putExtra("state", state);
				realIn.putExtra("chargeTime", chargeTime);
				realIn.putExtra("diandu", diandu);
				realIn.putExtra("feilv", feilv);
				realIn.putExtra("yuchong", yuchong);
				realIn.putExtra("yichong", yichong);
				realIn.putExtra("soc", soc);
				realIn.putExtra("interfacefrom", "mapmob");
				//mTcpSocketManager.close();
				startActivity(realIn);
			}
			//处理socket-连接充电桩事件
			private void handleConnectEvent(ByteArrayInputStream result) throws IOException {
				int successflag = StreamUtil.readByte(result);
				short errorcode = StreamUtil.readShort(result);
				int headState = StreamUtil.readByte(result);
				int type = 1;
				 try {
					type = StreamUtil.readByte(result);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				mTcpSocketManager.setPileType(type);
				switch (successflag) {
				case 1:
					//连接成功
					if(3 == headState || 0 == headState){
						 showToast("充电已结束");
						 PreferencesUtil.setPreferences(getActivity(),"chargepilenum","");
			        	 PreferencesUtil.setPreferences(getActivity(),"chargeheadnum","");
			        	 iv_charging_animation.setVisibility(View.GONE);
//			        	 fl_charging_confirm.setVisibility(View.GONE);
			        	 mTcpSocketManager.close();
					}
					break;
				case 0:
					showErrorCode(errorcode);
					mTcpSocketManager.close();
					break;
				default:
					break;
				}
		   }


			@Override
			public void handleSocketException() {
				getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						cancelPD();
						showErrorCode(110);
					}
				});
			}


			@Override
			public void mapawaitchange() {
				pilenum = PreferencesUtil.getStringPreferences(getActivity(),"chargepilenum");
				headnum = PreferencesUtil.getStringPreferences(getActivity(),"chargeheadnum");
				if(!Tools.isEmptyString(pilenum) && !Tools.isEmptyString(headnum)){
					lp.setMargins(141, 0, 180, 0);
					tv_search.setLayoutParams(lp);  
//					iv_charging_animation.setVisibility(View.VISIBLE);
//					fl_charging_confirm.setVisibility(View.VISIBLE);
//					animation.setDuration(3000);
//					animation.setRepeatCount(-1);
//					iv_charging_animation.startAnimation(animation);
			    }else {
//			    	iv_charging_animation.setVisibility(View.GONE);
			    	lp.setMargins(141, 0, 45, 0); 
			    	tv_search.setLayoutParams(lp);  
//			    	fl_charging_confirm.setVisibility(View.GONE);
			    }
			}
			
}
