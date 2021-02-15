package com.github.kjarosh.agh.pp.index;

import com.github.kjarosh.agh.pp.config.AppConfig;
import com.github.kjarosh.agh.pp.config.Config;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.index.events.Event;
import com.github.kjarosh.agh.pp.instrumentation.Instrumentation;
import com.github.kjarosh.agh.pp.instrumentation.Notification;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Inbox for {@link Event}s. Contains inboxes for
 * each vertex and allows to post new events and
 * listen for new events.
 *
 * @author Kamil Jarosz
 */
@Slf4j
@Service
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
public class Inbox {
    private final AtomicInteger inboxSize = new AtomicInteger(0);
    private final Map<VertexId, Deque<Event>> inboxes = new ConcurrentHashMap<>();
    private final List<Consumer<VertexId>> listeners = new CopyOnWriteArrayList<>();

    private final Instrumentation instrumentation = Instrumentation.getInstance();
    private boolean indexationEnabled = AppConfig.indexationEnabled;

    public int size() {
        return inboxSize.get();
    }

    @SneakyThrows
    public void post(VertexId id, Event event) {
        if (!id.owner().equals(Config.ZONE_ID)) {
            Outbox.forZone(id.owner()).postEvent(id, event);
            return;
        }

        log.trace("Event posted at " + id + ": " + event);
        instrumentation.notify(Notification.queued(id, event));
        if (!indexationEnabled) {
            instrumentation.notify(Notification.startProcessing(id, event));
            instrumentation.notify(Notification.endProcessing(id, event));
            return;
        }

        inboxes.computeIfAbsent(id, i -> new ConcurrentLinkedDeque<>()).addLast(event);
        inboxSize.incrementAndGet();
        listeners.forEach(l -> l.accept(id));
    }

    @SneakyThrows
    public Optional<Event> receive(VertexId id) {
        Deque<Event> queue = inboxes.get(id);
        if (queue == null) {
            return Optional.empty();
        }

        Event event = queue.pollFirst();
        if (event != null) {
            inboxSize.decrementAndGet();
        }
        return Optional.ofNullable(event);
    }

    public void setIndexationEnabled(boolean enable) {
        indexationEnabled = enable;
    }

    public void addInboxChangeListener(Consumer<VertexId> listener) {
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
