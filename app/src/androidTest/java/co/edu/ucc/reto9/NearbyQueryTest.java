package co.edu.ucc.reto9;

public class NearbyQueryTest extends android.test.AndroidTestCase {
    public void testHospitalQueryUsesSpatialIndexAndBothTags() {
        String query = NearbyQuery.build(4.73257, -74.05723, 20, 1);
        assertTrue(query.contains("amenity=hospital"));
        assertTrue(query.contains("healthcare=hospital"));
        assertFalse(query.contains("[name]"));
        assertFalse(query.contains("tourism"));
        assertFalse(query.contains("around:"));
        assertTrue(NearbyClient.READ_TIMEOUT_MS > 40000);
    }
    public void testTourismDoesNotIncludeHospitals() {
        String query = NearbyQuery.build(4.7,-74,1,2);
        assertFalse(query.contains("hospital"));
        assertTrue(query.contains("tourism"));
    }
    public void testDateLineAndPolesProduceValidQueries() {
        String dateLine = NearbyQuery.build(0,179.99,20,1);
        assertTrue(dateLine.contains(",-180.0000000,"));
        assertTrue(dateLine.contains(",180.0000000)"));
        String pole = NearbyQuery.build(90,0,20,1);
        assertFalse(pole.contains("NaN"));
        assertTrue(pole.contains("90.0000000,180.0000000"));
    }
    public void testInvalidOriginIsRejected() {
        try { NearbyQuery.build(95,0,20,1); fail("Origen inválido"); }
        catch (IllegalArgumentException expected) { }
    }
}
