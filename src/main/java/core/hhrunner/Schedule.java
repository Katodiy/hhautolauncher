package core.hhrunner;

import java.util.HashMap;

public class Schedule {
    public String name;
    public String description;
    public HashMap<String, Automation> automations;
    public long createdTime;
    public long lastModified;

    public Schedule() {
        this.automations = new HashMap<>();
        this.createdTime = System.currentTimeMillis();
        this.lastModified = this.createdTime;
    }

    public Schedule(String name, String description) {
        this();
        this.name = name;
        this.description = description;
    }

    public void updateModifiedTime() {
        this.lastModified = System.currentTimeMillis();
    }

    public void addAutomation(Automation automation) {
        automations.put(automation.name, automation);
        updateModifiedTime();
    }

    public void removeAutomation(String name) {
        automations.remove(name);
        updateModifiedTime();
    }

    public boolean hasAutomations() {
        return !automations.isEmpty();
    }

    public int getAutomationCount() {
        return automations.size();
    }
}