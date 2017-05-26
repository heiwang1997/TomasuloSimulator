import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.Pair;
import tableElements.CodeRow;
import tableElements.RSRow;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class MainSceneController implements Initializable {

    public Label clockLabel;
    public Label clockTimeLabel;
    public TableView<RSRow> rsTableView;
    public TableView<CodeRow> codeTableView;

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
        for (String line : codeLines) {
            line = line.trim();
            if (line.startsWith("#") || line.startsWith("//")) {
                continue;
            }
            if (line.matches(".*\\w.*")) {
                pipeline.addCmd(line);
            }
        }
        int parseResult = pipeline.parser();
        if (parseResult == -1) {
            showAlert(Alert.AlertType.ERROR, "错误", "语法错误", String.format("%s", "Error Msg."));
            return false;
        } else {
            return true;
        }
    }

    @FXML
    void startClicked(ActionEvent event) {
        switchStatus(true);
    }

    @FXML
    void stepClicked(ActionEvent event) {

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

        switchStatus(false);

        pipeline = new Pipeline();

        refreshReservationStation();
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
        } else {
            inputButton.setDisable(false);
            sampleButton.setDisable(false);
            startButton.setText("开始模拟");
            setClockTime(0);
            stepButton.setDisable(true);
            continueButton.setDisable(true);
            tailButton.setDisable(true);
        }
    }

    private void refreshReservationStation() {
        ObservableList<RSRow> a = FXCollections.observableArrayList(
                new RSRow("1"), new RSRow("2")
        );
        rsTableView.getItems().setAll(a);
    }

    private void refreshCodeSection() {
        ObservableList<CodeRow> a = FXCollections.observableArrayList();
        for (Cmd cmd : pipeline.decodedList) {
            CodeRow cRow = new CodeRow(a.size() + 1, "Code");
            cRow.setSt1("OK");
            a.add(cRow);
        }
        codeTableView.getItems().setAll(a);
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
}
