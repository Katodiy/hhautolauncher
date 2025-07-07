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
        automationComboBox.setItems(FXCollections.observableArrayList(automations.keySet()));
    }

    Process p;

    HashMap<String,Automation> automations = new HashMap<>();
    HashMap<Integer, Scenario> loadedScenarios = new HashMap<>();
    ListView<String> scenarioPicker;
    ComboBox<String> automationComboBox;

    public AtomicBoolean isWork = new AtomicBoolean(false);
    Thread th = null;

    class Worker implements Runnable{
        public Worker(String path) {
            this.path = path;
            long time = System.currentTimeMillis();
            for(String key : automations.keySet()){
                Automation auto = automations.get(key);
                tasks.add(new Task((auto.isStartTime) ? auto.startTime : time, auto));
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
                    if(!task.isWork() && task.start<=time){
                        try {
                            task.startProcess(Configuration.getInstance().hafenPath.value);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }else if (task.isWork() && task.stop<=time){
                        task.stopProcess();
                        forDelete.add(task);
                        if(task.automation.isRepeted)
                            forAdding.add(new Task(task.start+ (long) task.automation.interval *60*1000,task.automation));
                    }
                }
                for(Task task: forDelete){
                    tasks.remove(task);
                }
                tasks.addAll(forAdding);
                if(!forDelete.isEmpty() || !forAdding.isEmpty()){
                    table.setItems( FXCollections.observableArrayList(tasks));
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
        write(AUTOMATIONS_FILE);
    }

    public void save(){
        Automation res = build();
        automations.put(res.name,res);
        automationComboBox.setItems(FXCollections.observableArrayList(automations.keySet()));
        automationComboBox.getSelectionModel().select(res.name);
        write(AUTOMATIONS_FILE);
    }
    public void show(String name){

        Automation auto = automations.get(name);
        if(auto != null) {
            title.setText(auto.name);
            user.setText(auto.user);
            password.setText(auto.password);
            character.setText(auto.character);

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
        read(AUTOMATIONS_FILE);
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
        automationComboBox.setMinWidth(620);

        automationComboBox.setItems(FXCollections.observableArrayList(automations.keySet()));
        if (!automations.isEmpty()) {
            for (String key : automations.keySet()) {
                automationComboBox.getSelectionModel().select(key);
                break;
            }
        }

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
        VBox.setMargin(centrallayout, new Insets(5,5,5,10));
        centrallayout.setSpacing(5);
        centrallayout.getChildren().add(new Label("Task:"));
        centrallayout.getChildren().add(automationComboBox);

        /// Слой заголовка
        HBox titleBox  = new HBox();
        titleBox.getChildren().add(new Label("Automation name"));
        title = new TextField();
        titleBox.setSpacing(100);
        titleBox.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(title, Priority.ALWAYS);
        HBox.setMargin(title, new Insets(5,5,0,0));
        titleBox.getChildren().add(title);
        titleBox.setMinWidth(620);
        centrallayout.getChildren().add(titleBox);

        /// Слой параметров
        VBox propBox = new VBox();
        propBox.setMinWidth(365);
        propBox.setSpacing(7);
        disable = new CheckBox("Disable");
        propBox.getChildren().add(disable);

        HBox userBox  = new HBox();
        userBox.getChildren().add(new Label("User"));
        user = new TextField();
        user.setPrefWidth(200);
        Region filleru = new Region();
        HBox.setHgrow(filleru, Priority.ALWAYS);
        userBox.getChildren().add(filleru);
        userBox.getChildren().add(user);

        HBox passwordBox  = new HBox();
        passwordBox.getChildren().add(new Label("Password"));
        password = new PasswordField();
        password.setPrefWidth(200);
        Region fillerp = new Region();
        HBox.setHgrow(fillerp, Priority.ALWAYS);
        passwordBox.getChildren().add(fillerp);
        passwordBox.getChildren().add(password);

        HBox characterBox  = new HBox();
        characterBox.getChildren().add(new Label("Character"));
        character = new TextField();
        character.setPrefWidth(200);
        Region fillerc = new Region();
        HBox.setHgrow(fillerc, Priority.ALWAYS);
        characterBox.getChildren().add(fillerc);
        characterBox.getChildren().add(character);

        HBox rangeBox  = new HBox();
        rangeBox.getChildren().add(new Label("Time interval(min)"));
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
        Region fillerr = new Region();
        HBox.setHgrow(fillerr, Priority.ALWAYS);
        rangeBox.getChildren().add(fillerr);
        rangeBox.getChildren().add(range);

        HBox durationBox  = new HBox();
        durationBox.getChildren().add(new Label("Duration(min.)"));
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
        Region fillerd = new Region();
        HBox.setHgrow(fillerd, Priority.ALWAYS);
        durationBox.getChildren().add(fillerd);
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
        HBox timeBox = new HBox();
        timeBox.getChildren().add(isStartTime);
        Region fillert = new Region();
        HBox.setHgrow(fillert, Priority.ALWAYS);
        timeBox.getChildren().add(fillert);
        timeBox.getChildren().add(dataPicker);
        propBox.getChildren().add(timeBox);


        HBox nomadBox  = new HBox();
        nomadBox.getChildren().add(new Label("Nomad file(opt)"));
        nomadPath = new TextField();
        nomadPath.setPrefWidth(176);
        Region fillern = new Region();
        HBox.setHgrow(fillern, Priority.ALWAYS);
        nomadBox.getChildren().add(fillern);
        nomadBox.getChildren().add(nomadPath);
        nomadSelect = new Button("...");
        nomadSelect.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                FileChooser fileChooser = new FileChooser();
                FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Nomad files (*.DAT)", "*.dat");
                fileChooser.getExtensionFilters().add(extFilter);
                fileChooser.setTitle("Select *.dat file");
                File file = fileChooser.showOpenDialog(stage);
                if(file!=null && file.exists()) {
                    nomadPath.setText(file.getPath());
                }
            }
        });
        nomadBox.getChildren().add(nomadSelect);

        propBox.getChildren().add(nomadBox);

        HBox scenarioBox = new HBox();
        scenarioBox.setSpacing(7);
        scenarioBox.getChildren().add(propBox);

        ObservableList<String> scenarioNames = loadScenariosFromNurglingFile();

        VBox rightBox = new VBox();
        scenarioPicker = new ListView<>(scenarioNames);
        ObservableList<String> items = loadScenariosFromNurglingFile();
        scenarioPicker.setItems(items);
        scenarioPicker.setMinWidth(300);
        scenarioPicker.setMinHeight(320);
        scenarioPicker.setMaxHeight(390);
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
                    automationComboBox.setItems(FXCollections.observableArrayList(automations.keySet()));
                    for(String key: automations.keySet()){
                        automationComboBox.getSelectionModel().select(key);
                        break;
                    }
                }else{
                    clear();
                }
                write(AUTOMATIONS_FILE);
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

        mainlayout.getChildren().add(centrallayout);
        table = new TableView<>();
        table.setEditable(true);
        TableColumn<Task, String> name = new TableColumn<Task, String>("Name");
        name.setMinWidth(214);
        TableColumn<Task, String> start = new TableColumn<Task, String>("Start time");
        start.setMinWidth(212);
        TableColumn<Task, String> stop = new TableColumn<Task, String>("End time");
        stop.setMinWidth(212);
        name.setCellValueFactory(new PropertyValueFactory<>("name"));
        start.setCellValueFactory(new PropertyValueFactory<>("start"));
        stop.setCellValueFactory(new PropertyValueFactory<>("stop"));
//        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
//        firstNameCol.setCellValueFactory(new PropertyValueFactory<>("firstName"));

        table.getColumns().addAll(name, start, stop);

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
        Scene scene = new Scene(mainlayout, 640, 660);
        stage.setMaxWidth(660);
        stage.setMinWidth(660);
        stage.setTitle("HH client: Nurgling");
        stage.getIcons().add(new Image(
                        getClass().getResourceAsStream( "/timer.png" )));
        stage.setScene(scene);
        stage.show();
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


    public static void main(String[] args) {
        launch();
    }
}