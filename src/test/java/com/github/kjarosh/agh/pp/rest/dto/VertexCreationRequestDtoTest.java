package com.github.kjarosh.agh.pp.rest.dto;

import com.github.kjarosh.agh.pp.graph.model.Vertex;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Kamil Jarosz
 */
class VertexCreationRequestDtoTest {
    @Test
    void testToString() {
        VertexCreationRequestDto dto = VertexCreationRequestDto.builder()
                .name("test")
                .type(Vertex.Type.SPACE)
                .build();

        assertThat(dto.toString()).isEqualTo("SPACE/test");
    }

    @Test
    void fromString() {
        VertexCreationRequestDto dto = VertexCreationRequestDto.fromString("SPACE/test");

        assertThat(dto.getName()).isEqualTo("test");
        assertThat(dto.getType()).isEqualTo(Vertex.Type.SPACE);
    }

    @Test
    void serializeDeserialize() throws JsonProcessingException {
        VertexCreationRequestDto dto = VertexCreationRequestDto.builder()
                .name("test")
                .type(Vertex.Type.SPACE)
                .build();

        ObjectMapper mapper = new ObjectMapper();
        String s = mapper.writeValueAsString(dto);

        assertThat(s).isEqualTo("\"SPACE/test\"");

        VertexCreationRequestDto dto2 = mapper.readValue(s, VertexCreationRequestDto.class);

        assertThat(dto2).isEqualTo(dto);
        assertThat(dto2.getName()).isEqualTo("test");
        assertThat(dto2.getType()).isEqualTo(Vertex.Type.SPACE);
    }
}
