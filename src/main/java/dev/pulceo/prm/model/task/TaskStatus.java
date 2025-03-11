package dev.pulceo.prm.model.task;

public enum TaskStatus {
    NEW, // ARRIVED OR WAITING
    SCHEDULED, // ASSIGNED TO A NODE
    OFFLOADED,
    RUNNING,
    FAILED,
    COMPLETED
}
