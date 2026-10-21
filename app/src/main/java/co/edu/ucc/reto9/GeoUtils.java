package co.edu.ucc.reto9;

public final class GeoUtils {
    private GeoUtils() {}
    public static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double a = Math.pow(Math.sin(Math.toRadians(lat2-lat1)/2),2)
            + Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))*Math.pow(Math.sin(Math.toRadians(lon2-lon1)/2),2);
        return 6371.0088 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(Math.max(0,1-a)));
    }
    public static boolean valid(double lat, double lon) {
        return Double.isFinite(lat) && Double.isFinite(lon) && Math.abs(lat)<=90 && Math.abs(lon)<=180;
    }
    public static boolean validRadius(double km) { return Double.isFinite(km) && km>=0.1 && km<=20; }
}
