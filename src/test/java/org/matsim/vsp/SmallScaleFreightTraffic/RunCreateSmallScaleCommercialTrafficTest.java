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

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.freight.FreightConfigGroup;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.utils.FreightUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author Ricardo
 *
 */
public class RunCreateSmallScaleCommercialTrafficTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testMainRunAndResults() throws IOException {
		String inputDataDirectory = utils.getPackageInputDirectory() + "scenarios/testScenario";
		String output = utils.getOutputDirectory();
		String networkLocation = "https://raw.githubusercontent.com/matsim-org/matsim-libs/master/examples/scenarios/freight-chessboard-9x9/grid9x9.xml";
		String networkCRS = "EPSG:4326";
		String sample = "0.1";
		String jspritIterations = "2";
		String creationOption = "createNewCarrierFile";
		String landuseConfiguration = "useExistingDataDistribution";
		String trafficType = "bothTypes";
		String includeExistingModels = "true";
		String zoneShapeFileName = "testZones.shp";
		String buildingsShapeFileName = "testBuildings.shp";
		String landuseShapeFileName = "testLanduse.shp";

		try {
			new CreateSmallScaleCommercialTrafficDemand().execute(inputDataDirectory, "--network", networkLocation,
					"--networkCRS", networkCRS, "--sample", sample, "--output", output, "--jspritIterations",
					jspritIterations, "--creationOption", creationOption, "--landuseConfiguration",
					landuseConfiguration, "--trafficType", trafficType, "--includeExistingModels",
					includeExistingModels, "--zoneShapeFileName", zoneShapeFileName, "--buildingsShapeFileName",
					buildingsShapeFileName, "--landuseShapeFileName", landuseShapeFileName);
		} catch (Exception ee) {
			LogManager.getLogger(this.getClass()).fatal("there was an exception: \n" + ee);
			// if one catches an exception, then one needs to explicitly fail the test:
			Assert.fail();
		}

		// test results of complete run before
		Config config = ConfigUtils.createConfig();
		Scenario scenarioWOSolution = ScenarioUtils.createScenario(config);
		Scenario scenarioWSolution = ScenarioUtils.createScenario(config);
		File outputFolder = new File(output).listFiles()[0];
		Population population = null;
		String carriersWOSolutionFileLocation = null;
		String carriersWSolutionFileLocation = null;
		FreightConfigGroup freightConfigGroup = ConfigUtils.addOrGetModule(config, FreightConfigGroup.class);

		for (File outputFiles : Objects.requireNonNull(outputFolder.listFiles())) {

			if (outputFiles.getName().contains("pct_plans.xml.gz"))
				population = PopulationUtils.readPopulation(outputFiles.getPath());
			if (outputFiles.getName().equals("output_CarrierDemand.xml"))
				carriersWOSolutionFileLocation = outputFiles.getPath();
			if (outputFiles.getName().equals("output_CarrierDemandWithPlans.xml"))
				carriersWSolutionFileLocation = outputFiles.getPath();
			if (outputFiles.getName().equals("output_carriersVehicleTypes.xml.gz"))
				freightConfigGroup.setCarriersVehicleTypesFile(outputFiles.getPath());
		}

		freightConfigGroup.setCarriersFile(carriersWOSolutionFileLocation);
		FreightUtils.loadCarriersAccordingToFreightConfig(scenarioWOSolution);
		freightConfigGroup.setCarriersFile(carriersWSolutionFileLocation);
		FreightUtils.loadCarriersAccordingToFreightConfig(scenarioWSolution);

		for (Person person : population.getPersons().values()) {
			Assert.assertTrue(person.getSelectedPlan() != null);
			Assert.assertTrue(person.getAttributes().getAsMap().containsKey("tourStartArea"));
			Assert.assertTrue(person.getAttributes().getAsMap().containsKey("vehicles"));
			Assert.assertTrue(person.getAttributes().getAsMap().containsKey("subpopulation"));
			Assert.assertTrue(person.getAttributes().getAsMap().containsKey("purpose"));
		}

		Assert.assertEquals(FreightUtils.addOrGetCarriers(scenarioWSolution).getCarriers().size(),
				FreightUtils.addOrGetCarriers(scenarioWOSolution).getCarriers().size(), 0);
		int countedTours = 0;
		for (Carrier carrier_withSolution : FreightUtils.addOrGetCarriers(scenarioWSolution).getCarriers().values()) {
			countedTours += carrier_withSolution.getSelectedPlan().getScheduledTours().size();
		}
		Assert.assertEquals(population.getPersons().size(), countedTours, 0);
		int a = 8;
	}
}