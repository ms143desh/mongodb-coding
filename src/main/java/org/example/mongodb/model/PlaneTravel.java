package org.example.mongodb.model;

import java.util.Date;

public class PlaneTravel {

	private String travelFrom;
	private String travelTo;
	private double distanceTravelled;
	private Date landingTime;
	
	public String getTravelFrom() {
		return travelFrom;
	}
	public void setTravelFrom(String travelFrom) {
		this.travelFrom = travelFrom;
	}
	public String getTravelTo() {
		return travelTo;
	}
	public void setTravelTo(String travelTo) {
		this.travelTo = travelTo;
	}
	public double getDistanceTravelled() {
		return distanceTravelled;
	}
	public void setDistanceTravelled(double distanceTravelled) {
		this.distanceTravelled = distanceTravelled;
	}
	public Date getLandingTime() {
		return landingTime;
	}
	public void setLandingTime(Date landingTime) {
		this.landingTime = landingTime;
	}
	
}
