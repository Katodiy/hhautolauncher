# HH Auto Launcher - User Guide

**Version:** Latest  
**Date:** August 2025  
**Application:** Haven and Hearth Automation Launcher

---

## Table of Contents

1. [Introduction](#introduction)
2. [Initial Setup](#initial-setup)
3. [Understanding the Interface](#understanding-the-interface)
4. [Creating and Managing Schedules](#creating-and-managing-schedules)
5. [Setting Up Automation Tasks](#setting-up-automation-tasks)
6. [Running and Monitoring Tasks](#running-and-monitoring-tasks)
7. [Pause Control System](#pause-control-system)
8. [Timeline Visualization](#timeline-visualization)
9. [Advanced Features](#advanced-features)
10. [Troubleshooting](#troubleshooting)

---

## Introduction

HH Auto Launcher is a JavaFX desktop application designed to automatically launch the Nurgling client for "Haven and Hearth" game on a schedule. The application provides a comprehensive GUI for creating, managing, and executing automated tasks that can start the game client with specific configurations at predetermined times.

### Key Features
- **Multiple Named Schedules**: Organize different automation setups
- **Flexible Task Scheduling**: One-time or repeating tasks with custom intervals
- **Timeline Visualization**: See your automation schedule at a glance
- **Pause Control**: Individual task pause/resume with timed or indefinite options
- **User Conflict Prevention**: Automatic queuing for same-user tasks
- **Scenario Integration**: Load and use existing Nurgling scenarios

---

## Initial Setup

### Prerequisites
- Java 15+ installed on your system
- Haven and Hearth game client (Hafen JAR file)
- Nurgling scenarios (optional but recommended)

### First Launch Configuration

When you first launch HH Auto Launcher, you'll need to configure the basic settings:

1. **Open Configuration Dialog**
   - Go to `File` → `Configuration`
   
   **[SCREENSHOT NEEDED: Configuration dialog window]**

2. **Set Java Path**
   - Click the "..." button next to "Java path"
   - Navigate to your Java executable (usually `java.exe`)
   - Common locations:
     - `C:\Program Files\Java\jdk-xx\bin\java.exe`
     - `C:\Program Files\Java\jre-xx\bin\java.exe`

3. **Set Hafen Path**
   - Click the "..." button next to "Hafen path"
   - Navigate to your Haven and Hearth JAR file (e.g., `hafen.jar`)

4. **Java Version Selection**
   - Check "Java 18+ (Oracle)" if you're using Oracle Java 18 or newer
   - Leave unchecked for standard Java or older versions

5. **Save Configuration**
   - Click outside the dialog or close it to save settings

---

## Understanding the Interface

The main application window consists of two primary tabs:

### Configuration Tab

**[SCREENSHOT NEEDED: Main configuration tab overview]**

The Configuration tab contains all the tools for setting up and managing your automation:

#### Top Section - Schedule Management
- **Schedule Dropdown**: Select between different named schedules
- **Action Buttons**: New, Save All, Delete, Rename schedules

#### Middle Section - Task Configuration
- **Task Dropdown**: Select existing automation tasks within the current schedule
- **Automation Details**: Configure individual task settings
- **Available Scenarios**: List of scenarios loaded from Nurgling

#### Bottom Section - Running Tasks
- **Task Table**: Shows currently running or scheduled tasks
- **Pause Controls**: Individual task control buttons
- **Main Controls**: Run, Pause All/Resume All, Stop All buttons

### Timeline Tab

**[SCREENSHOT NEEDED: Timeline tab showing scheduled tasks]**

The Timeline tab provides a visual representation of your automation schedule:
- **15-minute intervals**: Precise scheduling visualization
- **Color-coded tasks**: Each task has a unique color
- **Overlap detection**: See potential conflicts at a glance
- **24-hour view**: Shows the next 24 hours of scheduled tasks

---

## Creating and Managing Schedules

Schedules are named collections of automation tasks that help you organize different setups (e.g., "Weekday Mining", "Weekend PvP", "Crafting Schedule").

### Creating a New Schedule

1. **Click "New" Button**
   - Located in the Schedule Actions section
   
   **[SCREENSHOT NEEDED: New schedule dialog]**

2. **Enter Schedule Details**
   - **Name**: Give your schedule a descriptive name
   - **Description**: Optional description of the schedule's purpose

3. **Create the Schedule**
   - Click "Create" to add the new schedule
   - The application will automatically switch to your new schedule

### Switching Between Schedules

**[SCREENSHOT NEEDED: Schedule dropdown with multiple schedules]**

- Use the **Schedule dropdown** to switch between different schedules
- Each schedule maintains its own set of automation tasks
- When you switch schedules, the task list updates automatically

### Managing Existing Schedules

#### Renaming a Schedule
1. Select the schedule you want to rename
2. Click **"Rename"** button
3. Update the name and/or description
4. Click **"Save"** to confirm changes

#### Deleting a Schedule
1. Select the schedule you want to delete
2. Click **"Delete"** button
3. Confirm the deletion (Note: You cannot delete the last remaining schedule)

---

## Setting Up Automation Tasks

Each automation task represents a single bot instance that will be launched with specific settings.

### Creating a New Task

**[SCREENSHOT NEEDED: Task configuration form filled out]**

1. **Set Basic Information**
   - **Automation name**: Unique identifier for this task
   - **User**: Game username (important for conflict prevention)
   - **Password**: Game password
   - **Character**: Character name to log in with

2. **Configure Timing**
   - **Duration (min)**: How long the task should run
   - **Repeat**: Check if this task should repeat automatically
   - **Time interval (min)**: How often to repeat (only if Repeat is checked)
   - **Start time (opt)**: Specific start time (optional)

3. **Select Scenario**
   - Choose from **Available scenarios** list on the right
   - Scenarios are loaded from your Nurgling configuration

   **[SCREENSHOT NEEDED: Scenario selection list]**

4. **Task Options**
   - **Disable**: Temporarily disable this task without deleting it

5. **Save the Task**
   - Click **"Add"** to create a new task
   - Or click **"Save"** to update an existing selected task

### Task Configuration Examples

#### Example 1: One-time Mining Task
- **Name**: "Morning Mining"
- **User**: "myusername"
- **Duration**: 120 (2 hours)
- **Repeat**: Unchecked
- **Start time**: 08:00 AM
- **Scenario**: "Mining Scenario (ID: 5)"

#### Example 2: Repeating Combat Task
- **Name**: "PvP Patrol"
- **User**: "myusername"
- **Duration**: 30 (30 minutes)
- **Repeat**: Checked
- **Time interval**: 180 (every 3 hours)
- **Scenario**: "Combat Scenario (ID: 3)"

### Editing Existing Tasks

1. **Select Task**: Use the Task dropdown to select an existing task
2. **Modify Settings**: Change any configuration options
3. **Save Changes**: Click "Save" to update the task

### Deleting Tasks

1. **Select Task**: Choose the task you want to delete from the dropdown
2. **Delete**: Click the "Delete" button
3. **Confirmation**: The task will be removed immediately

---

## Running and Monitoring Tasks

### Starting Your Automation

**[SCREENSHOT NEEDED: Bottom control buttons highlighted]**

1. **Prepare Your Schedule**
   - Ensure all tasks are configured correctly
   - Check the Timeline tab to verify scheduling

2. **Start Execution**
   - Click the **"Run"** button at the bottom right
   - The system will immediately begin processing your schedule

3. **Monitor Progress**
   - Watch the task table for real-time status updates
   - Running tasks will show "Active" status
   - Completed tasks are automatically removed and rescheduled if repeating

### Understanding Task Status

**[SCREENSHOT NEEDED: Task table with various statuses]**

The task table shows important information for each running task:

- **Name**: Task identifier
- **Start time**: When the task actually started
- **End time**: When the task is scheduled to finish
- **Status**: Current pause status (Active, Paused with time remaining, Paused indefinitely)
- **Pause Control**: Button to open individual task controls

### Automatic Conflict Resolution

The system automatically prevents username conflicts:

- **Same Username**: If two tasks use the same username, the second task waits for the first to finish
- **Different Usernames**: Tasks with different usernames run simultaneously without interference
- **Queue Visualization**: The Timeline tab shows adjusted start times for queued tasks

### Stopping Automation

- **Stop All**: Immediately stops all running tasks and ends the automation session
- **Individual Control**: Use the "Control" button for each task to pause/resume specific tasks

---

## Pause Control System

The pause control system allows you to temporarily halt individual tasks without stopping the entire automation.

### Individual Task Control

**[SCREENSHOT NEEDED: Pause control dialog window]**

1. **Open Control Dialog**
   - Click the **"Control"** button for any task in the table
   - A dialog will open showing task details and control options

2. **Dialog Information**
   - **Task name** and current status
   - **Current time**: Shows the current system time
   - **Next run time**: When this task will run next (for repeating tasks)
   - **Time until next run**: Countdown to the next execution

3. **Pause Options** (for active tasks)
   - **Minutes field**: Enter number of minutes to pause
   - **Pause indefinitely**: Check to pause without a time limit
   - **Pause Task**: Apply the pause settings

4. **Resume Options** (for paused tasks)
   - **Resume Task**: Immediately resume the paused task
   - **Cancel**: Close dialog without changes

### Bulk Pause Control

**[SCREENSHOT NEEDED: Pause All/Resume All button highlighted]**

- **Pause All**: Click to indefinitely pause all currently running tasks
  - Button text changes to "Resume All"
- **Resume All**: Click to resume all paused tasks
  - Button text changes back to "Pause All"

### Pause Use Cases

#### Temporary System Maintenance
1. Click "Pause All" to stop all automation
2. Perform your maintenance tasks
3. Click "Resume All" to continue automation

#### Individual Task Adjustment
1. Use "Control" button on specific task
2. Pause for specific duration or indefinitely
3. Resume when ready

---

## Timeline Visualization

The Timeline tab provides a powerful visual representation of your automation schedule.

**[SCREENSHOT NEEDED: Full timeline view with multiple colored tasks]**

### Understanding the Timeline

#### Time Scale
- **15-minute intervals**: Each row represents 15 minutes
- **Hour markers**: Bold time labels at hour boundaries (14:00, 15:00)
- **Half-hour markers**: Gray time labels at 30-minute marks (14:30, 15:30)
- **Grid lines**: Horizontal lines separate each hour

#### Task Representation
- **Colored blocks**: Each task appears as a colored rectangle
- **Unique colors**: Every task gets a distinct color for easy identification
- **Block size**: Height represents actual task duration
- **Task labels**: Show task name and time range (e.g., "Mining Task 14:30-15:00")

#### Overlap Visualization
- **Side-by-side lanes**: Overlapping tasks appear in separate horizontal lanes
- **Conflict resolution**: Shows how same-username tasks are automatically queued
- **Multiple users**: Different usernames can overlap without issues

### Reading the Timeline

#### Identifying Conflicts
**[SCREENSHOT NEEDED: Timeline showing overlapping tasks in different lanes]**

- Tasks in the same lane don't conflict
- Tasks in different lanes at the same time indicate simultaneous execution
- Delayed tasks show the automatic conflict resolution

#### Planning Optimization
- **Gap identification**: Spot periods with no automation
- **Resource planning**: See when multiple characters will be active
- **Schedule adjustment**: Visual feedback for schedule modifications

---

## Advanced Features

### Scenario Integration

**[SCREENSHOT NEEDED: Available scenarios list]**

The application automatically loads scenarios from your Haven and Hearth configuration:

- **Location**: `%APPDATA%/Haven and Hearth/scenarios.nurgling.json`
- **Auto-loading**: Scenarios appear in the "Available scenarios" list
- **Selection**: Click on a scenario to select it for the current task

### Configuration Persistence

All your settings are automatically saved:

- **Schedules**: Stored in `schedules.hhautolauncher.json`
- **Application config**: Stored in `hhrunner.config.json`
- **Location**: Haven and Hearth data directory

### Multiple Schedule Workflow

Example workflow for managing different activities:

1. **"Weekday Work"** schedule:
   - Morning mining: 7:00-11:00
   - Afternoon crafting: 13:00-17:00

2. **"Weekend PvP"** schedule:
   - Combat patrol: 10:00-12:00, 15:00-17:00, 20:00-22:00

3. **"Overnight AFK"** schedule:
   - Long mining session: 22:00-06:00

Switch between schedules based on your real-life schedule.

---

## Troubleshooting

### Common Issues

#### Tasks Don't Start
**Symptoms**: Tasks appear in the table but never begin running

**Solutions**:
1. **Check Configuration**:
   - Verify Java path is correct in File → Configuration
   - Verify Hafen path points to valid JAR file
   - Ensure Java 18+ checkbox matches your Java version

2. **Check Task Settings**:
   - Verify username and password are correct
   - Ensure selected scenario exists and is valid
   - Check that task isn't disabled

#### Application Won't Start
**Symptoms**: Application fails to launch or crashes immediately

**Solutions**:
1. **Java Version**: Ensure you have Java 15+ installed
2. **File Permissions**: Check that the application has write access to its directory
3. **Configuration Files**: Delete `hhrunner.config.json` to reset configuration

#### Scenarios Don't Load
**Symptoms**: "Available scenarios" list is empty

**Solutions**:
1. **File Location**: Ensure `scenarios.nurgling.json` exists in your Haven and Hearth data directory
2. **File Format**: Verify the scenarios file is valid JSON
3. **Permissions**: Check read permissions on the scenarios file

#### Timeline Shows Incorrect Information
**Symptoms**: Timeline doesn't match expected schedule

**Solutions**:
1. **Refresh**: Switch to Configuration tab and back to Timeline tab
2. **Save Schedule**: Click "Save All" to ensure all changes are persisted
3. **Check System Time**: Verify your system clock is correct

### Getting Help

If you continue to experience issues:

1. **Check Console Output**: Look for error messages in the application console
2. **Backup Data**: Make copies of your schedule files before troubleshooting
3. **Community Support**: Post in the Haven and Hearth community forums with:
   - Description of the issue
   - Steps to reproduce
   - Screenshots of error messages
   - Your operating system and Java version

---

## Best Practices

### Schedule Organization
- Use descriptive names for schedules and tasks
- Group related activities in the same schedule
- Keep backup copies of important schedules

### Security Considerations
- **Password Storage**: Passwords are stored in plain text in configuration files
- **File Security**: Ensure your computer is secure as login credentials are saved locally
- **Access Control**: Limit access to the application directory

### Performance Optimization
- **Task Duration**: Don't make tasks too short (minimum 5-10 minutes recommended)
- **Overlap Management**: Use the timeline to identify and resolve scheduling conflicts
- **System Resources**: Monitor system performance when running multiple simultaneous tasks

### Automation Ethics
- **Fair Play**: Ensure your automation complies with Haven and Hearth terms of service
- **Community Impact**: Consider the impact of automation on other players
- **Responsible Use**: Don't use automation to gain unfair advantages

---

## Appendix

### File Locations

**Windows**:
- Schedules: `%APPDATA%/Haven and Hearth/schedules.hhautolauncher.json`
- Configuration: `%APPDATA%/Haven and Hearth/hhrunner.config.json`
- Scenarios: `%APPDATA%/Haven and Hearth/scenarios.nurgling.json`

**Unix/Linux**:
- Schedules: `~/.haven/schedules.hhautolauncher.json`
- Configuration: `~/.haven/hhrunner.config.json`
- Scenarios: `~/.haven/scenarios.nurgling.json`

### Keyboard Shortcuts

Currently, the application primarily uses mouse interaction. Future versions may include keyboard shortcuts for common operations.

### Version History

- **Current Version**: Enhanced timeline visualization, pause control system, overlap prevention
- **Previous Versions**: Basic scheduling, single automation support

---

*This guide covers the essential features and usage of HH Auto Launcher. For additional support or feature requests, please contact the development team or community.*