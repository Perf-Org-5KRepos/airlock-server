package tests.restapi.copy_import.copy;

import java.io.IOException;


import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.BranchesRestApi;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;


public class CopyMIXConfigurationSameSeason {
	protected String seasonID;
	protected String productID;
	protected String featureID1;
	protected String featureID2;
	protected String configID;
	private String mixConfigID;
	protected String filePath;
	protected String m_url;
	protected ProductsRestApi p;
	protected FeaturesRestApi f;
	private String sessionToken = "";
	protected AirlockUtils baseUtils;
	private String srcBranchID;
	private String destBranchID;
	private boolean runOnMaster;

	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "runOnMaster"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, Boolean runOnMaster) throws Exception{
		m_url = url;
		filePath = configPath;
		p = new ProductsRestApi();
		p.setURL(m_url);
		f = new FeaturesRestApi();
		f.setURL(m_url);

	    
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		try {
			if (runOnMaster) {
				srcBranchID = BranchesRestApi.MASTER;
			} else {
				srcBranchID = baseUtils.createBranchInExperiment(analyticsUrl);
			}
		}catch (Exception e){
			srcBranchID = null;
		}

	}
	
	/*
	MIX Config under feature - allowed
	MIX Config under config - allowed
	MIX Config under mix of configs - allowed
	MIX Config under root - not allowed
	MIX Config under mix of features - not allowed
		
	 */
	
	@Test (description="Copy mix configuration under another feature in the same season. First, copy without namesuffix, then copy with namesuffix")
	public void copyConfigurationUnderFeature() throws IOException, JSONException{
		String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		featureID1 = f.addFeatureToBranch(seasonID, srcBranchID, feature1, "ROOT", sessionToken);
		Assert.assertFalse(featureID1.contains("error"), "Feature was not added to the season");
		
		String configurationMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		mixConfigID = f.addFeatureToBranch(seasonID, srcBranchID, configurationMix, featureID1, sessionToken);
		Assert.assertFalse(mixConfigID.contains("error"), "Feature was not added to the season");

		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		configID = f.addFeatureToBranch(seasonID, srcBranchID, configuration, mixConfigID, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Feature was not added to the season");
				
		String feature2 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		featureID2 = f.addFeatureToBranch(seasonID, srcBranchID, feature2, "ROOT", sessionToken);
		Assert.assertFalse(featureID2.contains("error"), "Feature was not added to the season");
		
		//should fail copy without suffix
		String response = f.copyItemBetweenBranches(mixConfigID, featureID2, "ACT", null, null, sessionToken, srcBranchID, srcBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Feature was copied with existing name ");

		
		response = f.copyItemBetweenBranches(mixConfigID, featureID2, "ACT", null, "suffix1", sessionToken, srcBranchID, srcBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Feature was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newFeature = f.getFeatureFromBranch(result.getString("newSubTreeId"), srcBranchID, sessionToken);
		String oldFeature = f.getFeatureFromBranch(mixConfigID, srcBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newFeature), new JSONObject(oldFeature)));

	}
	
	@Test (dependsOnMethods="copyConfigurationUnderFeature", description="Copy mix configuration under mix feature in the same season. First, copy without namesuffix, then copy with namesuffix")
	public void copyConfigurationUnderMixFeature() throws IOException{
		String featureMix = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		String mixId = f.addFeatureToBranch(seasonID, srcBranchID, featureMix, featureID2, sessionToken);
		Assert.assertFalse(mixId.contains("error"), "Feature was not added to the season" + mixId);
						
		String response = f.copyItemBetweenBranches(mixConfigID, mixId, "ACT", null, null, sessionToken, srcBranchID, srcBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuraiton mix was copied with existing name ");
		
		response = f.copyItemBetweenBranches(mixConfigID, mixId, "ACT", null, "suffix2", sessionToken, srcBranchID, srcBranchID);
		Assert.assertTrue(response.contains("error"), "Configuraiton mix was copied under features mix ");
	}
	
	@Test (dependsOnMethods="copyConfigurationUnderMixFeature", description="Copy mix configuration under root in the same season. First, copy without namesuffix, then copy with namesuffix")
	public void copyConfigurationUnderRoot() throws IOException{
		String rootId = f.getBranchRootId(seasonID, srcBranchID, sessionToken);
		
		String response = f.copyItemBetweenBranches(mixConfigID, rootId, "ACT", null, null, sessionToken, srcBranchID, srcBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuraiton mix was copied under root ");
		
		response = f.copyItemBetweenBranches(mixConfigID, rootId, "ACT", null, "suffix3", sessionToken, srcBranchID, srcBranchID);
		Assert.assertTrue(response.contains("error"), "Configuraiton mix was copied under root");
	}
	
	
	@Test (dependsOnMethods="copyConfigurationUnderRoot", description="Copy mix configuration under itself in the same season. First, copy without namesuffix, then copy with namesuffix")
	public void copyConfigurationUnderItself() throws IOException, JSONException{
		//should fail copy without suffix

		String response = f.copyItemBetweenBranches(mixConfigID, mixConfigID, "ACT", null, null, sessionToken, srcBranchID, srcBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuraiton mix was copied with existing name ");

		response = f.copyItemBetweenBranches(mixConfigID, mixConfigID, "ACT", null, "suffix4", sessionToken, srcBranchID, srcBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Configuration mix was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newFeature = f.getFeatureFromBranch(result.getString("newSubTreeId"), srcBranchID, sessionToken);
		JSONObject oldFeature = new JSONObject(f.getFeatureFromBranch(mixConfigID, srcBranchID, sessionToken));
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newFeature), oldFeature.getJSONArray("configurationRules").getJSONObject(1)));


	}
	
	@Test (dependsOnMethods="copyConfigurationUnderItself", description="Copy mix configuration under configuration in the same feature in the same season. First, copy without namesuffix, then copy with namesuffix")
	public void copyConfigurationUnderConfiguration() throws IOException, JSONException{
		String configuration = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
		String configID2 = f.addFeatureToBranch(seasonID, srcBranchID, configuration, featureID1, sessionToken);
		Assert.assertFalse(configID.contains("error"), "Feature was not added to the season");
				
		//should fail copy without suffix
		String response = f.copyItemBetweenBranches(mixConfigID, configID2, "ACT", null, null, sessionToken, srcBranchID, srcBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuraiton mix was copied with existing name ");
		
		response = f.copyItemBetweenBranches(mixConfigID, configID2, "ACT", null, "suffix5", sessionToken, srcBranchID, srcBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Configuraiton mix was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newFeature = f.getFeatureFromBranch(result.getString("newSubTreeId"), srcBranchID, sessionToken);
		String oldFeature = f.getFeatureFromBranch(mixConfigID, srcBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newFeature), new JSONObject(oldFeature)));

	}

	@Test (dependsOnMethods="copyConfigurationUnderConfiguration", description="Copy mix configuration under mix configuration in the same season. First, copy without namesuffix, then copy with namesuffix")
	public void copySingleFeatureUnderMixConfiguration() throws IOException, JSONException{
		String configuration = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		String mixConfigID2 = f.addFeatureToBranch(seasonID, srcBranchID, configuration, featureID2, sessionToken);
		Assert.assertFalse(mixConfigID2.contains("error"), "Feature was not added to the season");
			
		String response = f.copyItemBetweenBranches(mixConfigID, mixConfigID2, "ACT", null, null, sessionToken, srcBranchID, srcBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuraiton mix was copied with existing name ");
		
		response = f.copyItemBetweenBranches(mixConfigID, mixConfigID2, "ACT", null, "suffix6", sessionToken, srcBranchID, srcBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Configuration mix was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newFeature = f.getFeatureFromBranch(result.getString("newSubTreeId"), srcBranchID, sessionToken);
		String oldFeature = f.getFeatureFromBranch(mixConfigID, srcBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newFeature), new JSONObject(oldFeature)));

	}
	
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}