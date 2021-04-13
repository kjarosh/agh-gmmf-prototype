package com.github.kjarosh.agh.pp.graph.util;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.IOException;

@JsonRootName("type")
@JsonDeserialize(using = QueryType.Deserializer.class)
public enum QueryType {
    MEMBER,
    REACHES,
    EFFECTIVE_PERMISSIONS;

    @JsonValue
    @Override
    public String toString() {
        return this.name();
    }

    public static class Deserializer extends JsonDeserializer<QueryType> {
        @Override
        public QueryType deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            if(jsonParser.getCurrentToken().equals(JsonToken.VALUE_STRING)) {
                var name = jsonParser.getText().trim();
                return QueryType.valueOf(name);
            }

            return (QueryType) deserializationContext.handleUnexpectedToken(QueryType.class, jsonParser);
        }
    }
}
