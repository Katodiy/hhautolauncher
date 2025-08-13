package core.hhrunner;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import json.JSONArray;
import json.JSONObject;
import json.parser.JSONParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

public class CoreApplication extends Application {
    private static final String AUTOMATIONS_FILE = "automations.json";

    TextField title;
    TextField user;
    TextField password;
    TextField character;
    TextField nomadPath;

    TextField range;

    TextField duration;

    CheckBox disable;
    CheckBox isRepeted;
    CheckBox isStartTime;

    DateTimePicker dataPicker;

    Button add;
    Button save;
    Button delete;

    Button nomadSelect;
    Button startb;
    Button stopb;

    ListView<String> list;

    TableView<Task> table;

    void clear(){
        title.setText("");
        user.setText("");
        password.setText("");
        character.setText("");
        range.setText("0");
        duration.setText("0");
        disable.setSelected(false);
        isRepeted.setSelected(false);
        isStartTime.setSelected(false);
        dataPicker.setTime(0);
        nomadPath.setText("");
        refreshAutomationComboBox();
    }

    Process p;

    HashMap<String,Automation> automations = new HashMap<>();
    HashMap<Integer, Scenario> loadedScenarios = new HashMap<>();
    HashMap<String, Schedule> schedules = new HashMap<>();
    String currentScheduleName = "Default Schedule";
    
    ListView<String> scenarioPicker;
    ComboBox<String> automationComboBox;
    ComboBox<String> scheduleComboBox;
    Button newScheduleButton;
    Button saveScheduleButton;
    Button deleteScheduleButton;
    Button renameScheduleButton;
    TimelineView timelineView;

    public AtomicBoolean isWork = new AtomicBoolean(false);
    Thread th = null;

    class Worker implements Runnable{
        public Worker(String path) {
            this.path = path;
            long time = System.currentTimeMillis();
            for(String key : automations.keySet()){
                Automation auto = automations.get(key);
                long startTime = time; // Default to current time
                
                if (auto.isStartTime) {
                    // If the specified start time is in the future, use it
                    if (auto.startTime > time || !auto.isRepeted) {
                        startTime = auto.startTime;
                    } else if (auto.isRepeted) {
                        // If start time is in the past and task repeats, calculate next occurrence
                        long intervalMs = (long) auto.interval * 60 * 1000;
                        long timeSinceStart = time - auto.startTime;
                        long missedIntervals = timeSinceStart / intervalMs;
                        startTime = auto.startTime + (missedIntervals + 1) * intervalMs;
                    }
                    // If start time is in the past and task doesn't repeat, use current time
                }
                
                tasks.add(new Task(startTime, auto));
            }
            table.setItems( FXCollections.observableArrayList(tasks));
            isWork.set(true);
        }

        ArrayList<Task> tasks = new ArrayList<>();
        String path = "bin/hafen.jar";

        @Override
        public void run() {
            while (isWork.get()){
                long time = System.currentTimeMillis();
                ArrayList<Task> forDelete = new ArrayList<>();
                ArrayList<Task> forAdding = new ArrayList<>();
                for(Task task: tasks){
                    if(!task.isWork() && task.start<=time && !task.isPaused()){
                        try {
                            task.startProcess(Configuration.getInstance().hafenPath.value);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }else if (task.isWork() && task.stop<=time){
                        task.stopProcess();
                        forDelete.add(task);
                        if(task.automation.isRepeted) {
                            // Calculate next start time from current time + interval to avoid past scheduling
                            long nextStartTime = time + (long) task.automation.interval * 60 * 1000;
                            forAdding.add(new Task(nextStartTime, task.automation));
                        }
                    }
                }
                for(Task task: forDelete){
                    tasks.remove(task);
                }
                tasks.addAll(forAdding);
                if(!forDelete.isEmpty() || !forAdding.isEmpty()){
                    table.setItems( FXCollections.observableArrayList(tasks));
                } else {
                    // Refresh table periodically to update pause status display
                    table.refresh();
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            for(Task task: tasks){
                if(task.isWork())
                {
                    task.stopProcess();
                }
            }
            tasks.clear();
            table.setItems( FXCollections.observableArrayList(tasks));
        }
    }

    public void write (String path) {

        JSONObject obj = new JSONObject ();
        JSONArray jAutomations = new JSONArray ();

        for (String key : automations.keySet()) {
            Automation auto = automations.get(key);
            JSONObject objAuto = new JSONObject();
            objAuto.put("name", auto.name);
            objAuto.put("user", auto.user);
            objAuto.put("password", auto.password);
            objAuto.put("character", auto.character);
            objAuto.put("scenarioId", auto.scenarioId); // references loaded scenario
            objAuto.put("isStartTime", auto.isStartTime);
            objAuto.put("startTime", auto.startTime);
            objAuto.put("disabled", auto.disabled);
            objAuto.put("isRepeted", auto.isRepeted);
            objAuto.put("duration", auto.duration);
            objAuto.put("interval", auto.interval);
            jAutomations.add(objAuto);
        }
        obj.put("automations", jAutomations);


        try (Writer writer = new OutputStreamWriter(new FileOutputStream(path), java.nio.charset.StandardCharsets.UTF_8)) {
            writer.write(obj.toJSONString());
        }
        catch ( IOException e ) {
            e.printStackTrace ();
        }

    }


    public void read ( String path ) {
        File file = new File(path);
        if (!file.exists()) {
            // No automations saved yet; nothing to load.
            return;
        }

        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader( new FileInputStream( path ), StandardCharsets.UTF_8 ) );
            JSONParser parser = new JSONParser();
            JSONObject jsonObject = ( JSONObject ) parser.parse ( reader );

            JSONArray msg = (JSONArray) jsonObject.get("automations");
            if(msg!=null) {
                Iterator<JSONObject> iterator = msg.iterator();
                while (iterator.hasNext()) {
                    JSONObject item = iterator.next();
                    Automation auto = new Automation();
                    auto.name = item.get("name").toString();
                    auto.user = item.get("user").toString();
                    auto.password = item.get("password").toString();
                    auto.character = item.get("character").toString();
                    auto.scenarioId = ((Number) item.get("scenarioId")).intValue();
                    auto.isStartTime = (boolean)item.get("isStartTime");
                    if(item.get("startTime") != null) auto.startTime = (long)item.get("startTime");
                    auto.disabled = (boolean)item.get("disabled");
                    auto.isRepeted = (boolean)item.get("isRepeted");
                    auto.duration = ((Number)item.get("duration")).intValue();
                    auto.interval = ((Number)item.get("interval")).intValue();
                    automations.put(auto.name, auto);
                }

            }


        }
        catch ( IOException | json.parser.ParseException e ) {
            e.printStackTrace ();
        }
    }

