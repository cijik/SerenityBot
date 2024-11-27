package com.ciji.serenity.service;

import com.ciji.serenity.model.CharacterSheet;
import com.ciji.serenity.model.CharacterSheetDetails;
import com.ciji.serenity.model.SheetMatrix;
import com.ciji.serenity.repository.CharacterSheetDetailsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CharacterSheetDetailsServiceTest {

    @Mock
    private CharacterSheetDetailsRepository characterSheetDetailsRepository;

    @InjectMocks
    private CharacterSheetDetailsService characterSheetDetailsService;

    @Test
    void getCharacterSheetDetails() {
        CharacterSheet sheet = new CharacterSheet();
        sheet.setName("Character");
        CharacterSheetDetails sheetDetails = new CharacterSheetDetails();
        sheetDetails.setName("Character");
        when(characterSheetDetailsRepository.findByName("Character")).thenReturn(Optional.of(sheetDetails));

        assertThat(characterSheetDetailsService.getCharacterSheetDetails(sheet).getName()).isEqualTo(sheetDetails.getName());
    }
}