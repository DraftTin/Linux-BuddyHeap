package code.setWindow;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import code.runWindow.*;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class setController implements  Initializable{
    private static setController instance;
    private Scene scene;
    @FXML private AnchorPane rootPane;
    @FXML private Button sureBtn;
    @FXML private ChoiceBox pageSizeBtn;
    @FXML private ChoiceBox spaceSizeBtn;

    private double xOffset;
    private double yOffset;


    public setController(){
        instance = this;
    }
    public static setController getInstance(){return instance;}

    public void minWindow(){
        setWindow.getPrimaryStage().setIconified(true);
    }
    public void closeSystem(){
        Platform.exit();
        System.exit(0);
    }
    public void sureButtonAction() throws IOException {
        //传递参数
        runController.setPageSize(pageSizeBtn.getValue().toString());
        runController.setSpaceSize(spaceSizeBtn.getValue().toString());
        //这个传递必须放在下面那个的前面

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/views/runWindow.fxml"));
        Parent window = (Pane)fxmlLoader.load();

        this.scene = new Scene(window);
        showScene();
    }

    public void showScene()throws  IOException{
        Platform.runLater(()->{
            Stage stage = (Stage)rootPane.getScene().getWindow();  //应该是随便找一个都可以
            stage.setResizable(true);
            stage.setWidth(1040);
            stage.setHeight(620);

            stage.setOnCloseRequest((WindowEvent e)->{  //不晓得这个有什么存在的必要
                Platform.exit();
                System.exit(0);
            });
            stage.setScene(this.scene);
            stage.setMinWidth(800);
            stage.setMinHeight(300);
            //ResizeHelper    不明白他弄这个干啥
            stage.centerOnScreen();

        });
    }


    //窗口拖拽
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        rootPane.setOnMousePressed(event -> {
            xOffset = setWindow.getPrimaryStage().getX() - event.getScreenX();
            yOffset = setWindow.getPrimaryStage().getY() - event.getScreenY();
            rootPane.setCursor(Cursor.CLOSED_HAND);
        });

        rootPane.setOnMouseDragged(event -> {
            setWindow.getPrimaryStage().setX(event.getScreenX() + xOffset);
            setWindow.getPrimaryStage().setY(event.getScreenY() + yOffset);

        });

        rootPane.setOnMouseReleased(event -> {
            rootPane.setCursor(Cursor.DEFAULT);
        });
    }


}