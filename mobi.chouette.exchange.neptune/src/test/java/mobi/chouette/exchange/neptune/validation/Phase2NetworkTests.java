package mobi.chouette.exchange.neptune.validation;

import mobi.chouette.exchange.validator.report.CheckPoint;

import org.testng.annotations.Test;

public class Phase2NetworkTests extends ValidationTests {

	
	@Test(groups = { "Phase 2 network" }, description = "line missing in network's list")
	public void verifyTest_2_Network_1() throws Exception {
		
		verifyValidation( "2-NEPTUNE-Network-1.xml", "2-NEPTUNE-Network-1",CheckPoint.SEVERITY.WARNING, CheckPoint.RESULT.NOK);

	}


}