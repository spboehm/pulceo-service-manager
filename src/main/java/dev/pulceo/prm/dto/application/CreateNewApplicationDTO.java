package dev.pulceo.prm.dto.application;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Data
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class CreateNewApplicationDTO {

    private String name;
    @Builder.Default
    private List<CreateNewApplicationComponentDTO> applicationComponents = new ArrayList<>();

}
