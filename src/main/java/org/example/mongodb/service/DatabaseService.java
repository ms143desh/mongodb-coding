package org.example.mongodb.service;

import static org.example.mongodb.service.Constants.COLL_CARGOS;
import static org.example.mongodb.service.Constants.COLL_CITIES;
import static org.example.mongodb.service.Constants.COLL_PLANES;
import static org.example.mongodb.service.Constants.DB_LOGISTICS;
import static org.example.mongodb.service.Constants.FIELD_COURIER;
import static org.example.mongodb.service.Constants.FIELD_CURRENT_LOCATION;
import static org.example.mongodb.service.Constants.FIELD_HEADING;
import static org.example.mongodb.service.Constants.FIELD_LANDED;
import static org.example.mongodb.service.Constants.FIELD_LOCATION;
import static org.example.mongodb.service.Constants.FIELD_RECEIVED;
import static org.example.mongodb.service.Constants.FIELD_ROUTE;
import static org.example.mongodb.service.Constants.FIELD_STATUS;
import static org.example.mongodb.service.Constants.FIELD_UNDERSCORE_ID;
import static org.example.mongodb.service.Constants.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.bson.BsonArray;
import org.bson.BsonString;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.example.mongodb.model.Cargo;
import org.example.mongodb.model.City;
import org.example.mongodb.model.Plane;
import org.example.mongodb.model.PlaneRecord;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;

public class DatabaseService {

	private MongoClient mongoClient;
	
	public DatabaseService(MongoClient mongoClient) {
		this.mongoClient = mongoClient;
	}
	
	protected MongoCollection<Document> getGenericCollection(String databaseName, String collectionName)
	{
		return mongoClient.getDatabase(databaseName).getCollection(collectionName);
	}
	
	private MongoCollection<Plane> getPlaneCollection()
	{
		return mongoClient.getDatabase(DB_LOGISTICS).getCollection(COLL_PLANES, Plane.class);
	}
	
	private MongoCollection<City> getCityCollection()
	{
		return mongoClient.getDatabase(DB_LOGISTICS).getCollection(COLL_CITIES, City.class);
	}
	
	private MongoCollection<Cargo> getCargoCollection()
	{
		return mongoClient.getDatabase(DB_LOGISTICS).getCollection(COLL_CARGOS, Cargo.class);
	}
	
	private MongoCollection<PlaneRecord> getPlaneRecordCollection()
	{
		return mongoClient.getDatabase(DB_LOGISTICS).getCollection(COLL_PLANES, PlaneRecord.class);
	}
	
	protected List<Plane> getAllPlanesDocument()
	{
		List<Plane> planesList = new ArrayList<>();
		MongoCursor<Plane> planesCursor = getPlaneCollection().find().iterator();
		
		while(planesCursor.hasNext())
		{
			planesList.add(planesCursor.next());
		}
		
	    return planesList;
	}
	
	protected Plane getPlaneDocumentById(String planeId)
	{
		Plane plane = null;
		if(planeId != null)
		{
			Bson filter = Filters.eq(FIELD_UNDERSCORE_ID, planeId);
			MongoCursor<Plane> planesCursor = getPlaneCollection().find(filter).iterator();
			if(planesCursor.hasNext())
			{
				plane = planesCursor.next();
			}
		}
		
		return plane;
	}
	
	protected UpdateResult updatePlaneDocumentById(String planeId, List<Double> longLat, int heading, String landing)
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
                
		return getPlaneCollection().updateOne(filter, updates);
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
                
