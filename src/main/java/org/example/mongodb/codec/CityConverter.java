package org.example.mongodb.codec;

import java.util.List;

import org.bson.Document;
import org.example.mongodb.model.City;

public class CityConverter {

	public Document convert(City city) {
        Document document = new Document();
        document.put("_id", city.getName());
        document.put("position", city.getLocation());
        document.put("country", city.getCountry());

        return document;
    }

    public City convert(Document document) {
    	City city = new City();
    	city.setName(document.getString("_id"));
    	city.setLocation(document.get("position", List.class));
    	city.setCountry(document.getString("country"));

        return city;
    }
}
