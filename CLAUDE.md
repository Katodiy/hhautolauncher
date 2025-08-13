# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

HH Auto Launcher is a JavaFX desktop application that automatically launches the Nurgling client for "Haven and Hearth" game on a schedule. The application provides a GUI for creating, managing, and executing automated tasks that can start the game client with specific configurations at predetermined times.

## Build System and Commands

This is a Gradle-based Java project with JavaFX dependencies.

### Essential Commands
- `./gradlew build` - Build the project
- `./gradlew run` - Run the application
- `./gradlew test` - Run unit tests
- `./gradlew jlink` - Create a native Java application bundle
- `./gradlew jlinkZip` - Create a distributable zip of the application

### Development Requirements
- Java 15+ (project targets Java 15)
- JavaFX 15.0.1 (automatically handled by Gradle plugin)
- The application supports both standard Java and Oracle Java 18+ execution modes

## Architecture Overview

### Core Classes Structure
- **`Main`**: Entry point that delegates to `CoreApplication`
- **`CoreApplication`**: Main JavaFX application class containing the entire GUI and application logic
- **`Schedule`**: Data model for named collections of automations with metadata (created time, last modified)
- **`Automation`**: Data model for automated tasks with scheduling configuration
- **`Task`**: Runtime execution wrapper for automations that manages process lifecycle
- **`Scenario`**: Data model for game scenarios loaded from external configuration
- **`Configuration`**: Singleton for managing application settings (Java path, Hafen path, Java version)
- **`DateTimePicker`**: Custom JavaFX control for date/time input

### Key Application Flow
1. Application loads schedules from `schedules.hhautolauncher.json` in app data directory
2. User selects a schedule and creates/edits automation tasks within that schedule
3. User can create, switch between, rename, and delete multiple named schedules
4. When "Run" is pressed, a `Worker` thread is started for the current schedule's automations
5. Worker creates `Task` instances from current schedule's automations and manages their execution
6. Tasks launch external Java processes (Hafen client) with generated bot configuration files
7. Tasks are automatically stopped after their configured duration
8. Repeatable tasks are rescheduled based on interval settings

### Schedule Management Features
- **Multiple Named Schedules**: Users can create and manage multiple schedules (e.g., "Weekday Mining", "Weekend PvP")
- **Schedule Persistence**: All schedules are automatically saved to app data directory
- **Migration Support**: Existing `automations.json` files are automatically migrated to a "Default Schedule"
- **Schedule Operations**: Create new schedules, switch between them, rename, and delete via GUI
- **Cross-Platform Storage**: Uses Windows APPDATA or Unix home directory following Haven and Hearth conventions
- **Smart Start Time Handling**: Past start times are automatically adjusted to prevent rapid catch-up execution when restarting schedules
- **Individual Task Pause Control**: Users can pause specific tasks for a custom duration (in minutes) while other tasks continue running

### Data Persistence
- **`schedules.hhautolauncher.json`**: Stores named schedules containing automation collections in app data directory
- **`hhrunner.config.json`**: Stores application configuration (paths, Java version)
- **`scenarios.nurgling.json`**: External scenario definitions loaded from Haven and Hearth data directory
- **`automations.json.backup`**: Backup of old automation file (created during migration)

### External Integration
- Loads scenarios from Haven and Hearth's APPDATA directory (`%APPDATA%/Haven and Hearth/scenarios.nurgling.json`)
- Launches external Hafen JAR files with dynamically generated bot configuration
- Creates temporary JSON config files for each bot instance

### JSON Parsing
The project includes a custom JSON library in the `json` package (not using external dependencies like Jackson or Gson) for parsing configuration files and creating bot configurations.

## Configuration Notes

### Java Execution Modes
- Standard Java: Basic `-jar` execution
- Oracle Java 18+: Includes additional JVM arguments for module system compatibility:
  - `--add-exports java.base/java.lang=ALL-UNNAMED`
  - `--add-exports java.desktop/sun.awt=ALL-UNNAMED` 
  - `--add-exports java.desktop/sun.java2d=ALL-UNNAMED`

### File Paths
- Configuration paths are stored relative to the application JAR location
- Scenario files are resolved from system-specific Haven and Hearth directories
- Bot configuration files are created as temporary files during task execution

### GUI Considerations
- Single-window JavaFX application with embedded table for task monitoring
- Real-time task status updates displayed in TableView with pause/resume controls
- Individual task pause functionality with custom duration input (defaults to 30 minutes)
- Live countdown display showing remaining pause time for each task
- Modal configuration dialog for setting Java and Hafen paths
- File choosers for selecting executables and data files

## Recent Development Context

The current branch (`feat/cyrillic-characters`) suggests work on supporting Cyrillic character encoding, likely related to international username/character name support in the automation system. There are recent commits addressing null pointer fixes and bot-to-scenario migration.