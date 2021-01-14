package com.github.kjarosh.agh.pp.index;

import com.github.kjarosh.agh.pp.config.Config;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import com.github.kjarosh.agh.pp.index.events.Event;
import com.github.kjarosh.agh.pp.rest.client.ZoneClient;
import com.github.kjarosh.agh.pp.rest.dto.BulkMessagesDto;
import com.github.kjarosh.agh.pp.rest.dto.MessageDto;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Kamil Jarosz
 */
public class Outbox {
    private static final int INITIAL_DELAY = 10;
    private static final int MAX_DELAY = 1000;
    private static final double BACKOFF_COEFF = 1.2;
    private static final int BULK_SIZE = 100_000;

    private static final ThreadFactory treadFactory = new ThreadFactoryBuilder()
            .setNameFormat(Config.ZONE_ID + "-outbox-%d")
            .build();
    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2, treadFactory);
    private static final ConcurrentMap<ZoneId, Outbox> outboxes = new ConcurrentHashMap<>();

    private final ZoneId zone;
    private final ConcurrentLinkedDeque<Message> queue = new ConcurrentLinkedDeque<>();
    private long lastDelay;

    private Outbox(ZoneId zone) {
        this.zone = zone;
        this.lastDelay = INITIAL_DELAY;
        executor.schedule(this::flush, this.lastDelay, TimeUnit.MILLISECONDS);
    }

    public static Outbox forZone(ZoneId zone) {
        return outboxes.computeIfAbsent(zone, Outbox::new);
    }

    public static boolean allEmpty() {
        return outboxes.values()
                .stream()
                .allMatch(Outbox::isEmpty);
    }

    public static int allCount() {
        return outboxes.values()
                .stream()
                .mapToInt(Outbox::count)
                .sum();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public int count() {
        return queue.size();
    }

    public void postEvent(VertexId id, Event event) {
        if (!zone.equals(id.owner())) {
            throw new IllegalArgumentException();
        }

        queue.addLast(new Message(id, event));
    }

    private void flush() {
        int sent = 0;
        try {
            List<Message> toSend = new ArrayList<>();
            while (toSend.size() < BULK_SIZE && !queue.isEmpty()) {
                toSend.add(queue.removeFirst());
            }

            if (toSend.isEmpty()) {
                return;
            }

            boolean success = false;
            try {
                new ZoneClient().postEvents(zone, BulkMessagesDto.builder()
                        .messages(toSend.stream()
                                .map(m -> MessageDto.builder()
                                        .vertexName(m.getId().name())
                                        .event(m.getEvent())
                                        .build())
                                .collect(Collectors.toList()))
                        .build());
                success = true;
                sent = toSend.size();
            } finally {
                if (!success) {
                    for (int i = toSend.size() - 1; i >= 0; --i) {
                        queue.addFirst(toSend.get(i));
                    }
                }
            }
        } finally {
            if (sent != 0) {
                this.lastDelay = INITIAL_DELAY;
            } else {
                this.lastDelay *= BACKOFF_COEFF;
                if (this.lastDelay > MAX_DELAY) {
                    this.lastDelay = MAX_DELAY;
                }
            }
            executor.schedule(this::flush, this.lastDelay, TimeUnit.MILLISECONDS);
        }
    }
}
