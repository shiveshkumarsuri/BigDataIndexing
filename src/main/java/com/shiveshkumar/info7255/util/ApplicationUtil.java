package com.shiveshkumar.info7255.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.json.simple.parser.JSONParser;
import org.springframework.http.HttpHeaders;

public class ApplicationUtil {

	public static final String REDIS_SEPERATOR = "___";

	private static String FINAL_KEY = "qwerty0123456789";

	public static String addLineSeparator(String s) {
		return s + System.lineSeparator();
	}

	public static boolean authorize(HttpHeaders headers) {
		if (headers.getFirst("Authorization") == null)
			return false;

		String token = headers.getFirst("Authorization").substring(7);
		JSONParser parser = new JSONParser();
		try {
			System.out.println("Token being authorized:" + token);
			String initVector = "r#nd#minitv#ct#r";
			IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
			SecretKeySpec skeySpec = new SecretKeySpec(FINAL_KEY.getBytes("UTF-8"), "AES");

			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
			cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
			byte[] original = cipher.doFinal(org.apache.tomcat.util.codec.binary.Base64.decodeBase64(token));
			String entityDecoded = new String(original);

			System.out.println("### Decoded Key: " + entityDecoded);

			org.json.simple.JSONObject jsonobj = (org.json.simple.JSONObject) parser.parse(entityDecoded);
			Object arrayOfTests = jsonobj.get("ttl");
			Calendar calendar = Calendar.getInstance();
			Date date = calendar.getTime();
			String getDate = arrayOfTests.toString();
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

			Date end = formatter.parse(getDate);
			Date start = formatter.parse(formatter.format(date));

			System.out.println(start.toString());
			System.out.println(end.toString());

			if (!start.before(end)) {
				System.out.println("Token Expired!: "+end);
				return false;
			}
		} catch (Exception e) {
			System.out.println("Exception occured while generating token" + e.getStackTrace());
			return false;
		}
		return true;
	}
}
