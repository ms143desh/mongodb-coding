package org.example.mongodb.service;

import static org.example.mongodb.service.Constants.COLL_CITIES;
import static org.example.mongodb.service.Constants.COLL_PLANES;
import static org.example.mongodb.service.Constants.COLL_PLANES_TRAVEL_ARCHIVES;
import static org.example.mongodb.service.Constants.DB_LOGISTICS;
import static org.example.mongodb.service.Constants.FIELD_COUNT;
import static org.example.mongodb.service.Constants.FIELD_DISTANCE_TRAVELLED;
import static org.example.mongodb.service.Constants.FIELD_LANDING_TIME;
import static org.example.mongodb.service.Constants.FIELD_MAINTENANCE_REQUIRE;
import static org.example.mongodb.service.Constants.FIELD_PLANE_TRAVEL_HISTORY;
import static org.example.mongodb.service.Constants.FIELD_POSITION;
import static org.example.mongodb.service.Constants.FIELD_TOTAL_DISTANCE_TRAVEL;
import static org.example.mongodb.service.Constants.FIELD_TOTAL_TRAVEL_TIME;
import static org.example.mongodb.service.Constants.FIELD_TRAVEL_FROM;
import static org.example.mongodb.service.Constants.FIELD_TRAVEL_TO;
import static org.example.mongodb.service.Constants.*;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.MongoCommandException;
import com.mongodb.TransactionOptions;
import com.mongodb.WriteConcern;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;

public class PlaneRecordService {

	private DatabaseService databaseService;
	private MongoClient mongoClient;
	
	private static final Logger logger = LoggerFactory.getLogger(PlaneRecordService.class);

	public PlaneRecordService(MongoClient mongoClient) {
		this.mongoClient = mongoClient;
		this.databaseService = new DatabaseService(mongoClient);
	}
	
	protected void updatePlaneTravelRecord(String planeId, String lastLanded, String landed) {
		
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
			
			//Update plane travel archives
			updatePlaneTravelArchivesTransaction(planeId);
			
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
	
	private void updatePlaneTravelArchivesTransaction(String planeId)
	{
		ClientSession clientSession = mongoClient.startSession();
        try {
        	
        	clientSession.startTransaction(TransactionOptions.builder().writeConcern(WriteConcern.MAJORITY).build());
        	
        	updatePlaneTravelArchives(clientSession, planeId);
        	
    		clientSession.commitTransaction();
    		//logger.info("Plane travel archive transaction completed!!");
        } catch (MongoCommandException e) {
        	clientSession.abortTransaction();
            logger.error("ROLLBACK - plane travel archive transaction!!");
        } finally {
        	clientSession.close();
        }
	}
	
	private void updatePlaneTravelArchives(ClientSession clientSession, String planeId)
	{
		MongoCollection<Document> planeCollection = databaseService.getGenericCollection(DB_LOGISTICS, COLL_PLANES);
		Bson filterPlaneId = Filters.eq(FIELD_UNDERSCORE_ID, planeId);
		int minimumTravelHistory = 20;
		int minimumUpdateCheck = 40;
		
    	MongoCursor<Document> archiveHistoryCursor = aggregatePlaneHistoryToArchive(clientSession, planeId, minimumTravelHistory, minimumUpdateCheck, planeCollection);
    	
    	List<Document> planeTravelArchive = null;
		int historyCount = 0;
		if(archiveHistoryCursor.hasNext())
		{
			Document archiveHistoryDocument = archiveHistoryCursor.next();
			historyCount = archiveHistoryDocument.getInteger(FIELD_COUNT);
			planeTravelArchive = archiveHistoryDocument.getList(FIELD_ARCHIVE_TRAVEL_HISTORY, Document.class);
			
			updatePlaneTravelArchive(clientSession, filterPlaneId, planeTravelArchive);
			removePlaneTravelHistory(clientSession, historyCount, minimumTravelHistory, filterPlaneId, planeCollection);
		}
	}
	
	private MongoCursor<Document> aggregatePlaneHistoryToArchive(ClientSession clientSession, String planeId, int minimumTravelHistory, int minimumUpdateCheck, MongoCollection<Document> planeCollection)
	{
		Bson aggMatchPlaneId = Aggregates.match(Filters.eq(FIELD_UNDERSCORE_ID, planeId));
		Bson aggProjectTravelHistory = Aggregates.project(Projections.fields(Projections.include(FIELD_PLANE_TRAVEL_HISTORY),
				Projections.computed(FIELD_COUNT, new Document("$size", "$".concat(FIELD_PLANE_TRAVEL_HISTORY).concat(".").concat(FIELD_TRAVEL_TO)))));
		Bson aggMatchCountGt = Aggregates.match(Filters.gt(FIELD_COUNT, minimumUpdateCheck));
		Bson aggProjectHistoryToArchive = Aggregates.project(Projections.fields(Projections.include(FIELD_COUNT),
				Projections.computed(FIELD_ARCHIVE_TRAVEL_HISTORY, new Document("$slice", Arrays.asList("$".concat(FIELD_PLANE_TRAVEL_HISTORY), minimumUpdateCheck - minimumTravelHistory)))));
		
		return planeCollection.aggregate(clientSession, Arrays.asList(aggMatchPlaneId, aggProjectTravelHistory, aggMatchCountGt, aggProjectHistoryToArchive)).cursor();
	}
	
	private void updatePlaneTravelArchive(ClientSession clientSession, Bson filterPlaneId, List<Document> planeTravelArchive)
	{
		Bson updateTravelArchive = Updates.addEachToSet(FIELD_PLANE_TRAVEL_HISTORY, planeTravelArchive);
		UpdateOptions updateOptionsTravelArchive = new UpdateOptions().upsert(true);
		
		MongoCollection<Document> planeTravelArchiveCollection = databaseService.getGenericCollection(DB_LOGISTICS, COLL_PLANES_TRAVEL_ARCHIVES);
		planeTravelArchiveCollection.updateOne(clientSession, filterPlaneId, updateTravelArchive, updateOptionsTravelArchive);
	}
	
	private void removePlaneTravelHistory(ClientSession clientSession, int historyCount, int minimumTravelHistory, Bson filterPlaneId, MongoCollection<Document> planeCollection)
	{
		Map<String, Object> documentMap = new HashMap<>();
		documentMap.put("$each", Arrays.asList());
		documentMap.put("$slice", -historyCount+minimumTravelHistory);
		Bson updatePlaneHistory = Updates.push(FIELD_PLANE_TRAVEL_HISTORY, new Document(documentMap));
		
		planeCollection.updateOne(clientSession, filterPlaneId, updatePlaneHistory);
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
