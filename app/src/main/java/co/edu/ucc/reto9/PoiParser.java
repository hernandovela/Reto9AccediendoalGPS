package co.edu.ucc.reto9;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Comparator;

/** Convierte nodos y centros de polígonos OSM en lugares dentro del radio. */
public final class PoiParser {
    private PoiParser() {}

    public static ArrayList<MainActivity.Poi> parse(String body, double lat, double lon, double radius)
            throws JSONException {
        JSONObject data = new JSONObject(body);
        if (data.has("remark")) throw new JSONException(data.getString("remark"));
        JSONArray elements = data.getJSONArray("elements");
        ArrayList<MainActivity.Poi> places = new ArrayList<>();
        for (int i = 0; i < elements.length(); i++) {
            JSONObject element = elements.getJSONObject(i);
            JSONObject position = element.has("center") ? element.getJSONObject("center") : element;
            JSONObject tags = element.optJSONObject("tags");
            if (tags == null || tags.optString("name").trim().isEmpty()
                    || !position.has("lat") || !position.has("lon")) continue;
            double a = position.getDouble("lat"), b = position.getDouble("lon");
            if (!GeoUtils.valid(a, b)) continue;
            double distance = GeoUtils.distanceKm(lat, lon, a, b);
            if (distance <= radius) places.add(new MainActivity.Poi(tags.getString("name"), a, b, distance));
        }
        places.sort(Comparator.comparingDouble(place -> place.distance));
        return places;
    }
}
