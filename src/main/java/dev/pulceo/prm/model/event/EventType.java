package dev.pulceo.prm.model.event;

import dev.pulceo.prm.model.task.TaskStatus;

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
    SHUTDOWN,
    TASK_CREATED,
    TASK_SCHEDULED,
    TASK_OFFLOADED,
    TASK_RUNNING,
    TASK_FAILED,
    TASK_COMPLETED;

    public static EventType fromTaskStatus(TaskStatus taskStatus) {
        return switch (taskStatus) {
            case NEW -> TASK_CREATED;
            case SCHEDULED -> TASK_SCHEDULED;
            case OFFLOADED -> TASK_OFFLOADED;
            case RUNNING -> TASK_RUNNING;
            case COMPLETED -> TASK_COMPLETED;
            case FAILED -> TASK_FAILED;
            default -> throw new IllegalArgumentException("Unsupported TaskStatus: " + taskStatus);
        };
    }
}
