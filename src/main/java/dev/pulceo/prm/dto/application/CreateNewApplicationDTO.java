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

    @Builder.Default
    private UUID nodeUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    @Builder.Default
    private String name = "";
    @Builder.Default
    private List<CreateNewApplicationComponentDTO> applicationComponents = new ArrayList<>();

}
