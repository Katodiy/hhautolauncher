module core.hhrunner {
    requires javafx.controls;
    requires javafx.fxml;


    opens core.hhrunner to javafx.fxml;
    exports core.hhrunner;
}