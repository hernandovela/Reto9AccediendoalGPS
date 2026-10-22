package co.edu.ucc.reto9;

public class NearbyClientTest extends android.test.AndroidTestCase {
    public void testIncompleteHttp200FallsBack() throws Exception {
        int[] calls = {0};
        NearbyClient client = new NearbyClient(new String[]{"first", "second"}, (host, query) -> {
            calls[0]++;
            return host.equals("first") ? "{\"remark\":\"runtime timeout\",\"elements\":[]}"
                    : "{\"elements\":[]}";
        });
        assertEquals("{\"elements\":[]}", client.fetch("test", null));
        assertEquals(2, calls[0]);
    }

    public void testMalformedResponseFallsBack() throws Exception {
        NearbyClient client = new NearbyClient(new String[]{"first", "second"}, (host, query) ->
                host.equals("first") ? "<html>Gateway error</html>" : "{\"elements\":[]}");
        assertEquals("{\"elements\":[]}", client.fetch("test", null));
    }

    public void testValidEmptyResponseIsNotAFailure() throws Exception {
        int[] calls = {0};
        NearbyClient client = new NearbyClient(new String[]{"first", "second"}, (host, query) -> {
            calls[0]++;
            return "{\"elements\":[]}";
        });
        client.fetch("test", null);
        assertEquals(1, calls[0]);
    }

    public void testAllServersFailIsNotAnEmptyResult() throws Exception {
        NearbyClient client = new NearbyClient(new String[]{"first", "second"}, (host, query) -> {
            throw new java.net.SocketTimeoutException("timeout");
        });
        try { client.fetch("test", null); fail("No debe devolver un listado vacío"); }
        catch (java.io.IOException expected) { assertTrue(expected.getCause() instanceof java.net.SocketTimeoutException); }
    }

    public void testCancelPreventsFurtherRequests() throws Exception {
        int[] calls = {0};
        NearbyClient client = new NearbyClient(new String[]{"first"}, (host, query) -> {
            calls[0]++;
            return "{\"elements\":[]}";
        });
        client.cancel();
        try { client.fetch("test", null); fail("Debe cancelar"); }
        catch (java.util.concurrent.CancellationException expected) { assertEquals(0, calls[0]); }
    }
}
