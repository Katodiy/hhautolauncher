package core.hhrunner;

public class Scenario {
    public String name;
    public String user;
    public String password;
    public String character;
    public String bot;
    public boolean isStartTime = false;

    public long startTime = 0;
    public boolean disabled = false;
    public boolean isRepeted = false;
    public int duration = -1;
    public int interval = -1;
    public String nomad = "";
}
