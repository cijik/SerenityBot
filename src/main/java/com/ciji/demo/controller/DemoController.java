package com.ciji.demo.controller;

import com.ciji.demo.model.BotParam;
import com.ciji.demo.service.CommandProcessingService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;


@Controller
public class DemoController {

    @Autowired
    private CommandProcessingService commandProcessingService;
    
    @GetMapping(value="/demo")
    @ResponseBody
    public BotParam getEntryData(@RequestParam String id) {
        return commandProcessingService.getParameter(id);
    }
}
