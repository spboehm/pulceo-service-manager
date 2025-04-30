package dev.pulceo.prm.api;

import dev.pulceo.prm.api.exception.PrmApiException;
import dev.pulceo.prm.dto.node.NodeDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.List;
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
    private final static String PRM_LINKS_API_BASE_PATH = "/api/v1/links";
    private final static String PRM_RESOURCES_API_BASE_PATH = "/api/v1/resources";
    private final static String PRM_ORCHESTRATION_CONTEXT_API_BASE_PATH = "/api/v1/orchestration-context";
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
            if (node == null) {
                throw new PrmApiException("Failed to get node from PRM");
            }
            this.nodeCache.put(id, node);
            this.nodeCache.put(node.getNode().getName(), node);
            this.nodeCache.put(node.getPnaUUID().toString(), node);
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
                .uri(this.prmEndpoint + PRM_NODES_API_BASE_PATH + "/" + id)
                .retrieve()
                .bodyToMono(NodeDTO.class)
                .onErrorResume(e -> {
                    throw new RuntimeException(new PrmApiException("Failed to get node from PRM", e));
                })
                .block();
    }

    public List<NodeDTO> getAllNodes() {
        return webClient
                .get()
                .uri(this.prmEndpoint + PRM_NODES_API_BASE_PATH)
                .retrieve()
                .bodyToFlux(NodeDTO.class)
                .collectList()
                .onErrorResume(e -> {
                    throw new RuntimeException(new PrmApiException("Failed to get nodes from PRM", e));
                })
                .block();
    }

    public byte[] getAllNodesRaw() {
        return this.getRaw(URI.create(this.prmEndpoint + PRM_NODES_API_BASE_PATH));
    }

    public byte[] getAllLinksRaw() {
        return this.getRaw(URI.create(this.prmEndpoint + PRM_LINKS_API_BASE_PATH));
    }

    public byte[] getAllCpusRaw() {
        return this.getRaw(URI.create(this.prmEndpoint + PRM_RESOURCES_API_BASE_PATH + "/cpus"));
    }

    public byte[] getAllMemoryRaw() {
        return this.getRaw(URI.create(this.prmEndpoint + PRM_RESOURCES_API_BASE_PATH + "/memory"));
    }

    public byte[] getAllStorageRaw() {
        return this.getRaw(URI.create(this.prmEndpoint + PRM_RESOURCES_API_BASE_PATH + "/storage"));
    }

    private byte[] getRaw(URI uri) {
        return webClient
                .get()
                .uri(uri)
                .retrieve()
                .bodyToMono(byte[].class)
                .onErrorResume(e -> {
                    throw new RuntimeException(new PrmApiException("Failed to get nodes from PRM", e));
                })
                .block();
    }

    public String resolvePnaUuidToGlobalId(String pnaUUID) throws PrmApiException {
        String globalId = this.getNodeById(pnaUUID).getUuid().toString();
        if (globalId == null || globalId.isEmpty()) {
            throw new PrmApiException("Failed to resolve PNA UUID to global ID");
        } else {
            return globalId;
        }
    }

    public void resetOrchestrationContext() {
        this.logger.info("Reset orchestration context on PRM");
        this.webClient
                .post()
                .uri(this.prmEndpoint + PRM_ORCHESTRATION_CONTEXT_API_BASE_PATH + "/reset")
                .retrieve()
                .bodyToMono(Void.class)
                .timeout(Duration.ofSeconds(30))
                .doOnSuccess(response -> {
                    this.logger.info("Successfully reset orchestration context on PRM");
                })
                .onErrorResume(e -> {
                    this.logger.error("Failed to reset orchestration context on PRM: {}", e.getMessage());
                    return Mono.empty();
                })
                .subscribe();
    }

    private boolean checkIfUUID(String uuid) {
        String uuidRegex = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
        return uuid.matches(uuidRegex);
    }

}