    public void saveSchedules() {
        updateCurrentSchedule();
        String path = getScheduleFilePath();
        
        JSONObject obj = new JSONObject();
        JSONArray jSchedules = new JSONArray();
        
        for (String key : schedules.keySet()) {
            Schedule schedule = schedules.get(key);
            JSONObject objSchedule = new JSONObject();
            objSchedule.put("name", schedule.name);
            objSchedule.put("description", schedule.description);
            objSchedule.put("createdTime", schedule.createdTime);
            objSchedule.put("lastModified", schedule.lastModified);
            
            JSONArray jAutomations = new JSONArray();
            for (String autoKey : schedule.automations.keySet()) {
                Automation auto = schedule.automations.get(autoKey);
                JSONObject objAuto = new JSONObject();
                objAuto.put("name", auto.name);
                objAuto.put("user", auto.user);
                objAuto.put("password", auto.password);
                objAuto.put("character", auto.character);
                objAuto.put("scenarioId", auto.scenarioId);
                objAuto.put("isStartTime", auto.isStartTime);
                objAuto.put("startTime", auto.startTime);
                objAuto.put("disabled", auto.disabled);
                objAuto.put("isRepeted", auto.isRepeted);
                objAuto.put("duration", auto.duration);
                objAuto.put("interval", auto.interval);
                jAutomations.add(objAuto);
            }
            objSchedule.put("automations", jAutomations);
            jSchedules.add(objSchedule);
        }
        
        obj.put("schedules", jSchedules);
        obj.put("activeSchedule", currentScheduleName);
        
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(path), StandardCharsets.UTF_8)) {
            writer.write(obj.toJSONString());
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadSchedules() {
        String path = getScheduleFilePath();
        File file = new File(path);
        
        if (!file.exists()) {
            migrateFromOldAutomations();
            return;
        }

        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8));
            JSONParser parser = new JSONParser();
            JSONObject jsonObject = (JSONObject) parser.parse(reader);

            schedules.clear();
            JSONArray jSchedules = (JSONArray) jsonObject.get("schedules");
            if (jSchedules != null) {
                Iterator<JSONObject> iterator = jSchedules.iterator();
                while (iterator.hasNext()) {
                    JSONObject item = iterator.next();
                    Schedule schedule = new Schedule();
                    schedule.name = item.get("name").toString();
                    schedule.description = item.get("description") != null ? item.get("description").toString() : "";
                    schedule.createdTime = item.get("createdTime") != null ? (long)item.get("createdTime") : System.currentTimeMillis();
                    schedule.lastModified = item.get("lastModified") != null ? (long)item.get("lastModified") : System.currentTimeMillis();
                    
                    JSONArray jAutomations = (JSONArray) item.get("automations");
                    if (jAutomations != null) {
                        Iterator<JSONObject> autoIterator = jAutomations.iterator();
                        while (autoIterator.hasNext()) {
                            JSONObject autoItem = autoIterator.next();
                            Automation auto = new Automation();
                            auto.name = autoItem.get("name").toString();
                            auto.user = autoItem.get("user").toString();
                            auto.password = autoItem.get("password").toString();
                            auto.character = autoItem.get("character").toString();
                            auto.scenarioId = ((Number) autoItem.get("scenarioId")).intValue();
                            auto.isStartTime = (boolean)autoItem.get("isStartTime");
                            if(autoItem.get("startTime") != null) auto.startTime = (long)autoItem.get("startTime");
                            auto.disabled = (boolean)autoItem.get("disabled");
                            auto.isRepeted = (boolean)autoItem.get("isRepeted");
                            auto.duration = ((Number)autoItem.get("duration")).intValue();
                            auto.interval = ((Number)autoItem.get("interval")).intValue();
                            schedule.automations.put(auto.name, auto);
                        }
                    }
                    schedules.put(schedule.name, schedule);
                }
            }
            
