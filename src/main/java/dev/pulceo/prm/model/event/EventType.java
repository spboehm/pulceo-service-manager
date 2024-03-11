package dev.pulceo.prm.model.event;

public enum EventType {
    NODE_CREATED,
    NODE_UPDATED,
    NODE_CPU_RESOURCES_UPDATED,
    NODE_MEMORY_RESOURCES_UPDATED,
    NODE_STORAGE_RESOURCES_UPDATED,
    NODE_NETWORK_RESOURCES_UPDATED,
    LINK_CREATED,
    LINK_DELETED,
    LINK_METRIC_REQUEST_CREATED,
    METRIC_REQUEST_DELETED,
    NODE_METRIC_REQUEST_CREATED,
    APPLICATION_METRIC_REQUEST_CREATED,
    APPLICATION_CREATED,
    APPLICATION_COMPONENT_CREATED,
    APPLICATION_DELETED,
    SHUTDOWN;
}
