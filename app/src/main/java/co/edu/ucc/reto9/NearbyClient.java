package co.edu.ucc.reto9;

import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CancellationException;

/** Consulta cancelable, POST y recuperación también ante respuestas parciales HTTP 200. */
public final class NearbyClient {
    public static final int READ_TIMEOUT_MS = 60000; // Mayor que timeout:40 del servidor.
    private static final String[] SERVERS = {
        "https://overpass-api.de/api/interpreter",
        "https://maps.mail.ru/osm/tools/overpass/api/interpreter",
        "https://overpass.private.coffee/api/interpreter"
    };
    interface Transport { String post(String server, String query) throws Exception; }
    interface Progress { void trying(int number, int total); }
    private final String[] servers;
    private final Transport transport;
    private volatile boolean canceled;
    private volatile HttpURLConnection connection;

    public NearbyClient() { servers = SERVERS; transport = this::post; }
    NearbyClient(String[] servers, Transport transport) { this.servers = servers; this.transport = transport; }

    public String fetch(String query, Progress progress) throws Exception {
        Exception last = null;
        for (int i = 0; i < servers.length; i++) {
            checkCanceled();
            if (progress != null) progress.trying(i + 1, servers.length);
            try {
                String body = transport.post(servers[i], query);
                checkCanceled();
                JSONObject json = new JSONObject(body);
                if (json.has("remark")) throw new IOException("Respuesta incompleta: " + json.getString("remark"));
                json.getJSONArray("elements");
                return body;
            } catch (CancellationException e) { throw e; }
            catch (Exception e) { checkCanceled(); last = e; }
        }
        throw new IOException("Ningún servidor pudo completar la consulta", last);
    }

    private String post(String server, String query) throws Exception {
        checkCanceled();
        HttpURLConnection current = (HttpURLConnection) new URL(server).openConnection();
        connection = current;
        try {
            checkCanceled();
            current.setRequestMethod("POST");
            current.setConnectTimeout(10000);
            current.setReadTimeout(READ_TIMEOUT_MS);
            current.setRequestProperty("User-Agent", "Reto9AccediendoalGPS/1.1 (Android)");
            current.setRequestProperty("Accept", "application/json");
            current.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            byte[] payload = ("data=" + URLEncoder.encode(query, "UTF-8")).getBytes(StandardCharsets.UTF_8);
            current.setDoOutput(true);
            current.setFixedLengthStreamingMode(payload.length);
            try (java.io.OutputStream out = current.getOutputStream()) { out.write(payload); }
            if (current.getResponseCode() != 200) throw new IOException("HTTP " + current.getResponseCode());
            try (InputStream in = current.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] bytes = new byte[8192];
                int count;
                while ((count = in.read(bytes)) != -1) {
                    checkCanceled();
                    out.write(bytes, 0, count);
                    if (out.size() > 8 * 1024 * 1024) throw new IOException("Respuesta demasiado grande");
                }
                return new String(out.toByteArray(), StandardCharsets.UTF_8);
            }
        } finally { current.disconnect(); connection = null; }
    }

    public void cancel() {
        canceled = true;
        HttpURLConnection current = connection;
        if (current != null) current.disconnect();
    }

    private void checkCanceled() {
        if (canceled || Thread.currentThread().isInterrupted()) throw new CancellationException();
    }
}
