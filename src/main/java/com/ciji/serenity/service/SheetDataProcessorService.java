package com.ciji.serenity.service;

import com.ciji.serenity.model.CharacterSheet;
import com.ciji.serenity.model.CharacterSheetDetails;
import com.ciji.serenity.model.mapper.SheetMatrixMapper;
import com.ciji.serenity.repository.CharacterSheetDetailsRepository;
import com.ciji.serenity.repository.CharacterSheetRepository;
import com.google.api.services.sheets.v4.model.BatchGetValuesResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SheetDataProcessorService {

    private final CharacterSheetRepository characterSheetRepository;
    private final CharacterSheetDetailsService characterSheetDetailsService;
    private final CharacterSheetDetailsRepository characterSheetDetailsRepository;

    static final List<String> MATRIX_RANGES = List.of("'Sheet'!AN27:AO40", "'Sheet'!AP27:AV40", "'Sheet'!BF7:BJ40", "'Sheet'!BW7:CJ40", "'Sheet'!DA10", "'Sheet'!DA26");

    @Transactional
    protected void processSheetData(boolean isScheduled) {
        characterSheetRepository.findAll().forEach(sheet -> {
            log.debug("Refreshing sheet data for {}", sheet.getName());
            characterSheetDetailsRepository.findByName(sheet.getName()).ifPresentOrElse(sheetDetails -> {
                        retrieveMatrixValues(sheet, sheetDetails, isScheduled);
                        characterSheetDetailsRepository.save(sheetDetails);
                    },
                    () -> {
                        CharacterSheetDetails sheetDetails = new CharacterSheetDetails();
                        sheetDetails.setName(sheet.getName());
                        retrieveMatrixValues(sheet, sheetDetails, isScheduled);
                        characterSheetDetailsRepository.save(sheetDetails);
                    });
        });
        log.info("Finished refreshing character sheet data");
    }


    private void retrieveMatrixValues(CharacterSheet sheet, CharacterSheetDetails sheetDetails, boolean isScheduled) {
        BatchGetValuesResponse readResult;
        try {
            readResult = isScheduled
                    ?
                    characterSheetDetailsService.getSpreadsheetMatrix(sheet, MATRIX_RANGES)
                    :
                    characterSheetDetailsService.getActualSpreadsheetMatrix(sheet, MATRIX_RANGES);
            sheetDetails.setSpecialsMatrix(SheetMatrixMapper.map(List.of(readResult.getValueRanges().get(0), readResult.getValueRanges().get(1))));
            sheetDetails.setSkillMatrix(SheetMatrixMapper.map(List.of(readResult.getValueRanges().get(2), readResult.getValueRanges().get(3))));
            if (readResult.getValueRanges().get(4).getValues() != null) {
                sheetDetails.setRads(Integer.parseInt(readResult.getValueRanges().get(4).getValues().getFirst().getFirst().toString()));
            } else {
                sheetDetails.setRads(0);
            }
            if (readResult.getValueRanges().get(5).getValues() != null) {
                sheetDetails.setTemperature(Integer.parseInt(readResult.getValueRanges().get(5).getValues().getFirst().getFirst().toString().replace("°C", "")));
            } else {
                sheetDetails.setTemperature(0);
            }
        } catch (GeneralSecurityException | IOException e) {
            log.error("Cannot access character sheet of {}. Possibly not enough permissions", sheet.getName());
        } catch (NullPointerException e) {
            log.debug("No data entry found for {}", sheet.getName());
            log.error(e.getMessage(), e);
        }
    }
}
