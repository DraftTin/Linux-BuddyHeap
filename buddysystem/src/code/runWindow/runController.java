package code.runWindow;

import code.manager.BuddyHeapMgr;
import code.setWindow.setWindow;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;

import java.net.URL;
import java.util.*;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;
import javafx.scene.shape.*;

public class runController implements Initializable{
    @FXML private AnchorPane scrollAnchor;
    @FXML private ScrollPane scrollPane;
    @FXML private Label spaceSizeLab;
    @FXML private Label pageSizeLab;
    @FXML private ChoiceBox operationChoice;
    @FXML private TabPane tabPane;
    @FXML private Tab tab0;
    @FXML private TextArea text0;
    @FXML private TextArea opeRecord;
    @FXML private HBox textHBox;
    @FXML private HBox rectangleHBox;
    @FXML private HBox bottomHBox;
    @FXML private AnchorPane rootPane;
    @FXML private VBox drawVBox;
    @FXML private TextArea inputText;
    private double posX = 0;
    private double posY = 0;
    private int pageNum = 0; //页数

    private static String pageSize;
    private static String spaceSize;
    private BuddyHeapMgr buddyHeapMgr;

    private int operateNum = 0;   //用来记录操作次数
    private Set<KeyCode> pressedKeys = new HashSet<>();  //用来记录当前按下的键
    private double VBarValue;

    private final double vBarValue = 0.16985138004246284; //运行得来的数据，贼傻。换个尺寸就不对了
    private  double oralHeight = 1;

    private double scrollHeight; //记录画板原大小
    private double scrollWidth;
    private double scrollScaleX;
    private double scrollScaleY;

    private double xOffset;
    private double yOffset;


    public static String getPageSize() {
        return pageSize;
    }

    public static void setPageSize(String pageSize) {
        runController.pageSize = pageSize;
    }

    public static String getSpaceSize() {
        return spaceSize;
    }

    public void minWindow(){
        setWindow.getPrimaryStage().setIconified(true);
    }

    public void closeSystem(){
        Platform.exit();
        System.exit(0);
    }
    public void askInfo(){
        Stage stage = new Stage();
        Label l = new Label();
        l.setPrefSize(1920,1080);
        l.setAlignment(Pos.CENTER);
        Image image;
        image = new Image(getClass().getResourceAsStream("../../resources/imgs/info5.png"));
        l.setGraphic(new ImageView(image));
        Scene s = new Scene(l,1920,1080);
        stage.setScene(s);
        stage.show();
    }

    public static void setSpaceSize(String spaceSize) {
        runController.spaceSize = spaceSize;
    }

    public void operationAction(){   //点击一次操作一次
        int operateIndex = 0;
        String option = operationChoice.getValue().toString();
        if(option.equals("申请随机页框数")){
            operateIndex = 1;
            opeRecord.appendText((operateNum+1)+":"+buddyHeapMgr.run(operateIndex));
        }
        else if(option.equals("释放随机页框号")){
            operateIndex = 2;
            opeRecord.appendText((operateNum+1)+":"+buddyHeapMgr.run(operateIndex));
        }else if(option.equals("申请指定页框数")){
            try {
                if(Integer.parseInt(inputText.getText()) <= 0 || inputText.getText().isEmpty()){
                    inputText.setText("");
                    inputText.setPromptText("请输入大于0的整数!");
                    return;
                }
            }catch (NumberFormatException e){
                inputText.setText("");
                inputText.setPromptText("请输入大于0的整数!");
                return;
            }
            int size = Integer.parseUnsignedInt(inputText.getText());
            opeRecord.appendText((operateNum+1)+":"+buddyHeapMgr.manualAllocate(size));
        }else if(option.equals("释放指定页框号")){
            try {
                if(Integer.parseInt(inputText.getText()) <= 0 || inputText.getText().isEmpty()){
                    inputText.setText("");
                    inputText.setPromptText("请输入大于0的整数!");
                    return;
                }
            }catch (NumberFormatException e){
                inputText.setText("");
                inputText.setPromptText("请输入大于0的整数!");
                return;
            }
            int pageNum = Integer.parseUnsignedInt(inputText.getText());
            opeRecord.appendText((operateNum+1)+":"+buddyHeapMgr.manualFree(pageNum));
        }else if(option.equals("释放随机整块空间")){
            opeRecord.appendText((operateNum+1)+":"+buddyHeapMgr.randomFreeBlock());
        }

        operateNum++;

        drawState();

        //生成并添加一个新的tab
        Tab newTab = new Tab();
        newTab.setId("tab"+operateNum);
        newTab.setText(operateNum+"");
        AnchorPane newAnchorPane = new AnchorPane();
        newAnchorPane.setPrefHeight(255); newAnchorPane.setPrefWidth(275);
        newAnchorPane.setMinHeight(0); newAnchorPane.setMinWidth(0);
        TextArea newText  = new TextArea();
        newText.setId("text"+operateNum);
        newText.setPrefHeight(320); newText.setPrefWidth(294);
        newText.setText(buddyHeapMgr.returnFreeArea());
        newAnchorPane.getChildren().add(newText);
        newTab.setContent(newAnchorPane);
        tabPane.getTabs().add(newTab);
        tabPane.getSelectionModel().select(newTab);

    }

