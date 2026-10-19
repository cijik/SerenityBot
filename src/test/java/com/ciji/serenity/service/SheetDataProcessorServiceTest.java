package com.ciji.serenity.service;

import com.ciji.serenity.model.CharacterSheet;
import com.ciji.serenity.model.CharacterSheetDetails;
import com.ciji.serenity.repository.CharacterSheetDetailsRepository;
import com.ciji.serenity.repository.CharacterSheetRepository;
import com.google.api.services.sheets.v4.model.BatchGetValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SheetDataProcessorServiceTest {

    @Mock
    private CharacterSheetRepository characterSheetRepository;

    @Mock
    private CharacterSheetDetailsService characterSheetDetailsService;

    @Mock
    private CharacterSheetDetailsRepository characterSheetDetailsRepository;

    @InjectMocks
    private SheetDataProcessorService sheetDataProcessorService;

    @Test
    void processSheetDataUpdatesExistingDetailsUsingScheduledMatrix() throws Exception {
        CharacterSheet sheet = new CharacterSheet();
        sheet.setName("Character");
        CharacterSheetDetails existing = new CharacterSheetDetails();
        existing.setName("Character");

        when(characterSheetRepository.findAll()).thenReturn(List.of(sheet));
        when(characterSheetDetailsRepository.findByName("Character")).thenReturn(Optional.of(existing));
        when(characterSheetDetailsService.getSpreadsheetMatrix(eq(sheet), anyList())).thenReturn(matrixResponse());
        when(characterSheetDetailsRepository.save(any(CharacterSheetDetails.class))).thenAnswer(invocation -> invocation.getArgument(0));

        sheetDataProcessorService.processSheetData(true);

        verify(characterSheetDetailsService).getSpreadsheetMatrix(eq(sheet), anyList());
        verify(characterSheetDetailsRepository).save(existing);
        assertThat(existing.getSkillMatrix().getHeaders()).containsExactly("Name");
        assertThat(existing.getSkillMatrix().getRows()).hasSize(1);
        assertThat(existing.getRads()).isEqualTo(4);
        assertThat(existing.getTemperature()).isEqualTo(21);
        assertThat(existing.getInventoryMatrix()).isNotNull();
        assertThat(existing.getInventoryMatrix().getHeaders()).containsExactly("Item", "Weight", "Amount", "Value", "Total weight", "Total value");
        assertThat(existing.getInventoryMatrix().getRows()).hasSize(2);
        assertThat(existing.getInventoryMatrix().getRows().get(0).getRow()).containsExactly("Stimpak", "0.5", "3", "25", "1.5", "75");
        assertThat(existing.getInventoryMatrix().getRows().get(1).getRow()).containsExactly("Radium Rifle", "11", "1", "250", "11", "250");
    }

    @Test
    void processSheetDataCreatesDetailsUsingActualMatrix() throws Exception {
        CharacterSheet sheet = new CharacterSheet();
        sheet.setName("Character");

        when(characterSheetRepository.findAll()).thenReturn(List.of(sheet));
        when(characterSheetDetailsRepository.findByName("Character")).thenReturn(Optional.empty());
        when(characterSheetDetailsService.getActualSpreadsheetMatrix(eq(sheet), anyList())).thenReturn(matrixResponse());
        when(characterSheetDetailsRepository.save(any(CharacterSheetDetails.class))).thenAnswer(invocation -> invocation.getArgument(0));

        sheetDataProcessorService.processSheetData(false);

        verify(characterSheetDetailsService).getActualSpreadsheetMatrix(eq(sheet), anyList());
        verify(characterSheetDetailsRepository).save(any(CharacterSheetDetails.class));
    }

    @Test
    void processSheetDataHandlesEmptyInventoryRanges() throws Exception {
        CharacterSheet sheet = new CharacterSheet();
        sheet.setName("Character");
        CharacterSheetDetails existing = new CharacterSheetDetails();
        existing.setName("Character");

        ValueRange emptyMainInventory = new ValueRange();
        ValueRange emptyExtraInventory = new ValueRange();

        BatchGetValuesResponse response = new BatchGetValuesResponse().setValueRanges(List.of(
                new ValueRange().setValues(List.of(List.of("Name"))),
                new ValueRange().setValues(List.of(List.of("100", "90"))),
                new ValueRange().setValues(List.of(List.of("Name"))),
                new ValueRange().setValues(List.of(List.of("100", "80"))),
                new ValueRange().setValues(List.of(List.of(4))),
                new ValueRange().setValues(List.of(List.of("21°C"))),
                emptyMainInventory,
                emptyExtraInventory
        ));

        when(characterSheetRepository.findAll()).thenReturn(List.of(sheet));
        when(characterSheetDetailsRepository.findByName("Character")).thenReturn(Optional.of(existing));
        when(characterSheetDetailsService.getSpreadsheetMatrix(eq(sheet), anyList())).thenReturn(response);
        when(characterSheetDetailsRepository.save(any(CharacterSheetDetails.class))).thenAnswer(invocation -> invocation.getArgument(0));

        sheetDataProcessorService.processSheetData(true);

        verify(characterSheetDetailsRepository).save(existing);
    }

    private BatchGetValuesResponse matrixResponse() {
        ValueRange specialsHeaders = new ValueRange().setValues(List.of(List.of("Name")));
        ValueRange specialsRows = new ValueRange().setValues(List.of(List.of("100", "90")));
        ValueRange skillHeaders = new ValueRange().setValues(List.of(List.of("Name")));
        ValueRange skillRows = new ValueRange().setValues(List.of(List.of("100", "80")));
        ValueRange rads = new ValueRange().setValues(List.of(List.of(4)));
        ValueRange temperature = new ValueRange().setValues(List.of(List.of("21°C")));

        // Main inventory layout: (0, 8, 10, 12, 14, 18)
        List<Object> mainHeader = createPaddedRow(19, 0, "Item", 8, "Weight", 10, "Amount", 12, "Value", 14, "Total weight", 18, "Total value");
        List<Object> mainItem = createPaddedRow(19, 0, "Stimpak", 8, "0.5", 10, "3", 12, "25", 14, "1.5", 18, "75");
        List<Object> mainEmptyItem = createPaddedRow(19, 0, "", 8, "0", 10, "0", 12, "0", 14, "0", 18, "0");
        ValueRange mainInventory = new ValueRange().setValues(List.of(mainHeader, mainItem, mainEmptyItem));

        // Extra inventory layout: (0, 4, 5, 6, 7, 8)
        List<Object> extraItem = createPaddedRow(9, 0, "Radium Rifle", 4, "11", 5, "1", 6, "250", 7, "11", 8, "250");
        List<Object> extraTotal = createPaddedRow(9, 0, "Total", 4, "", 5, "", 6, "", 7, "11.5", 8, "325");
        ValueRange extraInventory = new ValueRange().setValues(List.of(extraItem, extraTotal));

        return new BatchGetValuesResponse().setValueRanges(List.of(
                specialsHeaders,
                specialsRows,
                skillHeaders,
                skillRows,
                rads,
                temperature,
                mainInventory,
                extraInventory
        ));
    }

    private static List<Object> createPaddedRow(int size, Object... indexValuePairs) {
        Object[] row = new Object[size];
        Arrays.fill(row, "");
        for (int i = 0; i < indexValuePairs.length; i += 2) {
            int index = (Integer) indexValuePairs[i];
            Object value = indexValuePairs[i + 1];
            row[index] = value;
        }
        return List.of(row);
    }
}
