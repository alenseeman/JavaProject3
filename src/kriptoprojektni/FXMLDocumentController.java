package kriptoprojektni;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.ObjectInputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * @author Semanic
 */
public class FXMLDocumentController implements Initializable {

    private Label label;
    @FXML
    private Button btnLogin;
    @FXML
    private Label lblLogin;
    @FXML
    private Label lblUserName;
    @FXML
    private Label lblPassword;
    @FXML
    private TextField txtFldUserName;
    @FXML
    private PasswordField txtFldPassword;
    @FXML
    private Label lblNotification;

    public static HashMap<String, String> users;
    
    public static String activeUser = "";
    public static boolean problem = false;

    @FXML
    private void handleButtonAction(ActionEvent event) throws Exception {
        if (txtFldUserName.getText().isEmpty() || txtFldPassword.getText().isEmpty()) {
            lblNotification.setText("Fill out all the fields!");
        } else if (!users.containsKey(txtFldUserName.getText())) {
            lblNotification.setText("User does not exist!");
        } else {
            String password = Util.hashSHA256(txtFldPassword.getText());
            if (users.get(txtFldUserName.getText()).equals(password)) {
                activeUser = txtFldUserName.getText();
                Stage stage = new Stage();
                Parent root3 = FXMLLoader.load(getClass().getResource("MainForm.fxml"));
                Scene scene = new Scene(root3);
                stage.setTitle("CryptoStenography");
                stage.setScene(scene);
                stage.setResizable(false);
                stage.show();
                Stage ThisStage = (Stage) btnLogin.getScene().getWindow();
                ThisStage.close();
            } else {
                lblNotification.setText("Username or password is wrong!");
            }
        }
    }

    private static void readUsersData() {
        File file = new File("app_data/passData.bin");
        if (file.exists()) {
            try {
                String dataFromFile = new String(Files.readAllBytes(Paths.get(file.getPath())), "UTF-8");
                String encodedInformations = dataFromFile.split("#")[0];
                String encodedUsersData = dataFromFile.split("#")[1];
                String decodedInformations = new String(Util.rsaDecryptData(encodedInformations.getBytes("UTF-8"), Util.privateKeyByUsername("appkey")), "UTF-8");
                String hash = decodedInformations.split("#")[1];
                byte[] decodedKey = Base64.getDecoder().decode(decodedInformations.split("#")[0]);
                SecretKey aesKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
                byte[] decodedUsersData = Util.aesDecryptData(encodedUsersData.getBytes("UTF-8"), aesKey);
                String calculatedHash = Util.hashSHA256(decodedUsersData);
                if (calculatedHash.equals(hash)) {
                    ByteArrayInputStream byteIn = new ByteArrayInputStream(decodedUsersData);
                    ObjectInputStream in = new ObjectInputStream(byteIn);
                    users = (HashMap<String, String>) in.readObject();
                } else {
                    problem=true;
                    System.err.println("File passData is compromisedd!");
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Error");
                    alert.setContentText("File with data is compromised and you cannot login!");
                    alert.showAndWait();
                }

            } catch (Exception e) {
                problem=true;
                e.printStackTrace();
                System.err.println("File passData is compromised!");
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Error");
                alert.setContentText("File with data is compromised and you cannot login!");
                alert.showAndWait();
            }
        } else {
            problem=true;
            System.err.println("File passData does not exist!");
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Error");
            alert.setContentText("File with data does not exist and you cannot login!");
            alert.showAndWait();
        }

    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
       readUsersData();
       btnLogin.setDisable(problem);
       if(problem)
       {
        btnLogin.setOnAction(e -> {
            try {
                handleButtonAction(e);
            } catch (Exception ex) {
                Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        }
    }
}
