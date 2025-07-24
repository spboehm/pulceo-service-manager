package dev.pulceo.prm.dto.orchestration;

import dev.pulceo.prm.model.orchestration.Orchestration;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Data
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class ShortOrchestrationContextDTO {

    private String service;
    private String uuid;
    private String name;

    public static ShortOrchestrationContextDTO fromOrchestration(String service, Orchestration orchestration) {
        return ShortOrchestrationContextDTO.builder()
                .service(service)
                .uuid(orchestration.getUuid().toString())
                .name(orchestration.getName())
                .build();
    }

    public static ShortOrchestrationContextDTO fromOrchestrationContextDTO(String service, OrchestrationContextDTO orchestrationContextDTO) {
        return ShortOrchestrationContextDTO.builder()
                .service(service)
                .uuid(orchestrationContextDTO.getUuid())
                .name(orchestrationContextDTO.getName())
                .build();
    }

}
