package com.shiveshkumar.info7255.persistence;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import com.shiveshkumar.info7255.util.EtagUtil;

import io.lettuce.core.RedisException;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisException;

@Configuration
public class RedisHelper {
	
	@Autowired
	private EtagUtil etagUtil;
	
	private static RedisPool redispool = RedisPool.getRedisPool();
	private JedisPool pool = redispool.pool;
	private static final String SEP = "____";

	public boolean insertSchema(String schema) {
		try {
			Jedis jedis = pool.getResource();
			if (jedis.set("plan_schema", schema).equals("OK"))
				return true;
			else
				return false;
		} catch (JedisException e) {
			e.printStackTrace();
			return false;
		}
	}

	public String getSchema() {
		try {
			Jedis jedis = pool.getResource();
			return jedis.get("plan_schema");
		} catch (JedisException e) {
			e.printStackTrace();
			return null;
		}
	}

	public String insert(JSONObject jsonObject) {
		String idOne = jsonObject.getString("objectType") + SEP + jsonObject.getString("objectId");
		if (insertUtil(jsonObject, idOne)) {
			Jedis jedis = pool.getResource();
			jedis.close();

			return jsonObject.getString("objectId");
		} else
			return null;
	}

	private boolean insertUtil(JSONObject jsonObject, String uuid) {
		try {
			Jedis jedis = pool.getResource();
			Map<String, String> simpleMap = new HashMap<String, String>();

			for (Object key : jsonObject.keySet()) {
				String attributeKey = String.valueOf(key);
				Object attributeVal = jsonObject.get(String.valueOf(key));
				String edge = attributeKey;
				if (attributeVal instanceof JSONObject) {

					JSONObject embdObject = (JSONObject) attributeVal;
					String setKey = uuid + SEP + edge;
					String embd_uuid = embdObject.get("objectType") + SEP + embdObject.getString("objectId");
					jedis.sadd(setKey, embd_uuid);
					insertUtil(embdObject, embd_uuid);

				} else if (attributeVal instanceof JSONArray) {

					JSONArray jsonArray = (JSONArray) attributeVal;
					Iterator<Object> jsonIterator = jsonArray.iterator();
					String setKey = uuid + SEP + edge;

					while (jsonIterator.hasNext()) {
						JSONObject embdObject = (JSONObject) jsonIterator.next();
						String embd_uuid = embdObject.get("objectType") + SEP + embdObject.getString("objectId");
						jedis.sadd(setKey, embd_uuid);
						insertUtil(embdObject, embd_uuid);
					}

				} else {
					simpleMap.put(attributeKey, String.valueOf(attributeVal));
				}
			}
			jedis.hmset(uuid, simpleMap);
			jedis.close();
		} catch (JedisException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	public JSONObject readJsonObject(String id) {

		JSONObject jsonObject = readUtil("plan" + SEP + id);
		if (!jsonObject.isEmpty())
			return jsonObject;
		else
			return null;
	}

	private JSONObject readUtil(String uuid) {
		try {
			Jedis jedis = pool.getResource();
			JSONObject retreivedObject = new JSONObject();
			System.out.println("Reading all matched keys");
			Set<String> keys = jedis.keys(uuid + SEP + "*");

			for (String key : keys) {
				Set<String> jsonKeySet = jedis.smembers(key);

				if (jsonKeySet.size() > 1) {

					JSONArray ja = new JSONArray();
					Iterator<String> jsonKeySetIterator = jsonKeySet.iterator();
					while (jsonKeySetIterator.hasNext()) {
						ja.put(readUtil(jsonKeySetIterator.next()));
					}
					retreivedObject.put(key.substring(key.lastIndexOf(SEP) + 4), ja);
				} else {

					Iterator<String> jsonKeySetIterator = jsonKeySet.iterator();
					JSONObject embdObject = null;
					while (jsonKeySetIterator.hasNext()) {
						embdObject = readUtil(jsonKeySetIterator.next());
					}
					retreivedObject.put(key.substring(key.lastIndexOf(SEP) + 4), embdObject);

				}

			}

			Map<String, String> keyMap = jedis.hgetAll(uuid);
			for (String simpleKey : keyMap.keySet()) {
				retreivedObject.put(simpleKey, keyMap.get(simpleKey));
			}

			jedis.close();
			return retreivedObject;
		} catch (RedisException e) {
			e.printStackTrace();
			return null;
		}

	}

	public String patch(JSONObject jsonObject, String planId) {
		if (patchUtil(jsonObject)) {
			JSONObject completeObj = readJsonObject(planId);
			try {

				String newETag = etagUtil.getETag(completeObj);
				return newETag;
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public boolean patchUtil(JSONObject jsonObject) {
		try {
			Jedis jedis = pool.getResource();
			String uuid = jsonObject.getString("objectType") + SEP + jsonObject.getString("objectId");
			Map<String, String> simpleMap = jedis.hgetAll(uuid);
			if (simpleMap.isEmpty()) {
				simpleMap = new HashMap<String, String>();
			}

			for (Object key : jsonObject.keySet()) {
				String attributeKey = String.valueOf(key);
				Object attributeVal = jsonObject.get(String.valueOf(key));
				String edge = attributeKey;

				if (attributeVal instanceof JSONObject) {

					JSONObject embdObject = (JSONObject) attributeVal;
					String setKey = uuid + SEP + edge;
					String embd_uuid = embdObject.get("objectType") + SEP + embdObject.getString("objectId");
					jedis.sadd(setKey, embd_uuid);
					patchUtil(embdObject);

				} else if (attributeVal instanceof JSONArray) {

					JSONArray jsonArray = (JSONArray) attributeVal;
					Iterator<Object> jsonIterator = jsonArray.iterator();
					String setKey = uuid + SEP + edge;

					while (jsonIterator.hasNext()) {
						JSONObject embdObject = (JSONObject) jsonIterator.next();
						String embd_uuid = embdObject.get("objectType") + SEP + embdObject.getString("objectId");
						jedis.sadd(setKey, embd_uuid);
						patchUtil(embdObject);
					}

				} else {
					simpleMap.put(attributeKey, String.valueOf(attributeVal));
				}
			}
			jedis.hmset(uuid, simpleMap);
			jedis.close();
			return true;

		} catch (JedisException e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean replace(JSONObject body) {

		try {
			return insert(body) != null ? true : false;

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

	}

	public boolean delete(String body) {
		JSONObject json = new JSONObject(body);
		if (!json.has("objectType") || !json.has("objectId"))
			return false;

		return deleteUtil(json.getString("objectType") + SEP + json.getString("objectId"));
	}

	public boolean deleteUtil(String uuid) {
		try {
			Jedis jedis = pool.getResource();
			Set<String> keys = jedis.keys(uuid + SEP + "*");
			for (String key : keys) {
				Set<String> jsonKeySet = jedis.smembers(key);
				for (String embd_uuid : jsonKeySet) {
					deleteUtil(embd_uuid);
				}
				jedis.del(key);
			}

			jedis.del(uuid);
			jedis.close();
			return true;
		} catch (JedisException e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean doesKeyExist(String id) {

		try {
			Jedis jedis = pool.getResource();
			if (jedis.exists(id) || !jedis.keys(id + SEP + "*").isEmpty()) {
				jedis.close();
				return true;
			} else {
				jedis.close();
				return false;
			}
		} catch (JedisException e) {
			e.printStackTrace();
			return false;
		}
	}

}
