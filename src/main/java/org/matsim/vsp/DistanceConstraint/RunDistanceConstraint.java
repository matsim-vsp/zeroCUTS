package org.matsim.vsp.DistanceConstraint;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.freight.carriers.CarrierCapabilities.FleetSize;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;

import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.Jsprit;
import com.graphhopper.jsprit.core.algorithm.box.Jsprit.Strategy;
import com.graphhopper.jsprit.core.algorithm.state.StateId;
import com.graphhopper.jsprit.core.algorithm.state.StateManager;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.constraint.ConstraintManager;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.util.Solutions;
import com.graphhopper.jsprit.core.util.VehicleRoutingTransportCostsMatrix;

import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.controller.CarrierControllerUtils;
import org.matsim.freight.carriers.controller.CarrierModule;
import org.matsim.freight.carriers.controller.CarrierScoringFunctionFactory;
import org.matsim.freight.carriers.controller.CarrierStrategyManager;
import org.matsim.freight.carriers.jsprit.MatsimJspritFactory;
import org.matsim.freight.carriers.jsprit.NetworkBasedTransportCosts;
import org.matsim.freight.carriers.jsprit.NetworkBasedTransportCosts.Builder;
import org.matsim.freight.carriers.jsprit.NetworkRouter;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

public class RunDistanceConstraint {
	static final Logger log = LogManager.getLogger(RunDistanceConstraint.class);

	private static final String original_Chessboard = "https://raw.githubusercontent.com/matsim-org/matsim/master/examples/scenarios/freight-chessboard-9x9/grid9x9.xml";


	/**
	 * @param args
	 * @throws Exception
	 */

