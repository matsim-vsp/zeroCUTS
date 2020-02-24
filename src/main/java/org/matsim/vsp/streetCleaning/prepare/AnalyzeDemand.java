package org.matsim.vsp.streetCleaning.prepare;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;

public class AnalyzeDemand {

	private static final String networkWithClaeiningInformation = "scenarios/networks/networkBerlinWithRKL.xml.gz";

	public static void main(String[] args) {

		// read Berlin network
		Network network = NetworkUtils.readNetwork(networkWithClaeiningInformation);

		int demandRKL_A1a_depot1 = 0;
		int demandRKL_A1b_depot1 = 0;
		int demandRKL_A2a_depot1 = 0;
		int demandRKL_A2b_depot1 = 0;
		int demandRKL_A3_depot1 = 0;
		int demandRKL_A4_depot1 = 0;
		int demandRKL_B_depot1 = 0;
		int demandRKL_C_depot1 = 0;
		int demandRKL_P_depot1 = 0;
		double distanceRKL_A1a_depot1 = 0;
		double distanceRKL_A1b_depot1 = 0;
		double distanceRKL_A2a_depot1 = 0;
		double distanceRKL_A2b_depot1 = 0;
		double distanceRKL_A3_depot1 = 0;
		double distanceRKL_A4_depot1 = 0;
		double distanceRKL_B_depot1 = 0;
		double distanceRKL_C_depot1 = 0;
		double distanceRKL_P_depot1 = 0;

		int demandRKL_A1a_depot2 = 0;
		int demandRKL_A1b_depot2 = 0;
		int demandRKL_A2a_depot2 = 0;
		int demandRKL_A2b_depot2 = 0;
		int demandRKL_A3_depot2 = 0;
		int demandRKL_A4_depot2 = 0;
		int demandRKL_B_depot2 = 0;
		int demandRKL_C_depot2 = 0;
		int demandRKL_P_depot2 = 0;
		
		double distanceRKL_A1a_depot2 = 0;
		double distanceRKL_A1b_depot2 = 0;
		double distanceRKL_A2a_depot2 = 0;
		double distanceRKL_A2b_depot2 = 0;
		double distanceRKL_A3_depot2 = 0;
		double distanceRKL_A4_depot2 = 0;
		double distanceRKL_B_depot2 = 0;
		double distanceRKL_C_depot2 = 0;
		double distanceRKL_P_depot2 = 0;

		int demandRKL_A1a_depot3 = 0;
		int demandRKL_A1b_depot3 = 0;
		int demandRKL_A2a_depot3 = 0;
		int demandRKL_A2b_depot3 = 0;
		int demandRKL_A3_depot3 = 0;
		int demandRKL_A4_depot3 = 0;
		int demandRKL_B_depot3 = 0;
		int demandRKL_C_depot3 = 0;
		int demandRKL_P_depot3 = 0;
		
		double distanceRKL_A1a_depot3 = 0;
		double distanceRKL_A1b_depot3 = 0;
		double distanceRKL_A2a_depot3 = 0;
		double distanceRKL_A2b_depot3 = 0;
		double distanceRKL_A3_depot3 = 0;
		double distanceRKL_A4_depot3 = 0;
		double distanceRKL_B_depot3 = 0;
		double distanceRKL_C_depot3 = 0;
		double distanceRKL_P_depot3 = 0;

		int demandRKL_A1a_depot4 = 0;
		int demandRKL_A1b_depot4 = 0;
		int demandRKL_A2a_depot4 = 0;
		int demandRKL_A2b_depot4 = 0;
		int demandRKL_A3_depot4 = 0;
		int demandRKL_A4_depot4 = 0;
		int demandRKL_B_depot4 = 0;
		int demandRKL_C_depot4 = 0;
		int demandRKL_P_depot4 = 0;
		
		double distanceRKL_A1a_depot4 = 0;
		double distanceRKL_A1b_depot4 = 0;
		double distanceRKL_A2a_depot4 = 0;
		double distanceRKL_A2b_depot4 = 0;
		double distanceRKL_A3_depot4 = 0;
		double distanceRKL_A4_depot4 = 0;
		double distanceRKL_B_depot4 = 0;
		double distanceRKL_C_depot4 = 0;
		double distanceRKL_P_depot4 = 0;

		int demandRKL_A1a_depot5 = 0;
		int demandRKL_A1b_depot5 = 0;
		int demandRKL_A2a_depot5 = 0;
		int demandRKL_A2b_depot5 = 0;
		int demandRKL_A3_depot5 = 0;
		int demandRKL_A4_depot5 = 0;
		int demandRKL_B_depot5 = 0;
		int demandRKL_C_depot5 = 0;
		int demandRKL_P_depot5 = 0;
		
		double distanceRKL_A1a_depot5 = 0;
		double distanceRKL_A1b_depot5 = 0;
		double distanceRKL_A2a_depot5 = 0;
		double distanceRKL_A2b_depot5 = 0;
		double distanceRKL_A3_depot5 = 0;
		double distanceRKL_A4_depot5 = 0;
		double distanceRKL_B_depot5 = 0;
		double distanceRKL_C_depot5 = 0;
		double distanceRKL_P_depot5 = 0;

		for (Link link : network.getLinks().values()) {
			if (link.getAttributes().getAsMap().containsKey("RKL")) {
				if (link.getAttributes().getAttribute("vehicleDepot").equals("R1")) {

					if (link.getAttributes().getAttribute("RKL").equals("A1a")) {
						demandRKL_A1a_depot1++;
						distanceRKL_A1a_depot1 = distanceRKL_A1a_depot1 + link.getLength();
					} else if (link.getAttributes().getAttribute("RKL").equals("A1b")) {
						demandRKL_A1b_depot1++;
						distanceRKL_A1b_depot1 = distanceRKL_A1b_depot1 + link.getLength();
					} else if (link.getAttributes().getAttribute("RKL").equals("A2a")) {
						demandRKL_A2a_depot1++;
						distanceRKL_A2a_depot1 = distanceRKL_A2a_depot1 + link.getLength();
					} else if (link.getAttributes().getAttribute("RKL").equals("A2b")) {
						demandRKL_A2b_depot1++;
						distanceRKL_A2b_depot1 = distanceRKL_A2b_depot1 + link.getLength();
					} else if (link.getAttributes().getAttribute("RKL").equals("A3")) {
						demandRKL_A3_depot1++;
						distanceRKL_A3_depot1 = distanceRKL_A3_depot1 + link.getLength();
					} else if (link.getAttributes().getAttribute("RKL").equals("A4")) {
						demandRKL_A4_depot1++;
						distanceRKL_A4_depot1 = distanceRKL_A4_depot1 + link.getLength();
					} else if (link.getAttributes().getAttribute("RKL").equals("B")) {
						demandRKL_B_depot1++;
						distanceRKL_B_depot1 = distanceRKL_B_depot1 + link.getLength();
					} else if (link.getAttributes().getAttribute("RKL").equals("C")) {
						demandRKL_C_depot1++;
						distanceRKL_C_depot1 = distanceRKL_C_depot1 + link.getLength();
					} else if (link.getAttributes().getAttribute("RKL").equals("P")) {
						demandRKL_P_depot1++;
						distanceRKL_P_depot1 = distanceRKL_P_depot1 + link.getLength();
					}
				}
				if (link.getAttributes().getAttribute("vehicleDepot").equals("R2")) {

					if (link.getAttributes().getAttribute("RKL").equals("A1a")) {
						demandRKL_A1a_depot2++;
						distanceRKL_A1a_depot2 = distanceRKL_A1a_depot2 + link.getLength();
					} else if (link.getAttributes().getAttribute("RKL").equals("A1b")) {
						demandRKL_A1b_depot2++;
						distanceRKL_A1b_depot2 = distanceRKL_A1b_depot2 + link.getLength();
					} else if (link.getAttributes().getAttribute("RKL").equals("A2a")) {
						demandRKL_A2a_depot2++;
						distanceRKL_A2a_depot2 = distanceRKL_A2a_depot2 + link.getLength();
					} else if (link.getAttributes().getAttribute("RKL").equals("A2b")) {
						demandRKL_A2b_depot2++;
						distanceRKL_A2b_depot2 = distanceRKL_A2b_depot2 + link.getLength();
					} else if (link.getAttributes().getAttribute("RKL").equals("A3")) {
						demandRKL_A3_depot2++;
						distanceRKL_A3_depot2 = distanceRKL_A3_depot2 + link.getLength();
					} else if (link.getAttributes().getAttribute("RKL").equals("A4")) {
						demandRKL_A4_depot2++;
						distanceRKL_A4_depot2 = distanceRKL_A4_depot2 + link.getLength();
					} else if (link.getAttributes().getAttribute("RKL").equals("B")) {
						demandRKL_B_depot2++;
						distanceRKL_B_depot2 = distanceRKL_B_depot2 + link.getLength();
					} else if (link.getAttributes().getAttribute("RKL").equals("C")) {
						demandRKL_C_depot2++;
						distanceRKL_C_depot2 = distanceRKL_C_depot2 + link.getLength();
					} else if (link.getAttributes().getAttribute("RKL").equals("P")) {
						demandRKL_P_depot2++;
						distanceRKL_P_depot2 = distanceRKL_P_depot2 + link.getLength();
					}
				}
				if (link.getAttributes().getAttribute("vehicleDepot").equals("R3")) {

					if (link.getAttributes().getAttribute("RKL").equals("A1a")) {
						demandRKL_A1a_depot3++;
						distanceRKL_A1a_depot3 = distanceRKL_A1a_depot3 + link.getLength();
					} else if (link.getAttributes().getAttribute("RKL").equals("A1b")) {
						demandRKL_A1b_depot3++;
						distanceRKL_A1b_depot3 = distanceRKL_A1b_depot3 + link.getLength();
					} else if (link.getAttributes().getAttribute("RKL").equals("A2a")) {
						demandRKL_A2a_depot3++;
						distanceRKL_A2a_depot3 = distanceRKL_A2a_depot3 + link.getLength();
					} else if (link.getAttributes().getAttribute("RKL").equals("A2b")) {
						demandRKL_A2b_depot3++;
						distanceRKL_A2b_depot3 = distanceRKL_A2b_depot3 + link.getLength();
					} else if (link.getAttributes().getAttribute("RKL").equals("A3")) {
						demandRKL_A3_depot3++;
						distanceRKL_A3_depot3 = distanceRKL_A3_depot3 + link.getLength();
					} else if (link.getAttributes().getAttribute("RKL").equals("A4")) {
						demandRKL_A4_depot3++;
						distanceRKL_A4_depot3 = distanceRKL_A4_depot3 + link.getLength();
					} else if (link.getAttributes().getAttribute("RKL").equals("B")) {
						demandRKL_B_depot3++;
						distanceRKL_B_depot3 = distanceRKL_B_depot3 + link.getLength();
					} else if (link.getAttributes().getAttribute("RKL").equals("C")) {
						demandRKL_C_depot3++;
						distanceRKL_C_depot3 = distanceRKL_C_depot3 + link.getLength();
					} else if (link.getAttributes().getAttribute("RKL").equals("P")) {
						demandRKL_P_depot3++;
						distanceRKL_P_depot3 = distanceRKL_P_depot3 + link.getLength();
					}
				}
				if (link.getAttributes().getAttribute("vehicleDepot").equals("R4")) {

					if (link.getAttributes().getAttribute("RKL").equals("A1a")) {
						demandRKL_A1a_depot4++;
						distanceRKL_A1a_depot4 = distanceRKL_A1a_depot4 + link.getLength();
					} else if (link.getAttributes().getAttribute("RKL").equals("A1b")) {
						demandRKL_A1b_depot4++;
						distanceRKL_A1b_depot4 = distanceRKL_A1b_depot4 + link.getLength();
					} else if (link.getAttributes().getAttribute("RKL").equals("A2a")) {
						demandRKL_A2a_depot4++;
						distanceRKL_A2a_depot4 = distanceRKL_A2a_depot4 + link.getLength();
					} else if (link.getAttributes().getAttribute("RKL").equals("A2b")) {
						demandRKL_A2b_depot4++;
						distanceRKL_A2b_depot4 = distanceRKL_A2b_depot4 + link.getLength();
					} else if (link.getAttributes().getAttribute("RKL").equals("A3")) {
						demandRKL_A3_depot4++;
						distanceRKL_A3_depot4 = distanceRKL_A3_depot4 + link.getLength();
					} else if (link.getAttributes().getAttribute("RKL").equals("A4")) {
						demandRKL_A4_depot4++;
						distanceRKL_A4_depot4 = distanceRKL_A4_depot4 + link.getLength();
					} else if (link.getAttributes().getAttribute("RKL").equals("B")) {
						demandRKL_B_depot4++;
						distanceRKL_B_depot4 = distanceRKL_B_depot4 + link.getLength();
					} else if (link.getAttributes().getAttribute("RKL").equals("C")) {
						demandRKL_C_depot4++;
						distanceRKL_C_depot4 = distanceRKL_C_depot4 + link.getLength();
					} else if (link.getAttributes().getAttribute("RKL").equals("P")) {
						demandRKL_P_depot4++;
						distanceRKL_P_depot4 = distanceRKL_P_depot4 + link.getLength();
					}
				}
				if (link.getAttributes().getAttribute("vehicleDepot").equals("R5")) {

					if (link.getAttributes().getAttribute("RKL").equals("A1a")) {
						demandRKL_A1a_depot5++;
						distanceRKL_A1a_depot5 = distanceRKL_A1a_depot5 + link.getLength();
					} else if (link.getAttributes().getAttribute("RKL").equals("A1b")) {
						demandRKL_A1b_depot5++;
						distanceRKL_A1b_depot5 = distanceRKL_A1b_depot5 + link.getLength();
					} else if (link.getAttributes().getAttribute("RKL").equals("A2a")) {
						demandRKL_A2a_depot5++;
						distanceRKL_A2a_depot5 = distanceRKL_A2a_depot5 + link.getLength();
					} else if (link.getAttributes().getAttribute("RKL").equals("A2b")) {
						demandRKL_A2b_depot5++;
						distanceRKL_A2b_depot5 = distanceRKL_A2b_depot5 + link.getLength();
					} else if (link.getAttributes().getAttribute("RKL").equals("A3")) {
						demandRKL_A3_depot5++;
						distanceRKL_A3_depot5 = distanceRKL_A3_depot5 + link.getLength();
					} else if (link.getAttributes().getAttribute("RKL").equals("A4")) {
						demandRKL_A4_depot5++;
						distanceRKL_A4_depot5 = distanceRKL_A4_depot5 + link.getLength();
					} else if (link.getAttributes().getAttribute("RKL").equals("B")) {
						demandRKL_B_depot5++;
						distanceRKL_B_depot5 = distanceRKL_B_depot5 + link.getLength();
					} else if (link.getAttributes().getAttribute("RKL").equals("C")) {
						demandRKL_C_depot5++;
						distanceRKL_C_depot5 = distanceRKL_C_depot5 + link.getLength();
					} else if (link.getAttributes().getAttribute("RKL").equals("P")) {
						demandRKL_P_depot5++;
						distanceRKL_P_depot5 = distanceRKL_P_depot5 + link.getLength();
					}
				}
			}
		}
		try {
			BufferedWriter writer;
			File file;
			file = new File("scenarios/StreetCleaning/analyzeOfDemand.txt");
			writer = new BufferedWriter(new FileWriter(file, true));
			String now = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date());
			writer.write("Analyse erstellt am: " + now + "\n\n");

			writer.write("RKL\tAnzahl Links Hof 1\tStrecke Links" + "\n\n");
			writer.write("A1a\t" + demandRKL_A1a_depot1 + "\t" + distanceRKL_A1a_depot1 / 1000 + "\t" + demandRKL_A1a_depot2 + "\t" + distanceRKL_A1a_depot2 / 1000 +"\t" + demandRKL_A1a_depot3 + "\t" + distanceRKL_A1a_depot3 / 1000+"\t" + demandRKL_A1a_depot4 + "\t" + distanceRKL_A1a_depot4 / 1000+"\t" + demandRKL_A1a_depot5 + "\t" + distanceRKL_A1a_depot5 / 1000+"\n");
			writer.write("A1b\t" + demandRKL_A1b_depot1 + "\t" + distanceRKL_A1b_depot1 / 1000+ "\t" + demandRKL_A1b_depot2 + "\t" + distanceRKL_A1b_depot2 / 1000 +"\t" + demandRKL_A1b_depot3 + "\t" + distanceRKL_A1b_depot3 / 1000+"\t" + demandRKL_A1b_depot4 + "\t" + distanceRKL_A1b_depot4 / 1000+"\t" + demandRKL_A1b_depot5 + "\t" + distanceRKL_A1b_depot5 / 1000+"\n");
			writer.write("A2a\t" + demandRKL_A2a_depot1 + "\t" + distanceRKL_A2a_depot1 / 1000 + "\t" + demandRKL_A2a_depot2 + "\t" + distanceRKL_A2a_depot2 / 1000 +"\t" + demandRKL_A2a_depot3 + "\t" + distanceRKL_A2a_depot3 / 1000+"\t" + demandRKL_A2a_depot4 + "\t" + distanceRKL_A2a_depot4 / 1000+"\t" + demandRKL_A2a_depot5 + "\t" + distanceRKL_A2a_depot5 / 1000+"\n");
			writer.write("A2b\t" + demandRKL_A2b_depot1 + "\t" + distanceRKL_A2b_depot1 / 1000 + "\t" + demandRKL_A2b_depot2 + "\t" + distanceRKL_A2b_depot2 / 1000 +"\t" + demandRKL_A2b_depot3 + "\t" + distanceRKL_A2b_depot3 / 1000+"\t" + demandRKL_A2b_depot4 + "\t" + distanceRKL_A2b_depot4 / 1000+"\t" + demandRKL_A2b_depot5 + "\t" + distanceRKL_A2b_depot5 / 1000+"\n");
			writer.write("A3\t" + demandRKL_A3_depot1 + "\t" + distanceRKL_A3_depot1 / 1000 + "\t" + demandRKL_A3_depot2 + "\t" + distanceRKL_A3_depot2 / 1000 +"\t" + demandRKL_A3_depot3 + "\t" + distanceRKL_A3_depot3 / 1000+"\t" + demandRKL_A3_depot4 + "\t" + distanceRKL_A3_depot4 / 1000+"\t" + demandRKL_A3_depot5 + "\t" + distanceRKL_A3_depot5 / 1000+"\n");
			writer.write("A4\t" + demandRKL_A4_depot1 + "\t" + distanceRKL_A4_depot1 / 1000 + "\t" + demandRKL_A4_depot2 + "\t" + distanceRKL_A4_depot2 / 1000 +"\t" + demandRKL_A4_depot3 + "\t" + distanceRKL_A4_depot3 / 1000+"\t" + demandRKL_A4_depot4 + "\t" + distanceRKL_A4_depot4 / 1000+"\t" + demandRKL_A4_depot5 + "\t" + distanceRKL_A4_depot5 / 1000+"\n");
			writer.write("B\t" + demandRKL_B_depot1 + "\t" + distanceRKL_B_depot1 / 1000 + "\t" + demandRKL_B_depot2 + "\t" + distanceRKL_B_depot2 / 1000 +"\t" + demandRKL_B_depot3 + "\t" + distanceRKL_B_depot3 / 1000+"\t" + demandRKL_B_depot4 + "\t" + distanceRKL_B_depot4 / 1000+"\t" + demandRKL_B_depot5 + "\t" + distanceRKL_B_depot5 / 1000+"\n");
			writer.write("C\t" + demandRKL_C_depot1 + "\t" + distanceRKL_C_depot1 / 1000 + "\t" + demandRKL_C_depot2 + "\t" + distanceRKL_C_depot2 / 1000 +"\t" + demandRKL_C_depot3 + "\t" + distanceRKL_C_depot3 / 1000+"\t" + demandRKL_C_depot4 + "\t" + distanceRKL_C_depot4 / 1000+"\t" + demandRKL_C_depot5 + "\t" + distanceRKL_C_depot5 / 1000+"\n");
			writer.write("P\t" + demandRKL_P_depot1 + "\t" + distanceRKL_P_depot1 / 1000 + "\t" + demandRKL_P_depot2 + "\t" + distanceRKL_P_depot2 / 1000 +"\t" + demandRKL_P_depot3 + "\t" + distanceRKL_P_depot3 / 1000+"\t" + demandRKL_P_depot4 + "\t" + distanceRKL_P_depot4 / 1000+"\t" + demandRKL_P_depot5 + "\t" + distanceRKL_P_depot5 / 1000+"\n");

			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
