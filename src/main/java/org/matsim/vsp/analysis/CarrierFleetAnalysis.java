package org.matsim.vsp.analysis;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.freight.carriers.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Class to analyze the possible fleet of a carrier and checks if the vehicles are used for tours.
 */
public class CarrierFleetAnalysis {
    public static void main(String[] args) throws IOException {

        String carriersFile = "../matsim-metropole-ruhr/scenarios/metropole-ruhr-v2024.0/output/rvr/commercial_100pct/smallScaleCommercial/metropole-ruhr-v2024.0-3pct.output_carriers_withPlans.xml"; // Specify the path to your carriers file
        String vehicleTypesFile = "../matsim-metropole-ruhr/scenarios/metropole-ruhr-v2024.0/output/rvr/commercial_100pct/smallScaleCommercial/metropole-ruhr-v2024.0-3pct.output_carriersVehicleTypes.xml.gz"; // Specify the path to your vehicle types file

        Config config = ConfigUtils.createConfig();
        FreightCarriersConfigGroup freightCarriersConfigGroup = ConfigUtils.addOrGetModule(config, FreightCarriersConfigGroup.class);
        freightCarriersConfigGroup.setCarriersFile(carriersFile);
        freightCarriersConfigGroup.setCarriersVehicleTypesFile(vehicleTypesFile);

        Scenario scenario = ScenarioUtils.loadScenario(config);
        CarriersUtils.loadCarriersAccordingToFreightConfig(scenario);

        writeAnalysisResults(scenario, carriersFile);
    }

    private static void writeAnalysisResults(Scenario scenario, String carriersFile) throws IOException {

        Carriers carriers = CarriersUtils.getCarriers(scenario);

        try (BufferedWriter writer = Files.newBufferedWriter(
                Path.of(carriersFile).getParent().resolve("analysis").resolve("freight").resolve("carrierFleetAnalysis.csv"))) {
            writer.write("vehicle;");
            writer.write("carrier;");
            writer.write("vehicleType;");
            writer.write("maxTourDuration;");
            writer.write("usedForTour");

            writer.newLine();
            for (Carrier carrier : carriers.getCarriers().values()) {
                carrier.getCarrierCapabilities().getCarrierVehicles().values().forEach(vehicle -> {
                    try {
                        writer.write(vehicle.getId().toString() + ";");
                        writer.write(carrier.getId() + ";");
                        writer.write(vehicle.getType().getId().toString() + ";");
                        writer.write((vehicle.getLatestEndTime() - vehicle.getEarliestStartTime()) + ";");
                        boolean isUsedForTour = false;
                        for (ScheduledTour tour : carrier.getSelectedPlan().getScheduledTours())
                            if (tour.getVehicle().getId().equals(vehicle.getId())) {
                                isUsedForTour = true;
                                break;
                            }
                        writer.write(String.valueOf(isUsedForTour));
                        writer.newLine();
                    } catch (IOException e) {
                        throw new RuntimeException("Error writing to file: " + e.getMessage(), e);
                    }
                });
            }
        }
    }
}
