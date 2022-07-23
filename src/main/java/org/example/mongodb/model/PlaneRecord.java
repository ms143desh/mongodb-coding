package org.example.mongodb.model;

import java.util.List;

public class PlaneRecord extends Plane{

	private long totalTravelTime;
	private double totalDistanceTravel;
	private boolean maintenanceRequire;
	private List<PlaneTravel> planeTravelHistory;
	
	public long getTotalTravelTime() {
		return totalTravelTime;
	}
	public void setTotalTravelTime(long totalTravelTime) {
		this.totalTravelTime = totalTravelTime;
	}
	public double getTotalDistanceTravel() {
		return totalDistanceTravel;
	}
	public void setTotalDistanceTravel(double totalDistanceTravel) {
		this.totalDistanceTravel = totalDistanceTravel;
	}
	public boolean isMaintenanceRequire() {
		return maintenanceRequire;
	}
	public void setMaintenanceRequire(boolean maintenanceRequire) {
		this.maintenanceRequire = maintenanceRequire;
	}
	public List<PlaneTravel> getPlaneTravelHistory() {
		return planeTravelHistory;
	}
	public void setPlaneTravelHistory(List<PlaneTravel> planeTravelHistory) {
		this.planeTravelHistory = planeTravelHistory;
	}
	
}
