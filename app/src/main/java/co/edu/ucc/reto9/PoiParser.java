package co.edu.ucc.reto9;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;

/** Convierte nodos y centros de polígonos OSM en lugares dentro del radio. */
public final class PoiParser {
    private PoiParser() {}

    public static ArrayList<MainActivity.Poi> parse(String body, double lat, double lon, double radius)
            throws JSONException {
        JSONObject data = new JSONObject(body);
        if (data.has("remark")) throw new JSONException(data.getString("remark"));
        JSONArray elements = data.getJSONArray("elements");
        ArrayList<MainActivity.Poi> places = new ArrayList<>();
        HashSet<String> ids = new HashSet<>();
        for (int i = 0; i < elements.length(); i++) {
            JSONObject element = elements.getJSONObject(i);
            JSONObject position = element.has("center") ? element.getJSONObject("center") : element;
            JSONObject tags = element.optJSONObject("tags");
            if (tags == null || !position.has("lat") || !position.has("lon")) continue;
            String id = element.optString("type") + "/" + element.optString("id");
            if (element.has("id") && !ids.add(id)) continue;
            double a = position.getDouble("lat"), b = position.getDouble("lon");
            if (!GeoUtils.valid(a, b)) continue;
            double distance = GeoUtils.distanceKm(lat, lon, a, b);
            boolean hospital = "hospital".equals(tags.optString("amenity"))
                    || "hospital".equals(tags.optString("healthcare"));
            String name = "";
            for (String field : new String[]{"name", "name:es", "official_name", "short_name", "operator", "brand"}) {
                name = tags.optString(field).trim();
                if (!name.isEmpty()) break;
            }
            if (name.isEmpty()) {
                // Conserva hospitales reales sin inventar un nombre propio.
                if (!hospital) continue;
                name = "Hospital sin nombre registrado en OSM";
            }
            if (hospital && ("veterinary".equals(tags.optString("amenity"))
                    || "veterinary".equals(tags.optString("healthcare"))
                    || tags.optString("healthcare:speciality").contains("veterinary")
                    || name.toLowerCase(java.util.Locale.ROOT).contains("veterinar"))) continue;
            if (distance <= radius) {
                MainActivity.Poi poi = new MainActivity.Poi(name, a, b, distance);
                poi.hospital = hospital;
                places.add(poi);
            }
        }
        places.sort(Comparator.comparingDouble(place -> place.distance));
        return places;
    }
}
