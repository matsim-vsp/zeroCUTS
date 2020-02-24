package org.matsim.vsp.streetCleaning.prepare;

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
		
		int demandRKL_A1a_depot2 = 0;
		int demandRKL_A1b_depot2 = 0;
		int demandRKL_A2a_depot2 = 0;
		int demandRKL_A2b_depot2 = 0;
		int demandRKL_A3_depot2 = 0;
		int demandRKL_A4_depot2 = 0;
		int demandRKL_B_depot2 = 0;
		int demandRKL_C_depot2 = 0;
		int demandRKL_P_depot2 = 0;
		
		int demandRKL_A1a_depot3 = 0;
		int demandRKL_A1b_depot3 = 0;
		int demandRKL_A2a_depot3 = 0;
		int demandRKL_A2b_depot3 = 0;
		int demandRKL_A3_depot3 = 0;
		int demandRKL_A4_depot3 = 0;
		int demandRKL_B_depot3 = 0;
		int demandRKL_C_depot3 = 0;
		int demandRKL_P_depot3 = 0;
		
		int demandRKL_A1a_depot4 = 0;
		int demandRKL_A1b_depot4 = 0;
		int demandRKL_A2a_depot4 = 0;
		int demandRKL_A2b_depot4 = 0;
		int demandRKL_A3_depot4 = 0;
		int demandRKL_A4_depot4 = 0;
		int demandRKL_B_depot4 = 0;
		int demandRKL_C_depot4 = 0;
		int demandRKL_P_depot4 = 0;
		
		int demandRKL_A1a_depot5 = 0;
		int demandRKL_A1b_depot5 = 0;
		int demandRKL_A2a_depot5 = 0;
		int demandRKL_A2b_depot5 = 0;
		int demandRKL_A3_depot5 = 0;
		int demandRKL_A4_depot5 = 0;
		int demandRKL_B_depot5 = 0;
		int demandRKL_C_depot5 = 0;
		int demandRKL_P_depot5 = 0;

		for (Link link : network.getLinks().values()) {
			if (link.getAttributes().getAsMap().containsKey("RKL")) {
				if (link.getAttributes().getAttribute("vehicleDepot").equals("R1")) {

				if (link.getAttributes().getAttribute("RKL").equals("A1a"))
					demandRKL_A1a_depot1++;
				else if (link.getAttributes().getAttribute("RKL").equals("A1b"))
					demandRKL_A1b_depot1++;
				else if (link.getAttributes().getAttribute("RKL").equals("A2a"))
					demandRKL_A2a_depot1++;
				else if (link.getAttributes().getAttribute("RKL").equals("A2b"))
					demandRKL_A2b_depot1++;
				else if (link.getAttributes().getAttribute("RKL").equals("A3"))
					demandRKL_A3_depot1++;
				else if (link.getAttributes().getAttribute("RKL").equals("A4"))
					demandRKL_A4_depot1++;
				else if (link.getAttributes().getAttribute("RKL").equals("B"))
					demandRKL_B_depot1++;
				else if (link.getAttributes().getAttribute("RKL").equals("C"))
					demandRKL_C_depot1++;
				else if (link.getAttributes().getAttribute("RKL").equals("P"))
					demandRKL_P_depot1++;
			}}
		}
		int assignedInput_depot1 = demandRKL_A1a_depot1 + demandRKL_A1b_depot1 + demandRKL_A2a_depot1 + demandRKL_A2b_depot1 + demandRKL_A3_depot1 + demandRKL_A4_depot1
				+ demandRKL_B_depot1 + demandRKL_C_depot1 + demandRKL_P_depot1;
		System.out.println("Zugeordneter Input: " + assignedInput_depot1);
		System.out.println("Gelesenen Input A1a: " + demandRKL_A1a_depot1);
		System.out.println("Gelesenen Input A1b: " + demandRKL_A1b_depot1);
		System.out.println("Gelesenen Input A2a: " + demandRKL_A2a_depot1);
		System.out.println("Gelesenen Input A2b: " + demandRKL_A2b_depot1);
		System.out.println("Gelesenen Input A3: " + demandRKL_A3_depot1);
		System.out.println("Gelesenen Input A4: " + demandRKL_A4_depot1);
		System.out.println("Gelesenen Input B: " + demandRKL_B_depot1);
		System.out.println("Gelesenen Input C: " + demandRKL_C_depot1);
		System.out.println("Gelesenen Input P: " + demandRKL_P_depot1);

	}
}
