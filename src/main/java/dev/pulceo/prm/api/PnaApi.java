package dev.pulceo.prm.api;

import dev.pulceo.prm.api.dto.task.CreateNewTaskOnPnaDTO;
import dev.pulceo.prm.api.dto.task.CreateNewTaskOnPnaResponseDTO;
import dev.pulceo.prm.api.exception.PnaApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class PnaApi {
    private final Logger logger = LoggerFactory.getLogger(PnaApi.class);
    @Value("${prm.endpoint}")
    private String prmEndpoint;
    // TODO: remove this if testing completes
    @Value("${pna1.test.init.token}")
    private String pna1TestInitToken;
    @Value("${pna1.test.uuid}")
    private String pna1TestUUID;
    private final WebClient webClient;
    private final static String PNA_TASKS_API_BASE_PATH = "/api/v1/tasks";
    @Value("${webclient.scheme}")
    private String webClientScheme;

    private final PrmApi prmApi;

    @Autowired
    public PnaApi(WebClient webClient, PrmApi prmApi) {
        this.webClient = webClient;
        this.prmApi = prmApi;
    }

    public void createNewTaskOnPna(String id, CreateNewTaskOnPnaDTO createNewTaskOnPnaDTO) throws PnaApiException {
        logger.info("Creating new task on PNA");

        // TODO: resolve node id

        // TODO: String nodeId

        // TODO: resolve node id
        String pnaNodeAgentUrl = "127.0.0.1:7676";

        try {

            CreateNewTaskOnPnaResponseDTO createNewTaskOnPnaResponseDTO = webClient
                    .post()
                    .uri(this.webClientScheme + "://" + pnaNodeAgentUrl + PNA_TASKS_API_BASE_PATH)
                    .header("Authorization", "Basic " + prmApi.getPnaTokenByNodeId(id))
                    .bodyValue(createNewTaskOnPnaDTO)
                    .retrieve()
                    .bodyToMono(CreateNewTaskOnPnaResponseDTO.class)
                    .onErrorResume(e -> {
                        throw new RuntimeException(new PnaApiException("Failed to to issue a cloud registration", e));
                    })
                    .block();

            System.out.println(createNewTaskOnPnaResponseDTO.toString());

        } catch (Exception e) {
            throw new PnaApiException("Failed to get PNA URL from PRM", e);
        }
    }
}
