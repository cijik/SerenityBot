package com.ciji.serenity.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class CacheRefreshServiceTest {

    @Mock
    private CharacterSheetDetailsService characterSheetDetailsService;

    @Mock
    private SheetDataProcessorService sheetDataProcessorService;

    @InjectMocks
    private CacheRefreshService cacheRefreshService;

    @Test
    void refreshSheetDataEvictsCachesBeforeProcessing() {
        cacheRefreshService.refreshSheetData(false);

        verify(characterSheetDetailsService).evictSheetDetailsCache();
        verify(characterSheetDetailsService).evictSheetRangesCache();
        verify(sheetDataProcessorService).processSheetData(false);
        verifyNoMoreInteractions(characterSheetDetailsService, sheetDataProcessorService);
    }
}
