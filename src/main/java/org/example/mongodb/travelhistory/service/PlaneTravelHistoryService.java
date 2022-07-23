package org.example.mongodb.travelhistory.service;

import static org.example.mongodb.service.Constants.COLL_CITIES;
import static org.example.mongodb.service.Constants.COLL_PLANES;
import static org.example.mongodb.service.Constants.DB_LOGISTICS;
import static org.example.mongodb.service.Constants.FIELD_DISTANCE_TRAVELLED;
import static org.example.mongodb.service.Constants.FIELD_LANDING_TIME;
import static org.example.mongodb.service.Constants.FIELD_MAINTENANCE_REQUIRE;
import static org.example.mongodb.service.Constants.FIELD_PLANE_TRAVEL_HISTORY;
import static org.example.mongodb.service.Constants.FIELD_POSITION;
import static org.example.mongodb.service.Constants.FIELD_TOTAL_DISTANCE_TRAVEL;
import static org.example.mongodb.service.Constants.FIELD_TOTAL_TRAVEL_TIME;
import static org.example.mongodb.service.Constants.FIELD_TRAVEL_FROM;
import static org.example.mongodb.service.Constants.FIELD_TRAVEL_TO;
import static org.example.mongodb.service.Constants.FIELD_UNDERSCORE_ID;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.example.mongodb.service.DatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Updates;

public class PlaneTravelHistoryService {

	private DatabaseService databaseService;
	
	private static final Logger logger = LoggerFactory.getLogger(PlaneTravelHistoryService.class);

	public PlaneTravelHistoryService(MongoClient mongoClient) {
		this.databaseService = new DatabaseService(mongoClient);
	}
	
	public void updatePlaneTravelHistory(String planeId, String lastLanded, String landed) {
		
		//logger.info("Updating plane {} travel records on landed {}", planeId, landed);
		
		int planeAverageSpeed = 500; // speed in miles per hour
		
		double distanceTraveled = 0;
		long lastTravelTime = 0;
		
		if(lastLanded != null && !lastLanded.equals("") && !landed.equals(lastLanded))
		{
			MongoCollection<Document> planeCollection = databaseService.getGenericCollection(DB_LOGISTICS, COLL_PLANES);
			
			// Get city positions (long, lat)
			MongoCursor<Document> mongoCursor = findCityPositions(landed, lastLanded);
			
			//Calculate traveling distance and time taken
			if(mongoCursor.hasNext())
			{
				Document landedPositionDocument = mongoCursor.next();
				List<Double> landedPositions = landedPositionDocument.getList(FIELD_POSITION, Double.class);
				Document lastLandedPositionDocument = mongoCursor.next();
				List<Double> lastLandedPositions = lastLandedPositionDocument.getList(FIELD_POSITION, Double.class);
				
				distanceTraveled = calculateLongLatDistance(lastLandedPositions.get(0), lastLandedPositions.get(1), landedPositions.get(0), landedPositions.get(1));
				lastTravelTime = (long)(Math.round((distanceTraveled/planeAverageSpeed) * 100.0) / 100.0) * 60 * 60;
			}
			
			//Build and update plane travel history record
			updatePlaneTravelHistory(planeId, lastLanded, landed, distanceTraveled, lastTravelTime, planeCollection);
			
			//Build and update plane for maintenance
			updatePlaneMaintenance(planeId, planeCollection);
		}
    }
	
	private MongoCursor<Document> findCityPositions(String landed, String lastLanded)
	{
		MongoCollection<Document> documentCityCollection = databaseService.getGenericCollection(DB_LOGISTICS, COLL_CITIES);
		Bson filterCity = Filters.in(FIELD_UNDERSCORE_ID, Arrays.asList(landed,lastLanded));
		Bson projectCity = Projections.fields(Projections.excludeId(),Projections.include(FIELD_POSITION));
		return documentCityCollection.find(filterCity).projection(projectCity).cursor();
	}
	
	private void updatePlaneTravelHistory(String planeId, String lastLanded, String landed, double distanceTraveled, long lastTravelTime, MongoCollection<Document> planeCollection)
	{
		Document planeTravelDocument = new Document().append(FIELD_TRAVEL_FROM, lastLanded)
				.append(FIELD_TRAVEL_TO, landed)
				.append(FIELD_DISTANCE_TRAVELLED, distanceTraveled)
				.append(FIELD_LANDING_TIME, new Date());
		Bson filterPlaneId = Filters.eq(FIELD_UNDERSCORE_ID, planeId);
		Bson update = Updates.combine(Updates.inc(FIELD_TOTAL_TRAVEL_TIME, lastTravelTime), 
				Updates.inc(FIELD_TOTAL_DISTANCE_TRAVEL, distanceTraveled), 
				Updates.push(FIELD_PLANE_TRAVEL_HISTORY, planeTravelDocument));
		
		planeCollection.updateOne(filterPlaneId, update);
		
		//logger.info("updated plane {} travel history. added last landed {}", planeId, lastLanded);
	}
	
	private void updatePlaneMaintenance(String planeId, MongoCollection<Document> planeCollection)
	{
		Bson filterMaintenance = Filters.and(Filters.eq(FIELD_UNDERSCORE_ID, planeId), Filters.gt(FIELD_TOTAL_DISTANCE_TRAVEL, 50000));
		Bson updateMaintenance = Updates.set(FIELD_MAINTENANCE_REQUIRE, true);
		planeCollection.updateOne(filterMaintenance, updateMaintenance);
		
		//logger.info("Plane {} maintenance update completed", planeId);
	}
	
	private double calculateLongLatDistance(double startLongitude, double startLatitude, double endLongitude, double endLatitude) {

	    final int R = 6378; // Radius of the earth in km

	    double latitudeDistance = Math.toRadians(endLatitude - startLatitude);
	    
	    double longitudeDistance = Math.toRadians(endLongitude - startLongitude);
	    
	    double a = Math.sin(latitudeDistance / 2) * Math.sin(latitudeDistance / 2) 
	    		+ Math.cos(Math.toRadians(startLatitude)) * Math.cos(Math.toRadians(endLatitude)) * Math.sin(longitudeDistance / 2) * Math.sin(longitudeDistance / 2);
	    
	    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
	    
	    return Math.round(R * c * 0.621371 * 100.0) / 100.0; // convert to miles
	}
}
