package org.example.mongodb.service;

import static org.example.mongodb.service.Constants.COLON_CITY;
import static org.example.mongodb.service.Constants.COLON_COUNT;
import static org.example.mongodb.service.Constants.COLON_COURIER;
import static org.example.mongodb.service.Constants.COLON_DESTINATION;
import static org.example.mongodb.service.Constants.COLON_HEADING;
import static org.example.mongodb.service.Constants.COLON_ID;
import static org.example.mongodb.service.Constants.COLON_LANDED;
import static org.example.mongodb.service.Constants.COLON_LOCATION;
import static org.example.mongodb.service.Constants.DELIVERED;
import static org.example.mongodb.service.Constants.ERROR_CODE;
import static org.example.mongodb.service.Constants.FIELD_DESTINATION;
import static org.example.mongodb.service.Constants.FIELD_LANDED;
import static org.example.mongodb.service.Constants.FIELD_LOCATION;
import static org.example.mongodb.service.Constants.FIELD_STATUS;
import static org.example.mongodb.service.Constants.IN_PROCESS;
import static org.example.mongodb.service.Constants.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.example.mongodb.exceptions.MCDPProjectException;
import org.example.mongodb.executor.service.PlaneTravelArchiveService;
import org.example.mongodb.executor.service.PlaneTravelHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.client.MongoClient;
import com.mongodb.client.result.UpdateResult;

import spark.Request;
import spark.Response;

public class APIRoutesService {

	private Gson gson = new Gson();
	private Gson gsonNull = new GsonBuilder().serializeNulls().create();
	
	private ValidationService validationService;
	private DatabaseService databaseService;
	private PlaneTravelHistoryService planeTravelHistoryService;
	private PlaneTravelArchiveService planeTravelArchiveService;
	private ExecutorService executorService = Executors.newFixedThreadPool(10);
	
	private static final Logger logger = LoggerFactory.getLogger(APIRoutesService.class);
	
	public APIRoutesService(MongoClient mongoClient) {
		validationService = new ValidationService();
		databaseService = new DatabaseService(mongoClient);
		planeTravelHistoryService = new PlaneTravelHistoryService(mongoClient);
		planeTravelArchiveService = new PlaneTravelArchiveService(mongoClient);
	}
	
	private void logAPICall(String requestUri)
	{
		//logger.info("API call for {}", requestUri);
	}
	
	public String getPlanes(Request req, Response res)
	{
		logAPICall(req.uri());
		List<Document> planesList = databaseService.getAllPlanesDocument();
	    return gson.toJson(planesList);
	}
	
	public String getPlaneById(Request req, Response res)
	{
		logAPICall(req.uri());
		String planeId = req.params(COLON_ID);
		Document plane = databaseService.getPlaneDocumentById(planeId);
		
		if(plane == null)
		{
			logger.error("PlaneId {}{}", planeId, LOG_DOES_NOT_EXISTS);
			req.attribute(ERROR_CODE, 404);
			throw new MCDPProjectException("PlaneId " + planeId + LOG_DOES_NOT_EXISTS);
		}
		
		return plane.toJson();
	}
	
	public String updatePlaneById(Request req, Response res)
	{
		logAPICall(req.uri());
		String location = req.params(COLON_LOCATION);
		String headingStr = req.params(COLON_HEADING);
		String landed = req.params(COLON_LANDED);
		String planeId = req.params(COLON_ID);
		
		List<Double> longLat = validationService.validateLocation(req, location);
		int heading = validationService.validateHeading(req, headingStr);
		
		Document city = null;
		Document plane = null;
		String planeLastLanded = null;
		
		if(landed != null)
		{
			city = databaseService.getCityDocumentById(landed);
			if(city == null)
			{
				logger.error("Landed value {}{}", landed, LOG_DOES_NOT_EXISTS);
				req.attribute(ERROR_CODE, 400);
				throw new MCDPProjectException("Landed value " + landed + LOG_DOES_NOT_EXISTS);
			}
			
			plane = databaseService.getPlaneDocumentById(planeId);
			if(plane == null)
			{
				logger.error("PlaneId {}{}", planeId, LOG_DOES_NOT_EXISTS);
				req.attribute(ERROR_CODE, 400);
				throw new MCDPProjectException("PlaneId " + planeId + LOG_DOES_NOT_EXISTS);
			}
			else
			{
				planeLastLanded = plane.getString(FIELD_LANDED);
			}
		}
		
		UpdateResult updateResult = databaseService.updatePlaneDocumentById(planeId, longLat, heading, landed);
		validationService.validateUpdateResult(planeId, updateResult, req, res);
		
		if(landed != null)
		{
			String lastLanded = planeLastLanded;
			executorService.execute(() -> {
		    	planeTravelHistoryService.updatePlaneTravelHistory(planeId, lastLanded, landed);
			});
		}
		
		return gson.toJson(databaseService.getPlaneDocumentById(planeId));
	}
	
