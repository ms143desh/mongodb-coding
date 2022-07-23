package org.example.mongodb.model;

import java.util.List;

public class NeighborCities {

	private List<City> neighbors;

	public List<City> getNeighbors() {
		return neighbors;
	}

	public void setNeighbors(List<City> neighbors) {
		this.neighbors = neighbors;
	}
	
}
