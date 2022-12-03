package core.hhrunner;

import json.JSONArray;
import json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Task {
    public final String getName() {
        return name;
    }

    public final String getStart(){
        Date currentDate = new Date(start);
        DateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        return df.format(currentDate);
    }

    public final String getStop(){
        Date currentDate = new Date(stop);
        DateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        return df.format(currentDate);
    }
    public String name;
    public Scenario scenario;
    Process p = null;
    public long start;
    public long stop;

    File bot_config;

    public Task(long start, Scenario scenario) {
        this.scenario = scenario;
        this.name = scenario.name;
        this.start = start;
        this.stop = start + ((long) scenario.duration * 60 * 1000);
        p = null;
    }

    public boolean isWork(){
        return p!=null;
    }

    public void startProcess(String path) throws IOException {
        bot_config = File.createTempFile("bot_config-",".json");
        write(bot_config.getPath());
        p = Runtime.getRuntime().exec("java -jar " + path + " -bots " + bot_config.getPath());
    }

    public void stopProcess() {
        if (p != null)
            p.destroy();
        if(bot_config.exists()){
            bot_config.delete();
        }
    }

    private void write(String path){

        JSONObject obj = new JSONObject ();
        obj.put ( "user", scenario.user );
        obj.put ( "password", scenario.password );
        obj.put ( "character", scenario.character );
        obj.put ( "bot", scenario.bot );
        obj.put ( "nomad", scenario.nomad );

        try ( FileWriter file = new FileWriter ( path ) ) {
            file.write ( obj.toJSONString () );
        }
        catch ( IOException e ) {
            e.printStackTrace ();
        }
    }
}
