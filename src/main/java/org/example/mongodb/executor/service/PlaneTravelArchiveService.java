package org.example.mongodb.executor.service;

import static org.example.mongodb.service.Constants.COLL_PLANES;
import static org.example.mongodb.service.Constants.COLL_PLANES_TRAVEL_ARCHIVES;
import static org.example.mongodb.service.Constants.DB_LOGISTICS;
import static org.example.mongodb.service.Constants.FIELD_ARCHIVE_TRAVEL_HISTORY;
import static org.example.mongodb.service.Constants.FIELD_COUNT;
import static org.example.mongodb.service.Constants.FIELD_PLANE_TRAVEL_HISTORY;
import static org.example.mongodb.service.Constants.FIELD_TRAVEL_TO;
import static org.example.mongodb.service.Constants.FIELD_UNDERSCORE_ID;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.example.mongodb.service.DatabaseService;
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

public class PlaneTravelArchiveService {
	
	private MongoClient mongoClient;
	private DatabaseService databaseService;
	int minTravelHistory = 20;
	int maxTravelHistory = 40;
	
	private static final Logger logger = LoggerFactory.getLogger(PlaneTravelArchiveService.class);
	
	public PlaneTravelArchiveService(MongoClient mongoClient) {
		this.mongoClient = mongoClient;
		this.databaseService = new DatabaseService(mongoClient);
	}

	public void updatePlaneTravelArchives()
	{
		MongoCollection<Document> planeCollection = databaseService.getGenericCollection(DB_LOGISTICS, COLL_PLANES);
		
    	MongoCursor<Document> planeTravelHistoryCursor = aggregatePlaneTravelHistoryToArchive(planeCollection);
    	
    	startPlaneTravelArchivesSession(planeTravelHistoryCursor, planeCollection);
	}
	
	private MongoCursor<Document> aggregatePlaneTravelHistoryToArchive(MongoCollection<Document> planeCollection)
	{
		Bson aggProjectTravelHistory = Aggregates.project(Projections.fields(Projections.include(FIELD_PLANE_TRAVEL_HISTORY),
				Projections.computed(FIELD_COUNT, new Document("$size", "$".concat(FIELD_PLANE_TRAVEL_HISTORY).concat(".").concat(FIELD_TRAVEL_TO)))));
		Bson aggMatchCountGt = Aggregates.match(Filters.gt(FIELD_COUNT, maxTravelHistory));
		Bson aggProjectHistoryToArchive = Aggregates.project(Projections.fields(Projections.include(FIELD_COUNT),
				Projections.computed(FIELD_ARCHIVE_TRAVEL_HISTORY, new Document("$slice", Arrays.asList("$".concat(FIELD_PLANE_TRAVEL_HISTORY), maxTravelHistory - minTravelHistory)))));
		
		return planeCollection.aggregate(Arrays.asList(aggProjectTravelHistory, aggMatchCountGt, aggProjectHistoryToArchive)).cursor();
	}
	
	private void startPlaneTravelArchivesSession(MongoCursor<Document> planeTravelHistoryCursor, MongoCollection<Document> planeCollection)
	{
		ClientSession clientSession = mongoClient.startSession();
        try {
        	while(planeTravelHistoryCursor.hasNext())
    		{
    			int travelHistoryCount = 0;
    			List<Document> planeTravelArchive = null;
    			Document travelHistoryDocument = planeTravelHistoryCursor.next();
    			String planeId = travelHistoryDocument.getString(FIELD_UNDERSCORE_ID);
    			travelHistoryCount = travelHistoryDocument.getInteger(FIELD_COUNT);
    			planeTravelArchive = travelHistoryDocument.getList(FIELD_ARCHIVE_TRAVEL_HISTORY, Document.class);
    			
    			updatePlaneTravelArchivesTransaction(clientSession, planeId, travelHistoryCount, planeTravelArchive, planeCollection);
    		}
        } 
        catch (MongoCommandException e) {
        	clientSession.abortTransaction();
            logger.error("ROLLBACK - plane travel archive transaction!!");
        } 
        finally {
        	clientSession.close();
        }
	}
	
	private void updatePlaneTravelArchivesTransaction(ClientSession clientSession, String planeId, int travelHistoryCount, List<Document> planeTravelArchive, MongoCollection<Document> planeCollection)
	{
        	
        clientSession.startTransaction(TransactionOptions.builder().writeConcern(WriteConcern.MAJORITY).build());
        	
        Bson filterPlaneId = Filters.eq(FIELD_UNDERSCORE_ID, planeId);
        	
        updatePlaneTravelArchive(clientSession, filterPlaneId, planeTravelArchive);
		removePlaneTravelHistory(clientSession, travelHistoryCount, filterPlaneId, planeCollection);
        	
    	clientSession.commitTransaction();
    	//logger.info("Plane travel archive transaction completed!!");
	}
	
	private void updatePlaneTravelArchive(ClientSession clientSession, Bson filterPlaneId, List<Document> planeTravelArchive)
	{
		Bson updateTravelArchive = Updates.addEachToSet(FIELD_PLANE_TRAVEL_HISTORY, planeTravelArchive);
		UpdateOptions updateOptionsTravelArchive = new UpdateOptions().upsert(true);
		
		MongoCollection<Document> planeTravelArchiveCollection = databaseService.getGenericCollection(DB_LOGISTICS, COLL_PLANES_TRAVEL_ARCHIVES);
		planeTravelArchiveCollection.updateOne(clientSession, filterPlaneId, updateTravelArchive, updateOptionsTravelArchive);
	}
	
	private void removePlaneTravelHistory(ClientSession clientSession, int historyCount, Bson filterPlaneId, MongoCollection<Document> planeCollection)
	{
		Map<String, Object> documentMap = new HashMap<>();
		documentMap.put("$each", Arrays.asList());
		documentMap.put("$slice", -historyCount+minTravelHistory);
		Bson updatePlaneHistory = Updates.push(FIELD_PLANE_TRAVEL_HISTORY, new Document(documentMap));
		
		planeCollection.updateOne(clientSession, filterPlaneId, updatePlaneHistory);
	}
}
