/*
  * *********************************************************************** *
  * project: org.matsim.*
  * *********************************************************************** *
  *                                                                         *
  * copyright       : (C) 2020 by the members listed in the COPYING,        *
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
  * *********************************************************************** *
 */

package org.matsim.vsp.emissions;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup.NonScenarioVehicles;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author ikaddoura
 * Copied from matsim-berlin 5.5.x
 */

class RunOfflineAirPollutionAnalysis {
    private final static String runDirectory = "../outputKMT/zerocuts/BerlinFood/I_Base_allVehicles/output/";
    private final static String runId = "";

    private final static String hbefaFileCold = "../shared-svn/projects/matsim-germany/hbefa/hbefa-files/v3.2/EFA_ColdStart_vehcat_2005average.txt";
    private final static String hbefaFileWarm = "../shared-svn/projects/matsim-germany/hbefa/hbefa-files/v3.2/EFA_HOT_vehcat_2005average.txt";

    public static void main(String[] args) {

        String rootDirectory = null;

        if (args.length == 1) {
            rootDirectory = args[0];
        } else {
            throw new RuntimeException("Please set the root directory. Aborting...");
        }

        if (!rootDirectory.endsWith("/")) rootDirectory = rootDirectory + "/";

        Config config = ConfigUtils.loadConfig(rootDirectory + runDirectory + runId + "output_config.xml");
        config.vehicles().setVehiclesFile(rootDirectory + runDirectory + runId + "output_allVehicles.xml.gz");         //Note: All Vehicles is necessary
        config.network().setInputFile(rootDirectory + runDirectory + runId + "output_network.xml.gz");
        config.network().setChangeEventsInputFile(null);      //not needed here
        config.plans().setInputFile(null);

        EmissionsConfigGroup emissionsConfigGroup = ConfigUtils.addOrGetModule(config, EmissionsConfigGroup.class);
        emissionsConfigGroup.setAverageColdEmissionFactorsFile(rootDirectory + hbefaFileCold);
        emissionsConfigGroup.setAverageWarmEmissionFactorsFile(rootDirectory + hbefaFileWarm);
        emissionsConfigGroup.setNonScenarioVehicles(NonScenarioVehicles.ignore);

        final String emissionEventOutputFile = rootDirectory + runDirectory + runId + "emission.events.offline.xml.gz";
        final String eventsFile = rootDirectory + runDirectory + runId + "output_events.xml.gz";

        Scenario scenario = ScenarioUtils.loadScenario(config);

        // network preparation
        addHbefaRoadAttributeToNetwork(scenario.getNetwork());

        // This we should not need, since it should be as attributes in the vehicles file
//        Id<VehicleType> carVehicleTypeId = Id.create("car", VehicleType.class);
//        Id<VehicleType> freightVehicleTypeId = Id.create("freight", VehicleType.class);
//
//        // vehicles
//        scenario.getVehicles().getVehicleTypes().get(carVehicleTypeId).setDescription("BEGIN_EMISSIONSPASSENGER_CAR;average;average;averageEND_EMISSIONS");
//        scenario.getVehicles().getVehicleTypes().get(freightVehicleTypeId).setDescription("BEGIN_EMISSIONSHEAVY_GOODS_VEHICLE;average;average;averageEND_EMISSIONS");

        // the following is copy paste from the example...

        EventsManager eventsManager = EventsUtils.createEventsManager();

        AbstractModule module = new AbstractModule(){
            @Override
            public void install(){
                bind( Scenario.class ).toInstance( scenario );
                bind( EventsManager.class ).toInstance( eventsManager );
                bind( EmissionModule.class ) ;
            }
        };

        com.google.inject.Injector injector = Injector.createInjector(config, module);

        EmissionModule emissionModule = injector.getInstance(EmissionModule.class);

        EventWriterXML emissionEventWriter = new EventWriterXML(emissionEventOutputFile);
        emissionModule.getEmissionEventsManager().addHandler(emissionEventWriter);

        MatsimEventsReader matsimEventsReader = new MatsimEventsReader(eventsManager);
        matsimEventsReader.readFile(eventsFile);

        emissionEventWriter.closeFile();
    }

    private static void addHbefaRoadAttributeToNetwork(Network network) {
        for (Link link : network.getLinks().values()) {

            double freespeed = Double.NaN;

            if (link.getFreespeed() <= 13.888889) {
                freespeed = link.getFreespeed() * 2;
                // for non motorway roads, the free speed level was reduced
            } else {
                freespeed = link.getFreespeed();
                // for motorways, the original speed levels seems ok.
            }

            if(freespeed <= 8.333333333){ //30kmh
                link.getAttributes().putAttribute("hbefa_road_type", "URB/Access/30");
            } else if(freespeed <= 11.111111111){ //40kmh
                link.getAttributes().putAttribute("hbefa_road_type", "URB/Access/40");
            } else if(freespeed <= 13.888888889){ //50kmh
                double lanes = link.getNumberOfLanes();
                if(lanes <= 1.0){
                    link.getAttributes().putAttribute("hbefa_road_type", "URB/Local/50");
                } else if(lanes <= 2.0){
                    link.getAttributes().putAttribute("hbefa_road_type", "URB/Distr/50");
                } else if(lanes > 2.0){
                    link.getAttributes().putAttribute("hbefa_road_type", "URB/Trunk-City/50");
                } else{
                    throw new RuntimeException("NoOfLanes not properly defined");
                }
            } else if(freespeed <= 16.666666667){ //60kmh
                double lanes = link.getNumberOfLanes();
                if(lanes <= 1.0){
                    link.getAttributes().putAttribute("hbefa_road_type", "URB/Local/60");
                } else if(lanes <= 2.0){
                    link.getAttributes().putAttribute("hbefa_road_type", "URB/Trunk-City/60");
                } else if(lanes > 2.0){
                    link.getAttributes().putAttribute("hbefa_road_type", "URB/MW-City/60");
                } else{
                    throw new RuntimeException("NoOfLanes not properly defined");
                }
            } else if(freespeed <= 19.444444444){ //70kmh
                link.getAttributes().putAttribute("hbefa_road_type", "URB/MW-City/70");
            } else if(freespeed <= 22.222222222){ //80kmh
                link.getAttributes().putAttribute("hbefa_road_type", "URB/MW-Nat./80");
            } else if(freespeed > 22.222222222){ //faster
                link.getAttributes().putAttribute("hbefa_road_type", "RUR/MW/>130");
            } else{
                throw new RuntimeException("Link not considered..." + link.getId());
            }
        }
    }

}
