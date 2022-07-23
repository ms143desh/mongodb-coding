package org.example.mongodb.service;

import static org.example.mongodb.service.Constants.ERROR_CODE;
import static org.example.mongodb.service.Constants.LOG_DOES_NOT_EXISTS;
import static org.example.mongodb.service.Constants.LOG_VALUES_ARE_NOT_CORRECT;

import java.util.ArrayList;
import java.util.List;

import org.example.mongodb.exceptions.MCDPProjectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.result.UpdateResult;

import spark.Request;
import spark.Response;

public class ValidationService {

	private static final Logger logger = LoggerFactory.getLogger(ValidationService.class);
	
	protected List<Double> validateLocation(Request req, String location)
	{
		List<Double> longLat = new ArrayList<>();
		String[]longLatStr = location.split(",");
		double latitude;
		double longitude;
		
		try {
			longitude = Double.valueOf(longLatStr[0]);
			latitude = Double.valueOf(longLatStr[1]);
		} catch (NumberFormatException e) {
			logger.error("Location {}{}", location, LOG_VALUES_ARE_NOT_CORRECT);
			req.attribute(ERROR_CODE, 400);
			throw new MCDPProjectException("Location " + location + LOG_VALUES_ARE_NOT_CORRECT);
		}
		
		if(latitude >= -90 && latitude <= 90 && longitude >= -180 && longitude <= 180)
		{
			longLat.add(longitude);
			longLat.add(latitude);
		}
		else
		{
			logger.error("Location {}{}", location, LOG_VALUES_ARE_NOT_CORRECT);
			req.attribute(ERROR_CODE, 400);
			throw new MCDPProjectException("Location " + location + LOG_VALUES_ARE_NOT_CORRECT); 
		}
		
		return longLat;
	}
	
	protected int validateHeading(Request req, String headingStr)
	{
		int heading;
		try {
			heading = Integer.valueOf(headingStr);
		} catch (NumberFormatException e) {
			logger.error("Heading {}{}", headingStr, LOG_VALUES_ARE_NOT_CORRECT);
			req.attribute(ERROR_CODE, 400);
			throw new MCDPProjectException("Heading " + headingStr + LOG_VALUES_ARE_NOT_CORRECT);
		}
		if(heading < 0 || heading > 360)
		{
			logger.error("Heading {}{}", heading, LOG_VALUES_ARE_NOT_CORRECT);
			req.attribute(ERROR_CODE, 400);
			throw new MCDPProjectException("Heading " + heading + LOG_VALUES_ARE_NOT_CORRECT);
		}
		
		return heading;
	}
	
	protected void validateUpdateResult(String id, UpdateResult updateResult, Request req, Response res)
	{
		if(updateResult.getMatchedCount() == 0 && updateResult.getModifiedCount() == 0)
		{
			logger.error("{}{}", id, LOG_DOES_NOT_EXISTS);
			req.attribute(ERROR_CODE, 404);
			throw new MCDPProjectException(id + LOG_DOES_NOT_EXISTS);
		}
		else
		{
			res.status(200);
		}
	}
	
	protected int validateCount(Request req, String countStr)
	{
		int count;
		
		try {
			count = Integer.valueOf(countStr);
		} catch (NumberFormatException e) {
			logger.error("Count {}{}", countStr, LOG_VALUES_ARE_NOT_CORRECT);
			req.attribute(ERROR_CODE, 400);
			throw new MCDPProjectException("Count " + countStr + LOG_VALUES_ARE_NOT_CORRECT);
		}
		return count;
	}
	
	protected void validateLocationDestination(Request req, String location, String destination)
	{
		if(location == null || destination == null)
		{
			logger.error("Location {} or Destination {} values are not provided correctly!!", location, destination);
			req.attribute(ERROR_CODE, 400);
			throw new MCDPProjectException("Location or Destination values are not provided correctly!!");
		}
		
		if(location.equals(destination))
		{
			logger.error("Location {} and Destination {} should not be same!!", location, destination);
			req.attribute(ERROR_CODE, 400);
			throw new MCDPProjectException("Location and Destination should not be same!!");
		}
	}
	
}
