package com.shiveshkumar.info7255.controller;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
public class TokenController {
    private static String FINAL_KEY = "qwerty0123456789";

    @GetMapping(value="/token")
    public ResponseEntity<Map<String, Object>> getToken(@RequestHeader HttpHeaders headers) throws UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, JSONException {

        String initVector = "r#nd#minitv#ct#r";
        JSONObject object = new JSONObject();
        object.put("organization", "example.com");
        object.put("user", "shiveshkumar");
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 60);
        Date date =  calendar.getTime();
        SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        object.put("ttl", df.format(date));

        String token = object.toString();
        System.out.println("Token values is " + token);
        System.out.println("TTL is : " + object.get("ttl"));


        IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
        SecretKeySpec skeySpec = new SecretKeySpec(FINAL_KEY.getBytes("UTF-8"), "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
       
        byte[] encrypted = cipher.doFinal(token.getBytes());

        Map<String, Object> finalToken = new HashMap<String, Object>();
        finalToken.put("Token",org.apache.tomcat.util.codec.binary.Base64.encodeBase64String(encrypted));
        return new ResponseEntity<Map<String, Object>>(finalToken, HttpStatus.CREATED);
    }
}
