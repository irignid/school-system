package com.school;

import com.school.util.DBConnection;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        if (!DBConnection.testConnection()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Database Error");
            alert.setHeaderText("Cannot connect to database");
            alert.setContentText(
                "Check that MySQL is running and db.properties is correct.\n" +
                "URL: " + DBConnection.getUrl()
            );
            alert.showAndWait();
            return;
        }

        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/com/school/fxml/login.fxml")
        );
        Scene scene = new Scene(loader.load(), 1000, 680);
        stage.setTitle("School Management System");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.centerOnScreen();
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}