		return getPlaneCollection().updateOne(filter, updates);
	}
	
	protected boolean validatePlaneFirstRouteAndLanded(String planeId)
	{
		boolean firstRouteLandedMatch = false;
		Bson matchPlaneId = Aggregates.match(Filters.eq(FIELD_UNDERSCORE_ID, planeId));
		Bson project = Aggregates.project(Projections.fields(Projections.excludeId(),
				Projections.include(FIELD_LANDED),
				Projections.computed("firstRoute", new Document("$arrayElemAt", Arrays.asList("$route", 0)))));
		
		//Bson matchExpr = Aggregates.match(Filters.expr(Filters.eq("$firstRoute","$landed")));
	    
		MongoCursor<Document> documentAggregateCursor = getGenericCollection(DB_LOGISTICS, COLL_PLANES).aggregate(Arrays.asList(matchPlaneId, project)).cursor();
		if(documentAggregateCursor.hasNext())
		{
			Document document = documentAggregateCursor.next();
			if(document.getString("firstRoute").equals(document.getString(FIELD_LANDED)))
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
		return getPlaneCollection().updateOne(filter, updates);
	}
	
	protected List<City> getAllCitiesDocument()
	{
		List<City> citiesList = new ArrayList<>();
		MongoCursor<City> cityCursor = getCityCollection().find().iterator();
		
		while(cityCursor.hasNext())
		{
			citiesList.add(cityCursor.next());
		}
		
	    return citiesList;
	}
	
	protected City getCityDocumentById(String cityId)
	{
		City city = null;
		if(cityId != null)
		{
			Bson filter = Filters.eq(FIELD_UNDERSCORE_ID, cityId);
			MongoCursor<City> cityCursor = getCityCollection().find(filter).iterator();
			if(cityCursor.hasNext())
			{
				city = cityCursor.next();
			}
		}
		
		return city;
	}
	
	protected List<City> getNearbyCityDocumentsForId(List<Double> location, int count)
	{
		List<City> citiesList = new ArrayList<>();
		Bson geometryFilter = Filters.eq("$geometry", Filters.and(Filters.eq("type", "Point"), Filters.eq("coordinates", location)));
		Bson nearFilter = Filters.eq("$near", Filters.and(geometryFilter, Filters.eq("$minDistance", 0.10)));
		Bson positionFilter = Filters.eq(FIELD_POSITION, nearFilter);
		
		MongoCursor<City> cityCursor = getCityCollection().find(positionFilter).limit(count).iterator();
		while(cityCursor.hasNext())
		{
			citiesList.add(cityCursor.next());
		}
		
	    return citiesList;
	}
	
	protected List<City> getAllCitiesDocumentByIds(List<String> cityIds)
	{
		List<City> citiesList = new ArrayList<>();
		Bson inFilter = Filters.in(FIELD_UNDERSCORE_ID, cityIds);
		
		MongoCursor<City> cityCursor = getCityCollection().find(inFilter).iterator();
		
		while(cityCursor.hasNext())
		{
			citiesList.add(cityCursor.next());
		}
		
	    return citiesList;
	}
	
	protected Cargo insertCargoDocument(Cargo cargo)
	{
		getCargoCollection().insertOne(cargo);
		return cargo;
	}
	
	protected List<Cargo> getAllCargoDocumentAtLocationInProcess(String location)
	{
		List<Cargo> cargosList = new ArrayList<>();
		
		Bson filter = Filters.and(Filters.eq(FIELD_LOCATION, location), Filters.eq(FIELD_STATUS, IN_PROCESS));
		MongoCursor<Cargo> cargoCursor = getCargoCollection().find(filter).iterator();
		
		while(cargoCursor.hasNext())
		{
			cargosList.add(cargoCursor.next());
		}
		
	    return cargosList;
	}
	
	protected Cargo getCargoDocumentById(String cargoId)
	{
		Cargo cargo = null;
		if(cargoId != null)
		{
			Bson filter = Filters.eq(FIELD_UNDERSCORE_ID, cargoId);
			MongoCursor<Cargo> cargoCursor = getCargoCollection().find(filter).iterator();
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
			if(courier != null)
			{
				updates = Updates.combine(Updates.set(FIELD_COURIER, courier));
			}
			else
			{
				updates = Updates.combine(Updates.unset(FIELD_COURIER));
			}
		}
		
		return getCargoCollection().updateOne(filter, updates);
	}
	
	protected PlaneRecord getPlaneHistoryRecordsDocumentById(String planeId)
	{
		PlaneRecord planeRecord = null;
		if(planeId != null)
		{
			Bson filter = Filters.eq(FIELD_UNDERSCORE_ID, planeId);
			MongoCursor<PlaneRecord> planesCursor = getPlaneRecordCollection().find(filter).iterator();
			if(planesCursor.hasNext())
			{
				planeRecord = planesCursor.next();
			}
		}
		
		return planeRecord;
	}
	
}
