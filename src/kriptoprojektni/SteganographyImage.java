/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kriptoprojektni;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Calendar;
import java.util.Random;
import javafx.scene.control.Alert;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;

/**
 *
 * @author Semanic
 */
public class SteganographyImage {

    private String fromUser;
    private String forUser;
    private String imagePath;
    private BufferedImage coverImage;
    private String message;
    private String messageWithHeader;
    private String dateTime;
    private String messageSignature;
    private SecretKey aesKey;
    private int encryptedSize;
    private byte[] encryptedMessage;
    private byte[] encryptedInfo;
    private byte[] decryptedMessage;
    private byte[] decryptedInfo;
    private boolean problem = false;
    private Charset utf = StandardCharsets.UTF_8;

    public SteganographyImage() {
    }

    public SteganographyImage(String fromUser, String forUser, String message, BufferedImage coverImage) {
        this.fromUser = fromUser;
        this.forUser = forUser;
        this.message = message;
        this.coverImage = coverImage;

        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
        dateTime = sdf.format(cal.getTime());

        //System.out.println("datum size: "+sdf.format(cal.getTime()).length());
        messageWithHeader = fromUser + "#" + dateTime + "#" + message;
    }

    public SteganographyImage(String from_user, String for_user, String message) {
        this.fromUser = from_user;
        this.forUser = for_user;
        this.message = message;

        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
        //System.out.println("datum size: "+sdf.format(cal.getTime()).length());
        messageWithHeader = fromUser + "#" + sdf.format(cal.getTime()) + "#" + message;
    }

