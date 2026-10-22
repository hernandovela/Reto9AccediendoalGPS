package co.edu.ucc.reto9;

import java.util.Locale;

/** Primero acota por índice espacial; PoiParser aplica después el radio circular exacto. */
public final class NearbyQuery {
    private NearbyQuery() {}

    public static String build(double lat, double lon, double km, int filter) {
        if (!GeoUtils.valid(lat, lon) || !GeoUtils.validRadius(km) || filter < 0 || filter > 2) {
            throw new IllegalArgumentException("Origen, radio o categoría inválidos");
        }
        double angle = km / 6371.0088;
        double south = Math.max(-90, lat - Math.toDegrees(angle));
        double north = Math.min(90, lat + Math.toDegrees(angle));
        double deltaLon = south <= -90 || north >= 90 ? 180
                : Math.toDegrees(Math.asin(Math.min(1, Math.sin(angle) / Math.cos(Math.toRadians(lat)))));
        double west = lon - deltaLon, east = lon + deltaLon;
        StringBuilder query = new StringBuilder("[out:json][timeout:40];(");
        if (deltaLon >= 180) append(query, south, -180, north, 180, filter);
        else if (west < -180) {
            append(query, south, west + 360, north, 180, filter);
            append(query, south, -180, north, east, filter);
        } else if (east > 180) {
            append(query, south, west, north, 180, filter);
            append(query, south, -180, north, east - 360, filter);
        } else append(query, south, west, north, east, filter);
        return query.append(");out center tags;").toString();
    }

    private static void append(StringBuilder query, double south, double west, double north,
                               double east, int filter) {
        String bbox = String.format(Locale.US, "(%.7f,%.7f,%.7f,%.7f);", south, west, north, east);
        if (filter != 2) {
            query.append("nwr[amenity=hospital]").append(bbox);
            query.append("nwr[healthcare=hospital]").append(bbox);
        }
        if (filter != 1) query.append("nwr[tourism~\"attraction|museum|viewpoint|gallery\"]").append(bbox);
    }
}
