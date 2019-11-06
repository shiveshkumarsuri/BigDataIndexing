package com.shiveshkumar.info7255.controller;

import java.util.HashMap;
import java.util.Map;

import org.everit.json.schema.Schema;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shiveshkumar.info7255.util.EtagUtil;
import com.shiveshkumar.info7255.util.JsonValidatorUtil;
import com.shiveshkumar.info7255.persistence.RedisHelper;
import com.shiveshkumar.info7255.service.BigDataApplicationService;
import com.shiveshkumar.info7255.util.ApplicationUtil;

@RestController
@RequestMapping("/bigdata")
public class BigDataApplicationController implements BigDataApplicationService{

	@Autowired
	private JsonValidatorUtil validator;
	@Autowired
	private RedisHelper redisHelper;
	@Autowired
	private EtagUtil etagManager;

	private Map<String, Object> entityMap = new HashMap<String, Object>();

	@Override
	public String home() {
		return "Welcome to Advanced Big Data Indexing Demo!";
	}

	@Override
	public ResponseEntity<Map<String, Object>> readPlan(String id,HttpHeaders requestHeaders) {
		entityMap.clear();
		if (!ApplicationUtil.authorize(requestHeaders)) {
			entityMap.put("message", "Authorization failed");
			return new ResponseEntity<Map<String, Object>>(entityMap, HttpStatus.UNAUTHORIZED);
		}

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		JSONObject jsonString = redisHelper.readJsonObject(id);
		if (jsonString != null) {
			String etag = etagManager.getETag(jsonString);
			if (!etagManager.verifyETag(jsonString, requestHeaders.getIfNoneMatch())) {
				headers.setETag(etag);
				return new ResponseEntity<Map<String, Object>>(jsonString.toMap(), headers, HttpStatus.OK);
			} else {
				headers.setETag(etag);
				return new ResponseEntity<Map<String, Object>>(entityMap, headers, HttpStatus.NOT_MODIFIED);
			}
		} else {
			entityMap.put("message", "Read Unsuccessful. Invalid Id.");
			return new ResponseEntity<>(entityMap, headers, HttpStatus.NOT_FOUND);
		}

	}

	@Override
	public ResponseEntity<Map<String, Object>> insertPlan(String body, HttpHeaders requestHeaders) {
		entityMap.clear();
		if (!ApplicationUtil.authorize(requestHeaders)) {
			entityMap.put("message", "Authorization failed");
			return new ResponseEntity<Map<String, Object>>(entityMap, HttpStatus.UNAUTHORIZED);
		}

		Schema schema = validator.getSchema();
		if (schema == null) {
			entityMap.put("message", "No Schema found");
			return new ResponseEntity<Map<String, Object>>(entityMap, HttpStatus.NOT_FOUND);
		}

		JSONObject jsonObject = validator.getJsonObjectFromString(body);

		if (validator.validate(jsonObject)) {
			String uuid = redisHelper.insert(jsonObject);
			String etag = etagManager.getETag(jsonObject);

			HttpHeaders responseHeader = new HttpHeaders();
			responseHeader.setETag(etag);
			entityMap.put("message", "Plan Added successfully");
			entityMap.put("Plan Id", uuid);
			return new ResponseEntity<Map<String, Object>>(entityMap, responseHeader, HttpStatus.CREATED);
		} else {
			entityMap.put("message", "Validation failed");
			return new ResponseEntity<Map<String, Object>>(entityMap, HttpStatus.BAD_REQUEST);
		}

	}

	@Override
	public ResponseEntity<Map<String, Object>> deletePlan(String body, HttpHeaders requestHeaders) {
		entityMap.clear();
		if (!ApplicationUtil.authorize(requestHeaders)) {
			entityMap.put("message", "Authorization failed");
			return new ResponseEntity<Map<String, Object>>(entityMap, HttpStatus.UNAUTHORIZED);
		}

		if (redisHelper.delete(body)) {
			entityMap.put("message", "Deleted successfully");
			return new ResponseEntity<Map<String, Object>>(entityMap, HttpStatus.OK);
		} else {
			entityMap.put("message", "Delete failed");
			return new ResponseEntity<Map<String, Object>>(entityMap, HttpStatus.BAD_REQUEST);
		}
	}

