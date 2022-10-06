/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package org.matsim.vsp.SmallScaleFreightTraffic;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.application.options.ShpOptions;
import org.matsim.application.options.ShpOptions.Index;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlReader;
import org.matsim.contrib.freight.carrier.CarrierUtils;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeReader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.contrib.freight.carrier.Tour.Pickup;
import org.matsim.contrib.freight.carrier.Tour.ServiceActivity;
import org.matsim.contrib.freight.carrier.Tour.TourElement;
import org.matsim.contrib.freight.jsprit.MatsimJspritFactory;
import org.matsim.contrib.freight.utils.FreightUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import com.google.common.base.Joiner;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.job.Job;
import com.graphhopper.jsprit.core.problem.solution.SolutionCostCalculator;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.solution.route.activity.BreakActivity;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;

/**
 * Utils for the SmallScaleFreightTraffic
 * 
 * @author Ricardo Ewert
 *
 */
public class SmallScaleFreightTrafficUtils {

	private static final Logger log = LogManager.getLogger(SmallScaleFreightTrafficUtils.class);
	private static final Joiner JOIN = Joiner.on("\t");

	/**
	 * Creates and return the Index of the zones shape.
	 * 
	 * @param shapeFileZonePath
	 * @return indexZones
	 */
	static Index getIndexZones(Path shapeFileZonePath) {

		ShpOptions shpZones = new ShpOptions(shapeFileZonePath, "EPSG:4326", StandardCharsets.UTF_8);
		Index indexZones = shpZones.createIndex("EPSG:4326", "areaID");
		return indexZones;
	}

	/**
	 * Creates and return the Index of the landuse shape.
	 * 
	 * @param shapeFileLandusePath
	 * @return indexLanduse
	 */
	static Index getIndexLanduse(Path shapeFileLandusePath) {

		ShpOptions shpLanduse = new ShpOptions(shapeFileLandusePath, "EPSG:4326", StandardCharsets.UTF_8);
		Index indexLanduse = shpLanduse.createIndex("EPSG:4326", "fclass");
		return indexLanduse;
	}

