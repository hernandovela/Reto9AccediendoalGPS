package co.edu.ucc.reto9;

/** Datos sintéticos controlados: estas pruebas no acreditan disponibilidad de Overpass. */
public class PoiParserTest extends android.test.AndroidTestCase {
    public void testNamedNodeAndPolygonSortedByDistance() throws Exception {
        String json="{\"elements\":[{\"center\":{\"lat\":0.01,\"lon\":0},\"tags\":{\"name\":\"Museo de prueba\"}},{\"lat\":0,\"lon\":0,\"tags\":{\"name\":\"Hospital de prueba\"}}]}";
        java.util.ArrayList<MainActivity.Poi> result=PoiParser.parse(json,0,0,2);
        assertEquals(2,result.size());
        assertEquals("Hospital de prueba",result.get(0).name);
        assertEquals("Museo de prueba",result.get(1).name);
    }
    public void testOutsideRadiusAndUnnamedExcluded() throws Exception {
        String json="{\"elements\":[{\"lat\":1,\"lon\":0,\"tags\":{\"name\":\"Lejano\"}},{\"lat\":0,\"lon\":0,\"tags\":{}}]}";
        assertTrue(PoiParser.parse(json,0,0,1).isEmpty());
    }
    public void testEmptyResponse() throws Exception {
        assertTrue(PoiParser.parse("{\"elements\":[]}",0,0,1).isEmpty());
    }
    public void testServerRemarkIsAnError() throws Exception {
        try {PoiParser.parse("{\"remark\":\"timeout\",\"elements\":[]}",0,0,1);fail("Debe informar el error del servidor");}
        catch(org.json.JSONException expected) {assertEquals("timeout",expected.getMessage());}
    }
}
