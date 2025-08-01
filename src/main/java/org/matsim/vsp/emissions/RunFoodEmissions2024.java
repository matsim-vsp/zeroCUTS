package org.matsim.vsp.emissions;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.HbefaVehicleCategory;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.contrib.emissions.analysis.EmissionsOnLinkEventHandler;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup.DetailedVsAverageLookupBehavior;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup.HbefaTableConsistencyCheckingLevel;
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
import org.matsim.vehicles.EngineInformation;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

/**
 * @author Kai Martins-Turner (kturner)
 */
public class RunFoodEmissions2024 {

  private static final Logger log = LogManager.getLogger(RunFoodEmissions2024.class);
  private static final String HGV_STRING = HbefaVehicleCategory.HEAVY_GOODS_VEHICLE.toString();
  private static final String DIESEL = "diesel";
  private static final String ELECTRICITY = "electricity";
  private static final String AVERAGE = "average";

  private final String runDirectory;

  private final String analysisOutputDirectory;

  private enum EtruckDefinition{ownVehicleType, allVehiclesAreElectric}
  private static EtruckDefinition etruckDefinition;

  public static void main(String[] args) throws IOException {


    //    // Update EFoods2020 -- ICEV-Case:
//    final String pathToRunDir = "/Users/kturner/git-and-svn/shared-svn/projects/freight/studies/UpdateEventsfromEarlierStudies/Food_ETrucks/";
//    var listOfRuns = List.of(
//        "Base_NwCE_BVWP_Pickup_10000it/"
//    )   ;
//    etruckDefinition= EtruckDefinition.ownVehicleType; //For CaseA and B only

    //    // Update EFoods2020 -- BEV-Cases:
//    final String pathToRunDir = "/Users/kturner/git-and-svn/shared-svn/projects/freight/studies/UpdateEventsfromEarlierStudies/Food_ETrucks/";
//    var listOfRuns = List.of(
//        "CaseA_E160_NwCE_BVWP_Pickup_10000it/",
//        "CaseB_E100_NwCE_BVWP_Pickup_10000it/"
//    )   ;
//    etruckDefinition= EtruckDefinition.allVehiclesAreElectric; //For CaseA and B only

//    final String pathToRunDir = "/Users/kturner/git-and-svn/shared-svn/projects/freight/studies/UpdateEventsfromEarlierStudies/";
//    var listOfRuns = List.of(
//        "foodRetailing_wo_rangeConstraint/71_ICEVBEV_NwCE_BVWP_10000it_DCoff_noTax/",
//        "foodRetailing_wo_rangeConstraint/71a_ICEV_NwCE_BVWP_10000it_DCoff_noTax/",
//        "foodRetailing_wo_rangeConstraint/72_ICEVBEV_NwCE_BVWP_10000it_DCoff_Tax25/",
//        "foodRetailing_wo_rangeConstraint/73_ICEVBEV_NwCE_BVWP_10000it_DCoff_Tax50/",
//        "foodRetailing_wo_rangeConstraint/74_ICEVBEV_NwCE_BVWP_10000it_DCoff_Tax100/",
//        "foodRetailing_wo_rangeConstraint/75_ICEVBEV_NwCE_BVWP_10000it_DCoff_Tax150/",
//        "foodRetailing_wo_rangeConstraint/76_ICEVBEV_NwCE_BVWP_10000it_DCoff_Tax200/",
//        "foodRetailing_wo_rangeConstraint/77_ICEVBEV_NwCE_BVWP_10000it_DCoff_Tax250/",
//        "foodRetailing_wo_rangeConstraint/78_ICEVBEV_NwCE_BVWP_10000it_DCoff_Tax300/",
//        //nun Runs mit ReichweitenConstraint
//        "foodRetailing_with_rangeConstraint/21_ICEVBEV_NwCE_BVWP_10000it_DC_noTax/",
//        "foodRetailing_with_rangeConstraint/22_ICEVBEV_NwCE_BVWP_10000it_DC_Tax25/",
//        "foodRetailing_with_rangeConstraint/23_ICEVBEV_NwCE_BVWP_10000it_DC_Tax50/",
//        "foodRetailing_with_rangeConstraint/24_ICEVBEV_NwCE_BVWP_10000it_DC_Tax100/",
//        "foodRetailing_with_rangeConstraint/25_ICEVBEV_NwCE_BVWP_10000it_DC_Tax150/",
//        "foodRetailing_with_rangeConstraint/26_ICEVBEV_NwCE_BVWP_10000it_DC_Tax200/",
//        "foodRetailing_with_rangeConstraint/27_ICEVBEV_NwCE_BVWP_10000it_DC_Tax250/",
//        "foodRetailing_with_rangeConstraint/28_ICEVBEV_NwCE_BVWP_10000it_DC_Tax300/"
//    )   ;

    //EFood 2024
    final String pathToRunDir = "/Users/kturner/Library/CloudStorage/GoogleDrive-martins-turner@vsp.tu-berlin.de/.shortcut-targets-by-id/1ME69UR7QBzkeVgfJzUTSpxRBUWwH4GVC/vsp-projects/2023/zerocuts/EFood2024/";
    var listOfRuns = List.of(
        //ICEVs only -> 3 Runs
//        "costsVariation_onlyICEV_10000it/Food_fuel1.55_energy0.18/",
//        "costsVariation_onlyICEV_10000it/Food_fuel1.78_energy0.18/",
//        "costsVariation_onlyICEV_10000it/Food_fuel3.2_energy0.18/",
        //ICEV and BEV
        "costsVariation_mixedFleet_withDC_5000it/Food_fuel1.55_energy0.18/",
        "costsVariation_mixedFleet_withDC_5000it/Food_fuel1.55_energy0.21/",
        "costsVariation_mixedFleet_withDC_5000it/Food_fuel1.55_energy0.24/",
        "costsVariation_mixedFleet_withDC_5000it/Food_fuel1.78_energy0.18/",
        "costsVariation_mixedFleet_withDC_5000it/Food_fuel1.78_energy0.21/",
        "costsVariation_mixedFleet_withDC_5000it/Food_fuel1.78_energy0.24/",
        "costsVariation_mixedFleet_withDC_5000it/Food_fuel3.2_energy0.18/",
        "costsVariation_mixedFleet_withDC_5000it/Food_fuel3.2_energy0.21/",
        "costsVariation_mixedFleet_withDC_5000it/Food_fuel3.2_energy0.24/"
    )   ;


    etruckDefinition= EtruckDefinition.ownVehicleType;

    if (args.length == 0) {
      for (String runDir : listOfRuns) {
        new RunFoodEmissions2024(pathToRunDir + runDir).run();
      }
    } else {
      new RunFoodEmissions2024(args[0]).run();
    }
  }

