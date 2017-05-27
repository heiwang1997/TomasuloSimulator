import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.Pair;
import tableElements.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Optional;
import java.util.ResourceBundle;

public class MainSceneController implements Initializable {

    private final int SAMPLE_COUNT = 2;
    private final String[] RS_NAME = {"ADD", "MULT", "LOAD", "STORE"};
    private final String[] OP_NAME = {"ADD", "SUB", "MUL", "DIV"};

    public Label clockLabel;
    public Label clockTimeLabel;
    public TableView<RSRow> rsTableView;
    public TableView<CodeRow> codeTableView;
    public TableView<FPRegRow> fpRegTableView;
    public TableView<CPURegRow> cpuRegTableView;
    public TableView<MemRow> memTableView;
    public Menu changeMenu;

    private int currentTime;

    @FXML
    private Button inputButton;

    @FXML
    private MenuButton sampleButton;

    @FXML
    private Button startButton;

    @FXML
    private Button stepButton;

    @FXML
    private Button continueButton;

    @FXML
    private Button tailButton;

    private Pipeline pipeline;

    @FXML
    void continueClicked(ActionEvent event) {

    }

    @FXML
    void inputClicked(ActionEvent event) {
        String inputCode = "";
        while (true) {
            // Create the custom dialog.
            Dialog<String> dialog = new Dialog<>();
            dialog.setTitle("输入指令");

            // Set the button types.
            ButtonType loginButtonType = new ButtonType("确定", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

            TextArea tArea = new TextArea(inputCode);
            tArea.setPromptText("请在这里输入代码");
            BorderPane borderPane = new BorderPane(tArea);
            BorderPane.setMargin(borderPane, new Insets(20, 20, 20, 20));

            dialog.getDialogPane().setContent(borderPane);

            // Convert the result to a username-password-pair when the login button is clicked.
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == loginButtonType) {
                    return tArea.getText();
                }
                return null;
            });

            Optional<String> result = dialog.showAndWait();

