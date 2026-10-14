package bff.client;

import bff.client.HttpClientHelper.Response;
import bff.config.ServiceConfig;

import java.io.IOException;

public class DocumentServiceClient {

    public Response generate(Object body, String jwt) throws IOException, InterruptedException {
        return HttpClientHelper.postBytes(ServiceConfig.document() + "/api/documents/generate", jwt, body);
    }
}
