package com.shiveshkumar.info7255.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;

import org.json.JSONObject;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EtagUtil {

	public String getETag(JSONObject jsonObject) {

		String encoded = null;
		try {
			MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
			byte[] jsonObjectHash = messageDigest.digest(jsonObject.toString().getBytes(StandardCharsets.UTF_8));
			encoded = Base64.getEncoder().encodeToString(jsonObjectHash);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return "\"" + encoded + "\"";
	}

	public boolean verifyETag(JSONObject jsonObject, List<String> etags) {
		if (etags.isEmpty())
			return false;
		String encoded = getETag(jsonObject);
		return etags.contains(encoded);
	}

}