	@Override
	public ResponseEntity<Map<String, Object>> updatePlan(String planID,String body,HttpHeaders requestHeaders) {
		entityMap.clear();
		if (!ApplicationUtil.authorize(requestHeaders)) {
			entityMap.put("message", "Authorization failed");
			return new ResponseEntity<Map<String, Object>>(entityMap, HttpStatus.UNAUTHORIZED);
		}

		Schema schema = validator.getSchema();
		if (schema == null) {
			entityMap.put("message", "No schema found!");
			return new ResponseEntity<Map<String, Object>>(entityMap, HttpStatus.NOT_FOUND);
		}

		JSONObject jsonObject = validator.getJsonObjectFromString(body);

		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.APPLICATION_JSON);

		JSONObject planJSON = redisHelper.readJsonObject(planID);
		if (planJSON != null) {
			String etag = etagManager.getETag(planJSON);
			if (etagManager.verifyETag(planJSON, requestHeaders.getIfMatch())) {
				String newETag = redisHelper.patch(jsonObject, planID);
				if (newETag == null) {
					entityMap.put("message", "Update failed");
					return new ResponseEntity<Map<String, Object>>(entityMap, HttpStatus.BAD_REQUEST);
				}
				responseHeaders.setETag(newETag);
				entityMap.put("message", "Plan Successfully Updated:" + planID);
				return new ResponseEntity<Map<String, Object>>(entityMap, responseHeaders, HttpStatus.OK);
			} else {
				if (requestHeaders.getIfMatch().isEmpty()) {
					entityMap.put("message", "If-Match ETag required");
					return new ResponseEntity<Map<String, Object>>(entityMap, responseHeaders,
							HttpStatus.PRECONDITION_REQUIRED);
				} else {
					responseHeaders.setETag(etag);
					return new ResponseEntity<Map<String, Object>>(entityMap, responseHeaders,
							HttpStatus.PRECONDITION_FAILED);

				}
			}
		} else {

			entityMap.put("message", "Invalid Plan Id");
			return new ResponseEntity<Map<String, Object>>(entityMap, HttpStatus.BAD_REQUEST);
		}
	}

	@Override
	public ResponseEntity<Map<String, Object>> partialUpdate(String planID, String body, HttpHeaders requestHeaders) {
		entityMap.clear();
		if (!ApplicationUtil.authorize(requestHeaders)) {
			entityMap.put("message", "Authorization failed");
			return new ResponseEntity<Map<String, Object>>(entityMap, HttpStatus.UNAUTHORIZED);
		}

		Schema schema = validator.getSchema();
		if (schema == null) {
			entityMap.put("message", "No schema found!");
			return new ResponseEntity<Map<String, Object>>(entityMap, HttpStatus.NOT_FOUND);
		}

		JSONObject jsonObject = validator.getJsonObjectFromString(body);

		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.APPLICATION_JSON);

		JSONObject planJSON = redisHelper.readJsonObject(planID);
		if (planJSON != null) {
			String etag = etagManager.getETag(planJSON);
			if (etagManager.verifyETag(planJSON, requestHeaders.getIfMatch())) {

				if (!redisHelper.replace(jsonObject)) {
					entityMap.put("message", "Update failed");
					return new ResponseEntity<Map<String, Object>>(entityMap, HttpStatus.BAD_REQUEST);
				}
				String newETag = etagManager.getETag(redisHelper.readJsonObject(planID));
				responseHeaders.setETag(newETag);
				entityMap.put("message:", "Plan Successfully Updated:" + planID);
				return new ResponseEntity<Map<String, Object>>(entityMap, responseHeaders, HttpStatus.OK);
			} else {
				if (requestHeaders.getIfMatch().isEmpty()) {
					entityMap.put("message", "If-Match ETag required");
					return new ResponseEntity<Map<String, Object>>(entityMap, responseHeaders,
							HttpStatus.PRECONDITION_REQUIRED);
				} else {
					responseHeaders.setETag(etag);
					return new ResponseEntity<Map<String, Object>>(entityMap, responseHeaders,
							HttpStatus.PRECONDITION_FAILED);

				}
			}
		} else {

			entityMap.put("message", "Invalid Plan Id");
			return new ResponseEntity<Map<String, Object>>(entityMap, HttpStatus.BAD_REQUEST);
		}
	}

}
