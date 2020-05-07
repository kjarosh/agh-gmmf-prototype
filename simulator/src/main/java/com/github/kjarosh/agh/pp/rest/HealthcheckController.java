package com.github.kjarosh.agh.pp.rest;

import com.github.kjarosh.agh.pp.graph.GraphLoader;
import com.github.kjarosh.agh.pp.index.Inbox;
import com.github.kjarosh.agh.pp.index.InboxProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Kamil Jarosz
 */
@Controller
public class HealthcheckController {
    @Autowired
    private InboxProcessor inboxProcessor;

    @RequestMapping(method = RequestMethod.GET, path = "healthcheck")
    @ResponseBody
    public void healthcheck() {

    }

    @RequestMapping(method = RequestMethod.GET, path = "index_ready")
    @ResponseBody
    public boolean indexReady() {
        return inboxProcessor.isStalled();
    }
}
