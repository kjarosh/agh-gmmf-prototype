package com.github.kjarosh.agh.pp.index;

import com.github.kjarosh.agh.pp.Config;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.index.events.Event;
import com.github.kjarosh.agh.pp.rest.ZoneClient;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Inbox for {@link Event}s. Contains inboxes for
 * each vertex and allows to post new events and
 * listen for new events.
 *
 * @author Kamil Jarosz
 */
@Service
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
public class Inbox {
    private static final Logger logger = LoggerFactory.getLogger(Inbox.class);

    private final Map<VertexId, Deque<Event>> inboxes = new ConcurrentHashMap<>();
    private final List<Consumer<VertexId>> listeners = new CopyOnWriteArrayList<>();

    @SneakyThrows
    public void post(VertexId id, Event event) {
        if (!id.owner().equals(Config.ZONE_ID)) {
            new ZoneClient().postEvent(id, event);
            return;
        }

        logger.info("Event posted at " + id + ": " + event);
        inboxes.computeIfAbsent(id, i -> new ConcurrentLinkedDeque<>()).addLast(event);
        listeners.forEach(l -> l.accept(id));
    }

    @SneakyThrows
    public Optional<Event> receive(VertexId id) {
        Deque<Event> queue = inboxes.get(id);
        if (queue == null) {
            return Optional.empty();
        }

        Event event = queue.pollFirst();
        return Optional.ofNullable(event);
    }

    public void addListener(Consumer<VertexId> listener) {
        listeners.add(listener);
    }

    public boolean isEmpty() {
        return inboxes.values()
                .stream()
                .allMatch(Collection::isEmpty);
    }

    public int queuedCount() {
        return Math.toIntExact(inboxes.values()
                .stream()
                .mapToLong(Collection::size)
                .sum());
    }
}
