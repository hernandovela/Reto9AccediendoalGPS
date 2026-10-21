package co.edu.ucc.reto9;
public class GeoUtilsTest extends android.test.AndroidTestCase {
    public void testDistanceIdentity(){assertEquals(0.0,GeoUtils.distanceKm(4.6,-74.1,4.6,-74.1),0.00001);}
    public void testKnownDistance(){assertEquals(111.195,GeoUtils.distanceKm(0,0,1,0),0.01);}
    public void testCoordinateBoundaries(){assertTrue(GeoUtils.valid(-90,180));assertFalse(GeoUtils.valid(91,0));assertFalse(GeoUtils.valid(0,-181));assertFalse(GeoUtils.valid(Double.NaN,0));}
    public void testRadiusBoundaries(){assertTrue(GeoUtils.validRadius(0.1));assertTrue(GeoUtils.validRadius(20));assertFalse(GeoUtils.validRadius(0));assertFalse(GeoUtils.validRadius(20.1));assertFalse(GeoUtils.validRadius(Double.NaN));}
}
