package org.matsim.vsp.DistanceConstraint;

import java.util.Collection;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.CarrierUtils;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.carrier.Tour;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.contrib.freight.jsprit.MatsimJspritFactory;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts;
import org.matsim.contrib.freight.jsprit.NetworkRouter;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts.Builder;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.vehicles.EngineInformation.FuelType;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.SchrimpfFactory;
import com.graphhopper.jsprit.core.algorithm.state.StateId;
import com.graphhopper.jsprit.core.algorithm.state.StateManager;
import com.graphhopper.jsprit.core.algorithm.state.StateUpdater;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.constraint.HardActivityConstraint;
import com.graphhopper.jsprit.core.problem.misc.JobInsertionContext;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.solution.route.activity.ActivityVisitor;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;
import com.graphhopper.jsprit.core.util.Solutions;
import com.graphhopper.jsprit.core.util.VehicleRoutingTransportCostsMatrix;

/**
 * @author rewert
 * 
 *         Includes all classes and methods for the distance constraint of every
 *         electric vehicle based on the capacity and the consumption of the
 *         battery. The base for calculating the consumption is only the driven
 *         distance and not the transported weight or other influences.
 * 
 *         !! No recharging is integrated. Vehicles are totally loaded at the
 *         beginning.
 *
 */
class DistanceConstraintUtils {
	static final Logger log = Logger.getLogger(DistanceConstraintUtils.class);

	/**
	 * Creates a VehicleRoutingCostMatrix for calculating the distance between all
	 * different locations of a carrier. Matrix has informations about the distance
	 * and the travelTime between every location. This is necessary for the
	 * distanceConstraint for the electric vehicles, because in the normal
	 * netBasedCosts only costs and travelTimes are calculated. Therefore for every
	 * from/to pair a jsprit searches the a good route (depends on the number of
	 * iterations for this little jsprit problem with one simple service). Perhaps a
	 * better solution for creating this matrix is possible.
	 * 
	 * @param vrpBuilder
	 * @param singleCarrier
	 * @param network
	 * @param netBuilder
	 * @return
	 */

