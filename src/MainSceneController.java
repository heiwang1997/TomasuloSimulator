import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import tableElements.RSRow;

import java.net.URL;
import java.util.ResourceBundle;

public class MainSceneController implements Initializable {

    public Label clockLabel;
    public Label clockTimeLabel;
    public TableView<RSRow> rsTableView;

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

    }

    @FXML
    void startClicked(ActionEvent event) {

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
}

