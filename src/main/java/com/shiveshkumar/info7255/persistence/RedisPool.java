package com.shiveshkumar.info7255.persistence;

import redis.clients.jedis.JedisPool;

public class RedisPool {

	public JedisPool pool = null;
	private static final String REDIS_HOST = "localhost";
	private static final Integer REDIS_PORT = 6379;
	
	private static RedisPool redisPool = null;

	private RedisPool() {
		pool = new JedisPool(REDIS_HOST, REDIS_PORT);
	}
	
	public static RedisPool getRedisPool() {
		if(redisPool == null) {
			redisPool = new RedisPool();
		}
		return redisPool;
	}
}
