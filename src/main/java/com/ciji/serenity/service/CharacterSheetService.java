package com.ciji.serenity.service;

import com.ciji.serenity.dao.CharacterSheetDao;
import com.ciji.serenity.enums.Modifiers;
import com.ciji.serenity.exception.OptionNotFoundException;
import com.ciji.serenity.model.CharacterSheet;
import com.google.api.services.sheets.v4.model.BatchGetValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.InteractionFollowupCreateSpec;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Locale;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class CharacterSheetService {

    private final CharacterSheetDao characterSheetDao;

    public Mono<Message> getCharacter(ChatInputInteractionEvent event) {
        String characterName = event
                .getOption("name")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .orElseThrow(OptionNotFoundException::new);
        event.deferReply().withEphemeral(true).block();
        CharacterSheet characterSheet = characterSheetDao.findByName(WordUtils.capitalize(characterName.toLowerCase(Locale.ROOT)));
        if (characterSheet == null) {
            return event.createFollowup("Character not found");
        } else {
            Button button = Button.link("https://docs.google.com/spreadsheets/d/" + characterSheet.getId(), "Sheet");
            return event.createFollowup(InteractionFollowupCreateSpec.builder()
                            .content("Character **" + characterName + "**:")
                            .components(List.of(ActionRow.of(button)))
                            .build());
        }
    }

    public Mono<Message> addCharacter(ChatInputInteractionEvent event) {
        String characterSheetUrl = event
                .getOption("url").flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .orElseThrow(OptionNotFoundException::new);
        String characterName = event
                .getOption("name").flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .orElseThrow(OptionNotFoundException::new);
        CharacterSheet characterSheet = new CharacterSheet();
        characterSheet.setId(characterSheetUrl.split("/")[5]);
        characterSheet.setName(WordUtils.capitalize(characterName.toLowerCase(Locale.ROOT)));
        event.deferReply().withEphemeral(true).block();
        characterSheetDao.save(characterSheet);
        return event.createFollowup("Character sheet added");
    }

    public Mono<Message> removeCharacter(ChatInputInteractionEvent event) {
        String characterName = event
                .getOption("name").flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .orElseThrow(OptionNotFoundException::new);
        event.deferReply().withEphemeral(true).block();
        CharacterSheet characterSheet = characterSheetDao.findByName(WordUtils.capitalize(characterName.toLowerCase(Locale.ROOT)));
        if (characterSheet == null) {
            return event.createFollowup("Character not found");
        } else {
            characterSheetDao.delete(characterSheet);
            return event.createFollowup("Character sheet deleted");
        }
    }

    @SneakyThrows
    public Mono<Message> readSheetValue(ChatInputInteractionEvent event) {
        String characterName = event
                .getOption("name")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .orElseThrow(OptionNotFoundException::new);
        String sheetValue = event
                .getOption("value")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .orElseThrow(OptionNotFoundException::new);
        event.deferReply().withEphemeral(true).block();

        CharacterSheet characterSheet = characterSheetDao.findByName(WordUtils.capitalize(characterName.toLowerCase(Locale.ROOT)));
        if (characterSheet == null) {
            return event.createFollowup("Character not found");
        } else {
            List<String> ranges = List.of("'Shadow Spells'!C2:C115", "'Shadow Spells'!I2:I115");
            BatchGetValuesResponse readResult = SheetsServiceUtil.getSheetsService().spreadsheets().values()
                    .batchGet(characterSheet.getId())
                    .setRanges(ranges)
                    .execute();
            sheetValue = WordUtils.capitalize(sheetValue.toLowerCase(Locale.ROOT));
            ValueRange spells = readResult.getValueRanges().getFirst();
            ValueRange descriptions = readResult.getValueRanges().get(1);
            int requestedSpell = spells.getValues().indexOf(List.of(sheetValue));

            return event.createFollowup(InteractionFollowupCreateSpec.builder()
                    .content(sheetValue + "'s **Description** is: " + descriptions.getValues().get(requestedSpell))
                    .build());
        }
    }

    @SneakyThrows
    public Mono<Message> rollSkill(ChatInputInteractionEvent event) {
        String characterName = event
                .getOption("character-name")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .orElseThrow(OptionNotFoundException::new);
        String skillName = event
                .getOption("skill-name")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .orElseThrow(OptionNotFoundException::new);
        String skillModifier = event
                .getOption("skill-modifier")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .orElseThrow(OptionNotFoundException::new);
        event.deferReply().withEphemeral(false).block();

        CharacterSheet characterSheet = characterSheetDao.findByName(WordUtils.capitalize(characterName.toLowerCase(Locale.ROOT)));
        if (characterSheet == null) {
            return event.createFollowup("Character not found");
        } else {
            List<String> ranges = List.of("'Sheet'!BF7:BJ40", "'Sheet'!BW7:CJ40");
            BatchGetValuesResponse readResult = SheetsServiceUtil.getSheetsService().spreadsheets().values()
                    .batchGet(characterSheet.getId())
                    .setRanges(ranges)
                    .execute();
            skillName = StringUtils.capitalize(skillName.toLowerCase(Locale.ROOT));
            ValueRange skillNames = readResult.getValueRanges().getFirst();
            ValueRange skillValueMatrix = readResult.getValueRanges().get(1);
            int requestedSkill = skillNames.getValues().indexOf(List.of(skillName));
            int requestedModifier = Modifiers.fromString(skillModifier).ordinal() * 2;

            int skillThreshold = Integer.parseInt((String) skillValueMatrix.getValues().get(requestedSkill).get(requestedModifier));
            int roll = new Random().nextInt(100);
            String result = roll < skillThreshold ? "Success!" : "Failure!";

            return event.createFollowup(InteractionFollowupCreateSpec.builder()
                    .content(WordUtils.capitalize(characterName.toLowerCase(Locale.ROOT)) + " rolls " + skillName + "[**" + skillThreshold + "**] at **" + skillModifier + "**, resulting in: **" + roll + "**, " + result)
                    .build());
        }
    }
}