	public String addPlaneRoute(Request req, Response res, boolean replaceRoute)
	{
		logAPICall(req.uri());
		String cityStr = req.params(COLON_CITY);
		String planeId = req.params(COLON_ID);
		
		Document city = databaseService.getCityDocumentById(cityStr);
		if(city == null)
		{
			logger.error("City value {}{}", cityStr, LOG_DOES_NOT_EXISTS);
			req.attribute(ERROR_CODE, 400);
			throw new MCDPProjectException("City value " + cityStr + LOG_DOES_NOT_EXISTS);
		}
		
		UpdateResult updateResult = databaseService.updatePlaneDocumentByIdRoute(planeId, cityStr, replaceRoute);
		validationService.validateUpdateResult(planeId, updateResult, req, res);
		
		return databaseService.getPlaneDocumentById(planeId).toJson();
	}
	
	public String removeFirstPlaneRoute(Request req, Response res)
	{
		logAPICall(req.uri());
		String planeId = req.params(COLON_ID);
		UpdateResult updateResult = databaseService.updatePlaneRemoveFirstRoute(planeId);
		validationService.validateUpdateResult(planeId, updateResult, req, res);
		return gson.toJson(databaseService.getPlaneDocumentById(planeId));
	}
	
	public String getCities(Request req, Response res)
	{
		logAPICall(req.uri());
		List<Document> citiesList = databaseService.getAllCitiesDocument();
	    return gson.toJson(citiesList);
	}
	
	public String getCityById(Request req, Response res)
	{
		logAPICall(req.uri());
		String cityId = req.params(COLON_ID);
		Document city = databaseService.getCityDocumentById(cityId);
		
		if(city == null)
		{
			logger.error("CityId {}{}", cityId, LOG_DOES_NOT_EXISTS);
			req.attribute(ERROR_CODE, 404);
			throw new MCDPProjectException("CityId " + cityId + LOG_DOES_NOT_EXISTS);
		}
		
		//return gson.toJson(city);
		return city.toJson();
	}
	
	public String getNearbyCitiesForId(Request req, Response res)
	{
		logAPICall(req.uri());
		String cityId = req.params(COLON_ID);
		String countStr = req.params(COLON_COUNT);
		int count = validationService.validateCount(req, countStr);
		
		Document city = databaseService.getCityDocumentById(cityId);
		if(city == null)
		{
			logger.error("CityId {}{}", cityId, LOG_DOES_NOT_EXISTS);
			req.attribute(ERROR_CODE, 404);
			throw new MCDPProjectException("CityId " + cityId + LOG_DOES_NOT_EXISTS);
		}
		
		List<Document> citiesList = databaseService.getNearbyCityDocumentsForId(city.getList(FIELD_LOCATION, Double.class), count);
		
		NeighborCities neighborCities = new NeighborCities();
		neighborCities.setNeighbors(citiesList);
		
		return gson.toJson(neighborCities);
	}
	
