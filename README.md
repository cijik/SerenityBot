# SerenityBot

SerenityBot is a Discord bot for managing a TTRPG character database backed by Google Sheets and Redis.

## What it does

The bot currently supports:

- Adding, updating, retrieving, listing, and removing character sheets in the database
- Reading shadow spell descriptions from a linked sheet
- Rolling dice expressions with math
- Rolling d100 against SPECIAL values or skills on a character sheet
- Rigging the next roll as pass or fail for testing or table control
- Setting radiation and temperature values for tracked characters
- Refreshing cached character sheet data
- Showing command help and linking to project documentation
- Sending a message to a chosen channel

## Command Summary

### Character database

- `/add-character`
- `/get-character`
- `/get-all-characters`
- `/update-character`
- `/remove-character`

### Sheet interaction

- `/read-sheet`
- `/set-radiation`
- `/set-temperature`
- `/refresh-character-data`

### Rolling

- `/roll`
- `/r`
- `/roll-targeted`
- `/roll-untargeted`
- `/rig`
- `/unrig`

### Help and docs

- `/help`
- `/docs`

### Misc

- `/say`

## Requirements

- Java 21
- Maven
- A Discord bot token
- Google Sheets credentials for the service account
- Redis

## Local configuration

The project expects local secret files that are not committed to git:

- `src/main/resources/token.properties`
- `src/main/resources/credentials.json`

These are ignored in [.gitignore](.gitignore).

## Build and test

```bash
mvn test
```

```bash
mvn package
```

## Notes

- The application uses Spring Boot.
- GitHub Actions handles the Maven build and release workflow on `master`.
- Versioning is derived in CI from the latest semantic tag and the current commit message.
