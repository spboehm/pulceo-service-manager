package dev.pulceo.prm.model.task;

import dev.pulceo.prm.dto.task.CreateNewTaskDTO;
import dev.pulceo.prm.model.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.proxy.HibernateProxy;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Task extends BaseEntity {

    @Builder.Default
    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private TaskMetaData taskMetaData = TaskMetaData.builder().build();
    @Builder.Default
    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private TaskScheduling taskScheduling = TaskScheduling.builder().build();
    @Builder.Default
    private Timestamp created = Timestamp.valueOf(LocalDateTime.now()); // timestamp where task is created on device
    @Builder.Default
    private Timestamp arrived = Timestamp.valueOf(LocalDateTime.now()); // timestamp where task has arrived at the servers
    @Builder.Default
    private byte[] payload = new byte[0]; // payload of the task
    @Builder.Default
    private long sizeOfWorkload = 0; // size of the input data
    @Builder.Default
    private long sizeDuringTransmission = 0; // (compressed) size of workload during transmission
    @Builder.Default
    private int deadline = 100; // tolerable deadline in ms
    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name = "requirement_key")
    @Column(name = "requirement_value")
    @CollectionTable(name = "task_requirements", joinColumns = @JoinColumn(name = "task_requirement_id"))
    private Map<String, String> requirements = new HashMap<>(); // requirements for the task
    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name = "property_key")
    @Column(name = "property_value")
    @CollectionTable(name = "task_properties", joinColumns = @JoinColumn(name = "task_property_id"))
    private Map<String, String> properties = new HashMap<>(); // properties of the task

    public static Task fromCreateNewTaskDTO(@Valid CreateNewTaskDTO createNewTaskDTO) {
        return Task.builder()
                .created(createNewTaskDTO.getCreated())
                .payload(createNewTaskDTO.getPayload())
                .sizeOfWorkload(createNewTaskDTO.getSizeOfWorkload())
                .sizeDuringTransmission(createNewTaskDTO.getSizeDuringTransmission())
                .deadline(createNewTaskDTO.getDeadline())
                .requirements(createNewTaskDTO.getRequirements())
                .properties(createNewTaskDTO.getProperties())
                .build();
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        Task task = (Task) o;
        return getId() != null && Objects.equals(getId(), task.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" +
                "deadline = " + deadline + ", " +
                "sizeDuringTransmission = " + sizeDuringTransmission + ", " +
                "sizeOfWorkload = " + sizeOfWorkload + ", " +
                "arrived = " + arrived + ", " +
                "created = " + created + ", " +
                "taskScheduling = " + taskScheduling + ", " +
                "taskMetaData = " + taskMetaData + ")";
    }
}