	public String createCargo(Request req, Response res)
	{
		logAPICall(req.uri());
		String location = req.params(COLON_LOCATION);
		String destination = req.params(COLON_DESTINATION);
		validationService.validateLocationDestination(req, location, destination);
		
		List<String> cityIds = new ArrayList<>();
		cityIds.add(destination);
		cityIds.add(location);
		
		List<Document> citiesList = databaseService.getAllCitiesDocumentByIds(cityIds);
		if(citiesList.size() < 2)
		{
			logger.error("Location {} or destination {}{}", location, destination, LOG_DOES_NOT_EXISTS);
			req.attribute(ERROR_CODE, 404);
			throw new MCDPProjectException("Location " + location + " or destination " + destination + LOG_DOES_NOT_EXISTS);
		}
		
		Document cargo = new Document();
		Date date = new Date();
		ObjectId id = new ObjectId();
		cargo.put(FIELD_UNDERSCORE_ID, id.toString());
		cargo.put(FIELD_ID, id.toString());
		cargo.put(FIELD_DESTINATION, destination);
		cargo.put(FIELD_LOCATION, location);
		cargo.put(FIELD_STATUS, IN_PROCESS);
		cargo.put(FIELD_COURIER, null);
		cargo.put(FIELD_RECEIVED, date.toString());
		
		cargo = databaseService.insertCargoDocument(cargo);
		return gson.toJson(cargo);
	}
	
	public String getCargoAtLocation(Request req, Response res)
	{
		logAPICall(req.uri());
		String location = req.params(COLON_LOCATION);
		List<Document> cargosList = databaseService.getAllCargoDocumentAtLocationInProcess(location);
		return gsonNull.toJson(cargosList);
	}
	
	public String cargoDelivered(Request req, Response res)
	{
		logAPICall(req.uri());
		String cargoId = req.params(COLON_ID);
		
		UpdateResult updateResult = databaseService.updateCargoDocument(cargoId, DELIVERED, null, null);
		validationService.validateUpdateResult(cargoId, updateResult, req, res);
		return gson.toJson(databaseService.getCargoDocumentById(cargoId));
	}
	
	public String cargoAssignCourier(Request req, Response res)
	{
		logAPICall(req.uri());
		String cargoId = req.params(COLON_ID);
		String courier = req.params(COLON_COURIER);
		
		Document plane = databaseService.getPlaneDocumentById(courier);
		
		if(plane == null)
		{
			logger.error("Courier {}{}", courier, LOG_DOES_NOT_EXISTS);
			req.attribute(ERROR_CODE, 404);
			throw new MCDPProjectException("Courier " + courier + LOG_DOES_NOT_EXISTS);
		}
		
		UpdateResult updateResult = databaseService.updateCargoDocument(cargoId, null, courier, null);
		validationService.validateUpdateResult(cargoId, updateResult, req, res);
		return gson.toJson(databaseService.getCargoDocumentById(cargoId));
	}
	
	public String cargoUnsetCourier(Request req, Response res)
	{
		logAPICall(req.uri());
		String cargoId = req.params(COLON_ID);
		UpdateResult updateResult = databaseService.updateCargoDocument(cargoId, null, null, null);
		validationService.validateUpdateResult(cargoId, updateResult, req, res);
		return gson.toJson(databaseService.getCargoDocumentById(cargoId));
	}
	
	public String cargoMove(Request req, Response res)
	{
		logAPICall(req.uri());
		String cargoId = req.params(COLON_ID);
		String location = req.params(COLON_LOCATION);
		
		UpdateResult updateResult = databaseService.updateCargoDocument(cargoId, null, null, location);
		validationService.validateUpdateResult(cargoId, updateResult, req, res);
		return gson.toJson(databaseService.getCargoDocumentById(cargoId));
	}
	
	public void schedulePlaneTravelArchives()
	{
		int initialStartDelay = 5;
		int scheduleDelay = 5;
		ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        Runnable runnablePlaneTravelArchiveTask = () -> {
        	planeTravelArchiveService.updatePlaneTravelArchives();
        };

        scheduledExecutorService.scheduleAtFixedRate(runnablePlaneTravelArchiveTask, initialStartDelay, scheduleDelay, TimeUnit.MINUTES);
	}
	
	public String getPlanesHistoryRecords(Request req, Response res)
	{
		logAPICall(req.uri());
		String planeId = req.params(COLON_ID);
		Document plane = databaseService.getPlaneHistoryRecordsDocumentById(planeId);
		
		if(plane == null)
		{
			logger.error("PlaneId {}{}", planeId, LOG_DOES_NOT_EXISTS);
			req.attribute(ERROR_CODE, 404);
			throw new MCDPProjectException("PlaneId " + planeId + LOG_DOES_NOT_EXISTS);
		}
		
		return gson.toJson(plane);
	}
}