            if (result.isPresent()) {
                inputCode = result.get();
                if (inputCode(inputCode)) {
                    refreshCodeSection();
                    break;
                }
            } else {
                return;
            }
        }
    }

    private boolean inputCode(String code) {
        String[] codeLines = code.split("\\r?\\n");
        // Cache current pipeline code
        ArrayList<String> cachedList = new ArrayList<>(pipeline.cmdList.size());
        cachedList.addAll(pipeline.cmdList);
        pipeline.cmdList.clear();
        for (String line : codeLines) {
            line = line.trim();
            if (line.startsWith("#") || line.startsWith("//")) {
                continue;
            }
            if (line.matches(".*\\w.*")) {
                System.out.println(line);
                pipeline.addCmd(line);
            }
        }
        int parseResult = pipeline.parser();
        if (parseResult == -1) {
            showAlert(Alert.AlertType.ERROR, "错误", "语法错误", pipeline.parseErrMessage);
            pipeline.cmdList.clear();
            pipeline.cmdList.addAll(cachedList);
            pipeline.parser();
            return false;
        } else {
            return true;
        }
    }

    @FXML
    void startClicked(ActionEvent event) {
        if (pipeline.isRunning) {
            // TODO: Fix this.
            pipeline.isRunning = false;
            currentTime = 0;
            switchStatus(false);
        } else {
            if (pipeline.decodedList.size() == 0) {
                showAlert(Alert.AlertType.ERROR, "错误", "操作错误",
                        "当前指令队列中没有指令，您可以考虑输入指令或使用示例指令。");
                return;
            }
            currentTime = 0;
            pipeline.run();
            switchStatus(true);
        }
    }

    @FXML
    void stepClicked(ActionEvent event) {
        pipeline.nextStep();
        currentTime += 1;
        refreshAll();
    }

    @FXML
    void tailClicked(ActionEvent event) {

    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        inputButton.setGraphic(new ImageView("/resources/input.png"));
        sampleButton.setGraphic(new ImageView("/resources/sample.png"));
        startButton.setGraphic(new ImageView("/resources/simulate.png"));
        stepButton.setGraphic(new ImageView("/resources/step.png"));
        continueButton.setGraphic(new ImageView("/resources/continue.png"));
        tailButton.setGraphic(new ImageView("/resources/tail.png"));

        clockLabel.setGraphic(new ImageView("/resources/time.png"));
        memTableView.setPlaceholder(new Label("内存中所有值均为0"));
        codeTableView.setPlaceholder(new Label("当前没有任何指令，点击左侧的输入指令或示例指令进行添加"));

        switchStatus(false);

        pipeline = new Pipeline();

        refreshAll();

        for (int i = 0; i < SAMPLE_COUNT; ++ i) {
            MenuItem sampleItem = new MenuItem("示例" + String.valueOf(i + 1));
            int finalI = i;
            sampleItem.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    InputStream in = getClass().getResourceAsStream(String.format("/samples/s%d.txt", finalI + 1));
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    try {
                        String line = reader.readLine();
                        StringBuilder sb = new StringBuilder();
                        while (line != null) {
                            sb.append(line).append("\n");
                            line = reader.readLine();
                        }
                        if (inputCode(sb.toString())) {
                            // TODO: Deduplicate this code.
                            refreshCodeSection();
                        }
                    } catch (IOException e) {
                        showAlert(Alert.AlertType.ERROR, "错误", "内部错误", "示例不可用，请直接输入代码");
                    }
                }
            });
            sampleButton.getItems().add(sampleItem);
        }
    }

    private void setClockTime(int time) {
        clockTimeLabel.setText(String.valueOf(time));
    }

    private void switchStatus(boolean isRunning) {
        if (isRunning) {
            inputButton.setDisable(true);
            sampleButton.setDisable(true);
            startButton.setText("结束模拟");
            stepButton.setDisable(false);
            continueButton.setDisable(false);
            tailButton.setDisable(false);
            changeMenu.setDisable(true);
        } else {
            inputButton.setDisable(false);
            sampleButton.setDisable(false);
            startButton.setText("开始模拟");
            setClockTime(0);
            stepButton.setDisable(true);
            continueButton.setDisable(true);
            tailButton.setDisable(true);
            fpRegTableView.setEditable(true);
            changeMenu.setDisable(false);
        }
    }

    private void refreshAll() {
        refreshReservationStation();
        refreshCodeSection();
        refreshRegisters();
        refreshMemory();
        setClockTime(currentTime);
    }

    private void refreshReservationStation() {
        ObservableList<RSRow> a = FXCollections.observableArrayList();
        for (int i = 0; i < 2; ++ i) {
            for (int j = 0; j < 3; ++ j) {
                String rsName = RS_NAME[i] + String.valueOf(j + 1);
                RSRow rRow = new RSRow(rsName);
                TMLBuffer thisBuffer = pipeline.buffers[i][j];
                if (thisBuffer.busy) {
                    rRow.setBusy("Yes");
                    int op = thisBuffer.operator;
                    rRow.setOp(OP_NAME[i * 2 + op]);
                } else {
                    rRow.setBusy("No");
                }
                a.add(rRow);
            }
        }
        rsTableView.getItems().setAll(a);
    }

    private void refreshCodeSection() {
        ObservableList<CodeRow> a = FXCollections.observableArrayList();
        for (Cmd cmd : pipeline.decodedList) {
            CodeRow cRow = new CodeRow(a.size() + 1, cmd.text);
            if (cmd.state >= 1) cRow.setSt1("OK");
            if (cmd.state >= 2) cRow.setSt2("OK");
            if (cmd.state >= 3) cRow.setSt3("OK");
            a.add(cRow);
        }
        codeTableView.getItems().setAll(a);
    }

    private void refreshRegisters() {
        ObservableList<CPURegRow> a = FXCollections.observableArrayList();
        for (int i = 0; i < 8; ++ i) {
            CPURegRow cRow = new CPURegRow("R" + String.valueOf(i + 1));
            cRow.setValue(String.valueOf(pipeline.cpuRegisters[i]));
            a.add(cRow);
        }
        cpuRegTableView.getItems().setAll(a);
        ObservableList<FPRegRow> b = FXCollections.observableArrayList();
        for (int i = 0; i < 30; ++ i) {
            FPRegRow fRow = new FPRegRow("F" + String.valueOf(i + 1));
            fRow.setValue(String.valueOf(pipeline.fpRegisters[i]));
            String rsName;
            if (pipeline.fpRegistersStatus[i][0] == -1) {
                // if Qi is empty
                rsName = "";
            } else {
                // if this register is going to be filled by an RS, show it.
                rsName = RS_NAME[pipeline.fpRegistersStatus[i][0]] +
                        String.valueOf(pipeline.fpRegistersStatus[i][1] + 1);
            }
            fRow.setRs(rsName);
            b.add(fRow);
        }
        fpRegTableView.getItems().setAll(b);
    }

    private void refreshMemory() {
        ObservableList<MemRow> a = FXCollections.observableArrayList();
        for (int i = 0; i < 4096; ++ i) {
            if (pipeline.memory[i] != 0.0) {
                MemRow memRow = new MemRow();
                memRow.setOffset(String.valueOf(i));
                memRow.setValue(String.valueOf(pipeline.memory[i]));
                a.add(memRow);
            }
        }
        memTableView.getItems().addAll(a);
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public void exitClicked(ActionEvent actionEvent) {
        System.exit(0);
    }

    public void aboutClicked(ActionEvent actionEvent) {
        showAlert(Alert.AlertType.INFORMATION, "信息", "关于本项目",
                "本项目是计算机系统结构课程的第二次大作业，内容是Tomasulo算法模拟器。");
    }

    public void registerChangeClicked(ActionEvent actionEvent) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("更改寄存器");
        dialog.setHeaderText("输入寄存器名以更改寄存器，以R开头的为CPU通用寄存器，以F开头的为浮点寄存器");

        // Set the button types.
        ButtonType loginButtonType = new ButtonType("确定", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        TextArea tArea = new TextArea();
        tArea.setPromptText("请在这里输入代码");
        BorderPane borderPane = new BorderPane(tArea);
        BorderPane.setMargin(borderPane, new Insets(20, 20, 20, 20));

        dialog.getDialogPane().setContent(borderPane);

        // Convert the result to a username-password-pair when the login button is clicked.
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return tArea.getText();
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();

        if (result.isPresent()) {
            System.out.println("Good");
        }
    }

    public void memoryChangeClicked(ActionEvent actionEvent) {

    }
}
