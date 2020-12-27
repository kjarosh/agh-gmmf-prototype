package com.github.kjarosh.agh.pp.rest.dto;

import com.github.kjarosh.agh.pp.graph.model.Vertex;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Kamil Jarosz
 */
class EdgeCreationRequestDtoTest {
    @Test
    void testToString() {
        EdgeCreationRequestDto dto = EdgeCreationRequestDto.builder()
                .fromName("f")
                .toName("t")
                .permissions("p")
                .trace("tr")
                .build();

        assertThat(dto.toString()).isEqualTo("f/t/p/tr");
    }

    @Test
    void fromString() {
        EdgeCreationRequestDto dto = EdgeCreationRequestDto.fromString("f/t/p/tr");

        assertThat(dto.getFromName()).isEqualTo("f");
        assertThat(dto.getToName()).isEqualTo("t");
        assertThat(dto.getPermissions()).isEqualTo("p");
        assertThat(dto.getTrace()).isEqualTo("tr");
    }

    @Test
    void fromString2() {
        EdgeCreationRequestDto dto = EdgeCreationRequestDto.fromString("f/t/p/");

        assertThat(dto.getFromName()).isEqualTo("f");
        assertThat(dto.getToName()).isEqualTo("t");
        assertThat(dto.getPermissions()).isEqualTo("p");
        assertThat(dto.getTrace()).isNull();
    }

    @Test
    void serializeDeserialize() throws JsonProcessingException {
        EdgeCreationRequestDto dto = EdgeCreationRequestDto.builder()
                .fromName("f")
                .toName("t")
                .permissions("p")
                .trace("tr")
                .build();

        ObjectMapper mapper = new ObjectMapper();
        String s = mapper.writeValueAsString(dto);

        assertThat(s).isEqualTo("\"f/t/p/tr\"");

        EdgeCreationRequestDto dto2 = mapper.readValue(s, EdgeCreationRequestDto.class);

        assertThat(dto2).isEqualTo(dto);
        assertThat(dto.getFromName()).isEqualTo("f");
        assertThat(dto.getToName()).isEqualTo("t");
        assertThat(dto.getPermissions()).isEqualTo("p");
        assertThat(dto.getTrace()).isEqualTo("tr");
    }
}
