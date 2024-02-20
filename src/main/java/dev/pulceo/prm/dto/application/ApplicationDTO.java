package dev.pulceo.prm.dto.application;

import dev.pulceo.prm.model.application.Application;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Data
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class ApplicationDTO {

    private String applicationUUID;
    private String name;
    @Builder.Default
    private List<ApplicationComponentDTO> applicationComponents = new ArrayList<>();

    public static ApplicationDTO fromApplication(Application application) {
        return ApplicationDTO.builder()
                .applicationUUID(String.valueOf(application.getUuid()))
                .name(application.getName())
                .applicationComponents(application.getApplicationComponents().stream().map(ApplicationComponentDTO::fromApplicationComponent).toList())
                .build();
    }

}
