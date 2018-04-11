/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kriptoprojektni;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.text.Text;
import static kriptoprojektni.MainFormController.writeHashData;


/**
 * FXML Controller class
 *
 * @author Semanic
 */
public class OpenMessageFXMLController implements Initializable {

    @FXML
    private Label lblMessage;
    @FXML
    private Text lblFromUser;
    @FXML
    private Text lblDate;
    @FXML
    private TextArea txtareaMessage;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
       try{
            SteganographyImage img = new SteganographyImage(FXMLDocumentController.activeUser, MainFormController.messageForOpen);
            String imagePath = img.getImagePath();
            String file_hash = Util.hashSHA256(new File(imagePath));
            if(img.getMessage())
            {
                lblFromUser.setText(img.getUser());
                lblDate.setText(img.getDate());
                txtareaMessage.setText(img.getMessageText());
            MainFormController.hashedUserData.get(FXMLDocumentController.activeUser).remove(file_hash);
            writeHashData();   
            new File(imagePath).delete();
            }
            else
            {
                  Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setContentText("Problem while opening message!");
                    alert.showAndWait();
            }
       } catch(Exception e){
            System.err.println("Problem while opening message!");
        }
    }    
    
}
