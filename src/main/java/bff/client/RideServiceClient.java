package bff.client;

import bff.client.HttpClientHelper.Response;
import bff.config.ServiceConfig;

import java.io.IOException;
import java.util.Map;

public class RideServiceClient {

    public Response start(Object body, String jwt) throws IOException, InterruptedException {
        return HttpClientHelper.post(ServiceConfig.ride() + "/api/rides", jwt, body);
    }

    public Response finish(Long id, Object body, String jwt) throws IOException, InterruptedException {
        return HttpClientHelper.put(ServiceConfig.ride() + "/api/rides/" + id + "/finish", jwt, body);
    }

    public Response pay(Long id, String jwt) throws IOException, InterruptedException {
        return HttpClientHelper.post(ServiceConfig.ride() + "/api/rides/" + id + "/pay", jwt, Map.of());
    }

    public Response byUser(Long userId, String jwt) throws IOException, InterruptedException {
        return HttpClientHelper.get(ServiceConfig.ride() + "/api/rides?userId=" + userId, jwt);
    }
}
