package dev.pulceo.prm.model.orchestration;

import com.fasterxml.jackson.databind.JsonNode;
import dev.pulceo.prm.model.BaseEntity;

import java.util.List;


public class Orchestration extends BaseEntity {

    private String name;
    private String description;
    private List<JsonNode> nodes;
    private List<JsonNode> links;
    private List<JsonNode> applications;
    private List<JsonNode> metricRequests;
    private List<JsonNode> metrics;
    private List<JsonNode> events;

}
