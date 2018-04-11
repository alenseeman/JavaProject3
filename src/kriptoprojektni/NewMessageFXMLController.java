/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kriptoprojektni;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javax.imageio.ImageIO;

/**
 * FXML Controller class
 *
 * @author Semanic
 */
public class NewMessageFXMLController implements Initializable {
    
    @FXML
    private Label lblNewMessage;
    @FXML
    private Label lblUser;
    @FXML
    private Label lblMessage;
    @FXML
    private Label lblPicture;
    @FXML
    private ComboBox<String> cmbbxUsers;
    @FXML
    private Button btnFile;
    @FXML
    private Button bntSend;
    
    private String filePath;
    @FXML
    private TextArea msgbxMessage;
    @FXML
    private Label lblProblem;
    @FXML
    private Label lblPictureName;

    /**
     * Initializes the controller class.
     * 
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // insert other users in combobox
        ObservableList<String> otherUsers = FXCollections.observableArrayList();
        ArrayList<String> users = new ArrayList<>(FXMLDocumentController.users.keySet());
        for (int i = 0; i < users.size(); i++) {
            if (!users.get(i).equals(FXMLDocumentController.activeUser)) {
                otherUsers.add(users.get(i));
            }
        }
        cmbbxUsers.getItems().addAll(otherUsers);
        // btnFile.disableProperty().bind(Bindings.isEmpty(msgbxMessage.textProperty()));
    }
    
    
    // add picture 
    @FXML
    private void handleButtonAddPictureAction(ActionEvent event) {
        FileChooser fc = new FileChooser();
        configureFileChooser(fc);
        File choosenFile = fc.showOpenDialog(null);
        if (choosenFile != null) {
            filePath = choosenFile.getAbsolutePath();
            File file = new File(filePath);
            String simpleFileName = file.getName();
            lblPictureName.setText(simpleFileName);
            if (isImage(filePath.toLowerCase())) {
                lblProblem.setText("");
                bntSend.setDisable(false);
                
            } else {
                lblProblem.setText("Format of picture is not good!");
                bntSend.setDisable(true);
            }
            
        }
    }
    
    private static void configureFileChooser(
        final FileChooser fileChooser) {
        fileChooser.setTitle("Insert Picture");
        fileChooser.getExtensionFilters().addAll(
        new FileChooser.ExtensionFilter("All Images", "*.*"),
        new FileChooser.ExtensionFilter("JPG", "*.jpg"),
        new FileChooser.ExtensionFilter("PNG", "*.png")
        );
    }
    
    private static boolean isImage(String filePath) {
        File f = new File(filePath);
        try {
            return ImageIO.read(f) != null;
        } catch (IOException ex) {
            return false;
        }
    }
    
    @FXML
    private void handleSendMessageAction(ActionEvent event) {
        int messageLength = msgbxMessage.getText().length();
        if (cmbbxUsers.getSelectionModel().getSelectedItem() != null && messageLength != 0 && filePath != null) {
            if (Util.checkCertificate(cmbbxUsers.getSelectionModel().getSelectedItem())) {
                if (Util.checkCertificate(FXMLDocumentController.activeUser)) {
                    BufferedImage cover_image = null;
                    int img_size = 0;
                    boolean ioProblem = false;
                    try {
                        cover_image = ImageIO.read(new File(filePath));
                    } catch (IOException e) {
                        ioProblem = true;
                        lblProblem.setText("Problem with loading picture!");
                    }
                    if (!ioProblem) {
                        String recipient = cmbbxUsers.getSelectionModel().getSelectedItem();
                        SteganographyImage stegoImage = new SteganographyImage(FXMLDocumentController.activeUser, recipient, msgbxMessage.getText(), cover_image);
                        if (!stegoImage.createSteganographyImage()) {
                            try {
                                String hash = Util.hashSHA256(new File(stegoImage.getImagePath()));
                                MainFormController.hashedUserData.get(recipient).add(hash);
                                MainFormController.writeHashData();
                                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                alert.setTitle("Success");
                                alert.setHeaderText(null);
                                alert.setContentText("Your message has been successfully sent !");
                                alert.showAndWait();
                                Stage primaryStage = (Stage) btnFile.getScene().getWindow();
                                primaryStage.close();
                            } catch (Exception e) {
                                Alert alert = new Alert(AlertType.INFORMATION);
                                alert.setTitle("Error");
                                alert.setHeaderText("Cannot send message");
                                alert.setContentText("Your message could not be sent!");
                                alert.showAndWait();
                            }
                        }
                    }
                } else {
                    System.err.println("Certificate is not valid");
                    Alert alert = new Alert(AlertType.INFORMATION);
                    alert.setTitle("Error");
                    alert.setHeaderText("Cannot send message");
                    alert.setContentText("Your certificate is not valid !");
                    alert.showAndWait();
                }
                
            } else {
                System.err.println("Certificate is not valid");
                Alert alert = new Alert(AlertType.INFORMATION);
                alert.setTitle("Error");
                alert.setHeaderText("Cannot send message");
                alert.setContentText("Users certificate is not valid !");
                alert.showAndWait();
            }
        } else {
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("Please fill out all the fields");
            alert.showAndWait();
        }
    }
}