    public void backBtnAction(){    //恢复画板
        scrollAnchor.getTransforms().clear();
        scrollAnchor.setPrefSize(scrollWidth,scrollHeight);
    }

    private  class PageBlock implements Comparable<PageBlock>{ //连续的同状态的块
        public int start = 0;
        public int end = 0;
        public boolean ifFree = true;  //判断是否空闲
        public PageBlock(int start,int end,boolean ifFree){
            this.start = start;
            this.end = end;
            this.ifFree = ifFree;
        }
        @Override
        public int compareTo(PageBlock o) {   //TreeSet从小到大
            PageBlock one = (PageBlock) o;
            if(this.start < one.start) return -1;
            else if(this.start == one.start) return 0;
            else if(this.start > one.start) return 1;
            return 0;
        }
    }

    public void emptyDraw(){    //把整个绘图板的组件清空
        while(!rectangleHBox.getChildren().isEmpty()){
            rectangleHBox.getChildren().remove(0);
        }
        while(!textHBox.getChildren().isEmpty()){
            textHBox.getChildren().remove(0);
        }
        while(!bottomHBox.getChildren().isEmpty()){
            bottomHBox.getChildren().remove(0);
        }
    }

    private int countNumber(int input){ //计算一个整数的位数
        int res = 1;
        while(input / 10 != 0){
            input = input / 10;
            res++;
        }
        return res;
    }
    public void drawState(){
        emptyDraw();  //画之前先清空

        TreeSet<PageBlock> pageBlockTreeSet = new TreeSet<>();
        //接下来把FreeArea放到pageBlockTreeSet来
        for(int i = 0;i < buddyHeapMgr.getFreeArea().size();i++){
            if(buddyHeapMgr.getFreeArea().get(i).isEmpty()){
                continue;
            }
            for(int j = 0;j < buddyHeapMgr.getFreeArea().get(i).size();++j){
                int start = buddyHeapMgr.getFreeArea().get(i).get(j);
                int end = start + (int)Math.pow(2,i) - 1;
                PageBlock pageBlock = new PageBlock(start,end,true);
                pageBlockTreeSet.add(pageBlock);
            }
        }
        //如果一个空闲块都没有的话，就画一整个红色的
        if(pageBlockTreeSet.size() == 0){
            //计算最后一位
            int tempSpace = Integer.parseInt(spaceSize.replace("MB",""));
            int tempPage = Integer.parseInt(pageSize.replace("KB",""));
            int lastNumber = tempSpace / tempPage * 1024;
            Rectangle firstRec = new Rectangle();
            firstRec.setHeight(rectangleHBox.getPrefHeight());
            firstRec.setWidth(rectangleHBox.getPrefWidth());
            firstRec.setArcHeight(10);
            firstRec.setArcWidth(10);
            firstRec.setFill(Color.RED);
            //添加一个点击此块就弹出起始和终止信息的功能
            String tempS = "起始位:"+0+" 终止位:"+lastNumber+"占用";
            firstRec.setAccessibleText(tempS);
            firstRec.addEventHandler(MouseEvent.MOUSE_CLICKED,event -> {
                Stage stage = new Stage();
                Label l = new Label(firstRec.getAccessibleText());
                l.setAlignment(Pos.CENTER);
                Scene s = new Scene(l,200,100);
                stage.setScene(s);
                stage.show();
            });
            rectangleHBox.getChildren().add(firstRec);
            Label start = new Label(0+"");
            Label end = new Label(lastNumber+"");
            //************样式信息*****************//
            start.setFont(Font.font(20));start.setWrapText(true);start.setPrefWidth(firstRec.getWidth());start.setPrefHeight(textHBox.getPrefHeight());
            end.setFont(Font.font(20));end.setWrapText(true);end.setPrefWidth(firstRec.getWidth());end.setPrefHeight(bottomHBox.getPrefHeight());
            start.setStyle("-fx-border-style: solid inside;" +
                    "-fx-border-width: 1;" +
                    "-fx-border-insets: 1;" +
                    "-fx-border-radius: 3;" +
                    "-fx-border-color: #0aebff");
            end.setStyle("-fx-border-style: solid inside;" +
                    "-fx-border-width: 1;" +
                    "-fx-border-insets: 1;" +
                    "-fx-border-radius: 3;" +
                    "-fx-border-color: #ff520c;");
            //************样式信息*****************//
            textHBox.getChildren().add(start);
            bottomHBox.getChildren().add(end);
            return;
        }


        //接下来由空闲区来互补生成占用区。
        //如果第一个不是从0开始的，那么就把第一个作为忙碌加进去
        if(pageBlockTreeSet.first().start != 0){
            PageBlock temp = new PageBlock(0,pageBlockTreeSet.first().start-1,false);
            pageBlockTreeSet.add(temp);
        }
        //因为不能一边迭代一边修改，所以只好先把需要加的pageblock存起来，迭代后一次性加入。
        TreeSet<PageBlock> needAddPageBlocks = new TreeSet<>();
        for(PageBlock temp: pageBlockTreeSet){  //这里报出异常了，不能在迭代的同时对其修改
            if(temp.equals(pageBlockTreeSet.last())) break;  //最后一个跳出循环
            PageBlock next = pageBlockTreeSet.higher(temp);
            if(temp.end == next.start -1) continue;
            PageBlock newPageBlock = new PageBlock(temp.end+1,next.start-1,false);
            needAddPageBlocks.add(newPageBlock);
        }
        //一次全部加进入
        for(PageBlock temp: needAddPageBlocks){
            pageBlockTreeSet.add(temp);
        }
        //单独处理最后一个区域
        if(pageBlockTreeSet.last().end != pageNum - 1){
            PageBlock lastOne = new PageBlock(pageBlockTreeSet.last().end + 1,pageNum - 1,false);
            pageBlockTreeSet.add(lastOne);
        }

        //准备工作完毕，正式开始依据TreeSet画图
        //先画下面的矩形,顺带把上面的Text也画了
        for(PageBlock one : pageBlockTreeSet){
            //这个是中间的矩形
            Rectangle newRec = new Rectangle();
            newRec.setHeight(rectangleHBox.getPrefHeight());
            double percentage = (double)(one.end-one.start)/pageNum;
            newRec.setWidth(percentage * rectangleHBox.getPrefWidth());
            newRec.setArcHeight(newRec.getWidth()/10);  //弧度值
            newRec.setArcWidth(newRec.getArcHeight());
            if(one.ifFree){
                newRec.setFill(Color.LIMEGREEN);
            }else{
                newRec.setFill(Color.RED);
            }
            newRec.setStroke(Color.BLACK);
            newRec.setStrokeType(StrokeType.INSIDE);
            //添加一个点击此块就弹出起始和终止信息的功能
            String tempS = "起始位:"+one.start+" 终止位:"+one.end;
            if(one.ifFree) tempS += " 空闲";
            else tempS += " 占用";
            newRec.setAccessibleText(tempS);
            newRec.addEventHandler(MouseEvent.MOUSE_CLICKED,event -> {
                Stage stage = new Stage();
                Label l = new Label(newRec.getAccessibleText());
                l.setAlignment(Pos.CENTER);
                Scene s = new Scene(l,200,100);
                stage.setScene(s);
                stage.show();
            });

            rectangleHBox.getChildren().add(newRec);

            //然后画上下两个起始数字块
            double font_size = 18;   //默认字体大小为18
            double limit1 = newRec.getWidth();  //字体大小不能超出这两个
            double limit2 = textHBox.getPrefHeight() / countNumber(one.start) - 6;  //很少会有边界点，考虑一个就行了
            if (font_size > Math.min(limit1,limit2)) font_size = Math.min(limit1,limit2);

            Label start = new Label(one.start+"");
            Label end = new Label(one.end+"");
            //************样式信息*****************//
            start.setAlignment(Pos.CENTER);start.setFont(Font.font(font_size));start.setWrapText(true);start.setPrefWidth(newRec.getWidth());start.setPrefHeight(textHBox.getPrefHeight());
            end.setAlignment(Pos.CENTER);end.setFont(Font.font(font_size));end.setWrapText(true);end.setPrefWidth(newRec.getWidth());end.setPrefHeight(bottomHBox.getPrefHeight());
            start.setStyle("-fx-border-style: solid inside;" +
                    "-fx-border-width: 1;" +
                    "-fx-border-insets: 0;" +
                    "-fx-border-radius: 2;" +
                    "-fx-border-color: #0aebff");
            end.setStyle("-fx-border-style: solid inside;" +
                    "-fx-border-width: 1;" +
                    "-fx-border-insets: 0;" +
                    "-fx-border-radius: 2;" +
                    "-fx-border-color: #ff520c;");
            //************样式信息*****************//

            textHBox.getChildren().add(start);
            bottomHBox.getChildren().add(end);
        }
    }

