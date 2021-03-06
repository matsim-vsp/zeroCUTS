package org.matsim.vsp.SmallScaleFreightTraffic;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.options.ShpOptions;
import org.matsim.contrib.freight.Freight;
import org.matsim.contrib.freight.FreightConfigGroup;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierCapabilities;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlWriterV2;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.CarrierUtils;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeReader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.carrier.TimeWindow;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.contrib.freight.controler.CarrierModule;
import org.matsim.contrib.freight.utils.FreightUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vsp.freightAnalysis.FreightAnalyse;
import org.opengis.feature.simple.SimpleFeature;
import com.google.common.base.Joiner;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import picocli.CommandLine;

/**
 * @author Ricardo Ewert
 *
 */
@CommandLine.Command(name = "generate-business-passenger-traffic", description = "Generate business passenger traffic model", showDefaultValues = true)
public class CreateSmallScaleCommercialTrafficDemand implements Callable<Integer> {

	private static final Logger log = LogManager.getLogger(CreateSmallScaleCommercialTrafficDemand.class);
	private static final Joiner JOIN = Joiner.on("\t");
	private HashMap<Integer, HashMap<String, Double>> generationRatesStart = new HashMap<Integer, HashMap<String, Double>>();
	private HashMap<Integer, HashMap<String, Double>> generationRatesStop = new HashMap<Integer, HashMap<String, Double>>();
	private HashMap<String, HashMap<String, Double>> commitmentRatesStart = new HashMap<String, HashMap<String, Double>>();
	private HashMap<String, HashMap<String, Double>> commitmentRatesStop = new HashMap<String, HashMap<String, Double>>();
	private static HashMap<String, HashMap<String, ArrayList<SimpleFeature>>> buildingsPerZone = new HashMap<>();
	private static Path shapeFileLandusePath = null;
	private static Path shapeFileZonePath = null;
	private static Path shapeFileBuildingsPath = null;
	private static List<Link> links = new ArrayList<Link>();
	private static Map<String, List<Link>> regionLinksMap = new HashMap<>();
	private static HashMap<String, ArrayList<String>> landuseCategoriesAndDataConnection = new HashMap<String, ArrayList<String>>();
	private ArrayList<String> listOfZones = new ArrayList<String>();
	private ArrayList<String> listOfModes = new ArrayList<String>();
	private ArrayList<Integer> listOfPurposes = new ArrayList<Integer>();
	private static ShpOptions.Index indexLanduse = null;
	private static ShpOptions.Index indexZones = null;

	private enum LanduseConfiguration {
		useOnlyOSMLanduse, useOSMBuildingsAndLanduse, useExistingDataDistribution
	}

	private enum ZoneChoice {
		useDistricts, useTrafficCells
	}

	private enum ModeDifferentiation {
		createOneODMatrix, createSeperateODMatricesForModes
	}

	@CommandLine.Parameters(arity = "1", paramLabel = "INPUT", description = "Path to the freight data directory", defaultValue = "../public-svn/matsim/scenarios/countries/de/small-scale-commercial-traffic/input")
	private static Path inputDataDirectory;

	@CommandLine.Option(names = "--network", defaultValue = "../public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5.5-network.xml.gz", description = "Path to desired network file", required = true)
	private static Path networkPath;

	@CommandLine.Option(names = "--sample", defaultValue = "0.01", description = "Scaling factor of the freight traffic (0, 1)", required = true)
	private double sample;

	@CommandLine.Option(names = "--output", description = "Path to output folder", required = true, defaultValue = "output/BusinessPassengerTraffic/")
	private Path output;

	@CommandLine.Option(names = "--jspritIterations", description = "Set number of jsprit iterations", required = true, defaultValue = "15")
	private static int jspritIterations;

	@CommandLine.Option(names = "--modeDifferentiation", defaultValue = "createOneODMatrix", description = "Set option of mode differentiation:  createOneODMatrix, createSeperateODMatricesForModes")
	private ModeDifferentiation usedModeDifferentiation;

	@CommandLine.Option(names = "--zoneChoice", defaultValue = "useTrafficCells", description = "Set option input zones. Options: useDistricts, useTrafficCells")
	private ZoneChoice usedZoneChoice;
// useDistricts, useTrafficCells
	@CommandLine.Option(names = "--landuseConfiguration", defaultValue = "useOSMBuildingsAndLanduse", description = "Set option of used OSM data. Options: useOnlyOSMLanduse, useOSMBuildingsAndLanduse, useExistingDataDistribution")
	private LanduseConfiguration usedLanduseConfiguration;
// useOnlyOSMLanduse, useOSMBuildingsAndLanduse, useExistingDataDistribution

	private final static SplittableRandom rnd = new SplittableRandom(4711);

	public static void main(String[] args) {
		System.exit(new CommandLine(new CreateSmallScaleCommercialTrafficDemand()).execute(args));
	}

	@Override
	public Integer call() throws Exception {
		Configurator.setLevel("org.matsim.core.utils.geometry.geotools.MGC", Level.ERROR);
		/*
		 * Fragen: bei only landuse; was passiert mit construction?
		 */

		switch (usedZoneChoice) {
		case useDistricts:
			shapeFileZonePath = inputDataDirectory.resolve("shp").resolve("districts")
					.resolve("bezirksgrenzen_Berlin.shp");
			break;
		case useTrafficCells:
			shapeFileZonePath = inputDataDirectory.resolve("shp").resolve("districts")
					.resolve("verkehrszellen_Berlin.shp");
			break;
		default:
			break;
		}

		shapeFileLandusePath = inputDataDirectory.resolve("shp").resolve("landuse")
				.resolve("gis_osm_landuse_a_free_1.shp");

		shapeFileBuildingsPath = inputDataDirectory.resolve("shp").resolve("landuse")
				.resolve("allBuildingsWithLevels.shp");
// 		shapeFileBuildingsPath = rawDataDirectory.resolve("shp").resolve("landuse").resolve("buildingSample.shp");

		if (!Files.exists(shapeFileLandusePath)) {
			log.error("Required landuse shape file {} not found", shapeFileLandusePath);
		}
		if (!Files.exists(shapeFileBuildingsPath)) {
			log.error("Required OSM buildings shape file {} not found", shapeFileBuildingsPath);
		}
		if (!Files.exists(shapeFileZonePath)) {
			log.error("Required distrcits shape file {} not found", shapeFileZonePath);
		}
		output = output.resolve(java.time.LocalDate.now().toString() + "_" + java.time.LocalTime.now().toSecondOfDay());

		Config config = prepareConfig();

		prepareVehicles(config);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		HashMap<String, Object2DoubleMap<String>> resultingDataPerZone = createInputDataDistribution();

		readInputParamters();

		HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> trafficVolumePerTypeAndZone_start = createTrafficVolume_start(
				resultingDataPerZone);
		HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> trafficVolumePerTypeAndZone_stop = createTrafficVolume_stop(
				resultingDataPerZone);

		HashMap<TripDistributionMatrixKey, Integer> odMatrix = createTripDistribution(trafficVolumePerTypeAndZone_start,
				trafficVolumePerTypeAndZone_stop);

		createCarriers(config, scenario, odMatrix, resultingDataPerZone);
		Controler controler = prepareControler(scenario);

		FreightUtils.runJsprit(controler.getScenario());
		controler.run();
		new CarrierPlanXmlWriterV2((Carriers) controler.getScenario().getScenarioElement("carriers"))
				.write(config.controler().getOutputDirectory() + "/output_jspritCarriersWithPlans.xml");

		FreightAnalyse.main(new String[] { scenario.getConfig().controler().getOutputDirectory() });

		return 0;
	}

	/**
	 * @return
	 */
	private Config prepareConfig() {
		Config config = ConfigUtils.createConfig();
		config.global().setCoordinateSystem("EPSG:4326");
		config.network().setInputFile(networkPath.toString());
		config.controler().setOutputDirectory(output.toString());
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setLastIteration(0);
		config.global().setRandomSeed(4177);
		new OutputDirectoryHierarchy(config.controler().getOutputDirectory(), config.controler().getRunId(),
				config.controler().getOverwriteFileSetting(), ControlerConfigGroup.CompressionType.gzip);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		new File(output.resolve("caculatedData").toString()).mkdir();
		return config;
	}

