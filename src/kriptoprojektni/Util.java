/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kriptoprojektni;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CRL;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import javax.crypto.*;
import org.bouncycastle.util.io.pem.*;

/**
 *
 * @author Semanic
 */
public class Util {

    public static String hashSHA256(byte[] bytes) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(bytes);
        return Base64.getEncoder().encodeToString(hash);
    }

    public static String hashSHA256(String s) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(s.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }

    public static String hashSHA256(File f) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] b = Files.readAllBytes(Paths.get(f.getPath()));
        byte[] hash = digest.digest(b);
        return Base64.getEncoder().encodeToString(hash);
    }

    public static byte[] aesEncryptData(byte[] bytes, SecretKey key) {
        byte[] encrypted_data = null;
        try {
            java.security.Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            encrypted_data = cipher.doFinal(bytes);
        } catch (Exception e) {
            System.err.println("AES encryption error!");
        }

        return Base64.getEncoder().encode(encrypted_data);
    }

    public static byte[] aesDecryptData(byte[] bytes, SecretKey key) throws Exception {
        byte[] decrypted_data = null;
        byte[] decoded_bytes = Base64.getDecoder().decode(bytes);
        try {
            java.security.Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, key);
            decrypted_data = cipher.doFinal(decoded_bytes);
        } catch (Exception ex) {
            System.err.println("AES decryption error!");
            throw new Exception();
        }
        return decrypted_data;
    }

    public static boolean checkCertificate(String username) {
        X509Certificate userCert = null;
        // verification
        try {
            PemReader certReader = new PemReader(new FileReader("CA/cacert.pem"));
            Certificate ca_cert = CertificateFactory.getInstance("x.509").generateCertificate(new ByteArrayInputStream(certReader.readPemObject().getContent()));
            certReader = new PemReader(new FileReader("CA/certs/" + username + ".pem"));
            userCert = (X509Certificate) CertificateFactory.getInstance("x.509").generateCertificate(new ByteArrayInputStream(certReader.readPemObject().getContent()));
            certReader.close();
            userCert.verify(ca_cert.getPublicKey());
        } catch (Exception ex) {
            System.err.println("Certificate verification failed!");
            return false;
        }

        try {
            userCert.checkValidity();
        } catch (Exception ex) {
            System.err.println("Certificate is not valid!");
            return false;
        }

        //check crl
        try {
            File crl_list = new File("CA/crl/crl1.pem");
//            System.out.println(crl_list.exists());
            if (crl_list.exists()) {
                PemReader crlListReader = new PemReader(new FileReader(crl_list));
                PemObject pemObject = crlListReader.readPemObject();
                if (pemObject.getType() != null) {
                    CRL crl = CertificateFactory.getInstance("x.509").generateCRL(new ByteArrayInputStream(pemObject.getContent()));
                    if (crl.isRevoked(userCert)) {
                        System.err.println("Certificate is revoked!");
                        return false;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error while searching crl list!");
            return false;
        }

        return true;
    }

    public static boolean checkSignature(PublicKey public_key, byte[] base64signature, byte[] str) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(public_key);
        signature.update(str);
        return signature.verify(Base64.getDecoder().decode(base64signature));
    }

    public static PublicKey getPublicKey(String username) throws Exception {
        PublicKey publicKey = null;
        try {
            java.security.Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
            PemReader certReader = new PemReader(new FileReader("CA/certs/" + username + ".pem"));
            X509Certificate user_cert = (X509Certificate) CertificateFactory.getInstance("x.509").generateCertificate(new ByteArrayInputStream(certReader.readPemObject().getContent()));
            publicKey = user_cert.getPublicKey();
            certReader.close();
        } catch (Exception e) {
            System.err.println("Problem with public key !");
            throw new Exception();
        }
        return publicKey;
    }

    public static byte[] rsaDecryptData(byte[] bytes, PrivateKey private_key) throws Exception {
        byte[] decrypted_data = null;
        byte[] decoded_bytes = Base64.getDecoder().decode(bytes);
        try {
            java.security.Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, private_key);
            decrypted_data = cipher.doFinal(decoded_bytes);
        } catch (Exception ex) {
            System.err.println("RSA decryption error!");
            throw new Exception();
        }
        return decrypted_data;
    }

    public static byte[] rsaEncyptData(byte[] bytes, PublicKey pk) throws Exception{
        byte[] encrypted_data = null;
            java.security.Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, pk);
            encrypted_data = cipher.doFinal(bytes);

        return Base64.getEncoder().encode(encrypted_data);
    }

    public static PrivateKey privateKeyByUsername(String username) {
        PrivateKey private_key = null;
        try {
            PemReader keyReader = new PemReader(new FileReader("CA/private/" + username + ".pem"));
            java.security.Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
            private_key = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(keyReader.readPemObject().getContent()));
            keyReader.close();
        } catch (Exception ex) {
            System.err.println("Private key does not exist!");
            return null;
        }
        return private_key;
    }

    public static String sign(PrivateKey pk, byte[] bytes) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(pk);
        signature.update(bytes);
        return Base64.getEncoder().encodeToString(signature.sign());
    }
}
