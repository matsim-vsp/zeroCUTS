package org.matsim.vsp.wasteCollection.Berlin;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.CarrierCapabilities;
import org.matsim.freight.carriers.CarrierVehicle;
import org.matsim.freight.carriers.CarrierVehicleTypes;
import org.matsim.freight.carriers.Carriers;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.freight.carriers.CarrierCapabilities.FleetSize;
import org.matsim.freight.carriers.CarrierImpl;

public class AbfallChessboardUtils {

	static String linkChessboardDump = "j(0,9)R";
	static String linkChessboardDepot = "j(0,7)R";
	static Carrier carrierChessboard = CarrierImpl.newInstance(Id.create("Carrier_Chessboard", Carrier.class));

	/**
	 * Creates shipments for the chessboard network with the input of the volume
	 * [kg] garbageToCollect.
	 * 
	 * @param
	 */
	static void createShipmentsForChessboardI(HashMap<String, Carrier> carrierMap, int garbageToCollect,
			Map<Id<Link>, ? extends Link> allLinks, double volumeBigDustbin, double serviceTimePerBigTrashcan,
			Scenario scenario, Carriers carriers) {
		Map<Id<Link>, Link> garbageLinks = new HashMap<Id<Link>, Link>();
		carrierMap.clear();
		carrierMap.put("carrierChessboard", carrierChessboard);
		double distanceWithShipments = 0;
		for (Link link : allLinks.values()) {
			if (link.getFreespeed() < 13.9) {
				garbageLinks.put(link.getId(), link);
				distanceWithShipments = distanceWithShipments + link.getLength();
			}
		}
		Id<Link> linkDumpId = Id.createLinkId(linkChessboardDump);
		AbfallUtils.createShipmentsForCarrierII(garbageToCollect, volumeBigDustbin, serviceTimePerBigTrashcan,
				distanceWithShipments, garbageLinks, scenario, carrierChessboard, linkDumpId, carriers);
		AbfallUtils.districtsWithShipments.add("Chessboard");
		carriers.addCarrier(carrierChessboard);

	}

	/**
	 * Creates shipments for the chessboard network with the input of the volume
	 * [kg] garbagePerMeterToCollect. So every meter of the network gets this volume
	 * of the garbage.
	 * 
	 * @param
	 */
	static void createShipmentsForChessboardII(HashMap<String, Carrier> carrierMap, double garbagePerMeterToCollect,
			Map<Id<Link>, ? extends Link> allLinks, double volumeBigDustbin, double serviceTimePerBigTrashcan,
			Scenario scenario, Carriers carriers) {
		Map<Id<Link>, Link> garbageLinks = new HashMap<Id<Link>, Link>();
		carrierMap.clear();
		carrierMap.put("carrierChessboard", carrierChessboard);
		double distanceWithShipments = 0;
		for (Link link : allLinks.values()) {
			if (link.getFreespeed() < 12) {
				garbageLinks.put(link.getId(), link);
				distanceWithShipments = distanceWithShipments + link.getLength();
			}
		}
		Id<Link> linkDumpId = Id.createLinkId(linkChessboardDump);
		AbfallUtils.createShipmentsForCarrierI(garbagePerMeterToCollect, volumeBigDustbin, serviceTimePerBigTrashcan,
				garbageLinks, scenario, carrierChessboard, linkDumpId, carriers);
		AbfallUtils.districtsWithShipments.add("Chessboard");
		carriers.addCarrier(carrierChessboard);

	}

	/**
	 * Creates the vehicle at the depot, ads this vehicle to the carriers and sets
	 * the capabilities. This method is for the Chessboard network with one depot.
	 * 
	 * @param
	 */
	static void createCarriersForChessboard(Carriers carriers, FleetSize fleetSize, CarrierVehicleTypes carrierVehicleTypes) {
		String vehicleName = "TruckChessboard";
		double earliestStartingTime = 6 * 3600;
		double latestFinishingTime = 14 * 3600;

		VehicleType vehicleType = carrierVehicleTypes.getVehicleTypes().values().iterator().next();
		CarrierVehicle newCarrierVehicle =  CarrierVehicle.Builder
			.newInstance(Id.create(vehicleName, Vehicle.class), Id.createLinkId(linkChessboardDepot), vehicleType)
			.setEarliestStart(earliestStartingTime).setLatestEnd(latestFinishingTime).build();

//		AbfallUtils.createGarbageTruck(vehicleName, linkChessboardDepot, earliestStartingTime, latestFinishingTime);

		// define Carriers

		defineCarriersChessboard(carriers, newCarrierVehicle, fleetSize, carrierVehicleTypes);
	}

	/**
	 * Defines and sets the Capabilities of the Carrier, including the vehicleTypes
	 * for the carriers for the Chessboard network
	 * 
	 * @param
	 * 
	 */
	private static void defineCarriersChessboard(Carriers carriers, CarrierVehicle vehicleDepot, FleetSize fleetSize, CarrierVehicleTypes carrierVehicleTypes) {
		CarrierCapabilities carrierCapabilities = CarrierCapabilities.Builder.newInstance()
				.addVehicle(vehicleDepot).setFleetSize(fleetSize).build();

		carrierChessboard.setCarrierCapabilities(carrierCapabilities);
	}
}
