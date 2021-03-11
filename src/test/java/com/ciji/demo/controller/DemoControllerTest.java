package com.ciji.demo.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.ciji.demo.model.BotParam;
import com.ciji.demo.service.CommandProcessingService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DemoControllerTest {
    
    @Mock
    private CommandProcessingService commandProcessingService;

    @InjectMocks
    private DemoController demoController;

    @Test
    public void testGetEntryData() {
        BotParam botParam = new BotParam();
        botParam.setName("prefix");
        botParam.setValue("!");
        when(commandProcessingService.getParameter("prefix")).thenReturn(botParam);

        assertEquals("!", demoController.getEntryData("prefix").getName());
    }
}
