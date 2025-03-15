package dev.pulceo.prm.api;

import dev.pulceo.prm.api.exception.PrmApiException;
import dev.pulceo.prm.dto.node.NodeDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class PrmApi {

    private final ConcurrentHashMap<String, NodeDTO> nodeCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> pnaTokenCache = new ConcurrentHashMap<>();

    private final Logger logger = LoggerFactory.getLogger(PrmApi.class);
    @Value("${prm.endpoint}")
    private String prmEndpoint;
    private final WebClient webClient;
    private final static String PRM_NODES_API_BASE_PATH = "/api/v1/nodes";
    @Value("${webclient.scheme}")
    private String webClientScheme;

    @Autowired
    public PrmApi(WebClient webClient) {
        this.webClient = webClient;
    }

    public NodeDTO getNodeById(String id) throws PrmApiException {
        if (nodeCache.containsKey(id)) {
            return nodeCache.get(id);
        } else {
            NodeDTO node = requestNodeFromPRM(id);
            this.nodeCache.put(id, node);
            this.nodeCache.put(node.getNode().getName(), node);
            return node;
        }
    }

    public String getPnaTokenByNodeId(String id) throws PrmApiException {
        if (pnaTokenCache.containsKey(id)) {
            return pnaTokenCache.get(id);
        } else {
            String pnaToken = webClient.get()
                    .uri(this.prmEndpoint + "/api/v1/nodes/" + id + "/pna-token")
                    .retrieve()
                    .bodyToMono(String.class)
                    .onErrorResume(error -> {
                        throw new RuntimeException(new PrmApiException("Failed to get PNA token for %s from PRM".formatted(id)));
                    })
                    .block();
            if (pnaToken == null) {
                throw new PrmApiException("Failed to get PNA token from PRM");
            }
            pnaTokenCache.put(id, pnaToken);
            return pnaToken;
        }
    }

    private NodeDTO requestNodeFromPRM(String id) {
        return webClient
                .get()
                .uri(this.webClientScheme + "://" + prmEndpoint + PRM_NODES_API_BASE_PATH + "/" + id)
                .retrieve()
                .bodyToMono(NodeDTO.class)
                .onErrorResume(e -> {
                    throw new RuntimeException(new PrmApiException("Failed to get node from PRM", e));
                })
                .block();
    }

    private boolean checkIfUUID(String uuid) {
        String uuidRegex = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
        return uuid.matches(uuidRegex);
    }

}
