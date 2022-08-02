package org.example.mongodb.service;

import static com.mongodb.client.model.Projections.computed;
import static com.mongodb.client.model.Projections.excludeId;
import static com.mongodb.client.model.Projections.fields;
import static com.mongodb.client.model.Projections.include;
import static org.example.mongodb.service.Constants.COLL_CARGOS;
import static org.example.mongodb.service.Constants.COLL_CITIES;
import static org.example.mongodb.service.Constants.COLL_PLANES;
import static org.example.mongodb.service.Constants.DB_LOGISTICS;
import static org.example.mongodb.service.Constants.FIELD_CALL_SIGN;
import static org.example.mongodb.service.Constants.FIELD_COUNTRY;
import static org.example.mongodb.service.Constants.FIELD_COURIER;
import static org.example.mongodb.service.Constants.FIELD_CURRENT_LOCATION;
import static org.example.mongodb.service.Constants.FIELD_FIRST_ROUTE;
import static org.example.mongodb.service.Constants.FIELD_HEADING;
import static org.example.mongodb.service.Constants.FIELD_LANDED;
import static org.example.mongodb.service.Constants.FIELD_LOCATION;
import static org.example.mongodb.service.Constants.FIELD_MAINTENANCE_REQUIRE;
import static org.example.mongodb.service.Constants.FIELD_NAME;
import static org.example.mongodb.service.Constants.FIELD_PLANE_TRAVEL_HISTORY;
import static org.example.mongodb.service.Constants.FIELD_RECEIVED;
import static org.example.mongodb.service.Constants.FIELD_ROUTE;
import static org.example.mongodb.service.Constants.FIELD_STATUS;
import static org.example.mongodb.service.Constants.FIELD_TOTAL_DISTANCE_TRAVEL;
import static org.example.mongodb.service.Constants.FIELD_TOTAL_TRAVEL_TIME;
import static org.example.mongodb.service.Constants.FIELD_UNDERSCORE_ID;
import static org.example.mongodb.service.Constants.IN_PROCESS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.bson.BsonArray;
import org.bson.BsonString;
import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;

public class DatabaseService {

	private MongoClient mongoClient;
	
	public DatabaseService(MongoClient mongoClient) {
		this.mongoClient = mongoClient;
	}
	
	public MongoCollection<Document> getGenericCollection(String databaseName, String collectionName)
	{
		return mongoClient.getDatabase(databaseName).getCollection(collectionName);
	}
	
	private static final Bson PLANE_DEFAULT_PROJECTION = fields(include(FIELD_CALL_SIGN, FIELD_CURRENT_LOCATION, FIELD_HEADING, FIELD_ROUTE, FIELD_LANDED), excludeId());
	private static final Bson CITY_DEFAULT_PROJECTION = fields(include(FIELD_NAME, FIELD_LOCATION, FIELD_COUNTRY), excludeId());
	private static final Bson PLANE_RECORD_DEFAULT_PROJECTION = fields(include(FIELD_CALL_SIGN, FIELD_CURRENT_LOCATION, FIELD_HEADING, FIELD_ROUTE, 
			FIELD_LANDED, FIELD_TOTAL_TRAVEL_TIME, FIELD_TOTAL_DISTANCE_TRAVEL, FIELD_MAINTENANCE_REQUIRE, FIELD_PLANE_TRAVEL_HISTORY), excludeId());
	
	protected List<Document> getAllPlanesDocument()
	{
		List<Document> planesList = new ArrayList<>();
		MongoCursor<Document> planesCursor = getGenericCollection(DB_LOGISTICS, COLL_PLANES).find().projection(PLANE_DEFAULT_PROJECTION).iterator();
		
		while(planesCursor.hasNext())
		{
			planesList.add(planesCursor.next());
		}
		
	    return planesList;
	}
	
	protected Document getPlaneDocumentById(String planeId)
	{
		Document plane = null;
		if(planeId != null)
		{
			Bson filter = Filters.eq(FIELD_UNDERSCORE_ID, planeId);
			MongoCursor<Document> planesCursor = getGenericCollection(DB_LOGISTICS, COLL_PLANES).find(filter).projection(PLANE_DEFAULT_PROJECTION).iterator();
			if(planesCursor.hasNext())
			{
				plane = planesCursor.next();
			}
		}
		
		return plane;
	}
	
