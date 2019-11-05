package com.shiveshkumar.info7255.controller;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.everit.json.schema.Schema;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.shiveshkumar.info7255.beans.EtagManager;
import com.shiveshkumar.info7255.beans.JSONValidator;
import com.shiveshkumar.info7255.beans.JedisBean;

@RestController
public class HomeController {

	@Autowired
	private JSONValidator validator;
	@Autowired
	private JedisBean jedisBean;
	
	private static String finalKey = "0123456789abcdef";

	@Autowired
	private EtagManager etagManager;

	Map<String, Object> m = new HashMap<String, Object>();

	@RequestMapping("/")
	public String home() {
		return "Welcome to Advanced Big Data Indexing Demo!";		
	}

	@GetMapping(value = "/plan/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> read(@PathVariable(name = "id", required = true) String id,
			@RequestHeader HttpHeaders requestHeaders) {
		m.clear();
		if (!authorize(requestHeaders)) {
			m.put("message", "Authorization failed");
			return new ResponseEntity<Map<String, Object>>(m, HttpStatus.UNAUTHORIZED);
		}

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		JSONObject jsonString = jedisBean.read(id);
		if (jsonString != null) {
			String etag = etagManager.getETag(jsonString);
			if (!etagManager.verifyETag(jsonString, requestHeaders.getIfNoneMatch())) {
				headers.setETag(etag);
				return new ResponseEntity<Map<String, Object>>(jsonString.toMap(), headers, HttpStatus.OK);
			} else {
				headers.setETag(etag);
				return new ResponseEntity<Map<String, Object>>(m, headers, HttpStatus.NOT_MODIFIED);
			}
		} else {
			m.put("message", "Read unsuccessful. Invalid Id.");
			return new ResponseEntity<>(m, headers, HttpStatus.NOT_FOUND);
		}

	}

	@PostMapping(value = "/plan", produces = MediaType.APPLICATION_JSON_VALUE, consumes = "application/json")
	public ResponseEntity<Map<String, Object>> insert(@RequestBody(required = true) String body,
			@RequestHeader HttpHeaders requestHeaders) {
		m.clear();
		if (!authorize(requestHeaders)) {
			m.put("message", "Authorization failed");
			return new ResponseEntity<Map<String, Object>>(m, HttpStatus.UNAUTHORIZED);
		}

		Schema schema = validator.getSchema();
		if (schema == null) {
			m.put("message", "No Schema found");
			return new ResponseEntity<Map<String, Object>>(m, HttpStatus.NOT_FOUND);
		}
		
		JSONObject jsonObject = validator.getJsonObjectFromString(body);

		if (validator.validate(jsonObject)) {
			String uuid = jedisBean.insert(jsonObject);
			String etag=etagManager.getETag(jsonObject);
			
			HttpHeaders responseHeader=new HttpHeaders();
			responseHeader.setETag(etag);
			m.put("message", "Added successfully");
			m.put("id", uuid);
			return new ResponseEntity<Map<String, Object>>(m,responseHeader, HttpStatus.CREATED);
		} else {
			m.put("message", "Validation failed");
			return new ResponseEntity<Map<String, Object>>(m, HttpStatus.BAD_REQUEST);
		}

	}

