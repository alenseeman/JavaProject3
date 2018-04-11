/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kriptoprojektni;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.ObjectOutputStream;
import java.util.Base64;
import java.util.HashMap;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

/**
 *
 * @author Semanic
 */
public class InitPassData {
    public static HashMap<String,String> users;
    public static void main(String[] args)
    {
//         int[][] matrica = {{0xab, 0x23, 0x49, 0x12}, {0xcd, 0x26, 0xaf, 0x1f}, {0x23, 0x12, 0x13, 0xca}, {0x4c, 0x56, 0x93, 0x43}};
//        
//        for(int niz[] : matrica){                
//                int a1 = two(niz[0]) ^ three(niz[1]) ^ niz[2] ^ niz[3];
//                int a2 = niz[0] ^ two(niz[1]) ^ three(niz[2]) ^ niz[3];
//                int a3 = niz[0] ^ niz[1] ^ two(niz[2]) ^ three(niz[3]);
//                int a4 = three(niz[0]) ^ niz[1] ^ niz[2] ^ two(niz[3]);
//            
//                System.out.println(String.format("0x%02X", a1));
//                System.out.println(String.format("0x%02X", a2));
//                System.out.println(String.format("0x%02X", a3));
//                System.out.println(String.format("0x%02X", a4));
//                System.out.println("------");
//        }
//	}
//
//	public static int two(int b){
//		int tmp=0;
//		int mask = 0x00ff;
//		tmp = b << 1;
//		if(b >= 0x80){
//			tmp ^= 0x1b; 
//		}		
//		return mask & tmp;
//	}
//        
//    public static int three(int b){   
//        int mask = 0x00ff;        
//        int tmp = two(b) ^ b;
//        return mask & tmp;            

        writeUsersData();
    
    }    
    
    
    private static void writeUsersData(){
        try{
            
            users = new HashMap<String, String>();          
            BufferedReader br=new BufferedReader(new FileReader(new File("passData.txt")));
            String input = "";
            while((input = br.readLine()) != null){
                String passwordHash=Util.hashSHA256(input.split("#")[1]);
               users.put(input.split("#")[0],passwordHash);
            }                      
            System.out.println(users);
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            ObjectOutputStream os = new ObjectOutputStream(byteOut);
            os.writeObject(users);            
            byte[] passpackObject = byteOut.toByteArray();
            
            SecretKey aesKey = KeyGenerator.getInstance("AES").generateKey();
            String encryptedUsersData = new String(Util.aesEncryptData(passpackObject, aesKey), "UTF-8");
            String hash = Util.hashSHA256(passpackObject);
            String keyString = Base64.getEncoder().encodeToString(aesKey.getEncoded());
            
            String encryptedInfo = new String(Util.rsaEncyptData((keyString + "#" + hash).getBytes("UTF-8"), Util.getPublicKey("app")), "UTF-8");
            
            byte[] data = (encryptedInfo + "#" + encryptedUsersData).getBytes("UTF-8");            
            
            File file = new File("app_data/passData.bin");
            if(file.exists()){
                file.delete();
            }            
            
            FileOutputStream fos = new FileOutputStream(file);            
            fos.write(data);
            fos.close();                
        } catch(Exception e){
            e.printStackTrace();
        }
    }
   
    
}
