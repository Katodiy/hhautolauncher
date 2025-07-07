package core.hhrunner;

import javafx.collections.FXCollections;
import json.JSONArray;
import json.JSONObject;
import json.parser.JSONParser;
import json.parser.ParseException;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class Configuration {
    public static Configuration getInstance(){

        if(configuration== null)
        {
            configuration = new Configuration();
            configuration.read();
        }
        return configuration;
    }

    public static class Path{
        public String value;
    }
    private static Configuration configuration;

    boolean isJava18 = false;
    Path javaPath = new Path();
    Path hafenPath = new Path();

    public Configuration() {

    }

    public void write() {
        JSONObject obj = new JSONObject();
        obj.put("hafenPath", hafenPath.value);
        obj.put("javaPath", javaPath.value);
        obj.put("isJava18", isJava18);
        URL url = Configuration.class.getProtectionDomain().getCodeSource().getLocation();
        if (url != null) {
            try {
                String path = url.toURI().getPath().substring(0, url.toURI().getPath().lastIndexOf("/")) + "/hhrunner.config.json";
                try (Writer writer = new OutputStreamWriter(new FileOutputStream(path), StandardCharsets.UTF_8)) {
                    writer.write(obj.toJSONString());
                }
            } catch (IOException | URISyntaxException e) {
                e.printStackTrace();
            }
        }
    }


    private void read (  ) {
        try {
            URL url = Configuration.class.getProtectionDomain().getCodeSource().getLocation();
            String path = url.toURI().getPath().substring(0, url.toURI().getPath().lastIndexOf("/")) + "/hhrunner.config.json";
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8));
            JSONParser parser = new JSONParser();
            JSONObject jsonObject = (JSONObject) parser.parse(reader);

            if(jsonObject.get("javaPath")!=null)
                javaPath.value = jsonObject.get("javaPath").toString();
            if(jsonObject.get("hafenPath")!=null)
                hafenPath.value = jsonObject.get("hafenPath").toString();
            if(jsonObject.get("isJava18")!=null)
            isJava18 = (boolean) jsonObject.get("isJava18");
        }
        catch ( IOException e ) {
            System.out.println("No config file");
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