    public int turnStringToOpt(String size){
        int res = 0;
        if(size.equals("256MB")) res = 1;
        else if(size.equals("512MB")) res = 2;
        else if(size.equals("64KB")) res = 1;
        else if(size.equals("128KB")) res = 2;
        else if(size.equals("256KB")) res = 3;
        else if(size.equals("512KB")) res = 4;
        return res;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // scrollPane.removeEventHandler(ScrollEvent.SCROLL,scrollPane.getOnScroll()); //想要消除滑动条自带的滚轮上下，不会
        oralHeight = scrollAnchor.getPrefHeight();

        //记录画板原大小
        scrollHeight = scrollAnchor.getPrefHeight();
        scrollWidth = scrollAnchor.getPrefWidth();
        scrollScaleX = scrollAnchor.getScaleX();
        scrollScaleY = scrollAnchor.getScaleY();

        BuddyHeapMgr.Config config = new BuddyHeapMgr.Config();
        config.init(turnStringToOpt(spaceSize),turnStringToOpt(pageSize));
        spaceSizeLab.setText(spaceSize);
        pageSizeLab.setText(pageSize);
        int spaceInt = Integer.parseUnsignedInt(spaceSize.replace("MB",""));
        int pageInt = Integer.parseUnsignedInt(pageSize.replace("KB",""));
        pageNum = spaceInt * 1024 /pageInt;


        buddyHeapMgr = new BuddyHeapMgr(config);
        buddyHeapMgr.init();
        buddyHeapMgr.showFreeArea();
        text0.setText(buddyHeapMgr.returnFreeArea());

        drawState();

        //监听缩放事件
        scrollAnchor.addEventHandler(ScrollEvent.SCROLL,event -> { //滚轮缩放
            double rate = 0;
            if(event.getDeltaY() > 0){
                rate = 0.05;
            }else{
                rate = -0.05;
            }
            if(pressedKeys.contains(KeyCode.ALT)){  //只有按了alt再滚动才有效
                double newWidth = scrollAnchor.getWidth()*(1+rate);
                double newHeight = scrollAnchor.getHeight()*(1+rate);
                scrollAnchor.setPrefSize(newWidth,newHeight);

                double mouseX = event.getX();
                double mouseY = event.getY();
                Scale scale = new Scale(1+rate,1+rate,mouseX,mouseY);
                scrollAnchor.getTransforms().add(scale);
                //这里还要加一个判断，确定按钮的上限和下限。


            }
        });
        scrollAnchor.addEventHandler(ScrollEvent.SCROLL,event -> {
            //我找不到取消自带鼠标滚动的方法，所以自己弄一个固定的
            //System.out.println(event.getDeltaY() + " || "+scrollPane.getVvalue() +" || "+scrollAnchor.getPrefHeight());
            double rate = 0;
            if(event.getDeltaY() > 0){
                rate = 0.05;
            }else{
                rate = -0.05;
            }
            if(rate > 0 ){ //如果大于0，说明是往上滚的，V会变小
                scrollPane.setVvalue(scrollPane.getVvalue() + vBarValue/(scrollAnchor.getHeight()/oralHeight));
            }else {
                scrollPane.setVvalue(scrollPane.getVvalue() - vBarValue/(scrollAnchor.getHeight()/oralHeight));
            }
        });


        //窗口拖拽
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

        //选项控件
        operationChoice.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                if(newValue.equals("申请随机页框数") || newValue.equals("释放随机页框号") || newValue.equals("释放随机整块空间")){
                    inputText.setVisible(false);
                    inputText.setText(""); //清空上次的输入
                }
                if(newValue.equals("申请指定页框数")){
                    inputText.setVisible(true);
                    inputText.setPromptText("请输入页框数");
                }else if(newValue.equals("释放指定页框号")){
                    inputText.setVisible(true);
                    inputText.setPromptText("请输入页框号");
                }
            }
        });

        //全局按钮
        //enter 执行一次
        //esc退出
        rootPane.setOnKeyPressed(event -> {
            if(event.getCode() == KeyCode.ENTER){
                operationAction();
            }
            if(event.getCode() == KeyCode.ESCAPE){
                System.exit(0);
            }
            pressedKeys.add(event.getCode());
        });
        rootPane.setOnKeyReleased(e -> pressedKeys.remove(e.getCode()));
    }
}