  public RunFoodEmissions2024(String runDirectory) {
    this.runDirectory = runDirectory;

    String analysisOutputDirectory = runDirectory + "/Analysis/1_emissions/";

    if (!analysisOutputDirectory.endsWith("/")) analysisOutputDirectory = analysisOutputDirectory + "/";
    this.analysisOutputDirectory = analysisOutputDirectory;
    new File(this.analysisOutputDirectory).mkdirs();
  }

  void run() throws IOException {

    Config config = ConfigUtils.createConfig();
    config.vehicles().setVehiclesFile(runDirectory + "/output_allVehicles.xml.gz");
    config.network().setInputFile(runDirectory + "/output_network.xml.gz");
    config.global().setCoordinateSystem("EPSG:31468");
//		config.global().setCoordinateSystem("GK4");
    config.plans().setInputFile(null);
    config.eventsManager().setNumberOfThreads(1);
    config.eventsManager().setEstimatedNumberOfEvents(null);
//    config.parallelEventHandling().setNumberOfThreads(null);
//    config.parallelEventHandling().setEstimatedNumberOfEvents(null);
    config.global().setNumberOfThreads(1);

    EmissionsConfigGroup eConfig = ConfigUtils.addOrGetModule(config, EmissionsConfigGroup.class);
    eConfig.setDetailedVsAverageLookupBehavior(
        DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageElseAbort);
    eConfig.setHbefaTableConsistencyCheckingLevel(
        HbefaTableConsistencyCheckingLevel.none);  //KMT: Vielleicht nicht die beste Einstellung, aber das ist eine andere Baustelle ;)
//		eConfig.setAverageColdEmissionFactorsFile("https://svn.vsp.tu-berlin.de/repos/public-svn/3507bb3997e5657ab9da76dbedbb13c9b5991d3e/0e73947443d68f95202b71a156b337f7f71604ae/22823adc0ee6a0e231f35ae897f7b224a86f3a7a.enc"); //scheint nicht ganz die richtige Tabelle zu sein
    eConfig.setAverageColdEmissionFactorsFile(
        "https://svn.vsp.tu-berlin.de/repos/public-svn/3507bb3997e5657ab9da76dbedbb13c9b5991d3e/0e73947443d68f95202b71a156b337f7f71604ae/r9230ru2n209r30u2fn0c9rn20n2rujkhkjhoewt84202.enc"); //daher nun ausnahmsweise doch mal als lokale Kopie, damit wir weiter kommen.
    eConfig.setDetailedColdEmissionFactorsFile(
        "https://svn.vsp.tu-berlin.de/repos/public-svn/3507bb3997e5657ab9da76dbedbb13c9b5991d3e/0e73947443d68f95202b71a156b337f7f71604ae/82t7b02rc0rji2kmsahfwp933u2rfjlkhfpi2u9r20.enc");
    eConfig.setAverageWarmEmissionFactorsFile(
        "https://svn.vsp.tu-berlin.de/repos/public-svn/3507bb3997e5657ab9da76dbedbb13c9b5991d3e/0e73947443d68f95202b71a156b337f7f71604ae/7eff8f308633df1b8ac4d06d05180dd0c5fdf577.enc");

    //TODO: In verschlüsselte Dateien integrieren und ins public SVN laden.
    // Dabei nochmal auf Spalten achten. mMn ist hier emConcept und Technology verdreht -.-
    // Tabelle mit Endung2 hat die Spalten korrigiert.
    eConfig.setDetailedWarmEmissionFactorsFile("original-input-data/HBEFA_summarized_final2.csv");
//    eConfig.setHbefaRoadTypeSource(HbefaRoadTypeSource.fromLinkAttributes);
  eConfig.setNonScenarioVehicles(NonScenarioVehicles.ignore);


    final String eventsFile = runDirectory + "/output_events.xml.gz";

    final String emissionEventOutputFile =
        analysisOutputDirectory + "emission.events.offline2.xml";
    final String linkEmissionAnalysisFile = analysisOutputDirectory  + "emissionsPerLink.csv";
    final String linkEmissionPerMAnalysisFile = analysisOutputDirectory + "emissionsPerLinkPerM.csv";
//    final String vehicleTypeFile = analysisOutputDirectory  + "emissionVehicleInformation.csv";
    final String vehicleEmissionAnalysisFile = analysisOutputDirectory  + "emissionsPerVehicle.csv";
    final String vehicleTypeEmissionAnalysisFile = analysisOutputDirectory  + "emissionsPerVehicleType.csv";
    final String pollutantEmissionAnalysisFile = analysisOutputDirectory + "emissionsPerPollutant.csv";

    Scenario scenario = ScenarioUtils.loadScenario(config);
    // network
    for (Link link : scenario.getNetwork().getLinks().values()) {

      double freespeed;

      if (link.getFreespeed() <= 13.888889) {
        freespeed = link.getFreespeed() * 2;
        // for non motorway roads, the free speed level was reduced
      } else {
        freespeed = link.getFreespeed();
        // for motorways, the original speed levels seems ok.
      }

      if (freespeed <= 8.333333333) { //30kmh
        link.getAttributes().putAttribute("hbefa_road_type", "URB/Access/30");
      } else if (freespeed <= 11.111111111) { //40kmh
        link.getAttributes().putAttribute("hbefa_road_type", "URB/Access/40");
      } else if (freespeed <= 13.888888889) { //50kmh
        double lanes = link.getNumberOfLanes();
        if (lanes <= 1.0) {
          link.getAttributes().putAttribute("hbefa_road_type", "URB/Local/50");
        } else if (lanes <= 2.0) {
          link.getAttributes().putAttribute("hbefa_road_type", "URB/Distr/50");
        } else if (lanes > 2.0) {
          link.getAttributes().putAttribute("hbefa_road_type", "URB/Trunk-City/50");
        } else {
          throw new RuntimeException("NoOfLanes not properly defined");
        }
      } else if (freespeed <= 16.666666667) { //60kmh
        double lanes = link.getNumberOfLanes();
        if (lanes <= 1.0) {
          link.getAttributes().putAttribute("hbefa_road_type", "URB/Local/60");
        } else if (lanes <= 2.0) {
          link.getAttributes().putAttribute("hbefa_road_type", "URB/Trunk-City/60");
        } else if (lanes > 2.0) {
          link.getAttributes().putAttribute("hbefa_road_type", "URB/MW-City/60");
        } else {
          throw new RuntimeException("NoOfLanes not properly defined");
        }
      } else if (freespeed <= 19.444444444) { //70kmh
        link.getAttributes().putAttribute("hbefa_road_type", "URB/MW-City/70");
      } else if (freespeed <= 22.222222222) { //80kmh
        link.getAttributes().putAttribute("hbefa_road_type", "URB/MW-Nat./80");
      } else if (freespeed > 22.222222222) { //faster
        link.getAttributes().putAttribute("hbefa_road_type", "RUR/MW/>130");
      } else {
        throw new RuntimeException("Link not considered...");
      }
    }

    // vehicles
    {
      VehicleType heavy26t_VehicleType = null;
      VehicleType heavy26t_electricityVehicleType = null;
      VehicleType heavy26t_frozenVehicleType = null;
      VehicleType heavy26t_frozen_electricityVehicleType = null;
      VehicleType heavy40tVehicleType = null;
      VehicleType heavy40t_electricityVehicleType = null;
      VehicleType light8tVehicleType = null;
      VehicleType light8t_electricityVehicleType = null;
      VehicleType light8t_frozenVehicleType = null;
      VehicleType light8t_frozen_electricityVehicleType = null;
      VehicleType medium18tVehicleType = null;
      VehicleType medium18t_electricityVehicleType = null;

      VehicleType heavy26t_electro_large_Daimler_VehicleType= null;
      VehicleType heavy26t_electro_small_Volvo_VehicleType = null;
      VehicleType heavy26t_frozen_electro_large_Daimler_VehicleType = null;
      VehicleType heavy26t_frozen_electro_small_Volvo_VehicleType = null;
      VehicleType heavy40t_electro_large_Scania_VehicleType = null;
      VehicleType heavy40t_electro_small_Daimler_VehicleType = null;
      VehicleType light8t_electro_large_Quantron_VehicleType = null;
      VehicleType light8t_electro_small_Mitsubishi_VehicleType = null;
      VehicleType light8t_frozen_electro_large_Quantron_VehicleType = null;
      VehicleType light8t_frozen_electro_small_Mitsubishi_VehicleType = null;
      VehicleType medium18t_electro_large_Volvo_VehicleType = null;
      VehicleType medium18t_electro_small_Renault_VehicleType = null;

      switch (etruckDefinition) {
        case ownVehicleType -> {
          //Runs mit ordentlicher Definition:
          heavy26t_VehicleType = scenario.getVehicles().getVehicleTypes()
              .get(Id.create("heavy26t", VehicleType.class));
          heavy26t_electricityVehicleType = scenario.getVehicles().getVehicleTypes()
              .get(Id.create("heavy26t_electro", VehicleType.class));
          heavy26t_frozenVehicleType = scenario.getVehicles().getVehicleTypes()
              .get(Id.create("heavy26t_frozen", VehicleType.class));
          heavy26t_frozen_electricityVehicleType = scenario.getVehicles().getVehicleTypes()
              .get(Id.create("heavy26t_frozen_electro", VehicleType.class));
          heavy40tVehicleType = scenario.getVehicles().getVehicleTypes()
              .get(Id.create("heavy40t", VehicleType.class));
          heavy40t_electricityVehicleType = scenario.getVehicles().getVehicleTypes()
              .get(Id.create("heavy40t_electro", VehicleType.class));
          light8tVehicleType = scenario.getVehicles().getVehicleTypes()
              .get(Id.create("light8t", VehicleType.class));
          light8t_electricityVehicleType = scenario.getVehicles().getVehicleTypes()
              .get(Id.create("light8t_electro", VehicleType.class));
          light8t_frozenVehicleType = scenario.getVehicles().getVehicleTypes()
              .get(Id.create("light8t_frozen", VehicleType.class));
          light8t_frozen_electricityVehicleType = scenario.getVehicles().getVehicleTypes()
              .get(Id.create("light8t_frozen_electro", VehicleType.class));
          medium18tVehicleType = scenario.getVehicles().getVehicleTypes()
              .get(Id.create("medium18t", VehicleType.class));
          medium18t_electricityVehicleType = scenario.getVehicles().getVehicleTypes()
              .get(Id.create("medium18t_electro", VehicleType.class));

          //And now the vehicle types for EFood2024:
          heavy26t_electro_large_Daimler_VehicleType = scenario.getVehicles().getVehicleTypes()
              .get(Id.create("heavy26t_electro_large_Daimler", VehicleType.class));

          heavy26t_electro_small_Volvo_VehicleType = scenario.getVehicles().getVehicleTypes()
              .get(Id.create("heavy26t_electro_small_Volvo", VehicleType.class));

          heavy26t_frozen_electro_large_Daimler_VehicleType = scenario.getVehicles().getVehicleTypes()
              .get(Id.create("heavy26t_frozen_electro_large_Daimler", VehicleType.class));

          heavy26t_frozen_electro_small_Volvo_VehicleType = scenario.getVehicles().getVehicleTypes()
              .get(Id.create("heavy26t_frozen_electro_small_Volvo", VehicleType.class));

          heavy40t_electro_large_Scania_VehicleType = scenario.getVehicles().getVehicleTypes()
              .get(Id.create("heavy40t_electro_large_Scania", VehicleType.class));

          heavy40t_electro_small_Daimler_VehicleType = scenario.getVehicles().getVehicleTypes()
              .get(Id.create("heavy40t_electro_small_Daimler", VehicleType.class));

          light8t_electro_large_Quantron_VehicleType = scenario.getVehicles().getVehicleTypes()
              .get(Id.create("light8t_electro_large_Quantron", VehicleType.class));

          light8t_electro_small_Mitsubishi_VehicleType = scenario.getVehicles().getVehicleTypes()
              .get(Id.create("light8t_electro_small_Mitsubishi", VehicleType.class));

          light8t_frozen_electro_large_Quantron_VehicleType = scenario.getVehicles().getVehicleTypes()
              .get(Id.create("light8t_frozen_electro_large_Quantron", VehicleType.class));

          light8t_frozen_electro_small_Mitsubishi_VehicleType = scenario.getVehicles().getVehicleTypes()
              .get(Id.create("light8t_frozen_electro_small_Mitsubishi", VehicleType.class));

          medium18t_electro_large_Volvo_VehicleType = scenario.getVehicles().getVehicleTypes()
              .get(Id.create("medium18t_electro_large_Volvo", VehicleType.class));

          medium18t_electro_small_Renault_VehicleType = scenario.getVehicles().getVehicleTypes()
              .get(Id.create("medium18t_electro_small_Renault", VehicleType.class));
        }


        case allVehiclesAreElectric -> { //TODO: Eigentlich müsste ich das schon beim Updaten der Runs "fixen".
          heavy26t_electricityVehicleType = scenario.getVehicles().getVehicleTypes()
              .get(Id.create("heavy26t", VehicleType.class));
          heavy26t_frozen_electricityVehicleType = scenario.getVehicles().getVehicleTypes()
              .get(Id.create("heavy26t_frozen", VehicleType.class));
          heavy40t_electricityVehicleType = scenario.getVehicles().getVehicleTypes()
              .get(Id.create("heavy40t", VehicleType.class));
          light8t_electricityVehicleType = scenario.getVehicles().getVehicleTypes()
              .get(Id.create("light8t", VehicleType.class));
          light8t_frozen_electricityVehicleType = scenario.getVehicles().getVehicleTypes()
              .get(Id.create("light8t_frozen", VehicleType.class));
          medium18t_electricityVehicleType = scenario.getVehicles().getVehicleTypes()
              .get(Id.create("medium18t", VehicleType.class));
        }
        default -> throw new IllegalStateException("Unexpected value: " + etruckDefinition);
      }


      if (heavy26t_VehicleType != null) {
        EngineInformation engineInfo = heavy26t_VehicleType.getEngineInformation();
        VehicleUtils.setHbefaVehicleCategory(engineInfo, HGV_STRING);
        VehicleUtils.setHbefaEmissionsConcept(engineInfo, DIESEL);
        VehicleUtils.setHbefaSizeClass(engineInfo, "RT >20-26t");
        VehicleUtils.setHbefaTechnology(engineInfo, AVERAGE);
      }

      if (heavy26t_electricityVehicleType != null) {
        EngineInformation engineInfo = heavy26t_electricityVehicleType.getEngineInformation();
        VehicleUtils.setHbefaVehicleCategory(engineInfo, HGV_STRING);
        VehicleUtils.setHbefaEmissionsConcept(engineInfo, ELECTRICITY);
        VehicleUtils.setHbefaSizeClass(engineInfo, "RT >12t");
        VehicleUtils.setHbefaTechnology(engineInfo, AVERAGE);
      }

      if (heavy26t_frozenVehicleType != null) {
        EngineInformation engineInfo = heavy26t_frozenVehicleType.getEngineInformation();
        VehicleUtils.setHbefaVehicleCategory(engineInfo, HGV_STRING);
        VehicleUtils.setHbefaEmissionsConcept(engineInfo, DIESEL);
        VehicleUtils.setHbefaSizeClass(engineInfo, "RT >20-26t");
        VehicleUtils.setHbefaTechnology(engineInfo, AVERAGE);
      }

      if (heavy26t_frozen_electricityVehicleType != null) {
        EngineInformation engineInfo = heavy26t_frozen_electricityVehicleType.getEngineInformation();
        VehicleUtils.setHbefaVehicleCategory(engineInfo, HGV_STRING);
        VehicleUtils.setHbefaEmissionsConcept(engineInfo, ELECTRICITY);
        VehicleUtils.setHbefaSizeClass(engineInfo, "RT >12t");
        VehicleUtils.setHbefaTechnology(engineInfo, AVERAGE);
      }

      if (heavy40tVehicleType != null) {
        EngineInformation engineInfo = heavy40tVehicleType.getEngineInformation();
        VehicleUtils.setHbefaVehicleCategory(engineInfo, HGV_STRING);
        VehicleUtils.setHbefaEmissionsConcept(engineInfo, DIESEL);
        VehicleUtils.setHbefaSizeClass(engineInfo, "RT >32t");
        VehicleUtils.setHbefaTechnology(engineInfo, AVERAGE);
      }

      if (heavy40t_electricityVehicleType != null) {
        EngineInformation engineInfo = heavy40t_electricityVehicleType.getEngineInformation();
        VehicleUtils.setHbefaVehicleCategory(engineInfo, HGV_STRING);
        VehicleUtils.setHbefaEmissionsConcept(engineInfo, ELECTRICITY);
        VehicleUtils.setHbefaSizeClass(engineInfo, "RT >12t");
        VehicleUtils.setHbefaTechnology(engineInfo, AVERAGE);
      }

      if (light8tVehicleType != null) {
        EngineInformation engineInfo = light8tVehicleType.getEngineInformation();
        VehicleUtils.setHbefaVehicleCategory(engineInfo, HGV_STRING);
        VehicleUtils.setHbefaEmissionsConcept(engineInfo, DIESEL);
        VehicleUtils.setHbefaSizeClass(engineInfo, "RT >7.5-12t");
        VehicleUtils.setHbefaTechnology(engineInfo, AVERAGE);
      }

      if (light8t_electricityVehicleType != null) {
        EngineInformation engineInfo = light8t_electricityVehicleType.getEngineInformation();
        VehicleUtils.setHbefaVehicleCategory(engineInfo, HGV_STRING);
        VehicleUtils.setHbefaEmissionsConcept(engineInfo, ELECTRICITY);
        VehicleUtils.setHbefaSizeClass(engineInfo, "RT >7.5-12t");
        VehicleUtils.setHbefaTechnology(engineInfo, AVERAGE);
      }

      if (light8t_frozenVehicleType != null) {
        EngineInformation engineInfo = light8t_frozenVehicleType.getEngineInformation();
        VehicleUtils.setHbefaVehicleCategory(engineInfo, HGV_STRING);
        VehicleUtils.setHbefaEmissionsConcept(engineInfo, DIESEL);
        VehicleUtils.setHbefaSizeClass(engineInfo, "RT >7.5-12t");
        VehicleUtils.setHbefaTechnology(engineInfo, AVERAGE);
      }

      if (light8t_frozen_electricityVehicleType != null) {
        EngineInformation engineInfo = light8t_frozen_electricityVehicleType.getEngineInformation();
        VehicleUtils.setHbefaVehicleCategory(engineInfo, HGV_STRING);
        VehicleUtils.setHbefaEmissionsConcept(engineInfo, ELECTRICITY);
        VehicleUtils.setHbefaSizeClass(engineInfo, "RT >7.5-12t");
        VehicleUtils.setHbefaTechnology(engineInfo, AVERAGE);
      }

      if (medium18tVehicleType != null) {
        EngineInformation engineInfo = medium18tVehicleType.getEngineInformation();
        VehicleUtils.setHbefaVehicleCategory(engineInfo, HGV_STRING);
        VehicleUtils.setHbefaEmissionsConcept(engineInfo, DIESEL);
        VehicleUtils.setHbefaSizeClass(engineInfo, "RT >14-20t");
        VehicleUtils.setHbefaTechnology(engineInfo, AVERAGE);
      }

      if (medium18t_electricityVehicleType != null) {
        EngineInformation engineInfo = medium18t_electricityVehicleType.getEngineInformation();
        VehicleUtils.setHbefaVehicleCategory(engineInfo, HGV_STRING);
        VehicleUtils.setHbefaEmissionsConcept(engineInfo, ELECTRICITY);
        VehicleUtils.setHbefaSizeClass(engineInfo, "RT >12t");
        VehicleUtils.setHbefaTechnology(engineInfo, AVERAGE);
      }

      //E-Food 2024:
      if (heavy26t_electro_large_Daimler_VehicleType != null) {
        EngineInformation engineInfo = heavy26t_electro_large_Daimler_VehicleType.getEngineInformation();
        VehicleUtils.setHbefaVehicleCategory(engineInfo, HGV_STRING);
        VehicleUtils.setHbefaEmissionsConcept(engineInfo, ELECTRICITY);
        VehicleUtils.setHbefaSizeClass(engineInfo, "RT >12t");
        VehicleUtils.setHbefaTechnology(engineInfo, AVERAGE);
      }


      if (heavy26t_electro_small_Volvo_VehicleType  != null) {
        EngineInformation engineInfo = heavy26t_electro_small_Volvo_VehicleType.getEngineInformation();
        VehicleUtils.setHbefaVehicleCategory(engineInfo, HGV_STRING);
        VehicleUtils.setHbefaEmissionsConcept(engineInfo, ELECTRICITY);
        VehicleUtils.setHbefaSizeClass(engineInfo, "RT >12t");
        VehicleUtils.setHbefaTechnology(engineInfo, AVERAGE);
      }

      if (heavy26t_frozen_electro_large_Daimler_VehicleType  != null) {
        EngineInformation engineInfo = heavy26t_frozen_electro_large_Daimler_VehicleType.getEngineInformation();
        VehicleUtils.setHbefaVehicleCategory(engineInfo, HGV_STRING);
        VehicleUtils.setHbefaEmissionsConcept(engineInfo, ELECTRICITY);
        VehicleUtils.setHbefaSizeClass(engineInfo, "RT >12t");
        VehicleUtils.setHbefaTechnology(engineInfo, AVERAGE);
      }

      if (heavy26t_frozen_electro_small_Volvo_VehicleType  != null) {
        EngineInformation engineInfo = heavy26t_frozen_electro_small_Volvo_VehicleType.getEngineInformation();
        VehicleUtils.setHbefaVehicleCategory(engineInfo, HGV_STRING);
        VehicleUtils.setHbefaEmissionsConcept(engineInfo, ELECTRICITY);
        VehicleUtils.setHbefaSizeClass(engineInfo, "RT >12t");
        VehicleUtils.setHbefaTechnology(engineInfo, AVERAGE);
      }

      if (heavy40t_electro_large_Scania_VehicleType  != null) {
        EngineInformation engineInfo = heavy40t_electro_large_Scania_VehicleType.getEngineInformation();
        VehicleUtils.setHbefaVehicleCategory(engineInfo, HGV_STRING);
        VehicleUtils.setHbefaEmissionsConcept(engineInfo, ELECTRICITY);
        VehicleUtils.setHbefaSizeClass(engineInfo, "RT >12t");
        VehicleUtils.setHbefaTechnology(engineInfo, AVERAGE);
      }

      if (heavy40t_electro_small_Daimler_VehicleType  != null) {
        EngineInformation engineInfo = heavy40t_electro_small_Daimler_VehicleType.getEngineInformation();
        VehicleUtils.setHbefaVehicleCategory(engineInfo, HGV_STRING);
        VehicleUtils.setHbefaEmissionsConcept(engineInfo, ELECTRICITY);
        VehicleUtils.setHbefaSizeClass(engineInfo, "RT >12t");
        VehicleUtils.setHbefaTechnology(engineInfo, AVERAGE);
      }

      if (light8t_electro_large_Quantron_VehicleType != null) {
        EngineInformation engineInfo = light8t_electro_large_Quantron_VehicleType.getEngineInformation();
        VehicleUtils.setHbefaVehicleCategory(engineInfo, HGV_STRING);
        VehicleUtils.setHbefaEmissionsConcept(engineInfo, ELECTRICITY);
        VehicleUtils.setHbefaSizeClass(engineInfo, "RT >7.5-12t");
        VehicleUtils.setHbefaTechnology(engineInfo, AVERAGE);
      }

      if (light8t_electro_small_Mitsubishi_VehicleType != null) {
        EngineInformation engineInfo = light8t_electro_small_Mitsubishi_VehicleType.getEngineInformation();
        VehicleUtils.setHbefaVehicleCategory(engineInfo, HGV_STRING);
        VehicleUtils.setHbefaEmissionsConcept(engineInfo, ELECTRICITY);
        VehicleUtils.setHbefaSizeClass(engineInfo, "RT >7.5-12t");
        VehicleUtils.setHbefaTechnology(engineInfo, AVERAGE);
      }

      if (light8t_frozen_electro_large_Quantron_VehicleType != null) {
        EngineInformation engineInfo = light8t_frozen_electro_large_Quantron_VehicleType.getEngineInformation();
        VehicleUtils.setHbefaVehicleCategory(engineInfo, HGV_STRING);
        VehicleUtils.setHbefaEmissionsConcept(engineInfo, ELECTRICITY);
        VehicleUtils.setHbefaSizeClass(engineInfo, "RT >7.5-12t");
        VehicleUtils.setHbefaTechnology(engineInfo, AVERAGE);
      }

      if (light8t_frozen_electro_small_Mitsubishi_VehicleType != null) {
        EngineInformation engineInfo = light8t_frozen_electro_small_Mitsubishi_VehicleType.getEngineInformation();
        VehicleUtils.setHbefaVehicleCategory(engineInfo, HGV_STRING);
        VehicleUtils.setHbefaEmissionsConcept(engineInfo, ELECTRICITY);
        VehicleUtils.setHbefaSizeClass(engineInfo, "RT >7.5-12t");
        VehicleUtils.setHbefaTechnology(engineInfo, AVERAGE);
      }

      if (medium18t_electro_large_Volvo_VehicleType != null) {
        EngineInformation engineInfo = medium18t_electro_large_Volvo_VehicleType.getEngineInformation();
        VehicleUtils.setHbefaVehicleCategory(engineInfo, HGV_STRING);
        VehicleUtils.setHbefaEmissionsConcept(engineInfo, ELECTRICITY);
        VehicleUtils.setHbefaSizeClass(engineInfo, "RT >12t");
        VehicleUtils.setHbefaTechnology(engineInfo, AVERAGE);
      }

      if (medium18t_electro_small_Renault_VehicleType != null) {
        EngineInformation engineInfo = medium18t_electro_small_Renault_VehicleType.getEngineInformation();
        VehicleUtils.setHbefaVehicleCategory(engineInfo, HGV_STRING);
        VehicleUtils.setHbefaEmissionsConcept(engineInfo, ELECTRICITY);
        VehicleUtils.setHbefaSizeClass(engineInfo, "RT >12t");
        VehicleUtils.setHbefaTechnology(engineInfo, AVERAGE);
      }


    }

    // the following is copy and paste from the example...

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


    EmissionsOnLinkEventHandler emissionsEventHandler = new EmissionsOnLinkEventHandler(48*3600.);
    eventsManager.addHandler(emissionsEventHandler);

    EmissionsPerVehicleEventHandler emissionsPerVehicleEventHandler = new EmissionsPerVehicleEventHandler();
    eventsManager.addHandler(emissionsPerVehicleEventHandler);

    eventsManager.initProcessing();
    MatsimEventsReader matsimEventsReader = new MatsimEventsReader(eventsManager);
    matsimEventsReader.readFile(eventsFile);
    eventsManager.finishProcessing();

    emissionEventWriter.closeFile();

    log.info("Done reading the events file.");
    log.info("Finish processing...");

    final Map<Id<Link>, Map<Pollutant, Double>> link2pollutants = emissionsEventHandler.getLink2pollutants();

    EmissionsWriterUtils.writePerLinkOutput(linkEmissionAnalysisFile, linkEmissionPerMAnalysisFile, scenario, link2pollutants);
    EmissionsWriterUtils.writePerVehicleOutput(vehicleEmissionAnalysisFile,vehicleTypeEmissionAnalysisFile,scenario, emissionsPerVehicleEventHandler);
    EmissionsWriterUtils.writePerPollutantOutput(pollutantEmissionAnalysisFile, link2pollutants);


    int totalVehicles = scenario.getVehicles().getVehicles().size();
    log.info("Total number of vehicles: " + totalVehicles);

  }

}