	static VehicleRoutingTransportCostsMatrix createMatrix(VehicleRoutingProblem.Builder vrpBuilder,
			Carrier singleCarrier, Network network, Builder netBuilder) {

		double distance = 0;
		int startTime = 10000000;
		int endTime = 0;
		int duration = 0;

		VehicleRoutingTransportCostsMatrix.Builder distanceMatrixBuilder = VehicleRoutingTransportCostsMatrix.Builder
				.newInstance(false);

		final NetworkBasedTransportCosts netBasedCostsMatrix = netBuilder.build();

		for (String from : vrpBuilder.getLocationMap().keySet()) {
			for (String to : vrpBuilder.getLocationMap().keySet()) {

				if (from.equals(to)) {
					distanceMatrixBuilder.addTransportDistance(from, to, 0);
					distanceMatrixBuilder.addTransportTime(from, to, (0));

				} else {

					Carrier oneShipmentCarrier = CarrierUtils.createCarrier(Id.create("OneShipment", Carrier.class));
					for (VehicleType vehicleType : singleCarrier.getCarrierCapabilities().getVehicleTypes()) {

						CarrierVehicle testVehicle = CarrierVehicle.Builder
								.newInstance(Id.create("testVehicle", Vehicle.class), Id.createLinkId(from))
								.setEarliestStart(0).setLatestEnd(72 * 3600).setType(vehicleType)
								.setTypeId(vehicleType.getId()).build();
						oneShipmentCarrier.getCarrierCapabilities().getVehicleTypes().add(vehicleType);
						CarrierUtils.addCarrierVehicle(oneShipmentCarrier, testVehicle);
						oneShipmentCarrier.getCarrierCapabilities().setFleetSize(FleetSize.FINITE);
						break;
					}

					CarrierService testService = CarrierService.Builder
							.newInstance(Id.create("singleService", CarrierService.class), Id.createLinkId(to))
							.setServiceDuration(0).build();
					CarrierUtils.addService(oneShipmentCarrier, testService);

					VehicleRoutingProblem.Builder vrpBuilder2 = MatsimJspritFactory
							.createRoutingProblemBuilder(oneShipmentCarrier, network);
					vrpBuilder2.setRoutingCost(netBasedCostsMatrix);
					VehicleRoutingProblem problem = vrpBuilder2.build();
					VehicleRoutingAlgorithm algorithm = new SchrimpfFactory().createAlgorithm(problem);
					algorithm.setMaxIterations(25);
					Collection<VehicleRoutingProblemSolution> solutions = algorithm.searchSolutions();
					VehicleRoutingProblemSolution bestSolution = Solutions.bestOf(solutions);
					CarrierPlan carrierPlanServices = MatsimJspritFactory.createPlan(oneShipmentCarrier, bestSolution);
					NetworkRouter.routePlan(carrierPlanServices, netBasedCostsMatrix);
					oneShipmentCarrier.setSelectedPlan(carrierPlanServices);

					for (ScheduledTour tour : carrierPlanServices.getScheduledTours()) {
						distance = 0.0;

						for (Tour.TourElement element : tour.getTour().getTourElements()) {
							if (element instanceof Tour.Leg) {
								Tour.Leg legElement = (Tour.Leg) element;
								if (legElement.getRoute().getDistance() != 0 && legElement.getRoute() != null)
									distance = distance + RouteUtils.calcDistance((NetworkRoute) legElement.getRoute(),
											0, 0, network);

								if (startTime > legElement.getExpectedDepartureTime())
									startTime = (int) legElement.getExpectedDepartureTime();
								if (endTime < (legElement.getExpectedDepartureTime()
										+ legElement.getExpectedTransportTime()))
									endTime = (int) (legElement.getExpectedDepartureTime()
											+ legElement.getExpectedTransportTime());
							}
							if (element instanceof Tour.ServiceActivity) {
								break;
							}
						}
						duration = endTime - startTime;

						distanceMatrixBuilder.addTransportDistance(from, to, distance);
						distanceMatrixBuilder.addTransportTime(from, to, (duration));
					}
				}

			}
		}

		return distanceMatrixBuilder.build();
	}

}

/**
 * Creates the distance constrain
 *
 */
class DistanceConstraint implements HardActivityConstraint {

	private final StateManager stateManager;

	private final VehicleRoutingTransportCostsMatrix costsMatrix;

	private final StateId distanceStateId;

	private final CarrierVehicleTypes vehicleTypes;

	DistanceConstraint(StateId distanceStateId, StateManager stateManager,
			VehicleRoutingTransportCostsMatrix transportCosts,
			CarrierVehicleTypes vehicleTypes) {
		this.costsMatrix = transportCosts;
		this.stateManager = stateManager;
		this.distanceStateId = distanceStateId;
		this.vehicleTypes = vehicleTypes;
	}

	/**
	 * When adding a TourActivity to the tour or changing i.e. changing the vehicle
	 * of the tour, in the algorithm always the fulfilled method checks if all
	 * conditions (constraints) are fulfilled or not. This method always checks the
	 * distance constraint. The constraint is only important for electric drive
	 * vehicles. Thats why the first steps checks if the used or the new vehicle is
	 * electric. When minimum one of them is electric the constraint will be
	 * checked. Because every activity is added separately and the pickup before the
	 * delivery of a shipment, it will be investigated which additional distance is
	 * necessary, when a pickup at the depot is added, although the additional
	 * distance of the pickup is null. Therefore the minimal additional distance of
	 * the associated Delivery is also important for the fulfilled decision of this
	 * function. At the end the conditions checks if the electric consumption of the
	 * tour including the additional shipment is possible with the used capacity of
	 * the battery.
	 */

