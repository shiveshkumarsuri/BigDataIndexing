package com.shiveshkumar.info7255.util;

import java.io.ByteArrayInputStream;

import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import com.shiveshkumar.info7255.persistence.RedisHelper;

@Configuration
public class JsonValidatorUtil {

	private Schema schema;
	@Autowired
	private RedisHelper redisHelper;

	public JsonValidatorUtil(RedisHelper j) {
		redisHelper = j;
	}

	public Schema getSchema() {
		if (schema != null)
			return schema;

		refreshSchema();
		return schema;
	}

	public void refreshSchema() {
		String schemaString = redisHelper.getSchema();
		if (schemaString != null)
			schema = SchemaLoader
					.load(new JSONObject(new JSONTokener(new ByteArrayInputStream(schemaString.getBytes()))));
	}

	public JSONObject getJsonObjectFromString(String jsonString) {
		return new JSONObject(jsonString);
	}

	public boolean validate(JSONObject jsonObject) {
		try {
			schema.validate(jsonObject);
			System.out.println("Validation success");
			return true;
		} catch (ValidationException e) {
			e.printStackTrace();
			return false;
		}
	}

}
