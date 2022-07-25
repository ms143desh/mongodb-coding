package org.example.mongodb.codec;

import static org.example.mongodb.service.Constants.FIELD_CURRENT_LOCATION;
import static org.example.mongodb.service.Constants.FIELD_HEADING;
import static org.example.mongodb.service.Constants.FIELD_LANDED;
import static org.example.mongodb.service.Constants.FIELD_ROUTE;
import static org.example.mongodb.service.Constants.FIELD_UNDERSCORE_ID;

import org.bson.Document;
import org.example.mongodb.model.Plane;

public class PlaneConverter {

    public Document convert(Plane plane) {
        Document document = new Document();
        document.put(FIELD_UNDERSCORE_ID, plane.getCallsign());
        document.put(FIELD_CURRENT_LOCATION, plane.getCurrentLocation());
        document.put(FIELD_HEADING, plane.getHeading());
        document.put(FIELD_ROUTE, plane.getRoute());
        document.put(FIELD_LANDED, plane.getLanded());
        
        return document;
    }

    public Plane convert(Document document) {
    	Plane plane = new Plane();
    	plane.setCallsign(document.getString(FIELD_UNDERSCORE_ID));
    	plane.setCurrentLocation(document.getList(FIELD_CURRENT_LOCATION, Double.class));
    	
    	if(document.get(FIELD_HEADING) instanceof Double)
    	{
    		plane.setHeading(document.getDouble(FIELD_HEADING).intValue());
    	}
    	else
    	{
    		plane.setHeading(document.getInteger(FIELD_HEADING));
    	}
    	plane.setRoute(document.getList(FIELD_ROUTE, String.class));
    	plane.setLanded(document.getString(FIELD_LANDED));
    	
        return plane;
    }
}
