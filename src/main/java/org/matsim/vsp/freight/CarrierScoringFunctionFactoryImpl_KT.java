package org.matsim.vsp.freight;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.Tour.ServiceActivity;
import org.matsim.freight.carriers.Tour.TourElement;
import org.matsim.freight.carriers.controller.CarrierScoringFunctionFactory;
import org.matsim.freight.carriers.controller.FreightActivity;
import org.matsim.freight.carriers.jsprit.VehicleTypeDependentRoadPricingCalculator;
import org.matsim.vehicles.Vehicle;

/**
 * Defines carrier scoring function (factory).
 *
 * @author Kt, based on stefan
 * from sschroeder: package org.matsim.contrib.freight.usecases.chessboard ;
 * 
 */
public class CarrierScoringFunctionFactoryImpl_KT implements CarrierScoringFunctionFactory {
	
	@Override
	public ScoringFunction createScoringFunction(Carrier carrier) {
		return null;
	}

	private static Scenario scenario;
	private static String outputDir;
	
    public CarrierScoringFunctionFactoryImpl_KT(Scenario scenario, String outputDir) {
        super();
        CarrierScoringFunctionFactoryImpl_KT.scenario = scenario;
        if (outputDir.endsWith("/")){
        	CarrierScoringFunctionFactoryImpl_KT.outputDir = outputDir;
        } else {
        	CarrierScoringFunctionFactoryImpl_KT.outputDir = outputDir + "/";
        }
    }
    

    static class VehicleFixCostScoring implements SumScoringFunction.BasicScoring {
    	
    	private double score;
        private Carrier carrier;

        VehicleFixCostScoring(Carrier carrier) {
            super();
            this.carrier = carrier;
        }

        
        @Override
        public void finish() {	
        }

        @Override
        public double getScore() {
        	calcFixCosts();  //Geht so, da Fixkosten nur einmal auftreten und somit vor der finalen Abfrage des Scores berechnet werden können
			return score;
        }

		private void calcFixCosts() {
			CarrierPlan selectedPlan = carrier.getSelectedPlan();
        	if(selectedPlan != null) {
        		for(ScheduledTour tour : selectedPlan.getScheduledTours()){
        			if(!tour.getTour().getTourElements().isEmpty()){
        				double fixCosts= tour.getVehicle().getType().getCostInformation().getFixedCosts();
        				score += (-1)*fixCosts;
        			}
        		}
        	}
		}  
   } //End class  FixCosts

    static class LegScoring implements SumScoringFunction.LegScoring {
    	
    	private static Logger log = LogManager.getLogger(LegScoring.class);
    	
    	private double score = 0. ;
    	private Carrier carrier;

        LegScoring(Carrier carrier) {
             super();
             this.carrier = carrier;
         }
		
		private double getTimeParameter(CarrierVehicle vehicle) {
            return vehicle.getType().getCostInformation().getCostsPerSecond();
        }

        private double getDistanceParameter(CarrierVehicle vehicle) {
            return vehicle.getType().getCostInformation().getCostsPerMeter();
        }

        private CarrierVehicle getVehicle(Id<?> vehicleId) {
            for(CarrierVehicle cv : carrier.getCarrierCapabilities().getCarrierVehicles().values()){
                if(cv.getId().equals(vehicleId)){
                    return cv;
                }
            }
            return null;
        }

		@Override
		public void finish() {	
		}

		@Override
		public double getScore() {
			return this.score;
		}
		
		@Override
		public void handleLeg(Leg leg) {
			if(leg.getRoute() instanceof NetworkRoute){
                NetworkRoute nRoute = (NetworkRoute) leg.getRoute();
                Id<?> vehicleId = nRoute.getVehicleId();
                CarrierVehicle vehicle = getVehicle(vehicleId);
                if(vehicle == null) throw new IllegalStateException("vehicle with id " + vehicleId + " is missing");
                
                //Berechnung der TravelDistance (aus: org.matsim.population.algorithms.PersonPrepareForSim kopiert.) KT 08.01.15
				//Jedoch werden der Start- und Ziellink nicht mit einbezogen (weil nicht Teil der Route, sondern extra aufgeführt...!
                Double dist = null;
				if (leg.getRoute() instanceof NetworkRoute){
					dist = RouteUtils.calcDistanceExcludingStartEndLink((NetworkRoute) leg.getRoute(), scenario.getNetwork()); //Route selbst (ohne Start und Endlink
					dist += scenario.getNetwork().getLinks().get(leg.getRoute().getStartLinkId()).getLength();	//StartLink
					dist += scenario.getNetwork().getLinks().get(leg.getRoute().getEndLinkId()).getLength(); //EndLink
				}
				if (dist != null){
					leg.getRoute().setDistance(dist);
				}

                double distanceCosts = dist*getDistanceParameter(vehicle);
                assert distanceCosts >= 0.0 : "distanceCosts must be positive";
                score += (-1) * distanceCosts;

                double timeCosts = leg.getTravelTime().seconds() *getTimeParameter(vehicle);
                assert timeCosts >= 0.0 : "timeCosts must be positive";
                score += (-1) * timeCosts;
                
                leg.setMode(vehicle.getType().getId().toString());		//KT: 28.03.2015 Zuweisung des VehicleTxpes als Mode -> Sinnvoll? Zumindest besser als "car".
                
                } else {
				log.warn("Route not scored in LegScoring:" + leg.toString()) ;
			}
			
		}
    	
    } // End Class LegScoring
    
