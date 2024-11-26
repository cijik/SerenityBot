package com.ciji.serenity.service;

import com.google.api.services.sheets.v4.model.ValueRange;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.entity.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.security.GeneralSecurityException;

@Service
@RequiredArgsConstructor
@Slf4j
public class EffectsService {

    private final CharacterSheetService characterSheetService;

    private final CharacterSheetDetailsService characterSheetDetailsService;

    public Mono<Message> setRadiation(ChatInputInteractionEvent event) {
        String characterName = getParameterValue(event, "character-name");
        String rads = getParameterValue(event, "rads");

        return characterSheetService.getCharacterSheet(characterName, event.getInteraction().getUser().getId().asString())
                        .flatMap(characterSheet -> {
                            ValueRange radsCell;
                            try {
                                radsCell = characterSheetDetailsService.getSpreadsheetMatrix(characterSheet, CacheRefreshService.MATRIX_RANGES).getValueRanges().get(4);
                            } catch (IOException | GeneralSecurityException e) {
                                log.error("Could not retrieve spreadsheet matrices for {}. Cause: {}", characterName, e.getMessage());
                                return event.createFollowup("Could not retrieve sheet data for " + characterName);
                            }
                            radsCell.getValues().getFirst().set(0, rads);
                            try {
                                SheetsServiceUtil.getSheetsService()
                                        .spreadsheets().values()
                                        .update(characterSheet.getId(), CacheRefreshService.MATRIX_RANGES.get(4).replace("'", ""), radsCell)
                                        .setValueInputOption("RAW")
                                        .execute();
                            } catch (IOException | GeneralSecurityException e) {
                                log.error("Could not connect to Sheets Service. Cause: {}", e.getMessage());
                                return event.createFollowup("Could not connect to Google Sheets. Try again later.");
                            }
                            log.info("Changed radiation for {} to {}", characterName, rads);
                            return event.createFollowup("Radiation changed to **" + rads + "** for " + characterName);
                        });
    }

    private static String getParameterValue(ChatInputInteractionEvent event, String name) {
        return event
                .getOption(name)
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .orElse("");
    }
}
