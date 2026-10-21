package co.edu.ucc.reto9;

import android.Manifest;
import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.*;
import android.view.*;
import android.widget.*;
import org.osmdroid.config.Configuration;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.*;
import org.osmdroid.util.GeoPoint;
import org.json.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private MapView map;
    private TextView status, coordinates, radiusLabel;
    private EditText input;
    private Spinner category;
    private LinearLayout results;
    private android.content.SharedPreferences prefs;
    private double lat=4.60971,lon=-74.08175,radius=2;
    private int generation=0;
    private boolean located=false;
    private LocationManager locationManager;
    private LocationListener listener;
    private final int ink=Color.rgb(20,48,65), teal=Color.rgb(0,112,115);

    @Override public void onCreate(Bundle state) {
        super.onCreate(state); getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        prefs=getSharedPreferences("explora",MODE_PRIVATE);
        radius=Double.longBitsToDouble(prefs.getLong("radius",Double.doubleToLongBits(2)));
        if (!GeoUtils.validRadius(radius)) radius=2;
        if(state!=null){lat=state.getDouble("lat");lon=state.getDouble("lon");located=state.getBoolean("located");}
        Configuration.getInstance().load(this,prefs);
        Configuration.getInstance().setUserAgentValue("Reto9AccediendoalGPS/1.0 (Android; educational)");
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.rgb(244,248,249));
        root.setPadding(16,0,16,0);
        root.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(16,insets.getSystemWindowInsetTop(),16,insets.getSystemWindowInsetBottom());return insets;});
        setContentView(root);
        TextView title=text("EXPLORA",26);title.setTypeface(null,Typeface.BOLD);root.addView(title);
        root.addView(text("RETO 9  /  Lugares cerca de ti",13));
        coordinates=text("Vista inicial: Bogotá · elige GPS o un origen",12);root.addView(coordinates);
        input=new EditText(this);input.setSingleLine(true);input.setTextSize(14);input.setHint("Dirección o latitud, longitud");input.setContentDescription("Dirección o coordenadas");root.addView(input);
        LinearLayout actions=new LinearLayout(this);root.addView(actions);
        button(actions,"Buscar",v->resolve());button(actions,"Mi GPS",v->gps());button(actions,"Radio",v->radiusDialog());
        LinearLayout filters=new LinearLayout(this);root.addView(filters);
        category=new Spinner(this);category.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Todos los lugares","Hospitales","Turismo"}));
        filters.addView(category,new LinearLayout.LayoutParams(0,dp(48),1));radiusLabel=text("",13);filters.addView(radiusLabel);updateRadius();
        category.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){} public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){if(located)search();}});
        map=new MapView(this);map.setMultiTouchControls(true);map.getController().setZoom(14.0);map.getController().setCenter(new GeoPoint(lat,lon));root.addView(map,new LinearLayout.LayoutParams(-1,0,1));
        TextView attribution=text("© OpenStreetMap contributors · datos: Overpass",11);root.addView(attribution);
        status=text("Define un origen para encontrar lugares cercanos.",13);status.setPadding(0,8,0,8);root.addView(status);
        ScrollView scroll=new ScrollView(this);results=new LinearLayout(this);results.setOrientation(LinearLayout.VERTICAL);scroll.addView(results);root.addView(scroll,new LinearLayout.LayoutParams(-1,dp(115)));
        if(located)search();
    }
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private TextView text(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextColor(ink);t.setTextSize(size);t.setPadding(4,4,4,4);return t;}
    private void button(LinearLayout parent,String name,View.OnClickListener click){Button b=new Button(this);b.setText(name);b.setTextColor(teal);b.setTextSize(12);parent.addView(b,new LinearLayout.LayoutParams(0,dp(48),1));b.setOnClickListener(click);}
    private void updateRadius(){radiusLabel.setText(String.format(Locale.US,"Radio: %.1f km",radius));}
    private void radiusDialog(){EditText field=new EditText(this);field.setInputType(8194);field.setText(String.valueOf(radius));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Radio de búsqueda (km)").setMessage("Entre 0.1 y 20 km. Se guarda en preferencias.").setView(field).setNegativeButton("Cancelar",null).setPositiveButton("Guardar",null).create();
        dialog.setOnShowListener(d->dialog.getButton(-1).setOnClickListener(v->{try{double value=Double.parseDouble(field.getText().toString().replace(',','.'));if(!GeoUtils.validRadius(value))throw new Exception();radius=value;prefs.edit().putLong("radius",Double.doubleToLongBits(radius)).apply();updateRadius();dialog.dismiss();if(located)search();}catch(Exception e){field.setError("Usa un valor entre 0.1 y 20");}}));dialog.show();}
    private void resolve(){String value=input.getText().toString().trim();if(value.isEmpty() && located){search();return;}if(value.isEmpty()){input.setError("Escribe una dirección o coordenadas");return;}
        String[] parts=value.split(",");if(parts.length==2){try{double a=Double.parseDouble(parts[0].trim()),b=Double.parseDouble(parts[1].trim());if(!GeoUtils.valid(a,b)){input.setError("Coordenadas fuera de rango");return;}origin(a,b);return;}catch(NumberFormatException ignored){}}
        status.setText("Buscando dirección…");final int request=++generation;
        worker.execute(()->{try{String body=get("https://nominatim.openstreetmap.org/search?format=json&limit=1&q="+URLEncoder.encode(value,"UTF-8"));JSONArray list=new JSONArray(body);if(list.length()==0)throw new IOException("No se encontró la dirección");JSONObject o=list.getJSONObject(0);double a=o.getDouble("lat"),b=o.getDouble("lon");handler.post(()->{if(request==generation)origin(a,b);});}catch(Exception e){error(request,"No se pudo resolver la dirección. Revisa la conexión o usa coordenadas.");}});
    }
    private void origin(double a,double b){lat=a;lon=b;located=true;coordinates.setText(String.format(Locale.US,"Origen: %.5f, %.5f",lat,lon));map.getController().setCenter(new GeoPoint(lat,lon));search();}
    private void gps(){if(checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},10);return;}
        locationManager=(LocationManager)getSystemService(LOCATION_SERVICE);
        String provider=checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED?LocationManager.GPS_PROVIDER:LocationManager.NETWORK_PROVIDER;
        if(!locationManager.isProviderEnabled(provider)){status.setText("Activa la ubicación del dispositivo y vuelve a intentar.");startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));return;}
        status.setText("Esperando ubicación GPS…");if(listener!=null)locationManager.removeUpdates(listener);
        listener=new LocationListener(){@Override public void onLocationChanged(Location l){locationManager.removeUpdates(this);origin(l.getLatitude(),l.getLongitude());}};
        locationManager.requestLocationUpdates(provider,1000,0,listener);
        handler.postDelayed(()->{if(listener!=null && status.getText().toString().equals("Esperando ubicación GPS…")){locationManager.removeUpdates(listener);status.setText("Sin señal GPS. Reintenta o escribe coordenadas.");}},30000);
    }
    @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] grants){super.onRequestPermissionsResult(code,permissions,grants);if(code==10){if(checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED)gps();else status.setText("Permiso denegado. Puedes buscar por dirección o coordenadas.");}}
    private String get(String url)throws Exception {HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(10000);c.setReadTimeout(20000);c.setRequestProperty("User-Agent","Reto9AccediendoalGPS/1.0 (educational Android project)");try{if(c.getResponseCode()!=200)throw new IOException("HTTP "+c.getResponseCode());try(InputStream in=c.getInputStream()){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);return new String(out.toByteArray(),StandardCharsets.UTF_8);}}finally{c.disconnect();}}
    private String overpass(String query) throws Exception {Exception last=null; for(String host:new String[]{"https://overpass-api.de/api/interpreter","https://overpass.private.coffee/api/interpreter"}){try{return get(host+"?data="+URLEncoder.encode(query,"UTF-8"));}catch(Exception e){last=e;}}throw last;}
    private void error(int request,String message){handler.post(()->{if(request==generation)status.setText(message);});}
    private void search(){final int request=++generation;final double a=lat,b=lon,r=radius;final int filter=category.getSelectedItemPosition();
        map.getOverlays().clear();Marker origin=new Marker(map);origin.setPosition(new GeoPoint(a,b));origin.setTitle("Origen de búsqueda");map.getOverlays().add(origin);
        Polygon circle=new Polygon();circle.setPoints(Polygon.pointsAsCircle(new GeoPoint(a,b),r*1000));circle.getOutlinePaint().setColor(teal);circle.getOutlinePaint().setStrokeWidth(2);circle.getFillPaint().setColor(0x15007073);map.getOverlays().add(0,circle);map.invalidate();results.removeAllViews();status.setText("Consultando lugares en "+r+" km…");
        worker.execute(()->{try{String around=String.format(Locale.US,"(around:%.0f,%.6f,%.6f)",r*1000,a,b);String query="[out:json][timeout:25];("+(filter!=2?"nwr[amenity=hospital][name]"+around+";":"")+(filter!=1?"nwr[tourism~\"attraction|museum|viewpoint|gallery\"][name]"+around+";":"")+");out center;";
            ArrayList<Poi> pois=PoiParser.parse(overpass(query),a,b,r);handler.post(()->{if(request!=generation||isDestroyed())return;status.setText(pois.isEmpty()?"Sin lugares en este radio. Prueba ampliarlo.":pois.size()+" lugares encontrados · toca un nombre");for(Poi p:pois){Marker m=new Marker(map);m.setPosition(new GeoPoint(p.lat,p.lon));m.setTitle(p.name);m.setSnippet(String.format(Locale.US,"%.2f km del origen",p.distance));map.getOverlays().add(m);TextView row=text(String.format(Locale.US,"%s  ·  %.2f km",p.name,p.distance),14);row.setPadding(8,12,8,12);row.setOnClickListener(v->{map.getController().animateTo(m.getPosition());m.showInfoWindow();});results.addView(row);}map.invalidate();});
        }catch(Exception e){android.util.Log.w("Explora","Consulta Overpass fallida",e);error(request,"No se pudieron cargar lugares. Revisa Internet y pulsa Buscar para reintentar.");}});
    }
    static class Poi{String name;double lat,lon,distance;Poi(String n,double a,double b,double d){name=n;lat=a;lon=b;distance=d;}}
    @Override protected void onSaveInstanceState(Bundle out){out.putDouble("lat",lat);out.putDouble("lon",lon);out.putBoolean("located",located);super.onSaveInstanceState(out);}
    @Override protected void onResume(){super.onResume();if(map!=null)map.onResume();}
    @Override protected void onPause(){if(map!=null)map.onPause();if(locationManager!=null&&listener!=null)locationManager.removeUpdates(listener);super.onPause();}
    @Override protected void onDestroy(){generation++;worker.shutdownNow();handler.removeCallbacksAndMessages(null);if(map!=null)map.onDetach();super.onDestroy();}
}
