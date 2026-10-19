package com.ciji.serenity.service;

import com.ciji.serenity.model.CharacterSheetDetails;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private static final int DISCORD_MESSAGE_LIMIT = 2000;

    // Labels for row columns 1..5 (column 0 is the item name)
    private static final List<String> COLUMN_LABELS = List.of("Weight", "Amount", "Value", "Total weight", "Total value");

    private final CharacterSheetService characterSheetService;

    private final CharacterSheetDetailsService characterSheetDetailsService;

    public Mono<Message> getCharacterInventory(ChatInputInteractionEvent event) {
        String characterName = SheetsUtil.getParameterValue(event, "character-name");

        return characterSheetService.getCharacterSheet(characterName, event.getInteraction().getUser().getId().asString())
                .flatMap(characterSheet -> {
                    CharacterSheetDetails characterSheetDetails = characterSheetDetailsService.getCharacterSheetDetails(characterSheet);
                    String header = "**" + escapeMarkdown(characterSheetDetails.getName()) + "'s Inventory:**\n";

                    if (characterSheetDetails.getInventoryMatrix() == null
                            || characterSheetDetails.getInventoryMatrix().getRows().isEmpty()) {
                        return event.createFollowup(header + "*Empty*");
                    }

                    List<String> lines = characterSheetDetails.getInventoryMatrix().getRows().stream()
                            .map(row -> formatRow(row.getRow()))
                            .filter(line -> !line.isEmpty())
                            .toList();

                    return Flux.fromIterable(splitIntoMessages(header, lines))
                            .concatMap(event::createFollowup) // sequential, keeps the order of messages
                            .last();
                });
    }

    /**
     * Formats a row as: **Item** (Weight: x, Amount: y, ...)
     * Blank columns are left out.
     */
    private static String formatRow(List<?> row) {
        if (row == null || row.isEmpty() || row.getFirst() == null || row.getFirst().toString().isBlank()) {
            return "";
        }
        String item = "**" + escapeMarkdown(row.getFirst().toString().trim()) + "**";

        String details = IntStream.range(0, COLUMN_LABELS.size())
                .filter(i -> i + 1 < row.size() && row.get(i + 1) != null && !row.get(i + 1).toString().isBlank())
                .mapToObj(i -> COLUMN_LABELS.get(i) + ": " + row.get(i + 1).toString().trim())
                .collect(Collectors.joining(", "));

        return details.isEmpty() ? item : item + " (" + details + ")";
    }

    /**
     * Splits the lines into chunks that each fit within Discord's 2000-character limit.
     */
    private static List<String> splitIntoMessages(String header, List<String> lines) {
        List<String> messages = new ArrayList<>();
        StringBuilder current = new StringBuilder(header);

        for (String line : lines) {
            if (current.length() + line.length() + 1 > DISCORD_MESSAGE_LIMIT) {
                messages.add(current.toString());
                current = new StringBuilder();
            }
            current.append(line).append('\n');
        }
        if (!current.isEmpty()) {
            messages.add(current.toString());
        }
        return messages;
    }

    private static String escapeMarkdown(String text) {
        return text.replaceAll("([*_~`|>\\\\])", "\\\\$1");
    }
}
