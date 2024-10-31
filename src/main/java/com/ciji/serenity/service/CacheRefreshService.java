package com.ciji.serenity.service;

import com.ciji.serenity.model.CharacterSheet;
import com.ciji.serenity.model.CharacterSheetDetails;
import com.ciji.serenity.model.mapper.SheetMatrixMapper;
import com.ciji.serenity.repository.CharacterSheetDetailsRepository;
import com.ciji.serenity.repository.CharacterSheetRepository;
import com.google.api.services.sheets.v4.model.BatchGetValuesResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final CharacterSheetRepository characterSheetRepository;

    private final CharacterSheetDetailsService characterSheetDetailsService;

    private final CharacterSheetDetailsRepository characterSheetDetailsRepository;

    private static final List<String> MATRIX_RANGES = List.of("'Sheet'!AN27:AO40", "'Sheet'!AP27:AV40", "'Sheet'!BF7:BJ40", "'Sheet'!BW7:CJ40");

    @Scheduled(fixedRate = 12, timeUnit = TimeUnit.HOURS)
    @Transactional
    public void refreshSheetData() {
        log.info("Refreshing character sheet data of all users");
        characterSheetRepository.findAll().forEach(sheet -> {
            log.debug("Refreshing sheet data for {}", sheet.getName());
            characterSheetDetailsRepository.findByName(sheet.getName()).ifPresentOrElse(sheetDetails -> {
                        retrieveMatrixValues(sheet, sheetDetails);
                        characterSheetDetailsRepository.save(sheetDetails);
                    },
                    () -> {
                        CharacterSheetDetails sheetDetails = new CharacterSheetDetails();
                        sheetDetails.setName(sheet.getName());
                        retrieveMatrixValues(sheet, sheetDetails);
                        characterSheetDetailsRepository.save(sheetDetails);
                    });
        });
    }

    private void retrieveMatrixValues(CharacterSheet sheet, CharacterSheetDetails sheetDetails) {
        BatchGetValuesResponse readResult;
        try {
            readResult = characterSheetDetailsService.getSpreadsheetMatrix(sheet, MATRIX_RANGES);
            sheetDetails.setSpecialsMatrix(SheetMatrixMapper.map(List.of(readResult.getValueRanges().get(0), readResult.getValueRanges().get(1))));
            sheetDetails.setSkillMatrix(SheetMatrixMapper.map(List.of(readResult.getValueRanges().get(2), readResult.getValueRanges().get(3))));
        } catch (GeneralSecurityException | IOException e) {
            log.error("Cannot access character sheet of {}. Possibly not enough permissions", sheet.getName());
        }
    }
}
