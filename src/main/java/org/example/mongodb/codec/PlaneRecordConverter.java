package org.example.mongodb.codec;

import static org.example.mongodb.service.Constants.FIELD_CURRENT_LOCATION;
import static org.example.mongodb.service.Constants.FIELD_DISTANCE_TRAVELLED;
import static org.example.mongodb.service.Constants.FIELD_HEADING;
import static org.example.mongodb.service.Constants.FIELD_LANDED;
import static org.example.mongodb.service.Constants.FIELD_LANDING_TIME;
import static org.example.mongodb.service.Constants.FIELD_MAINTENANCE_REQUIRE;
import static org.example.mongodb.service.Constants.FIELD_PLANE_TRAVEL_HISTORY;
import static org.example.mongodb.service.Constants.FIELD_ROUTE;
import static org.example.mongodb.service.Constants.FIELD_TOTAL_DISTANCE_TRAVEL;
import static org.example.mongodb.service.Constants.FIELD_TOTAL_TRAVEL_TIME;
import static org.example.mongodb.service.Constants.FIELD_TRAVEL_FROM;
import static org.example.mongodb.service.Constants.FIELD_TRAVEL_TO;
import static org.example.mongodb.service.Constants.FIELD_UNDERSCORE_ID;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.example.mongodb.model.PlaneRecord;
import org.example.mongodb.model.PlaneTravel;

public class PlaneRecordConverter {

	public Document convert(PlaneRecord planeRecord) {
        Document document = new Document();
        document.put(FIELD_UNDERSCORE_ID, planeRecord.getCallsign());
        document.put(FIELD_CURRENT_LOCATION, planeRecord.getCurrentLocation());
        document.put(FIELD_HEADING, planeRecord.getHeading());
        document.put(FIELD_ROUTE, planeRecord.getRoute());
        document.put(FIELD_LANDED, planeRecord.getLanded());

        return document;
    }

    public PlaneRecord convert(Document document) {
    	PlaneRecord planeRecord = new PlaneRecord();
    	planeRecord.setCallsign(document.getString(FIELD_UNDERSCORE_ID));
    	planeRecord.setCurrentLocation(document.getList(FIELD_CURRENT_LOCATION, Double.class));
    	
    	if(document.get(FIELD_HEADING) instanceof Double)
    	{
    		planeRecord.setHeading(document.getDouble(FIELD_HEADING).intValue());
    	}
    	else
    	{
    		planeRecord.setHeading(document.getInteger(FIELD_HEADING));
    	}
    	planeRecord.setRoute(document.getList(FIELD_ROUTE, String.class));
    	planeRecord.setLanded(document.getString(FIELD_LANDED));
    	
    	if(document.getLong(FIELD_TOTAL_TRAVEL_TIME) != null)
    	{
    		planeRecord.setTotalTravelTime(document.getLong(FIELD_TOTAL_TRAVEL_TIME));
    	}
    	
    	if(document.getDouble(FIELD_TOTAL_DISTANCE_TRAVEL) != null)
    	{
    		planeRecord.setTotalDistanceTravel(document.getDouble(FIELD_TOTAL_DISTANCE_TRAVEL));
    	}
    	
    	if(document.getBoolean(FIELD_MAINTENANCE_REQUIRE) != null)
    	{
    		planeRecord.setMaintenanceRequire(document.getBoolean(FIELD_MAINTENANCE_REQUIRE));
    	}
    	
    	List<Document> planeTravelHistoryList = document.getList(FIELD_PLANE_TRAVEL_HISTORY, Document.class);
    	
    	List<PlaneTravel> planeTravelList = new ArrayList<PlaneTravel>();
    	
    	for (Document planeTravelDocument : planeTravelHistoryList) {
			PlaneTravel planeTravel = new PlaneTravel();
			if(planeTravelDocument.getString(FIELD_TRAVEL_FROM) != null)
			{
				planeTravel.setTravelFrom(planeTravelDocument.getString(FIELD_TRAVEL_FROM));
			}
			
			if(planeTravelDocument.getString(FIELD_TRAVEL_TO) != null)
			{
				planeTravel.setTravelTo(planeTravelDocument.getString(FIELD_TRAVEL_TO));
			}
			
			if(planeTravelDocument.getDouble(FIELD_DISTANCE_TRAVELLED) != null)
			{
				planeTravel.setDistanceTravelled(planeTravelDocument.getDouble(FIELD_DISTANCE_TRAVELLED));
			}
			
			if(planeTravelDocument.getDate(FIELD_LANDING_TIME) != null)
			{
				planeTravel.setLandingTime(planeTravelDocument.getDate(FIELD_LANDING_TIME));
			}
			
			planeTravelList.add(planeTravel);
				
		}
    	planeRecord.setPlaneTravelHistory(planeTravelList);
    	
        return planeRecord;
    }
    
}
