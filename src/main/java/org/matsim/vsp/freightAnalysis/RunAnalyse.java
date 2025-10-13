package org.matsim.vsp.freightAnalysis;

import org.matsim.freight.carriers.analysis.CarriersAnalysis;

import java.io.IOException;

public class RunAnalyse {

	public static void main(String[] args) throws  IOException {
		
//		String runDir = "C:/Users/Ricardo/git/zeroCUTS/output/BusinessPassengerTraffic/vulkaneifel/commercialPersonTraffic_2pct_2023-08-30_32900_0.005/";
//		String runDir = "C:/Users/Ricardo/Desktop/Cluster/Vulkaneifel/base.completeSmallScaleCommercialTraffic_25pct_2023-09-01_60149_0.005/";
//		String runDir = "C:/Users/Ricardo/Desktop/Cluster/Berlin/completeSmallScaleCommercialTraffic_25pct_2023-09-04_30655_0.005/";
//		String runDir = "../matsim-berlin/output/commercialPersonTraffic/";
//		String runDir = "../matsim-berlin/output/goodsTraffic/";
        String runDir = "C:/git-and-svn/shared-svn/projects/freight/studies/UpdateEventsfromEarlierStudies/foodRetailing_wo_rangeConstraint/71a_ICEV_NwCE_BVWP_10000it_DCoff_noTax/";
		//		String runId = "tax40CV.";
//		String networkCRS = TransformationFactory.DHDN_GK4; //Berlin
		String networkCRS = "EPSG:25832"; //vulkaneifel

//		String runId = "commercialPersonTraffic.";
		String runId = "goodsTraffic.";
		enum FreightAnalysisVersion {
			eventBased, oldVersion
		}
		FreightAnalysisVersion slectedFreightAnalysisVersion = FreightAnalysisVersion.eventBased;
		switch (slectedFreightAnalysisVersion) {
			case eventBased -> {
				CarriersAnalysis freightAnalysis = new CarriersAnalysis(runDir);
				freightAnalysis.runCarrierAnalysis(CarriersAnalysis.CarrierAnalysisType.carriersAndEvents);
			}
			case oldVersion -> {
				if (runId != null)
					FreightAnalyse.main(new String[] { runDir, "true", networkCRS, runId});
				else
					FreightAnalyse.main(new String[] { runDir, "true", networkCRS});
			}
		}


	}

}
