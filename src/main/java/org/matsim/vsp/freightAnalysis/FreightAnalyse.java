package org.matsim.vsp.freightAnalysis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.freight.carriers.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.IOException;

public class FreightAnalyse {

	/**
	 * Calculates and writes some analysis for the defined Runs.
	 *
	 * @author rewert
	 */

	private static String RUN_DIR = null;

	private static String OUTPUT_DIR = null;

	private static String runId = "";
	
	private static String networkCRS = null;
	
	private static boolean onlyAllCarrierResults = false;

	private static final Logger log = LogManager.getLogger(FreightAnalyse.class);

	public static void main(String[] args) throws IOException {
		RUN_DIR = args[0] + "/";
		OUTPUT_DIR = RUN_DIR + "Analysis/";
		OutputDirectoryLogging.initLoggingWithOutputDirectory(OUTPUT_DIR);
		if (args.length > 1)
			onlyAllCarrierResults = Boolean.parseBoolean(args[1]);
		if (args.length > 2)
			networkCRS = args[2];
		if (args.length > 3)
			runId = args[3];
		FreightAnalyse analysis = new FreightAnalyse();
		analysis.run();
		log.info("### Finished ###");
		OutputDirectoryLogging.closeOutputDirLogging();

	}

	private void run() {

//		File configFile = new File(RUN_DIR + "output_config.xml");
//		File configFile = new File(RUN_DIR + "output_config.xml.gz");
//		File populationFile = new File(RUN_DIR + "output_plans.xml.gz");
//		File networkFile = new File(RUN_DIR + "output_network.xml.gz");
//		File carrierFile = new File(RUN_DIR + "output_carriers.xml.gz");
//		File vehicleTypeFile = new File(RUN_DIR + "output_carriersVehicleTypes.xml.gz");
//		File vehicleFile = new File(RUN_DIR + "output_allVehicles.xml.gz");
		Config config = ConfigUtils.createConfig();
		config.vehicles().setVehiclesFile(RUN_DIR + runId + "output_allVehicles.xml.gz");
		config.network().setInputFile(RUN_DIR + runId + "output_network.xml.gz");
		config.global().setCoordinateSystem(networkCRS);
		FreightCarriersConfigGroup freightConfigGroup = ConfigUtils.addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightConfigGroup.setCarriersFile(RUN_DIR + runId + "output_carriers.xml.gz");
		freightConfigGroup.setCarriersVehicleTypesFile(RUN_DIR + runId + "output_carriersVehicleTypes.xml.gz");
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		CarriersUtils.loadCarriersAccordingToFreightConfig(scenario);
//		Network network = NetworkUtils.readNetwork(networkFile.getAbsolutePath());

//		CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes();
//		new CarrierVehicleTypeReader(vehicleTypes).readFile(vehicleTypeFile.getAbsolutePath());

//		log.warn("VehicleTypes: " + vehicleTypes.getVehicleTypes().keySet().toString());
		log.warn("VehicleTypes: " + scenario.getVehicles().getVehicleTypes().keySet());

		
//		Carriers carriers = new Carriers();
//		new CarrierPlanXmlReader(carriers, vehicleTypes).readFile(carrierFile.getAbsolutePath());

		EventsManager eventsManager = EventsUtils.createEventsManager();
		TripEventHandler tripHandler = new TripEventHandler(scenario.getNetwork(), scenario.getVehicles());
		eventsManager.addHandler(tripHandler);

		log.info("Reading the event file...");
		eventsManager.initProcessing();
		MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
		reader.readFile(RUN_DIR + runId + "output_events.xml.gz");
		eventsManager.finishProcessing();
		log.info("Reading the event file... Done.");

		TripWriter tripWriter = new TripWriter(tripHandler, OUTPUT_DIR);
		if (!onlyAllCarrierResults) {
			for (Carrier carrier : CarriersUtils.addOrGetCarriers(scenario).getCarriers().values()) {
				// tripWriter.writeDetailedResultsSingleCarrier(carrier.getId().toString());
				tripWriter.writeTourResultsSingleCarrier(carrier.getId().toString());
			}
			tripWriter.writeResultsPerVehicleTypes();
			tripWriter.writeTourResultsAllCarrier();
		}
		tripWriter.writeResultsPerVehicleTypes();
		tripWriter.writeTourResultsAllCarrier();
		tripWriter.writeResultsAllCarrier(CarriersUtils.addOrGetCarriers(scenario));

		log.info("### Analysis DONE");

	}

}
