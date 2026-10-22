package co.edu.ucc.reto9;

import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

/** Aceptación con red real: ejecutar explícitamente con -e class ...HospitalLiveCheck. */
@LargeTest
public class HospitalLiveCheck extends ActivityInstrumentationTestCase2<MainActivity> {
    public HospitalLiveCheck() { super(MainActivity.class); }
    private static final double LAT = 4.73257, LON = -74.05723;

    @Override protected void setUp() throws Exception {
        super.setUp();
        getInstrumentation().getTargetContext().getSharedPreferences("explora",0).edit()
                .putBoolean("has_origin",true).putLong("origin_lat",Double.doubleToLongBits(LAT))
                .putLong("origin_lon",Double.doubleToLongBits(LON)).putString("origin_input","4.73257,-74.05723")
                .putLong("radius",Double.doubleToLongBits(20)).putInt("category",1).commit();
    }

    public void testLiveHospitalsWithin20KmRenderedOnMap() throws Exception {
        MainActivity activity = getActivity();
        AtomicReference<String> text = new AtomicReference<>("");
        long deadline = android.os.SystemClock.elapsedRealtime() + 190000;
        while (android.os.SystemClock.elapsedRealtime() < deadline) {
            getInstrumentation().runOnMainSync(() -> text.set(readText(activity.getWindow().getDecorView())));
            if (text.get().contains("hospitales encontrados") || text.get().contains("No se pudo consultar")) break;
            Thread.sleep(500);
        }
        assertTrue("La búsqueda real no terminó correctamente: " + text.get(), text.get().contains("hospitales encontrados"));
        JSONArray markers = new JSONArray();
        getInstrumentation().runOnMainSync(() -> {
            MapView map = findMap(activity.getWindow().getDecorView());
            assertNotNull(map);
            for (Overlay overlay : map.getOverlays()) {
                if (!(overlay instanceof Marker)) continue;
                Marker marker = (Marker)overlay;
                if ("Origen de búsqueda".equals(marker.getTitle())) continue;
                double distance = GeoUtils.distanceKm(LAT,LON,marker.getPosition().getLatitude(),marker.getPosition().getLongitude());
                assertTrue("Un hospital quedó fuera del radio: " + marker.getTitle(),distance<=20);
                assertFalse(marker.getTitle().toLowerCase(java.util.Locale.ROOT).contains("veterinar"));
                assertNotNull(marker.getIcon());
                JSONObject item = new JSONObject();
                try { item.put("name",marker.getTitle());item.put("distance_km",distance);
                    item.put("lat",marker.getPosition().getLatitude());item.put("lon",marker.getPosition().getLongitude()); }
                catch (Exception e) { throw new AssertionError(e); }
                markers.put(item);
            }
        });
        assertTrue("Se esperaban varios hospitales reales",markers.length()>20);
        File folder = activity.getExternalFilesDir(null);
        JSONObject report = new JSONObject();
        report.put("origin_lat",LAT);report.put("origin_lon",LON);report.put("radius_km",20);
        report.put("count",markers.length());report.put("status",text.get());report.put("hospitals",markers);
        try (FileOutputStream output = new FileOutputStream(new File(folder,"hospital-live.json"))) {
            output.write(report.toString(2).getBytes(StandardCharsets.UTF_8));
        }
        Thread.sleep(1500);
        android.graphics.Bitmap screenshot = getInstrumentation().getUiAutomation().takeScreenshot();
        assertNotNull(screenshot);
        try (FileOutputStream output = new FileOutputStream(new File(folder,"hospital-live.png"))) {
            screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG,100,output);
        }
        screenshot.recycle();
    }

    private static String readText(View view) {
        StringBuilder text = new StringBuilder();
        if(view instanceof TextView) text.append(((TextView)view).getText()).append('\n');
        if(view instanceof ViewGroup) for(int i=0;i<((ViewGroup)view).getChildCount();i++)
            text.append(readText(((ViewGroup)view).getChildAt(i)));
        return text.toString();
    }

    private static MapView findMap(View view) {
        if(view instanceof MapView) return (MapView)view;
        if(view instanceof ViewGroup) for(int i=0;i<((ViewGroup)view).getChildCount();i++) {
            MapView result=findMap(((ViewGroup)view).getChildAt(i));if(result!=null)return result;
        }
        return null;
    }
}
