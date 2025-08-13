package core.hhrunner;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class TimelineView extends VBox {
    private static final int HOUR_HEIGHT = 30;
    private static final int TASK_WIDTH = 200;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM dd");
    
    private VBox timelineContent;
    private ScrollPane scrollPane;
    
    public TimelineView() {
        this.setSpacing(5);
        this.setPadding(new Insets(10));
        
        Label titleLabel = new Label("Schedule Timeline (Next 24 Hours)");
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        timelineContent = new VBox();
        scrollPane = new ScrollPane(timelineContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(300);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        
        this.getChildren().addAll(titleLabel, scrollPane);
    }
    
    public void updateTimeline(HashMap<String, Automation> automations) {
        timelineContent.getChildren().clear();
        
        if (automations.isEmpty()) {
            Label emptyLabel = new Label("No tasks scheduled");
            emptyLabel.setStyle("-fx-text-fill: gray; -fx-font-style: italic;");
            timelineContent.getChildren().add(emptyLabel);
            return;
        }
        
        // Generate timeline for next 24 hours
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTime = now.plusHours(24);
        
        // Collect all task runs in the next 24 hours
        List<TaskRun> taskRuns = new ArrayList<>();
        
        for (Automation auto : automations.values()) {
            if (auto.disabled) continue;
            
            List<TaskRun> runs = calculateTaskRuns(auto, now, endTime);
            taskRuns.addAll(runs);
        }
        
        // Sort by start time
        taskRuns.sort(Comparator.comparing(tr -> tr.startTime));
        
        // Create timeline visualization
        createTimelineGrid(taskRuns, now, endTime);
    }
    
    private List<TaskRun> calculateTaskRuns(Automation auto, LocalDateTime start, LocalDateTime end) {
        List<TaskRun> runs = new ArrayList<>();
        
        LocalDateTime currentTime = start;
        LocalDateTime taskStart;
        
        if (auto.isStartTime) {
            // Convert milliseconds to LocalDateTime
            taskStart = LocalDateTime.ofEpochSecond(auto.startTime / 1000, 0, 
                java.time.ZoneOffset.systemDefault().getRules().getOffset(java.time.Instant.now()));
            
            // If start time is in the past and task repeats, calculate next occurrence
            if (taskStart.isBefore(currentTime) && auto.isRepeted) {
                long intervalMinutes = auto.interval;
                long minutesPassed = java.time.Duration.between(taskStart, currentTime).toMinutes();
                long intervalsSkipped = minutesPassed / intervalMinutes;
                taskStart = taskStart.plusMinutes((intervalsSkipped + 1) * intervalMinutes);
            }
        } else {
            taskStart = currentTime;
        }
        
        // Generate runs within the time window
        while (taskStart.isBefore(end)) {
            LocalDateTime taskEnd = taskStart.plusMinutes(auto.duration);
            runs.add(new TaskRun(auto.name, taskStart, taskEnd, auto.isRepeted));
            
            if (!auto.isRepeted) break;
            taskStart = taskStart.plusMinutes(auto.interval);
        }
        
        return runs;
    }
    
    private void createTimelineGrid(List<TaskRun> taskRuns, LocalDateTime start, LocalDateTime end) {
        // Create hour grid
        LocalDateTime current = start.withMinute(0).withSecond(0);
        
        while (current.isBefore(end) || current.equals(end)) {
            HBox hourRow = new HBox();
            hourRow.setSpacing(10);
            hourRow.setAlignment(Pos.CENTER_LEFT);
            hourRow.setPrefHeight(HOUR_HEIGHT);
            
            // Time label
            Label timeLabel = new Label(current.format(TIME_FORMAT));
            timeLabel.setPrefWidth(50);
            timeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: gray;");
            
            // Task area
            HBox taskArea = new HBox();
            taskArea.setSpacing(5);
            taskArea.setPrefHeight(HOUR_HEIGHT - 5);
            
            // Find tasks that run in this hour
            LocalDateTime nextHour = current.plusHours(1);
            for (TaskRun run : taskRuns) {
                if (run.overlapsHour(current, nextHour)) {
                    Rectangle taskRect = createTaskRectangle(run, current, nextHour);
                    Label taskLabel = new Label(run.taskName);
                    taskLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: white; -fx-font-weight: bold;");
                    
                    StackPane taskPane = new StackPane();
                    taskPane.getChildren().addAll(taskRect, taskLabel);
                    taskPane.setMaxWidth(TASK_WIDTH);
                    
                    taskArea.getChildren().add(taskPane);
                }
            }
            
            hourRow.getChildren().addAll(timeLabel, taskArea);
            
            // Add separator line
            if (current.getHour() % 4 == 0) {
                hourRow.setStyle("-fx-border-color: lightgray; -fx-border-width: 0 0 1 0;");
            }
            
            timelineContent.getChildren().add(hourRow);
            current = current.plusHours(1);
        }
    }
    
    private Rectangle createTaskRectangle(TaskRun run, LocalDateTime hourStart, LocalDateTime hourEnd) {
        Rectangle rect = new Rectangle();
        rect.setHeight(HOUR_HEIGHT - 10);
        
        // Calculate width based on overlap with this hour
        long overlapMinutes = run.getOverlapMinutes(hourStart, hourEnd);
        double widthRatio = overlapMinutes / 60.0;
        rect.setWidth(Math.max(80, TASK_WIDTH * widthRatio));
        
        // Color based on task type
        if (run.isRepeating) {
            rect.setFill(Color.web("#4CAF50")); // Green for repeating tasks
        } else {
            rect.setFill(Color.web("#2196F3")); // Blue for one-time tasks
        }
        
        rect.setArcWidth(5);
        rect.setArcHeight(5);
        
        return rect;
    }
    
    // Helper class to represent a task run
    private static class TaskRun {
        final String taskName;
        final LocalDateTime startTime;
        final LocalDateTime endTime;
        final boolean isRepeating;
        
        TaskRun(String taskName, LocalDateTime startTime, LocalDateTime endTime, boolean isRepeating) {
            this.taskName = taskName;
            this.startTime = startTime;
            this.endTime = endTime;
            this.isRepeating = isRepeating;
        }
        
        boolean overlapsHour(LocalDateTime hourStart, LocalDateTime hourEnd) {
            return startTime.isBefore(hourEnd) && endTime.isAfter(hourStart);
        }
        
        long getOverlapMinutes(LocalDateTime hourStart, LocalDateTime hourEnd) {
            LocalDateTime overlapStart = startTime.isAfter(hourStart) ? startTime : hourStart;
            LocalDateTime overlapEnd = endTime.isBefore(hourEnd) ? endTime : hourEnd;
            return java.time.Duration.between(overlapStart, overlapEnd).toMinutes();
        }
    }
}