	@Override
	public ConstraintsStatus fulfilled(JobInsertionContext context, TourActivity prevAct, TourActivity newAct,
			TourActivity nextAct, double v) {
		double additionalDistance;
		// TODO this method is created with the only delivery shipments from a depot.
		// Perhaps some conditions have to be changed when pickups in different
		// locations are possible
		// TODO also for services
		VehicleType vehicleTypeOfNewVehicle = vehicleTypes.getVehicleTypes()
				.get(Id.create(context.getNewVehicle().getType().getTypeId().toString(), VehicleType.class));

		if (vehicleTypeOfNewVehicle.getEngineInformation().getAttributes()
				.getAttribute("fuelType") == (FuelType.electricity)) {

			Double electricityCapacityinkWh = (Double) vehicleTypeOfNewVehicle.getEngineInformation().getAttributes()
					.getAttribute("engeryCapacity");
			Double electricityConsumptionPerkm = (Double) vehicleTypeOfNewVehicle.getEngineInformation().getAttributes()
					.getAttribute("engeryConsumptionPerKm");
			Double routeConsumption = null;

			Double routeDistance = stateManager.getRouteState(context.getRoute(), distanceStateId, Double.class);

			if (routeDistance == null) {
				routeDistance = 0.;
				routeConsumption = 0.;
			} else {
				routeConsumption = routeDistance * (electricityConsumptionPerkm / 1000);
			}
			if (newAct.getName().contains("pickupShipment")) {
				additionalDistance = getDistance(prevAct, newAct) + getDistance(newAct, nextAct)
						- getDistance(prevAct, nextAct) + findMinimalAdditionalDistance(context, newAct, nextAct);
			} else {
				additionalDistance = getDistance(prevAct, newAct) + getDistance(newAct, nextAct)
						- getDistance(prevAct, nextAct);

			}
			double additionalConsumption = additionalDistance * (electricityConsumptionPerkm / 1000);
			double newRouteConsumption = routeConsumption + additionalConsumption;

			if (newRouteConsumption > electricityCapacityinkWh) {
				return ConstraintsStatus.NOT_FULFILLED;
			} else
				return ConstraintsStatus.FULFILLED;
		} else {
			return ConstraintsStatus.FULFILLED;
		}
	}