	/**
	 * Writes a csv file with result of the distribution per zone of the input data.
	 * 
	 * @param resultingDataPerZone
	 * @param outputFileInOutputFolder
	 * @param zoneIdNameConnection
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	static void writeResultOfDataDistribution(HashMap<String, Object2DoubleMap<String>> resultingDataPerZone,
			Path outputFileInOutputFolder, HashMap<String, String> zoneIdNameConnection)
			throws IOException, MalformedURLException {

		writeCSVWithCategoryHeader(resultingDataPerZone, outputFileInOutputFolder, zoneIdNameConnection);
		log.info("The data distribution is finished and written to: " + outputFileInOutputFolder);
	}

	/**
	 * Writer of data distribution data.
	 * 
	 * @param resultingDataPerZone
	 * @param outputFileInInputFolder
	 * @param zoneIdNameConnection
	 * @throws MalformedURLException
	 */
	private static void writeCSVWithCategoryHeader(HashMap<String, Object2DoubleMap<String>> resultingDataPerZone,
			Path outputFileInInputFolder, HashMap<String, String> zoneIdNameConnection) throws MalformedURLException {
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFileInInputFolder.toUri().toURL(),
				StandardCharsets.UTF_8, true);
		try {
			String[] header = new String[] { "areaID", "areaName", "Inhabitants", "Employee", "Employee Primary Sector",
					"Employee Construction", "Employee Secondary Sector Rest", "Employee Retail",
					"Employee Traffic/Parcels", "Employee Tertiary Sector Rest" };
			JOIN.appendTo(writer, header);
			writer.write("\n");
			for (String zone : resultingDataPerZone.keySet()) {
				List<String> row = new ArrayList<>();
				row.add(zone);
				row.add(zoneIdNameConnection.get(zone));
				for (String category : header) {
					if (!category.equals("areaID") && !category.equals("areaName"))
						row.add(String.valueOf((int) Math.round(resultingDataPerZone.get(zone).getDouble(category))));
				}
				JOIN.appendTo(writer, row);
				writer.write("\n");
			}

			writer.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Creates a population including the plans in preparation for the MATSim run.
	 * 
	 * @param controler
	 * @param usedTrafficType
	 * @param sample
	 * @param output
	 * @param inputDataDirectory
	 */
	static void createPlansBasedOnCarrierPlans(Controler controler, String usedTrafficType, double sample, Path output,
			Path inputDataDirectory) {
		Scenario scenario = controler.getScenario();
		Population population;
		if (usedTrafficType.equals("businessTraffic"))
			population = controler.getScenario().getPopulation();
		else {
			Path longDistancePopulation = inputDataDirectory
					.resolve("berlin_longDistanceFreight_" + (int) (sample * 100) + "pct.xml.gz");
			if (Files.exists(longDistancePopulation)) {
				log.error("Required population for the long distance freight {} not found", longDistancePopulation);

				population = PopulationUtils.readPopulation(longDistancePopulation.toString());
				log.info("Number of imported tours of longDistance freight traffic: " + population.getPersons().size());
				population.getPersons().values()
						.forEach(person -> VehicleUtils.insertVehicleIdsIntoAttributes(
								scenario.getPopulation().getPersons().get(person.getId()),
								(new HashMap<String, Id<Vehicle>>() {
									{
										put("freight", Id.createVehicleId(person.getId().toString()));
									}
								})));
				population.getPersons().values()
						.forEach(person -> scenario.getVehicles()
								.addVehicle(VehicleUtils.createVehicle(Id.createVehicleId(person.getId().toString()),
										scenario.getVehicles().getVehicleTypes()
												.get(Id.create("heavy40t", VehicleType.class)))));
			} else
				population = PopulationUtils.createPopulation(controler.getConfig());
		}
		PopulationFactory popFactory = population.getFactory();

		Population populationFromCarrier = (Population) scenario.getScenarioElement("allpersons");
		for (Person person : populationFromCarrier.getPersons().values()) {

			Plan plan = popFactory.createPlan();
			String carrierName = person.getId().toString().split("freight_")[1].split("_veh_")[0];
			Carrier relatedCarrier = FreightUtils.addOrGetCarriers(scenario).getCarriers()
					.get(Id.create(carrierName, Carrier.class));
			String subpopulation = relatedCarrier.getAttributes().getAttribute("subpopulation").toString();
			String mode = null;
			if (subpopulation.equals("businessTraffic"))
				mode = "car";
			else if (subpopulation.equals("freightTraffic"))
				mode = "freight";
			else if (subpopulation.equals("dispersedTraffic"))
				mode = relatedCarrier.getAttributes().getAttribute("networkMode").toString();
			final String usedMainMode = mode;
			List<PlanElement> tourElements = person.getSelectedPlan().getPlanElements();
			double tourStartTime = 0;
			for (PlanElement tourElement : tourElements) {

				if (tourElement instanceof Activity) {
					Activity activity = (Activity) tourElement;
					activity.setCoord(
							scenario.getNetwork().getLinks().get(activity.getLinkId()).getFromNode().getCoord());
					if (!activity.getType().equals("start"))
						activity.setEndTimeUndefined();
					else
						tourStartTime = activity.getEndTime().seconds();
					if (activity.getType().equals("end"))
						activity.setStartTime(tourStartTime + 8 * 3600);
					plan.addActivity(activity);
				}
				if (tourElement instanceof Leg) {
					Leg legActivity = popFactory.createLeg(usedMainMode);
					plan.addLeg(legActivity);
				}
			}
			Person newPerson = popFactory.createPerson(person.getId());
			newPerson.addPlan(plan);
			PopulationUtils.putSubpopulation(newPerson, subpopulation);
			//TODO also for existing carriers
			if (relatedCarrier.getAttributes().getAsMap().containsKey("tourStartArea"))
				PopulationUtils.putPersonAttribute(newPerson, "tourStartArea",
						relatedCarrier.getAttributes().getAttribute("tourStartArea"));
			VehicleUtils.insertVehicleIdsIntoAttributes(newPerson, (new HashMap<String, Id<Vehicle>>() {
				{
					put(usedMainMode, (Id.createVehicleId(person.getId().toString())));
				}
			}));
			population.addPerson(newPerson);
		}
		PopulationUtils.writePopulation(population,
				output.toString() + "/berlin_" + usedTrafficType + "_" + (int) (sample * 100) + "pct_plans.xml.gz");
		controler.getScenario().getPopulation().getPersons().clear();
	}

	/**
	 * Reads existing scenarios and add them to the scenario. If the scenario is
	 * part of the freightTraffic or businessTraffic the demand of the existing
	 * scenario reduces the demand of the small scale commercial traffic. The
	 * dispersedTraffic will be added additionally.
	 * 
	 * @param scenario
	 * @param sampleScenario
	 * @param inputDataDirectory 
	 * @param regionLinksMap 
	 * @throws Exception
	 */
	static void readExistingModels(Scenario scenario, double sampleScenario, Path inputDataDirectory, Map<String, HashMap<Id<Link>, Link>> regionLinksMap) throws Exception {
		// TODO

		String locationOfExistingModels = inputDataDirectory.getParent().getParent().resolve("existingModels")
				.resolve("existingModels.csv").toString();
		CSVParser parse = CSVFormat.DEFAULT.withDelimiter('\t').withFirstRecordAsHeader()
				.parse(IOUtils.getBufferedReader(locationOfExistingModels));
		for (CSVRecord record : parse) {
			String modelName = record.get("model");
			double sampleSizeExistingScenario = Double.parseDouble(record.get("sampleSize"));
			String modelTrafficType = record.get("trafficType");
			final Integer modelPurpose; // TODO input notwendig?
			if (record.get("purpose") != "")
				modelPurpose = Integer.parseInt(record.get("purpose"));
			else 
				modelPurpose = 0;
			final String vehicleType; // TODO input notwendig?
			if (record.get("vehicleType") != "")
				vehicleType = record.get("vehicleType");
			else
				vehicleType = "nn";
			final String modelMode;
			if (record.get("networkMode") != "")
				modelMode = record.get("networkMode");
			else
				modelMode = "tba";

			Path scenarioLocation = inputDataDirectory.getParent().getParent().resolve("existingModels")
					.resolve(modelName);
			if (!Files.exists(scenarioLocation.resolve("output_carriers.xml.gz")))
				throw new Exception("For the existig model " + modelName
						+ " no carrierFile exists. The carrierFile should have the name 'output_carriers.xml.gz'");
			if (!Files.exists(scenarioLocation.resolve("vehicleTypes.xml.gz")))
				throw new Exception("For the existig model " + modelName
						+ " no vehcileTypesFile exists. The vehcileTypesFile should have the name 'vehicleTypes.xml.gz'");

			log.info("Integrating existing scenario: " + modelName);

			CarrierVehicleTypes readVehicleTypes = new CarrierVehicleTypes();
			CarrierVehicleTypes usedVehicleTypes = new CarrierVehicleTypes();
			new CarrierVehicleTypeReader(readVehicleTypes)
					.readFile(scenarioLocation.resolve("vehicleTypes.xml.gz").toString());

			Carriers carriers = new Carriers();
			new CarrierPlanXmlReader(carriers, readVehicleTypes)
					.readFile(scenarioLocation.resolve("output_carriers.xml.gz").toString());

			if (sampleSizeExistingScenario < sampleScenario)
				throw new Exception("The sample size of the existing scenario " + modelName
						+ "is smaler than the sample size of the scenario. No upscaling for existing scenarios inplemented.");

			double sampleFactor = sampleScenario / sampleSizeExistingScenario;

			int numberOfToursExistingScenario = 0;
			for (Carrier carrier : carriers.getCarriers().values()) {
				numberOfToursExistingScenario = numberOfToursExistingScenario
						+ carrier.getSelectedPlan().getScheduledTours().size();
			}
			int sampledNumberOfToursExistingScenario = (int) Math.round(numberOfToursExistingScenario * sampleFactor);
			List<Carrier> carrierToRemove = new ArrayList<Carrier>();
			int countCarrier = 0;
			int remaindTours = 0;
			double roundingError = 0.;

			log.info("The existing scenario " + modelName + " is a " + (int) (sampleSizeExistingScenario * 100)
					+ "% scenario and has " + numberOfToursExistingScenario + " tours");
			log.info("The existing scenario " + modelName + " will be sampled down to the scenario sample size of "
					+ (int) (sampleScenario) + "%which results in " + sampledNumberOfToursExistingScenario + " tours.");
			for (Carrier carrier : carriers.getCarriers().values()) {

				countCarrier++;
				int numberOfOriginalTours = carrier.getSelectedPlan().getScheduledTours().size();
				int numberOfRemainingTours = (int) Math.round(numberOfOriginalTours * sampleFactor);
				roundingError = roundingError + numberOfRemainingTours - (numberOfOriginalTours * sampleFactor);
				int numberOfToursToRemove = numberOfOriginalTours - numberOfRemainingTours;
				List<ScheduledTour> toursToRemove = new ArrayList<ScheduledTour>();

				if (roundingError <= -1 && numberOfToursToRemove > 0) {
					numberOfToursToRemove = numberOfToursToRemove - 1;
					numberOfRemainingTours = numberOfRemainingTours + 1;
					roundingError = roundingError + 1;
				}
				if (roundingError >= 1 && numberOfRemainingTours != numberOfToursToRemove) {
					numberOfToursToRemove = numberOfToursToRemove + 1;
					numberOfRemainingTours = numberOfRemainingTours - 1;
					roundingError = roundingError - 1;
				}
				remaindTours = remaindTours + numberOfRemainingTours;
				if (remaindTours > sampledNumberOfToursExistingScenario) {
					remaindTours = remaindTours - 1;
					numberOfRemainingTours = numberOfRemainingTours - 1;
					numberOfToursToRemove = numberOfToursToRemove + 1;
				}
				if (countCarrier == carriers.getCarriers().size()
						&& remaindTours != sampledNumberOfToursExistingScenario) {
					numberOfRemainingTours = sampledNumberOfToursExistingScenario - remaindTours;
					numberOfToursToRemove = numberOfOriginalTours - numberOfRemainingTours;
					remaindTours = remaindTours + numberOfRemainingTours;
				}
				if (numberOfOriginalTours == numberOfToursToRemove) {
					carrierToRemove.add(carrier);
					continue;
				}

				for (ScheduledTour tour : carrier.getSelectedPlan().getScheduledTours()) {
					if (toursToRemove.size() < numberOfToursToRemove)
						toursToRemove.add(tour);
					else
						break;
				}

				carrier.getSelectedPlan().getScheduledTours().removeAll(toursToRemove);

				// remove services/shipments from removed tours
				if (carrier.getServices().size() != 0) {
					for (ScheduledTour removedTour : toursToRemove) {
						for (TourElement tourElement : removedTour.getTour().getTourElements()) {
							if (tourElement instanceof ServiceActivity) {
								ServiceActivity service = (ServiceActivity) tourElement;
								carrier.getServices().remove(service.getService().getId());
							}
						}
					}
				} else if (carrier.getShipments().size() != 0) {
					for (ScheduledTour removedTour : toursToRemove) {
						for (TourElement tourElement : removedTour.getTour().getTourElements()) {
							if (tourElement instanceof Pickup) {
								Pickup pickup = (Pickup) tourElement;
								carrier.getShipments().remove(pickup.getShipment().getId());
							}
						}
					}
				}
				// remove vehicles of removed tours and check if all vehicleTypes are still
				// needed
				if (carrier.getCarrierCapabilities().getFleetSize().equals(FleetSize.FINITE)) {
					for (ScheduledTour removedTour : toursToRemove) {
						carrier.getCarrierCapabilities().getCarrierVehicles().remove(removedTour.getVehicle().getId());
					}
				}
				else if (carrier.getCarrierCapabilities().getFleetSize().equals(FleetSize.INFINITE)) {
					carrier.getCarrierCapabilities().getCarrierVehicles().clear();
					for (ScheduledTour tour : carrier.getSelectedPlan().getScheduledTours()) {
						carrier.getCarrierCapabilities().getCarrierVehicles().put(tour.getVehicle().getId(), tour.getVehicle());
					}
				}
				List<VehicleType> vehcileTypesToRemove = new ArrayList<VehicleType>();
				for (VehicleType existingVehicleType : carrier.getCarrierCapabilities().getVehicleTypes()) {
					boolean vehicleTypeNeeded = false;
					for (CarrierVehicle vehicle : carrier.getCarrierCapabilities().getCarrierVehicles().values()) {
						if (vehicle.getType().equals(existingVehicleType)) {
							vehicleTypeNeeded = true;
							usedVehicleTypes.getVehicleTypes().put(existingVehicleType.getId(), existingVehicleType);
						}
					}
					if (vehicleTypeNeeded == false)
						vehcileTypesToRemove.add(existingVehicleType);
				}
				carrier.getCarrierCapabilities().getVehicleTypes().removeAll(vehcileTypesToRemove);
			}
			carrierToRemove.forEach(carrier -> carriers.getCarriers().remove(carrier.getId()));
			FreightUtils.getCarrierVehicleTypes(scenario).getVehicleTypes().putAll(usedVehicleTypes.getVehicleTypes());

			carriers.getCarriers().values().forEach(carrier -> {
				Carrier newCarrier = CarrierUtils
						.createCarrier(Id.create(modelName + "_" + carrier.getId().toString(), Carrier.class));
				newCarrier.getAttributes().putAttribute("subpopulation", modelTrafficType);
				newCarrier.getAttributes().putAttribute("purpose", modelPurpose);
				newCarrier.getAttributes().putAttribute("existingModel", modelName);
				newCarrier.getAttributes().putAttribute("networkMode", modelMode);
				newCarrier.getAttributes().putAttribute("vehicleType", vehicleType);
				newCarrier.setCarrierCapabilities(carrier.getCarrierCapabilities());
				newCarrier.setSelectedPlan(carrier.getSelectedPlan());
				
				List<String> startAreas = new ArrayList<String>();
				for (ScheduledTour tour : newCarrier.getSelectedPlan().getScheduledTours()) {
					startAreas.add(findZoneOfLink(tour.getTour().getStartLinkId(), regionLinksMap));
				}
				newCarrier.getAttributes().putAttribute("tourStartArea", startAreas.stream().collect(Collectors.joining(";")));
				
				if (carrier.getServices().size() > 0)
					newCarrier.getServices().putAll(carrier.getServices());
				else if (carrier.getShipments().size() > 0)
					newCarrier.getShipments().putAll(carrier.getShipments());
				CarrierUtils.setJspritIterations(newCarrier, 0); // because carrier already has solution
				
				//recalculate score for selectedPlan
		        VehicleRoutingProblem vrp = MatsimJspritFactory.createRoutingProblemBuilder(carrier, scenario.getNetwork()).build();
				VehicleRoutingProblemSolution solution = MatsimJspritFactory.createSolution(newCarrier.getSelectedPlan(), vrp);
		        SolutionCostCalculator solutionCostsCalculator = getObjectiveFunction(vrp, Double.MAX_VALUE);
		        double costs = solutionCostsCalculator.getCosts(solution) *(-1);
		        carrier.getSelectedPlan().setScore(costs);
				FreightUtils.addOrGetCarriers(scenario).getCarriers().put(newCarrier.getId(), newCarrier);
			});
		}
	}
	
	/**
	 * @param linkId
	 * @param regionLinksMap
	 * @return
	 */
	static String findZoneOfLink(Id<Link> linkId, Map<String, HashMap<Id<Link>, Link>> regionLinksMap) {
		for (String area : regionLinksMap.keySet()) {
			if (regionLinksMap.get(area).containsKey(linkId))
				return area;
		}
		return null;
	}
    /**
     * @param vrp
     * @param maxCosts
     * @return
     */
    private static SolutionCostCalculator getObjectiveFunction(final VehicleRoutingProblem vrp, final double maxCosts) {

        SolutionCostCalculator solutionCostCalculator = new SolutionCostCalculator() {
            @Override
            public double getCosts(VehicleRoutingProblemSolution solution) {
                double costs = 0.;

                for (VehicleRoute route : solution.getRoutes()) {
                    costs += route.getVehicle().getType().getVehicleCostParams().fix;
                    boolean hasBreak = false;
                    TourActivity prevAct = route.getStart();
                    for (TourActivity act : route.getActivities()) {
                        if (act instanceof BreakActivity) hasBreak = true;
                        costs += vrp.getTransportCosts().getTransportCost(prevAct.getLocation(), act.getLocation(), prevAct.getEndTime(), route.getDriver(), route.getVehicle());
                        costs += vrp.getActivityCosts().getActivityCost(act, act.getArrTime(), route.getDriver(), route.getVehicle());
                        prevAct = act;
                    }
                    costs += vrp.getTransportCosts().getTransportCost(prevAct.getLocation(), route.getEnd().getLocation(), prevAct.getEndTime(), route.getDriver(), route.getVehicle());
                    if (route.getVehicle().getBreak() != null) {
                        if (!hasBreak) {
                            //break defined and required but not assigned penalty
                            if (route.getEnd().getArrTime() > route.getVehicle().getBreak().getTimeWindow().getEnd()) {
                                costs += 4 * (maxCosts * 2 + route.getVehicle().getBreak().getServiceDuration() * route.getVehicle().getType().getVehicleCostParams().perServiceTimeUnit);
                            }
                        }
                    }
                }
                for(Job j : solution.getUnassignedJobs()){
                    costs += maxCosts * 2 * (11 - j.getPriority());
                }
                return costs;
            }
        };
        return solutionCostCalculator;
    }
}