            if (jsonObject.get("activeSchedule") != null) {
                currentScheduleName = jsonObject.get("activeSchedule").toString();
            }
            
            if (schedules.isEmpty()) {
                createDefaultSchedule();
            }
            
            if (!schedules.containsKey(currentScheduleName)) {
                currentScheduleName = schedules.keySet().iterator().next();
            }
            
            loadCurrentSchedule();
            
        } catch (IOException | json.parser.ParseException e) {
            e.printStackTrace();
            createDefaultSchedule();
        }
    }

    private void migrateFromOldAutomations() {
        File oldFile = new File(AUTOMATIONS_FILE);
        if (oldFile.exists()) {
            read(AUTOMATIONS_FILE);
            Schedule defaultSchedule = new Schedule("Default Schedule", "Migrated from automations.json");
            defaultSchedule.automations.putAll(automations);
            schedules.put(defaultSchedule.name, defaultSchedule);
            currentScheduleName = defaultSchedule.name;
            saveSchedules();
            
            File backupFile = new File(AUTOMATIONS_FILE + ".backup");
            oldFile.renameTo(backupFile);
        } else {
            createDefaultSchedule();
        }
    }

    private void createDefaultSchedule() {
        Schedule defaultSchedule = new Schedule("Default Schedule", "Default automation schedule");
        schedules.put(defaultSchedule.name, defaultSchedule);
        currentScheduleName = defaultSchedule.name;
        automations.clear();
    }

    private void updateCurrentSchedule() {
        if (schedules.containsKey(currentScheduleName)) {
            Schedule current = schedules.get(currentScheduleName);
            current.automations.clear();
            current.automations.putAll(automations);
            current.updateModifiedTime();
        }
    }

    private void loadCurrentSchedule() {
        if (schedules.containsKey(currentScheduleName)) {
            Schedule current = schedules.get(currentScheduleName);
            automations.clear();
            automations.putAll(current.automations);
        }
    }

    private Automation build() {
        Automation auto = new Automation();
        auto.name = title.getText();
        auto.user = user.getText();
        auto.password = password.getText();
        auto.character = character.getText();
        String selectedScenario = scenarioPicker.getSelectionModel().getSelectedItem();
        if (selectedScenario != null && selectedScenario.contains("ID:")) {
            auto.scenarioId = Integer.parseInt(selectedScenario.replaceAll("[^0-9]", ""));
        }
        auto.isStartTime = isStartTime.isSelected();
        auto.startTime = dataPicker.getTime();
        auto.disabled = disable.isSelected();
        auto.isRepeted = isRepeted.isSelected();
        auto.duration = Integer.parseInt(duration.getText());
        auto.interval = Integer.parseInt(range.getText());
        return auto;
    }

    public void add(){
        Automation res = build();
        automations.put(res.name,res);
        automationComboBox.setItems(FXCollections.observableArrayList(automations.keySet()));
        automationComboBox.getSelectionModel().select(res.name);
        show(res.name);
        saveSchedules();
        refreshTimeline();
    }

    public void save(){
        Automation res = build();
        automations.put(res.name,res);
        automationComboBox.setItems(FXCollections.observableArrayList(automations.keySet()));
        automationComboBox.getSelectionModel().select(res.name);
        show(res.name);
        saveSchedules();
        refreshTimeline();
    }

    public void createNewSchedule(String name, String description) {
        if (schedules.containsKey(name)) {
            return; // Schedule already exists
        }
        
        updateCurrentSchedule();
        Schedule newSchedule = new Schedule(name, description);
        schedules.put(name, newSchedule);
        currentScheduleName = name;
        loadCurrentSchedule();
        refreshScheduleComboBox();
        refreshAutomationComboBox();
        clear();
        saveSchedules();
    }

    public void switchToSchedule(String scheduleName) {
        if (!schedules.containsKey(scheduleName)) {
            return;
        }
        
        updateCurrentSchedule();
        currentScheduleName = scheduleName;
        loadCurrentSchedule();
        refreshAutomationComboBox();
        
        if (!automations.isEmpty()) {
            automationComboBox.getSelectionModel().selectFirst();
            show(automationComboBox.getSelectionModel().getSelectedItem());
        } else {
            clear();
        }
        saveSchedules();
        refreshTimeline();
    }

    public void deleteSchedule(String scheduleName) {
        if (schedules.size() <= 1) {
            return; // Cannot delete the last schedule
        }
        
        schedules.remove(scheduleName);
        
        if (currentScheduleName.equals(scheduleName)) {
            currentScheduleName = schedules.keySet().iterator().next();
            loadCurrentSchedule();
            refreshAutomationComboBox();
            if (!automations.isEmpty()) {
                automationComboBox.getSelectionModel().selectFirst();
                show(automationComboBox.getSelectionModel().getSelectedItem());
            } else {
                clear();
            }
        }
        
        refreshScheduleComboBox();
        saveSchedules();
    }

    public void renameSchedule(String oldName, String newName, String newDescription) {
        if (!schedules.containsKey(oldName) || schedules.containsKey(newName)) {
            return;
        }
        
        Schedule schedule = schedules.get(oldName);
        schedule.name = newName;
        schedule.description = newDescription;
        schedule.updateModifiedTime();
        
        schedules.remove(oldName);
        schedules.put(newName, schedule);
        
        if (currentScheduleName.equals(oldName)) {
            currentScheduleName = newName;
        }
        
        refreshScheduleComboBox();
        saveSchedules();
    }

    private void refreshScheduleComboBox() {
        if (scheduleComboBox != null) {
            scheduleComboBox.setItems(FXCollections.observableArrayList(schedules.keySet()));
            scheduleComboBox.getSelectionModel().select(currentScheduleName);
        }
    }

    private void refreshAutomationComboBox() {
        if (automationComboBox != null) {
            automationComboBox.setItems(FXCollections.observableArrayList(automations.keySet()));
        }
    }
    
    private void refreshTimeline() {
        if (timelineView != null) {
            timelineView.updateTimeline(automations);
        }
    }

    public void show(String name){

        Automation auto = automations.get(name);
        if(auto != null) {
            title.setText(auto.name);
            user.setText(auto.user);
            password.setText(auto.password);
            character.setText(auto.character);

            range.setText(String.valueOf(auto.interval));
            duration.setText(String.valueOf(auto.duration));
            disable.setSelected(auto.disabled);
            isRepeted.setSelected(auto.isRepeted);
            isStartTime.setSelected(auto.isStartTime);
            dataPicker.setTime(auto.startTime);

            for (Scenario sc : loadedScenarios.values()) {
                String display = sc.name + " (ID: " + sc.id + ")";
                if (sc.id == auto.scenarioId) {
                    scenarioPicker.getSelectionModel().select(display);
                    break;
                }
            }
        }
    }

    @Override
    public void stop() throws Exception {
        isWork.set(false);
        super.stop();
    }

    @Override
    public void start(Stage stage) throws IOException {
        loadSchedules();
        /// Главное меню
        MenuBar menuBar = new MenuBar();
        Menu menu1 = new Menu("File");
        menuBar.getMenus().add(menu1);
        MenuItem openn= new MenuItem("New");
        openn.setOnAction(new EventHandler<ActionEvent>() {
                              @Override
                              public void handle(ActionEvent event) {
                                  automations.clear();
                                  clear();
                                  saveSchedules();
                              }

        });
        MenuItem openi= new MenuItem("Open");
        openi.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                FileChooser fileChooser = new FileChooser();
                FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("JSON files (*.JSON)", "*.json");
                fileChooser.getExtensionFilters().add(extFilter);
                fileChooser.setTitle("Open scenario");
                fileChooser.setInitialDirectory(new File("./"));
                File file = fileChooser.showOpenDialog(stage);
                if(file!=null && file.exists()) {
                    automations.clear();
                    read(file.getPath());
                    saveSchedules();
                    refreshAutomationComboBox();
                }
            }
        });
        MenuItem savei = new MenuItem("Save");
        savei.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                FileChooser fileChooser = new FileChooser();
                FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("JSON files (*.JSON)", "*.json");
                fileChooser.getExtensionFilters().add(extFilter);
                fileChooser.setTitle("Save scenario");
                fileChooser.setInitialDirectory(new File("./"));
                File file = fileChooser.showSaveDialog(stage);
                if(file!=null) {
                    write(file.getPath());
                }
            }
        });

        MenuItem config = new MenuItem("Configuration");
        config.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                // New window (Stage)
                Stage newWindow = new Stage();
                newWindow.setTitle("Configuration");
                newWindow.initModality(Modality.WINDOW_MODAL);
                newWindow.initOwner(stage);
                VBox mainlayout = new VBox();
                VBox.setMargin(mainlayout, new Insets(5,5,5,10));
                VBox centrallayout = new VBox();
                VBox.setMargin(centrallayout, new Insets(5,5,5,10));
                centrallayout.setSpacing(5);
                centrallayout.getChildren().add(new Label("Java path:"));
                centrallayout.getChildren().add(setPath(stage,Configuration.getInstance().javaPath, "Java VM", "java.exe"));
                centrallayout.getChildren().add(new Label("Hafen path:"));
                centrallayout.getChildren().add(setPath(stage,Configuration.getInstance().hafenPath,"HnH jar file", "hafen.jar"));
                CheckBox java18 = new CheckBox("Java 18+ (Oracle)");
                java18.setSelected(Configuration.getInstance().isJava18);
                java18.selectedProperty().addListener(new ChangeListener<Boolean>() {

                    @Override
                    public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                        Configuration.getInstance().isJava18 = newValue;
                        Configuration.getInstance().write();
                    }
                });
                centrallayout.getChildren().add(java18);
                mainlayout.getChildren().add(centrallayout);
                Scene scene = new Scene(mainlayout, 315, 220);
                newWindow.setScene(scene);

                // Set position of second window, related to primary window.
                newWindow.setX(stage.getX() + 200);
                newWindow.setY(stage.getY() + 100);

                newWindow.show();
            }
        });

        menu1.getItems().addAll(openn, openi, savei, config);

        ObservableList<String> langs = FXCollections.observableArrayList();
        automationComboBox = new ComboBox<String>(langs);
        automationComboBox.setMaxWidth(Double.MAX_VALUE);

        automationComboBox.valueProperty().addListener(new ChangeListener<String>() {
            @Override public void changed(ObservableValue ov, String t, String t1) {
                if(t1!=null) {
                    show(t1);
                }
            }
        });

        /// Слои
        VBox mainlayout = new VBox(menuBar);
        /// Основной слой
        VBox centrallayout = new VBox();
        VBox.setMargin(centrallayout, new Insets(5,15,5,15));
        centrallayout.setPadding(new Insets(0,10,0,10));
        centrallayout.setSpacing(5);
        
        // Schedule management section
        HBox scheduleSection = new HBox();
        scheduleSection.setSpacing(5);
        scheduleSection.setAlignment(Pos.CENTER_LEFT);
        
        VBox scheduleSelectionBox = new VBox();
        scheduleSelectionBox.setSpacing(3);
        scheduleSelectionBox.getChildren().add(new Label("Schedule:"));
        
        scheduleComboBox = new ComboBox<>();
        scheduleComboBox.setMaxWidth(Double.MAX_VALUE);
        scheduleComboBox.valueProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue ov, String oldValue, String newValue) {
                if (newValue != null && !newValue.equals(currentScheduleName)) {
                    switchToSchedule(newValue);
                }
            }
        });
        scheduleSelectionBox.getChildren().add(scheduleComboBox);
        VBox.setVgrow(scheduleComboBox, Priority.ALWAYS);
        
        VBox scheduleButtonsBox = new VBox();
        scheduleButtonsBox.setSpacing(3);
        scheduleButtonsBox.getChildren().add(new Label("Actions:"));
        
        HBox scheduleButtons = new HBox();
        scheduleButtons.setSpacing(5);
        
        newScheduleButton = new Button("New");
        newScheduleButton.setOnAction(e -> showNewScheduleDialog());
        
        saveScheduleButton = new Button("Save All");
        saveScheduleButton.setOnAction(e -> saveSchedules());
        
        deleteScheduleButton = new Button("Delete");
        deleteScheduleButton.setOnAction(e -> showDeleteScheduleDialog());
        
        renameScheduleButton = new Button("Rename");
        renameScheduleButton.setOnAction(e -> showRenameScheduleDialog());
        
        scheduleButtons.getChildren().addAll(newScheduleButton, saveScheduleButton, deleteScheduleButton, renameScheduleButton);
        scheduleButtonsBox.getChildren().add(scheduleButtons);
        
        scheduleSection.getChildren().addAll(scheduleSelectionBox, scheduleButtonsBox);
        HBox.setHgrow(scheduleSelectionBox, Priority.ALWAYS);
        centrallayout.getChildren().add(scheduleSection);
        
        centrallayout.getChildren().add(new Label("Task:"));
        HBox taskBox = new HBox();
        taskBox.getChildren().add(automationComboBox);
        HBox.setHgrow(automationComboBox, Priority.ALWAYS);
        centrallayout.getChildren().add(taskBox);

        /// Слой заголовка
        HBox titleBox  = new HBox();
        titleBox.getChildren().add(new Label("Automation name"));
        title = new TextField();
        titleBox.setSpacing(30);
        titleBox.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(title, Priority.ALWAYS);
        HBox.setMargin(title, new Insets(5,5,0,0));
        titleBox.getChildren().add(title);
        titleBox.setMinWidth(630);
        centrallayout.getChildren().add(titleBox);

        /// Слой параметров
        VBox propBox = new VBox();
        propBox.setMinWidth(350);
        propBox.setPrefWidth(350);
        propBox.setSpacing(7);
        disable = new CheckBox("Disable");
        propBox.getChildren().add(disable);

        HBox userBox  = new HBox();
        userBox.setSpacing(10);
        Label userLabel = new Label("User");
        userLabel.setPrefWidth(120);
        userBox.getChildren().add(userLabel);
        user = new TextField();
        user.setPrefWidth(200);
        userBox.getChildren().add(user);

        HBox passwordBox  = new HBox();
        passwordBox.setSpacing(10);
        Label passwordLabel = new Label("Password");
        passwordLabel.setPrefWidth(120);
        passwordBox.getChildren().add(passwordLabel);
        password = new PasswordField();
        password.setPrefWidth(200);
        passwordBox.getChildren().add(password);

        HBox characterBox  = new HBox();
        characterBox.setSpacing(10);
        Label characterLabel = new Label("Character");
        characterLabel.setPrefWidth(120);
        characterBox.getChildren().add(characterLabel);
        character = new TextField();
        character.setPrefWidth(200);
        characterBox.getChildren().add(character);

        HBox rangeBox  = new HBox();
        rangeBox.setSpacing(10);
        Label rangeLabel = new Label("Time interval(min)");
        rangeLabel.setPrefWidth(120);
        rangeBox.getChildren().add(rangeLabel);
        range = new TextField();
        range.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue,
                                String newValue) {
                if (!newValue.matches("\\d*")) {
                    range.setText(newValue.replaceAll("[^\\d]", ""));
                }
            }
        });
        range.setPrefWidth(200);
        range.setText("0");
        rangeBox.getChildren().add(range);

        HBox durationBox  = new HBox();
        durationBox.setSpacing(10);
        Label durationLabel = new Label("Duration(min.)");
        durationLabel.setPrefWidth(120);
        durationBox.getChildren().add(durationLabel);
        duration = new TextField();
        duration.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue,
                                String newValue) {
                if (!newValue.matches("\\d*")) {
                    duration.setText(newValue.replaceAll("[^\\d]", ""));
                }
            }
        });

        duration.setPrefWidth(200);
        duration.setText("0");
        durationBox.getChildren().add(duration);

        propBox.getChildren().add(userBox);
        propBox.getChildren().add(passwordBox);
        propBox.getChildren().add(characterBox);
        propBox.getChildren().add(durationBox);

        isRepeted = new CheckBox("Repeat");
        propBox.getChildren().add(isRepeted);

        propBox.getChildren().add(rangeBox);


        dataPicker = new DateTimePicker();
        dataPicker.getEditor().focusedProperty().addListener((obj, wasFocused, isFocused)->{
            if (!isFocused) {
                try {
                    dataPicker.setValue(dataPicker.getConverter().fromString(dataPicker.getEditor().getText()));
                } catch (DateTimeParseException e) {
                    dataPicker.getEditor().setText(dataPicker.getConverter().toString(dataPicker.getValue()));
                }
            }
        });
        dataPicker.setMinWidth(200);

        isStartTime = new CheckBox("Start time(opt)");
        isStartTime.setPrefWidth(120);
        HBox timeBox = new HBox();
        timeBox.setSpacing(10);
        timeBox.setPrefWidth(120);
        timeBox.getChildren().add(isStartTime);
        timeBox.getChildren().add(dataPicker);
        propBox.getChildren().add(timeBox);

        HBox scenarioBox = new HBox();
        scenarioBox.setSpacing(15);
        scenarioBox.setAlignment(Pos.CENTER_LEFT);
        scenarioBox.getChildren().add(propBox);

        ObservableList<String> scenarioNames = loadScenariosFromNurglingFile();

        VBox rightBox = new VBox();
        scenarioPicker = new ListView<>(scenarioNames);
        ObservableList<String> items = loadScenariosFromNurglingFile();
        scenarioPicker.setItems(items);
        scenarioPicker.setMinWidth(290);
        scenarioPicker.setPrefWidth(290);
        scenarioPicker.setMinHeight(300);
        scenarioPicker.setMaxHeight(350);
        rightBox.getChildren().add(new Label("Available scenarios:"));
        rightBox.getChildren().add(scenarioPicker);
        rightBox.setSpacing(7);
        HBox buttonBox = new HBox();
        buttonBox.setSpacing(7);
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        add = new Button("Add");
        add.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (scenarioPicker.getSelectionModel().getSelectedItem() != null)
                    add();
            }
        });
        save = new Button("Save");
        save.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (scenarioPicker.getSelectionModel().getSelectedItem() != null)
                    save();
            }
        });
        delete = new Button("Delete");
        delete.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                automations.remove(automationComboBox.getSelectionModel().getSelectedItem());
                if(automations.size()>0){
                    refreshAutomationComboBox();
                    automationComboBox.getSelectionModel().selectFirst();
                    show(automationComboBox.getSelectionModel().getSelectedItem());
                }else{
                    clear();
                }
                saveSchedules();
                refreshTimeline();
            }
        });
        HBox.setHgrow(buttonBox, Priority.ALWAYS);
