package org.example.mongodb.model;

import java.util.List;

public class Plane {

	private String callsign;
	
	private List<Double> currentLocation;
	
	private double heading;
	
	private List<String> route;
	
	private String landed;
	
	public String getCallsign() {
		return callsign;
	}
	public void setCallsign(String callsign) {
		this.callsign = callsign;
	}

	public double getHeading() {
		return heading;
	}
	public void setHeading(double heading) {
		this.heading = heading;
	}
	public List<Double> getCurrentLocation() {
		return currentLocation;
	}
	public void setCurrentLocation(List<Double> currentLocation) {
		this.currentLocation = currentLocation;
	}
	public List<String> getRoute() {
		return route;
	}
	public void setRoute(List<String> route) {
		this.route = route;
	}
	public String getLanded() {
		return landed;
	}
	public void setLanded(String landed) {
		this.landed = landed;
	}
	
}
