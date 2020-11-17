package com.github.kjarosh.agh.pp.rest;

import com.github.kjarosh.agh.pp.graph.GraphLoader;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import com.github.kjarosh.agh.pp.index.InboxProcessor;
import com.github.kjarosh.agh.pp.rest.client.ZoneClient;
import com.github.kjarosh.agh.pp.rest.dto.DependentZonesDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Kamil Jarosz
 */
@Controller
public class MetainfoController {
    @Autowired
    private InboxProcessor inboxProcessor;

    @Autowired
    private GraphLoader graphLoader;

    @RequestMapping(method = RequestMethod.GET, path = "healthcheck")
    @ResponseBody
    public void healthcheck() {

    }

    @RequestMapping(method = RequestMethod.GET, path = "index_ready")
    @ResponseBody
    public boolean indexReady() {
        return inboxProcessor.isStalled();
    }

    @RequestMapping(method = RequestMethod.POST, path = "dependent_zones")
    @ResponseBody
    public DependentZonesDto dependentZones(@RequestBody List<ZoneId> exclude) {
        Set<ZoneId> directlyDependent = new HashSet<>(graphLoader.getGraph().allZones());
        directlyDependent.removeAll(exclude);

        List<ZoneId> newExclude = new ArrayList<>(exclude);
        newExclude.addAll(directlyDependent);

        Set<ZoneId> dependent = new HashSet<>(directlyDependent);
        directlyDependent.stream()
                .map(zone -> new ZoneClient().getDependentZones(zone, newExclude))
                .map(DependentZonesDto::getZones)
                .flatMap(Collection::stream)
                .forEach(dependent::add);

        return DependentZonesDto.builder()
                .zones(dependent)
                .build();
    }
}
