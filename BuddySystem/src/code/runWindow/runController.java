package code.runWindow;

import code.manager.BuddyHeapMgr;
import code.setWindow.setWindow;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import code.runWindow.*;

import java.io.IOException;
import java.net.URL;
import java.util.*;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
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

    public static void setSpaceSize(String spaceSize) {
        runController.spaceSize = spaceSize;
    }

    public void operationAction(){   //点击一次操作一次
        int operateIndex = 0;
        String option = operationChoice.getValue().toString();
        if(option.equals("申请")) operateIndex = 1;
        else if(option.equals("释放")) operateIndex = 2;
        //opeRecord.setText(opeRecord.getText() + buddyHeapMgr.run(operateIndex));  //记录操作信息
        opeRecord.appendText(buddyHeapMgr.run(operateIndex));
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
            Rectangle firstRec = new Rectangle();
            firstRec.setHeight(rectangleHBox.getPrefHeight());
            firstRec.setWidth(rectangleHBox.getPrefWidth());
            firstRec.setArcHeight(10);
            firstRec.setArcWidth(10);
            firstRec.setFill(Color.RED);
            rectangleHBox.getChildren().add(firstRec);
            Label start = new Label(0+"");
            //计算最后一位
            int tempSpace = Integer.parseInt(spaceSize.replace("MB",""));
            int tempPage = Integer.parseInt(pageSize.replace("KB",""));
            int lastNumber = tempSpace / tempPage * 1024;
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
            PageBlock newPageBlock = new PageBlock(temp.end+1,next.end-1,false);
            needAddPageBlocks.add(newPageBlock);
            //pageBlockTreeSet.add(newPageBlock);
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

            //取消间隔，保证上中下对应
//            Rectangle gap = new Rectangle();
//            gap.setOpacity(0);   //设置一个透明的间隔
//            gap.setHeight(51);
//            gap.setWidth(1);
//            //如果最小模式就别再间隔了
//            if(Integer.parseUnsignedInt(pageSize.replace("KB","")) == 1){
//                gap.setWidth(0);
//            }
            textHBox.getChildren().add(start);
//            textHBox.getChildren().add(gap);
            bottomHBox.getChildren().add(end);
//            gap.setWidth(gap.getWidth()-3);  //我也不知道上下一模一样为什么会差了3个像素
//            if(Integer.parseUnsignedInt(pageSize.replace("KB","")) == 1){
//                gap.setWidth(0);
//            }
//            bottomHBox.getChildren().add(gap);
        }
    }

    public int turnStringToOpt(String size){
        int res = 0;
        if(size.equals("256MB")) res = 1;
        else if(size.equals("512MB")) res = 2;
        else if(size.equals("1KB")) res = 1;
        else if(size.equals("2KB")) res = 2;
        else if(size.equals("4KB")) res = 3;
        else if(size.equals("256KB")) res = 4;
        return res;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
       // scrollPane.removeEventHandler(ScrollEvent.SCROLL,scrollPane.getOnScroll()); //想要消除滑动条自带的滚轮上下，不会
        oralHeight = scrollAnchor.getPrefHeight();
//        drawVBox.setPrefWidth(drawVBox.getPrefWidth()*3);
//        drawVBox.setPrefHeight(drawVBox.getPrefHeight()*3);
//        scrollAnchor.setPrefWidth(scrollAnchor.getPrefWidth()*3);
//        scrollAnchor.setPrefHeight(scrollAnchor.getPrefHeight()*3);
//        scrollPane.setVvalue(0.5);
//        drawVBox.setAlignment(Pos.CENTER);
//        textHBox.setPrefWidth(textHBox.getPrefWidth()*3);textHBox.setAlignment(Pos.TOP_CENTER);
//        rectangleHBox.setPrefWidth(rectangleHBox.getPrefWidth()*3);rectangleHBox.setAlignment(Pos.TOP_CENTER);
//        bottomHBox.setPrefWidth(bottomHBox.getPrefWidth()*3);bottomHBox.setAlignment(Pos.TOP_CENTER);
//        scrollPane.setHvalue(0.5);

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

        //监听按键
        rootPane.setOnKeyPressed(e -> pressedKeys.add(e.getCode()));
        rootPane.setOnKeyReleased(e -> pressedKeys.remove(e.getCode()));

//        //取消自带的滚轮上下，全靠拖动
//        scrollPane.removeEventHandler(ScrollEvent.SCROLL,scrollPane.getOnScroll());



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

//        //这种拖拽只能拖拽单个结点，对于scrollPane来说没什么用   不需要，有自带的
//        //所以我尝试用拖拽来控制bar
//        scrollAnchor.addEventHandler(MouseEvent.MOUSE_PRESSED,event -> { //提示用户可拖拽
//            scrollAnchor.setCursor(Cursor.MOVE);
//            //当按压事件发生时，缓存事件发生的位置坐标
//            posX = event.getX();
//            posY = event.getY();
//        });
//        scrollAnchor.addEventHandler(MouseEvent.MOUSE_RELEASED,event -> scrollAnchor.setCursor(Cursor.DEFAULT));
//        //拖完后恢复原样
//        scrollAnchor.addEventHandler(MouseEvent.MOUSE_RELEASED,event -> {
//            double distanceX = event.getX() - posX;
//            double distanceY = event.getY() - posY;
//
//            double changeX = distanceX / scrollPane.getWidth();
//            double changeY = distanceY / scrollPane.getHeight();
//
//            scrollPane.setHvalue(scrollPane.getHvalue()-changeX);
//            scrollPane.setVvalue(scrollPane.getVvalue()-changeY);
//        });

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
    }
}
