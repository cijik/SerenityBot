package com.ciji.serenity.enums;

import com.google.common.collect.ImmutableMap;
import lombok.Getter;
import java.util.Map;
import java.util.Collections;

@Getter
public enum Command {

    ADD_CHARACTER("add-character", "Adds a character sheet to the database",
    "Add a character to the database with a name and link to their Google Sheet. To make the sheet accessible for interactions, please add the bot (serenity-bot@serenitybot.iam.gserviceaccount.com) as an editor to the character sheet and make the sheet viewable to everyone with the link",
    ImmutableMap.of("name", "Name of the character to add",
           "url", "Link to the character's sheet")),

    GET_CHARACTER("get-character", "Retrieves a character's sheet", 
    "Get the link to the character sheet from the database by `name`",
    ImmutableMap.of("name", "Name of the character to retrieve as written when they were added (case insensitive)")),

    GET_ALL_CHARACTERS("get-all-characters", "Retrieves a list of all owned characters",
    "Get a list of all added characters that currently exist in the database",
    Collections.emptyMap()),

    UPDATE_CHARACTER("update-character", "Updates a character sheet in the database",
    "",
    ImmutableMap.of("name", "Name of the character to update",
           "owner-id", "User ID of the character's owner")),

    REMOVE_CHARACTER("remove-character", "Removes the character from the database",
    "Remove the specified previously added character from the database",
    ImmutableMap.of("name", "Name of the character to remove as written when they were added (case insensitive)")),

    READ_SHEET("read-sheet", "Read a shadow spell description",
    "(Subject to change, will fail if response is too large) Get a Shadow Spell's description from a sheet",
    ImmutableMap.of("name", "Name of the character sheet to read from",
           "value", "Name of the Shadow Spell")),

    ROLL_TARGETED("roll-targeted", "Rolls a character's attribute with MFD target",
    "Roll a d100 targeting a specific MFD of a SPECIAL or Skill of a specified character",
    ImmutableMap.of("character-name", "Name of the character sheet to read from as they written when they were added (case insensitive)",
           "rolls-for", "Name of the SPECIAL or Skill to roll from the sheet. Spaces are allowed",
           "with-target-mfd", "Target MFD to check against as one of the following values: 2, 1 1/2, 1, 3/4, 1/2, 1/4, 1/10")),

    ROLL_UNTARGETED("roll-untargeted", "Rolls a character's attribute",
    "Roll a d100 for a specific SPECIAL or Skill of a specified character and get the MFD threshold it passes, with any bonuses or maluses applied",
    ImmutableMap.of("character-name", "Name of the character sheet to read from as they written when they were added (case insensitive)",
           "rolls-for", "Name of the SPECIAL or Skill to roll from the sheet, written plainly. Spaces are allowed",
           "with-step-bonus", "Step bonus or penalty to apply to the roll. Bonus is positive, penalty is negative")),

    ROLL("roll", "Rolls one or several dice",
    "Roll dice as a math expression, e. g. `1d20 + 2d10 + 3`",
    ImmutableMap.of("roll", "The roll expression")),

    SHORT_ROLL("r", ROLL.getShortDesc(), ROLL.getFullDesc(), ROLL.getParamDescs()),

    HELP("help", "Displays full list of commands, or one command in detail",
    "Get a list of all commands and their short descriptions. Specify a `command` to get its full description as well as its parameters",
    ImmutableMap.of("command", "Specific command to get info about")),
    
    DOCS("docs", "Gives a link to the documentation",
    "Returns a link to the full documentation of the bot",
    Collections.emptyMap()),

    SAY("say", "Allows you to post in bot's name",
            "Speak in the bot's name in a specified channel",
            ImmutableMap.of("channel", "ID of the channel to post in",
                            "message", "Message to post"));

    private final String command;

    private final String shortDesc;

    private final String fullDesc;

    private final Map<String, String> paramDescs;

    Command(String command, String shortDesc, String fullDesc, Map<String, String> paramDescs) {
        this.command = command;
        this.shortDesc = shortDesc;
        this.fullDesc = fullDesc;
        this.paramDescs = paramDescs;
    }

    public static Command fromString(String value) {
        Command command = null;
        for (Command c : Command.values()) {
            if (c.command.equalsIgnoreCase(value)) {
                command = c;
                break;
            }
        }
        return command;
    }
}
