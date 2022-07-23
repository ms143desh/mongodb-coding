package org.example.mongodb.codec;

import org.bson.Document;
import org.example.mongodb.model.Cargo;

public class CargoConverter {

	public Document convert(Cargo cargo) {
        Document document = new Document();
        document.put("_id", cargo.getId());
        document.put("destination", cargo.getDestination());
        document.put("location", cargo.getLocation());
        document.put("courier", cargo.getCourier());
        document.put("received", cargo.getReceived());
        document.put("status", cargo.getStatus());

        return document;
    }

    public Cargo convert(Document document) {
    	Cargo cargo = new Cargo();
    	cargo.setId(document.getString("_id"));
    	cargo.setDestination(document.getString("destination"));
    	cargo.setLocation(document.getString("location"));
    	cargo.setCourier(document.getString("courier"));
    	cargo.setReceived(document.getDate("received"));
    	cargo.setStatus(document.getString("status"));

        return cargo;
    }

}
