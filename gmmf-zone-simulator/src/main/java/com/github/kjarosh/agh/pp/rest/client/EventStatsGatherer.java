package com.github.kjarosh.agh.pp.rest.client;

import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import com.github.kjarosh.agh.pp.index.events.EventStats;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * @author Kamil Jarosz
 */
@Slf4j
public class EventStatsGatherer implements Supplier<EventStats> {
    private final ZoneClient client = new ZoneClient();
    private final Collection<? extends ZoneId> allZones;

    public EventStatsGatherer(ZoneId zone) {
        this.allZones = client.getDependentZones(zone).getZones();
        log.debug("Queried all zones: {}", allZones);
    }

    public EventStatsGatherer(Collection<? extends ZoneId> allZones) {
        this.allZones = allZones;
    }

    @Override
    public EventStats get() {
        return gather();
    }

    private EventStats gather() {
        return allZones.stream()
                .flatMap(zone -> {
                    try {
                        return Stream.of(client.getEventStats(zone));
                    } catch (Exception e) {
                        log.warn("Zone " + zone + " failed to return stats", e);
                        return Stream.empty();
                    }
                })
                .reduce(EventStats.empty(), EventStats::combine);
    }
}