    public boolean createSteganographyImage() {
        try {
            generateCipherData();
            String eI = new String(encryptedInfo);
            String eM = new String(encryptedMessage);
            String messageForSend = eI + "#" + eM;
            boolean pr = steg(messageForSend, coverImage, coverImage, forUser);
            if (!pr) {
                problem = true;
                System.err.println("Please select new picture!");
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Error");
                alert.setHeaderText(null);
                alert.setContentText("Your message could not be sent because picture is small for this message!");
                alert.showAndWait();
            }
// System.err.println(messageForSend);
            //saveStegoImage();
        } catch (Exception ex) {
//            ex.printStackTrace();
            problem = true;
            System.err.println("Problem while creating image!");
        }
        if (problem) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("Your message could not be sent!");
            alert.showAndWait();
        }
        return problem;
    }

    private void generateCipherData() throws Exception {

        try {
            //System.out.println("message"+message.length()+" from user:"+fromUser.length()+" ");
            messageSignature = Util.sign(Util.privateKeyByUsername(fromUser), messageWithHeader.getBytes(utf));
            //System.out.println("messageSignature: "+messageSignature.length());
        } catch (Exception ex) {
            problem = true;
            System.err.println("Problem with private key from " + fromUser);
            throw new Exception();
        }
        String messageWithSignature = messageSignature + "#" + messageWithHeader;
        //System.out.println("mWS: "+messageWithSignature.length());
        try {
            aesKey = KeyGenerator.getInstance("AES").generateKey();
            //  System.out.println("aeskey: "+aesKey.toString().length());
        } catch (NoSuchAlgorithmException ex) {
            problem = true;
            System.err.println("Algorithm does not exist!");
            throw new Exception();
        }
        encryptedMessage = Util.aesEncryptData(messageWithSignature.getBytes(utf), aesKey);

        encryptedSize = encryptedMessage.length;
        //System.out.println("encrypted size: "+encryptedSize);
        String encodedInfo = Base64.getEncoder().encodeToString(aesKey.getEncoded()) + "#" + encryptedSize + "#";
        //System.out.println("encoded info: "+encodedInfo.length());
        encryptedInfo = Util.rsaEncyptData(encodedInfo.getBytes(utf), Util.getPublicKey(forUser));
        //System.out.println(encryptedInfo);
        //System.out.println("encryptedInfo: "+encryptedInfo.length);
    }

    public SteganographyImage(String for_user, String image_name) {
        this.forUser = for_user;
        this.imagePath = "data/" + forUser + "/" + image_name;
    }

    public boolean getMessage() {
        try {
            coverImage = ImageIO.read(new File(imagePath));
            String data = desteg(coverImage);
            String[] datas = data.split("#");
            //System.out.println(data);
            encryptedInfo = datas[0].getBytes();
            //System.out.println(encryptedInfo);
            //System.out.println(datas[0]);
            encryptedMessage = datas[1].getBytes();

            //System.out.println(encryptedMessage);
            decryptedInfo = Util.rsaDecryptData(encryptedInfo, Util.privateKeyByUsername(forUser));
            String decodedInfo = new String(decryptedInfo, utf);
            byte[] decodedKey = Base64.getDecoder().decode(decodedInfo.split("#")[0]);
            aesKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
            encryptedSize = Integer.parseInt(decodedInfo.split("#")[1]);
            decryptMessage();
        } catch (Exception ex) {
            problem = true;
            ex.printStackTrace();
            System.err.println("Loading image problem!");
            return false;
        }
        return true;
    }

    public String getUser() {
        return fromUser;
    }

    public String getMessageText() {
        return message;
    }

    public String getDate() {
        return dateTime;
    }

    private void getCipherData() throws Exception {
        // encryptedInfo = loadDataFromImage2(0, 344/3+1);
        decryptedInfo = Util.rsaDecryptData(encryptedInfo, Util.privateKeyByUsername(forUser));
        String decodedInfo = new String(decryptedInfo, utf);
        byte[] decodedKey = Base64.getDecoder().decode(decodedInfo.split("#")[0]);
        aesKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
        encryptedSize = Integer.parseInt(decodedInfo.split("#")[1]);
        //encryptedMessage = loadDataFromImage2(encryptedInfo.length * 8, encryptedSize);
    }

    private void decryptMessage() throws Exception {

        decryptedMessage = Util.aesDecryptData(encryptedMessage, aesKey);
        String message_with_signature = new String(decryptedMessage, utf);
        messageSignature = message_with_signature.split("#")[0];
        messageWithHeader = message_with_signature.substring(messageSignature.length() + 1);
        fromUser = messageWithHeader.split("#")[0];
        dateTime = messageWithHeader.split("#")[1];
        message = messageWithHeader.substring(fromUser.length() + dateTime.length() + 2);
        PublicKey pk = Util.getPublicKey(fromUser);
        boolean signatureCheck = false;
        try {
            signatureCheck = Util.checkSignature(Util.getPublicKey(fromUser), messageSignature.getBytes(utf), messageWithHeader.getBytes(utf));
        } catch (Exception ex) {
            problem = true;
            System.err.println("Problem with signature from " + fromUser);
            throw new Exception();
        }

        if (!signatureCheck) {
            System.err.println("\nMessage is compromised!");
        }

    }

    public String getImagePath() {
        return imagePath;
    }

    public boolean steg(String message, BufferedImage input, BufferedImage output, String outputName) throws Exception {

        message = message.length() + ":" + message;
        boolean flag = false;
        if ((message.length() * 8) > (output.getHeight() * output.getWidth())) {
            System.err.println("Message is to big for selected picture !");
            return false;
        } else {
            byte[] messageBytes = message.getBytes();
            Point point = new Point(0, 0);
            for (int bite : messageBytes) {
                // For each byte read the MSB, write that to the image, and shift the byte to the right for the next MSB
                for (int i = 0; i < 8; i++) {
                    if ((bite & 128) == 128) {
                        output.setRGB(point.x, point.y, setLeastSignificantBit(output.getRGB(point.x, point.y), true));
                    } else {
                        output.setRGB(point.x, point.y, setLeastSignificantBit(output.getRGB(point.x, point.y), false));
                    }
                    bite = bite << 1;
                    movePointer(point, input); // Move to the next pixel
                }
            }

            String path = "data/" + outputName + "/pic";
            int rand_no = 0;
            String tmp_path = "";
            do {
                rand_no = new Random().nextInt(999) + 1;
                tmp_path = path + rand_no + ".png";
            } while (new File(tmp_path).exists());

            imagePath = tmp_path;

            try {
                ImageIO.write(output, "PNG", new File(tmp_path));
            } catch (IOException ex) {
                System.err.println("Saving image problem!");
                throw new Exception();
            }
        }
        return true;
    }

    public static String desteg(BufferedImage image) {

        int bitsInByte = 1;
        int extractedValue = 0;
        StringBuilder buffer = new StringBuilder();
        boolean gotLength = false;
        int messageLength = 0;
        int currentMessageLength = 0;
        Point point = new Point(0, 0); // points to pixel in image

        // While extracting the message...
        while (!gotLength || currentMessageLength < messageLength) {

            // Add one to the current byte if the LSB is one
            if ((image.getRGB(point.x, point.y) & 1) == 1) {
                extractedValue += 1;
            }

            // If current byte is complete...
            if (bitsInByte == 8) {

                // Was it the end of length header?
                if (!gotLength && ':' == extractedValue) {
                    // Record the message length and reset the buffer so it will just contain the message
                    gotLength = true;
                    messageLength = Integer.parseInt(buffer.toString());
                    currentMessageLength = 0;
                    buffer = new StringBuilder(messageLength);
                } else {
                    // Otherwise add the new character to the message
                    currentMessageLength++;
                    buffer.append((char) extractedValue);
                }
                // reset extraction variables
                extractedValue = 0;
                bitsInByte = 0;
            }

            extractedValue = extractedValue << 1; // Shift left so the next bit to be added is at the LSB position
            movePointer(point, image); // Move to the next pixel
            bitsInByte++; // Count where we are on creating the current byte.
        }

        return buffer.toString();
    }

    public static int setLeastSignificantBit(int b, boolean setToOne) {

        b = (b >> 1);
        b = (b << 1);
        if (setToOne) {
            b++;
        }
        return b;
    }

    public static void movePointer(Point point, BufferedImage image) {

        if (point.x == (image.getWidth() - 1)) {
            point.x = -1;
            point.y++;
        }
        point.x++;

        if (point.y == image.getHeight()) {
            throw new RuntimeException("Pointer moved beyond the end of the image");
        }
    }
}
