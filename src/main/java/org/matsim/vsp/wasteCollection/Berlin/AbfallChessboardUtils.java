package org.matsim.vsp.wasteCollection.Berlin;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.freight.carriers.*;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.freight.carriers.CarrierCapabilities.FleetSize;

public class AbfallChessboardUtils {

	static String linkChessboardDump = "j(0,9)R";
	static String linkChessboardDepot = "j(0,7)R";
	static Carrier carrierChessboard = CarriersUtils.createCarrier(Id.create("Carrier_Chessboard", Carrier.class));

	/**
     * Creates shipments for the chessboard network with the input of the volume
     * [kg] garbageToCollect.
     *
     * @param jspritIterations number of iterations for jsprit
     */
	static void createShipmentsForChessboardI(HashMap<String, Carrier> carrierMap, int garbageToCollect,
                                              Map<Id<Link>, ? extends Link> allLinks, double volumeBigDustbin, double serviceTimePerBigTrashcan,
                                              Scenario scenario, Carriers carriers, int jspritIterations) {
		Map<Id<Link>, Link> garbageLinks = new HashMap<Id<Link>, Link>();
		carrierMap.clear();
        carriers.getCarriers().clear();
        CarriersUtils.setJspritIterations(carrierChessboard, jspritIterations);
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
     */
	static void createCarriersForChessboard(FleetSize fleetSize, CarrierVehicleTypes carrierVehicleTypes) {
		String vehicleName = "TruckChessboard";
		double earliestStartingTime = 6 * 3600;
		double latestFinishingTime = 14 * 3600;

		VehicleType vehicleType = carrierVehicleTypes.getVehicleTypes().values().iterator().next();
		CarrierVehicle newCarrierVehicle =  CarrierVehicle.Builder
			.newInstance(Id.create(vehicleName, Vehicle.class), Id.createLinkId(linkChessboardDepot), vehicleType)
			.setEarliestStart(earliestStartingTime).setLatestEnd(latestFinishingTime).build();

//		AbfallUtils.createGarbageTruck(vehicleName, linkChessboardDepot, earliestStartingTime, latestFinishingTime);

		// define Carriers

		defineCarriersChessboard(newCarrierVehicle, fleetSize);
	}

	/**
	 * Defines and sets the Capabilities of the Carrier, including the vehicleTypes
	 * for the carriers for the Chessboard network
	 */
	private static void defineCarriersChessboard(CarrierVehicle vehicleDepot, FleetSize fleetSize) {
		CarrierCapabilities carrierCapabilities = CarrierCapabilities.Builder.newInstance()
				.addVehicle(vehicleDepot).setFleetSize(fleetSize).build();

		carrierChessboard.setCarrierCapabilities(carrierCapabilities);
	}
}
