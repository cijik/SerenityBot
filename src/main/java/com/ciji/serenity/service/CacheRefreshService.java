package com.ciji.serenity.service;

import com.ciji.serenity.model.CharacterSheet;
import com.ciji.serenity.model.CharacterSheetDetails;
import com.ciji.serenity.model.mapper.SheetMatrixMapper;
import com.ciji.serenity.repository.CharacterSheetDetailsRepository;
import com.ciji.serenity.repository.CharacterSheetRepository;
import com.google.api.services.sheets.v4.model.BatchGetValuesResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheRefreshService {

    private final CharacterSheetDetailsService characterSheetDetailsService;

    private final SheetDataProcessorService sheetDataProcessorService;

    @Scheduled(fixedRate = 12, timeUnit = TimeUnit.HOURS)
    public void refreshSheetData() {
        refreshSheetData(true);
    }

    public void refreshSheetData(boolean isScheduled) {
        log.info("Refreshing character sheet data of all users");
        // Evict cache in a separate transaction
        characterSheetDetailsService.evictSheetRangesCache();

        // Optional: Add a small delay to ensure cache eviction completes
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Process data in a new transaction
        sheetDataProcessorService.processSheetData(isScheduled);
    }
}
