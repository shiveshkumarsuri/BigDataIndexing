package com.shiveshkumar.info7255.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.shiveshkumar.info7255.beans.EtagManager;
import com.shiveshkumar.info7255.beans.JSONValidator;
import com.shiveshkumar.info7255.beans.JedisBean;

@Configuration
public class RedisConfig {

	@Bean("validator")
	public JSONValidator validator() {
		return new JSONValidator(jedisBean()) ;
	}
	
	@Bean("jedisBean")
	public JedisBean jedisBean() {
		return new JedisBean() ;
	}
	
	@Bean("etagManager")
	public EtagManager etagManager() {
		return new EtagManager();
	}
}
