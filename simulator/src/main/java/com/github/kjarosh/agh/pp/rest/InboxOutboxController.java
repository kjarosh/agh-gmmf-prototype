package com.github.kjarosh.agh.pp.rest;

import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.index.Inbox;
import com.github.kjarosh.agh.pp.index.events.Event;
import com.github.kjarosh.agh.pp.index.events.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Kamil Jarosz
 */
@Controller
public class InboxOutboxController {
    private static final Logger logger = LoggerFactory.getLogger(InboxOutboxController.class);

    @Autowired
    private Inbox inbox;

    @RequestMapping(method = RequestMethod.POST, path = "events")
    @ResponseBody
    public void postEvent(
            @RequestParam("id") String idString,
            @RequestBody Event event) {
        VertexId id = new VertexId(idString);
        inbox.post(id, event);
    }
}