//        HBox.setHgrow(save, Priority.ALWAYS);
//        HBox.setHgrow(delete, Priority.ALWAYS);
        buttonBox.getChildren().add(add);
        buttonBox.getChildren().add(save);
        buttonBox.getChildren().add(delete);
        scenarioBox.getChildren().add(rightBox);
        centrallayout.getChildren().add(scenarioBox);
        centrallayout.getChildren().add(buttonBox);

        // Create timeline view
        timelineView = new TimelineView();
        
        // Create tab pane for configuration and timeline
        TabPane tabPane = new TabPane();
        
        Tab configTab = new Tab("Configuration");
        configTab.setClosable(false);
        configTab.setContent(centrallayout);
        
        Tab timelineTab = new Tab("Timeline");
        timelineTab.setClosable(false);
        timelineTab.setContent(timelineView);
        
        tabPane.getTabs().addAll(configTab, timelineTab);
        
        mainlayout.getChildren().add(tabPane);
        table = new TableView<>();
        table.setEditable(true);
        TableColumn<Task, String> name = new TableColumn<Task, String>("Name");
        name.setMinWidth(100);
        name.setPrefWidth(100);
        TableColumn<Task, String> start = new TableColumn<Task, String>("Start time");
        start.setMinWidth(130);
        start.setPrefWidth(130);
        TableColumn<Task, String> stop = new TableColumn<Task, String>("End time");
        stop.setMinWidth(130);
        stop.setPrefWidth(130);
        TableColumn<Task, String> status = new TableColumn<Task, String>("Status");
        status.setMinWidth(120);
        status.setPrefWidth(120);
        TableColumn<Task, Void> pauseCol = new TableColumn<Task, Void>("Pause");
        pauseCol.setMinWidth(140);
        pauseCol.setPrefWidth(140);
        
        name.setCellValueFactory(new PropertyValueFactory<>("name"));
        start.setCellValueFactory(new PropertyValueFactory<>("start"));
        stop.setCellValueFactory(new PropertyValueFactory<>("stop"));
        status.setCellValueFactory(new PropertyValueFactory<>("pauseStatus"));
        
        pauseCol.setCellFactory(col -> new TableCell<Task, Void>() {
            private final Button pauseButton = new Button("Pause");
            private final TextField minutesField = new TextField("30");
            private final HBox buttonBox = new HBox(5);
            
            {
                minutesField.setPrefWidth(35);
                minutesField.setPromptText("min");
                pauseButton.setPrefWidth(60);
                pauseButton.setOnAction(e -> {
                    Task task = getTableView().getItems().get(getIndex());
                    if (task != null) {
                        try {
                            int minutes = Integer.parseInt(minutesField.getText());
                            if (task.isPaused()) {
                                task.resume();
                            } else {
                                task.pauseForMinutes(minutes);
                            }
                            getTableView().refresh();
                        } catch (NumberFormatException ex) {
                            // Invalid number, ignore
                        }
                    }
                });
                buttonBox.setAlignment(Pos.CENTER);
                buttonBox.getChildren().addAll(minutesField, pauseButton);
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Task task = getTableView().getItems().get(getIndex());
                    if (task != null) {
                        pauseButton.setText(task.isPaused() ? "Resume" : "Pause");
                        minutesField.setVisible(!task.isPaused());
                    }
                    setGraphic(buttonBox);
                }
            }
        });

        table.getColumns().addAll(name, start, stop, status, pauseCol);
        table.setPrefHeight(200);
        table.setMaxHeight(200);

        mainlayout.getChildren().add(table);

        HBox mainButton = new HBox();

        startb = new Button("Run");
        startb.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if(!automations.isEmpty()) {
                    th = new Thread(new Worker("./hafen.jar"));
                    th.start();
                    scenarioBox.setDisable(true);
                    startb.setDisable(true);
                    titleBox.setDisable(true);
                    stopb.setDisable(false);
                }

            }
        });
        stopb = new Button("Stop all");
        stopb.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                    if(th!=null){
                        scenarioBox.setDisable(false);
                        startb.setDisable(false);
                        titleBox.setDisable(false);
                        stopb.setDisable(true);
                        isWork.set(false);
                        try {
                            th.join();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
            }
        });
        stopb.setDisable(true);
        mainButton.setAlignment(Pos.CENTER_RIGHT);
        mainButton.getChildren().add(startb);
        mainButton.getChildren().add(stopb);

        mainlayout.getChildren().add(mainButton);
        Scene scene = new Scene(mainlayout, 680, 720);
        stage.setMaxWidth(700);
        stage.setMinWidth(700);
        stage.setTitle("HH client: Nurgling");
        stage.getIcons().add(new Image(
                        getClass().getResourceAsStream( "/timer.png" )));

        refreshScheduleComboBox();
        refreshAutomationComboBox();
        
        if (!automations.isEmpty()) {
            automationComboBox.getSelectionModel().selectFirst();
            show(automationComboBox.getSelectionModel().getSelectedItem());
        }
        
        // Initialize timeline
        refreshTimeline();

        stage.setScene(scene);
        stage.show();
    }

    private void showNewScheduleDialog() {
        Stage dialog = new Stage();
        dialog.setTitle("New Schedule");
        dialog.initModality(Modality.APPLICATION_MODAL);
        
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(20));
        
        Label nameLabel = new Label("Schedule Name:");
        TextField nameField = new TextField();
        nameField.setPromptText("Enter schedule name");
        
        Label descLabel = new Label("Description (optional):");
        TextField descField = new TextField();
        descField.setPromptText("Enter description");
        
        HBox buttons = new HBox(10);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        
        Button createButton = new Button("Create");
        createButton.setOnAction(e -> {
            String name = nameField.getText().trim();
            if (!name.isEmpty() && !schedules.containsKey(name)) {
                createNewSchedule(name, descField.getText().trim());
                dialog.close();
            }
        });
        
        Button cancelButton = new Button("Cancel");
        cancelButton.setOnAction(e -> dialog.close());
        
        buttons.getChildren().addAll(cancelButton, createButton);
        layout.getChildren().addAll(nameLabel, nameField, descLabel, descField, buttons);
        
        Scene scene = new Scene(layout, 300, 200);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void showDeleteScheduleDialog() {
        if (schedules.size() <= 1) {
            return; // Cannot delete the last schedule
        }
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Schedule");
        alert.setHeaderText("Delete Schedule: " + currentScheduleName);
        alert.setContentText("Are you sure you want to delete this schedule? This action cannot be undone.");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                deleteSchedule(currentScheduleName);
            }
        });
    }

    private void showRenameScheduleDialog() {
        Stage dialog = new Stage();
        dialog.setTitle("Rename Schedule");
        dialog.initModality(Modality.APPLICATION_MODAL);
        
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(20));
        
        Schedule current = schedules.get(currentScheduleName);
        
        Label nameLabel = new Label("Schedule Name:");
        TextField nameField = new TextField(current.name);
        
        Label descLabel = new Label("Description:");
        TextField descField = new TextField(current.description);
        
        HBox buttons = new HBox(10);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        
        Button saveButton = new Button("Save");
        saveButton.setOnAction(e -> {
            String newName = nameField.getText().trim();
            if (!newName.isEmpty() && (!schedules.containsKey(newName) || newName.equals(current.name))) {
                renameSchedule(current.name, newName, descField.getText().trim());
                dialog.close();
            }
        });
        
        Button cancelButton = new Button("Cancel");
        cancelButton.setOnAction(e -> dialog.close());
        
        buttons.getChildren().addAll(cancelButton, saveButton);
        layout.getChildren().addAll(nameLabel, nameField, descLabel, descField, buttons);
        
        Scene scene = new Scene(layout, 300, 200);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    HBox setPath(Stage stage, Configuration.Path path, String name, String filter)
    {
        HBox cacpbox  = new HBox();
        TextField configPath = new TextField();
        configPath.setText(path.value);
        configPath.setPrefWidth(276);
        Region fillern = new Region();
        HBox.setHgrow(fillern, Priority.ALWAYS);
        cacpbox.getChildren().add(fillern);
        cacpbox.getChildren().add(configPath);
        Button configSelect = new Button("...");
        configSelect.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                FileChooser fileChooser = new FileChooser();
                FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(name, filter);
                fileChooser.getExtensionFilters().add(extFilter);
                fileChooser.setTitle("Select path");
                File file = fileChooser.showOpenDialog(stage);
                if(file!=null && file.exists()) {
                    configPath.setText(file.getPath());
                    path.value = file.getPath();
                    Configuration.getInstance().write();
                }
            }
        });
        cacpbox.getChildren().add(configSelect);
        return cacpbox;
    }

    private ObservableList<String> loadScenariosFromNurglingFile() {
        ObservableList<String> scenarioNames = FXCollections.observableArrayList();
        loadedScenarios.clear();
        String path = getScenarioFilePath();
        File file = new File(path);
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
                JSONParser parser = new JSONParser();
                JSONObject main = (JSONObject) parser.parse(reader);
                JSONArray array = (JSONArray) main.get("scenarios");
                for (Object o : array) {
                    JSONObject item = (JSONObject) o;
                    int id = ((Number) item.get("id")).intValue();
                    String name = item.get("name").toString();
                    loadedScenarios.put(id, new Scenario(id, name));
                    scenarioNames.add(name + " (ID: " + id + ")");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return scenarioNames;
    }

    private static String getScenarioFilePath() {
        String appdata = System.getenv("APPDATA");
        if (appdata != null) {
            File base = new File(appdata, "Haven and Hearth/data");
            if (!base.exists()) {
                base.mkdirs();
            }

            File parent = base.getParentFile();
            if (parent != null) {
                File scenarioFile = new File(parent, "scenarios.nurgling.json");
                return scenarioFile.getAbsolutePath();
            }
        }

        String userHome = System.getProperty("user.home");
        if (userHome != null) {
            File base = new File(userHome, ".haven/data");
            if (!base.exists()) {
                base.mkdirs();
            }
            File parent = base.getParentFile();
            if (parent != null) {
                File scenarioFile = new File(parent, "scenarios.nurgling.json");
                return scenarioFile.getAbsolutePath();
            }
        }
        throw new RuntimeException("Unable to resolve scenario file path");
    }

    private static String getScheduleFilePath() {
        String appdata = System.getenv("APPDATA");
        if (appdata != null) {
            File base = new File(appdata, "Haven and Hearth/data");
            if (!base.exists()) {
                base.mkdirs();
            }

            File parent = base.getParentFile();
            if (parent != null) {
                File scheduleFile = new File(parent, "schedules.hhautolauncher.json");
                return scheduleFile.getAbsolutePath();
            }
        }

        String userHome = System.getProperty("user.home");
        if (userHome != null) {
            File base = new File(userHome, ".haven/data");
            if (!base.exists()) {
                base.mkdirs();
            }
            File parent = base.getParentFile();
            if (parent != null) {
                File scheduleFile = new File(parent, "schedules.hhautolauncher.json");
                return scheduleFile.getAbsolutePath();
            }
        }
        throw new RuntimeException("Unable to resolve schedule file path");
    }


    public static void main(String[] args) {
        launch();
    }
}