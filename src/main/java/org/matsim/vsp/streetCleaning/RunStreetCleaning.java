package org.matsim.vsp.streetCleaning;

import java.awt.List;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup.CompressionType;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.network.filter.NetworkLinkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.operation.MathTransform;

public class RunStreetCleaning {

	static final Logger log = Logger.getLogger(RunStreetCleaning.class);

	private static final String berlin = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-1pct/output-berlin-v5.4-1pct/berlin-v5.4-1pct.output_network.xml.gz";
	private static final String cleaningDemandInputShape = "C:\\Users\\Ricardo\\OneDrive\\Dokumente\\Arbeit\\VSP\\zeroCUTs\\Straßenreinigung\\Shape Reinigungsklassen/Reinigungsklassen.shp";

	public static void main(String[] args) {

		log.setLevel(Level.INFO);

		Config config = ConfigUtils.createConfig();
//		Scenario scenario = ScenarioUtils.createScenario(config);
//		Network network = scenario.getNetwork();
////		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84,
////				"EPSG:3857");
//
//		new MatsimNetworkReader(TransformationFactory.WGS84, scenario.getNetwork()).readFile(berlin);
//		scenario.getConfig().global().setCoordinateSystem("EPSG:3857");

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		new OutputDirectoryHierarchy(config.controler().getOutputDirectory(), config.controler().getRunId(),
				config.controler().getOverwriteFileSetting(), CompressionType.gzip);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		config.controler().setLastIteration(0);
		config.global().setRandomSeed(4177);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		config.global().setCoordinateSystem(TransformationFactory.GK4);
		config.controler().setOutputDirectory("output/StreetCleaning/Test1");
		config.network().setInputFile(berlin);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		Collection<SimpleFeature> cleaningInput = ShapeFileReader.getAllFeatures(cleaningDemandInputShape);

		NetworkFilterManager networkFilterManager = new NetworkFilterManager(scenario.getNetwork());

		networkFilterManager.addLinkFilter(new NetworkLinkFilter() {
			@Override
			public boolean judgeLink(Link thisLink) {
				if (thisLink.getId().toString().contains("pt"))
					return true;
				else
					return false;
			}
		});
		new NetworkCleaner().run(scenario.getNetwork());
		networkFilterManager.applyFilters();

		for (SimpleFeature singleDemand : cleaningInput) {

			MultiLineString weg = (MultiLineString) singleDemand.getDefaultGeometry();
			LineString wegeteil = (LineString) weg.getGeometryN(0);

			Coord startPoint = MGC.point2Coord(MGC.xy2Point(wegeteil.getStartPoint().getCoordinate().getX(),
					wegeteil.getStartPoint().getCoordinate().getY()));
			Coord endPoint = MGC.point2Coord(MGC.xy2Point(wegeteil.getEndPoint().getCoordinate().getX(),
					wegeteil.getEndPoint().getCoordinate().getY()));
			Coord middlePoint = middlePointOfLink(startPoint, endPoint);
			middlePoint = TransformationFactory.getCoordinateTransformation("EPSG:4326", "DHDN_GK4")
					.transform(middlePoint);

			Link link = NetworkUtils.getNearestLink(scenario.getNetwork(), middlePoint); // TODO no pt-link
			double distance = NetworkUtils.getEuclideanDistance(middlePoint, link.getCoord());

			link.getAttributes().putAttribute("RKL", singleDemand.getAttribute("RVZRKL").toString());
			link.getAttributes().putAttribute("vehicleDepot", singleDemand.getAttribute("HOF").toString());
			link.getAttributes().putAttribute("distanceFeatureAndLink", distance);
			NetworkUtils.writeNetwork(scenario.getNetwork(), "scenarios/networks/testmatsimin.xml.gz");
			boolean shape = false;
			if (shape == true) {
				String shapeFileLine = "scenarios/networks/testmatsiminShp2.shp";
				String shapeFilePoly = "scenarios/networks/testmatsimShp.shp";
				String crs = TransformationFactory.DHDN_GK4;
				Links2ESRIShape.main(new String[] { berlin, shapeFileLine, shapeFilePoly, crs });
			}

		}
	}

	/**
	 * Creates the middle point of a link.
	 * 
	 * @param link
	 * @return Middle Point of the Link
	 */
	private static Coord middlePointOfLink(Coord startCoord, Coord endCoord) {

		double x, y, xCoordFrom, xCoordTo, yCoordFrom, yCoordTo;
		xCoordFrom = startCoord.getX();
		xCoordTo = endCoord.getX();
		yCoordFrom = startCoord.getY();
		yCoordTo = endCoord.getY();
		if (xCoordFrom > xCoordTo)
			x = xCoordFrom - ((xCoordFrom - xCoordTo) / 2);
		else
			x = xCoordTo - ((xCoordTo - xCoordFrom) / 2);
		if (yCoordFrom > yCoordTo)
			y = yCoordFrom - ((yCoordFrom - yCoordTo) / 2);
		else
			y = yCoordTo - ((yCoordTo - yCoordFrom) / 2);

		return MGC.point2Coord(MGC.xy2Point(x, y));
	}

	private static double findMinimalDistance(double distance, double minimalDistance) {
		if (distance < minimalDistance) {
			minimalDistance = distance;
		}
		return minimalDistance;
	}

}
