package com.ciji.serenity.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;
import org.springframework.stereotype.Service;

import com.ciji.serenity.enums.Modifier;
import com.ciji.serenity.enums.Special;
import com.ciji.serenity.exception.OptionNotFoundException;
import com.ciji.serenity.model.CharacterSheet;
import com.ciji.serenity.model.CharacterSheetDetails;
import com.ciji.serenity.model.SheetRow;
import com.ezylang.evalex.EvaluationException;
import com.ezylang.evalex.Expression;
import com.ezylang.evalex.data.EvaluationValue;
import com.ezylang.evalex.parser.ParseException;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.InteractionFollowupCreateSpec;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class RollProcessingService {

    private final CharacterSheetService characterSheetService;

    private final CharacterSheetDetailsService characterSheetDetailsService;

    private static final String DICE_REGEX = "\\d*d\\d+";

    private static final Set<String> SKILLS = Set.of("dig", "melee", "energy weapons",
            "explosives", "lockpicking", "big guns",
            "survival", "unarmed", "mercantile",
            "speechcraft", "magic", "medicine",
            "repair", "science", "flight", "small guns", "sneak");

    @SneakyThrows
    public Mono<Message> rollTargeted(ChatInputInteractionEvent event) {
        String characterName = getParameterValue(event, "character-name");
        String attributeName = getParameterValue(event, "rolls-for");
        String targetMFD = getParameterValue(event, "with-target-mfd");
        event.deferReply().withEphemeral(false);

        CharacterSheet characterSheet = characterSheetService.getCharacterSheet(characterName, event.getInteraction().getUser().getId().asString());
        if (characterSheet == null) {
            return event.createFollowup("Character not found");
        } else {
            RollParameters rollParameters = getRollParameters(attributeName);

            CharacterSheetDetails sheetDetails = characterSheetDetailsService.getCharacterSheetDetails(characterSheet);
            attributeName = WordUtils.capitalize(attributeName.toLowerCase(Locale.ROOT));

            AttributeParameters attributeParameters;
            try {
                attributeParameters = getAttributeParameters(rollParameters, sheetDetails, attributeName);
            } catch (IllegalArgumentException e) {
                return event.createFollowup("**" + characterName + "** does not have this attribute");
            }
            int requestedModifier;
            try {
                requestedModifier = Modifier.fromString(targetMFD).ordinal() * rollParameters.cellModifier();
            } catch (IllegalArgumentException e) {
                log.error("Invalid target MFD: {}", targetMFD);
                return event.createFollowup(targetMFD + " is not a valid target MFD. Please specify one from the following list: 2, 1 1/2, 1, 3/4, 1/2, 1/4, 1/10");
            }

            int attributeThreshold;
            log.info("Parsing attribute threshold");
            try {
                attributeThreshold = Integer.parseInt(attributeParameters.attributeValueMatrix().get(attributeParameters.requestedAttribute()).getRow().get(requestedModifier));
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
        event.deferReply().withEphemeral(false);

        CharacterSheet characterSheet = characterSheetService.getCharacterSheet(characterName, event.getInteraction().getUser().getId().asString());
        if (characterSheet == null) {
            return event.createFollowup("Character not found");
        } else {
            RollParameters rollParameters = null;
            try {
                rollParameters = getRollParameters(attributeName);
            } catch (IllegalArgumentException e) {
                return event.createFollowup(attributeName + " is not a valid attribute");
            }

            CharacterSheetDetails sheetDetails = characterSheetDetailsService.getCharacterSheetDetails(characterSheet);
            attributeName = WordUtils.capitalize(rollParameters.attributeName().toLowerCase(Locale.ROOT));
            characterName = WordUtils.capitalize(characterName.toLowerCase(Locale.ROOT));
            AttributeParameters attributeParameters;
            try {
                attributeParameters = getAttributeParameters(rollParameters, sheetDetails, attributeName);
            } catch (IllegalArgumentException e) {
                return event.createFollowup("**" + characterName + "** does not have this attribute");
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
                currentMFD = attributeParameters.attributeValueMatrix().get(attributeParameters.requestedAttribute()).getRow().reversed().stream()
                        .filter(mfd -> !mfd.isEmpty() && Integer.parseInt(mfd) >= roll).findFirst().orElse(null);
            } catch (IndexOutOfBoundsException e) {
                log.error("Requested MFD value does not exist for attribute {}", attributeName);
                return event.createFollowup(attributeName + " is not a valid attribute for specified attribute type");
            }
            if (currentMFD == null) {
                log.error("No higher MFD threshold found for {}", roll);
                return event.createFollowup("The roll of **" + roll + "** is above MFD 2 (**" + attributeParameters.attributeValueMatrix().get(attributeParameters.requestedAttribute()).getRow().getFirst() + "**) for " + attributeName);
            } else {
                int MFDIndex = attributeParameters.attributeValueMatrix().get(attributeParameters.requestedAttribute()).getRow().indexOf(currentMFD);
                String matchingMFD = Modifier.values()[MFDIndex / rollParameters.cellModifier()].getModifier();
                int matchingMFDValue = Integer.parseInt(attributeParameters.attributeValueMatrix().get(attributeParameters.requestedAttribute()).getRow().get(MFDIndex));
                String modifiedMatchingMFD;
                try {
                    modifiedMatchingMFD = getModifiedMFD(MFDIndex, rollParameters.cellModifier(), stepModifier);
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

    private static AttributeParameters getAttributeParameters(RollParameters rollParameters, CharacterSheetDetails sheetDetails, String attributeName) {
        List<String> attributeNames;
        List<SheetRow> attributeValueMatrix;
        if (rollParameters.isSpecial()) {
            attributeNames = sheetDetails.getSpecialsMatrix().getHeaders();
            attributeValueMatrix = sheetDetails.getSpecialsMatrix().getRows();
        } else {
            attributeNames = sheetDetails.getSkillMatrix().getHeaders();
            attributeValueMatrix = sheetDetails.getSkillMatrix().getRows();
        }
        int requestedAttribute;
        if (Special.fromString(attributeName.toLowerCase(Locale.ROOT)) != null) {
            Special.fromString(StringUtils.toRootUpperCase(attributeName));
            requestedAttribute = attributeNames.indexOf(StringUtils.truncate(attributeName, 1));
        } else {
            requestedAttribute = attributeNames.indexOf(attributeName);
        }
        return new AttributeParameters(attributeValueMatrix, requestedAttribute);
    }

    private record AttributeParameters(List<SheetRow> attributeValueMatrix, int requestedAttribute) {
    }

    private static RollParameters getRollParameters(String attributeName) {
        boolean isSpecial;
        int cellModifier;
        if (Special.fromString(attributeName.toLowerCase(Locale.ROOT)) != null) {
            attributeName = Special.fromString(attributeName.toLowerCase(Locale.ROOT)).name().toLowerCase(Locale.ROOT);
            isSpecial = true;
            cellModifier = 1;
        } else if (SKILLS.contains(attributeName.toLowerCase(Locale.ROOT))) {
            isSpecial = false;
            cellModifier = 2;
        } else {
            log.error("Invalid attribute: {}", attributeName);
            throw new IllegalArgumentException();
        }
        return new RollParameters(attributeName, isSpecial, cellModifier);
    }

    private record RollParameters(String attributeName, boolean isSpecial, int cellModifier) {
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

    private static String getParameterValue(ChatInputInteractionEvent event, String name) {
        return event
                .getOption(name)
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .orElseThrow(OptionNotFoundException::new);
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
            modifiedMatchingMFD = Modifier.values()[finalIndex].getModifier();
        } catch (ArrayIndexOutOfBoundsException e) {
            log.warn("Matching MFD is outside of existing thresholds");
            if (finalIndex < 0) {
                modifiedMatchingMFD = "Above 2";
            } else {
                modifiedMatchingMFD = Modifier.ONE_TENTH.getModifier();
            }
        }
        return modifiedMatchingMFD;
    }
}
