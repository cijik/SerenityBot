package com.ciji.serenity.service;

import com.ciji.serenity.repository.CharacterSheetRepository;
import com.google.api.services.sheets.v4.model.ValueRange;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EffectsService {

    private final CharacterSheetService characterSheetService;

    private final CharacterSheetDetailsService characterSheetDetailsService;

    private final CharacterSheetRepository characterSheetRepository;

    public Mono<Message> setRadiation(ChatInputInteractionEvent event) {
        String characterName = SheetsUtil.getParameterValue(event, "character-name");
        String rads = SheetsUtil.getParameterValue(event, "rads");
        try {
            Integer.parseInt(rads);
        } catch (NumberFormatException e) {
            log.error("Invalid radiation value: {}", rads);
            return event.createFollowup(rads + " is not a valid radiation value");
        }

        return characterSheetService.getCharacterSheet(characterName, event.getInteraction().getUser().getId().asString())
                        .flatMap(characterSheet -> {
                            ValueRange radsCell;
                            try {
                                radsCell = characterSheetDetailsService.getSpreadsheetMatrix(characterSheet, SheetDataProcessorService.MATRIX_RANGES).getValueRanges().get(4);
                            } catch (IOException | GeneralSecurityException e) {
                                log.error("Could not retrieve spreadsheet matrices for {}. Cause: {}", characterName, e.getMessage());
                                return event.createFollowup("Could not retrieve sheet data for " + characterName);
                            }
                            radsCell.getValues().getFirst().set(0, rads);
                            try {
                                SheetsUtil.getSheetsService()
                                        .spreadsheets().values()
                                        .update(characterSheet.getId(), SheetDataProcessorService.MATRIX_RANGES.get(4).replace("'", ""), radsCell)
                                        .setValueInputOption("USER_ENTERED")
                                        .execute();
                            } catch (IOException | GeneralSecurityException e) {
                                log.error("Could not connect to Sheets Service. Cause: {}", e.getMessage());
                                return event.createFollowup("Could not connect to Google Sheets. Try again later.");
                            }
                            log.info("Changed radiation for {} to {}", characterName, rads);
                            return event.createFollowup("Radiation changed to **" + rads + "** for " + characterName);
                        });
    }

    public Mono<Message> setTemperature(ChatInputInteractionEvent event) {
        String characterNames = SheetsUtil.getParameterValue(event, "character-names");
        String temperature = SheetsUtil.getParameterValue(event, "temperature");
        try {
            Integer.parseInt(temperature);
        } catch (NumberFormatException e) {
            log.error("Invalid temperature value: {}", temperature);
            return event.createFollowup(temperature + " is not a valid temperature value");
        }

        List<String> responses = Arrays.stream(characterNames.split(","))
                .map(characterName -> characterSheetRepository.findByName(characterName)
                        .map(characterSheet -> {
                            ValueRange tempCell;
                            try {
                                tempCell = characterSheetDetailsService.getSpreadsheetMatrix(characterSheet, SheetDataProcessorService.MATRIX_RANGES).getValueRanges().get(5);
                            } catch (IOException | GeneralSecurityException e) {
                                log.error("Could not retrieve spreadsheet matrices for {}. Cause: {}", characterName, e.getMessage());
                                return "Could not retrieve sheet data for " + characterName;
                            }
                            tempCell.getValues().getFirst().set(0, temperature);
                            try {
                                SheetsUtil.getSheetsService()
                                        .spreadsheets().values()
                                        .update(characterSheet.getId(), SheetDataProcessorService.MATRIX_RANGES.get(5).replace("'", ""), tempCell)
                                        .setValueInputOption("USER_ENTERED")
                                        .execute();
                            } catch (IOException | GeneralSecurityException e) {
                                log.error("Could not connect to Sheets Service for {}. Cause: {}", characterName, e.getMessage());
                                return "Could not connect to Google Sheets. Try again later.";
                            }
                            log.info("Changed temperature for {} to {}", characterName, temperature);
                            return characterName;
                        })
                        .orElse("Character not found"))
                .toList();

        String successfulCharacterList = responses.stream()
                .filter(response -> !response.contains("Could not") && !response.contains("not found"))
                .reduce((character1, character2) -> character1 + ", " + character2)
                .orElse("");

        if (responses.stream().filter(response -> !response.contains("Could not") && !response.contains("not found")).count() < characterNames.length()) {
            return event.createFollowup("Temperature changes applied with issues. The following characters' sheets have been updated: " + successfulCharacterList);
        } else {
            return event.createFollowup("Temperature changes applied successfully.");
        }
    }
}
