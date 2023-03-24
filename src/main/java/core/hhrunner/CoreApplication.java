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
import javafx.scene.chart.PieChart;
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
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class CoreApplication extends Application {

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
        scenariosCombobox.setItems(FXCollections.observableArrayList(scenarios.keySet()));
    }

    Process p;

    HashMap<String,Scenario> scenarios = new HashMap<>();

    ComboBox<String> scenariosCombobox;

    public AtomicBoolean isWork = new AtomicBoolean(false);
    Thread th = null;

    class Worker implements Runnable{
        public Worker(String path) {
            this.path = path;
            long time = System.currentTimeMillis();
            for(String ket: scenarios.keySet()){
                Scenario res = scenarios.get(ket);
                tasks.add(new Task((res.isStartTime)? res.startTime : time,res));
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
                            task.startProcess(path);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }else if (task.isWork() && task.stop<=time){
                        task.stopProcess();
                        forDelete.add(task);
                        if(task.scenario.isRepeted)
                            forAdding.add(new Task(task.start+ (long) task.scenario.interval *60*1000,task.scenario));
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
        JSONArray jscenarios = new JSONArray ();
        for ( String key : scenarios.keySet() ) {
            Scenario res = scenarios.get(key);
            JSONObject userobj = new JSONObject ();
            userobj.put ( "name", res.name );
            userobj.put ( "user", res.user );
            userobj.put ( "password", res.password );
            userobj.put ( "character", res.character );
            userobj.put ( "bot", res.bot );
            userobj.put ( "isStartTime", res.isStartTime );
            userobj.put ( "time", res.startTime );
            userobj.put ( "disabled", res.disabled );
            userobj.put ( "isRepeted", res.isRepeted );
            userobj.put ( "duration", res.duration );
            userobj.put ( "interval", res.interval );
            userobj.put ( "nomad", res.nomad );
            jscenarios.add ( userobj );
        }
        obj.put("scenarios",jscenarios);


        try ( FileWriter file = new FileWriter ( path ) ) {
            file.write ( obj.toJSONString () );
        }
        catch ( IOException e ) {
            e.printStackTrace ();
        }

    }


    public void read ( String path ) {
        try {
            BufferedReader reader = new BufferedReader (
                    new InputStreamReader( new FileInputStream( path ), "cp1251" ) );
            JSONParser parser = new JSONParser();
            JSONObject jsonObject = ( JSONObject ) parser.parse ( reader );

            JSONArray msg = ( JSONArray ) jsonObject.get ( "scenarios" );
            if(msg!=null) {
                Iterator<JSONObject> iterator = msg.iterator();
                while (iterator.hasNext()) {
                    JSONObject item = iterator.next();
                    Scenario res = new Scenario();
                    res.name = item.get("name").toString();
                    res.user = item.get("user").toString();
                    res.password = item.get("password").toString();
                    res.character = item.get("character").toString();
                    res.bot = item.get("bot").toString();
                    res.isStartTime = (boolean)item.get("isStartTime");
                    if(item.get("time")!=null) {
                        res.startTime = (long) item.get("time");
                    }
                    res.disabled = (boolean)item.get("disabled");
                    res.isRepeted = (boolean)item.get("isRepeted");
                    res.duration =(int)((long)item.get("duration"));
                    res.interval = (int)((long)item.get("interval"));
                    res.nomad = item.get("nomad").toString();
                    scenarios.put(res.name,res);
                }

                if(scenarios.size()>0){
                    scenariosCombobox.setItems(FXCollections.observableArrayList(scenarios.keySet()));
                    for(String key: scenarios.keySet()){
                        scenariosCombobox.getSelectionModel().select(key);
                        break;
                    }
                }
            }


        }
        catch ( IOException | json.parser.ParseException e ) {
            e.printStackTrace ();
        }
    }

    private Scenario build(){
        Scenario res = new Scenario();
        res.name = title.getText();
        res.user = user.getText();
        res.password = password.getText();
        res.character = character.getText();
        res.bot = list.getSelectionModel().getSelectedItem();
        res.isStartTime = isStartTime.isSelected();
        res.startTime = dataPicker.getTime();
        res.disabled = disable.isSelected();
        res.isRepeted = isRepeted.isSelected();
        res.duration = Integer.parseInt(duration.getText());
        res.interval = Integer.parseInt(range.getText());
        res.nomad = nomadPath.getText();
        return res;
    }

    public void add(){
        Scenario res = build();
        scenarios.put(res.name,res);
        scenariosCombobox.setItems(FXCollections.observableArrayList(scenarios.keySet()));
        scenariosCombobox.getSelectionModel().select(res.name);
    }

    public void save(){
        Scenario res = build();
        scenarios.put(res.name,res);
        scenariosCombobox.setItems(FXCollections.observableArrayList(scenarios.keySet()));
        scenariosCombobox.getSelectionModel().select(res.name);
    }
    public void show(String name){

        Scenario res = scenarios.get(name);
        if(res!=null) {
            title.setText(res.name);
            user.setText(res.user);
            password.setText(res.password);
            character.setText(res.character);
            list.getSelectionModel().select(res.bot);
            isStartTime.setSelected(res.isStartTime);
            dataPicker.setTime(res.startTime);
            disable.setSelected(res.disabled);
            isRepeted.setSelected(res.isRepeted);
            duration.setText(String.valueOf(res.duration));
            range.setText(String.valueOf(res.interval));
            nomadPath.setText(res.nomad);
        }
    }

    @Override
    public void start(Stage stage) throws IOException {
        /// Главное меню
        MenuBar menuBar = new MenuBar();
        Menu menu1 = new Menu("File");
        menuBar.getMenus().add(menu1);
        MenuItem openn= new MenuItem("New");
        openn.setOnAction(new EventHandler<ActionEvent>() {
                              @Override
                              public void handle(ActionEvent event) {
                                  scenarios.clear();
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
                    scenarios.clear();
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
                centrallayout.getChildren().add(new Label("Hafen config and calibration path:"));
                centrallayout.getChildren().add(setPath(stage,Configuration.getInstance().ncacPath, "Nurgling config", "config.nurgling.json"));
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
        scenariosCombobox = new ComboBox<String>(langs);
        scenariosCombobox.setMinWidth(620);
        scenariosCombobox.valueProperty().addListener(new ChangeListener<String>() {
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
        centrallayout.getChildren().add(scenariosCombobox);

        /// Слой заголовка
        HBox titleBox  = new HBox();
        titleBox.getChildren().add(new Label("Scenario name"));
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

        VBox rightBox = new VBox();
        list = new ListView<String>();
        ObservableList<String> items =FXCollections.observableArrayList (
                "Dreamer", "Candleberry", "Steel", "Smelter", "Clay", "Truffle", "GobFinder");
        list.setItems(items);
        list.setMinHeight(320);
        list.setMaxHeight(390);
        rightBox.getChildren().add(new Label("Available bots:"));
        rightBox.getChildren().add(list);
        rightBox.setSpacing(7);
        HBox buttonBox = new HBox();
        buttonBox.setSpacing(7);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        add = new Button("Add");
        add.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if(list.getSelectionModel().getSelectedItem()!=null)
                    add();
            }
        });
        save = new Button("Save");
        save.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if(list.getSelectionModel().getSelectedItem()!=null)
                    save();
            }
        });
        delete = new Button("Delete");
        delete.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                scenarios.remove(scenariosCombobox.getSelectionModel().getSelectedItem());
                if(scenarios.size()>0){
                    scenariosCombobox.setItems(FXCollections.observableArrayList(scenarios.keySet()));
                    for(String key: scenarios.keySet()){
                        scenariosCombobox.getSelectionModel().select(key);
                        break;
                    }
                }else{
                    clear();
                }
            }
        });
        HBox.setHgrow(buttonBox, Priority.ALWAYS);
//        HBox.setHgrow(save, Priority.ALWAYS);
//        HBox.setHgrow(delete, Priority.ALWAYS);
        buttonBox.getChildren().add(add);
        buttonBox.getChildren().add(save);
        buttonBox.getChildren().add(delete);
        rightBox.getChildren().add(buttonBox);
        scenarioBox.getChildren().add(rightBox);
        centrallayout.getChildren().add(scenarioBox);

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
                if(!scenarios.isEmpty()) {
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

    public static void main(String[] args) {
        launch();
    }
}