	/**
	 * Prepares the controller.
	 * 
	 * @param scenario
	 * @return
	 */
	private Controler prepareControler(Scenario scenario) {
		Controler controler = new Controler(scenario);

		Freight.configure(controler);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				install(new CarrierModule());
			}
		});
		return controler;
	}

	/**
	 * @param config
	 * @param scenario
	 * @param odMatrix
	 * @param resultingDataPerZone
	 */
	private void createCarriers(Config config, Scenario scenario, HashMap<TripDistributionMatrixKey, Integer> odMatrix,
			HashMap<String, Object2DoubleMap<String>> resultingDataPerZone) {
		int maxNumberOfCarrier = getListOfPurposes(odMatrix).size() * getListOfZones(odMatrix).size()
				* getListOfModes(odMatrix).size();
		int createdCarrier = 0;
		for (Integer purpose : getListOfPurposes(odMatrix)) {
			for (String startZone : getListOfZones(odMatrix)) {
				boolean isStartingLocation = false;
				checkIfIsStartingPosition: {
					for (String possibleStopZone : getListOfZones(odMatrix)) {
						for (String possibleMode : getListOfModes(odMatrix)) {
							if (possibleMode.equals("total") || possibleMode.equals("it"))
								if (odMatrix.get(makeKey(startZone, possibleStopZone, possibleMode, purpose)) != 0) {
									isStartingLocation = true;
									break checkIfIsStartingPosition;
								}
						}
					}
				}
				if (isStartingLocation) {
					double occupancyRate = 0;
					String[] vehilceTypes = null;
					Integer serviceTimePerStop = null;
					ArrayList<String> startCategory = new ArrayList<String>();
					ArrayList<String> stopCategory = new ArrayList<String>();

					if (purpose == 1) {
						vehilceTypes = new String[] { "mercedes313" };
						serviceTimePerStop = (int) Math.round(71.7 * 60);
						occupancyRate = 1.5;
						startCategory.add("Employee Secondary Sector Rest");
						stopCategory.add("Employee Secondary Sector Rest");
					} else if (purpose == 2) {
						vehilceTypes = new String[] { "mercedes313" };
						serviceTimePerStop = (int) Math.round(70.4 * 60); // Durschnitt aus Handel,Transp.,Einw.
						occupancyRate = 1.6;
						startCategory.add("Employee Secondary Sector Rest");
						stopCategory.add("Employee Primary Sector");
						stopCategory.add("Employee Construction");
						stopCategory.add("Employee Secondary Sector Rest");
						stopCategory.add("Employee Retail");
						stopCategory.add("Employee Traffic/Parcels");
						stopCategory.add("Employee Tertiary Sector Rest");
						stopCategory.add("Inhabitants");
					} else if (purpose == 3) {
						vehilceTypes = new String[] { "golf1.4" };
						serviceTimePerStop = (int) Math.round(70.4 * 60);
						occupancyRate = 1.2;
						startCategory.add("Employee Retail");
						startCategory.add("Employee Tertiary Sector Rest");
						stopCategory.add("Employee Primary Sector");
						stopCategory.add("Employee Construction");
						stopCategory.add("Employee Secondary Sector Rest");
						stopCategory.add("Employee Retail");
						stopCategory.add("Employee Traffic/Parcels");
						stopCategory.add("Employee Tertiary Sector Rest");
						stopCategory.add("Inhabitants");
					} else if (purpose == 4) {
						vehilceTypes = new String[] { "golf1.4" };
						serviceTimePerStop = (int) Math.round(100.6 * 60);
						occupancyRate = 1.2;
						startCategory.add("Employee Traffic/Parcels");
						stopCategory.add("Employee Primary Sector");
						stopCategory.add("Employee Construction");
						stopCategory.add("Employee Secondary Sector Rest");
						stopCategory.add("Employee Retail");
						stopCategory.add("Employee Traffic/Parcels");
						stopCategory.add("Employee Tertiary Sector Rest");
						stopCategory.add("Inhabitants");
					} else if (purpose == 5) {
						vehilceTypes = new String[] { "mercedes313" };
						serviceTimePerStop = (int) Math.round(214.7 * 60);
						occupancyRate = 1.7;
						startCategory.add("Employee Construction");
						stopCategory.add("Employee Primary Sector");
						stopCategory.add("Employee Construction");
						stopCategory.add("Employee Secondary Sector Rest");
						stopCategory.add("Employee Retail");
						stopCategory.add("Employee Traffic/Parcels");
						stopCategory.add("Employee Tertiary Sector Rest");
						stopCategory.add("Inhabitants");
					}
					String selectedStartCategory = startCategory.get(rnd.nextInt(startCategory.size()));
					for (int i = 0; resultingDataPerZone.get(startZone).getDouble(selectedStartCategory) == 0
							&& i < startCategory.size() * 30; i++) {
						selectedStartCategory = startCategory.get(rnd.nextInt(startCategory.size()));
						if (i > startCategory.size() * 20)
							selectedStartCategory = stopCategory.get(rnd.nextInt(stopCategory.size()));
					} // TODO vielleicht besser l??sen

					String mode; // TODO Notwendigkeit ??berpr??fen
					if (getListOfModes(odMatrix).contains("total") && getListOfModes(odMatrix).size() == 1)
						mode = "total";
					else
						mode = "it";
					String carrierName = "Carrier_" + startZone + "_purpose_" + purpose;
					int numberOfDepots = (int) Math
							.ceil((double) getSumOfServicesForStartZone(odMatrix, startZone, mode, purpose)
									* serviceTimePerStop / (8 * 3600) * 2); // TODO
					FleetSize fleetSize = FleetSize.FINITE;
					int fixedNumberOfVehilcePerTypeAndLocation = 1;
					ArrayList<String> vehicleDepots = new ArrayList<String>();
					createdCarrier++;
					log.info("Create carrier number " + createdCarrier + " of a maximum Number of " + maxNumberOfCarrier
							+ " carriers.");
					log.info("Carrier: " + carrierName + "; depots: " + numberOfDepots + "; services: " + (int) Math
							.ceil(getSumOfServicesForStartZone(odMatrix, startZone, mode, purpose) / occupancyRate));
					createNewCarrierAndAddVehilceTypes(scenario, purpose, startZone,
							ConfigUtils.addOrGetModule(config, FreightConfigGroup.class), selectedStartCategory,
							carrierName, vehilceTypes, numberOfDepots, fleetSize,
							fixedNumberOfVehilcePerTypeAndLocation, vehicleDepots);
					log.info("Create services for carrier: " + carrierName);
					for (String stopZone : getListOfZones(odMatrix)) {
						int demand = 0;
						int numberOfJobs = (int) Math
								.ceil(odMatrix.get(makeKey(startZone, stopZone, mode, purpose)) / occupancyRate);
						if (numberOfJobs == 0)
							continue;
						String selectedStopCategory = stopCategory.get(rnd.nextInt(stopCategory.size()));
						while (resultingDataPerZone.get(stopZone).getDouble(selectedStopCategory) == 0)
							selectedStopCategory = stopCategory.get(rnd.nextInt(stopCategory.size()));
						String[] serviceArea = new String[] { stopZone };
						TimeWindow serviceTimeWindow = TimeWindow.newInstance(6 * 3600, 20 * 3600);
//						NewDemand newDemand = new NewDemand(carrierName, demand, numberOfJobs, null,
//								areasFirstJobElement, (int) numberOfJobs, null, serviceTimePerStop, serviceTimeWindow);
						createServices(scenario, purpose, vehicleDepots, selectedStopCategory, carrierName, demand,
								numberOfJobs, serviceArea, serviceTimePerStop, serviceTimeWindow);
					}
				}
			}
		}
		for (Carrier carrier : FreightUtils.getCarriers(scenario).getCarriers().values()) {
			CarrierUtils.setJspritIterations(carrier, jspritIterations);
		}
		log.warn("The jspritIterations are now set to " + jspritIterations + " in this simulation!");
		log.info("Finished creating " + createdCarrier + " carriers including related services.");

		new CarrierPlanXmlWriterV2(FreightUtils.addOrGetCarriers(scenario))
				.write(scenario.getConfig().controler().getOutputDirectory() + "/output_CarrierPlans.xml");
	}

	private void createServices(Scenario scenario, Integer purpose, ArrayList<String> noPossibleLinks,
			String selectedStopCategory, String carrierName, int demand, int numberOfJobs, String[] serviceArea,
			Integer serviceTimePerStop, TimeWindow serviceTimeWindow) {

		String stopZone = serviceArea[0];

		for (int i = 0; i < numberOfJobs; i++) {

			Id<Link> linkId = findPossibleLink(stopZone, selectedStopCategory, noPossibleLinks, scenario.getNetwork());
			Id<CarrierService> idNewService = Id.create(carrierName + "_" + linkId + "_" + rnd.nextInt(10000),
					CarrierService.class);

			CarrierService thisService = CarrierService.Builder.newInstance(idNewService, linkId)
					.setServiceDuration(serviceTimePerStop).setServiceStartTimeWindow(serviceTimeWindow).build();
			FreightUtils.getCarriers(scenario).getCarriers().get(Id.create(carrierName, Carrier.class)).getServices()
					.put(thisService.getId(), thisService);
		}

	}

	private void createNewCarrierAndAddVehilceTypes(Scenario scenario, Integer purpose, String startZone,
			FreightConfigGroup freightConfigGroup, String selectedStartCategory, String carrierName,
			String[] vehilceTypes, int numberOfDepots, FleetSize fleetSize, int fixedNumberOfVehilcePerTypeAndLocation,
			ArrayList<String> vehicleDepots) {

		Carriers carriers = FreightUtils.addOrGetCarriers(scenario);
		CarrierVehicleTypes carrierVehicleTypes = FreightUtils.getCarrierVehicleTypes(scenario);
		if (carrierVehicleTypes.getVehicleTypes().isEmpty()) {
			new CarrierVehicleTypeReader(carrierVehicleTypes)
					.readFile(freightConfigGroup.getCarriersVehicleTypesFile());
		} else
			carrierVehicleTypes = FreightUtils.getCarrierVehicleTypes(scenario);
		CarrierCapabilities carrierCapabilities = null;

		Carrier thisCarrier = CarrierUtils.createCarrier(Id.create(carrierName, Carrier.class));
		if (jspritIterations > 0)
			CarrierUtils.setJspritIterations(thisCarrier, jspritIterations);
		carrierCapabilities = CarrierCapabilities.Builder.newInstance().setFleetSize(fleetSize).build();
		carriers.addCarrier(thisCarrier);

		while (vehicleDepots.size() < numberOfDepots) {
			Id<Link> link = findPossibleLink(startZone, selectedStartCategory, null, scenario.getNetwork());
			vehicleDepots.add(link.toString());
		}
		for (String singleDepot : vehicleDepots) {
			for (String thisVehicleType : vehilceTypes) {
				int vehicleStartTime = rnd.nextInt(6 * 3600, 14 * 3600); // TODO Verteilung ??ber den Tag pr??fen
				int vehicleEndTime = vehicleStartTime + 8 * 3600;
				VehicleType thisType = carrierVehicleTypes.getVehicleTypes()
						.get(Id.create(thisVehicleType, VehicleType.class));
				if (fixedNumberOfVehilcePerTypeAndLocation == 0)
					fixedNumberOfVehilcePerTypeAndLocation = 1;
				for (int i = 0; i < fixedNumberOfVehilcePerTypeAndLocation; i++) {
					CarrierVehicle newCarrierVehicle = CarrierVehicle.Builder
							.newInstance(
									Id.create(
											thisType.getId().toString() + "_" + thisCarrier.getId().toString() + "_"
													+ (carrierCapabilities.getCarrierVehicles().size() + 1),
											Vehicle.class),
									Id.createLinkId(singleDepot), thisType)
							.setEarliestStart(vehicleStartTime).setLatestEnd(vehicleEndTime).build();
					carrierCapabilities.getCarrierVehicles().put(newCarrierVehicle.getId(), newCarrierVehicle);
					if (!carrierCapabilities.getVehicleTypes().contains(thisType))
						carrierCapabilities.getVehicleTypes().add(thisType);
				}
			}

			thisCarrier.setCarrierCapabilities(carrierCapabilities);
		}
	}

	private Id<Link> findPossibleLink(String zone, String selectedCategory, ArrayList<String> noPossibleLinks,
			Network network) {
		ShpOptions shpZones = new ShpOptions(shapeFileZonePath, "EPSG:4326", StandardCharsets.UTF_8);

		getIndexLanduse();
		getIndexZones();

		if (links.isEmpty()) {
			Network networkToChange = NetworkUtils.readNetwork(networkPath.toString());
			links = networkToChange.getLinks().values().stream().filter(l -> l.getAllowedModes().contains("car"))
					.collect(Collectors.toList());
			links.forEach(l -> l.getAttributes().putAttribute("newCoord",
					shpZones.createTransformation("EPSG:31468").transform(l.getCoord())));
			links.forEach(l -> l.getAttributes().putAttribute("zone",
					indexZones.query((Coord) l.getAttributes().getAttribute("newCoord"))));
			links = links.stream().filter(l -> l.getAttributes().getAttribute("zone") != null)
					.collect(Collectors.toList());
			links.forEach(l -> regionLinksMap
					.computeIfAbsent((String) l.getAttributes().getAttribute("zone"), (k) -> new ArrayList<>()).add(l));
		}
		if (buildingsPerZone.isEmpty()) {
			ShpOptions shpBuildings = new ShpOptions(shapeFileBuildingsPath, "EPSG:4326", StandardCharsets.UTF_8);
			List<SimpleFeature> buildingsFeatures = shpBuildings.readFeatures();
			log.info("Analyzing buildings types. This may take some time...");
			for (SimpleFeature singleBuildingFeature : buildingsFeatures) {
				analyzeBuildingType(singleBuildingFeature);
			}
			log.info("Finished anlyzing buildings types.");
		}
		Id<Link> newLink = null;
		for (int a = 0; newLink == null && a < buildingsPerZone.get(zone).get(selectedCategory).size() * 2; a++) {

			SimpleFeature possibleBuilding = buildingsPerZone.get(zone).get(selectedCategory)
					.get(rnd.nextInt(buildingsPerZone.get(zone).get(selectedCategory).size()));
			Coord centroidPointOfBuildingPolygon = MGC
					.point2Coord(((Geometry) possibleBuilding.getDefaultGeometry()).getCentroid());
			double minDistance = Double.MAX_VALUE;
			int numberOfPossibleLinks = regionLinksMap.get(zone).size();
//TODO eventuell auch opposite Links als noPossible deklarieren
			searchLink: for (Link possibleLink : regionLinksMap.get(zone)) {
				if (noPossibleLinks != null && numberOfPossibleLinks > noPossibleLinks.size())
					for (String depotLink : noPossibleLinks) {
						if (depotLink.equals(possibleLink.getId().toString()))
							continue searchLink;
					}
				double distance = NetworkUtils.getEuclideanDistance(centroidPointOfBuildingPolygon,
						(Coord) possibleLink.getAttributes().getAttribute("newCoord"));
				if (distance < minDistance) {
					newLink = possibleLink.getId();
					minDistance = distance;
				}
			}
		}
		if (newLink == null)
			throw new RuntimeException(
					"No possible link for buildings with type '" + selectedCategory + "' in zone '" + zone + "' found");
		return newLink;
	}

	/**
	 * @param categoriesOfBuilding
	 * @param singleBuildingFeature
	 */
	private void analyzeBuildingType(SimpleFeature singleBuildingFeature) {

		List<String> categoriesOfBuilding = new ArrayList<String>();
		String[] buildingTypes;
		Coord centroidPointOfBuildingPolygon = MGC
				.point2Coord(((Geometry) singleBuildingFeature.getDefaultGeometry()).getCentroid());
		String singleZone = indexZones.query(centroidPointOfBuildingPolygon);
		String buildingType = String.valueOf(singleBuildingFeature.getAttribute("type"));
		if (buildingType.equals("") || buildingType.equals("null")) {
			buildingType = indexLanduse.query(centroidPointOfBuildingPolygon);
			buildingTypes = new String[] { buildingType };
		} else {
			buildingType.replace(" ", "");
			buildingTypes = buildingType.split(";");
		}
		singleBuildingFeature.setAttribute("type", String.join(";", buildingTypes));
		for (String singleBuildingType : buildingTypes) {
			for (String category : landuseCategoriesAndDataConnection.keySet()) {
				if (landuseCategoriesAndDataConnection.get(category).contains(singleBuildingType)
						&& !categoriesOfBuilding.contains(category)) {
					categoriesOfBuilding.add(category);
				}
			}
		}
		if (singleZone != null) {
			categoriesOfBuilding.forEach(c -> buildingsPerZone
					.computeIfAbsent(singleZone, k -> new HashMap<String, ArrayList<SimpleFeature>>())
					.computeIfAbsent(c, k -> new ArrayList<SimpleFeature>()).add(singleBuildingFeature));
		}
	}

	private void getIndexZones() {
		if (indexZones == null) {
			ShpOptions shpZones = new ShpOptions(shapeFileZonePath, "EPSG:4326", StandardCharsets.UTF_8);
			indexZones = shpZones.createIndex("EPSG:4326", "gml_id");
		}
	}

	private void getIndexLanduse() {
		if (indexLanduse == null) {
			ShpOptions shpLanduse = new ShpOptions(shapeFileLandusePath, "EPSG:4326", StandardCharsets.UTF_8);
			indexLanduse = shpLanduse.createIndex("EPSG:4326", "fclass");
		}
	}

	/**
	 * Reads in the vehicle types.
	 * 
	 * @param config
	 * @param vehicleTypesFileLocation
	 */
	private void prepareVehicles(Config config) {

		FreightConfigGroup freightConfigGroup = ConfigUtils.addOrGetModule(config, FreightConfigGroup.class);
		String vehicleTypesFileLocation = inputDataDirectory.resolve("vehicleTypes.xml").toString();
		if (vehicleTypesFileLocation == "")
			throw new RuntimeException("No path to the vehicleTypes selected");
		else {
			freightConfigGroup.setCarriersVehicleTypesFile(vehicleTypesFileLocation);
			log.info("Get vehicleTypes from: " + vehicleTypesFileLocation);
		}
		FreightUtils.addOrGetCarriers(ScenarioUtils.loadScenario(config));
		CarrierVehicleTypes carrierVehicleTypes = new CarrierVehicleTypes();
		new CarrierVehicleTypeReader(carrierVehicleTypes).readFile(freightConfigGroup.getCarriersVehicleTypesFile());
	}

	/**
	 * Creates the number of trips between the zones for each mode and purpose.
	 * 
	 * @param trafficVolumePerTypeAndZone_start
	 * @param trafficVolumePerTypeAndZone_stop
	 * @param shapeFileZonesPath
	 * @return
	 * @throws MalformedURLException
	 * @throws UncheckedIOException
	 */
	private HashMap<TripDistributionMatrixKey, Integer> createTripDistribution(
			HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> trafficVolumePerTypeAndZone_start,
			HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> trafficVolumePerTypeAndZone_stop)
			throws UncheckedIOException, MalformedURLException {

		HashMap<TripDistributionMatrixKey, Integer> odMatrix = new HashMap<TripDistributionMatrixKey, Integer>();

		ShpOptions shpZones = new ShpOptions(shapeFileZonePath, null, StandardCharsets.UTF_8);
		List<SimpleFeature> zonesFeatures = shpZones.readFeatures();
		HashMap<ResistanceFunktionKey, Double> resistanceFunktionValues = createResistanceFunktionValues(zonesFeatures);
		int count = 0;
		for (String startZone : trafficVolumePerTypeAndZone_start.keySet()) {
			count++;
			if (count % 50 == 0 || count == 1)
				log.info("Create OD pairs for start zone :" + startZone + ". Zone " + count + " of "
						+ trafficVolumePerTypeAndZone_start.size());
			Object2DoubleMap<String> roundingError = new Object2DoubleOpenHashMap<>();
			for (String mode : trafficVolumePerTypeAndZone_start.get(startZone).keySet()) {
				for (Integer purpose : trafficVolumePerTypeAndZone_start.get(startZone).get(mode).keySet()) {
					double volumeStart = trafficVolumePerTypeAndZone_start.get(startZone).get(mode).getDouble(purpose);
					double gravityConstantA = getGravityConstant(startZone, trafficVolumePerTypeAndZone_stop,
							resistanceFunktionValues, mode, purpose);
					int createdVolume = 0;

					for (String stopZone : trafficVolumePerTypeAndZone_start.keySet()) {
						double volumeStop = trafficVolumePerTypeAndZone_stop.get(stopZone).get(mode).getDouble(purpose);
						double resitanceValue = resistanceFunktionValues
								.get(makeResistanceFunktionKey(startZone, stopZone));
//						double gravityConstantB= getGravityConstant(stopZone, trafficVolumePerTypeAndZone_start,
//								resistanceFunktionValues, mode, purpose);
						/*
						 * gravity model Anpassungen: Faktor anpassen, z.B. reale Reisezeiten im Netz,
						 * auch besonders f??r ??V Bisher: Gravity model mit fixem Quellverkehr
						 */
						double volume = gravityConstantA * volumeStart * volumeStop * resitanceValue;
						double sampledVolume = sample * volume;
						int roundedSampledVolume = (int) Math.floor(sampledVolume);

						double certainRoundingError = sampledVolume - roundedSampledVolume;
						roundingError.merge((mode + "_" + purpose), certainRoundingError, Double::sum);
						if (roundingError.getDouble((mode + "_" + purpose)) >= 1) {
							roundedSampledVolume++;
							roundingError.merge((mode + "_" + purpose), -1, Double::sum);
						} // TODO eventuell methodik f??r den letzten error rest am Ende
						createdVolume = createdVolume + roundedSampledVolume;
						odMatrix.put(makeKey(startZone, stopZone, mode, purpose), roundedSampledVolume);
					}
				}
			}
		}
		writeODMatrices(odMatrix);
		return odMatrix;
	}

	/**
	 * Calculates the gravity constant.
	 * 
	 * @param baseZone
	 * @param trafficVolumePerTypeAndZone
	 * @param resistanceFunktionValues
	 * @param mode
	 * @param purpose
	 * @return
	 */
	private double getGravityConstant(String baseZone,
			HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> trafficVolumePerTypeAndZone,
			HashMap<ResistanceFunktionKey, Double> resistanceFunktionValues, String mode, Integer purpose) {

		double sum = 0;

		for (String zone : trafficVolumePerTypeAndZone.keySet()) {
			double volume = trafficVolumePerTypeAndZone.get(zone).get(mode).getDouble(purpose);
			double resistanceFunktionValue = resistanceFunktionValues.get(makeResistanceFunktionKey(baseZone, zone));
			sum = sum + (volume * resistanceFunktionValue);
		}
		double getGravityCostant = 1 / sum;
		return getGravityCostant;
	}

	/**
	 * Creates a map of the values of the resistance function between two zones.
	 * 
	 * @param zonesFeatures
	 * @return
	 */
	private HashMap<ResistanceFunktionKey, Double> createResistanceFunktionValues(List<SimpleFeature> zonesFeatures) {
		HashMap<ResistanceFunktionKey, Double> resistanceFunktionValues = new HashMap<ResistanceFunktionKey, Double>();
		for (SimpleFeature startZoneFeature : zonesFeatures) {
			for (SimpleFeature stopZoneFeature : zonesFeatures) {
				String startZone = String.valueOf(startZoneFeature.getAttribute("gml_id"));
				String stopZone = String.valueOf(stopZoneFeature.getAttribute("gml_id"));
				double distance = Double.MAX_VALUE;

				if (startZone.equals(stopZone))
					distance = 0;
				else {
					Point geometryStartZone = ((Geometry) startZoneFeature.getDefaultGeometry()).getCentroid();
					Point geometryStopZone = ((Geometry) stopZoneFeature.getDefaultGeometry()).getCentroid();

					distance = geometryStartZone.distance(geometryStopZone);
				}
				double resistanceFunktionResult = Math.exp(-distance);
				resistanceFunktionValues.put(makeResistanceFunktionKey(startZone, stopZone), resistanceFunktionResult);
			}
		}

		return resistanceFunktionValues;
	}

	/**
	 * Writes every matrix for each mode and purpose.
	 * 
	 * @param odMatrix
	 * @param usedModes
	 * @param usedPurposes
	 * @param usedZones
	 * @throws UncheckedIOException
	 * @throws MalformedURLException
	 */
	private void writeODMatrices(HashMap<TripDistributionMatrixKey, Integer> odMatrix)
			throws UncheckedIOException, MalformedURLException {

		ArrayList<String> usedModes = getListOfModes(odMatrix);
		ArrayList<String> usedZones = getListOfZones(odMatrix);
		ArrayList<Integer> usedPurposes = getListOfPurposes(odMatrix);

		for (String mode : usedModes) {
			for (int purpose : usedPurposes) {

				Path outputFolder = output.resolve("caculatedData")
						.resolve("odMatrix_" + mode + "_" + purpose + ".csv");

				BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder.toUri().toURL(), StandardCharsets.UTF_8,
						true);
				try {

					List<String> headerRow = new ArrayList<>();
					headerRow.add("");
					for (int i = 0; i < usedZones.size(); i++) {
						headerRow.add(usedZones.get(i));
					}
					JOIN.appendTo(writer, headerRow);
					writer.write("\n");

					for (String startZone : usedZones) {
						List<String> row = new ArrayList<>();
						row.add(startZone);
						for (String stopZone : usedZones) {
							if (odMatrix.get(makeKey(startZone, stopZone, mode, purpose)) == null)
								throw new RuntimeException("OD pair is missing; start: " + startZone + "; stop: "
										+ stopZone + "; mode: " + mode + "; purpuse: " + purpose);
							row.add(String.valueOf(odMatrix.get(makeKey(startZone, stopZone, mode, purpose))));
						}
						JOIN.appendTo(writer, row);
						writer.write("\n");
					}
					writer.close();

				} catch (IOException e) {
					e.printStackTrace();
				}
				log.info("Write OD matrix for mode " + mode + " and for purpose " + purpose + " to "
						+ outputFolder.toString());
			}
		}
	}

	/**
	 * Creates a distribution of the given input data for each zone based on the
	 * used OSM data.
	 * 
	 * @param shapeFileZonePath
	 * @param shapeFileLandusePath
	 * @param shapeFileBuildingsPath
	 * @return
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	private HashMap<String, Object2DoubleMap<String>> createInputDataDistribution()
			throws IOException, MalformedURLException {

		HashMap<String, Object2DoubleMap<String>> resultingDataPerZone = new HashMap<String, Object2DoubleMap<String>>();
		Path outputFileInOutputFolder = output.resolve("caculatedData").resolve("dataDistributionPerZone.csv");

		landuseCategoriesAndDataConnection.put("Inhabitants", new ArrayList<String>(Arrays.asList("residential",
				"apartments", "dormitory", "dwelling_house", "house", "retirement_home", "semidetached_house")));
		landuseCategoriesAndDataConnection.put("Employee Primary Sector",
				new ArrayList<String>(Arrays.asList("farmyard", "farmland", "farm", "farm_auxiliary", "greenhouse")));
		landuseCategoriesAndDataConnection.put("Employee Construction",
				new ArrayList<String>(Arrays.asList("construction")));
		landuseCategoriesAndDataConnection.put("Employee Secondary Sector Rest",
				new ArrayList<String>(Arrays.asList("industrial", "factory", "manufacture")));
		landuseCategoriesAndDataConnection.put("Employee Retail",
				new ArrayList<String>(Arrays.asList("retail", "kiosk", "mall", "shop", "supermarket")));
		landuseCategoriesAndDataConnection.put("Employee Traffic/Parcels", new ArrayList<String>(
				Arrays.asList("commercial", "post_office", "storage", "storage_tank", "warehouse")));
		landuseCategoriesAndDataConnection.put("Employee Tertiary Sector Rest", new ArrayList<String>(
				Arrays.asList("commercial", "embassy", "foundation", "government", "office", "townhall")));
		landuseCategoriesAndDataConnection.put("Employee", new ArrayList<String>());
		landuseCategoriesAndDataConnection.get("Employee")
				.addAll(landuseCategoriesAndDataConnection.get("Employee Primary Sector"));
		landuseCategoriesAndDataConnection.get("Employee")
				.addAll(landuseCategoriesAndDataConnection.get("Employee Construction"));
		landuseCategoriesAndDataConnection.get("Employee")
				.addAll(landuseCategoriesAndDataConnection.get("Employee Secondary Sector Rest"));
		landuseCategoriesAndDataConnection.get("Employee")
				.addAll(landuseCategoriesAndDataConnection.get("Employee Retail"));
		landuseCategoriesAndDataConnection.get("Employee")
				.addAll(landuseCategoriesAndDataConnection.get("Employee Traffic/Parcels"));
		landuseCategoriesAndDataConnection.get("Employee")
				.addAll(landuseCategoriesAndDataConnection.get("Employee Tertiary Sector Rest"));

		switch (usedLanduseConfiguration) {
		case useExistingDataDistribution:
			Path existingDataDistribution = inputDataDirectory.resolve("dataDistributionPerZone.csv");

			if (!Files.exists(existingDataDistribution)) {
				log.error("Required data per zone file {} not found", existingDataDistribution);
			}

			try (BufferedReader reader = IOUtils.getBufferedReader(existingDataDistribution.toString())) {
				CSVParser parse = CSVFormat.DEFAULT.withDelimiter('\t').withFirstRecordAsHeader().parse(reader);

				for (CSVRecord record : parse) {
					String zoneID = record.get("areaID");
					resultingDataPerZone.put(zoneID, new Object2DoubleOpenHashMap<>());
					for (int n = 1; n < parse.getHeaderMap().size(); n++) {
						resultingDataPerZone.get(zoneID).mergeDouble(parse.getHeaderNames().get(n),
								Double.valueOf(record.get(n)), Double::sum);
					}
				}
			}
			log.info("Data distribution for " + resultingDataPerZone.size() + " zones was imported from ",
					existingDataDistribution);
			Files.copy(existingDataDistribution, outputFileInOutputFolder, StandardCopyOption.COPY_ATTRIBUTES);
			break;

		default:

			log.info("New analyze for data distribution is started. The used method is: " + usedLanduseConfiguration);
			HashMap<String, Object2DoubleMap<String>> landuseCategoriesPerZone = new HashMap<String, Object2DoubleMap<String>>();
			createLanduseDistribution(landuseCategoriesPerZone);

			HashMap<String, HashMap<String, Integer>> investigationAreaData = new HashMap<String, HashMap<String, Integer>>();
			readAreaData(investigationAreaData);

			createResultingDataForLanduseInZones(landuseCategoriesPerZone, investigationAreaData, resultingDataPerZone);

			writeResultOfDataDistribution(resultingDataPerZone, outputFileInOutputFolder);
		}

		return resultingDataPerZone;
	}

	private void readInputParamters() throws IOException {

		// Read generation rates for start potentials
		Path generationRatesStartPath = inputDataDirectory.resolve("parameters").resolve("generationRates_start.csv");
		generationRatesStart = readGenerationRates(generationRatesStartPath);
		log.info("Read generations rates (start)");

		// Read generation rates for stop potentials
		Path generationRatesStopPath = inputDataDirectory.resolve("parameters").resolve("generationRates_stop.csv");
		generationRatesStop = readGenerationRates(generationRatesStopPath);
		log.info("Read generations rates (stop)");

		// read commitment rates for start potentials
		Path commitmentRatesStartPath = inputDataDirectory.resolve("parameters").resolve("commitmentRates_start.csv");
		commitmentRatesStart = readCommitmentRates(commitmentRatesStartPath);
		log.info("Read commitment rates (start)");

		// read commitment rates for stop potentials
		Path commitmentRatesStopPath = inputDataDirectory.resolve("parameters").resolve("commitmentRates_stop.csv");
		commitmentRatesStop = readCommitmentRates(commitmentRatesStopPath);
		log.info("Read commitment rates (stop)");
	}

	/**
	 * Creates the traffic volume (start) for each zone separated in the 3 modes and
	 * the 5 purposes.
	 * 
	 * @param resultingDataPerZone
	 * @return
	 * @throws MalformedURLException
	 */
	private HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> createTrafficVolume_start(
			HashMap<String, Object2DoubleMap<String>> resultingDataPerZone) throws MalformedURLException {

		HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> trafficVolumePerTypeAndZone_start = new HashMap<String, HashMap<String, Object2DoubleMap<Integer>>>();
		calculateTrafficVolumePerZone(trafficVolumePerTypeAndZone_start, resultingDataPerZone, "start");
		Path outputFileStart = output.resolve("caculatedData")
				.resolve("TrafficVolume_startPerZone_" + (int) (sample * 100) + "pt.csv");
		writeCSVWithTrafficVolumeperZoneAndModes(trafficVolumePerTypeAndZone_start, outputFileStart);
		log.info("Write traffic volume for start trips per zone in CSV: " + outputFileStart);
		return trafficVolumePerTypeAndZone_start;
	}

	/**
	 * Creates the traffic volume (stop) for each zone separated in the 3 modes and
	 * the 5 purposes.
	 * 
	 * @param resultingDataPerZone
	 * @return
	 * @throws MalformedURLException
	 */
	private HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> createTrafficVolume_stop(
			HashMap<String, Object2DoubleMap<String>> resultingDataPerZone) throws MalformedURLException {

		HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> trafficVolumePerTypeAndZone_stop = new HashMap<String, HashMap<String, Object2DoubleMap<Integer>>>();
		calculateTrafficVolumePerZone(trafficVolumePerTypeAndZone_stop, resultingDataPerZone, "stop");
		Path outputFileStop = output.resolve("caculatedData")
				.resolve("TrafficVolume_stopPerZone_" + (int) (sample * 100) + "pt.csv");
		writeCSVWithTrafficVolumeperZoneAndModes(trafficVolumePerTypeAndZone_stop, outputFileStop);
		log.info("Write traffic volume for stop trips per zone in CSV: " + outputFileStop);
		return trafficVolumePerTypeAndZone_stop;
	}

	/**
	 * Method create the percentage for each land use category in each zone based on
	 * the sum of this category in all zones of the zone shape file
	 * 
	 * @param shapeFileLandusePath     Path to shape file with the land use
	 *                                 information
	 * @param shapeFileZonesPath       Path to shape file with the zone information
	 * @param landuseCategoriesPerZone
	 * @param shapeFileLandusePath
	 * @param shapeFileBuildingsPath
	 * 
	 */
	private void createLanduseDistribution(HashMap<String, Object2DoubleMap<String>> landuseCategoriesPerZone) {

		List<String> neededLanduseCategories = List.of("residential", "industrial", "commercial", "retail", "farmyard",
				"farmland", "construction");

		ShpOptions shpLanduse = new ShpOptions(shapeFileLandusePath, null, StandardCharsets.UTF_8);
		ShpOptions shpZones = new ShpOptions(shapeFileZonePath, null, StandardCharsets.UTF_8);

		List<SimpleFeature> landuseFeatures = shpLanduse.readFeatures();
		List<SimpleFeature> zonesFeatures = shpZones.readFeatures();

//		ShpOptions.Index indexLanduse = shpLanduse.createIndex("EPSG:4326", "fclass");
//		ShpOptions.Index indexZones = shpZones.createIndex("EPSG:4326", "gml_id");
		getIndexZones();
		getIndexLanduse();
		for (SimpleFeature districId : zonesFeatures) {
			Object2DoubleMap<String> landusePerCategory = new Object2DoubleOpenHashMap<>();
			landuseCategoriesPerZone.put((String) districId.getAttribute("gml_id"), landusePerCategory);
		}

		int countOSMObjects = 0;

		switch (usedLanduseConfiguration) {
		case useOSMBuildingsAndLanduse:

			ShpOptions shpBuildings = new ShpOptions(shapeFileBuildingsPath, null, StandardCharsets.UTF_8);
			List<SimpleFeature> buildingsFeatures = shpBuildings.readFeatures();
			for (SimpleFeature singleBuildingFeature : buildingsFeatures) {
				countOSMObjects++;
				if (countOSMObjects % 10000 == 0)
					log.info("Investigate Building " + countOSMObjects + " of " + buildingsFeatures.size()
							+ " buildings: " + Math.round((double) countOSMObjects / buildingsFeatures.size() * 100)
							+ " %");
				analyzeBuildingType(singleBuildingFeature);
			}
			for (String zone : buildingsPerZone.keySet()) {
				for (String category : buildingsPerZone.get(zone).keySet()) {
					for (SimpleFeature building : buildingsPerZone.get(zone).get(category)) {
						String[] buildingTypes = ((String) building.getAttribute("type")).split(";");
						for (String singleCategoryOfBuilding : buildingTypes) {
							double buildingLevels;
							if (building.getAttribute("levels") == null)
								buildingLevels = 1;
							else
								buildingLevels = (long) building.getAttribute("levels") / buildingTypes.length;
							double area = (double) ((long) building.getAttribute("area")) * buildingLevels;

							landuseCategoriesPerZone.get(zone).mergeDouble(singleCategoryOfBuilding, area, Double::sum);
						}

					}
				}
			}
			break;
		case useOnlyOSMLanduse:
			for (SimpleFeature singleLanduseFeature : landuseFeatures) {
				if (!neededLanduseCategories.contains((String) singleLanduseFeature.getAttribute("fclass")))
					continue;
				Point centroidPointOfLandusePolygon = ((Geometry) singleLanduseFeature.getDefaultGeometry())
						.getCentroid();

				for (SimpleFeature singleZone : zonesFeatures) {
					if (((Geometry) singleZone.getDefaultGeometry()).contains(centroidPointOfLandusePolygon)) {
						landuseCategoriesPerZone.get(singleZone.getAttribute("gml_id")).mergeDouble(
								(String) singleLanduseFeature.getAttribute("fclass"),
								(double) singleLanduseFeature.getAttribute("area"), Double::sum);
						continue;
					}
				}
			}
			break;

		default:
			throw new RuntimeException("No possible option for the use of OSM data selected");
		}
	}

	/**
	 * Reads the input data for certain areas from the csv file.
	 * 
	 * @param areaDataPath
	 * @param areaData
	 * @throws IOException
	 */
	private void readAreaData(HashMap<String, HashMap<String, Integer>> areaData) throws IOException {

		Path areaDataPath = inputDataDirectory.resolve("investigationAreaData.csv");
		if (!Files.exists(areaDataPath)) {
			log.error("Required input data file {} not found", areaDataPath);
		}
		try (CSVParser parser = new CSVParser(Files.newBufferedReader(areaDataPath),
				CSVFormat.TDF.withFirstRecordAsHeader())) {

			for (CSVRecord record : parser) {
				HashMap<String, Integer> lookUpTable = new HashMap<>();
				for (String csvRecord : parser.getHeaderMap().keySet()) {
					if (parser.getHeaderMap().get(csvRecord) > 0)
						lookUpTable.put(csvRecord, Integer.valueOf(record.get(csvRecord)));
				}
				areaData.put(record.get("Area"), lookUpTable);
			}
		}
	}

	/**
	 * @param landuseCategoriesPerZone
	 * @param investigationAreaData
	 * @param resultingDataPerZone
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	private void createResultingDataForLanduseInZones(
			HashMap<String, Object2DoubleMap<String>> landuseCategoriesPerZone,
			HashMap<String, HashMap<String, Integer>> investigationAreaData,
			HashMap<String, Object2DoubleMap<String>> resultingDataPerZone) throws MalformedURLException, IOException {

		Object2DoubleMap<String> totalSquareMetersPerCategory = new Object2DoubleOpenHashMap<>();
		Object2DoubleMap<String> totalEmployeesInCategoriesPerZone = new Object2DoubleOpenHashMap<>();
		Object2DoubleMap<String> totalEmployeesPerCategories = new Object2DoubleOpenHashMap<>();

		// connects the collected landuse data with the needed categories
		for (String zoneID : landuseCategoriesPerZone.keySet()) {
			resultingDataPerZone.put(zoneID, new Object2DoubleOpenHashMap<>());
			for (String categoryLanduse : landuseCategoriesPerZone.get(zoneID).keySet()) {
				for (String categoryData : landuseCategoriesAndDataConnection.keySet()) {
					if (landuseCategoriesAndDataConnection.get(categoryData).contains(categoryLanduse)) {
						double additionalArea = landuseCategoriesPerZone.get(zoneID).getDouble(categoryLanduse);
						resultingDataPerZone.get(zoneID).mergeDouble(categoryData, additionalArea, Double::sum);
						totalSquareMetersPerCategory.mergeDouble(categoryData, additionalArea, Double::sum);
					}
				}
			}
		}

		/*
		 * creates the percentages of each category and zones based on the sum in this
		 * category
		 */
		for (String zoneId : resultingDataPerZone.keySet()) {
			for (String categoryData : resultingDataPerZone.get(zoneId).keySet()) {
				double newValue = resultingDataPerZone.get(zoneId).getDouble(categoryData)
						/ totalSquareMetersPerCategory.getDouble(categoryData);
				resultingDataPerZone.get(zoneId).replace(categoryData,
						resultingDataPerZone.get(zoneId).getDouble(categoryData), newValue);
			}
		}
		// can be deleted or used as test
		Object2DoubleMap<String> checkPercentages = new Object2DoubleOpenHashMap<>();
		for (String landuseCategoriesForSingleZone : resultingDataPerZone.keySet()) {
			for (String category : resultingDataPerZone.get(landuseCategoriesForSingleZone).keySet()) {
				checkPercentages.mergeDouble(category,
						resultingDataPerZone.get(landuseCategoriesForSingleZone).getDouble(category), Double::sum);
			}
		}
		for (String category : checkPercentages.keySet()) {
			if (Math.abs(1 - checkPercentages.getDouble(category)) > 0.01)
				throw new RuntimeException("Sum of percenatges is not 1. For " + category + " the sum is "
						+ checkPercentages.getDouble(category) + "%");
		}
		// calculates the data per zone and category data
		for (String zoneId : resultingDataPerZone.keySet()) {
			for (String categoryData : resultingDataPerZone.get(zoneId).keySet()) {
				double percentageValue = resultingDataPerZone.get(zoneId).getDouble(categoryData);
				int inputDataForCategory = investigationAreaData.get("Berlin").get(categoryData);
				double resultingNumberPerCategory = percentageValue * inputDataForCategory;
				resultingDataPerZone.get(zoneId).replace(categoryData, percentageValue, resultingNumberPerCategory);
				totalEmployeesPerCategories.mergeDouble(categoryData, resultingNumberPerCategory, Double::sum);
				if (!categoryData.equals("Employee") && !categoryData.equals("Inhabitants"))
					totalEmployeesInCategoriesPerZone.mergeDouble(zoneId, resultingNumberPerCategory, Double::sum);

			}
		}
		// corrects the number of employees in the categories so that the sum is correct
		for (int i = 0; i < 30; i++) { // TODO perhaps find number of iterations
			for (String zoneId : resultingDataPerZone.keySet()) {
				for (String categoryData : resultingDataPerZone.get(zoneId).keySet()) {
					if (!categoryData.equals("Employee") && !categoryData.equals("Inhabitants")) {
						double correctionFactor = resultingDataPerZone.get(zoneId).getDouble("Employee")
								/ totalEmployeesInCategoriesPerZone.getDouble(zoneId);
						double resultingNumberPerCategory = correctionFactor
								* resultingDataPerZone.get(zoneId).getDouble(categoryData);
						resultingDataPerZone.get(zoneId).replace(categoryData,
								resultingDataPerZone.get(zoneId).getDouble(categoryData), resultingNumberPerCategory);
					}
				}
			}

			for (String categoryData : investigationAreaData.get("Berlin").keySet()) {
				for (String zoneId : resultingDataPerZone.keySet()) {
					if (!categoryData.equals("Employee") && !categoryData.equals("Inhabitants")) {
						double correctionFactor = investigationAreaData.get("Berlin").get(categoryData)
								/ totalEmployeesPerCategories.getDouble(categoryData);
						double resultingNumberPerCategory = correctionFactor
								* resultingDataPerZone.get(zoneId).getDouble(categoryData);
						resultingDataPerZone.get(zoneId).replace(categoryData,
								resultingDataPerZone.get(zoneId).getDouble(categoryData), resultingNumberPerCategory);
					}
				}
			}
			// update totals per sum becaus eof the changes before
			totalEmployeesInCategoriesPerZone.clear();
			totalEmployeesPerCategories.clear();
			for (String zoneId : resultingDataPerZone.keySet()) {
				for (String categoryData : resultingDataPerZone.get(zoneId).keySet()) {
					totalEmployeesPerCategories.mergeDouble(categoryData,
							resultingDataPerZone.get(zoneId).getDouble(categoryData), Double::sum);
					if (!categoryData.equals("Employee") && !categoryData.equals("Inhabitants"))
						totalEmployeesInCategoriesPerZone.mergeDouble(zoneId,
								resultingDataPerZone.get(zoneId).getDouble(categoryData), Double::sum);
				}
			}

		}
	}

	/**
	 * Writes a csv file with result of the distribution per zone of the input data.
	 * 
	 * @param resultingDataPerZone
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	private void writeResultOfDataDistribution(HashMap<String, Object2DoubleMap<String>> resultingDataPerZone,
			Path outputFileInOutputFolder) throws IOException, MalformedURLException {

		Path outputFileInInputFolder = inputDataDirectory.resolve("dataDistributionPerZone.csv");

		if (Files.exists(outputFileInInputFolder)) {
			Path oldFile = Path.of(outputFileInInputFolder.toString().replace(".csv", "_old.csv"));
			log.warn("The result of data distribution already exists. The existing data will be moved to: " + oldFile);
			Files.deleteIfExists(oldFile);
			Files.move(outputFileInInputFolder, oldFile, StandardCopyOption.REPLACE_EXISTING);
		}

		writeCSVWithCategoryHeader(resultingDataPerZone, outputFileInInputFolder);
		log.info("The data distribution is finished and written to: " + outputFileInInputFolder);
		Files.copy(outputFileInInputFolder, outputFileInOutputFolder, StandardCopyOption.COPY_ATTRIBUTES);
	}

	/**
	 * @param resultingDataPerZone
	 * @param outputFileInInputFolder
	 * @throws MalformedURLException
	 */
	private void writeCSVWithCategoryHeader(HashMap<String, Object2DoubleMap<String>> resultingDataPerZone,
			Path outputFileInInputFolder) throws MalformedURLException {
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFileInInputFolder.toUri().toURL(),
				StandardCharsets.UTF_8, true);
		try {
			String[] header = new String[] { "areaID", "Inhabitants", "Employee", "Employee Primary Sector",
					"Employee Construction", "Employee Secondary Sector Rest", "Employee Retail",
					"Employee Traffic/Parcels", "Employee Tertiary Sector Rest" };
			JOIN.appendTo(writer, header);
			writer.write("\n");
			for (String zone : resultingDataPerZone.keySet()) {
				List<String> row = new ArrayList<>();
				row.add(zone);
				for (String category : header) {
					if (!category.equals("areaID")) {
						row.add(String.valueOf((int) Math.round(resultingDataPerZone.get(zone).getDouble(category))));
//						row.add(String.valueOf(resultingDataPerZone.get(zone).getDouble(category)));
					}
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
	 * @param trafficVolumePerTypeAndZone
	 * @param outputFileInInputFolder
	 * @throws MalformedURLException
	 */
	private void writeCSVWithTrafficVolumeperZoneAndModes(
			HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> trafficVolumePerTypeAndZone,
			Path outputFileInInputFolder) throws MalformedURLException {
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFileInInputFolder.toUri().toURL(),
				StandardCharsets.UTF_8, true);
		try {
			String[] header = new String[] { "areaID", "mode", "1", "2", "3", "4", "5" };
			JOIN.appendTo(writer, header);
			writer.write("\n");
			for (String zoneID : trafficVolumePerTypeAndZone.keySet()) {
				for (String mode : trafficVolumePerTypeAndZone.get(zoneID).keySet()) {
					List<String> row = new ArrayList<>();
					row.add(zoneID);
					row.add(mode);
					Integer count = 1;
					while (count < 6) {
						row.add(String.valueOf(Math
								.round(trafficVolumePerTypeAndZone.get(zoneID).get(mode).getDouble(count) * sample)));
						count++;
					}
					JOIN.appendTo(writer, row);
					writer.write("\n");
				}
			}
			writer.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Reads the data for the generation rates.
	 * 
	 * @param generationRatesPath
	 * @return
	 * @throws IOException
	 */
	private HashMap<Integer, HashMap<String, Double>> readGenerationRates(Path generationRatesPath) throws IOException {
		HashMap<Integer, HashMap<String, Double>> generationRates = new HashMap<Integer, HashMap<String, Double>>();
		if (!Files.exists(generationRatesPath)) {
			log.error("Required input data file {} not found", generationRatesPath);
		}
		try (CSVParser parser = new CSVParser(Files.newBufferedReader(generationRatesPath),
				CSVFormat.TDF.withFirstRecordAsHeader())) {

			for (CSVRecord record : parser) {
				HashMap<String, Double> lookUpTable = new HashMap<>();
				for (String category : parser.getHeaderMap().keySet()) {
					if (!category.equals("purpose"))
						lookUpTable.put(category, Double.valueOf(record.get(category)));
				}
				generationRates.put(Integer.valueOf(record.get(0)), lookUpTable);
			}
		}
		return generationRates;
	}

	/**
	 * Reads the data for the commitment rates. For modes: pt = public transport; it
	 * = individual traffic; op = elective (wahlfrei)
	 * 
	 * @param generationRatesPath
	 * @return
	 * @throws IOException
	 */
	private HashMap<String, HashMap<String, Double>> readCommitmentRates(Path commitmentRatesPath) throws IOException {
		HashMap<String, HashMap<String, Double>> commitmentRates = new HashMap<String, HashMap<String, Double>>();
		if (!Files.exists(commitmentRatesPath)) {
			log.error("Required input data file {} not found", commitmentRatesPath);
		}
		try (CSVParser parser = new CSVParser(Files.newBufferedReader(commitmentRatesPath),
				CSVFormat.TDF.withFirstRecordAsHeader())) {

			for (CSVRecord record : parser) {
				HashMap<String, Double> lookUpTable = new HashMap<>();
				for (String category : parser.getHeaderMap().keySet()) {
					if (!category.equals("purpose") && !category.equals("mode"))
						lookUpTable.put(category, Double.valueOf(record.get(category)));
				}
				commitmentRates.put((record.get(0) + "_" + record.get(1)), lookUpTable);
			}

		}
		return commitmentRates;
	}

	/**
	 * Calculates the traffic volume for each zone and purpose.
	 * 
	 * @param trafficVolumePerZone
	 * @param resultingDataPerZone
	 * @param volumeType
	 * @return
	 */
	private HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> calculateTrafficVolumePerZone(
			HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> trafficVolumePerZone,
			HashMap<String, Object2DoubleMap<String>> resultingDataPerZone, String volumeType) {

		HashMap<Integer, HashMap<String, Double>> generationRates = new HashMap<Integer, HashMap<String, Double>>();
		HashMap<String, HashMap<String, Double>> commitmentRates = new HashMap<String, HashMap<String, Double>>();

		ArrayList<String> modes;
		switch (usedModeDifferentiation) {
		case createOneODMatrix:
			modes = new ArrayList<String>(Arrays.asList("total"));
			break;
		case createSeperateODMatricesForModes:
			modes = new ArrayList<String>(Arrays.asList("pt", "it", "op"));
			break;
		default:
			throw new RuntimeException("No mode differentiation selected.");
		}

		if (volumeType.equals("start")) {
			generationRates = generationRatesStart;
			commitmentRates = commitmentRatesStart;
		} else if (volumeType.equals("stop")) {
			generationRates = generationRatesStop;
			commitmentRates = commitmentRatesStop;
		} else
			throw new RuntimeException("No generation and commitment rates selected. Please check!");

		for (String zoneId : resultingDataPerZone.keySet()) {
			HashMap<String, Object2DoubleMap<Integer>> valuesForZone = new HashMap<String, Object2DoubleMap<Integer>>();
			for (String mode : modes) {
				Object2DoubleMap<Integer> trafficValuesPerPurpose = new Object2DoubleOpenHashMap<>();
				for (Integer purpose : generationRates.keySet()) {

					if (resultingDataPerZone.get(zoneId).isEmpty())
						trafficValuesPerPurpose.merge(purpose, 0., Double::sum);
					else
						for (String category : resultingDataPerZone.get(zoneId).keySet()) {
							double commitmentFactor;
							if (mode.equals("total"))
								commitmentFactor = 1;
							else
								commitmentFactor = commitmentRates.get(purpose + "_" + mode).get(category);
							double generationFactor = generationRates.get(purpose).get(category);
							double newValue = resultingDataPerZone.get(zoneId).getDouble(category) * generationFactor
									* commitmentFactor;
							trafficValuesPerPurpose.merge(purpose, newValue, Double::sum);
						}
					trafficValuesPerPurpose.replace(purpose, trafficValuesPerPurpose.getDouble(purpose)); // notwendig?
				}
				valuesForZone.put(mode, trafficValuesPerPurpose);
			}
			trafficVolumePerZone.put(zoneId, valuesForZone);
		}
		return trafficVolumePerZone;
	}

	static class TripDistributionMatrixKey {
		private final String fromZone;
		private final String toZone;
		private final String mode;
		private final int purpose;

		public TripDistributionMatrixKey(String fromZone, String toZone, String mode, int purpose) {
			super();
			this.fromZone = fromZone;
			this.toZone = toZone;
			this.mode = mode;
			this.purpose = purpose;
		}

		public String getFromZone() {
			return fromZone;
		}

		public String getToZone() {
			return toZone;
		}

		public String getMode() {
			return mode;
		}

		public int getPurpose() {
			return purpose;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((fromZone == null) ? 0 : fromZone.hashCode());
			long temp;
			temp = Double.doubleToLongBits(purpose);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			result = prime * result + ((toZone == null) ? 0 : toZone.hashCode());
			result = prime * result + ((mode == null) ? 0 : mode.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TripDistributionMatrixKey other = (TripDistributionMatrixKey) obj;
			if (fromZone == null) {
				if (other.fromZone != null)
					return false;
			} else if (!fromZone.equals(other.fromZone))
				return false;
			if (Double.doubleToLongBits(purpose) != Double.doubleToLongBits(other.purpose))
				return false;
			if (toZone == null) {
				if (other.toZone != null)
					return false;
			} else if (!toZone.equals(other.toZone))
				return false;
			if (mode == null) {
				if (other.mode != null)
					return false;
			} else if (!mode.equals(other.mode))
				return false;
			return true;
		}
	}

	/**
	 * Creates a key for the tripDistributionMatrix.
	 * 
	 * @param fromZone
	 * @param toZone
	 * @param mode
	 * @param purpose
	 * @return
	 */
	private TripDistributionMatrixKey makeKey(String fromZone, String toZone, String mode, int purpose) {
		return new TripDistributionMatrixKey(fromZone, toZone, mode, purpose);
	}

	/**
	 * Returns all zones being used as a start and/or stop location
	 * 
	 * @param odMatrix
	 * @return
	 */
	private ArrayList<String> getListOfZones(HashMap<TripDistributionMatrixKey, Integer> odMatrix) {
		if (listOfZones.isEmpty())
			for (TripDistributionMatrixKey key : odMatrix.keySet()) {
				if (!listOfZones.contains(key.getFromZone()))
					listOfZones.add(key.getFromZone());
				if (!listOfZones.contains(key.getToZone()))
					listOfZones.add(key.getToZone());
			}
		return listOfZones;
	}

	/**
	 * Returns all modes being used.
	 * 
	 * @param odMatrix
	 * @return
	 */
	private ArrayList<String> getListOfModes(HashMap<TripDistributionMatrixKey, Integer> odMatrix) {
		if (listOfModes.isEmpty()) {
			for (TripDistributionMatrixKey key : odMatrix.keySet()) {
				if (!listOfModes.contains(key.getMode()))
					listOfModes.add(key.getMode());
			}
		}
		return listOfModes;
	}

	/**
	 * Returns all purposes being used.
	 * 
	 * @param odMatrix
	 * @return
	 */
	private ArrayList<Integer> getListOfPurposes(HashMap<TripDistributionMatrixKey, Integer> odMatrix) {
		if (listOfPurposes.isEmpty()) {
			for (TripDistributionMatrixKey key : odMatrix.keySet()) {
				if (!listOfPurposes.contains(key.getPurpose()))
					listOfPurposes.add(key.getPurpose());
			}
		}
		return listOfPurposes;
	}

	private int getSumOfServicesForStartZone(HashMap<TripDistributionMatrixKey, Integer> odMatrix, String startZone,
			String mode, int purpose) {
		int numberOfTrips = 0;
		ArrayList<String> zones = getListOfZones(odMatrix);
		for (String stopZone : zones)
			numberOfTrips = numberOfTrips + odMatrix.get(makeKey(startZone, stopZone, mode, purpose));
		return numberOfTrips;
	}

	class ResistanceFunktionKey {
		private final String fromZone;
		private final String toZone;

		public ResistanceFunktionKey(String fromZone, String toZone) {
			super();
			this.fromZone = fromZone;
			this.toZone = toZone;

		}

		public String getFromZone() {
			return fromZone;
		}

		public String getToZone() {
			return toZone;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((fromZone == null) ? 0 : fromZone.hashCode());
			result = prime * result + ((toZone == null) ? 0 : toZone.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ResistanceFunktionKey other = (ResistanceFunktionKey) obj;
			if (fromZone == null) {
				if (other.fromZone != null)
					return false;
			} else if (!fromZone.equals(other.fromZone))
				return false;
			if (toZone == null) {
				if (other.toZone != null)
					return false;
			} else if (!toZone.equals(other.toZone))
				return false;
			return true;
		}
	}

	/**
	 * Creates a key for the tripDistributionMatrix.
	 * 
	 * @param fromZone
	 * @param toZone
	 * @param mode
	 * @param purpose
	 * @return
	 */
	private ResistanceFunktionKey makeResistanceFunktionKey(String fromZone, String toZone) {
		return new ResistanceFunktionKey(fromZone, toZone);
	}
}
