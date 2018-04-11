package kriptoprojektni;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ResourceBundle;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * FXML Controller class
 *
 * @author Semanic
 */
public class MainFormController implements Initializable {

    @FXML
    private Label lblWelcome;
    @FXML
    private Label lblUserName;
    @FXML
    private Button btnOpenMessage;
    @FXML
    private Button btnNewMessage;
    @FXML
    private Label lblMessages;
    @FXML
    private ListView<String> lstvwMessages;
    public static HashMap<String, HashSet<String>> hashedUserData;
    public static String messageForOpen;
    private String activeUser;
    @FXML
    private Label lblNumberUnreadMessages;
    @FXML
    private AnchorPane anchor;
    public boolean firstTime = true;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        activeUser = FXMLDocumentController.activeUser;
        lblUserName.setText(activeUser);
        readDataHash();
        checkFiles();
        ObservableList<String> files = FXCollections.observableArrayList();
        File userFile = new File("data/" + activeUser);
        // create new folder for user if does not exist
        if (!userFile.exists()) {
            userFile.mkdir();
        }
        //insert messages for user in listview
        for (String path : userFile.list()) {
            files.add(path);
        }
        lstvwMessages.setItems(files);
        lblNumberUnreadMessages.setText("You have " + files.size() + " unread messages!");
        btnOpenMessage.disableProperty().bind(Bindings.size(lstvwMessages.getSelectionModel().getSelectedItems()).isEqualTo(0));
    }

    @FXML
    private void clkNewMessage(ActionEvent event) throws IOException {
        Stage stage = new Stage();
        Parent root3 = FXMLLoader.load(getClass().getResource("NewMessageFXML.fxml"));
        Scene scene = new Scene(root3);
        stage.setTitle("CryptoStenography");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    //refresh messages
    public void focuss() {
        if (firstTime) {
            firstTime = false;
            Stage primaryStage = (Stage) btnNewMessage.getScene().getWindow();
            primaryStage.focusedProperty().addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> ov, Boolean onHidden, Boolean onShown) {
                    if (onShown) {
                        // System.out.println("focusss");
                        ObservableList<String> files = FXCollections.observableArrayList();
                        File userFile = new File("data/" + activeUser);
                        if (!userFile.exists()) {
                            userFile.mkdir();
                        }
                        for (String path : userFile.list()) {
                            files.add(path);
                        }
                        lstvwMessages.setItems(files);
                        lblNumberUnreadMessages.setText("You have " + files.size() + " unread messages!");
                    }
                }
            });

        }
    }
// read data about users messages from object
    private static void readDataHash() {
        try {
            File file = new File("app_data/hashData.bin");
            if (file.exists()) {
                String dataFromFile = new String(Files.readAllBytes(Paths.get(file.getPath())), "UTF-8");
                String encInfo = dataFromFile.split("#")[0];
                String encHashData = dataFromFile.split("#")[1];
                String decInfo = new String(Util.rsaDecryptData(encInfo.getBytes("UTF-8"), Util.privateKeyByUsername("appkey")), "UTF-8");
                String hash = decInfo.split("#")[1];
                byte[] decKey = Base64.getDecoder().decode(decInfo.split("#")[0]);
                SecretKey aesKey = new SecretKeySpec(decKey, 0, decKey.length, "AES");
                byte[] decData = Util.aesDecryptData(encHashData.getBytes("UTF-8"), aesKey);
                String calcHash = Util.hashSHA256(decData);
                if (calcHash.equals(hash)) {
                    ByteArrayInputStream byteIn = new ByteArrayInputStream(decData);
                    ObjectInputStream in = new ObjectInputStream(byteIn);
                    hashedUserData = (HashMap<String, HashSet<String>>) in.readObject();
                } else {
                    notification("File hashData is compromised!");
                    generateEmptyData();
                }
            } else {
                generateEmptyData();
            }

        } catch (Exception e) {
            notification("File hashData is compromised!");
            generateEmptyData();
        }

    }

    private static void generateEmptyData() {
        System.out.println("Generating new empty data...");
        hashedUserData = new HashMap<String, HashSet<String>>();
        File folder = new File("data");
        File[] folderList = folder.listFiles();
        for (File userFolder : folderList) {
            HashSet hashSet = new HashSet<String>();
            hashedUserData.put(userFolder.getName(), hashSet);
        }
    }

    private void checkFiles() {
        HashSet<String> tmp = new HashSet<>();
        File userFile = new File("data/" + lblUserName.getText());
        if (!userFile.exists()) {
            userFile.mkdir();
        }
        try {
            for (File file : userFile.listFiles()) {
                String file_hash = Util.hashSHA256(file);
                if (hashedUserData.get(activeUser).contains(file_hash)) {
                    tmp.add(file_hash);
                } else {
                    file.delete();
                }
            }

            int cnt = hashedUserData.get(activeUser).size() - tmp.size();
            if (cnt > 0) {
                notification("Some files are compromised, so we deleted them !");
                hashedUserData.get(activeUser).retainAll(tmp);
                writeHashData();
            }
        } catch (Exception e) {
            notification("Problem with file hash!");
        }

    }

    public static void notification(String s) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Error");
        alert.setContentText(s);
        alert.showAndWait();
    }

    @FXML
    private void handleOpenMessageAction(ActionEvent event) throws IOException {
        messageForOpen = lstvwMessages.getSelectionModel().getSelectedItem();
//        System.out.println(messageForOpen);
        Stage stage = new Stage();
        Parent root3 = FXMLLoader.load(getClass().getResource("OpenMessageFXML.fxml"));
        Scene scene = new Scene(root3);
        stage.setTitle("CryptoStenography");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
        focuss();
    }

    public static void writeHashData() throws Exception {
        try {
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            ObjectOutputStream os = new ObjectOutputStream(byteOut);
            os.writeObject(hashedUserData);
            byte[] hashDataObject
                    = byteOut.toByteArray();

            SecretKey aesKey = KeyGenerator.getInstance("AES").generateKey();
            String encryptedHashData = new String(Util.aesEncryptData(hashDataObject, aesKey), "UTF-8");
            String hash = Util.hashSHA256(hashDataObject);
            String keyString = Base64.getEncoder().encodeToString(aesKey.getEncoded());

            String encryptedInformations = new String(Util.rsaEncyptData((keyString + "#" + hash).getBytes("UTF-8"), Util.getPublicKey("app")), "UTF-8");

            byte[] data = (encryptedInformations + "#" + encryptedHashData).getBytes("UTF-8");

            File file = new File("app_data/hashData.bin");
            if (file.exists()) {
                file.delete();
            }

            FileOutputStream fos = new FileOutputStream(file);
            fos.write(data);
            fos.close();
        } catch (Exception e) {
            System.err.println("Error while writing hash data!");
            throw new Exception();
        }
    }

}
