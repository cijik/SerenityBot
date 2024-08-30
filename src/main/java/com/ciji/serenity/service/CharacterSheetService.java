package com.ciji.serenity.service;

import com.ciji.serenity.dao.CharacterSheetDao;
import com.ciji.serenity.enums.Modifiers;
import com.ciji.serenity.enums.Specials;
import com.ciji.serenity.exception.OptionNotFoundException;
import com.ciji.serenity.model.CharacterSheet;
import com.ezylang.evalex.EvaluationException;
import com.ezylang.evalex.Expression;
import com.ezylang.evalex.data.EvaluationValue;
import com.ezylang.evalex.parser.ParseException;
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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CharacterSheetService {

    private final CharacterSheetDao characterSheetDao;

    private static final String DICE_REGEX = "\\d*d\\d+";

    private static final Set<String> SPECIALS = Set.of("strength", "perception", "endurance", "charisma", "intelligence", "agility", "luck");
    private static final Set<String> SKILLS = Set.of("dig", "melee", "energy weapons",
            "explosives", "lockpicking", "big guns",
            "survival", "unarmed", "mercantile",
            "speechcraft", "magic", "medicine",
            "repair", "science", "flight", "small guns", "sneak");

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

    @SneakyThrows
    public Mono<Message> rollTargeted(ChatInputInteractionEvent event) {
        String characterName = getParameterValue(event, "character-name");
        String attributeName = getParameterValue(event, "rolls-for");
        String targetMFD = getParameterValue(event, "with-target-mfd");
        event.deferReply().withEphemeral(false).block();

        CharacterSheet characterSheet = getCharacterSheet(characterName, event.getInteraction().getUser().getId().asString());
        if (characterSheet == null) {
            return event.createFollowup("Character not found");
        } else {
            List<String> ranges;
            int cellModifier;
            if (SPECIALS.contains(attributeName.toLowerCase(Locale.ROOT))) {
                ranges = List.of("'Sheet'!AN27:AO40", "'Sheet'!AP27:AV40");
                cellModifier = 1;
            } else if (SKILLS.contains(attributeName.toLowerCase(Locale.ROOT))) {
                ranges = List.of("'Sheet'!BF7:BJ40", "'Sheet'!BW7:CJ40");
                cellModifier = 2;
            } else {
                log.error("Invalid attribute: {}", attributeName);
                return event.createFollowup(attributeName + " is not a valid attribute");
            }

            BatchGetValuesResponse readResult;
            try {
                readResult = getSpreadsheetMatrix(characterSheet, ranges);
            } catch (GoogleJsonResponseException e) {
                log.error("Cannot access character sheet");
                return event.createFollowup("Cannot access character sheet. Please add the bot (serenity-bot@serenitybot.iam.gserviceaccount.com) as an editor to the character sheet and/or make the sheet viewable to everyone with the link.");
            }
            attributeName = WordUtils.capitalize(attributeName.toLowerCase(Locale.ROOT));
            ValueRange attributeNames = readResult.getValueRanges().getFirst();
            ValueRange attributeValueMatrix = readResult.getValueRanges().get(1);
            int requestedAttribute;
            if (SPECIALS.contains(attributeName.toLowerCase(Locale.ROOT))) {
                try {
                    Specials.valueOf(StringUtils.toRootUpperCase(attributeName));
                } catch (IllegalArgumentException e) {
                    return event.createFollowup("**" + characterName + "** does not have this attribute");
                }
                requestedAttribute = attributeNames.getValues().indexOf(List.of(StringUtils.truncate(attributeName, 1)));
            } else {
                requestedAttribute = attributeNames.getValues().indexOf(List.of(attributeName));
            }
            int requestedModifier;
            try {
                requestedModifier = Modifiers.fromString(targetMFD).ordinal() * cellModifier;
            } catch (IllegalArgumentException e) {
                log.error("Invalid target MFD: {}", targetMFD);
                return event.createFollowup(targetMFD + " is not a valid target MFD. Please specify one from the following list: 2, 1 1/2, 1, 3/4, 1/2, 1/4, 1/10");
            }

            int attributeThreshold;
            log.info("Parsing attribute threshold");
            try {
                attributeThreshold = Integer.parseInt((String) attributeValueMatrix.getValues().get(requestedAttribute).get(requestedModifier));
            } catch (IndexOutOfBoundsException e) {
                log.error("Attribute threshold out of bounds");
                return event.createFollowup("**" + characterName + "** does not have this attribute");
            }
            return createRollResultFollowup(event, characterName, attributeName, targetMFD, attributeThreshold);
        }
    }

    @SneakyThrows
    public Mono<Message> rollUntargeted(ChatInputInteractionEvent event) {
        String characterName = getParameterValue(event, "character-name");
        String attributeName = getParameterValue(event, "rolls-for");
        String stepModifier = getParameterValue(event, "with-step-bonus");
        event.deferReply().withEphemeral(false).block();

        CharacterSheet characterSheet = getCharacterSheet(characterName, event.getInteraction().getUser().getId().asString());
        if (characterSheet == null) {
            return event.createFollowup("Character not found");
        } else {
            List<String> ranges;
            int cellModifier;
            if (SPECIALS.contains(attributeName.toLowerCase(Locale.ROOT))) {
                ranges = List.of("'Sheet'!AN27:AO40", "'Sheet'!AP27:AV40");
                cellModifier = 1;
            } else if (SKILLS.contains(attributeName.toLowerCase(Locale.ROOT))) {
                ranges = List.of("'Sheet'!BF7:BJ40", "'Sheet'!BW7:CJ40");
                cellModifier = 2;
            } else {
                log.error("Invalid attribute: {}", attributeName);
                return event.createFollowup(attributeName + " is not a valid attribute");
            }

            BatchGetValuesResponse readResult;
            try {
                readResult = getSpreadsheetMatrix(characterSheet, ranges);
            } catch (GoogleJsonResponseException e) {
                log.error("Cannot access character sheet");
                return event.createFollowup("Cannot access character sheet. Please add the bot (serenity-bot@serenitybot.iam.gserviceaccount.com) as an editor to the character sheet and/or make the sheet viewable to everyone with the link.");
            }
            attributeName = WordUtils.capitalize(attributeName.toLowerCase(Locale.ROOT));
            characterName = WordUtils.capitalize(characterName.toLowerCase(Locale.ROOT));
            ValueRange attributeNames = readResult.getValueRanges().getFirst();
            ValueRange attributeValueMatrix = readResult.getValueRanges().get(1);
            int requestedAttribute;
            if (SPECIALS.contains(attributeName.toLowerCase(Locale.ROOT))) {
                try {
                    Specials.valueOf(StringUtils.toRootUpperCase(attributeName));
                } catch (IllegalArgumentException e) {
                    return event.createFollowup("**" + characterName + "** does not have this attribute");
                }
                requestedAttribute = attributeNames.getValues().indexOf(List.of(StringUtils.truncate(attributeName, 1)));
            } else {
                requestedAttribute = attributeNames.getValues().indexOf(List.of(attributeName));
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
                log.error("Requested MFD value does not exist for attribute {}", attributeName);
                return event.createFollowup(attributeName + " is not a valid attribute for specified attribute type");
            }
            if (currentMFD == null) {
                log.error("No higher MFD threshold found for {}", roll);
                return event.createFollowup("The roll of **" + roll + "** is above MFD 2 (**" + attributeValueMatrix.getValues().get(requestedAttribute).getFirst() + "**) for " + attributeName);
            } else {
                int MFDIndex = attributeValueMatrix.getValues().get(requestedAttribute).indexOf(currentMFD);
                String matchingMFD = Modifiers.values()[MFDIndex / cellModifier].getModifier();
                int matchingMFDValue = Integer.parseInt((String) attributeValueMatrix.getValues().get(requestedAttribute).get(MFDIndex));
                String modifiedMatchingMFD;
                try {
                    modifiedMatchingMFD = getModifiedMFD(MFDIndex, cellModifier, stepModifier);
                } catch (NumberFormatException e) {
                    log.error("{} is not a valid step modifier", stepModifier);
                    return event.createFollowup(stepModifier + " is not a valid step modifier");
                }
                boolean isModifierPositive = Integer.parseInt(stepModifier) >= 0;
                StringBuilder response = new StringBuilder();
                response
                        .append(characterName)
                        .append(" rolls **").append(roll)
                        .append("** for ").append(attributeName)
                        .append(" succeeding MFD **").append(matchingMFD).append("** (**").append(matchingMFDValue).append("**)");
                if (Integer.parseInt(stepModifier) != 0) {
                    response
                            .append(" normally or **").append(modifiedMatchingMFD).append("** ")
                            .append("with a step ").append(isModifierPositive ? "bonus " : "penalty ").append("of ").append(stepModifier);
                }
                return event.createFollowup(InteractionFollowupCreateSpec.builder()
                        .content(response.toString())
                        .build());
            }
        }
    }

    public Mono<Message> roll(ChatInputInteractionEvent event) {
        String roll = getParameterValue(event, "roll");
        event.deferReply().withEphemeral(false).block();
        String comment = "";
        String user = getUser(event);
        boolean rollContainsComment = roll.contains("#");
        if (rollContainsComment) {
            comment = roll.split("#")[1];
        }
        String originalRoll = roll = roll.split("#")[0];

        Pattern dicePattern = Pattern.compile(DICE_REGEX);
        List<String> dice = new ArrayList<>();
        dicePattern.matcher(roll).results().map(MatchResult::group).forEach(dice::add);

        List<BigDecimal> performedRolls = new ArrayList<>();
        List<List<Integer>> individualPerformedRolls = new ArrayList<>();
        dice.forEach(die -> {
            List<Integer> dieRolls = new ArrayList<>();
            int numberOfDice;
            try {
                numberOfDice = Integer.parseInt(die.substring(0, die.indexOf("d")));
            } catch (NumberFormatException e) {
                numberOfDice = 1;
            }
            int numberOfSides = Integer.parseInt(die.substring(die.indexOf("d")+1));
            BigDecimal finalSum = BigDecimal.ZERO;
            for (int i = 0; i < numberOfDice; i++) {
                int rollResult = new Random().nextInt(numberOfSides) + 1;
                dieRolls.add(rollResult);
                finalSum = finalSum.add(BigDecimal.valueOf(rollResult));
            }
            performedRolls.add(finalSum);
            individualPerformedRolls.add(dieRolls);
        });

        roll = StringUtils.replaceEach(roll, dice.toArray(String[]::new), performedRolls.stream().map(String::valueOf).toArray(String[]::new));

        log.info("Processing expression '{}'", roll);
        if (roll.matches(".*[a-z].*")) {
            log.error("Expression '{}' contains invalid operands", roll);
            return event.createFollowup("Invalid roll expression");
        }
        Expression expression = new Expression(roll);
        EvaluationValue finalResult;

        try {
            finalResult = expression.evaluate();
        } catch (EvaluationException | ParseException e) {
            log.error("Expression '{}' could not be processed as a number", roll);
            return event.createFollowup("Invalid roll expression");
        }

        StringBuilder response = new StringBuilder();
        response.append("**").append(user).append("** rolls ");
        if (finalResult.getNumberValue().compareTo(BigDecimal.ZERO) < 0) {
            response.append("**0** (").append(finalResult.getNumberValue().setScale(2, RoundingMode.DOWN).stripTrailingZeros().toPlainString()).append(")");
            return createResponse(event, comment, rollContainsComment, originalRoll, dice, individualPerformedRolls, response);
        }

        response.append("**").append(finalResult.getNumberValue().setScale(2, RoundingMode.DOWN).stripTrailingZeros().toPlainString()).append("**");
        return createResponse(event, comment, rollContainsComment, originalRoll, dice, individualPerformedRolls, response);
    }

    private static String getUser(ChatInputInteractionEvent event) {
        return event.getInteraction().getMember().orElseThrow().getMemberData().nick().get().orElse(
                event.getInteraction().getUser().getGlobalName().orElse(
                        event.getInteraction().getUser().getUsername()
                )
        );
    }

    private Mono<Message> createResponse(ChatInputInteractionEvent event, String comment, boolean rollContainsComment, String originalRoll, List<String> dice, List<List<Integer>> individualPerformedRolls, StringBuilder response) {
        if (rollContainsComment) {
            response.append(" with comment: '").append(comment.strip()).append("'");
        }
        appendIndividualRolls(response, individualPerformedRolls, dice, originalRoll);
        if (response.length() > 2000) {
            return event.createFollowup("Resulting message too long, please split your rolls into smaller ones.");
        }
        return event.createFollowup(InteractionFollowupCreateSpec.builder()
                .content(response.toString())
                .build());
    }

    private static void appendIndividualRolls(StringBuilder response, List<List<Integer>> individualPerformedRolls, List<String> dice, String originalRoll) {
        response.append("\n").append("```").append("\n").append("Roll details: ").append(originalRoll);
        AtomicInteger index = new AtomicInteger(0);
        individualPerformedRolls.forEach(listOfRolls -> {
            response.append("\n").append(dice.get(index.getAndIncrement())).append(": [ ");
            listOfRolls.forEach(recordedRoll -> response.append(recordedRoll).append(" "));
            response.append("]");
        });
        response.append("```");
    }

    private static String getParameterValue(ChatInputInteractionEvent event, String name) {
        return event
                .getOption(name)
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .orElseThrow(OptionNotFoundException::new);
    }

    private CharacterSheet getCharacterSheet(String characterName, String ownerId) {
        log.info("Retrieving character {} for user {}", characterName, ownerId);
        return characterSheetDao.findByNameAndOwnerId(WordUtils.capitalize(characterName.toLowerCase(Locale.ROOT)), ownerId);
    }

    private static InteractionFollowupCreateMono createMissingCharacterFollowup(ChatInputInteractionEvent event, String characterName) {
        log.error("Character {} not found for user {}", characterName, event.getInteraction().getUser().getId().asString());
        return event.createFollowup("Character not found");
    }

    private static BatchGetValuesResponse getSpreadsheetMatrix(CharacterSheet characterSheet, List<String> ranges) throws IOException, GeneralSecurityException {
        log.info("Retrieving sheet range matrix");
        BatchGetValuesResponse readResult = SheetsServiceUtil.getSheetsService().spreadsheets().values()
                .batchGet(characterSheet.getId())
                .setRanges(ranges)
                .execute();
        return readResult;
    }

    private Mono<Message> createRollResultFollowup(ChatInputInteractionEvent event, String characterName, String skillName, String skillModifier, int skillThreshold) {
        int roll = new Random().nextInt(100);
        String result = roll <= skillThreshold ? "Success!" : "Failure!";

        return event.createFollowup(InteractionFollowupCreateSpec.builder()
                .content(WordUtils.capitalize(characterName.toLowerCase(Locale.ROOT)) + " rolls **" + roll + "** for " + skillName + " with target MFD **" + skillModifier + "** [**" + skillThreshold + "**], " + result)
                .build());
    }

    private static String getModifiedMFD(int MFDIndex, int cellModifier, String attributeModifier) {
        String modifiedMatchingMFD;
        int finalIndex = MFDIndex / cellModifier + Integer.parseInt(attributeModifier);
        try {
            modifiedMatchingMFD = Modifiers.values()[finalIndex].getModifier();
        } catch (ArrayIndexOutOfBoundsException e) {
            log.warn("Matching MFD is outside of existing thresholds");
            if (finalIndex < 0) {
                modifiedMatchingMFD = "Above 2";
            } else {
                modifiedMatchingMFD = Modifiers.ONE_TENTH.getModifier();
            }
        }
        return modifiedMatchingMFD;
    }
}
