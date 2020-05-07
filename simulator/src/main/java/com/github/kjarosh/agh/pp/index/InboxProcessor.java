package com.github.kjarosh.agh.pp.index;

import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.index.events.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Kamil Jarosz
 */
@Service
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
public class InboxProcessor {
    private static final Logger logger = LoggerFactory.getLogger(InboxProcessor.class);

    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final Set<VertexId> processing = new HashSet<>();

    @Autowired
    private Inbox inbox;

    @Autowired
    private EventProcessor eventProcessor;

    @PostConstruct
    public void init() {
        inbox.addListener(this::inboxChanged);
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
                logger.info("Processing event " + event + " at " + id);

                eventProcessor.process(id, event);

                synchronized (processing) {
                    processing.remove(id);
                }
                inboxChanged(id);
            });
        }
    }

    public boolean isStalled() {
        synchronized (processing) {
            return inbox.isEmpty() && processing.isEmpty();
        }
    }
}