	protected UpdateResult updatePlaneDocumentById(String planeId, List<Double> longLat, float heading, String landing)
	{
		Bson filter = Filters.eq(FIELD_UNDERSCORE_ID, planeId);
		Bson updates = null;
		if(landing != null)
		{
			updates = Updates.combine(Updates.set(FIELD_CURRENT_LOCATION, longLat),Updates.set(FIELD_HEADING, heading),Updates.set(FIELD_LANDED, landing));
		}
		else
		{
			updates = Updates.combine(Updates.set(FIELD_CURRENT_LOCATION, longLat),Updates.set(FIELD_HEADING, heading));
		}
                
		return getGenericCollection(DB_LOGISTICS, COLL_PLANES).updateOne(filter, updates);
	}
	
	protected UpdateResult updatePlaneDocumentByIdRoute(String planeId, String cityStr, boolean replaceRoute)
	{
		Bson filter = Filters.eq(FIELD_UNDERSCORE_ID, planeId);
		Bson updates = null;
		
		if(replaceRoute)
		{
			BsonArray bsonArray = new BsonArray();
			bsonArray.add(new BsonString(cityStr));
			updates = Updates.combine(Updates.set(FIELD_ROUTE, bsonArray));
		}
		else
		{
			updates = Updates.combine(Updates.addToSet(FIELD_ROUTE, cityStr));
		}
                
		return getGenericCollection(DB_LOGISTICS, COLL_PLANES).updateOne(filter, updates);
	}
	
	protected boolean validatePlaneFirstRouteAndLanded(String planeId)
	{
		boolean firstRouteLandedMatch = false;
		Bson matchPlaneId = Aggregates.match(Filters.eq(FIELD_UNDERSCORE_ID, planeId));
		Bson project = Aggregates.project(fields(excludeId(),
				include(FIELD_LANDED),
				computed(FIELD_FIRST_ROUTE, new Document("$arrayElemAt", Arrays.asList("$route", 0)))));
		
		MongoCursor<Document> documentAggregateCursor = getGenericCollection(DB_LOGISTICS, COLL_PLANES).aggregate(Arrays.asList(matchPlaneId, project)).cursor();
		if(documentAggregateCursor.hasNext())
		{
			Document document = documentAggregateCursor.next();
			if(document.getString(FIELD_FIRST_ROUTE).equals(document.getString(FIELD_LANDED)))
			{
				firstRouteLandedMatch = true;
			}
		}
	    return firstRouteLandedMatch;
	}
	
	protected UpdateResult updatePlaneRemoveFirstRoute(String planeId)
	{
		Bson filter = Filters.eq(FIELD_UNDERSCORE_ID, planeId);
		Bson updates = Updates.combine(Updates.popFirst(FIELD_ROUTE));
		return getGenericCollection(DB_LOGISTICS, COLL_PLANES).updateOne(filter, updates);
	}
	
	protected List<Document> getAllCitiesDocument()
	{
		List<Document> citiesList = new ArrayList<>();
		MongoCursor<Document> cityCursor = getGenericCollection(DB_LOGISTICS, COLL_CITIES).find().projection(CITY_DEFAULT_PROJECTION).iterator();
		
		while(cityCursor.hasNext())
		{
			citiesList.add(cityCursor.next());
		}
		
	    return citiesList;
	}
	
	protected Document getCityDocumentById(String cityId)
	{
		Document city = null;
		if(cityId != null)
		{
			Bson filter = Filters.eq(FIELD_UNDERSCORE_ID, cityId);
			MongoCursor<Document> cityCursor = getGenericCollection(DB_LOGISTICS, COLL_CITIES).find(filter).projection(CITY_DEFAULT_PROJECTION).iterator();
			if(cityCursor.hasNext())
			{
				city = cityCursor.next();
			}
		}
		
		return city;
	}
	
