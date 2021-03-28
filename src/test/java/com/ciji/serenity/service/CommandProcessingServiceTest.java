package com.ciji.serenity.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.ciji.serenity.dao.BotParameterDao;
import com.ciji.serenity.model.BotParam;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CommandProcessingServiceTest {
    
    @Mock
    private BotParameterDao botParameterDao;

    @InjectMocks
    private CommandProcessingService commandProcessingService;

    @Test
    public void testGetData() {
        BotParam botParam = new BotParam();
        botParam.setName("prefix");
        botParam.setValue("!");
        when(botParameterDao.findById("prefix")).thenReturn(Optional.of(botParam));

        assertEquals("prefix", commandProcessingService.getParameter("!").getName());
    }

    @Test
    public void testThrowsExceptionWhenEmpty() {
        when(botParameterDao.findById("prefix")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            commandProcessingService.getParameter("prefix");
        });
    }
}
