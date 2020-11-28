package com.github.kjarosh.agh.pp.test;

import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import com.github.kjarosh.agh.pp.index.events.EventStats;
import com.github.kjarosh.agh.pp.rest.client.ZoneClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.function.Supplier;

/**
 * @author Kamil Jarosz
 */
public class EventStatsGatherer implements Supplier<EventStats> {
    private static final Logger logger = LoggerFactory.getLogger(EventStatsGatherer.class);

    private final ZoneClient client = new ZoneClient();
    private final Collection<? extends ZoneId> allZones;

    public EventStatsGatherer(ZoneId zone) {
        this.allZones = client.getDependentZones(zone).getZones();
        logger.debug("Queried all zones: {}", allZones);
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
                .map(client::getEventStats)
                .reduce(EventStats.empty(), EventStats::combine);
    }
}
