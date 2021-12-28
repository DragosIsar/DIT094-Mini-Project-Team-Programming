package com.gui.bankwiser.Controllers;

import com.gui.bankwiser.BankWiserApp;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Optional;

public class NewPasswordCustomerController {

    @FXML
    private Button confirmNewPassword;

    @FXML
    private Button cancelNewPassword;

    @FXML
    private void onConfirmNewPasswordClicked() throws IOException {
        Alert alertBox = new Alert(Alert.AlertType.CONFIRMATION);
        alertBox.setContentText("Your password is updated successfully.");
        Optional<ButtonType> result = alertBox.showAndWait();
        if (result.get() == ButtonType.OK) {
            // probably not implement anything here
        }
    }

    @FXML
    public void handleCloseNewPasswordAction(ActionEvent event) {
        Stage stage = (Stage) cancelNewPassword.getScene().getWindow();
        stage.close();
    }




}