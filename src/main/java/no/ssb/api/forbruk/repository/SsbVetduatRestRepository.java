package no.ssb.api.forbruk.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class SsbVetduatRestRepository {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Value("${ssbvetduat.url}")
    private String ssbVetduatUrl;

    @Value("${ssbvetduat.endpoint}")
    private String ssbVetduatEndpoint;

    @Value("${ssbvetduat.api.keys}")
    private String ssbVetduatApiKeys;

    public String callSsbVetDuAt(String codeType, String codes) {
        log.info("call ssb-vetduat-api med {}", codes);
        String result = "";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ssbVetduatUrl + ssbVetduatEndpoint + "/" + codeType + "/" + codes))
                .GET()
                .header("Content-Type", "application/json")
                .header("api_key", ssbVetduatApiKeys)
                .build();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpResponse response = client.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("response: body: {}, status: {}", response.body(), response.statusCode());
            result = response.body() != null ? response.body().toString() : "{\"produktInfo\":{\"codes\":\"" + codes + "\"},{\"respons\":\"ingen respons\"}";
        } catch (IOException e) {
            log.error("something wrong in calling ssb-vetduat-api: {}", e.getMessage());
        } catch (InterruptedException e) {
            log.error("interrupted exception calling ssb-vetduat-api: {}", e.getMessage());
        }
        log.info("result: {}", result);
        return result;
    }


}