	public static void main(String[] args) throws Exception {

		Config config = ConfigUtils.createConfig();
		config.controller().setOutputDirectory("output/original_Chessboard/EWGT-DistanceConstraint");
		config.network().setInputFile(original_Chessboard);
		prepareConfig(config);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		Carriers carriers = new Carriers();

		CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes();

		FleetSize fleetSize = FleetSize.INFINITE;

//Option 1: Tour is possible with the vehicle with the small battery

		Carrier carrierV1 = CarriersUtils.createCarrier(Id.create("Carrier_Version1", Carrier.class));
		VehicleType newVT1 = VehicleUtils.createVehicleType(Id.create("LargeBattery_V1", VehicleType.class));
		newVT1.getCostInformation().setCostsPerMeter(0.00055).setCostsPerSecond(0.008).setFixedCost(100.);
		newVT1.getEngineInformation().getAttributes().putAttribute("fuelType", "electricity");
		newVT1.getEngineInformation().getAttributes().putAttribute("engeryCapacity", 450.);
		newVT1.getEngineInformation().getAttributes().putAttribute("engeryConsumptionPerKm", 15.);
		newVT1.getCapacity().setOther(80.);
		newVT1.setDescription("Carrier_Version1");
		VehicleType newVT2 = VehicleUtils.createVehicleType(Id.create("SmallBattery_V1", VehicleType.class));
		newVT2.getCostInformation().setCostsPerMeter(0.00055).setCostsPerSecond(0.008).setFixedCost(70.);
		newVT2.getEngineInformation().getAttributes().putAttribute("fuelType", "electricity");
		newVT2.getEngineInformation().getAttributes().putAttribute("engeryCapacity", 300.);
		newVT2.getEngineInformation().getAttributes().putAttribute("engeryConsumptionPerKm", 10.);
		newVT2.setDescription("Carrier_Version1");
		newVT2.getCapacity().setOther(80.);


		vehicleTypes.getVehicleTypes().put(newVT1.getId(), newVT1);
		vehicleTypes.getVehicleTypes().put(newVT2.getId(), newVT2);
		boolean threeServices = false;
		createServices(carrierV1, threeServices, carriers);
		createCarriers(fleetSize, carrierV1, vehicleTypes);

//Option 2: Tour is not possible with the vehicle with the small battery

		Carrier carrierV2 = CarriersUtils.createCarrier(Id.create("Carrier_Version2", Carrier.class));

		VehicleType newVT3 = VehicleUtils.createVehicleType(Id.create("LargeBattery_V2", VehicleType.class));
		newVT3.getCostInformation().setCostsPerMeter(0.00055).setCostsPerSecond(0.008).setFixedCost(100.);
		newVT3.getEngineInformation().getAttributes().putAttribute("fuelType", "electricity");
		newVT3.getEngineInformation().getAttributes().putAttribute("engeryCapacity", 450.);
		newVT3.getEngineInformation().getAttributes().putAttribute("engeryConsumptionPerKm", 15.);
		newVT3.setDescription("Carrier_Version2");
		newVT3.getCapacity().setOther(80.);
		VehicleType newVT4 = VehicleUtils.createVehicleType(Id.create("SmallBattery_V2", VehicleType.class));
		newVT4.getCostInformation().setCostsPerMeter(0.00055).setCostsPerSecond(0.008).setFixedCost(70.);
		newVT4.getEngineInformation().getAttributes().putAttribute("fuelType", "electricity");
		newVT4.getEngineInformation().getAttributes().putAttribute("engeryCapacity", 150.);
		newVT4.getEngineInformation().getAttributes().putAttribute("engeryConsumptionPerKm", 10.);
		newVT4.setDescription("Carrier_Version2");
		newVT4.getCapacity().setOther(80.);

		vehicleTypes.getVehicleTypes().put(newVT3.getId(), newVT3);
		vehicleTypes.getVehicleTypes().put(newVT4.getId(), newVT4);

		createServices(carrierV2, threeServices, carriers);
		createCarriers(fleetSize, carrierV2, vehicleTypes);

//Option 3: costs for using one long range vehicle are higher than the costs of using two short range truck	

		Carrier carrierV3 = CarriersUtils.createCarrier(Id.create("Carrier_Version3", Carrier.class));

		VehicleType newVT5 = VehicleUtils.createVehicleType(Id.create("LargeBattery_V3", VehicleType.class));
		newVT5.getCostInformation().setCostsPerMeter(0.00055).setCostsPerSecond(0.008).setFixedCost(100.);
		newVT5.getEngineInformation().getAttributes().putAttribute("fuelType", "electricity");
		newVT5.getEngineInformation().getAttributes().putAttribute("engeryCapacity", 450.);
		newVT5.getEngineInformation().getAttributes().putAttribute("engeryConsumptionPerKm", 15.);
		newVT5.setDescription("Carrier_Version3");
		newVT5.getCapacity().setOther(80.);
		VehicleType newVT6 = VehicleUtils.createVehicleType(Id.create("SmallBattery_V3", VehicleType.class));
		newVT6.getCostInformation().setCostsPerMeter(0.00055).setCostsPerSecond(0.008).setFixedCost(40.);
		newVT6.getEngineInformation().getAttributes().putAttribute("fuelType", "electricity");
		newVT6.getEngineInformation().getAttributes().putAttribute("engeryCapacity", 500.);
		newVT6.getEngineInformation().getAttributes().putAttribute("engeryConsumptionPerKm", 15.);
		newVT6.setDescription("Carrier_Version3");
		newVT6.getCapacity().setOther(40.);

		vehicleTypes.getVehicleTypes().put(newVT5.getId(), newVT5);
		vehicleTypes.getVehicleTypes().put(newVT6.getId(), newVT6);

		createServices(carrierV3, threeServices, carriers);
		createCarriers(fleetSize, carrierV3, vehicleTypes);

//Option 4: An additional shipment outside the range of both BEVtypes

		Carrier carrierV4 = CarriersUtils.createCarrier(Id.create("Carrier_Version4", Carrier.class));

		VehicleType newVT7 = VehicleUtils.createVehicleType(Id.create("LargeBattery_V4", VehicleType.class));
		newVT7.getCostInformation().setCostsPerMeter(0.00055).setCostsPerSecond(0.008).setFixedCost(100.);
		newVT7.getEngineInformation().getAttributes().putAttribute("fuelType", "electricity");
		newVT7.getEngineInformation().getAttributes().putAttribute("engeryCapacity", 450.);
		newVT7.getEngineInformation().getAttributes().putAttribute("engeryConsumptionPerKm", 15.);
		newVT7.setDescription("Carrier_Version4");
		newVT7.getCapacity().setOther(120.);
		VehicleType newVT8 = VehicleUtils.createVehicleType(Id.create("SmallBattery_V4", VehicleType.class));
		newVT8.getCostInformation().setCostsPerMeter(0.00055).setCostsPerSecond(0.008).setFixedCost(70.);
		newVT8.getEngineInformation().getAttributes().putAttribute("fuelType", "electricity");
		newVT8.getEngineInformation().getAttributes().putAttribute("engeryCapacity", 300.);
		newVT8.getEngineInformation().getAttributes().putAttribute("engeryConsumptionPerKm", 10.);
		newVT8.setDescription("Carrier_Version4");
		newVT8.getCapacity().setOther(120.);
		VehicleType newVT9 = VehicleUtils.createVehicleType(Id.create("DieselVehicle", VehicleType.class));
		newVT9.getCostInformation().setCostsPerMeter(0.00055).setCostsPerSecond(0.008).setFixedCost(400.);
		newVT9.getEngineInformation().getAttributes().putAttribute("fuelType", "diesel");
		newVT9.getEngineInformation().getAttributes().putAttribute("fuelConsumptionLitersPerMeter", 0.0001625);
		newVT9.setDescription("Carrier_Version4");
		newVT9.getCapacity().setOther(40.);

		vehicleTypes.getVehicleTypes().put(newVT7.getId(), newVT7);
		vehicleTypes.getVehicleTypes().put(newVT8.getId(), newVT8);
		vehicleTypes.getVehicleTypes().put(newVT9.getId(), newVT9);

		threeServices = true;
		createServices(carrierV4, threeServices, carriers);
		createCarriers(fleetSize, carrierV4, vehicleTypes);

		int jspritIterations = 100;
		solveJspritAndMATSim(scenario, vehicleTypes, carriers, jspritIterations);
		createResultFile(scenario, carriers, vehicleTypes);
	}

