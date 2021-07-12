package com.github.kjarosh.agh.pp.instrumentation.jpa;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * @author Kamil Jarosz
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(indexes = {
        @Index(columnList = "zone,eventId,trace,vertex"),
        @Index(columnList = "trace"),
})
public class DbNotification {
    @Id
    private String id = UUID.randomUUID().toString();

    private String zone;
    private Instant time;
    private String thread;
    private String type;
    private String trace;
    private String eventId;
    private String eventType;
    private String vertex;
    private String sender;
    private String originalSender;
    private int forkChildren;
}
