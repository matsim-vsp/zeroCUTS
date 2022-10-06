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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.carrier.Tour.Pickup;
import org.matsim.contrib.freight.carrier.Tour.ServiceActivity;
import org.matsim.contrib.freight.carrier.Tour.TourElement;
import org.matsim.contrib.freight.utils.FreightUtils;
import org.matsim.core.utils.io.IOUtils;

import com.google.common.base.Joiner;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;

/**
 * @author Ricardo Ewert
 *
 */
public class TrafficVolumeGeneration {

	private static final Logger log = LogManager.getLogger(TrafficVolumeGeneration.class);
	private static final Joiner JOIN = Joiner.on("\t");

	private static HashMap<Integer, HashMap<String, Double>> generationRatesStart = new HashMap<Integer, HashMap<String, Double>>();
	private static HashMap<Integer, HashMap<String, Double>> generationRatesStop = new HashMap<Integer, HashMap<String, Double>>();
	private static HashMap<String, HashMap<String, Double>> commitmentRatesStart = new HashMap<String, HashMap<String, Double>>();
	private static HashMap<String, HashMap<String, Double>> commitmentRatesStop = new HashMap<String, HashMap<String, Double>>();

	/**
	 * Creates the traffic volume (start) for each zone separated in the
	 * modesORvehTypes and the purposes.
	 * 
	 * @param resultingDataPerZone
	 * @param output
	 * @param inputDataDirectory
	 * @param sample
	 * @param modesORvehTypes
	 * @return trafficVolume_start
	 * @throws MalformedURLException
	 */
	static HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> createTrafficVolume_start(
			HashMap<String, Object2DoubleMap<String>> resultingDataPerZone, Path output,
			double sample, ArrayList<String> modesORvehTypes, String trafficType) throws MalformedURLException {

		HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> trafficVolume_start = new HashMap<String, HashMap<String, Object2DoubleMap<Integer>>>();
		calculateTrafficVolumePerZone(trafficVolume_start, resultingDataPerZone, "start", modesORvehTypes, sample);
		Path outputFileStart = output.resolve("caculatedData")
				.resolve("TrafficVolume_" + trafficType + "_" + "startPerZone_" + (int) (sample * 100) + "pt.csv");
		writeCSVTrafficVolume(trafficVolume_start, outputFileStart);
		log.info("Write traffic volume for start trips per zone in CSV: " + outputFileStart);
		return trafficVolume_start;
	}

	/**
	 * Creates the traffic volume (stop) for each zone separated in the
	 * modesORvehTypes and the purposes.
	 * 
	 * @param resultingDataPerZone
	 * @param output
	 * @param inputDataDirectory
	 * @param sample
	 * @param modesORvehTypes
	 * @return trafficVolume_stop
	 * @throws MalformedURLException
	 */
	static HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> createTrafficVolume_stop(
			HashMap<String, Object2DoubleMap<String>> resultingDataPerZone, Path output,
			double sample, ArrayList<String> modesORvehTypes, String trafficType) throws MalformedURLException {
//TODO change type to Object2IntMap<Integer>
		HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> trafficVolume_stop = new HashMap<String, HashMap<String, Object2DoubleMap<Integer>>>();
		calculateTrafficVolumePerZone(trafficVolume_stop, resultingDataPerZone, "stop", modesORvehTypes, sample);
		Path outputFileStop = output.resolve("caculatedData")
				.resolve("TrafficVolume_" + trafficType + "_" + "stopPerZone_" + (int) (sample * 100) + "pt.csv");
		writeCSVTrafficVolume(trafficVolume_stop, outputFileStop);
		log.info("Write traffic volume for stop trips per zone in CSV: " + outputFileStop);
		return trafficVolume_stop;
	}

