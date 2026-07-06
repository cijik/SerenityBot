package com.ciji.serenity.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import com.ciji.serenity.enums.RigType;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;
import org.springframework.stereotype.Service;

import com.ciji.serenity.enums.Modifier;
import com.ciji.serenity.enums.Special;
import com.ciji.serenity.model.CharacterSheetDetails;
import com.ciji.serenity.model.SheetRow;
import com.ezylang.evalex.EvaluationException;
import com.ezylang.evalex.Expression;
import com.ezylang.evalex.data.EvaluationValue;
import com.ezylang.evalex.parser.ParseException;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
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

    private final RollRandomSource rollRandomSource;

    private RigType rigType = RigType.NONE;

    private boolean isCrit;

    private static final String DICE_REGEX = "\\d*d\\d+";

    private static final Set<String> SKILLS = Set.of("dig", "melee", "energy weapons",
            "explosives", "lockpicking", "big guns",
            "survival", "unarmed", "mercantile",
            "speechcraft", "magic", "medicine",
            "repair", "science", "flight", "small guns", "sneak");

    private record AttributeSelection(String attributeName, boolean special, int cellModifier) {
    }

    private record AttributeMatrix(List<String> headers, List<SheetRow> rows) {
    }

    @SneakyThrows
    public Mono<Message> rollTargeted(ChatInputInteractionEvent event) {
        String characterName = SheetsUtil.getParameterValue(event, "character-name");
        String targetMFD = SheetsUtil.getParameterValue(event, "with-target-mfd");

        return characterSheetService.getCharacterSheet(characterName, event.getInteraction().getUser().getId().asString())
                .flatMap(characterSheet -> {
                    String attributeName = SheetsUtil.getParameterValue(event, "rolls-for");
                    AttributeSelection attributeSelection = resolveAttributeSelection(attributeName);
                    if (attributeSelection == null) {
                        return event.createFollowup(attributeName + " is not a valid attribute");
                    }
                    CharacterSheetDetails sheetDetails = characterSheetDetailsService.getCharacterSheetDetails(characterSheet);
                    AttributeMatrix attributeMatrix = resolveAttributeMatrix(sheetDetails, attributeSelection.special());
                    String normalizedAttributeName = WordUtils.capitalize(attributeSelection.attributeName());
                    int requestedAttribute = getRequestedAttributeIndex(attributeMatrix.headers(), normalizedAttributeName, attributeSelection.special());
                    if (requestedAttribute < 0) {
                        return event.createFollowup("**" + characterName + "** does not have this attribute");
                    }
                    int requestedModifier;
                    try {
                        requestedModifier = Modifier.fromString(targetMFD).ordinal() * attributeSelection.cellModifier();
                    } catch (IllegalArgumentException e) {
                        log.error("Invalid target MFD: {}", targetMFD);
                        return event.createFollowup(targetMFD + " is not a valid target MFD. Please specify one from the following list: 2, 1 1/2, 1, 3/4, 1/2, 1/4, 1/10");
                    }

                    int attributeThreshold;
                    log.info("Parsing attribute threshold");
                    try {
                        attributeThreshold = Integer.parseInt(attributeMatrix.rows().get(requestedAttribute).getRow().get(requestedModifier));
                    } catch (IndexOutOfBoundsException e) {
                        log.error("Attribute threshold out of bounds");
                        return event.createFollowup("**" + characterName + "** does not have this attribute");
                    }
                    return createRollResultFollowup(event, characterName, normalizedAttributeName, targetMFD, attributeThreshold);
                })
                .switchIfEmpty(event.createFollowup("Character not found"));
    }

    @SneakyThrows
    public Mono<Message> rollUntargeted(ChatInputInteractionEvent event) {
        String characterName = SheetsUtil.getParameterValue(event, "character-name");
        String stepModifier = SheetsUtil.getParameterValue(event, "with-step-bonus");

        return characterSheetService.getCharacterSheet(characterName, event.getInteraction().getUser().getId().asString())
                .flatMap(characterSheet -> {
                    String attributeName = SheetsUtil.getParameterValue(event, "rolls-for");
                    AttributeSelection attributeSelection = resolveAttributeSelection(attributeName);
                    if (attributeSelection == null) {
                        return event.createFollowup(attributeName + " is not a valid attribute");
                    }
                    CharacterSheetDetails sheetDetails = characterSheetDetailsService.getCharacterSheetDetails(characterSheet);
                    String modifiableCharacterName = WordUtils.capitalize(characterName.toLowerCase(Locale.ROOT));
                    AttributeMatrix attributeMatrix = resolveAttributeMatrix(sheetDetails, attributeSelection.special());
                    String normalizedAttributeName = WordUtils.capitalize(attributeSelection.attributeName());
                    int requestedAttribute = getRequestedAttributeIndex(attributeMatrix.headers(), normalizedAttributeName, attributeSelection.special());
                    if (requestedAttribute < 0) {
                        return event.createFollowup("**" + modifiableCharacterName + "** does not have this attribute");
                    }

                    int roll;

                    if (!rigType.equals(RigType.NONE)) {
                        int lowerBound = rigType.equals(RigType.FAIL) ? isCrit ? 96 : Integer.parseInt(attributeMatrix.rows().get(requestedAttribute).getRow().reversed().get(1)) : 1;
                        int upperBound = rigType.equals(RigType.PASS) ? isCrit ? 6 : Integer.parseInt(attributeMatrix.rows().get(requestedAttribute).getRow().get(1)) : 101; //upper bound is exclusive
                        roll = rollRandomSource.nextInt(lowerBound, upperBound);
                    } else {
                        roll = rollRandomSource.nextInt(1, 101); //upper bound is exclusive
                    }
                    if (roll > 95) {
                        return event.createFollowup(modifiableCharacterName + " rolls a **" + roll + "** on " + normalizedAttributeName + ", **failing spectacularly**!");
                    }
                    if (roll < 6) {
                        return event.createFollowup(modifiableCharacterName + " rolls a **" + roll + "** on " + normalizedAttributeName + ", **succeeding critically**!");
                    }

                    Object currentMFD;
                    try {
                        currentMFD = attributeMatrix.rows().get(requestedAttribute).getRow().reversed().stream()
                                .filter(mfd -> !mfd.isEmpty() && Integer.parseInt(mfd) >= roll).findFirst().orElse(null);
                    } catch (IndexOutOfBoundsException e) {
                        log.error("Requested MFD value does not exist for attribute {}", normalizedAttributeName);
                        return event.createFollowup(normalizedAttributeName + " is not a valid attribute for specified attribute type");
                    }
                    if (currentMFD == null) {
                        log.error("No higher MFD threshold found for {}", roll);
                        return event.createFollowup("The roll of **" + roll + "** is above MFD 2 (**" + attributeMatrix.rows().get(requestedAttribute).getRow().getFirst() + "**) for " + normalizedAttributeName);
                    } else {
                        int MFDIndex = attributeMatrix.rows().get(requestedAttribute).getRow().indexOf(currentMFD);
                        String matchingMFD = Modifier.values()[MFDIndex / attributeSelection.cellModifier()].getModifier();
                        int matchingMFDValue = Integer.parseInt(attributeMatrix.rows().get(requestedAttribute).getRow().get(MFDIndex));
                        String modifiedMatchingMFD;
                        try {
                            modifiedMatchingMFD = getModifiedMFD(MFDIndex, attributeSelection.cellModifier(), stepModifier);
                        } catch (NumberFormatException e) {
                            log.error("{} is not a valid step modifier", stepModifier);
                            return event.createFollowup(stepModifier + " is not a valid step modifier");
                        }
                        boolean isModifierPositive = Integer.parseInt(stepModifier) >= 0;
                        StringBuilder response = new StringBuilder();
                        response
                                .append(modifiableCharacterName)
                                .append(" rolls **").append(roll)
                                .append("** for ").append(normalizedAttributeName)
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
                });
    }

    public Mono<Message> roll(ChatInputInteractionEvent event) {
        String roll = SheetsUtil.getParameterValue(event, "roll");

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
                int rollResult = rollRandomSource.nextInt(1, numberOfSides + 1); //upper bound is exclusive
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

    public Mono<Message> rig(ChatInputInteractionEvent event) {
        String type = SheetsUtil.getParameterValue(event, "type");
        String isCrit = SheetsUtil.getParameterValue(event, "is-crit");

        try {
            rigType = RigType.valueOf(type.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return event.createFollowup("Invalid type of rigging. Please use pass/fail.").withEphemeral(true);
        }

        this.isCrit = isCrit.toLowerCase(Locale.ROOT).equals("yes") || isCrit.toLowerCase(Locale.ROOT).equals("true");

        return event.createFollowup("All next rolls are rigged").withEphemeral(true);
    }

    public Mono<Message> unrig(ChatInputInteractionEvent event) {
        rigType = RigType.NONE;
        isCrit = false;

        return event.createFollowup("All next rolls are no longer rigged.").withEphemeral(true);
    }

    private static String getUser(ChatInputInteractionEvent event) {
        return event.getInteraction().getMember().orElseThrow().getMemberData().nick().get().orElse(
                event.getInteraction().getUser().getGlobalName().orElse(
                        event.getInteraction().getUser().getUsername()
                )
        );
    }

    private AttributeSelection resolveAttributeSelection(String attributeName) {
        String normalizedAttributeName = attributeName.toLowerCase(Locale.ROOT);
        Special special = Special.fromString(normalizedAttributeName);
        if (special != null) {
            return new AttributeSelection(special.name().toLowerCase(Locale.ROOT), true, 1);
        }
        if (SKILLS.contains(normalizedAttributeName)) {
            return new AttributeSelection(normalizedAttributeName, false, 2);
        }
        log.error("Invalid attribute: {}", attributeName);
        return null;
    }

    private AttributeMatrix resolveAttributeMatrix(CharacterSheetDetails sheetDetails, boolean special) {
        if (special) {
            return new AttributeMatrix(sheetDetails.getSpecialsMatrix().getHeaders(), sheetDetails.getSpecialsMatrix().getRows());
        }
        return new AttributeMatrix(sheetDetails.getSkillMatrix().getHeaders(), sheetDetails.getSkillMatrix().getRows());
    }

    private int getRequestedAttributeIndex(List<String> attributeNames, String attributeName, boolean special) {
        if (special) {
            try {
                Special.fromString(StringUtils.toRootUpperCase(attributeName));
            } catch (IllegalArgumentException e) {
                return -1;
            }
            return attributeNames.indexOf(StringUtils.truncate(attributeName, 1));
        }
        return attributeNames.indexOf(attributeName);
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
        final int lowerBound = rigType.equals(RigType.FAIL) ? isCrit ? 96 : skillThreshold : 1;
        final int upperBound = rigType.equals(RigType.PASS) ? isCrit ? 6 : skillThreshold : 101; //upper bound is exclusive
        final int roll = rollRandomSource.nextInt(lowerBound, upperBound);
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
