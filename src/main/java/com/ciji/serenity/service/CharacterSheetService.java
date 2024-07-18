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
        String characterName = getParameterValue(event, "name");
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
        String characterSheetUrl = getParameterValue(event, "url");
        String characterName = getParameterValue(event, "name");
        CharacterSheet characterSheet = new CharacterSheet();
        characterSheet.setId(characterSheetUrl.split("/")[5]);
        characterSheet.setName(WordUtils.capitalize(characterName.toLowerCase(Locale.ROOT)));
        event.deferReply().withEphemeral(true).block();
        characterSheetDao.save(characterSheet);
        return event.createFollowup("Character sheet added");
    }

    public Mono<Message> removeCharacter(ChatInputInteractionEvent event) {
        String characterName = getParameterValue(event, "name");
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
        String characterName = getParameterValue(event, "name");
        String sheetValue = getParameterValue(event, "value");
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
        String characterName = getParameterValue(event, "character-name");
        String skillName = getParameterValue(event, "skill-name");
        String skillModifier = getParameterValue(event, "skill-modifier");
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
            skillName = WordUtils.capitalize(skillName.toLowerCase(Locale.ROOT));
            ValueRange skillNames = readResult.getValueRanges().getFirst();
            ValueRange skillValueMatrix = readResult.getValueRanges().get(1);
            int requestedSkill = skillNames.getValues().indexOf(List.of(skillName));
            int requestedModifier = Modifiers.fromString(skillModifier).ordinal() * 2;

            int skillThreshold;
            try {
                skillThreshold = Integer.parseInt((String) skillValueMatrix.getValues().get(requestedSkill).get(requestedModifier));
            } catch (IndexOutOfBoundsException e) {
                return event.createFollowup("**" + characterName + "** does not have this skill");
            }
            int roll = new Random().nextInt(100);
            String result = roll <= skillThreshold ? "Success!" : "Failure!";

            return event.createFollowup(InteractionFollowupCreateSpec.builder()
                    .content(WordUtils.capitalize(characterName.toLowerCase(Locale.ROOT)) + " rolls " + skillName + "[**" + skillThreshold + "**] at **" + skillModifier + "**, resulting in: **" + roll + "**, " + result)
                    .build());
        }
    }

    @SneakyThrows
    public Mono<Message> rollSpecial(ChatInputInteractionEvent event) {
        String characterName = getParameterValue(event, "character-name");
        String specialName = getParameterValue(event, "special-name");
        String specialModifier = getParameterValue(event, "special-modifier");
        event.deferReply().withEphemeral(false).block();

        CharacterSheet characterSheet = characterSheetDao.findByName(WordUtils.capitalize(characterName.toLowerCase(Locale.ROOT)));
        if (characterSheet == null) {
            return event.createFollowup("Character not found");
        } else {
            List<String> ranges = List.of("'Sheet'!AN27:AO40", "'Sheet'!AP27:AV40");
            BatchGetValuesResponse readResult = SheetsServiceUtil.getSheetsService().spreadsheets().values()
                    .batchGet(characterSheet.getId())
                    .setRanges(ranges)
                    .execute();
            specialName = StringUtils.capitalize(specialName.toLowerCase(Locale.ROOT));
            ValueRange specialNames = readResult.getValueRanges().getFirst();
            ValueRange specialValueMatrix = readResult.getValueRanges().get(1);
            int requestedSpecial = specialNames.getValues().indexOf(List.of(StringUtils.truncate(specialName, 1)));
            int requestedModifier = Modifiers.fromString(specialModifier).ordinal();

            int specialThreshold;
            try {
                specialThreshold = Integer.parseInt((String) specialValueMatrix.getValues().get(requestedSpecial).get(requestedModifier));
            } catch (IndexOutOfBoundsException e) {
                return event.createFollowup(specialName + " is not a valid SPECIAL");
            }
            int roll = new Random().nextInt(100);
            String result = roll <= specialThreshold ? "Success!" : "Failure!";

            return event.createFollowup(InteractionFollowupCreateSpec.builder()
                    .content(WordUtils.capitalize(characterName.toLowerCase(Locale.ROOT)) + " rolls " + specialName + "[**" + specialThreshold + "**] at **" + specialModifier + "**, resulting in: **" + roll + "**, " + result)
                    .build());
        }
    }

    @SneakyThrows
    public Mono<Message> rollMFD(ChatInputInteractionEvent event) {
        String characterName = getParameterValue(event, "character-name");
        String attributeType = getParameterValue(event, "attribute-type");
        String attributeName = getParameterValue(event, "attribute-name");
        String attributeModifier = getParameterValue(event, "step-modifier");
        event.deferReply().withEphemeral(false).block();

        CharacterSheet characterSheet = characterSheetDao.findByName(WordUtils.capitalize(characterName.toLowerCase(Locale.ROOT)));
        if (characterSheet == null) {
            return event.createFollowup("Character not found");
        } else {
            List<String> ranges;
            int cellModifier;
            if (attributeType.equalsIgnoreCase("SPECIAL")) {
                ranges = List.of("'Sheet'!AN27:AO40", "'Sheet'!AP27:AV40");
                cellModifier = 1;
            } else if (attributeType.equalsIgnoreCase("Skill")) {
                ranges = List.of("'Sheet'!BF7:BJ40", "'Sheet'!BW7:CJ40");
                cellModifier = 2;
            } else {
                return event.createFollowup(attributeType + " is not a valid attribute type");
            }
            BatchGetValuesResponse readResult = SheetsServiceUtil.getSheetsService().spreadsheets().values()
                    .batchGet(characterSheet.getId())
                    .setRanges(ranges)
                    .execute();
            attributeName = WordUtils.capitalize(attributeName.toLowerCase(Locale.ROOT));
            characterName = WordUtils.capitalize(characterName.toLowerCase(Locale.ROOT));
            ValueRange attributeNames = readResult.getValueRanges().getFirst();
            ValueRange attributeValueMatrix = readResult.getValueRanges().get(1);
            int requestedAttribute;
            if (attributeType.equalsIgnoreCase("SPECIAL")) {
                requestedAttribute = attributeNames.getValues().indexOf(List.of(StringUtils.truncate(attributeName, 1)));
            } else if (attributeType.equalsIgnoreCase("Skill")) {
                requestedAttribute = attributeNames.getValues().indexOf(List.of(attributeName));
            } else {
                return event.createFollowup(attributeType + " is not a valid attribute type");
            }

            int roll = new Random().nextInt(100) + 1;
            if (roll > 95) {
                return event.createFollowup(characterName + " rolls a **" + roll + "** on " + attributeName + ", **failing spectacularly**!");
            }
            if (roll < 6) {
                return event.createFollowup(characterName + " rolls a **" + roll + "** on " + attributeName + ", **succeeding critically**!");
            }

            Object currentMFD;
            try {
                currentMFD = attributeValueMatrix.getValues().get(requestedAttribute).reversed().stream()
                        .filter(mfd -> !((String) mfd).isEmpty() && Integer.parseInt((String) mfd) >= roll).findFirst().orElse(null);
            } catch (IndexOutOfBoundsException e) {
                return event.createFollowup(attributeName + " is not a valid attribute for specified attribute type");
            }
            if (currentMFD == null) {
                return event.createFollowup("The roll of **" + roll + "** is above MFD 2 (**" + attributeValueMatrix.getValues().get(requestedAttribute).getFirst() + "**) for " + attributeName);
            } else {
                int MFDIndex = attributeValueMatrix.getValues().get(requestedAttribute).indexOf(currentMFD);
                String matchingMFD = Modifiers.values()[MFDIndex / cellModifier].getModifier();
                int matchingMFDValue = Integer.parseInt((String) attributeValueMatrix.getValues().get(requestedAttribute).get(MFDIndex));
                String modifiedMatchingMFD;
                try {
                    modifiedMatchingMFD = getModifiedMFD(MFDIndex, cellModifier, attributeModifier);
                } catch (NumberFormatException e) {
                    return event.createFollowup(attributeModifier + " is not a valid modifier");
                }
                boolean isModifierPositive = Integer.parseInt(attributeModifier) >= 0;
                StringBuilder response = new StringBuilder();
                response
                        .append(characterName)
                        .append(" rolls **").append(roll)
                        .append("** for ").append(attributeName)
                        .append(" succeeding MFD **").append(matchingMFD).append("** (**").append(matchingMFDValue).append("**)");
                if (Integer.parseInt(attributeModifier) != 0) {
                    response
                            .append(" normally or **").append(modifiedMatchingMFD).append("** ")
                            .append("with a step ").append(isModifierPositive ? "bonus " : "penalty ").append("of ").append(attributeModifier);
                }
                return event.createFollowup(InteractionFollowupCreateSpec.builder()
                        .content(response.toString())
                        .build());
            }
        }
    }

    private static String getParameterValue(ChatInputInteractionEvent event, String name) {
        return event
                .getOption(name)
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .orElseThrow(OptionNotFoundException::new);
    }

    private static String getModifiedMFD(int MFDIndex, int cellModifier, String attributeModifier) {
        String modifiedMatchingMFD;
        int finalIndex = MFDIndex / cellModifier + Integer.parseInt(attributeModifier);
        try {
            modifiedMatchingMFD = Modifiers.values()[finalIndex].getModifier();
        } catch (ArrayIndexOutOfBoundsException e) {
            if (finalIndex < 0) {
                modifiedMatchingMFD = "Above 2";
            } else {
                modifiedMatchingMFD = Modifiers.ONE_TENTH.getModifier();
            }
        }
        return modifiedMatchingMFD;
    }
}
