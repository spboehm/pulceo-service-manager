package dev.pulceo.prm.dto.application;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class CreateNewApplicationDTO {

    private UUID nodeUUID;
    private String name;
    @Builder.Default
    private List<CreateNewApplicationComponentDTO> applicationComponents = new ArrayList<>();

}