	/**
	 * Calculates the traffic volume for each zone and purpose.
	 * 
	 * @param trafficVolume
	 * @param resultingDataPerZone
	 * @param volumeType
	 * @param modesORvehTypes
	 * @param sample 
	 * @return trafficVolume
	 */
	private static HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> calculateTrafficVolumePerZone(
			HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> trafficVolume,
			HashMap<String, Object2DoubleMap<String>> resultingDataPerZone, String volumeType,
			ArrayList<String> modesORvehTypes, double sample) {

		HashMap<Integer, HashMap<String, Double>> generationRates = new HashMap<Integer, HashMap<String, Double>>();
		HashMap<String, HashMap<String, Double>> commitmentRates = new HashMap<String, HashMap<String, Double>>();

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
			for (String modeORvehType : modesORvehTypes) {
				Object2DoubleMap<Integer> trafficValuesPerPurpose = new Object2DoubleOpenHashMap<>();
				for (Integer purpose : generationRates.keySet()) {

					if (resultingDataPerZone.get(zoneId).isEmpty())
						trafficValuesPerPurpose.merge(purpose, 0., Double::sum);
					else
						for (String category : resultingDataPerZone.get(zoneId).keySet()) {
							double commitmentFactor;
							if (modeORvehType.equals("total"))
								commitmentFactor = 1;
							else
								commitmentFactor = commitmentRates
										.get(purpose + "_" + modeORvehType.substring(modeORvehType.length() - 1))
										.get(category);
							double generationFactor = generationRates.get(purpose).get(category);
							double newValue = resultingDataPerZone.get(zoneId).getDouble(category) * generationFactor
									* commitmentFactor;					
							trafficValuesPerPurpose.merge(purpose, newValue, Double::sum);
						}
					int sampledVolume = (int)Math.round(sample * trafficValuesPerPurpose.getDouble(purpose));
					trafficValuesPerPurpose.replace(purpose, sampledVolume); //TODO notwendig?
				}
				valuesForZone.put(modeORvehType, trafficValuesPerPurpose);
			}
			trafficVolume.put(zoneId, valuesForZone);
		}
		return trafficVolume;
	}

	/**
	 * Writes the traffic volume.
	 * 
	 * @param trafficVolume
	 * @param outputFileInInputFolder
	 * @param sample
	 * @throws MalformedURLException
	 */
	private static void writeCSVTrafficVolume(HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> trafficVolume,
			Path outputFileInInputFolder) throws MalformedURLException {
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFileInInputFolder.toUri().toURL(),
				StandardCharsets.UTF_8, true);
		try {
			String[] header = new String[] { "areaID", "mode/vehType", "1", "2", "3", "4", "5" };
			JOIN.appendTo(writer, header);
			writer.write("\n");
			for (String zoneID : trafficVolume.keySet()) {
				for (String modeORvehType : trafficVolume.get(zoneID).keySet()) {
					List<String> row = new ArrayList<>();
					row.add(zoneID);
					row.add(modeORvehType);
					Integer count = 1;
					while (count < 6) {
						row.add(String.valueOf((int)
								trafficVolume.get(zoneID).get(modeORvehType).getDouble(count)));
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
	 * Loads the input data based on the selected trafficType.
	 * 
	 * @param trafficType
	 * @throws IOException
	 */
	static void setInputParamters(String trafficType) throws IOException {

		// Set generation rates for start potentials
		generationRatesStart = setGenerationRates(trafficType, "start");

		// Set generation rates for stop potentials
		generationRatesStop = setGenerationRates(trafficType, "stop");

		// Set commitment rates for start potentials
		commitmentRatesStart = setCommitmentRates(trafficType, "start");

		// Set commitment rates for stop potentials
		commitmentRatesStop = setCommitmentRates(trafficType, "stop");
	}

	/** Reduces the traffic volumes based on the added existing models.
	 * @param scenario
	 * @param regionLinksMap
	 * @param usedTrafficType
	 * @param trafficVolumePerTypeAndZone_start
	 * @param trafficVolumePerTypeAndZone_stop
	 */
	public static void reduceDemandBasedOnExistingCarriers(Scenario scenario,
			Map<String, HashMap<Id<Link>, Link>> regionLinksMap, String usedTrafficType,
			HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> trafficVolumePerTypeAndZone_start,
			HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> trafficVolumePerTypeAndZone_stop) {
		
		for (Carrier carrier : FreightUtils.addOrGetCarriers(scenario).getCarriers().values()) {
			if (!carrier.getAttributes().getAsMap().containsKey("subpopulation") || !carrier.getAttributes().getAttribute("subpopulation").equals(usedTrafficType))
				continue;
			String modeORvehType = null;
			if (usedTrafficType.equals("freightTraffic"))
				modeORvehType = (String) carrier.getAttributes().getAttribute("vehicleType");
			else
				modeORvehType = "total";
			Integer purpose = (Integer) carrier.getAttributes().getAttribute("purpose");
			for (ScheduledTour tour : carrier.getSelectedPlan().getScheduledTours()) {
				String startZone = SmallScaleFreightTrafficUtils.findZoneOfLink(tour.getTour().getStartLinkId(), regionLinksMap);
				for (TourElement tourElement : tour.getTour().getTourElements()) {
					if (tourElement instanceof ServiceActivity) {
						ServiceActivity service = (ServiceActivity) tourElement;
						String stopZone = SmallScaleFreightTrafficUtils.findZoneOfLink(service.getLocation(), regionLinksMap);
						reduzeVolumeForThisExistingJobElement(trafficVolumePerTypeAndZone_start, trafficVolumePerTypeAndZone_stop, modeORvehType,
								purpose, startZone, stopZone);
					}
					if (tourElement instanceof Pickup) {
						Pickup shipment = (Pickup) tourElement;
						startZone = SmallScaleFreightTrafficUtils.findZoneOfLink(shipment.getShipment().getFrom(), regionLinksMap);
						String stopZone = SmallScaleFreightTrafficUtils.findZoneOfLink(shipment.getShipment().getTo(), regionLinksMap);
						reduzeVolumeForThisExistingJobElement(trafficVolumePerTypeAndZone_start, trafficVolumePerTypeAndZone_stop, modeORvehType,
								purpose, startZone, stopZone);
					}
				}
			}
		}	
	}

	/** Reduces the demand for certain zone.
	 * @param trafficVolumePerTypeAndZone_start
	 * @param trafficVolumePerTypeAndZone_stop
	 * @param modeORvehType
	 * @param purpose
	 * @param startZone
	 * @param stopZone
	 */
	private static void reduzeVolumeForThisExistingJobElement(
			HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> trafficVolumePerTypeAndZone_start,
			HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> trafficVolumePerTypeAndZone_stop,
			String modeORvehType, Integer purpose, String startZone, String stopZone) {
		
		if (trafficVolumePerTypeAndZone_start.get(startZone).get(modeORvehType).getDouble(purpose) == 0)
			reduzeVolumeForOtherArea(trafficVolumePerTypeAndZone_start, modeORvehType, purpose);
		else
			trafficVolumePerTypeAndZone_start.get(startZone).get(modeORvehType).mergeDouble(purpose, -1, Double::sum);
		if (trafficVolumePerTypeAndZone_stop.get(stopZone).get(modeORvehType).getDouble(purpose) == 0)
			reduzeVolumeForOtherArea(trafficVolumePerTypeAndZone_stop, modeORvehType, purpose);
		else
			trafficVolumePerTypeAndZone_stop.get(stopZone).get(modeORvehType).mergeDouble(purpose, -1, Double::sum);
	}

	/** Find zone with demand and reduces the demand by 1.
	 * @param trafficVolumePerTypeAndZone
	 * @param modeORvehType
	 * @param purpose
	 */
	private static void reduzeVolumeForOtherArea(
			HashMap<String, HashMap<String, Object2DoubleMap<Integer>>> trafficVolumePerTypeAndZone,
			String modeORvehType, Integer purpose) {
		ArrayList<String> shuffeldZones = new ArrayList<String>(trafficVolumePerTypeAndZone.keySet());
		Collections.shuffle(shuffeldZones);
		for (String zone : shuffeldZones) {
			if (trafficVolumePerTypeAndZone.get(zone).get(modeORvehType).getDouble(purpose) > 0) {
				trafficVolumePerTypeAndZone.get(zone).get(modeORvehType).mergeDouble(purpose, -1, Double::sum);
				log.warn("Volume of other zone was reduzed because the volume for the zone where an existing model has a demand has generated demand of 0");
				break;
			}
		}
	}

	/**
	 * Sets the generation rates based on the IVV 2005
	 * 
	 * @param trafficType
	 * @param generationType
	 * @return
	 */
	private static HashMap<Integer, HashMap<String, Double>> setGenerationRates(String trafficType,
			String generationType) {

		HashMap<Integer, HashMap<String, Double>> generationRates = new HashMap<Integer, HashMap<String, Double>>();
		HashMap<String, Double> ratesPerPurpose1 = new HashMap<>();
		HashMap<String, Double> ratesPerPurpose2 = new HashMap<>();
		HashMap<String, Double> ratesPerPurpose3 = new HashMap<>();
		HashMap<String, Double> ratesPerPurpose4 = new HashMap<>();
		HashMap<String, Double> ratesPerPurpose5 = new HashMap<>();
		HashMap<String, Double> ratesPerPurpose6 = new HashMap<>();
		if (trafficType.equals("businessTraffic")) {
			if (generationType.equals("start")) {
				ratesPerPurpose1.put("Inhabitants", 0.0);
				ratesPerPurpose1.put("Employee", 0.0);
				ratesPerPurpose1.put("Employee Primary Sector", 0.0);
				ratesPerPurpose1.put("Employee Construction", 0.0);
				ratesPerPurpose1.put("Employee Secondary Sector Rest", 0.059);
				ratesPerPurpose1.put("Employee Retail", 0.0);
				ratesPerPurpose1.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose1.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose2.put("Inhabitants", 0.0);
				ratesPerPurpose2.put("Employee", 0.029);
				ratesPerPurpose2.put("Employee Primary Sector", 0.0);
				ratesPerPurpose2.put("Employee Construction", 0.0);
				ratesPerPurpose2.put("Employee Secondary Sector Rest", 0.045);
				ratesPerPurpose2.put("Employee Retail", 0.0);
				ratesPerPurpose2.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose2.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose3.put("Inhabitants", 0.0);
				ratesPerPurpose3.put("Employee", 0.021);
				ratesPerPurpose3.put("Employee Primary Sector", 0.0);
				ratesPerPurpose3.put("Employee Construction", 0.0);
				ratesPerPurpose3.put("Employee Secondary Sector Rest", 0.0);
				ratesPerPurpose3.put("Employee Retail", 0.0192);
				ratesPerPurpose3.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose3.put("Employee Tertiary Sector Rest", 0.184);

				ratesPerPurpose4.put("Inhabitants", 0.0);
				ratesPerPurpose4.put("Employee", 0.021);
				ratesPerPurpose4.put("Employee Primary Sector", 0.0);
				ratesPerPurpose4.put("Employee Construction", 0.0);
				ratesPerPurpose4.put("Employee Secondary Sector Rest", 0.0);
				ratesPerPurpose4.put("Employee Retail", 0.0);
				ratesPerPurpose4.put("Employee Traffic/Parcels", 0.203);
				ratesPerPurpose4.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose5.put("Inhabitants", 0.0);
				ratesPerPurpose5.put("Employee", 0.03);
				ratesPerPurpose5.put("Employee Primary Sector", 0.0);
				ratesPerPurpose5.put("Employee Construction", 0.29);
				ratesPerPurpose5.put("Employee Secondary Sector Rest", 0.0);
				ratesPerPurpose5.put("Employee Retail", 0.0);
				ratesPerPurpose5.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose5.put("Employee Tertiary Sector Rest", 0.0);
			} else if (generationType.equals("stop")) {
				ratesPerPurpose1.put("Inhabitants", 0.0);
				ratesPerPurpose1.put("Employee", 0.0);
				ratesPerPurpose1.put("Employee Primary Sector", 0.0);
				ratesPerPurpose1.put("Employee Construction", 0.0);
				ratesPerPurpose1.put("Employee Secondary Sector Rest", 0.02);
				ratesPerPurpose1.put("Employee Retail", 0.0);
				ratesPerPurpose1.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose1.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose2.put("Inhabitants", 0.002);
				ratesPerPurpose2.put("Employee", 0.0);
				ratesPerPurpose2.put("Employee Primary Sector", 0.029);
				ratesPerPurpose2.put("Employee Construction", 0.029);
				ratesPerPurpose2.put("Employee Secondary Sector Rest", 0.009);
				ratesPerPurpose2.put("Employee Retail", 0.029);
				ratesPerPurpose2.put("Employee Traffic/Parcels", 0.039);
				ratesPerPurpose2.put("Employee Tertiary Sector Rest", 0.029);

				ratesPerPurpose3.put("Inhabitants", 0.025);
				ratesPerPurpose3.put("Employee", 0.0);
				ratesPerPurpose3.put("Employee Primary Sector", 0.0168);
				ratesPerPurpose3.put("Employee Construction", 0.168);
				ratesPerPurpose3.put("Employee Secondary Sector Rest", 0.0168);
				ratesPerPurpose3.put("Employee Retail", 0.0168);
				ratesPerPurpose3.put("Employee Traffic/Parcels", 0.097);
				ratesPerPurpose3.put("Employee Tertiary Sector Rest", 0.168);

				ratesPerPurpose4.put("Inhabitants", 0.002);
				ratesPerPurpose4.put("Employee", 0.0);
				ratesPerPurpose4.put("Employee Primary Sector", 0.025);
				ratesPerPurpose4.put("Employee Construction", 0.025);
				ratesPerPurpose4.put("Employee Secondary Sector Rest", 0.025);
				ratesPerPurpose4.put("Employee Retail", 0.025);
				ratesPerPurpose4.put("Employee Traffic/Parcels", 0.075);
				ratesPerPurpose4.put("Employee Tertiary Sector Rest", 0.025);

				ratesPerPurpose5.put("Inhabitants", 0.004);
				ratesPerPurpose5.put("Employee", 0.0);
				ratesPerPurpose5.put("Employee Primary Sector", 0.015);
				ratesPerPurpose5.put("Employee Construction", 0.002);
				ratesPerPurpose5.put("Employee Secondary Sector Rest", 0.015);
				ratesPerPurpose5.put("Employee Retail", 0.015);
				ratesPerPurpose5.put("Employee Traffic/Parcels", 0.02);
				ratesPerPurpose5.put("Employee Tertiary Sector Rest", 0.015);

			}
		} else if (trafficType.equals("freightTraffic")) {
			if (generationType.equals("start")) {
				ratesPerPurpose1.put("Inhabitants", 0.0);
				ratesPerPurpose1.put("Employee", 0.0);
				ratesPerPurpose1.put("Employee Primary Sector", 0.0);
				ratesPerPurpose1.put("Employee Construction", 0.0);
				ratesPerPurpose1.put("Employee Secondary Sector Rest", 0.023);
				ratesPerPurpose1.put("Employee Retail", 0.0);
				ratesPerPurpose1.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose1.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose2.put("Inhabitants", 0.0);
				ratesPerPurpose2.put("Employee", 0.002);
				ratesPerPurpose2.put("Employee Primary Sector", 0.0);
				ratesPerPurpose2.put("Employee Construction", 0.0);
				ratesPerPurpose2.put("Employee Secondary Sector Rest", 0.049);
				ratesPerPurpose2.put("Employee Retail", 0.0);
				ratesPerPurpose2.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose2.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose3.put("Inhabitants", 0.0);
				ratesPerPurpose3.put("Employee", 0.002);
				ratesPerPurpose3.put("Employee Primary Sector", 0.0);
				ratesPerPurpose3.put("Employee Construction", 0.0);
				ratesPerPurpose3.put("Employee Secondary Sector Rest", 0.0);
				ratesPerPurpose3.put("Employee Retail", 0.139);
				ratesPerPurpose3.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose3.put("Employee Tertiary Sector Rest", 0.059);

				ratesPerPurpose4.put("Inhabitants", 0.0);
				ratesPerPurpose4.put("Employee", 0.002);
				ratesPerPurpose4.put("Employee Primary Sector", 0.0);
				ratesPerPurpose4.put("Employee Construction", 0.0);
				ratesPerPurpose4.put("Employee Secondary Sector Rest", 0.0);
				ratesPerPurpose4.put("Employee Retail", 0.0);
				ratesPerPurpose4.put("Employee Traffic/Parcels", 0.333);
				ratesPerPurpose4.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose5.put("Inhabitants", 0.0);
				ratesPerPurpose5.put("Employee", 0.002);
				ratesPerPurpose5.put("Employee Primary Sector", 0.0);
				ratesPerPurpose5.put("Employee Construction", 0.220);
				ratesPerPurpose5.put("Employee Secondary Sector Rest", 0.0);
				ratesPerPurpose5.put("Employee Retail", 0.0);
				ratesPerPurpose5.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose5.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose6.put("Inhabitants", 0.009);
				ratesPerPurpose6.put("Employee", 0.0);
				ratesPerPurpose6.put("Employee Primary Sector", 0.0);
				ratesPerPurpose6.put("Employee Construction", 0.0);
				ratesPerPurpose6.put("Employee Secondary Sector Rest", 0.0);
				ratesPerPurpose6.put("Employee Retail", 0.0);
				ratesPerPurpose6.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose6.put("Employee Tertiary Sector Rest", 0.0);

			} else if (generationType.equals("stop")) {
				ratesPerPurpose1.put("Inhabitants", 0.0);
				ratesPerPurpose1.put("Employee", 0.0);
				ratesPerPurpose1.put("Employee Primary Sector", 0.0);
				ratesPerPurpose1.put("Employee Construction", 0.0);
				ratesPerPurpose1.put("Employee Secondary Sector Rest", 0.031);
				ratesPerPurpose1.put("Employee Retail", 0.0);
				ratesPerPurpose1.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose1.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose2.put("Inhabitants", 0.001);
				ratesPerPurpose2.put("Employee", 0.0);
				ratesPerPurpose2.put("Employee Primary Sector", 0.001);
				ratesPerPurpose2.put("Employee Construction", 0.01);
				ratesPerPurpose2.put("Employee Secondary Sector Rest", 0.011);
				ratesPerPurpose2.put("Employee Retail", 0.021);
				ratesPerPurpose2.put("Employee Traffic/Parcels", 0.001);
				ratesPerPurpose2.put("Employee Tertiary Sector Rest", 0.001);

				ratesPerPurpose3.put("Inhabitants", 0.009);
				ratesPerPurpose3.put("Employee", 0.0);
				ratesPerPurpose3.put("Employee Primary Sector", 0.02);
				ratesPerPurpose3.put("Employee Construction", 0.005);
				ratesPerPurpose3.put("Employee Secondary Sector Rest", 0.029);
				ratesPerPurpose3.put("Employee Retail", 0.055);
				ratesPerPurpose3.put("Employee Traffic/Parcels", 0.02);
				ratesPerPurpose3.put("Employee Tertiary Sector Rest", 0.02);

				ratesPerPurpose4.put("Inhabitants", 0.014);
				ratesPerPurpose4.put("Employee", 0.0);
				ratesPerPurpose4.put("Employee Primary Sector", 0.02);
				ratesPerPurpose4.put("Employee Construction", 0.002);
				ratesPerPurpose4.put("Employee Secondary Sector Rest", 0.11);
				ratesPerPurpose4.put("Employee Retail", 0.154);
				ratesPerPurpose4.put("Employee Traffic/Parcels", 0.02);
				ratesPerPurpose4.put("Employee Tertiary Sector Rest", 0.02);

				ratesPerPurpose5.put("Inhabitants", 0.002);
				ratesPerPurpose5.put("Employee", 0.0);
				ratesPerPurpose5.put("Employee Primary Sector", 0.005);
				ratesPerPurpose5.put("Employee Construction", 0.002);
				ratesPerPurpose5.put("Employee Secondary Sector Rest", 0.01);
				ratesPerPurpose5.put("Employee Retail", 0.01);
				ratesPerPurpose5.put("Employee Traffic/Parcels", 0.005);
				ratesPerPurpose5.put("Employee Tertiary Sector Rest", 0.005);

				ratesPerPurpose6.put("Inhabitants", 0.002);
				ratesPerPurpose6.put("Employee", 0.0);
				ratesPerPurpose6.put("Employee Primary Sector", 0.005);
				ratesPerPurpose6.put("Employee Construction", 0.002);
				ratesPerPurpose6.put("Employee Secondary Sector Rest", 0.01);
				ratesPerPurpose6.put("Employee Retail", 0.01);
				ratesPerPurpose6.put("Employee Traffic/Parcels", 0.005);
				ratesPerPurpose6.put("Employee Tertiary Sector Rest", 0.005);
			}
			generationRates.put(6, ratesPerPurpose6);
		}
		generationRates.put(1, ratesPerPurpose1);
		generationRates.put(2, ratesPerPurpose2);
		generationRates.put(3, ratesPerPurpose3);
		generationRates.put(4, ratesPerPurpose4);
		generationRates.put(5, ratesPerPurpose5);
		return generationRates;
	}

	/**
	 * Sets the commitment rates based on the IVV 2005 for the freight traffic. The
	 * commitment rate for the businessTraffic is 1, because mode choice will be
	 * done in MATSim.
	 * 
	 * @param trafficType
	 * @param commitmentType
	 * @return
	 */
	private static HashMap<String, HashMap<String, Double>> setCommitmentRates(String trafficType,
			String commitmentType) {
		HashMap<String, HashMap<String, Double>> commitmentRates = new HashMap<String, HashMap<String, Double>>();

		if (trafficType.equals("freightTraffic")) {

			// the first number is the purpose; second number the vehicle type
			HashMap<String, Double> ratesPerPurpose1_1 = new HashMap<>();
			HashMap<String, Double> ratesPerPurpose1_2 = new HashMap<>();
			HashMap<String, Double> ratesPerPurpose1_3 = new HashMap<>();
			HashMap<String, Double> ratesPerPurpose1_4 = new HashMap<>();
			HashMap<String, Double> ratesPerPurpose1_5 = new HashMap<>();
			HashMap<String, Double> ratesPerPurpose2_1 = new HashMap<>();
			HashMap<String, Double> ratesPerPurpose2_2 = new HashMap<>();
			HashMap<String, Double> ratesPerPurpose2_3 = new HashMap<>();
			HashMap<String, Double> ratesPerPurpose2_4 = new HashMap<>();
			HashMap<String, Double> ratesPerPurpose2_5 = new HashMap<>();
			HashMap<String, Double> ratesPerPurpose3_1 = new HashMap<>();
			HashMap<String, Double> ratesPerPurpose3_2 = new HashMap<>();
			HashMap<String, Double> ratesPerPurpose3_3 = new HashMap<>();
			HashMap<String, Double> ratesPerPurpose3_4 = new HashMap<>();
			HashMap<String, Double> ratesPerPurpose3_5 = new HashMap<>();
			HashMap<String, Double> ratesPerPurpose4_1 = new HashMap<>();
			HashMap<String, Double> ratesPerPurpose4_2 = new HashMap<>();
			HashMap<String, Double> ratesPerPurpose4_3 = new HashMap<>();
			HashMap<String, Double> ratesPerPurpose4_4 = new HashMap<>();
			HashMap<String, Double> ratesPerPurpose4_5 = new HashMap<>();
			HashMap<String, Double> ratesPerPurpose5_1 = new HashMap<>();
			HashMap<String, Double> ratesPerPurpose5_2 = new HashMap<>();
			HashMap<String, Double> ratesPerPurpose5_3 = new HashMap<>();
			HashMap<String, Double> ratesPerPurpose5_4 = new HashMap<>();
			HashMap<String, Double> ratesPerPurpose5_5 = new HashMap<>();
			HashMap<String, Double> ratesPerPurpose6_1 = new HashMap<>();
			HashMap<String, Double> ratesPerPurpose6_2 = new HashMap<>();
			HashMap<String, Double> ratesPerPurpose6_3 = new HashMap<>();
			HashMap<String, Double> ratesPerPurpose6_4 = new HashMap<>();
			HashMap<String, Double> ratesPerPurpose6_5 = new HashMap<>();
			if (commitmentType.equals("start")) {
				ratesPerPurpose1_1.put("Inhabitants", 0.0);
				ratesPerPurpose1_1.put("Employee", 0.8);
				ratesPerPurpose1_1.put("Employee Primary Sector", 0.0);
				ratesPerPurpose1_1.put("Employee Construction", 0.0);
				ratesPerPurpose1_1.put("Employee Secondary Sector Rest", 0.44);
				ratesPerPurpose1_1.put("Employee Retail", 0.0);
				ratesPerPurpose1_1.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose1_1.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose1_2.put("Inhabitants", 0.0);
				ratesPerPurpose1_2.put("Employee", 0.1);
				ratesPerPurpose1_2.put("Employee Primary Sector", 0.0);
				ratesPerPurpose1_2.put("Employee Construction", 0.0);
				ratesPerPurpose1_2.put("Employee Secondary Sector Rest", 0.11);
				ratesPerPurpose1_2.put("Employee Retail", 0.0);
				ratesPerPurpose1_2.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose1_2.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose1_3.put("Inhabitants", 0.0);
				ratesPerPurpose1_3.put("Employee", 0.1);
				ratesPerPurpose1_3.put("Employee Primary Sector", 0.0);
				ratesPerPurpose1_3.put("Employee Construction", 0.0);
				ratesPerPurpose1_3.put("Employee Secondary Sector Rest", 0.22);
				ratesPerPurpose1_3.put("Employee Retail", 0.0);
				ratesPerPurpose1_3.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose1_3.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose1_4.put("Inhabitants", 0.0);
				ratesPerPurpose1_4.put("Employee", 0.0);
				ratesPerPurpose1_4.put("Employee Primary Sector", 0.0);
				ratesPerPurpose1_4.put("Employee Construction", 0.0);
				ratesPerPurpose1_4.put("Employee Secondary Sector Rest", 0.06);
				ratesPerPurpose1_4.put("Employee Retail", 0.0);
				ratesPerPurpose1_4.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose1_4.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose1_5.put("Inhabitants", 0.0);
				ratesPerPurpose1_5.put("Employee", 0.0);
				ratesPerPurpose1_5.put("Employee Primary Sector", 0.0);
				ratesPerPurpose1_5.put("Employee Construction", 0.0);
				ratesPerPurpose1_5.put("Employee Secondary Sector Rest", 0.16);
				ratesPerPurpose1_5.put("Employee Retail", 0.0);
				ratesPerPurpose1_5.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose1_5.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose2_1.put("Inhabitants", 0.0);
				ratesPerPurpose2_1.put("Employee", 0.8);
				ratesPerPurpose2_1.put("Employee Primary Sector", 0.0);
				ratesPerPurpose2_1.put("Employee Construction", 0.0);
				ratesPerPurpose2_1.put("Employee Secondary Sector Rest", 0.44);
				ratesPerPurpose2_1.put("Employee Retail", 0.0);
				ratesPerPurpose2_1.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose2_1.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose2_2.put("Inhabitants", 0.0);
				ratesPerPurpose2_2.put("Employee", 0.1);
				ratesPerPurpose2_2.put("Employee Primary Sector", 0.0);
				ratesPerPurpose2_2.put("Employee Construction", 0.0);
				ratesPerPurpose2_2.put("Employee Secondary Sector Rest", 0.11);
				ratesPerPurpose2_2.put("Employee Retail", 0.0);
				ratesPerPurpose2_2.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose2_2.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose2_3.put("Inhabitants", 0.0);
				ratesPerPurpose2_3.put("Employee", 0.1);
				ratesPerPurpose2_3.put("Employee Primary Sector", 0.0);
				ratesPerPurpose2_3.put("Employee Construction", 0.0);
				ratesPerPurpose2_3.put("Employee Secondary Sector Rest", 0.22);
				ratesPerPurpose2_3.put("Employee Retail", 0.0);
				ratesPerPurpose2_3.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose2_3.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose2_4.put("Inhabitants", 0.0);
				ratesPerPurpose2_4.put("Employee", 0.0);
				ratesPerPurpose2_4.put("Employee Primary Sector", 0.0);
				ratesPerPurpose2_4.put("Employee Construction", 0.0);
				ratesPerPurpose2_4.put("Employee Secondary Sector Rest", 0.06);
				ratesPerPurpose2_4.put("Employee Retail", 0.0);
				ratesPerPurpose2_4.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose2_4.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose2_5.put("Inhabitants", 0.0);
				ratesPerPurpose2_5.put("Employee", 0.0);
				ratesPerPurpose2_5.put("Employee Primary Sector", 0.0);
				ratesPerPurpose2_5.put("Employee Construction", 0.0);
				ratesPerPurpose2_5.put("Employee Secondary Sector Rest", 0.16);
				ratesPerPurpose2_5.put("Employee Retail", 0.0);
				ratesPerPurpose2_5.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose2_5.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose3_1.put("Inhabitants", 0.0);
				ratesPerPurpose3_1.put("Employee", 0.8);
				ratesPerPurpose3_1.put("Employee Primary Sector", 0.0);
				ratesPerPurpose3_1.put("Employee Construction", 0.0);
				ratesPerPurpose3_1.put("Employee Secondary Sector Rest", 0.0);
				ratesPerPurpose3_1.put("Employee Retail", 0.46);
				ratesPerPurpose3_1.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose3_1.put("Employee Tertiary Sector Rest", 0.54);

				ratesPerPurpose3_2.put("Inhabitants", 0.0);
				ratesPerPurpose3_2.put("Employee", 0.1);
				ratesPerPurpose3_2.put("Employee Primary Sector", 0.0);
				ratesPerPurpose3_2.put("Employee Construction", 0.0);
				ratesPerPurpose3_2.put("Employee Secondary Sector Rest", 0.0);
				ratesPerPurpose3_2.put("Employee Retail", 0.1);
				ratesPerPurpose3_2.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose3_2.put("Employee Tertiary Sector Rest", 0.1);

				ratesPerPurpose3_3.put("Inhabitants", 0.0);
				ratesPerPurpose3_3.put("Employee", 0.1);
				ratesPerPurpose3_3.put("Employee Primary Sector", 0.0);
				ratesPerPurpose3_3.put("Employee Construction", 0.0);
				ratesPerPurpose3_3.put("Employee Secondary Sector Rest", 0.0);
				ratesPerPurpose3_3.put("Employee Retail", 0.23);
				ratesPerPurpose3_3.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose3_3.put("Employee Tertiary Sector Rest", 0.2);

				ratesPerPurpose3_4.put("Inhabitants", 0.0);
				ratesPerPurpose3_4.put("Employee", 0.0);
				ratesPerPurpose3_4.put("Employee Primary Sector", 0.0);
				ratesPerPurpose3_4.put("Employee Construction", 0.0);
				ratesPerPurpose3_4.put("Employee Secondary Sector Rest", 0.0);
				ratesPerPurpose3_4.put("Employee Retail", 0.06);
				ratesPerPurpose3_4.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose3_4.put("Employee Tertiary Sector Rest", 0.2);

				ratesPerPurpose3_5.put("Inhabitants", 0.0);
				ratesPerPurpose3_5.put("Employee", 0.0);
				ratesPerPurpose3_5.put("Employee Primary Sector", 0.0);
				ratesPerPurpose3_5.put("Employee Construction", 0.0);
				ratesPerPurpose3_5.put("Employee Secondary Sector Rest", 0.0);
				ratesPerPurpose3_5.put("Employee Retail", 0.15);
				ratesPerPurpose3_5.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose3_5.put("Employee Tertiary Sector Rest", 0.14);

				ratesPerPurpose4_1.put("Inhabitants", 0.009);
				ratesPerPurpose4_1.put("Employee", 0.8);
				ratesPerPurpose4_1.put("Employee Primary Sector", 0.0);
				ratesPerPurpose4_1.put("Employee Construction", 0.0);
				ratesPerPurpose4_1.put("Employee Secondary Sector Rest", 0.0);
				ratesPerPurpose4_1.put("Employee Retail", 0.0);
				ratesPerPurpose4_1.put("Employee Traffic/Parcels", 0.18);
				ratesPerPurpose4_1.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose4_2.put("Inhabitants", 0.0);
				ratesPerPurpose4_2.put("Employee", 0.1);
				ratesPerPurpose4_2.put("Employee Primary Sector", 0.0);
				ratesPerPurpose4_2.put("Employee Construction", 0.0);
				ratesPerPurpose4_2.put("Employee Secondary Sector Rest", 0.0);
				ratesPerPurpose4_2.put("Employee Retail", 0.0);
				ratesPerPurpose4_2.put("Employee Traffic/Parcels", 0.06);
				ratesPerPurpose4_2.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose4_3.put("Inhabitants", 0.0);
				ratesPerPurpose4_3.put("Employee", 0.1);
				ratesPerPurpose4_3.put("Employee Primary Sector", 0.0);
				ratesPerPurpose4_3.put("Employee Construction", 0.0);
				ratesPerPurpose4_3.put("Employee Secondary Sector Rest", 0.0);
				ratesPerPurpose4_3.put("Employee Retail", 0.0);
				ratesPerPurpose4_3.put("Employee Traffic/Parcels", 0.25);
				ratesPerPurpose4_3.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose4_4.put("Inhabitants", 0.0);
				ratesPerPurpose4_4.put("Employee", 0.0);
				ratesPerPurpose4_4.put("Employee Primary Sector", 0.0);
				ratesPerPurpose4_4.put("Employee Construction", 0.0);
				ratesPerPurpose4_4.put("Employee Secondary Sector Rest", 0.0);
				ratesPerPurpose4_4.put("Employee Retail", 0.0);
				ratesPerPurpose4_4.put("Employee Traffic/Parcels", 0.08);
				ratesPerPurpose4_4.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose4_5.put("Inhabitants", 0.0);
				ratesPerPurpose4_5.put("Employee", 0.0);
				ratesPerPurpose4_5.put("Employee Primary Sector", 0.0);
				ratesPerPurpose4_5.put("Employee Construction", 0.0);
				ratesPerPurpose4_5.put("Employee Secondary Sector Rest", 0.0);
				ratesPerPurpose4_5.put("Employee Retail", 0.0);
				ratesPerPurpose4_5.put("Employee Traffic/Parcels", 0.43);
				ratesPerPurpose4_5.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose5_1.put("Inhabitants", 0.0);
				ratesPerPurpose5_1.put("Employee", 0.8);
				ratesPerPurpose5_1.put("Employee Primary Sector", 0.0);
				ratesPerPurpose5_1.put("Employee Construction", 0.25);
				ratesPerPurpose5_1.put("Employee Secondary Sector Rest", 0.0);
				ratesPerPurpose5_1.put("Employee Retail", 0.0);
				ratesPerPurpose5_1.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose5_1.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose5_2.put("Inhabitants", 0.0);
				ratesPerPurpose5_2.put("Employee", 0.1);
				ratesPerPurpose5_2.put("Employee Primary Sector", 0.0);
				ratesPerPurpose5_2.put("Employee Construction", 0.2);
				ratesPerPurpose5_2.put("Employee Secondary Sector Rest", 0.0);
				ratesPerPurpose5_2.put("Employee Retail", 0.0);
				ratesPerPurpose5_2.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose5_2.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose5_3.put("Inhabitants", 0.0);
				ratesPerPurpose5_3.put("Employee", 0.1);
				ratesPerPurpose5_3.put("Employee Primary Sector", 0.0);
				ratesPerPurpose5_3.put("Employee Construction", 0.25);
				ratesPerPurpose5_3.put("Employee Secondary Sector Rest", 0.0);
				ratesPerPurpose5_3.put("Employee Retail", 0.139);
				ratesPerPurpose5_3.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose5_3.put("Employee Tertiary Sector Rest", 0.059);

				ratesPerPurpose5_4.put("Inhabitants", 0.0);
				ratesPerPurpose5_4.put("Employee", 0.0);
				ratesPerPurpose5_4.put("Employee Primary Sector", 0.0);
				ratesPerPurpose5_4.put("Employee Construction", 0.02);
				ratesPerPurpose5_4.put("Employee Secondary Sector Rest", 0.0);
				ratesPerPurpose5_4.put("Employee Retail", 0.0);
				ratesPerPurpose5_4.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose5_4.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose5_5.put("Inhabitants", 0.0);
				ratesPerPurpose5_5.put("Employee", 0.0);
				ratesPerPurpose5_5.put("Employee Primary Sector", 0.0);
				ratesPerPurpose5_5.put("Employee Construction", 0.28);
				ratesPerPurpose5_5.put("Employee Secondary Sector Rest", 0.0);
				ratesPerPurpose5_5.put("Employee Retail", 0.0);
				ratesPerPurpose5_5.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose5_5.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose6_1.put("Inhabitants", 0.0);
				ratesPerPurpose6_1.put("Employee", 0.0);
				ratesPerPurpose6_1.put("Employee Primary Sector", 0.0);
				ratesPerPurpose6_1.put("Employee Construction", 0.0);
				ratesPerPurpose6_1.put("Employee Secondary Sector Rest", 0.0);
				ratesPerPurpose6_1.put("Employee Retail", 0.0);
				ratesPerPurpose6_1.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose6_1.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose6_2.put("Inhabitants", 0.29);
				ratesPerPurpose6_2.put("Employee", 0.0);
				ratesPerPurpose6_2.put("Employee Primary Sector", 0.0);
				ratesPerPurpose6_2.put("Employee Construction", 0.0);
				ratesPerPurpose6_2.put("Employee Secondary Sector Rest", 0.0);
				ratesPerPurpose6_2.put("Employee Retail", 0.0);
				ratesPerPurpose6_2.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose6_2.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose6_3.put("Inhabitants", 0.63);
				ratesPerPurpose6_3.put("Employee", 0.0);
				ratesPerPurpose6_3.put("Employee Primary Sector", 0.0);
				ratesPerPurpose6_3.put("Employee Construction", 0.0);
				ratesPerPurpose6_3.put("Employee Secondary Sector Rest", 0.0);
				ratesPerPurpose6_3.put("Employee Retail", 0.0);
				ratesPerPurpose6_3.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose6_3.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose6_4.put("Inhabitants", 0.07);
				ratesPerPurpose6_4.put("Employee", 0.0);
				ratesPerPurpose6_4.put("Employee Primary Sector", 0.0);
				ratesPerPurpose6_4.put("Employee Construction", 0.0);
				ratesPerPurpose6_4.put("Employee Secondary Sector Rest", 0.0);
				ratesPerPurpose6_4.put("Employee Retail", 0.0);
				ratesPerPurpose6_4.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose6_4.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose6_5.put("Inhabitants", 0.001);
				ratesPerPurpose6_5.put("Employee", 0.0);
				ratesPerPurpose6_5.put("Employee Primary Sector", 0.0);
				ratesPerPurpose6_5.put("Employee Construction", 0.2);
				ratesPerPurpose6_5.put("Employee Secondary Sector Rest", 0.0);
				ratesPerPurpose6_5.put("Employee Retail", 0.0);
				ratesPerPurpose6_5.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose6_5.put("Employee Tertiary Sector Rest", 0.0);
			} else if (commitmentType.equals("stop")) {
				ratesPerPurpose1_1.put("Inhabitants", 0.0);
				ratesPerPurpose1_1.put("Employee", 0.0);
				ratesPerPurpose1_1.put("Employee Primary Sector", 0.0);
				ratesPerPurpose1_1.put("Employee Construction", 0.0);
				ratesPerPurpose1_1.put("Employee Secondary Sector Rest", 0.35);
				ratesPerPurpose1_1.put("Employee Retail", 0.0);
				ratesPerPurpose1_1.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose1_1.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose1_2.put("Inhabitants", 0.0);
				ratesPerPurpose1_2.put("Employee", 0.0);
				ratesPerPurpose1_2.put("Employee Primary Sector", 0.0);
				ratesPerPurpose1_2.put("Employee Construction", 0.0);
				ratesPerPurpose1_2.put("Employee Secondary Sector Rest", 0.1);
				ratesPerPurpose1_2.put("Employee Retail", 0.0);
				ratesPerPurpose1_2.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose1_2.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose1_3.put("Inhabitants", 0.0);
				ratesPerPurpose1_3.put("Employee", 0.0);
				ratesPerPurpose1_3.put("Employee Primary Sector", 0.0);
				ratesPerPurpose1_3.put("Employee Construction", 0.0);
				ratesPerPurpose1_3.put("Employee Secondary Sector Rest", 0.27);
				ratesPerPurpose1_3.put("Employee Retail", 0.0);
				ratesPerPurpose1_3.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose1_3.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose1_4.put("Inhabitants", 0.0);
				ratesPerPurpose1_4.put("Employee", 0.0);
				ratesPerPurpose1_4.put("Employee Primary Sector", 0.0);
				ratesPerPurpose1_4.put("Employee Construction", 0.0);
				ratesPerPurpose1_4.put("Employee Secondary Sector Rest", 0.01);
				ratesPerPurpose1_4.put("Employee Retail", 0.0);
				ratesPerPurpose1_4.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose1_4.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose1_5.put("Inhabitants", 0.0);
				ratesPerPurpose1_5.put("Employee", 0.0);
				ratesPerPurpose1_5.put("Employee Primary Sector", 0.0);
				ratesPerPurpose1_5.put("Employee Construction", 0.0);
				ratesPerPurpose1_5.put("Employee Secondary Sector Rest", 0.27);
				ratesPerPurpose1_5.put("Employee Retail", 0.0);
				ratesPerPurpose1_5.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose1_5.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose2_1.put("Inhabitants", 0.55);
				ratesPerPurpose2_1.put("Employee", 0.0);
				ratesPerPurpose2_1.put("Employee Primary Sector", 0.46);
				ratesPerPurpose2_1.put("Employee Construction", 0.46);
				ratesPerPurpose2_1.put("Employee Secondary Sector Rest", 0.46);
				ratesPerPurpose2_1.put("Employee Retail", 0.46);
				ratesPerPurpose2_1.put("Employee Traffic/Parcels", 0.34);
				ratesPerPurpose2_1.put("Employee Tertiary Sector Rest", 0.46);

				ratesPerPurpose2_2.put("Inhabitants", 0.09);
				ratesPerPurpose2_2.put("Employee", 0.0);
				ratesPerPurpose2_2.put("Employee Primary Sector", 0.09);
				ratesPerPurpose2_2.put("Employee Construction", 0.09);
				ratesPerPurpose2_2.put("Employee Secondary Sector Rest", 0.09);
				ratesPerPurpose2_2.put("Employee Retail", 0.09);
				ratesPerPurpose2_2.put("Employee Traffic/Parcels", 0.1);
				ratesPerPurpose2_2.put("Employee Tertiary Sector Rest", 0.09);

				ratesPerPurpose2_3.put("Inhabitants", 0.21);
				ratesPerPurpose2_3.put("Employee", 0.0);
				ratesPerPurpose2_3.put("Employee Primary Sector", 0.22);
				ratesPerPurpose2_3.put("Employee Construction", 0.22);
				ratesPerPurpose2_3.put("Employee Secondary Sector Rest", 0.22);
				ratesPerPurpose2_3.put("Employee Retail", 0.22);
				ratesPerPurpose2_3.put("Employee Traffic/Parcels", 0.29);
				ratesPerPurpose2_3.put("Employee Tertiary Sector Rest", 0.22);

				ratesPerPurpose2_4.put("Inhabitants", 0.06);
				ratesPerPurpose2_4.put("Employee", 0.0);
				ratesPerPurpose2_4.put("Employee Primary Sector", 0.06);
				ratesPerPurpose2_4.put("Employee Construction", 0.06);
				ratesPerPurpose2_4.put("Employee Secondary Sector Rest", 0.06);
				ratesPerPurpose2_4.put("Employee Retail", 0.06);
				ratesPerPurpose2_4.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose2_4.put("Employee Tertiary Sector Rest", 0.06);

				ratesPerPurpose2_5.put("Inhabitants", 0.1);
				ratesPerPurpose2_5.put("Employee", 0.0);
				ratesPerPurpose2_5.put("Employee Primary Sector", 0.17);
				ratesPerPurpose2_5.put("Employee Construction", 0.17);
				ratesPerPurpose2_5.put("Employee Secondary Sector Rest", 0.17);
				ratesPerPurpose2_5.put("Employee Retail", 0.17);
				ratesPerPurpose2_5.put("Employee Traffic/Parcels", 0.27);
				ratesPerPurpose2_5.put("Employee Tertiary Sector Rest", 0.17);

				ratesPerPurpose3_1.put("Inhabitants", 0.489);
				ratesPerPurpose3_1.put("Employee", 0.0);
				ratesPerPurpose3_1.put("Employee Primary Sector", 0.538);
				ratesPerPurpose3_1.put("Employee Construction", 0.538);
				ratesPerPurpose3_1.put("Employee Secondary Sector Rest", 0.538);
				ratesPerPurpose3_1.put("Employee Retail", 0.538);
				ratesPerPurpose3_1.put("Employee Traffic/Parcels", 0.59);
				ratesPerPurpose3_1.put("Employee Tertiary Sector Rest", 0.538);

				ratesPerPurpose3_2.put("Inhabitants", 0.106);
				ratesPerPurpose3_2.put("Employee", 0.0);
				ratesPerPurpose3_2.put("Employee Primary Sector", 0.092);
				ratesPerPurpose3_2.put("Employee Construction", 0.092);
				ratesPerPurpose3_2.put("Employee Secondary Sector Rest", 0.092);
				ratesPerPurpose3_2.put("Employee Retail", 0.092);
				ratesPerPurpose3_2.put("Employee Traffic/Parcels", 0.03);
				ratesPerPurpose3_2.put("Employee Tertiary Sector Rest", 0.092);

				ratesPerPurpose3_3.put("Inhabitants", 0.26);
				ratesPerPurpose3_3.put("Employee", 0.0);
				ratesPerPurpose3_3.put("Employee Primary Sector", 0.19);
				ratesPerPurpose3_3.put("Employee Construction", 0.19);
				ratesPerPurpose3_3.put("Employee Secondary Sector Rest", 0.19);
				ratesPerPurpose3_3.put("Employee Retail", 0.19);
				ratesPerPurpose3_3.put("Employee Traffic/Parcels", 0.102);
				ratesPerPurpose3_3.put("Employee Tertiary Sector Rest", 0.19);

				ratesPerPurpose3_4.put("Inhabitants", 0.033);
				ratesPerPurpose3_4.put("Employee", 0.0);
				ratesPerPurpose3_4.put("Employee Primary Sector", 0.032);
				ratesPerPurpose3_4.put("Employee Construction", 0.032);
				ratesPerPurpose3_4.put("Employee Secondary Sector Rest", 0.032);
				ratesPerPurpose3_4.put("Employee Retail", 0.032);
				ratesPerPurpose3_4.put("Employee Traffic/Parcels", 0.058);
				ratesPerPurpose3_4.put("Employee Tertiary Sector Rest", 0.032);

				ratesPerPurpose3_5.put("Inhabitants", 0.112);
				ratesPerPurpose3_5.put("Employee", 0.0);
				ratesPerPurpose3_5.put("Employee Primary Sector", 0.147);
				ratesPerPurpose3_5.put("Employee Construction", 0.147);
				ratesPerPurpose3_5.put("Employee Secondary Sector Rest", 0.147);
				ratesPerPurpose3_5.put("Employee Retail", 0.147);
				ratesPerPurpose3_5.put("Employee Traffic/Parcels", 0.219);
				ratesPerPurpose3_5.put("Employee Tertiary Sector Rest", 0.147);

				ratesPerPurpose4_1.put("Inhabitants", 0.37);
				ratesPerPurpose4_1.put("Employee", 0.0);
				ratesPerPurpose4_1.put("Employee Primary Sector", 0.14);
				ratesPerPurpose4_1.put("Employee Construction", 0.14);
				ratesPerPurpose4_1.put("Employee Secondary Sector Rest", 0.14);
				ratesPerPurpose4_1.put("Employee Retail", 0.14);
				ratesPerPurpose4_1.put("Employee Traffic/Parcels", 0.06);
				ratesPerPurpose4_1.put("Employee Tertiary Sector Rest", 0.14);

				ratesPerPurpose4_2.put("Inhabitants", 0.05);
				ratesPerPurpose4_2.put("Employee", 0.0);
				ratesPerPurpose4_2.put("Employee Primary Sector", 0.07);
				ratesPerPurpose4_2.put("Employee Construction", 0.07);
				ratesPerPurpose4_2.put("Employee Secondary Sector Rest", 0.07);
				ratesPerPurpose4_2.put("Employee Retail", 0.07);
				ratesPerPurpose4_2.put("Employee Traffic/Parcels", 0.07);
				ratesPerPurpose4_2.put("Employee Tertiary Sector Rest", 0.07);

				ratesPerPurpose4_3.put("Inhabitants", 0.4);
				ratesPerPurpose4_3.put("Employee", 0.0);
				ratesPerPurpose4_3.put("Employee Primary Sector", 0.21);
				ratesPerPurpose4_3.put("Employee Construction", 0.21);
				ratesPerPurpose4_3.put("Employee Secondary Sector Rest", 0.21);
				ratesPerPurpose4_3.put("Employee Retail", 0.21);
				ratesPerPurpose4_3.put("Employee Traffic/Parcels", 0.19);
				ratesPerPurpose4_3.put("Employee Tertiary Sector Rest", 0.21);

				ratesPerPurpose4_4.put("Inhabitants", 0.13);
				ratesPerPurpose4_4.put("Employee", 0.0);
				ratesPerPurpose4_4.put("Employee Primary Sector", 0.05);
				ratesPerPurpose4_4.put("Employee Construction", 0.05);
				ratesPerPurpose4_4.put("Employee Secondary Sector Rest", 0.05);
				ratesPerPurpose4_4.put("Employee Retail", 0.05);
				ratesPerPurpose4_4.put("Employee Traffic/Parcels", 0.08);
				ratesPerPurpose4_4.put("Employee Tertiary Sector Rest", 0.05);

				ratesPerPurpose4_5.put("Inhabitants", 0.05);
				ratesPerPurpose4_5.put("Employee", 0.0);
				ratesPerPurpose4_5.put("Employee Primary Sector", 0.54);
				ratesPerPurpose4_5.put("Employee Construction", 0.54);
				ratesPerPurpose4_5.put("Employee Secondary Sector Rest", 0.54);
				ratesPerPurpose4_5.put("Employee Retail", 0.54);
				ratesPerPurpose4_5.put("Employee Traffic/Parcels", 0.61);
				ratesPerPurpose4_5.put("Employee Tertiary Sector Rest", 0.54);

				ratesPerPurpose5_1.put("Inhabitants", 0.16);
				ratesPerPurpose5_1.put("Employee", 0.0);
				ratesPerPurpose5_1.put("Employee Primary Sector", 0.4);
				ratesPerPurpose5_1.put("Employee Construction", 0.4);
				ratesPerPurpose5_1.put("Employee Secondary Sector Rest", 0.4);
				ratesPerPurpose5_1.put("Employee Retail", 0.4);
				ratesPerPurpose5_1.put("Employee Traffic/Parcels", 0.14);
				ratesPerPurpose5_1.put("Employee Tertiary Sector Rest", 0.4);

				ratesPerPurpose5_2.put("Inhabitants", 0.55);
				ratesPerPurpose5_2.put("Employee", 0.11);
				ratesPerPurpose5_2.put("Employee Primary Sector", 0.11);
				ratesPerPurpose5_2.put("Employee Construction", 0.11);
				ratesPerPurpose5_2.put("Employee Secondary Sector Rest", 0.11);
				ratesPerPurpose5_2.put("Employee Retail", 0.11);
				ratesPerPurpose5_2.put("Employee Traffic/Parcels", 0.06);
				ratesPerPurpose5_2.put("Employee Tertiary Sector Rest", 0.11);

				ratesPerPurpose5_3.put("Inhabitants", 0.22);
				ratesPerPurpose5_3.put("Employee", 0.0);
				ratesPerPurpose5_3.put("Employee Primary Sector", 0.17);
				ratesPerPurpose5_3.put("Employee Construction", 0.17);
				ratesPerPurpose5_3.put("Employee Secondary Sector Rest", 0.17);
				ratesPerPurpose5_3.put("Employee Retail", 0.17);
				ratesPerPurpose5_3.put("Employee Traffic/Parcels", 0.21);
				ratesPerPurpose5_3.put("Employee Tertiary Sector Rest", 0.17);

				ratesPerPurpose5_4.put("Inhabitants", 0.0);
				ratesPerPurpose5_4.put("Employee", 0.0);
				ratesPerPurpose5_4.put("Employee Primary Sector", 0.04);
				ratesPerPurpose5_4.put("Employee Construction", 0.04);
				ratesPerPurpose5_4.put("Employee Secondary Sector Rest", 0.04);
				ratesPerPurpose5_4.put("Employee Retail", 0.04);
				ratesPerPurpose5_4.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose5_4.put("Employee Tertiary Sector Rest", 0.04);

				ratesPerPurpose5_5.put("Inhabitants", 0.06);
				ratesPerPurpose5_5.put("Employee", 0.0);
				ratesPerPurpose5_5.put("Employee Primary Sector", 0.28);
				ratesPerPurpose5_5.put("Employee Construction", 0.28);
				ratesPerPurpose5_5.put("Employee Secondary Sector Rest", 0.28);
				ratesPerPurpose5_5.put("Employee Retail", 0.28);
				ratesPerPurpose5_5.put("Employee Traffic/Parcels", 0.58);
				ratesPerPurpose5_5.put("Employee Tertiary Sector Rest", 0.28);

				ratesPerPurpose6_1.put("Inhabitants", 0.0);
				ratesPerPurpose6_1.put("Employee", 0.0);
				ratesPerPurpose6_1.put("Employee Primary Sector", 0.0);
				ratesPerPurpose6_1.put("Employee Construction", 0.0);
				ratesPerPurpose6_1.put("Employee Secondary Sector Rest", 0.0);
				ratesPerPurpose6_1.put("Employee Retail", 0.0);
				ratesPerPurpose6_1.put("Employee Traffic/Parcels", 0.0);
				ratesPerPurpose6_1.put("Employee Tertiary Sector Rest", 0.0);

				ratesPerPurpose6_2.put("Inhabitants", 0.85);
				ratesPerPurpose6_2.put("Employee", 0.0);
				ratesPerPurpose6_2.put("Employee Primary Sector", 0.21);
				ratesPerPurpose6_2.put("Employee Construction", 0.21);
				ratesPerPurpose6_2.put("Employee Secondary Sector Rest", 0.21);
				ratesPerPurpose6_2.put("Employee Retail", 0.21);
				ratesPerPurpose6_2.put("Employee Traffic/Parcels", 0.09);
				ratesPerPurpose6_2.put("Employee Tertiary Sector Rest", 0.21);

				ratesPerPurpose6_3.put("Inhabitants", 0.15);
				ratesPerPurpose6_3.put("Employee", 0.0);
				ratesPerPurpose6_3.put("Employee Primary Sector", 0.58);
				ratesPerPurpose6_3.put("Employee Construction", 0.58);
				ratesPerPurpose6_3.put("Employee Secondary Sector Rest", 0.58);
				ratesPerPurpose6_3.put("Employee Retail", 0.58);
				ratesPerPurpose6_3.put("Employee Traffic/Parcels", 0.55);
				ratesPerPurpose6_3.put("Employee Tertiary Sector Rest", 0.58);

				ratesPerPurpose6_4.put("Inhabitants", 0.0);
				ratesPerPurpose6_4.put("Employee", 0.0);
				ratesPerPurpose6_4.put("Employee Primary Sector", 0.21);
				ratesPerPurpose6_4.put("Employee Construction", 0.21);
				ratesPerPurpose6_4.put("Employee Secondary Sector Rest", 0.21);
				ratesPerPurpose6_4.put("Employee Retail", 0.21);
				ratesPerPurpose6_4.put("Employee Traffic/Parcels", 0.25);
				ratesPerPurpose6_4.put("Employee Tertiary Sector Rest", 0.21);

				ratesPerPurpose6_5.put("Inhabitants", 0.0);
				ratesPerPurpose6_5.put("Employee", 0.0);
				ratesPerPurpose6_5.put("Employee Primary Sector", 0.0);
				ratesPerPurpose6_5.put("Employee Construction", 0.0);
				ratesPerPurpose6_5.put("Employee Secondary Sector Rest", 0.0);
				ratesPerPurpose6_5.put("Employee Retail", 0.0);
				ratesPerPurpose6_5.put("Employee Traffic/Parcels", 0.11);
				ratesPerPurpose6_5.put("Employee Tertiary Sector Rest", 0.0);
			}
			commitmentRates.put("1_1", ratesPerPurpose1_1);
			commitmentRates.put("1_2", ratesPerPurpose1_2);
			commitmentRates.put("1_3", ratesPerPurpose1_3);
			commitmentRates.put("1_4", ratesPerPurpose1_4);
			commitmentRates.put("1_5", ratesPerPurpose1_5);
			commitmentRates.put("2_1", ratesPerPurpose1_1);
			commitmentRates.put("2_2", ratesPerPurpose1_2);
			commitmentRates.put("2_3", ratesPerPurpose1_3);
			commitmentRates.put("2_4", ratesPerPurpose1_4);
			commitmentRates.put("2_5", ratesPerPurpose1_5);
			commitmentRates.put("3_1", ratesPerPurpose1_1);
			commitmentRates.put("3_2", ratesPerPurpose1_2);
			commitmentRates.put("3_3", ratesPerPurpose1_3);
			commitmentRates.put("3_4", ratesPerPurpose1_4);
			commitmentRates.put("3_5", ratesPerPurpose1_5);
			commitmentRates.put("4_1", ratesPerPurpose1_1);
			commitmentRates.put("4_2", ratesPerPurpose1_2);
			commitmentRates.put("4_3", ratesPerPurpose1_3);
			commitmentRates.put("4_4", ratesPerPurpose1_4);
			commitmentRates.put("4_5", ratesPerPurpose1_5);
			commitmentRates.put("5_1", ratesPerPurpose1_1);
			commitmentRates.put("5_2", ratesPerPurpose1_2);
			commitmentRates.put("5_3", ratesPerPurpose1_3);
			commitmentRates.put("5_4", ratesPerPurpose1_4);
			commitmentRates.put("5_5", ratesPerPurpose1_5);
			commitmentRates.put("6_1", ratesPerPurpose1_1);
			commitmentRates.put("6_2", ratesPerPurpose1_2);
			commitmentRates.put("6_3", ratesPerPurpose1_3);
			commitmentRates.put("6_4", ratesPerPurpose1_4);
			commitmentRates.put("6_5", ratesPerPurpose1_5);
		}
		return commitmentRates;
	}
}