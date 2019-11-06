package com.shiveshkumar.info7255.service;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/default")
public interface BigDataApplicationService {
	
	@RequestMapping("/")
	public String home();
	
	@GetMapping(value = "/plan/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> readPlan(@PathVariable(name = "id", required = true) String id,
			@RequestHeader HttpHeaders requestHeaders);
	
	@PostMapping(value = "/plan", produces = MediaType.APPLICATION_JSON_VALUE, consumes = "application/json")
	public ResponseEntity<Map<String, Object>> insertPlan(@RequestBody(required = true) String body,
			@RequestHeader HttpHeaders requestHeaders);
	
	@DeleteMapping(value = "/deleteplan", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> deletePlan(@RequestBody(required = true) String body,
			@RequestHeader HttpHeaders requestHeaders);
	
	@PatchMapping(value = "/plan/{id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = "application/json")
	public ResponseEntity<Map<String, Object>> updatePlan(@PathVariable(name = "id", required = true) String planID,
			@RequestBody(required = true) String body, @RequestHeader HttpHeaders requestHeaders);
	
	@PutMapping(value = "/plan/{id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = "application/json")
	public ResponseEntity<Map<String, Object>> partialUpdate(@PathVariable(name = "id", required = true) String planID,
			@RequestBody(required = true) String body, @RequestHeader HttpHeaders requestHeaders);

}
