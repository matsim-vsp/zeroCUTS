package org.matsim.vsp.SmallScaleFreightTraffic;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.application.options.ShpOptions;
import org.matsim.application.options.ShpOptions.Index;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;

/**
 * @author Ricardo
 *
 */
public class LanduseBuildingAnalysis {

	private static final Logger log = LogManager.getLogger(LanduseBuildingAnalysis.class);

	/**
	 * Creates a distribution of the given input data for each zone based on the
	 * used OSM data.
	 * 
	 * @param landuseCategoriesAndDataConnection
	 * @param inputDataDirectory
	 * @param shapeFileZonePath
	 * @param shapeFileLandusePath
	 * @param i
	 * @param string
	 * 
	 * @param shapeFileZonePath
	 * @param shapeFileLandusePath
	 * @param shapeFileBuildingsPath
	 * @param shapeFileBuildingsPath
	 * @param buildingsPerZone
	 * @param indexZones
	 * @param indexLanduse
	 * @return
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	static HashMap<String, Object2DoubleMap<String>> createInputDataDistribution(Path output,
			HashMap<String, ArrayList<String>> landuseCategoriesAndDataConnection, Path inputDataDirectory,
			String usedLanduseConfiguration, Path shapeFileLandusePath, Path shapeFileZonePath,
			Path shapeFileBuildingsPath, HashMap<String, HashMap<String, ArrayList<SimpleFeature>>> buildingsPerZone) throws IOException, MalformedURLException {

		HashMap<String, Object2DoubleMap<String>> resultingDataPerZone = new HashMap<String, Object2DoubleMap<String>>();
		Path outputFileInOutputFolder = output.resolve("caculatedData").resolve("dataDistributionPerZone.csv");

		landuseCategoriesAndDataConnection.put("Inhabitants", new ArrayList<String>(Arrays.asList("residential",
				"apartments", "dormitory", "dwelling_house", "house", "retirement_home", "semidetached_house")));
		landuseCategoriesAndDataConnection.put("Employee Primary Sector",
				new ArrayList<String>(Arrays.asList("farmyard", "farmland", "farm", "farm_auxiliary", "greenhouse")));
		landuseCategoriesAndDataConnection.put("Employee Construction",
				new ArrayList<String>(Arrays.asList("construction")));
		landuseCategoriesAndDataConnection.put("Employee Secondary Sector Rest",
				new ArrayList<String>(Arrays.asList("industrial", "factory", "manufacture")));
		landuseCategoriesAndDataConnection.put("Employee Retail",
				new ArrayList<String>(Arrays.asList("retail", "kiosk", "mall", "shop", "supermarket")));
		landuseCategoriesAndDataConnection.put("Employee Traffic/Parcels", new ArrayList<String>(
				Arrays.asList("commercial", "post_office", "storage", "storage_tank", "warehouse")));
		landuseCategoriesAndDataConnection.put("Employee Tertiary Sector Rest", new ArrayList<String>(
				Arrays.asList("commercial", "embassy", "foundation", "government", "office", "townhall")));
		landuseCategoriesAndDataConnection.put("Employee", new ArrayList<String>());
		landuseCategoriesAndDataConnection.get("Employee")
				.addAll(landuseCategoriesAndDataConnection.get("Employee Primary Sector"));
		landuseCategoriesAndDataConnection.get("Employee")
				.addAll(landuseCategoriesAndDataConnection.get("Employee Construction"));
		landuseCategoriesAndDataConnection.get("Employee")
				.addAll(landuseCategoriesAndDataConnection.get("Employee Secondary Sector Rest"));
		landuseCategoriesAndDataConnection.get("Employee")
				.addAll(landuseCategoriesAndDataConnection.get("Employee Retail"));
		landuseCategoriesAndDataConnection.get("Employee")
				.addAll(landuseCategoriesAndDataConnection.get("Employee Traffic/Parcels"));
		landuseCategoriesAndDataConnection.get("Employee")
				.addAll(landuseCategoriesAndDataConnection.get("Employee Tertiary Sector Rest"));

		if (usedLanduseConfiguration.equals("useExistingDataDistribution")) {
			Path existingDataDistribution = inputDataDirectory.resolve("dataDistributionPerZone.csv");

			if (!Files.exists(existingDataDistribution)) {
				log.error("Required data per zone file {} not found", existingDataDistribution);
			}

			try (BufferedReader reader = IOUtils.getBufferedReader(existingDataDistribution.toString())) {
				CSVParser parse = CSVFormat.DEFAULT.withDelimiter('\t').withFirstRecordAsHeader().parse(reader);

				for (CSVRecord record : parse) {
					String zoneID = record.get("areaID");
					resultingDataPerZone.put(zoneID, new Object2DoubleOpenHashMap<>());
					for (int n = 1; n < parse.getHeaderMap().size(); n++) {
						resultingDataPerZone.get(zoneID).mergeDouble(parse.getHeaderNames().get(n),
								Double.valueOf(record.get(n)), Double::sum);
					}
				}
			}
			log.info("Data distribution for " + resultingDataPerZone.size() + " zones was imported from ",
					existingDataDistribution);
			Files.copy(existingDataDistribution, outputFileInOutputFolder, StandardCopyOption.COPY_ATTRIBUTES);
		}

		else {

			log.info("New analyze for data distribution is started. The used method is: " + usedLanduseConfiguration);
			HashMap<String, Object2DoubleMap<String>> landuseCategoriesPerZone = new HashMap<String, Object2DoubleMap<String>>();
			createLanduseDistribution(landuseCategoriesPerZone, shapeFileLandusePath, shapeFileZonePath,
					usedLanduseConfiguration, shapeFileBuildingsPath, landuseCategoriesAndDataConnection,
					buildingsPerZone);

			HashMap<String, HashMap<String, Integer>> investigationAreaData = new HashMap<String, HashMap<String, Integer>>();
			readAreaData(investigationAreaData, inputDataDirectory);

			createResultingDataForLanduseInZones(landuseCategoriesPerZone, investigationAreaData, resultingDataPerZone,
					landuseCategoriesAndDataConnection);

			SmallScaleFreightTrafficUtils.writeResultOfDataDistribution(resultingDataPerZone, outputFileInOutputFolder);
		}

		return resultingDataPerZone;
	}

	/**
	 * @param landuseCategoriesPerZone
	 * @param investigationAreaData
	 * @param resultingDataPerZone
	 * @param landuseCategoriesAndDataConnection
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	private static void createResultingDataForLanduseInZones(
			HashMap<String, Object2DoubleMap<String>> landuseCategoriesPerZone,
			HashMap<String, HashMap<String, Integer>> investigationAreaData,
			HashMap<String, Object2DoubleMap<String>> resultingDataPerZone,
			HashMap<String, ArrayList<String>> landuseCategoriesAndDataConnection)
			throws MalformedURLException, IOException {

		Object2DoubleMap<String> totalSquareMetersPerCategory = new Object2DoubleOpenHashMap<>();
		Object2DoubleMap<String> totalEmployeesInCategoriesPerZone = new Object2DoubleOpenHashMap<>();
		Object2DoubleMap<String> totalEmployeesPerCategories = new Object2DoubleOpenHashMap<>();

		// connects the collected landuse data with the needed categories
		for (String zoneID : landuseCategoriesPerZone.keySet()) {
			resultingDataPerZone.put(zoneID, new Object2DoubleOpenHashMap<>());
			for (String categoryLanduse : landuseCategoriesPerZone.get(zoneID).keySet()) {
				for (String categoryData : landuseCategoriesAndDataConnection.keySet()) {
					if (landuseCategoriesAndDataConnection.get(categoryData).contains(categoryLanduse)) {
						double additionalArea = landuseCategoriesPerZone.get(zoneID).getDouble(categoryLanduse);
						resultingDataPerZone.get(zoneID).mergeDouble(categoryData, additionalArea, Double::sum);
						totalSquareMetersPerCategory.mergeDouble(categoryData, additionalArea, Double::sum);
					}
				}
			}
		}

		/*
		 * creates the percentages of each category and zones based on the sum in this
		 * category
		 */
		for (String zoneId : resultingDataPerZone.keySet()) {
			for (String categoryData : resultingDataPerZone.get(zoneId).keySet()) {
				double newValue = resultingDataPerZone.get(zoneId).getDouble(categoryData)
						/ totalSquareMetersPerCategory.getDouble(categoryData);
				resultingDataPerZone.get(zoneId).replace(categoryData,
						resultingDataPerZone.get(zoneId).getDouble(categoryData), newValue);
			}
		}
		// can be deleted or used as test
		Object2DoubleMap<String> checkPercentages = new Object2DoubleOpenHashMap<>();
		for (String landuseCategoriesForSingleZone : resultingDataPerZone.keySet()) {
			for (String category : resultingDataPerZone.get(landuseCategoriesForSingleZone).keySet()) {
				checkPercentages.mergeDouble(category,
						resultingDataPerZone.get(landuseCategoriesForSingleZone).getDouble(category), Double::sum);
			}
		}
		for (String category : checkPercentages.keySet()) {
			if (Math.abs(1 - checkPercentages.getDouble(category)) > 0.01)
				throw new RuntimeException("Sum of percenatges is not 1. For " + category + " the sum is "
						+ checkPercentages.getDouble(category) + "%");
		}
		// calculates the data per zone and category data
		for (String zoneId : resultingDataPerZone.keySet()) {
			for (String categoryData : resultingDataPerZone.get(zoneId).keySet()) {
				double percentageValue = resultingDataPerZone.get(zoneId).getDouble(categoryData);
				int inputDataForCategory = investigationAreaData.get("Berlin").get(categoryData);
				double resultingNumberPerCategory = percentageValue * inputDataForCategory;
				resultingDataPerZone.get(zoneId).replace(categoryData, percentageValue, resultingNumberPerCategory);
				totalEmployeesPerCategories.mergeDouble(categoryData, resultingNumberPerCategory, Double::sum);
				if (!categoryData.equals("Employee") && !categoryData.equals("Inhabitants"))
					totalEmployeesInCategoriesPerZone.mergeDouble(zoneId, resultingNumberPerCategory, Double::sum);

			}
		}
		// corrects the number of employees in the categories so that the sum is correct
		for (int i = 0; i < 30; i++) { // TODO perhaps find number of iterations
			for (String zoneId : resultingDataPerZone.keySet()) {
				for (String categoryData : resultingDataPerZone.get(zoneId).keySet()) {
					if (!categoryData.equals("Employee") && !categoryData.equals("Inhabitants")) {
						double correctionFactor = resultingDataPerZone.get(zoneId).getDouble("Employee")
								/ totalEmployeesInCategoriesPerZone.getDouble(zoneId);
						double resultingNumberPerCategory = correctionFactor
								* resultingDataPerZone.get(zoneId).getDouble(categoryData);
						resultingDataPerZone.get(zoneId).replace(categoryData,
								resultingDataPerZone.get(zoneId).getDouble(categoryData), resultingNumberPerCategory);
					}
				}
			}

			for (String categoryData : investigationAreaData.get("Berlin").keySet()) {
				for (String zoneId : resultingDataPerZone.keySet()) {
					if (!categoryData.equals("Employee") && !categoryData.equals("Inhabitants")) {
						double correctionFactor = investigationAreaData.get("Berlin").get(categoryData)
								/ totalEmployeesPerCategories.getDouble(categoryData);
						double resultingNumberPerCategory = correctionFactor
								* resultingDataPerZone.get(zoneId).getDouble(categoryData);
						resultingDataPerZone.get(zoneId).replace(categoryData,
								resultingDataPerZone.get(zoneId).getDouble(categoryData), resultingNumberPerCategory);
					}
				}
			}
			// update totals per sum becaus eof the changes before
			totalEmployeesInCategoriesPerZone.clear();
			totalEmployeesPerCategories.clear();
			for (String zoneId : resultingDataPerZone.keySet()) {
				for (String categoryData : resultingDataPerZone.get(zoneId).keySet()) {
					totalEmployeesPerCategories.mergeDouble(categoryData,
							resultingDataPerZone.get(zoneId).getDouble(categoryData), Double::sum);
					if (!categoryData.equals("Employee") && !categoryData.equals("Inhabitants"))
						totalEmployeesInCategoriesPerZone.mergeDouble(zoneId,
								resultingDataPerZone.get(zoneId).getDouble(categoryData), Double::sum);
				}
			}

		}
	}

	/**
	 * Method create the percentage for each land use category in each zone based on
	 * the sum of this category in all zones of the zone shape file
	 * 
	 * @param shapeFileLandusePath               Path to shape file with the land
	 *                                           use information
	 * @param shapeFileZonesPath                 Path to shape file with the zone
	 *                                           information
	 * @param landuseCategoriesPerZone
	 * @param shapeFileZonePath
	 * @param shapeFileLandusePath
	 * @param shapeFileLandusePath
	 * @param usedLanduseConfiguration
	 * @param shapeFileBuildingsPath
	 * @param shapeFileBuildingsPath
	 * @param landuseCategoriesAndDataConnection
	 * @param buildingsPerZone
	 * @param indexZones
	 * @param indexLanduse
	 * 
	 */
	static void createLanduseDistribution(HashMap<String, Object2DoubleMap<String>> landuseCategoriesPerZone,
			Path shapeFileLandusePath, Path shapeFileZonePath, String usedLanduseConfiguration,
			Path shapeFileBuildingsPath, HashMap<String, ArrayList<String>> landuseCategoriesAndDataConnection,
			HashMap<String, HashMap<String, ArrayList<SimpleFeature>>> buildingsPerZone) {

		List<String> neededLanduseCategories = List.of("residential", "industrial", "commercial", "retail", "farmyard",
				"farmland", "construction");

		ShpOptions shpLanduse = new ShpOptions(shapeFileLandusePath, null, StandardCharsets.UTF_8);
		ShpOptions shpZones = new ShpOptions(shapeFileZonePath, null, StandardCharsets.UTF_8);

		List<SimpleFeature> landuseFeatures = shpLanduse.readFeatures();
		List<SimpleFeature> zonesFeatures = shpZones.readFeatures();

//		 Index indexLanduse = SmallScaleFreightTrafficUtils.getIndexLanduse(shapeFileLandusePath);
		 Index indexZones = SmallScaleFreightTrafficUtils.getIndexZones(shapeFileZonePath);

		for (SimpleFeature districId : zonesFeatures) {
			Object2DoubleMap<String> landusePerCategory = new Object2DoubleOpenHashMap<>();
			landuseCategoriesPerZone.put((String) districId.getAttribute("gml_id"), landusePerCategory);
		}

		if (usedLanduseConfiguration.equals("useOSMBuildingsAndLanduse")) {

			ShpOptions shpBuildings = new ShpOptions(shapeFileBuildingsPath, null, StandardCharsets.UTF_8);
			List<SimpleFeature> buildingsFeatures = shpBuildings.readFeatures();
			analyzeBuildingType(buildingsFeatures, buildingsPerZone, landuseCategoriesAndDataConnection, indexZones,
					indexZones);

			for (String zone : buildingsPerZone.keySet()) {
				for (String category : buildingsPerZone.get(zone).keySet()) {
					for (SimpleFeature building : buildingsPerZone.get(zone).get(category)) {
						String[] buildingTypes = ((String) building.getAttribute("type")).split(";");
						for (String singleCategoryOfBuilding : buildingTypes) {
							double buildingLevels;
							if (building.getAttribute("levels") == null)
								buildingLevels = 1;
							else
								buildingLevels = (long) building.getAttribute("levels") / buildingTypes.length;
							double area = (double) ((long) building.getAttribute("area")) * buildingLevels;

							landuseCategoriesPerZone.get(zone).mergeDouble(singleCategoryOfBuilding, area, Double::sum);
						}

					}
				}
			}
		} else if (usedLanduseConfiguration.equals("useOnlyOSMLanduse")) {
			for (SimpleFeature singleLanduseFeature : landuseFeatures) {
				if (!neededLanduseCategories.contains((String) singleLanduseFeature.getAttribute("fclass")))
					continue;
				Point centroidPointOfLandusePolygon = ((Geometry) singleLanduseFeature.getDefaultGeometry())
						.getCentroid();

				for (SimpleFeature singleZone : zonesFeatures) {
					if (((Geometry) singleZone.getDefaultGeometry()).contains(centroidPointOfLandusePolygon)) {
						landuseCategoriesPerZone.get(singleZone.getAttribute("gml_id")).mergeDouble(
								(String) singleLanduseFeature.getAttribute("fclass"),
								(double) singleLanduseFeature.getAttribute("area"), Double::sum);
						continue;
					}
				}
			}
		}
	}

	/**
	 * Reads the input data for certain areas from the csv file.
	 * 
	 * @param areaDataPath
	 * @param areaData
	 * @param inputDataDirectory
	 * @throws IOException
	 */
	private static void readAreaData(HashMap<String, HashMap<String, Integer>> areaData, Path inputDataDirectory)
			throws IOException {

		Path areaDataPath = inputDataDirectory.resolve("investigationAreaData.csv");
		if (!Files.exists(areaDataPath)) {
			log.error("Required input data file {} not found", areaDataPath);
		}
		try (CSVParser parser = new CSVParser(Files.newBufferedReader(areaDataPath),
				CSVFormat.TDF.withFirstRecordAsHeader())) {

			for (CSVRecord record : parser) {
				HashMap<String, Integer> lookUpTable = new HashMap<>();
				for (String csvRecord : parser.getHeaderMap().keySet()) {
					if (parser.getHeaderMap().get(csvRecord) > 0)
						lookUpTable.put(csvRecord, Integer.valueOf(record.get(csvRecord)));
				}
				areaData.put(record.get("Area"), lookUpTable);
			}
		}
	}

	/**
	 * @param categoriesOfBuilding
	 * @param buildingsFeatures
	 * @param buildingsPerZone
	 * @param landuseCategoriesAndDataConnection
	 * @param indexZones
	 * @param indexLanduse
	 */
	static void analyzeBuildingType(List<SimpleFeature> buildingsFeatures,
			HashMap<String, HashMap<String, ArrayList<SimpleFeature>>> buildingsPerZone,
			HashMap<String, ArrayList<String>> landuseCategoriesAndDataConnection, Index indexLanduse,
			Index indexZones) {
		int countOSMObjects = 0;
		log.info("Analyzing buildings types. This may take some time...");
		for (SimpleFeature singleBuildingFeature : buildingsFeatures) {
			countOSMObjects++;
			if (countOSMObjects % 10000 == 0)
				log.info("Investigate Building " + countOSMObjects + " of " + buildingsFeatures.size() + " buildings: "
						+ Math.round((double) countOSMObjects / buildingsFeatures.size() * 100) + " %");

			List<String> categoriesOfBuilding = new ArrayList<String>();
			String[] buildingTypes;
			Coord centroidPointOfBuildingPolygon = MGC
					.point2Coord(((Geometry) singleBuildingFeature.getDefaultGeometry()).getCentroid());
			String singleZone = indexZones.query(centroidPointOfBuildingPolygon);
			String buildingType = String.valueOf(singleBuildingFeature.getAttribute("type"));
			if (buildingType.equals("") || buildingType.equals("null")) {
				buildingType = indexLanduse.query(centroidPointOfBuildingPolygon);
				buildingTypes = new String[] { buildingType };
			} else {
				buildingType.replace(" ", "");
				buildingTypes = buildingType.split(";");
			}
			singleBuildingFeature.setAttribute("type", String.join(";", buildingTypes));
			for (String singleBuildingType : buildingTypes) {
				for (String category : landuseCategoriesAndDataConnection.keySet()) {
					if (landuseCategoriesAndDataConnection.get(category).contains(singleBuildingType)
							&& !categoriesOfBuilding.contains(category)) {
						categoriesOfBuilding.add(category);
					}
				}
			}
			if (singleZone != null) {
				categoriesOfBuilding.forEach(c -> buildingsPerZone
						.computeIfAbsent(singleZone, k -> new HashMap<String, ArrayList<SimpleFeature>>())
						.computeIfAbsent(c, k -> new ArrayList<SimpleFeature>()).add(singleBuildingFeature));
			}
		}
		log.info("Finished anlyzing buildings types.");
	}

}
