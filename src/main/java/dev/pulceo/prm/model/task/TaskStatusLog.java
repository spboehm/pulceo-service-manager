package dev.pulceo.prm.model.task;


import com.fasterxml.jackson.annotation.JsonBackReference;
import dev.pulceo.prm.model.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.proxy.HibernateProxy;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@ToString
public class TaskStatusLog extends BaseEntity {

    @Builder.Default
    private Timestamp timestamp = Timestamp.valueOf(LocalDateTime.now());
    @Builder.Default
    private TaskStatus previousStatus = TaskStatus.NONE;
    @Builder.Default
    private TaskStatus newStatus = TaskStatus.NEW;
    @Builder.Default
    @NotNull
    private Timestamp modifiedOn = Timestamp.valueOf(LocalDateTime.now());
    @Builder.Default
    private String modifiedBy = "psm";
    @Builder.Default
    private String modifiedById = "";
    @Builder.Default
    private String previousStateOfTask = "";
    @Builder.Default
    private String newStateOfTask = "";
    @Builder.Default
    private String comment = "";
    @ManyToOne(targetEntity = Task.class)
    @JoinColumn
    @JsonBackReference
    private Task task; // task
    @ManyToOne(targetEntity = TaskScheduling.class)
    @JoinColumn
    @JsonBackReference
    private TaskScheduling taskScheduling;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        TaskStatusLog that = (TaskStatusLog) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
