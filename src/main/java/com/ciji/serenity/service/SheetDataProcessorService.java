package com.ciji.serenity.service;

import com.ciji.serenity.model.CharacterSheet;
import com.ciji.serenity.model.CharacterSheetDetails;
import com.ciji.serenity.model.mapper.SheetMatrixMapper;
import com.ciji.serenity.repository.CharacterSheetDetailsRepository;
import com.ciji.serenity.repository.CharacterSheetRepository;
import com.google.api.services.sheets.v4.model.BatchGetValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class SheetDataProcessorService {

    private final CharacterSheetRepository characterSheetRepository;
    private final CharacterSheetDetailsService characterSheetDetailsService;
    private final CharacterSheetDetailsRepository characterSheetDetailsRepository;

    static final List<String> MATRIX_RANGES = List.of("'Sheet'!AN27:AO40", "'Sheet'!AP27:AV40", "'Sheet'!BF7:BJ40", "'Sheet'!BW7:CJ40",
            "'Sheet'!DA10", "'Sheet'!DA26", "'Sheet'!CX72:DS99", "'Extra Inventory'!B5:J283");

    private static final InventoryLayout MAIN_INVENTORY_LAYOUT = new InventoryLayout(0, 8, 10, 12, 14, 18);
    private static final InventoryLayout EXTRA_INVENTORY_LAYOUT = new InventoryLayout(0, 4, 5, 6, 7, 8);

    /**
     * Column indexes relative to the start of the requested range.
     * Normalized row shape: [item, weight, amount, value, totalWeight, totalValue]
     */
    private record InventoryLayout(int item, int weight, int amount, int value, int totalWeight, int totalValue) {

        List<Object> normalize(List<Object> row) {
            return List.of(cell(row, item), cell(row, weight), cell(row, amount),
                    cell(row, value), cell(row, totalWeight), cell(row, totalValue));
        }

        boolean hasItem(List<Object> row) {
            return !cell(row, item).toString().isBlank();
        }

        // Rows can be shorter than the range, because trailing empty cells are omitted
        private static Object cell(List<Object> row, int index) {
            return index < row.size() ? Objects.requireNonNullElse(row.get(index), "") : "";
        }
    }

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
            sheetDetails.setInventoryMatrix(SheetMatrixMapper.mapInventory(
                    mergeInventoryRows(readResult.getValueRanges().get(6), readResult.getValueRanges().get(7))));
        } catch (GeneralSecurityException | IOException e) {
            log.error("Cannot access character sheet of {}. Possibly not enough permissions", sheet.getName());
        } catch (NullPointerException e) {
            log.debug("No data entry found for {}", sheet.getName());
            log.error(e.getMessage(), e);
        }
    }

    private static List<List<Object>> mergeInventoryRows(ValueRange main, ValueRange extra) {
        return Stream.concat(
                        normalizeRows(main, MAIN_INVENTORY_LAYOUT),
                        normalizeRows(extra, EXTRA_INVENTORY_LAYOUT))
                .toList();
    }

    private static Stream<List<Object>> normalizeRows(ValueRange range, InventoryLayout layout) {
        if (range == null || range.getValues() == null) { // range is completely empty
            return Stream.empty();
        }
        return range.getValues().stream()
                .filter(Objects::nonNull)
                .filter(layout::hasItem) // skip rows without an item name
                .map(layout::normalize);
    }
}
