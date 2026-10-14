package bff.client;

import bff.client.HttpClientHelper.Response;
import bff.config.ServiceConfig;

import java.io.IOException;

public class ModerationServiceClient {

    public Response isBlocked(Long userId, String jwt) throws IOException, InterruptedException {
        return HttpClientHelper.get(
                ServiceConfig.moderation() + "/api/moderation/users/" + userId + "/blocked", jwt);
    }
}