	protected List<Document> getNearbyCityDocumentsForId(List<Double> location, int count)
	{
		List<Document> citiesList = new ArrayList<>();
		Bson geometryFilter = Filters.eq("$geometry", Filters.and(Filters.eq("type", "Point"), Filters.eq("coordinates", location)));
		Bson nearFilter = Filters.eq("$near", Filters.and(geometryFilter, Filters.eq("$minDistance", 0.10)));
		Bson positionFilter = Filters.eq(FIELD_LOCATION, nearFilter);
		
		MongoCursor<Document> cityCursor = getGenericCollection(DB_LOGISTICS, COLL_CITIES).find(positionFilter).projection(CITY_DEFAULT_PROJECTION).limit(count).iterator();
		while(cityCursor.hasNext())
		{
			citiesList.add(cityCursor.next());
		}
		
	    return citiesList;
	}
	
	protected List<Document> getAllCitiesDocumentByIds(List<String> cityIds)
	{
		List<Document> citiesList = new ArrayList<>();
		Bson inFilter = Filters.in(FIELD_UNDERSCORE_ID, cityIds);
		
		MongoCursor<Document> cityCursor = getGenericCollection(DB_LOGISTICS, COLL_CITIES).find(inFilter).projection(CITY_DEFAULT_PROJECTION).iterator();
		
		while(cityCursor.hasNext())
		{
			citiesList.add(cityCursor.next());
		}
		
	    return citiesList;
	}
	
	protected Document insertCargoDocument(Document cargo)
	{
		getGenericCollection(DB_LOGISTICS, COLL_CARGOS).insertOne(cargo);
		return cargo;
	}
	
	protected List<Document> getAllCargoDocumentAtLocationInProcess(String location)
	{
		List<Document> cargosList = new ArrayList<>();
		
		Bson filter = Filters.and(Filters.eq(FIELD_LOCATION, location), Filters.eq(FIELD_STATUS, IN_PROCESS));
		MongoCursor<Document> cargoCursor = getGenericCollection(DB_LOGISTICS, COLL_CARGOS).find(filter).projection(excludeId()).iterator();
		
		while(cargoCursor.hasNext())
		{
			cargosList.add(cargoCursor.next());
		}
		
	    return cargosList;
	}
	
	protected Document getCargoDocumentById(String cargoId)
	{
		Document cargo = null;
		if(cargoId != null)
		{
			Bson filter = Filters.eq(FIELD_UNDERSCORE_ID, cargoId);
			MongoCursor<Document> cargoCursor = getGenericCollection(DB_LOGISTICS, COLL_CARGOS).find(filter).projection(excludeId()).iterator();
			if(cargoCursor.hasNext())
			{
				cargo = cargoCursor.next();
			}
		}
		
		return cargo;
	}
	
	protected UpdateResult updateCargoDocument(String cargoId, String status, String courier, String location)
	{
		Bson filter = Filters.eq(FIELD_UNDERSCORE_ID, cargoId);
		Bson updates = null;
		if(status != null)
		{
			updates = Updates.combine(Updates.set(FIELD_STATUS, status), Updates.set(FIELD_RECEIVED, new Date()));
		}
		else if(location != null)
		{
			updates = Updates.combine(Updates.set(FIELD_LOCATION, location));
		}
		else
		{
			updates = Updates.combine(Updates.set(FIELD_COURIER, courier));
		}
		
		return getGenericCollection(DB_LOGISTICS, COLL_CARGOS).updateOne(filter, updates);
	}
	
	protected Document getPlaneHistoryRecordsDocumentById(String planeId)
	{
		Document planeRecord = null;
		if(planeId != null)
		{
			Bson filter = Filters.eq(FIELD_UNDERSCORE_ID, planeId);
			MongoCursor<Document> planesCursor = getGenericCollection(DB_LOGISTICS, COLL_PLANES).find(filter).projection(PLANE_RECORD_DEFAULT_PROJECTION).iterator();
			if(planesCursor.hasNext())
			{
				planeRecord = planesCursor.next();
			}
		}
		
		return planeRecord;
	}
	
}
