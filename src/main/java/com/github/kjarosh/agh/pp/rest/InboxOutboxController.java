package com.github.kjarosh.agh.pp.rest;

import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.index.Inbox;
import com.github.kjarosh.agh.pp.index.InboxProcessor;
import com.github.kjarosh.agh.pp.index.events.Event;
import com.github.kjarosh.agh.pp.index.events.EventStats;
import com.github.kjarosh.agh.pp.rest.dto.BulkMessagesDto;
import com.github.kjarosh.agh.pp.rest.dto.MessageDto;
import com.github.kjarosh.agh.pp.rest.error.TooManyEventsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import static com.github.kjarosh.agh.pp.config.Config.ZONE_ID;

/**
 * @author Kamil Jarosz
 */
@Controller
public class InboxOutboxController {
    @Autowired
    private Inbox inbox;

    @Autowired
    private InboxProcessor inboxProcessor;

    @RequestMapping(method = RequestMethod.POST, path = "events")
    @ResponseBody
    public void postEvent(
            @RequestParam("id") String idString,
            @RequestBody Event event) {
        failIfInboxSizeTooBig();

        VertexId id = new VertexId(idString);
        if (!ZONE_ID.equals(id.owner())) {
            throw new IllegalArgumentException();
        }
        inbox.post(id, event);
    }

    @RequestMapping(method = RequestMethod.POST, path = "events/bulk")
    @ResponseBody
    public void postEvents(@RequestBody BulkMessagesDto messages) {
        failIfInboxSizeTooBig();

        for (MessageDto message : messages.getMessages()) {
            VertexId id = new VertexId(ZONE_ID, message.getVertexName());
            inbox.post(id, message.getEvent());
        }
    }

    private void failIfInboxSizeTooBig() {
        if (inbox.size() > 1_000_000) {
            throw new TooManyEventsException();
        }
    }

    @RequestMapping(method = RequestMethod.GET, path = "events/stats")
    @ResponseBody
    public EventStats eventStats() {
        return inboxProcessor.stats();
    }
}
