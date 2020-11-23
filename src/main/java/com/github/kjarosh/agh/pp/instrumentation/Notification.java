package com.github.kjarosh.agh.pp.instrumentation;

import com.github.kjarosh.agh.pp.config.Config;
import com.github.kjarosh.agh.pp.index.events.Event;
import com.github.kjarosh.agh.pp.index.events.EventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Kamil Jarosz
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Notification {
    public static final Map<String, NotificationFieldSerializer> serializers = new LinkedHashMap<>();

    static {
        serializers.put("zone", Notification::getZone);
        serializers.put("time", n -> n.getTime().toString());
        serializers.put("thread", Notification::getThread);
        serializers.put("value", notification -> notification.getType().getValue());
        serializers.put("trace", Notification::getTrace);
        serializers.put("eventType", notification -> notification.getEventType().toString());
        serializers.put("sender", Notification::getSender);
        serializers.put("originalSender", Notification::getOriginalSender);
        serializers.put("forkChildren", notification -> String.valueOf(notification.getForkChildren()));
    }

    @Builder.Default
    private String zone = Config.ZONE_ID.toString();
    @Builder.Default
    private Instant time = Instant.now();
    @Builder.Default
    private String thread = Thread.currentThread().getName();
    private Type type;
    private String trace;
    private EventType eventType;
    private String sender;
    private String originalSender;
    private int forkChildren;

    private static NotificationBuilder fromEvent(Event event) {
        return Notification.builder()
                .trace(event.getTrace())
                .eventType(event.getType())
                .sender(event.getSender().toString())
                .originalSender(event.getOriginalSender().toString());
    }

    public static Notification startProcessing(Event event) {
        return fromEvent(event)
                .type(Type.START_EVENT_PROCESSING)
                .build();
    }

    public static Notification endProcessing(Event event) {
        return fromEvent(event)
                .type(Type.END_EVENT_PROCESSING)
                .build();
    }

    public static Notification failProcessing(Event event) {
        return fromEvent(event)
                .type(Type.FAIL_EVENT_PROCESSING)
                .build();
    }

    public static Notification forkEvent(Event event, int children) {
        return fromEvent(event)
                .type(Type.FORK_EVENT)
                .forkChildren(children)
                .build();
    }

    public enum Type {
        START_EVENT_PROCESSING("start"),
        END_EVENT_PROCESSING("end"),
        FAIL_EVENT_PROCESSING("fail"),
        FORK_EVENT("fork"),
        ;

        @Getter
        private final String value;

        Type(String value) {
            this.value = value;
        }
    }
}
