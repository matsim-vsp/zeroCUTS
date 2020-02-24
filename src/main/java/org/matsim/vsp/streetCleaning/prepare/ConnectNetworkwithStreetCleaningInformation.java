package org.matsim.vsp.streetCleaning.prepare;

import java.util.Collection;
import java.util.HashMap;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

/**
 * connects the Berlin network with the information of the street cleaning in
 * Berlin.
 *
 * @author rewert
 */

public class ConnectNetworkwithStreetCleaningInformation {

	static final Logger log = Logger.getLogger(ConnectNetworkwithStreetCleaningInformation.class);

	private static final String berlin = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-1pct/output-berlin-v5.4-1pct/berlin-v5.4-1pct.output_network.xml.gz";
	private static final String cleaningDemandInputShape = "C:\\Users\\Ricardo\\OneDrive\\Dokumente\\Arbeit\\VSP\\zeroCUTs\\Straßenreinigung\\Shape Reinigungsklassen/Reinigungsklassen.shp";
	// source:
	// https://daten.berlin.de/datensaetze/bsr-straßenreinigung-verzeichnisse-und-reinigungsklassen
	private static final String areaOfBerlin = "scenarios/avscenario/shp/berlin.shp";

	public static void main(String[] args) {
		//TODO perheps no use of RKL P

		log.setLevel(Level.INFO);

		// read original Berlin network
		Network network = NetworkUtils.readNetwork(berlin);

		// import links with information about the depot and the amount of street
		// cleaning per week
		Collection<SimpleFeature> cleaningInput = ShapeFileReader.getAllFeatures(cleaningDemandInputShape);

		// import the area of total Berlin
		Geometry areaBerlin = (Geometry) ShapeFileReader.getAllFeatures(areaOfBerlin).iterator().next()
				.getDefaultGeometry();

		// read all link information and convert the coordinates into the correct
		// coordinate system and create a map with key Coord
		HashMap<Coord, SimpleFeature> featureMap = new HashMap<Coord, SimpleFeature>();
		for (SimpleFeature singleDemand : cleaningInput) {
			if (singleDemand.getAttribute("RVZRKL").toString().isBlank() == false) {
				MultiLineString weg = (MultiLineString) singleDemand.getDefaultGeometry();
				LineString wegeteil = (LineString) weg.getGeometryN(0);

				Coord startPoint = MGC.coordinate2Coord(wegeteil.getStartPoint().getCoordinate());
				Coord endPoint = MGC.coordinate2Coord(wegeteil.getEndPoint().getCoordinate());
				Coord midPoint = midPointOfLink(startPoint, endPoint);
				midPoint = TransformationFactory.getCoordinateTransformation("EPSG:4326", "DHDN_GK4")
						.transform(midPoint);
				featureMap.put(midPoint, singleDemand);
			}
		}

		// add the cleaning information to the attributes of the Berlin network.
		// Therefore find nearest Coord to the midpoint of every link in Berlin
		for (Link thisLink : network.getLinks().values()) {
			if (thisLink.getAllowedModes().contains("car") && areaBerlin.contains(MGC.coord2Point(thisLink.getCoord()))
					&& thisLink.getAttributes().getAttribute("type").toString().equals("motorway") == false
					&& thisLink.getAttributes().getAttribute("type").toString().equals("motorway_link") == false) {
				Coord middleLink2 = thisLink.getCoord();
				double minimalDistance = Double.MAX_VALUE;
				Coord coordwithMinimalDistance = null;
				for (Coord coordFeature : featureMap.keySet()) {
					double distance = NetworkUtils.getEuclideanDistance(middleLink2, coordFeature);
					minimalDistance = findMinimalDistance(distance, minimalDistance);
					if (minimalDistance == distance)
						coordwithMinimalDistance = coordFeature;
				}
				thisLink.getAttributes().putAttribute("RKL",
						featureMap.get(coordwithMinimalDistance).getAttribute("RVZRKL").toString());
				thisLink.getAttributes().putAttribute("vehicleDepot",
						featureMap.get(coordwithMinimalDistance).getAttribute("HOF").toString());
				Link oppositeLink = NetworkUtils.findLinkInOppositeDirection(thisLink);
				if (oppositeLink != null) {
					oppositeLink.getAttributes().putAttribute("RKL",
							featureMap.get(coordwithMinimalDistance).getAttribute("RVZRKL").toString());
					oppositeLink.getAttributes().putAttribute("vehicleDepot",
							featureMap.get(coordwithMinimalDistance).getAttribute("HOF").toString());
				}
			}
		}

		// write new network
		NetworkUtils.writeNetwork(network, "scenarios/networks/networkBerlinWithRKL.xml.gz");
	}

	/**
	 * Creates the midpoint of a link.
	 * 
	 * @param link
	 * @return Middle Point of the Link
	 */
	private static Coord midPointOfLink(Coord startCoord, Coord endCoord) {

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

	/**
	 * returns the link with the smaller length
	 * 
	 * @param link
	 * @return minimal distance
	 */
	private static double findMinimalDistance(double distance, double minimalDistance) {
		if (distance < minimalDistance) {
			minimalDistance = distance;
		}
		return minimalDistance;
	}

}