    /**
     * Bewertet die Aktivität und berücksichtigt dabei alle Wartezeiten.
     * Der Kostensatz ist einheitlich mit 0.008 EUR/s festgelegt. 
     * 
     * Es erfolgt KEINE Korrektur der Wartezeit für den ersten Service.
     * 
     * @author kt
     */
    static class ActivityScoring implements SumScoringFunction.ActivityScoring {

    	private static Logger log = LogManager.getLogger(ActivityScoring.class);
    	
		private double score = 0. ;
		private final double margUtlOfTime_s = 0.008 ;  //Wert aus Schröder/Liedtke 2014
    	
		private Carrier carrier;
		
		ActivityScoring(Carrier carrier) {
			super();
			this.carrier = carrier;
		}
    			
		@Override
		public void finish() {
		}

		@Override
		public double getScore() {
			return this.score;
		}

		@Override
		public void handleFirstActivity(Activity act) { 
		}

		@Override
		public void handleActivity(Activity activity) {
			double actCosts = 0;
			if (activity instanceof FreightActivity) {
				FreightActivity act = (FreightActivity) activity;
				
				actCosts = calcActCosts(act);		//costs for whole Activity, inkl waiting.
				
				score += (-1) * actCosts;
			} else {
				log.warn("Carrier activities which are not FreightActivities are not scored here: " + activity.toString()) ;
			}			
		}

		//Kosten für Zeit von Beginn bis Ende der Aktivität (enthält aktuell jun '15 auch Wartezeit bis Service beginnt)
		private double calcActCosts(FreightActivity act) {
				// deduct score for the time spent at the facility:
				final double actStartTime = act.getStartTime().seconds();
				final double actEndTime = act.getEndTime().seconds();
				return (actEndTime - actStartTime) * this.margUtlOfTime_s ;
		}

		@Override
		public void handleLastActivity(Activity act) {	
			handleActivity(act);
			// no penalty for everything that is after the last act (people don't work)		
		}
		
    }  //End Class ActivityScoring
    
    /**
     * 
     * Bestimmt die Kosten für die Aktivitäten des Carriers.
     * Der Kostensatz ist einheitlich mit 0.008 EUR/s festgelegt. 
     * 
     * Korrektur erfolgt für die Wartezeit vor dem ersten Service. Diese wird aus den Kosten rausgerechnet
     * Hintergrund: Fzg fahren in der Simulation mit Depotöffnung los und warten dann vor dem Service
     * auf dessen Öffnung. Bewertet man diese Zeit nun negativ mit, so kann das Ergebnis (Kosten) alleine durch 
     * eine veränderte Depotöffnugszeit stark verändert werden, ohne das tatsächlich eine Veränderung der 
     * Tour stattgefunden hat. 
     * Für den Fall, dass MAtSim mit mehrern Iterationen und der entsprechenden Strategy zum
     * verändern der Abfahrtszeiten ausgestattet wird - würde sich dieser Fehler hoffentlich selbst 
     * rausiterieren und die Korrektur sollte nicht angewendet werden.
     * 
     * @author kt (24.6.15)
     */
    static class ActivityScoringWithCorrection implements SumScoringFunction.ActivityScoring {

    	private static Logger log = LogManager.getLogger(ActivityScoring.class);
    	

		private double score = 0. ;
		private final double margUtlOfTime_s = 0.008 ;  //Wert aus Schröder/Liedtke 2014
    	
		private Carrier carrier;
		private List<ScheduledTour> correctedTours = new ArrayList<ScheduledTour>(); 
		
		ActivityScoringWithCorrection(Carrier carrier) {
			super();
			this.carrier = carrier;
		}
    	
