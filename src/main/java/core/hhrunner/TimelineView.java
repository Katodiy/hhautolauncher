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
    private static final int INTERVAL_HEIGHT = 20; // Height for each 15-minute interval
    private static final int TASK_WIDTH = 200;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM dd");
    
    // Color palette for different tasks
    private static final String[] TASK_COLORS = {
        "#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4", "#FFEAA7", 
        "#DDA0DD", "#98D8C8", "#F7DC6F", "#BB8FCE", "#85C1E9",
        "#F8C471", "#82E0AA", "#F1948A", "#85CDDF", "#D7BDE2"
    };
    
    private Map<String, String> taskColorMap = new HashMap<>();
    
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
        taskColorMap.clear(); // Reset color assignments
        
        if (automations.isEmpty()) {
            Label emptyLabel = new Label("No tasks scheduled");
            emptyLabel.setStyle("-fx-text-fill: gray; -fx-font-style: italic;");
            timelineContent.getChildren().add(emptyLabel);
            return;
        }
        
        // Assign colors to tasks
        assignTaskColors(automations);
        
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
    
    private void assignTaskColors(HashMap<String, Automation> automations) {
        int colorIndex = 0;
        for (String taskName : automations.keySet()) {
            if (!automations.get(taskName).disabled) {
                taskColorMap.put(taskName, TASK_COLORS[colorIndex % TASK_COLORS.length]);
                colorIndex++;
            }
        }
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
        // Create a canvas-like approach with absolute positioning
        VBox timeGrid = new VBox();
        
        // Calculate total height needed
        long totalMinutes = java.time.Duration.between(start, end).toMinutes();
        double totalHeight = (totalMinutes / 15.0) * INTERVAL_HEIGHT;
        
        // Create the time scale on the left
        VBox timeScale = new VBox();
        timeScale.setPrefWidth(60);
        
        // Create the task area with absolute positioning
        Pane taskArea = new Pane();
        taskArea.setPrefHeight(totalHeight);
        taskArea.setPrefWidth(600);
        taskArea.setStyle("-fx-border-color: lightgray; -fx-border-width: 1;");
        
        // Draw time labels and grid lines
        LocalDateTime current = start.withMinute(start.getMinute() / 15 * 15).withSecond(0);
        double yPos = 0;
        
        while (current.isBefore(end)) {
            // Time labels
            if (current.getMinute() == 0) {
                Label timeLabel = new Label(current.format(TIME_FORMAT));
                timeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: black; -fx-font-weight: bold;");
                timeLabel.setLayoutY(yPos);
                timeScale.getChildren().add(timeLabel);
                
                // Hour separator line
                javafx.scene.shape.Line line = new javafx.scene.shape.Line();
                line.setStartX(0);
                line.setEndX(600);
                line.setStartY(yPos);
                line.setEndY(yPos);
                line.setStroke(Color.LIGHTGRAY);
                taskArea.getChildren().add(line);
            } else if (current.getMinute() == 30) {
                Label timeLabel = new Label(current.format(TIME_FORMAT));
                timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: gray;");
                timeLabel.setLayoutY(yPos);
                timeScale.getChildren().add(timeLabel);
            } else {
                // Add spacer for 15 and 45 minute marks
                Label spacer = new Label("");
                spacer.setPrefHeight(INTERVAL_HEIGHT);
                timeScale.getChildren().add(spacer);
            }
            
            current = current.plusMinutes(15);
            yPos += INTERVAL_HEIGHT;
        }
        
        // Draw complete task blocks with proper lane assignment
        double taskLaneWidth = 150;
        int maxLanes = 4;
        
        // Assign lanes to tasks using proper overlap detection
        Map<TaskRun, Integer> taskLanes = assignTaskLanes(taskRuns, maxLanes);
        
        for (TaskRun run : taskRuns) {
            // Calculate task position and size
            double taskStartY = calculateYPosition(run.startTime, start);
            double taskHeight = java.time.Duration.between(run.startTime, run.endTime).toMinutes() / 15.0 * INTERVAL_HEIGHT;
            
            // Get assigned lane
            int lane = taskLanes.getOrDefault(run, 0);
            double taskX = lane * (taskLaneWidth + 10);
            
            // Create complete task rectangle
            Rectangle taskRect = new Rectangle();
            taskRect.setWidth(taskLaneWidth);
            taskRect.setHeight(Math.max(taskHeight, INTERVAL_HEIGHT));
            taskRect.setLayoutX(taskX);
            taskRect.setLayoutY(taskStartY);
            
            // Use assigned task color
            String taskColor = taskColorMap.get(run.taskName);
            if (taskColor != null) {
                taskRect.setFill(Color.web(taskColor));
            } else {
                taskRect.setFill(Color.web("#4CAF50"));
            }
            taskRect.setArcWidth(5);
            taskRect.setArcHeight(5);
            taskRect.setStroke(Color.WHITE);
            taskRect.setStrokeWidth(1);
            
            // Create task label
            Label taskLabel = new Label(run.taskName + "\n" + 
                run.startTime.format(TIME_FORMAT) + " - " + run.endTime.format(TIME_FORMAT));
            taskLabel.setLayoutX(taskX + 5);
            taskLabel.setLayoutY(taskStartY + 5);
            taskLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: white; -fx-font-weight: bold;");
            taskLabel.setMaxWidth(taskLaneWidth - 10);
            taskLabel.setWrapText(true);
            
            taskArea.getChildren().addAll(taskRect, taskLabel);
        }
        
        // Combine time scale and task area
        HBox timelineLayout = new HBox();
        timelineLayout.getChildren().addAll(timeScale, taskArea);
        timelineContent.getChildren().add(timelineLayout);
    }
    
    private double calculateYPosition(LocalDateTime taskTime, LocalDateTime timelineStart) {
        long minutesFromStart = java.time.Duration.between(timelineStart, taskTime).toMinutes();
        return (minutesFromStart / 15.0) * INTERVAL_HEIGHT;
    }
    
    private Map<TaskRun, Integer> assignTaskLanes(List<TaskRun> taskRuns, int maxLanes) {
        Map<TaskRun, Integer> taskLanes = new HashMap<>();
        
        // Sort tasks by start time for better lane assignment
        List<TaskRun> sortedTasks = new ArrayList<>(taskRuns);
        sortedTasks.sort(Comparator.comparing(task -> task.startTime));
        
        // Keep track of when each lane becomes free
        LocalDateTime[] laneEndTimes = new LocalDateTime[maxLanes];
        for (int i = 0; i < maxLanes; i++) {
            laneEndTimes[i] = LocalDateTime.MIN;
        }
        
        for (TaskRun task : sortedTasks) {
            // Find the first available lane
            int assignedLane = 0;
            for (int lane = 0; lane < maxLanes; lane++) {
                // A lane is available if its last task ended before this task starts
                if (laneEndTimes[lane].isBefore(task.startTime) || laneEndTimes[lane].equals(task.startTime)) {
                    assignedLane = lane;
                    break;
                }
            }
            
            // Assign the task to the lane and update the lane's end time
            taskLanes.put(task, assignedLane);
            laneEndTimes[assignedLane] = task.endTime;
        }
        
        return taskLanes;
    }
    
    private boolean tasksOverlap(TaskRun task1, TaskRun task2) {
        return task1.startTime.isBefore(task2.endTime) && task2.startTime.isBefore(task1.endTime);
    }
    
    private Rectangle createTaskRectangle(TaskRun run, LocalDateTime intervalStart, LocalDateTime intervalEnd) {
        Rectangle rect = new Rectangle();
        rect.setHeight(INTERVAL_HEIGHT - 4);
        
        // Calculate width based on overlap with this 15-minute interval
        long overlapMinutes = run.getOverlapMinutes(intervalStart, intervalEnd);
        double widthRatio = overlapMinutes / 15.0; // 15-minute intervals
        rect.setWidth(Math.max(60, TASK_WIDTH * widthRatio));
        
        // Use assigned task color
        String taskColor = taskColorMap.get(run.taskName);
        if (taskColor != null) {
            rect.setFill(Color.web(taskColor));
        } else {
            // Fallback colors based on task type
            if (run.isRepeating) {
                rect.setFill(Color.web("#4CAF50")); // Green for repeating tasks
            } else {
                rect.setFill(Color.web("#2196F3")); // Blue for one-time tasks
            }
        }
        
        rect.setArcWidth(3);
        rect.setArcHeight(3);
        
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
        
        boolean overlapsInterval(LocalDateTime intervalStart, LocalDateTime intervalEnd) {
            return startTime.isBefore(intervalEnd) && endTime.isAfter(intervalStart);
        }
        
        // Keep the old method for backward compatibility
        boolean overlapsHour(LocalDateTime hourStart, LocalDateTime hourEnd) {
            return overlapsInterval(hourStart, hourEnd);
        }
        
        long getOverlapMinutes(LocalDateTime hourStart, LocalDateTime hourEnd) {
            LocalDateTime overlapStart = startTime.isAfter(hourStart) ? startTime : hourStart;
            LocalDateTime overlapEnd = endTime.isBefore(hourEnd) ? endTime : hourEnd;
            return java.time.Duration.between(overlapStart, overlapEnd).toMinutes();
        }
    }
}