	/**
	 * Deletes the existing output file and sets the number of the last MATSim
	 * iteration.
	 *
	 * @param config
	 */
	static void prepareConfig(Config config) {
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		new OutputDirectoryHierarchy(config.controller().getOutputDirectory(), config.controller().getRunId(),
				config.controller().getOverwriteFileSetting(), ControllerConfigGroup.CompressionType.gzip);
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);

		config.controller().setLastIteration(0);
		config.global().setRandomSeed(4177);
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);

	}

	private static void createServices(Carrier carrier, boolean threeServices, Carriers carriers) {
// Service 1		
		CarrierService service1 = CarrierService.Builder
				.newInstance(Id.create("Service1", CarrierService.class), Id.createLinkId("j(3,8)"))
				.setServiceDuration(20).setServiceStartTimeWindow(TimeWindow.newInstance(8 * 3600, 10 * 3600))
				.setCapacityDemand(40).build();
		CarriersUtils.addService(carrier, service1);

// Service 2
		CarrierService service2 = CarrierService.Builder
				.newInstance(Id.create("Service2", CarrierService.class), Id.createLinkId("j(0,3)R"))
				.setServiceDuration(20).setServiceStartTimeWindow(TimeWindow.newInstance(8 * 3600, 10 * 3600))
				.setCapacityDemand(40).build();
		CarriersUtils.addService(carrier, service2);

// Service 3
		if (threeServices) {
			CarrierService service3 = CarrierService.Builder
					.newInstance(Id.create("Service3", CarrierService.class), Id.createLinkId("j(9,2)"))
					.setServiceDuration(20).setServiceStartTimeWindow(TimeWindow.newInstance(8 * 3600, 10 * 3600))
					.setCapacityDemand(40).build();
			CarriersUtils.addService(carrier, service3);
		}
		carriers.addCarrier(carrier);
	}

	/**
	 * Creates the vehicle at the depot, ads this vehicle to the carriers and sets
	 * the capabilities. Sets TimeWindow for the carriers.
	 * 
	 * @param
	 */
	private static void createCarriers(FleetSize fleetSize, Carrier singleCarrier,
									   CarrierVehicleTypes vehicleTypes) {
		double earliestStartingTime = 8 * 3600;
		double latestFinishingTime = 10 * 3600;
		List<CarrierVehicle> vehicles = new ArrayList<>();
		for (VehicleType singleVehicleType : vehicleTypes.getVehicleTypes().values()) {
			if (singleCarrier.getId().toString().equals(singleVehicleType.getDescription()))
				vehicles.add(createGarbageTruck(singleVehicleType.getId().toString(), earliestStartingTime,
						latestFinishingTime, singleVehicleType));
		}

		// define Carriers

		defineCarriers(fleetSize, singleCarrier, vehicles, vehicleTypes);
	}

	/**
	 * Method for creating a new carrierVehicle
	 * 
	 * @param
	 * 
	 * @return new carrierVehicle at the depot
	 */
	static CarrierVehicle createGarbageTruck(String vehicleName, double earliestStartingTime,
			double latestFinishingTime, VehicleType singleVehicleType) {

		return CarrierVehicle.Builder.newInstance(Id.create(vehicleName, Vehicle.class), Id.createLinkId("i(1,8)"), singleVehicleType)
				.setEarliestStart(earliestStartingTime).setLatestEnd(latestFinishingTime).build();
	}

	/**
	 * Defines and sets the Capabilities of the Carrier, including the vehicleTypes
	 * for the carriers
	 * 
	 * @param
	 * 
	 */
	private static void defineCarriers(FleetSize fleetSize, Carrier singleCarrier,
									   List<CarrierVehicle> vehicles, CarrierVehicleTypes vehicleTypes) {

		singleCarrier.setCarrierCapabilities(CarrierCapabilities.Builder.newInstance().setFleetSize(fleetSize).build());
		for (CarrierVehicle carrierVehicle : vehicles) {
			CarriersUtils.addCarrierVehicle(singleCarrier, carrierVehicle);
		}
		singleCarrier.getCarrierCapabilities().getVehicleTypes().addAll(vehicleTypes.getVehicleTypes().values());
	}

	private static void solveJspritAndMATSim(Scenario scenario, CarrierVehicleTypes vehicleTypes, Carriers carriers,
			int jspritIterations) {
		solveWithJsprit(scenario, carriers, jspritIterations, vehicleTypes);
		final Controler controler = new Controler(scenario);

		scoringAndManagerFactory(scenario, controler);

		//The VSP default settings are designed for person transport simulation. After talking to Kai, they will be set to WARN here. Kai MT may'23
		controler.getConfig().vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn);
		controler.run();
	}

	/**
	 * Solves with jsprit and gives a xml output of the plans and a plot of the
	 * solution. Because of using the distance constraint it is necessary to create
	 * a cost matrix before solving the vrp with jsprit. The jsprit algorithm solves
	 * a solution for every created carrier separately.
	 * 
	 * @param
	 */
	private static void solveWithJsprit(Scenario scenario, Carriers carriers, int jspritIteration,
			CarrierVehicleTypes vehicleTypes) {

		// Netzwerk integrieren und Kosten für jsprit
		Network network = scenario.getNetwork();
		Builder netBuilder = NetworkBasedTransportCosts.Builder.newInstance(network,
				vehicleTypes.getVehicleTypes().values());
		final NetworkBasedTransportCosts netBasedCosts = netBuilder.build();

		for (Carrier singleCarrier : carriers.getCarriers().values()) {

			netBuilder.setTimeSliceWidth(1800);

			VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder(singleCarrier,
					network);
			vrpBuilder.setRoutingCost(netBasedCosts);
			// VehicleRoutingProblem problem = vrpBuilder.build();

			VehicleRoutingTransportCostsMatrix distanceMatrix = DistanceConstraintUtils.createMatrix(vrpBuilder,
					singleCarrier, network, netBuilder);

			VehicleRoutingProblem problem = vrpBuilder.build();

			StateManager stateManager = new StateManager(problem);

			StateId distanceStateId = stateManager.createStateId("distance");

			stateManager.addStateUpdater(new DistanceUpdater(distanceStateId, stateManager, distanceMatrix));

			ConstraintManager constraintManager = new ConstraintManager(problem, stateManager);
			constraintManager.addConstraint(
					new DistanceConstraint(distanceStateId, stateManager, distanceMatrix, vehicleTypes),
					ConstraintManager.Priority.CRITICAL);

			// get the algorithm out-of-the-box, search solution and get the best one.
			//
			VehicleRoutingAlgorithm algorithm = Jsprit.Builder.newInstance(problem)
					.setStateAndConstraintManager(stateManager, constraintManager)
					.setProperty(Strategy.RADIAL_REGRET.toString(), "1.").buildAlgorithm();
//			VehicleRoutingAlgorithm algorithm = new SchrimpfFactory().createAlgorithm(problem);
			algorithm.setMaxIterations(jspritIteration);
			Collection<VehicleRoutingProblemSolution> solutions = algorithm.searchSolutions();
			VehicleRoutingProblemSolution bestSolution = Solutions.bestOf(solutions);

			// Routing bestPlan to Network
			CarrierPlan carrierPlanServices = MatsimJspritFactory.createPlan(bestSolution);
			NetworkRouter.routePlan(carrierPlanServices, netBasedCosts);
			singleCarrier.setSelectedPlan(carrierPlanServices);

		}
		new CarrierPlanWriter(carriers)
				.write(scenario.getConfig().controller().getOutputDirectory() + "/jsprit_CarrierPlans.xml");

	}

	/**
	 * @param
	 */
	static void scoringAndManagerFactory(Scenario scenario, final Controler controler) {
		CarrierScoringFunctionFactory scoringFunctionFactory = createMyScoringFunction2();
		CarrierStrategyManager planStrategyManagerFactory = createMyStrategyManager();

		CarriersUtils.addOrGetCarriers(scenario);
		CarrierModule listener = new CarrierModule();
		controler.addOverridingModule( new AbstractModule(){
			@Override
			public void install(){
				bind( CarrierScoringFunctionFactory.class ).toInstance(scoringFunctionFactory) ;
				bind( CarrierStrategyManager.class ).toInstance(planStrategyManagerFactory);
			}
		} ) ;
		controler.addOverridingModule(listener);
	}

	/**
	 * @return
	 */
	private static CarrierScoringFunctionFactory createMyScoringFunction2() {

		return new CarrierScoringFunctionFactory() {
			
			@Override
			public ScoringFunction createScoringFunction(Carrier carrier) {
				// TODO Auto-generated method stub
				return null;
			}
		};
	}

	/**
	 * @return
	 */
	private static CarrierStrategyManager createMyStrategyManager() {
		return CarrierControllerUtils.createDefaultCarrierStrategyManager();
	}

	/**
	 * @param scenario
	 * @param carriers
	 * @param vehicleTypes
	 * @throws IOException
	 */

	private static void createResultFile(Scenario scenario, Carriers carriers, CarrierVehicleTypes vehicleTypes)
			throws Exception {

		log.info("Starting");

		// String inputDir;
		Map<Id<Person>, Double> personId2tourDistance = new HashMap<>();
		Map<Id<Person>, Double> personId2tourConsumptionkWh = new HashMap<>();
		Map<String, Integer> usedNumberPerVehicleType = new HashMap<>();
		ArrayList<String> toursWithOverconsumption = new ArrayList<>();

		for (VehicleType singleVehicleType : vehicleTypes.getVehicleTypes().values()) {
			usedNumberPerVehicleType.put(singleVehicleType.getId().toString(), 0);
		}
		Network network = scenario.getNetwork();

		BufferedWriter writer;
		File file;
		file = new File(scenario.getConfig().controller().getOutputDirectory() + "/02_SummaryOutput.txt");

		writer = new BufferedWriter(new FileWriter(file, true));
		String now = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date());
		writer.write("Tourenstatisitik erstellt am: " + now + "\n\n");

		for (Carrier singleCarrier : carriers.getCarriers().values()) {

			double totalDistance = 0;
			int numberOfVehicles = 0;
			double distanceTour;
			int numCollections = 0;
			int tourNumberCarrier = 1;
			VehicleType vt = null;

			for (ScheduledTour scheduledTour : singleCarrier.getSelectedPlan().getScheduledTours()) {
				distanceTour = 0.0;
				for (VehicleType vt2 : singleCarrier.getCarrierCapabilities().getVehicleTypes()) {
					if (vt2.getId().toString().contains(scheduledTour.getVehicle().getId().toString())) {

						vt = vt2;
						break;
					}
				}

				int vehicleTypeCount = usedNumberPerVehicleType
						.get(scheduledTour.getVehicle().getId().toString());
				usedNumberPerVehicleType.replace(scheduledTour.getVehicle().getId().toString(),
						vehicleTypeCount + 1);

				List<Tour.TourElement> elements = scheduledTour.getTour().getTourElements();
				for (Tour.TourElement element : elements) {
					if (element instanceof Tour.ServiceActivity) {
						numCollections++;

					}
					if (element instanceof Tour.Leg legElement) {
						if (legElement.getRoute().getDistance() != 0)
							distanceTour = distanceTour
									+ RouteUtils.calcDistance((NetworkRoute) legElement.getRoute(), 0, 0, network);
					}
				}
				Id<Person> personId = Id.create(
						scheduledTour.getVehicle().getId().toString() + "-Tour " + tourNumberCarrier,
						Person.class);
				personId2tourDistance.put(personId, distanceTour);
				assert vt != null;
				if (vt.getEngineInformation().getAttributes().getAttribute("fuelType").equals("electricity")) {
					personId2tourConsumptionkWh.put(personId, (distanceTour / 1000) * (double) vt.getEngineInformation()
							.getAttributes().getAttribute("engeryConsumptionPerKm"));
				}
				totalDistance = totalDistance + distanceTour;
				tourNumberCarrier++;
			}
			numberOfVehicles = numberOfVehicles + (tourNumberCarrier - 1);
			writer.write("\n\n" + "Version: " + singleCarrier.getId().toString() + "\n");
			writer.write("\tAnzahl der Abholstellen (Soll): \t\t\t\t\t" + singleCarrier.getServices().size() + "\n");
			writer.write("\tAnzahl der Abholstellen ohne Abholung: \t\t\t\t"
					+ (singleCarrier.getServices().size() - numCollections) + "\n");
			writer.write("\tAnzahl der Fahrzeuge:\t\t\t\t\t\t\t\t" + numberOfVehicles + "\n");
			for (VehicleType singleVehicleType : vehicleTypes.getVehicleTypes().values()) {
				if (singleCarrier.getId().toString().equals(singleVehicleType.getDescription())) {
					writer.write("\t\t\tAnzahl Typ " + singleVehicleType.getId().toString() + ":\t\t\t\t"
							+ usedNumberPerVehicleType.get(singleVehicleType.getId().toString()) + "\n");
				}
			}
			writer.write(
					"\n" + "\tGefahrene Kilometer insgesamt:\t\t\t\t\t\t" + Math.round(totalDistance / 1000) + " km\n");
			writer.write("\tVerfügbare Fahrzeugtypen:\t\t\t\t\t\n\n");
			for (VehicleType singleVehicleType : vehicleTypes.getVehicleTypes().values()) {
				if (singleCarrier.getId().toString().equals(singleVehicleType.getDescription())) {
					writer.write("\t\t\tID: " + singleVehicleType.getId() + "\t\tAntrieb: "
							+ singleVehicleType.getEngineInformation().getAttributes().getAttribute("fuelType")
									.toString()
							+ "\t\tKapazität: " + singleVehicleType.getCapacity().getOther() + "\t\tFixkosten:"
							+ singleVehicleType.getCostInformation().getFixedCosts() + " €");
					if (singleVehicleType.getEngineInformation().getAttributes()
							.getAttribute("fuelType").equals("electricity")) {
						double electricityConsumptionPer100km;
						double electricityCapacityinkWh;
						electricityConsumptionPer100km = (double) singleVehicleType.getEngineInformation()
								.getAttributes().getAttribute("engeryConsumptionPerKm");
						electricityCapacityinkWh = (double) singleVehicleType.getEngineInformation().getAttributes()
								.getAttribute("engeryCapacity");

						writer.write("\t\tLadekapazität: " + electricityCapacityinkWh + " kWh\t\tVerbrauch: "
								+ electricityConsumptionPer100km + " kWh/100km\t\tReichweite: "
								+ (int) Math.round(electricityCapacityinkWh / electricityConsumptionPer100km)
								+ " km\n");
					} else
						writer.write("\n");
				}
			}
			writer.write("\n\n" + "\tTourID\t\t\t\t\t\tdistance (max Distance) (km)\tconsumption (capacity) (kWh)\n\n");

			for (Id<Person> id : personId2tourDistance.keySet()) {

				int tourDistance = (int) Math.round(personId2tourDistance.get(id) / 1000);
				int consumption = 0;
				double distanceRange = 0;
				double electricityCapacityinkWh = 0;
				double electricityConsumptionPerkm;

				for (VehicleType singleVehicleType : vehicleTypes.getVehicleTypes().values()) {

					if (id.toString().contains(singleVehicleType.getId().toString())
							&& singleVehicleType.getEngineInformation().getAttributes().getAttribute("fuelType").equals("electricity")) {

						electricityConsumptionPerkm = (double) singleVehicleType.getEngineInformation().getAttributes()
								.getAttribute("engeryConsumptionPerKm");
						electricityCapacityinkWh = (double) singleVehicleType.getEngineInformation().getAttributes()
								.getAttribute("engeryCapacity");
						distanceRange = (int) Math.round(electricityCapacityinkWh / electricityConsumptionPerkm);
						consumption = (int) Math.round(personId2tourConsumptionkWh.get(id));

						if (consumption > electricityCapacityinkWh)
							toursWithOverconsumption.add(id.toString());
					}
				}

				writer.write("\t" + id + "\t\t" + tourDistance);
				if (distanceRange > 0) {
					writer.write(" (" + distanceRange + ")\t\t\t\t\t\t" + consumption + " (" + electricityCapacityinkWh
							+ ")");
				} else
					writer.write("\t\t\t\t\t\t\t\t\t\t");
				writer.newLine();

			}
			personId2tourConsumptionkWh.clear();
			personId2tourDistance.clear();
		}
		writer.flush();
		writer.close();
		log.info("Output geschrieben");
		log.info("### Done.");
		if (!toursWithOverconsumption.isEmpty())
			throw new Exception("The tour(s) " + toursWithOverconsumption
					+ " have a higher consumption then their capacity");

	}

}

