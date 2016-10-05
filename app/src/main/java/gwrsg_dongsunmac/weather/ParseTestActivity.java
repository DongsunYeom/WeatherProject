package gwrsg_dongsunmac.weather;

/**
 * Created by gwrsg-dongsunmac on 1/10/16.
 */
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by gwrsg-dongsunmac on 1/10/16.
 */

public class ParseTestActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener, GoogleMap.OnMapClickListener {

    private static final int PERMISSION_LOCATION_REQUEST_CODE = 1;

    private ArrayList<DataGetterSetters> dataList;
    private DataGetterSetters data = null;

    Marker selectedMarker;
    View marker_root_view;
    TextView tv_marker;
    private GoogleMap mMap;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        int result = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION);
        if (result == PackageManager.PERMISSION_GRANTED) {
            // Obtain the SupportMapFragment and get notified when the map is ready to be used.
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);
        } else {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_LOCATION_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapClickListener(this);

        setCustomMarkerView();
        connectionUrl();
    }

    @Override
    public void onMapClick(LatLng latLng) {

    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        return false;
    }

    private void setCustomMarkerView() {
        marker_root_view = LayoutInflater.from(this).inflate(R.layout.marker_item, null);
        tv_marker = (TextView) marker_root_view.findViewById(R.id.tv_marker);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void getXmlData(InputStream is) {
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(new InputStreamReader(is, "UTF-8"));
            String tag;
            xpp.next();
            int eventType = xpp.getEventType();

            data = new DataGetterSetters();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_DOCUMENT:
                        break;

                    case XmlPullParser.START_TAG:
                        tag = xpp.getName();    //테그 이름 얻어오기

                        if (data.getMainItem() == null) {
                            if (tag.equalsIgnoreCase("title")) {
                                xpp.next();
                                data.setTitle(xpp.getText());
                            } else if (tag.equalsIgnoreCase("source")) {
                                xpp.next();
                                data.setSource(xpp.getText());
                            } else if (tag.equalsIgnoreCase("description")) {
                                xpp.next();
                                data.setDescription(xpp.getText());
                            } else if (tag.equals("item")) {
                                xpp.next();
                                data.setMainItem(new MainItem());
                            }
                        } else {
                            if (tag.equalsIgnoreCase("title")) {
                                xpp.next();
                                data.getMainItem().setTitle(xpp.getText());
                            } else if (tag.equalsIgnoreCase("category")) {
                                xpp.next();
                                data.getMainItem().setCategory(xpp.getText());
                            } else if (tag.equalsIgnoreCase("forecastIssue")) {
                                data.getMainItem().setForecastIssue(xpp.getText());
                            } else if (tag.equalsIgnoreCase("validTime")) {
                                xpp.next();
                                data.getMainItem().setValidTime(xpp.getText());
                            } else if (tag.equalsIgnoreCase("weatherForecast")) {
                                data.getMainItem().setWeatherForecast(new ArrayList<WeatherForecast>());
                            }
                        }
                        break;

                    case XmlPullParser.TEXT:
                        break;

                    case XmlPullParser.END_TAG:
                        tag = xpp.getName();    //테그 이름 얻어오기
                        if (tag.equals("forecastIssue")) {
                            String date = xpp.getAttributeValue(null, "date");
                            String time = xpp.getAttributeValue(null, "time");
                            data.getMainItem().setForecastIssue(date + " " + time);
                        } else if (tag.equals("area")) {
                            WeatherForecast weatherForecast = new WeatherForecast();
                            weatherForecast.setForecast(xpp.getAttributeValue(null, "forecast"));
                            weatherForecast.setLat(xpp.getAttributeValue(null, "lat"));
                            weatherForecast.setLon(xpp.getAttributeValue(null, "lon"));
                            weatherForecast.setName(xpp.getAttributeValue(null, "name"));
                            data.getMainItem().getWeatherForecast().add(weatherForecast);
                        }
                        break;
                }
                eventType = xpp.next();
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void updateUI(){
        for (WeatherForecast weatherForecast : data.getMainItem().getWeatherForecast()) {
            addMarker(weatherForecast, false);
        }
    }

    private Marker addMarker(WeatherForecast markerItem, boolean isSelectedMarker) {
        LatLng position = new LatLng(Double.valueOf(markerItem.getLat()), Double.valueOf(markerItem.getLon()));
        String name = markerItem.getName();
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 14));
        tv_marker.setText(name);
        tv_marker.setTextColor(Color.BLACK);

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.title(name);
        markerOptions.position(position);
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(createDrawableFromView(marker_root_view)));

        return mMap.addMarker(markerOptions);
    }

    private Bitmap createDrawableFromView(View view) {

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        view.measure(displayMetrics.widthPixels, displayMetrics.heightPixels);
        view.layout(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels);
        view.buildDrawingCache();
        Bitmap bitmap = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);

        return bitmap;
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {

            case PERMISSION_LOCATION_REQUEST_CODE: // for location
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                            .findFragmentById(R.id.map);
                    mapFragment.getMapAsync(this);
                } else {
                    // Goto Add devices screen if NOT accessed from side menu, else wehre to go?
                }
                break;
        }
    }

    public void connectionUrl(){
        Observable observable = Observable.create(new Observable.OnSubscribe<Object>() {
            @Override
            public void call(Subscriber<? super Object> subscriber) {
                String datasetName = "2hr_nowcast";
                String keyref = "781CF461BB6606AD1260F4D81345157F9F25285B80002F64";
                try {
                    String urlString = "http://www.nea.gov.sg/api/WebAPI?dataset=" + datasetName + "&keyref=" + keyref;

                    // Step 2: Call API Url
                    URL obj = new URL(urlString);
                    HttpURLConnection con = (HttpURLConnection)obj.openConnection();
                    con.setRequestMethod("GET");
                    int responseCode = con.getResponseCode();

                    // Step 3: Check the response status
                    if(responseCode == 200) {
                        getXmlData(con.getInputStream());
                        subscriber.onNext(null);
                    } else {
                        subscriber.onError(new Throwable());
                    }

                    subscriber.onCompleted();
                } catch (Exception e) {
                    System.out.println(e);
                }

            }
        }).subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread());
        observable.subscribe(new Observer<InputStream>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onNext(InputStream is) {
                updateUI();
            }
        });
    }

    public void getLocationAddress(final double lat, final double lon) {
        Observable observable = Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {

                String address = lat + ", " + lon;

                Geocoder geocoder = new Geocoder(getApplicationContext());
                try {
                    List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);

                    if (addresses != null && addresses.size() > 0) {
                        int maxAddressLine = addresses.get(0).getMaxAddressLineIndex();
                        StringBuilder addr = new StringBuilder();
                        for (int i = 0; i < maxAddressLine; i++) {
                            addr.append(addresses.get(0).getAddressLine(i));
                            addr.append(" ");
                        }
                        if (!TextUtils.isEmpty(addr.toString())) {
                            address = addr.toString();
                        }
                    }
                } catch (IOException e) {
                    // do nothing
                }

                subscriber.onNext(address);
                subscriber.onCompleted();
            }
        });

        observable.subscribe(new Observer() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Object o) {

            }
        });
    }
}