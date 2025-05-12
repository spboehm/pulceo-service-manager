package dev.pulceo.prm.api;

import dev.pulceo.prm.api.dto.resource.PrmResources;
import dev.pulceo.prm.api.exception.PrmApiException;
import dev.pulceo.prm.dto.node.NodeDTO;
import dev.pulceo.prm.util.FileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PrmApi {

    private final ConcurrentHashMap<String, NodeDTO> nodeCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> pnaTokenCache = new ConcurrentHashMap<>();

    private final Logger logger = LoggerFactory.getLogger(PrmApi.class);
    @Value("${prm.endpoint}")
    private String prmEndpoint;
    private final WebClient webClient;
    private final static String PRM_PROVIDERS_API_BASE_PATH = "/api/v1/providers";
    private final static String PRM_NODES_API_BASE_PATH = "/api/v1/nodes";
    private final static String PRM_LINKS_API_BASE_PATH = "/api/v1/links";
    private final static String PRM_RESOURCES_API_BASE_PATH = "/api/v1/resources";
    private final static String PRM_ORCHESTRATION_CONTEXT_API_BASE_PATH = "/api/v1/orchestration-context";
    @Value("${webclient.scheme}")
    private String webClientScheme;
    private final ApiUtils apiUtils;
    @Value("${pulceo.data.dir}")
    private String psmDataDir;
    private final FileManager fileManager;

    @Autowired
    public PrmApi(WebClient webClient, ApiUtils apiUtils, FileManager fileManager) {
        this.webClient = webClient;
        this.apiUtils = apiUtils;
        this.fileManager = fileManager;
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

    public void requestResource(UUID orchestrationUuid, PrmResources resourceType, boolean cleanUp) throws PrmApiException {
        this.logger.info("Request static orchestration data with orchestrationUuid={} resourceType={} and cleanUp={}", orchestrationUuid, resourceType, cleanUp);
        Path requestedFilePath = Path.of(this.psmDataDir, "raw", orchestrationUuid.toString(), resourceType + ".json");
        boolean requestedFileExists = this.fileManager.checkIfRequestedFileExists(requestedFilePath);

        if (requestedFileExists) {
            if (!cleanUp) {
                return;
            }
        }
        this.requestResource(orchestrationUuid, resourceType);
    }

    private void requestResource(UUID orchestrationUuid, PrmResources resourceType) throws PrmApiException {
        switch (resourceType) {
            case PROVIDERS -> requestProviders(orchestrationUuid);
            case NODES -> requestNodes(orchestrationUuid);
            case LINKS -> requestLinks(orchestrationUuid);
            case CPUS -> requestCPUs(orchestrationUuid);
            case MEMORY -> requestMemory(orchestrationUuid);
            case STORAGE -> requestStorage(orchestrationUuid);
            default -> throw new PrmApiException("Unsupported resource type: " + resourceType);
        }
    }

    private void requestStorage(UUID orchestrationUuid) {
        byte[] storageRaw = this.getAllStorageRaw();
        this.fileManager.saveAsJson(storageRaw, "raw", orchestrationUuid.toString(), "STORAGE.json");
    }

    private void requestMemory(UUID orchestrationUuid) {
        byte[] memoryRaw = this.getAllMemoryRaw();
        this.fileManager.saveAsJson(memoryRaw, "raw", orchestrationUuid.toString(), "MEMORY.json");
    }

    private void requestCPUs(UUID orchestrationUuid) {
        byte[] cpusRaw = this.getAllCpusRaw();
        this.fileManager.saveAsJson(cpusRaw, "raw", orchestrationUuid.toString(), "CPUS.json");
    }

    private void requestLinks(UUID orchestrationUuid) {
        byte[] linksRaw = this.getAllLinksRaw();
        this.fileManager.saveAsJson(linksRaw, "raw", orchestrationUuid.toString(), "LINKS.json");
    }

    private void requestNodes(UUID orchestrationUuid) {
        byte[] nodesRaw = this.getAllNodesRaw();
        this.fileManager.saveAsJson(nodesRaw, "raw", orchestrationUuid.toString(), "NODES.json");
    }

    private void requestProviders(UUID orchestrationUuid) {
        byte[] providersRaw = this.getAllProvidersRaw();
        this.fileManager.saveAsJson(providersRaw, "raw", orchestrationUuid.toString(), "PROVIDERS.json");
    }

    public byte[] getAllProvidersRaw() {
        return this.apiUtils.getRaw(URI.create(this.prmEndpoint + PRM_PROVIDERS_API_BASE_PATH));
    }

    public byte[] getAllNodesRaw() {
        return this.apiUtils.getRaw(URI.create(this.prmEndpoint + PRM_NODES_API_BASE_PATH));
    }

    public byte[] getAllLinksRaw() {
        return this.apiUtils.getRaw(URI.create(this.prmEndpoint + PRM_LINKS_API_BASE_PATH));
    }

    public byte[] getAllCpusRaw() {
        return this.apiUtils.getRaw(URI.create(this.prmEndpoint + PRM_RESOURCES_API_BASE_PATH + "/cpus"));
    }

    public byte[] getAllMemoryRaw() {
        return this.apiUtils.getRaw(URI.create(this.prmEndpoint + PRM_RESOURCES_API_BASE_PATH + "/memory"));
    }

    public byte[] getAllStorageRaw() {
        return this.apiUtils.getRaw(URI.create(this.prmEndpoint + PRM_RESOURCES_API_BASE_PATH + "/storage"));
    }

    public void collectStaticOrchestrationData(UUID orchestrationUuid, boolean cleanUp) throws PrmApiException {
        this.logger.info("Collect static orchestration data with orchestrationUuid={} and cleanUp={}", orchestrationUuid, cleanUp);
        this.requestResource(orchestrationUuid, PrmResources.PROVIDERS, cleanUp);
        this.requestResource(orchestrationUuid, PrmResources.NODES, cleanUp);
        this.requestResource(orchestrationUuid, PrmResources.LINKS, cleanUp);
        this.requestResource(orchestrationUuid, PrmResources.CPUS, cleanUp);
        this.requestResource(orchestrationUuid, PrmResources.MEMORY, cleanUp);
        this.requestResource(orchestrationUuid, PrmResources.STORAGE, cleanUp);
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
