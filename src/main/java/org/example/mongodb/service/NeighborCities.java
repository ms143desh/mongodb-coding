package org.example.mongodb.service;

import java.util.List;

import org.bson.Document;

public class NeighborCities {

	private List<Document> neighbors;

	public List<Document> getNeighbors() {
		return neighbors;
	}

	public void setNeighbors(List<Document> neighbors) {
		this.neighbors = neighbors;
	}
	
}
