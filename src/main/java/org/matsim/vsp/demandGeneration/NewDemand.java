package org.matsim.vsp.demandGeneration;

import org.matsim.contrib.freight.carrier.TimeWindow;

final class NewDemand {

	private String carrierID;
	private Integer demandToDistribute;
	private Integer numberOfJobs;
	private Double shareOfPopulationWithThisDemand;
	private String[] areasFirstJobElement;
	private Integer numberOfFirstJobElementLocations;
	private String[] locationsOfFirstJobElement;
	private Integer firstJobElementTimePerUnit;
	private TimeWindow firstJobElementTimeWindow;
	private String[] areasSecondJobElement;
	private Integer numberOfSecondJobElementLocations;
	private String[] locationsOfSecondJobElement;
	private Integer secondJobElementTimePerUnit;
	private TimeWindow secondJobElementTimeWindow;

	NewDemand(String carrierID, Integer demandToDistribute, Integer numberOfJobs,
			Double shareOfPopulationWithThisDemand, String[] areasFirstJobElement,
			Integer numberOfFirstJobElementLocations, String[] locationsOfFirstJobElement,
			Integer firstJobElementTimePerUnit, TimeWindow firstJobElementTimeWindow) {
		this.setCarrierID(carrierID);
		this.setDemandToDistribute(demandToDistribute);
		this.setNumberOfJobs(numberOfJobs);
		this.setShareOfPopulationWithThisDemand(shareOfPopulationWithThisDemand);
		this.setAreasFirstJobElement(areasFirstJobElement);
		this.setNumberOfFirstJobElementLocations(numberOfFirstJobElementLocations);
		this.setLocationsOfFirstJobElement(locationsOfFirstJobElement);
		this.setFirstJobElementTimePerUnit(firstJobElementTimePerUnit);
		this.setFirstJobElementTimeWindow(firstJobElementTimeWindow);
	}

	NewDemand(String carrierID, Integer demandToDistribute, Integer numberOfJobs,
			Double shareOfPopulationWithThisDemand, String[] areasFirstJobElement,
			Integer numberOfFirstJobElementLocations, String[] locationsOfFirstJobElement,
			Integer firstJobElementTimePerUnit, TimeWindow firstJobElementTimeWindow, String[] areasSecondJobElement,
			Integer numberOfSecondJobElementLocations, String[] locationsOfSecondJobElement,
			Integer secondJobElementTimePerUnit, TimeWindow secondJobElementTimeWindow) {
		this.setCarrierID(carrierID);
		this.setDemandToDistribute(demandToDistribute);
		this.setNumberOfJobs(numberOfJobs);
		this.setShareOfPopulationWithThisDemand(shareOfPopulationWithThisDemand);
		this.setAreasFirstJobElement(areasFirstJobElement);
		this.setNumberOfFirstJobElementLocations(numberOfFirstJobElementLocations);
		this.setLocationsOfFirstJobElement(locationsOfFirstJobElement);
		this.setFirstJobElementTimePerUnit(firstJobElementTimePerUnit);
		this.setFirstJobElementTimeWindow(firstJobElementTimeWindow);
		this.setAreasSecondJobElement(areasSecondJobElement);
		this.setNumberOfSecondJobElementLocations(numberOfSecondJobElementLocations);
		this.setLocationsOfSecondJobElement(locationsOfSecondJobElement);
		this.setSecondJobElementTimePerUnit(secondJobElementTimePerUnit);
		this.setSecondJobElementTimeWindow(secondJobElementTimeWindow);
	}

	public String getCarrierID() {
		return carrierID;
	}

	public void setCarrierID(String carrierID) {
		this.carrierID = carrierID;
	}

	public Integer getDemandToDistribute() {
		return demandToDistribute;
	}

	public void setDemandToDistribute(Integer demandToDistribute) {
		this.demandToDistribute = demandToDistribute;
	}

	public Integer getNumberOfJobs() {
		return numberOfJobs;
	}

	public void setNumberOfJobs(Integer numberOfJobs) {
		this.numberOfJobs = numberOfJobs;
	}

	public Integer getFirstJobElementTimePerUnit() {
		return firstJobElementTimePerUnit;
	}

	public void setFirstJobElementTimePerUnit(Integer firstJobElementTimePerUnit) {
		this.firstJobElementTimePerUnit = firstJobElementTimePerUnit;
	}

	public String[] getAreasFirstJobElement() {
		return areasFirstJobElement;
	}

	public void setAreasFirstJobElement(String[] areasFirstJobElement) {
		this.areasFirstJobElement = areasFirstJobElement;
	}

	public TimeWindow getFirstJobElementTimeWindow() {
		return firstJobElementTimeWindow;
	}

	public void setFirstJobElementTimeWindow(TimeWindow firstJobElementTimeWindow) {
		this.firstJobElementTimeWindow = firstJobElementTimeWindow;
	}

	public Double getShareOfPopulationWithThisDemand() {
		return shareOfPopulationWithThisDemand;
	}

	public void setShareOfPopulationWithThisDemand(Double shareOfPopulationWithThisDemand) {
		this.shareOfPopulationWithThisDemand = shareOfPopulationWithThisDemand;
	}

	public TimeWindow getSecondJobElementTimeWindow() {
		return secondJobElementTimeWindow;
	}

	public void setSecondJobElementTimeWindow(TimeWindow secondJobElementTimeWindow) {
		this.secondJobElementTimeWindow = secondJobElementTimeWindow;
	}

	public Integer getSecondJobElementTimePerUnit() {
		return secondJobElementTimePerUnit;
	}

	public void setSecondJobElementTimePerUnit(Integer secondJobElementTimePerUnit) {
		this.secondJobElementTimePerUnit = secondJobElementTimePerUnit;
	}

	public Integer getNumberOfFirstJobElementLocations() {
		return numberOfFirstJobElementLocations;
	}

	public void setNumberOfFirstJobElementLocations(Integer numberOfFirstJobElementLocations) {
		this.numberOfFirstJobElementLocations = numberOfFirstJobElementLocations;
	}

	public String[] getLocationsOfFirstJobElement() {
		return locationsOfFirstJobElement;
	}

	public void setLocationsOfFirstJobElement(String[] locationsOfFirstJobElement) {
		this.locationsOfFirstJobElement = locationsOfFirstJobElement;
	}

	public String[] getAreasSecondJobElement() {
		return areasSecondJobElement;
	}

	public void setAreasSecondJobElement(String[] areasSecondJobElement) {
		this.areasSecondJobElement = areasSecondJobElement;
	}

	public Integer getNumberOfSecondJobElementLocations() {
		return numberOfSecondJobElementLocations;
	}

	public void setNumberOfSecondJobElementLocations(Integer numberOfSecondJobElementLocations) {
		this.numberOfSecondJobElementLocations = numberOfSecondJobElementLocations;
	}

	public String[] getLocationsOfSecondJobElement() {
		return locationsOfSecondJobElement;
	}

	public void setLocationsOfSecondJobElement(String[] locationsOfSecondJobElement) {
		this.locationsOfSecondJobElement = locationsOfSecondJobElement;
	}
}
