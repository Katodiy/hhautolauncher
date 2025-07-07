package core.hhrunner;

import json.JSONObject;

import java.io.*;
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
    public Automation automation;
    Process p = null;
    public long start;
    public long stop;
    File bot_config;

    public Task(long start, Automation automation) {
        this.automation = automation;
        this.name = automation.name;
        this.start = start;
        this.stop = start + ((long) automation.duration * 60 * 1000);p = null;
    }

    public boolean isWork(){
        return p!=null;
    }

    public void startProcess(String path) throws IOException {
        bot_config = File.createTempFile("bot_config-",".json");
        write(bot_config.getPath());
        if(Configuration.getInstance().isJava18)
        {
            String cmd = "\"" + Configuration.getInstance().javaPath.value + "\"" + " -jar -Xms4g -Xmx4g --add-exports java.base/java.lang=ALL-UNNAMED --add-exports java.desktop/sun.awt=ALL-UNNAMED --add-exports java.desktop/sun.java2d=ALL-UNNAMED " + "\"" + Configuration.getInstance().hafenPath.value + "\"" + " -bots " + "\"" +bot_config.getPath() + "\"";
            System.out.println(cmd);
            p = Runtime.getRuntime().exec(cmd);

        }
        else {
            String cmd = "\"" + Configuration.getInstance().javaPath.value + "\"" + " -jar " + "\"" + Configuration.getInstance().hafenPath.value + "\"" + " -bots " + "\"" + bot_config.getPath() + "\"";
            System.out.println(cmd);
            p = Runtime.getRuntime().exec(cmd);
        }
//
//        BufferedReader stdInput = new BufferedReader(new
//                InputStreamReader(p.getInputStream()));
//
//        BufferedReader stdError = new BufferedReader(new
//                InputStreamReader(p.getErrorStream()));
//
//        try {
//            int err = p.waitFor();
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
//        String s = null;
//        while ((s = stdInput.readLine()) != null) {
//            System.out.println(s);
//        }
//
//        while ((s = stdError.readLine()) != null) {
//
//            System.out.println(s);
//        }
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
        obj.put ( "user", automation.user );
        obj.put ( "password", automation.password );
        obj.put ( "character", automation.character );
        obj.put ( "scenarioId", automation.scenarioId );


        try (Writer writer = new OutputStreamWriter(new FileOutputStream(path), java.nio.charset.StandardCharsets.UTF_8)) {
            writer.write(obj.toJSONString());
        }
        catch ( IOException e ) {
            e.printStackTrace ();
        }
    }
}
