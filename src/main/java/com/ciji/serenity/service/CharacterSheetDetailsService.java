package com.ciji.serenity.service;

import com.ciji.serenity.model.CharacterSheet;
import com.ciji.serenity.model.CharacterSheetDetails;
import com.ciji.serenity.repository.CharacterSheetDetailsRepository;
import com.google.api.services.sheets.v4.model.BatchGetValuesResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CharacterSheetDetailsService {

    private final CharacterSheetDetailsRepository characterSheetDetailsRepository;

    @Cacheable(cacheNames = "sheets", key = "#sheet.name")
    public CharacterSheetDetails getCharacterSheetDetails(CharacterSheet sheet) {
        return characterSheetDetailsRepository.findByName(sheet.getName()).orElseThrow();
    }

    @Cacheable(cacheNames = "sheet-ranges", key = "'ranges@' + #characterSheet.name")
    public BatchGetValuesResponse getSpreadsheetMatrix(CharacterSheet characterSheet, List<String> ranges) throws IOException, GeneralSecurityException {
        log.info("Retrieving sheet range matrix");
        return SheetsUtil.getSheetsService().spreadsheets().values()
                .batchGet(characterSheet.getId())
                .setRanges(ranges)
                .execute();
    }

    public BatchGetValuesResponse getActualSpreadsheetMatrix(CharacterSheet characterSheet, List<String> ranges) throws IOException, GeneralSecurityException {
        log.info("Retrieving actual sheet range matrix");
        return SheetsUtil.getSheetsService().spreadsheets().values()
                .batchGet(characterSheet.getId())
                .setRanges(ranges)
                .execute();
    }
}
