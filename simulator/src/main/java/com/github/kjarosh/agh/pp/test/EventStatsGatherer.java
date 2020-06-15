package com.github.kjarosh.agh.pp.test;

import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import com.github.kjarosh.agh.pp.index.events.EventStats;
import com.github.kjarosh.agh.pp.rest.ZoneClient;

import java.util.List;
import java.util.function.Supplier;

/**
 * @author Kamil Jarosz
 */
public class EventStatsGatherer implements Supplier<EventStats> {
    private final ZoneClient client;
    private final List<ZoneId> allZones;

    public EventStatsGatherer(ZoneClient client, List<ZoneId> allZones) {
        this.client = client;
        this.allZones = allZones;
    }

    @Override
    public EventStats get() {
        return gather();
    }

    private EventStats gather() {
        return allZones.stream()
                .map(client::getEventStats)
                .reduce(EventStats.empty(), EventStats::combine);
    }
}
