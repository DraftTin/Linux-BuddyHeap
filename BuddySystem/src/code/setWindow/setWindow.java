package code.setWindow;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import code.manager.BuddyHeapMgr;

public class setWindow extends Application {
    private static Stage primaryStageObj;

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStageObj = primaryStage;
        Parent root = FXMLLoader.load(getClass().getClassLoader().getResource("views/setWindow.fxml"));
        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.setTitle("Buddy System");
        primaryStage.getIcons().add(new Image(getClass().getClassLoader().getResource("imgs/icon.jpg").toString()));
        Scene mainScene = new Scene(root, 540, 360);
        primaryStage.setResizable(false);
        primaryStage.setScene(mainScene);
        primaryStage.show();
        primaryStage.setOnCloseRequest(e -> Platform.exit());
    }


    public static void main(String[] args) {

        launch(args);
    }

    public static Stage getPrimaryStage() {
        return primaryStageObj;
    }
}
