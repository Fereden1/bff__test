package bff.client;

import bff.client.HttpClientHelper.Response;
import bff.config.ServiceConfig;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class ScooterServiceClient {

    public Response list(String jwt, boolean readyOnly) throws IOException, InterruptedException {
        String url = ServiceConfig.scooter() + "/api/scooters"
                + (readyOnly ? "?status=READY" : "");
        return HttpClientHelper.get(url, jwt);
    }

    public Response nearest(String jwt, Object body) throws IOException, InterruptedException {
        return HttpClientHelper.post(ServiceConfig.scooter() + "/api/scooters/nearest", jwt, body);
    }

    public Response getByNumberPlate(String numberPlate, String jwt)
            throws IOException, InterruptedException {
        return HttpClientHelper.get(
                ServiceConfig.scooter() + "/api/scooters/" + enc(numberPlate), jwt);
    }

    private static String enc(String v) {
        return URLEncoder.encode(v, StandardCharsets.UTF_8);
    }
}
