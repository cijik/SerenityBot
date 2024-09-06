package com.ciji.serenity.service;

import com.ciji.serenity.dao.CharacterSheetDao;
import com.ciji.serenity.exception.OptionNotFoundException;
import com.ciji.serenity.model.CharacterSheet;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.sheets.v4.model.BatchGetValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.InteractionFollowupCreateMono;
import discord4j.core.spec.InteractionFollowupCreateSpec;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.WordUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CharacterSheetService {

    private final CharacterSheetDao characterSheetDao;

    public Mono<Message> getCharacter(ChatInputInteractionEvent event) {
        String characterName = getParameterValue(event, "name");
        event.deferReply().withEphemeral(true).block();

        CharacterSheet characterSheet = getCharacterSheet(characterName, event.getInteraction().getUser().getId().asString());
        if (characterSheet == null) {
            return createMissingCharacterFollowup(event, characterName);
        } else {
            Button button = Button.link("https://docs.google.com/spreadsheets/d/" + characterSheet.getId(), "Sheet");
            return event.createFollowup(InteractionFollowupCreateSpec.builder()
                            .content("Character **" + characterName + "**:")
                            .components(List.of(ActionRow.of(button)))
                            .build());
        }
    }

    public Mono<Message> getAllCharacters(ChatInputInteractionEvent event) {
        event.deferReply().withEphemeral(true).block();

        List<CharacterSheet> characters = characterSheetDao.findAllByOwnerId(event.getInteraction().getUser().getId().asString());
        String characterList = characters.stream().map(CharacterSheet::getName).collect(Collectors.joining(","));

        return event.createFollowup("Here's the list of all characters you own:\n**" + characterList + "**");
    }

    public Mono<Message> addCharacter(ChatInputInteractionEvent event) {
        String characterSheetUrl = getParameterValue(event, "url");
        String characterName = getParameterValue(event, "name");
        CharacterSheet characterSheet = new CharacterSheet();
        characterSheet.setId(characterSheetUrl.split("/")[5]);
        characterSheet.setName(WordUtils.capitalize(characterName.toLowerCase(Locale.ROOT)));
        characterSheet.setOwnerId(event.getInteraction().getUser().getId().asString());
        event.deferReply().withEphemeral(true).block();

        log.info("Adding character {} to database", characterName);
        characterSheetDao.save(characterSheet);
        return event.createFollowup("Character sheet added");
    }

    public Mono<Message> updateCharacter(ChatInputInteractionEvent event) {
        String characterName = getParameterValue(event, "name");
        String ownerId = getParameterValue(event, "owner-id");
        event.deferReply().withEphemeral(true).block();

        CharacterSheet characterSheet = characterSheetDao.findByName(WordUtils.capitalize(characterName.toLowerCase(Locale.ROOT)));
        if (characterSheet == null) {
            return createMissingCharacterFollowup(event, characterName);
        } else {
            characterSheet.setOwnerId(ownerId);
            characterSheetDao.save(characterSheet);
            return event.createFollowup(InteractionFollowupCreateSpec.builder()
                    .content("Character **" + characterName + "** has been updated with ownerId " + ownerId + ".")
                    .build());
        }
    }

    public Mono<Message> removeCharacter(ChatInputInteractionEvent event) {
        String characterName = getParameterValue(event, "name");
        event.deferReply().withEphemeral(true).block();

        CharacterSheet characterSheet = getCharacterSheet(characterName, event.getInteraction().getUser().getId().asString());
        if (characterSheet == null) {
            return createMissingCharacterFollowup(event, characterName);
        } else {
            log.info("Removing character {} from database", characterName);
            characterSheetDao.delete(characterSheet);
            return event.createFollowup("Character sheet deleted");
        }
    }

    @SneakyThrows
    public Mono<Message> readSheetValue(ChatInputInteractionEvent event) {
        String characterName = getParameterValue(event, "name");
        String sheetValue = getParameterValue(event, "value");
        event.deferReply().withEphemeral(true).block();

        CharacterSheet characterSheet = getCharacterSheet(characterName, event.getInteraction().getUser().getId().asString());
        if (characterSheet == null) {
            return createMissingCharacterFollowup(event, characterName);
        } else {
            List<String> ranges = List.of("'Shadow Spells'!C2:C115", "'Shadow Spells'!I2:I115");

            BatchGetValuesResponse readResult;
            try {
                readResult = getSpreadsheetMatrix(characterSheet, ranges);
            } catch (GoogleJsonResponseException e) {
                log.error("Cannot access character sheet");
                return event.createFollowup("Cannot access character sheet. Please add the bot (serenity-bot@serenitybot.iam.gserviceaccount.com) as an editor to the character sheet and/or make the sheet viewable to everyone with the link.");
            }
            sheetValue = WordUtils.capitalize(sheetValue.toLowerCase(Locale.ROOT));
            ValueRange spells = readResult.getValueRanges().getFirst();
            ValueRange descriptions = readResult.getValueRanges().get(1);
            int requestedSpell = spells.getValues().indexOf(List.of(sheetValue));

            return event.createFollowup(InteractionFollowupCreateSpec.builder()
                    .content(sheetValue + "'s **Description** is: " + descriptions.getValues().get(requestedSpell))
                    .build());
        }
    }

    public CharacterSheet getCharacterSheet(String characterName, String ownerId) {
        log.info("Retrieving character {} for user {}", characterName, ownerId);
        return characterSheetDao.findByNameAndOwnerId(WordUtils.capitalize(characterName.toLowerCase(Locale.ROOT)), ownerId);
    }

    @Cacheable(cacheNames = "sheets", key = "#characterSheet.name + '@' + #ranges.get(1)")
    public BatchGetValuesResponse getSpreadsheetMatrix(CharacterSheet characterSheet, List<String> ranges) throws IOException, GeneralSecurityException {
        log.info("Retrieving sheet range matrix");
        return SheetsServiceUtil.getSheetsService().spreadsheets().values()
                .batchGet(characterSheet.getId())
                .setRanges(ranges)
                .execute();
    }

    private static String getParameterValue(ChatInputInteractionEvent event, String name) {
        return event
                .getOption(name)
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .orElseThrow(OptionNotFoundException::new);
    }

    private static InteractionFollowupCreateMono createMissingCharacterFollowup(ChatInputInteractionEvent event, String characterName) {
        log.error("Character {} not found for user {}", characterName, event.getInteraction().getUser().getId().asString());
        return event.createFollowup("Character not found");
    }
}
