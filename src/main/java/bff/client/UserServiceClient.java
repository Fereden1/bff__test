package bff.client;

import bff.client.HttpClientHelper.Response;
import bff.config.ServiceConfig;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class UserServiceClient {

    public Response register(Object body) throws IOException, InterruptedException {
        return HttpClientHelper.post(ServiceConfig.user() + "/api/users/register", null, body);
    }

    public Response login(Object body) throws IOException, InterruptedException {
        return HttpClientHelper.post(ServiceConfig.user() + "/api/users/login", null, body);
    }

    public Response me(String jwt) throws IOException, InterruptedException {
        return HttpClientHelper.get(ServiceConfig.user() + "/api/users/me", jwt);
    }

    public Response exists(String login) throws IOException, InterruptedException {
        String q = URLEncoder.encode(login, StandardCharsets.UTF_8);
        return HttpClientHelper.get(ServiceConfig.user() + "/api/users/exists?login=" + q, null);
    }

    public Response validate(String jwt) throws IOException, InterruptedException {
        return HttpClientHelper.get(ServiceConfig.user() + "/api/users/validate", jwt);
    }
}