	/**
	 * Finds a minimal additional distance for the tour, when a pickup is added to
	 * the plan. The AssociatedActivities contains both activities of a job which
	 * should be added to the existing tour. The TourActivities which are already in
	 * the tour are found in context.getRoute().getTourActivities. In this method
	 * the position of the new pickup is fixed and three options of the location of
	 * the delivery activity will be checked: delivery between every other activity
	 * after the pickup, delivery as the last activity before the end and delivery
	 * directly behind the new pickup. This method gives back the minimal distance
	 * of this three options.
	 * 
	 * @param context
	 * @param newAct
	 * @param nextAct
	 * @return minimal distance of the associated delivery
	 */
	private double findMinimalAdditionalDistance(JobInsertionContext context, TourActivity newAct,
			TourActivity nextAct) {
		double minimalAdditionalDistance = 0;

		if (context.getAssociatedActivities().get(1).getName().contains("deliverShipment")) {
			TourActivity assignedDelivery = context.getAssociatedActivities().get(1);
			minimalAdditionalDistance = 0;
			int indexNextActicity = nextAct.getIndex();
			int index = 0;
			int countIndex = 0;

			// search the index of the activity behind the pickup activity which should be
			// added to the tour
			for (TourActivity tourActivity : context.getRoute().getTourActivities().getActivities()) {
				if (tourActivity.getIndex() == indexNextActicity) {
					while (countIndex < context.getRoute().getTourActivities().getActivities().size()) {
						if (context.getRoute().getTourActivities().getActivities().get(countIndex)
								.getIndex() == indexNextActicity) {
							index = countIndex;
							break;
						}
					}
				}
				break;
			}

			// search the minimal distance between every exiting TourAcitivity
			while ((index + 1) < context.getRoute().getTourActivities().getActivities().size()) {
				TourActivity activityBefore = context.getRoute().getTourActivities().getActivities().get(index);
				TourActivity activityAfter = context.getRoute().getTourActivities().getActivities().get(index + 1);
				double possibleAdditionalDistance = getDistance(activityBefore, assignedDelivery)
						+ getDistance(assignedDelivery, activityAfter) - getDistance(activityBefore, activityAfter);
				minimalAdditionalDistance = findMinimalDistance(minimalAdditionalDistance, possibleAdditionalDistance);
				index++;
			}
			// checks the distance if the delivery is the last activity before the end of
			// the tour
			if (context.getRoute().getTourActivities().getActivities().size() > 0) {
				TourActivity activityLastDelivery = context.getRoute().getTourActivities().getActivities()
						.get(context.getRoute().getTourActivities().getActivities().size() - 1);
				TourActivity activityEnd = context.getRoute().getEnd();
				double possibleAdditionalDistance = getDistance(activityLastDelivery, assignedDelivery)
						+ getDistance(assignedDelivery, activityEnd) - getDistance(activityLastDelivery, activityEnd);
				minimalAdditionalDistance = findMinimalDistance(minimalAdditionalDistance, possibleAdditionalDistance);
				// Checks the distance if the delivery will added directly behind the pickup
				TourActivity newPickupActivity = newAct;
				TourActivity activityAfter = context.getRoute().getTourActivities().getActivities().get(index);
				possibleAdditionalDistance = getDistance(newPickupActivity, assignedDelivery)
						+ getDistance(assignedDelivery, activityAfter) - getDistance(newPickupActivity, activityAfter);
				minimalAdditionalDistance = findMinimalDistance(minimalAdditionalDistance, possibleAdditionalDistance);
			}

		}
		return minimalAdditionalDistance;
	}

	/**
	 * Checks if the find possible distance is the minimal one.
	 * 
	 * @param minimalAdditionalDistance
	 * @param possibleAdditionalDistance
	 * @return
	 */
	private double findMinimalDistance(double minimalAdditionalDistance, double possibleAdditionalDistance) {
		if (minimalAdditionalDistance == 0)
			minimalAdditionalDistance = possibleAdditionalDistance;
		else if (possibleAdditionalDistance < minimalAdditionalDistance)
			minimalAdditionalDistance = possibleAdditionalDistance;
		return minimalAdditionalDistance;
	}

	private double getDistance(TourActivity from, TourActivity to) {
		return costsMatrix.getDistance(from.getLocation().getId(), to.getLocation().getId());
	}
}

/**
 * Given class for working with the a distance constraint
 *
 */
class DistanceUpdater implements StateUpdater, ActivityVisitor {

	private final StateManager stateManager;

	private final VehicleRoutingTransportCostsMatrix costMatrix;

	private final StateId distanceStateId;

	private VehicleRoute vehicleRoute;

	private double distance = 0.;

	private TourActivity prevAct;

	public DistanceUpdater(StateId distanceStateId, StateManager stateManager,
			VehicleRoutingTransportCostsMatrix transportCosts) {
		this.costMatrix = transportCosts;
		this.stateManager = stateManager;
		this.distanceStateId = distanceStateId;
	}

	@Override
	public void begin(VehicleRoute vehicleRoute) {
		distance = 0.;
		prevAct = vehicleRoute.getStart();
		this.vehicleRoute = vehicleRoute;
	}

	@Override
	public void visit(TourActivity tourActivity) {
		distance += getDistance(prevAct, tourActivity);
		prevAct = tourActivity;
	}

	@Override
	public void finish() {
		distance += getDistance(prevAct, vehicleRoute.getEnd());
		stateManager.putRouteState(vehicleRoute, distanceStateId, distance);
	}

	double getDistance(TourActivity from, TourActivity to) {
		return costMatrix.getDistance(from.getLocation().getId(), to.getLocation().getId());
	}
}