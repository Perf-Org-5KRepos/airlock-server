package tests.restapi.scenarios.experiment_and_branch_validation;

import java.io.IOException;


import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

// delete from schema a required field used in branch
public class UpdateSchemaInUseByFeature
{
	protected Config config;

	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword",  "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws IOException
	{
		config = new Config(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		config.stage = "PRODUCTION";
		
	}

	@Test ( description="init branch")
	public void initBranch() throws Exception
	{
		config.addSeason( "1.1.1");
		config.updateSchema("inputSchemas/inputSchema_update_device_locale_to_production.txt");

		String branchID = config.addBranch("branch1", "experiments/branch1.txt");
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

		String featureID = config.addBranchFeature(branchID, "1.1.1", "feature1.txt", "context.device.locale !== undefined");
		Assert.assertFalse(featureID.contains("error"), "Feature was not added to the season");
	}

	@Test (dependsOnMethods = "initBranch", description="update input schema field to development used in branch rule")
	public void updateSchema() throws Exception
	{
        String response =  config.updateSchema("inputSchemas/inputSchema_update_device_locale_to_development.txt");
        Assert.assertTrue(response.contains("error"), "Schema should not be allowed to change - " + response);
	}

	@AfterTest
	private void reset(){
		config.reset();
	}
}
