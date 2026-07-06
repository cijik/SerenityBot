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

    private BatchGetValuesResponse matrixResponse() {
        ValueRange specialsHeaders = new ValueRange().setValues(List.of(List.of("Name")));
        ValueRange specialsRows = new ValueRange().setValues(List.of(List.of("100", "90")));
        ValueRange skillHeaders = new ValueRange().setValues(List.of(List.of("Name")));
        ValueRange skillRows = new ValueRange().setValues(List.of(List.of("100", "80")));
        ValueRange rads = new ValueRange().setValues(List.of(List.of(4)));
        ValueRange temperature = new ValueRange().setValues(List.of(List.of("21°C")));

        return new BatchGetValuesResponse().setValueRanges(List.of(
                specialsHeaders,
                specialsRows,
                skillHeaders,
                skillRows,
                rads,
                temperature
        ));
    }
}