		@Override
		public void finish() {
		}

		@Override
		public double getScore() {
			return this.score;
		}

		@Override
		public void handleFirstActivity(Activity act) {
		}

		@Override
		public void handleActivity(Activity activity) {
			double actCosts = 0;
			if (activity instanceof FreightActivity) {
				FreightActivity act = (FreightActivity) activity;
				
				actCosts = calcActCosts(act);		//costs for whole Activity, inkl waiting.
				
				//Identify the first serviceActivity on tour and correct costs 
				boolean isfirstAct = isFirstServiceAct(act);
				if (isfirstAct){
					actCosts -= correctFirstService(act);  //Ziehe die zuviel berechneten Kosten ab. 
				}
				
				score += (-1) * actCosts;
			} else {
				log.warn("Carrier activities which are not FreightActivities are not scored here: " + activity.toString()) ;
			}			
		}
		
		@Override
		public void handleLastActivity(Activity act) {	
		}

		//Costs für Zeit von Begin bis Ende der Aktivität (enthält aktuell jun '15 auch Wartezeit bis Service beginnt)
		private double calcActCosts(FreightActivity act) {
				// deduct score for the time spent at the facility:
				final double actStartTime = act.getStartTime().seconds();
				final double actEndTime = act.getEndTime().seconds();
				return (actEndTime - actStartTime) * this.margUtlOfTime_s ;
		}
		
		//Aussage erfolgt über derzeit über  Location, Zeitfenster und Act-Type.
		private boolean isFirstServiceAct(FreightActivity act) {
			boolean isfirstAct = false;
			
			for (ScheduledTour tour : carrier.getSelectedPlan().getScheduledTours() ){
				if (!correctedTours.contains(tour)){
					for (TourElement te : tour.getTour().getTourElements()) {
						if (te instanceof  ServiceActivity){
							ServiceActivity sa = (ServiceActivity) te;
							if (sa.getLocation() == act.getLinkId()
									&& sa.getTimeWindow() == act.getTimeWindow()
									&& sa.getActivityType() == act.getType()){
								isfirstAct = true;
								correctedTours.add(tour);
							}
						}
					}
				}
			}
			return isfirstAct;
		}

		//Korrigiert den Score bei der ersten Service-Aktivität (Wartezeit, da bereits zu Beginn der Depotöffnung losgefahren)
		//indem diese Zeit wieder mit einem positiven Wert gegengerechnet wird
		private double correctFirstService(FreightActivity act){
			final double actStartTime = act.getStartTime().seconds();
			final double windowStartTime = act.getTimeWindow().getStart();
				if ( actStartTime < windowStartTime ) {	//Fahrzeug vor Öffnungszeit angekommen.
					return ( windowStartTime - actStartTime ) * this.margUtlOfTime_s ;
				}
				else {
					return 0.0;
			} 
		}



    }  //End Class ActivityScoring
    
  //TollScoring von Stefan Schroeder {@see org.matsim.contrib.freight.usecases.chessboard.CarrierScoringFunctionFactoryImp.TollScoring}
    static class TollScoring implements SumScoringFunction.BasicScoring, SumScoringFunction.ArbitraryEventScoring {

        private double score = 0.;
        private Carrier carrier;
        private Network network;

        private VehicleTypeDependentRoadPricingCalculator roadPricing;
        
        public TollScoring(Carrier carrier, Network network, VehicleTypeDependentRoadPricingCalculator roadPricing) {
            this.carrier = carrier;
            this.roadPricing = roadPricing;
            this.network = network;
        }

        @Override
        public void handleEvent(Event event) {
            if(event instanceof LinkEnterEvent){
                CarrierVehicle carrierVehicle = getVehicle(((LinkEnterEvent) event).getVehicleId());
                if(carrierVehicle == null) throw new IllegalStateException("carrier vehicle missing");
                double toll = roadPricing.getTollAmount(carrierVehicle.getType().getId(),network.getLinks().get(((LinkEnterEvent) event).getLinkId()),event.getTime());
                if(toll > 0.) System.out.println("bing: vehicle " + carrierVehicle.getId() + " paid toll " + toll + "");

                score += (-1) * toll;
            }
        }

        private CarrierVehicle getVehicle(Id<Vehicle> vehicleId) {
            for(CarrierVehicle v : carrier.getCarrierCapabilities().getCarrierVehicles().values()){
                if(v.getId().equals(vehicleId)){
                    return v;
                }
            }
            return null;
        }

        @Override
        public void finish() {
        }

        @Override
        public double getScore() {
            return score;
        }
    }
    

}
