package org.matsim.vsp.streetCleaning.prepare;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;

public class analyzeDemand {

	private static final String networkWithClaeiningInformation = "scenarios/networks/networkBerlinWithRKL.xml.gz";

	public static void main(String[] args) {

		// read Berlin network
		Network network = NetworkUtils.readNetwork(networkWithClaeiningInformation);

		int demandRKL_A1a = 0;
		int demandRKL_A1b = 0;
		int demandRKL_A2a = 0;
		int demandRKL_A2b = 0;
		int demandRKL_A3 = 0;
		int demandRKL_A4 = 0;
		int demandRKL_B = 0;
		int demandRKL_C = 0;
		int demandRKL_P = 0;

		for (Link link : network.getLinks().values()) {
			if (link.getAttributes().getAsMap().containsKey("RKL")) {

				if (link.getAttributes().getAttribute("RKL").equals("A1a"))
					demandRKL_A1a++;
				else if (link.getAttributes().getAttribute("RKL").equals("A1b"))
					demandRKL_A1b++;
				else if (link.getAttributes().getAttribute("RKL").equals("A2a"))
					demandRKL_A2a++;
				else if (link.getAttributes().getAttribute("RKL").equals("A2b"))
					demandRKL_A2b++;
				else if (link.getAttributes().getAttribute("RKL").equals("A3"))
					demandRKL_A3++;
				else if (link.getAttributes().getAttribute("RKL").equals("A4"))
					demandRKL_A4++;
				else if (link.getAttributes().getAttribute("RKL").equals("B"))
					demandRKL_B++;
				else if (link.getAttributes().getAttribute("RKL").equals("C"))
					demandRKL_C++;
				else if (link.getAttributes().getAttribute("RKL").equals("P"))
					demandRKL_P++;
			}
		}
		int assignedInput = demandRKL_A1a + demandRKL_A1b + demandRKL_A2a + demandRKL_A2b + demandRKL_A3 + demandRKL_A4
				+ demandRKL_B + demandRKL_C + demandRKL_P;
		System.out.println("Zugeordneter Input: " + assignedInput);
		System.out.println("Gelesenen Input A1a: " + demandRKL_A1a);
		System.out.println("Gelesenen Input A1b: " + demandRKL_A1b);
		System.out.println("Gelesenen Input A2a: " + demandRKL_A2a);
		System.out.println("Gelesenen Input A2b: " + demandRKL_A2b);
		System.out.println("Gelesenen Input A3: " + demandRKL_A3);
		System.out.println("Gelesenen Input A4: " + demandRKL_A4);
		System.out.println("Gelesenen Input B: " + demandRKL_B);
		System.out.println("Gelesenen Input C: " + demandRKL_C);
		System.out.println("Gelesenen Input P: " + demandRKL_P);

	}
}
