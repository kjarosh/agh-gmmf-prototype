package com.github.kjarosh.agh.pp.rest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Kamil Jarosz
 */
@Controller
public class HealthcheckController {
    @RequestMapping(method = RequestMethod.GET, path = "healthcheck")
    @ResponseBody
    public void healthcheck() {

    }
}