	@DeleteMapping(value = "/plan", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> delete(@RequestBody(required = true) String body,
			@RequestHeader HttpHeaders requestHeaders) {
		m.clear();
		if (!authorize(requestHeaders)) {
			m.put("message", "Authorization failed");
			return new ResponseEntity<Map<String, Object>>(m, HttpStatus.UNAUTHORIZED);
		}
		
		if (jedisBean.delete(body)) {
			m.put("message", "Deleted successfully");
			return new ResponseEntity<Map<String, Object>>(m, HttpStatus.OK);
		} else {
			m.put("message", "Delete failed");
			return new ResponseEntity<Map<String, Object>>(m, HttpStatus.BAD_REQUEST);
		}
	}

	@RequestMapping(method= RequestMethod.PATCH,value = "/plan/{planID}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = "application/json")
	public ResponseEntity<Map<String, Object>> update(@PathVariable(name = "planID", required = true) String planID,@RequestBody(required = true) String body,
			@RequestHeader HttpHeaders requestHeaders) {
		m.clear();
		if (!authorize(requestHeaders)) {
			m.put("message", "Authorization failed");
			return new ResponseEntity<Map<String, Object>>(m, HttpStatus.UNAUTHORIZED);
		}

		Schema schema = validator.getSchema();
		if (schema == null) {	
			m.put("message", "No schema found!");
			return new ResponseEntity<Map<String, Object>>(m, HttpStatus.NOT_FOUND);
		}
		
		
		JSONObject jsonObject = validator.getJsonObjectFromString(body);

		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.APPLICATION_JSON);
		
		JSONObject planJSON=jedisBean.read(planID);
		if(planJSON!=null)
		{
			String etag = etagManager.getETag(planJSON);
			if (etagManager.verifyETag(planJSON, requestHeaders.getIfMatch())) {
				String newETag=jedisBean.patch(jsonObject,planID);
				if (newETag==null) {
					m.put("message", "Update failed");
					return new ResponseEntity<Map<String, Object>>(m, HttpStatus.BAD_REQUEST);
				}
				//String newETag=etagManager.getETag(jedisBean.read(planID));
				responseHeaders.setETag(newETag);
				m.put("message", "Plan Successfully Updated:"+planID);
				return new ResponseEntity<Map<String, Object>>(m, responseHeaders, HttpStatus.OK);
			} else {
				if(requestHeaders.getIfMatch().isEmpty()) {
					m.put("message","If-Match ETag required");
					return new ResponseEntity<Map<String, Object>>(m, responseHeaders, HttpStatus.PRECONDITION_REQUIRED);
				}
				else {
					responseHeaders.setETag(etag);
					return new ResponseEntity<Map<String, Object>>(m, responseHeaders, HttpStatus.PRECONDITION_FAILED);
			
				}
			}
		}
		else {

		m.put("message", "Invalid Plan Id");
		return new ResponseEntity<Map<String, Object>>(m, HttpStatus.BAD_REQUEST);
		}
	}
	
	@RequestMapping(method= RequestMethod.PUT,value = "/plan/{planID}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = "application/json")
	public ResponseEntity<Map<String, Object>> update1(@PathVariable(name = "planID", required = true) String planID,@RequestBody(required = true) String body,
			@RequestHeader HttpHeaders requestHeaders) {
		m.clear();
		if (!authorize(requestHeaders)) {
			m.put("message", "Authorization failed");
			return new ResponseEntity<Map<String, Object>>(m, HttpStatus.UNAUTHORIZED);
		}

		Schema schema = validator.getSchema();
		if (schema == null) {	
			m.put("message", "No schema found!");
			return new ResponseEntity<Map<String, Object>>(m, HttpStatus.NOT_FOUND);
		}
		
		
		JSONObject jsonObject = validator.getJsonObjectFromString(body);

		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.APPLICATION_JSON);
		
		JSONObject planJSON=jedisBean.read(planID);
		if(planJSON!=null)
		{
			String etag = etagManager.getETag(planJSON);
			if (etagManager.verifyETag(planJSON, requestHeaders.getIfMatch())) {
			
				if (!jedisBean.replace(jsonObject)) {
					m.put("message", "Update failed");
					return new ResponseEntity<Map<String, Object>>(m, HttpStatus.BAD_REQUEST);
				}
				String newETag=etagManager.getETag(jedisBean.read(planID));
				responseHeaders.setETag(newETag);
				m.put("message:", "Plan Successfully Updated:"+planID);
				return new ResponseEntity<Map<String, Object>>(m, responseHeaders, HttpStatus.OK);
			} else {
				if(requestHeaders.getIfMatch().isEmpty()) {
					m.put("message","If-Match ETag required");
					return new ResponseEntity<Map<String, Object>>(m, responseHeaders, HttpStatus.PRECONDITION_REQUIRED);
				}
				else {
					responseHeaders.setETag(etag);
					return new ResponseEntity<Map<String, Object>>(m, responseHeaders, HttpStatus.PRECONDITION_FAILED);
			
				}
			}
		}
		else {

		m.put("message", "Invalid Plan Id");
		return new ResponseEntity<Map<String, Object>>(m, HttpStatus.BAD_REQUEST);
		}
	}

	public boolean authorize(HttpHeaders headers) {
		if (headers.getFirst("Authorization") == null)
			return false;

		String token = headers.getFirst("Authorization").substring(7);
		 JSONParser parser = new JSONParser();
	        try
	        {
	            System.out.println("token coming in authorize"+token);
	            String initVector = "RandomInitVector";
	            IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
	            SecretKeySpec skeySpec = new SecretKeySpec(finalKey.getBytes("UTF-8"), "AES");

	            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
	            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
	            byte[] original = cipher.doFinal(org.apache.tomcat.util.codec.binary.Base64.decodeBase64(token));
	            String entityDecoded = new String(original);

	            System.out.println("***Entity Decoded is "+entityDecoded);

	            org.json.simple.JSONObject jsonobj = (org.json.simple.JSONObject)parser.parse(entityDecoded);
	            Object arrayOfTests = jsonobj.get("ttl");
	            Calendar calendar = Calendar.getInstance();
	            Date date =  calendar.getTime();
	            String getDate = arrayOfTests.toString();
	            SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

	            Date end = formatter.parse(getDate);
	            Date start = formatter.parse(formatter.format(date));

	            System.out.println(start.toString());
	            System.out.println(end.toString());

	            if(!start.before(end))
	            {
	                System.out.println("The Token Validity has expired");
	                return false;
	            }
	        }
	        catch(Exception e)
	        {
	            System.out.println("inside exception---"+ e);
	            return false;
	        }
	        return true;
	}

}
