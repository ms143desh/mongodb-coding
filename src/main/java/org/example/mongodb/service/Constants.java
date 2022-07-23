package org.example.mongodb.service;

public class Constants {

	private Constants()
	{
	}
	
	public static final String MONGODB_CONNECTION_URI = "mongodb://localhost:27017";
	public static final String DB_LOGISTICS = "logistics";
	public static final String COLL_PLANES = "planes";
	public static final String COLL_CITIES = "cities";
	public static final String COLL_CARGOS = "cargos";
	public static final String COLL_PLANES_TRAVEL_ARCHIVES = "planes_travel_archives";
	
	public static final String FIELD_UNDERSCORE_ID = "_id";
	public static final String FIELD_CURRENT_LOCATION = "currentLocation";
	public static final String FIELD_HEADING = "heading";
	public static final String FIELD_LANDED = "landed";
	public static final String FIELD_ROUTE = "route";
	public static final String FIELD_LOCATION = "location";
	public static final String FIELD_POSITION = "position";
	public static final String FIELD_DESTINATION = "destination";
	public static final String FIELD_STATUS = "status";
	public static final String FIELD_RECEIVED = "received";
	public static final String FIELD_COURIER = "courier";
	
	public static final String FIELD_LANDING_TIME = "landingTime";
	public static final String FIELD_DISTANCE_TRAVELLED = "distanceTravelled";
	public static final String FIELD_TRAVEL_FROM = "travelFrom";
	public static final String FIELD_TRAVEL_TO = "travelTo";
	
	public static final String FIELD_TOTAL_TRAVEL_TIME = "totalTravelTime";
	public static final String FIELD_TOTAL_DISTANCE_TRAVEL = "totalDistanceTravel";
	public static final String FIELD_PLANE_TRAVEL_HISTORY = "planeTravelHistory";
	public static final String FIELD_MAINTENANCE_REQUIRE = "maintenanceRequire";
	public static final String FIELD_ARCHIVE_TRAVEL_HISTORY = "archiveTravelHistory";
	
	public static final String FIELD_COUNT = "count";
	
	public static final String COLON_ID = ":id";
	public static final String COLON_LOCATION = ":location";
	public static final String COLON_HEADING = ":heading";
	public static final String COLON_LANDED = ":landed";
	public static final String COLON_CITY = ":city";
	public static final String COLON_COUNT = ":count";
	public static final String COLON_DESTINATION = ":destination";
	public static final String COLON_COURIER = "courier";
	public static final String ERROR_CODE = "errorCode";
	
	
	public static final String IN_PROCESS = "in process";
	public static final String DELIVERED = "delivered";
	public static final String LOG_VALUES_ARE_NOT_CORRECT = " values are not correct";
	public static final String LOG_DOES_NOT_EXISTS = " does not exists";
}
