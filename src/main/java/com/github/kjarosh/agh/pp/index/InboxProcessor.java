package com.github.kjarosh.agh.pp.index;

import com.codahale.metrics.Meter;
import com.codahale.metrics.SlidingTimeWindowMovingAverages;
import com.github.kjarosh.agh.pp.config.Config;
import com.github.kjarosh.agh.pp.graph.GraphLoader;
import com.github.kjarosh.agh.pp.graph.model.Vertex;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.index.events.Event;
import com.github.kjarosh.agh.pp.index.events.EventStats;
import com.github.kjarosh.agh.pp.util.ClockX60;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Combines {@link Inbox} and {@link EventProcessor} together,
 * receives events from {@link Inbox} and passes them along
 * to the {@link EventProcessor}. It also ensures the correct order
 * of processing events, i.e. events meant for one vertex are
 * to be executed sequentially and in the correct order.
 * <p>
 * It also provides basic statistics about the events.
 *
 * @author Kamil Jarosz
 */
@Slf4j
@Service
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
public class InboxProcessor {
    private final ThreadFactory treadFactory = new ThreadFactoryBuilder()
            .setNameFormat("worker-" + Config.ZONE_ID + "-%d")
            .build();
    private final ExecutorService executor =
            Executors.newFixedThreadPool(5, treadFactory);
    private final Set<VertexId> processing = new HashSet<>();

    private final Meter eventsMeter = new Meter(new SlidingTimeWindowMovingAverages(new ClockX60()));

    @Autowired
    private Inbox inbox;

    @Autowired
    private GraphLoader graphLoader;

    @Autowired
    private EventProcessor eventProcessor;

    @PostConstruct
    public void init() {
        inbox.addInboxChangeListener(this::inboxChanged);
    }

    private void inboxChanged(VertexId id) {
        synchronized (processing) {
            if (processing.contains(id)) {
                // cannot process one vertex concurrently
                return;
            }

            Event event = inbox.receive(id).orElse(null);
            if (event == null) {
                // no event to process
                return;
            }

            processing.add(id);
            executor.submit(() -> {
                log.trace("Processing event " + event + " at " + id);

                try {
                    eventProcessor.process(id, event);
                    eventsMeter.mark();
                } catch (Exception e) {
                    log.error("An exception occurred while processing an event", e);
                } finally {
                    synchronized (processing) {
                        processing.remove(id);
                    }
                    inboxChanged(id);
                }
            });
        }
    }

    public boolean isStalled() {
        synchronized (processing) {
            return processing.isEmpty() && inbox.isEmpty() && Outbox.allEmpty();
        }
    }

    public EventStats stats() {
        List<VertexId> currentProcessing;
        synchronized (processing) {
            currentProcessing = new ArrayList<>(processing);
        }

        Map<Vertex.Type, Integer> processingByType = new HashMap<>();
        currentProcessing.forEach(id -> {
            Vertex vertex = graphLoader.getGraph().getVertex(id);
            processingByType.computeIfAbsent(vertex.type(), i -> 0);
            processingByType.computeIfPresent(vertex.type(), (k, v) -> v + 1);
        });

        return EventStats.builder()
                .processing(currentProcessing.size())
                .processingByType(processingByType)
                .queued(inbox.queuedCount())
                .outbox(Outbox.allCount())
                .total(eventsMeter.getCount())
                .load1(eventsMeter.getOneMinuteRate())
                .load5(eventsMeter.getFiveMinuteRate() / 5)
                .load15(eventsMeter.getFifteenMinuteRate() / 15)
                .build();
    }
}
