package com.github.kjarosh.agh.pp.instrumentation;

import com.github.kjarosh.agh.pp.index.events.Event;
import com.github.kjarosh.agh.pp.index.events.EventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * @author Kamil Jarosz
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Notification {
    private Instant time = Instant.now();
    private String thread = Thread.currentThread().getName();
    private Type type;
    private String trace;
    private EventType eventType;
    private String sender;
    private String originalSender;

    public Notification(Type type, Event event) {
        this.type = type;
        this.trace = event.getTrace();
        this.eventType = event.getType();
        this.sender = event.getSender().toString();
        this.originalSender = event.getOriginalSender().toString();
    }

    public enum Type {
        START_EVENT_PROCESSING("start"),
        END_EVENT_PROCESSING("end"),
        FAIL_EVENT_PROCESSING("fail"),
        ;

        @Getter
        private final String value;

        Type(String value) {
            this.value = value;
        }
    }
}
