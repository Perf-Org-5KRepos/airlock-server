package tests.restapi.proddevsplit;

import java.io.IOException;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.apache.wink.json4j.JSONArray;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;
import tests.restapi.RuntimeDateUtilities;
import tests.restapi.RuntimeRestApi;
import tests.restapi.SeasonsRestApi;

public class MaxFeatureOnInMTXInsideMTX {
	protected String seasonID;
	protected String mtxID1;
	protected String mtxID2;
	protected String featureID1;
	protected String featureID2;
	protected String featureID3;
	protected String featureID4;
	protected String featureID5;
	protected String filePath;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	protected SeasonsRestApi s;
	protected String productID;
	protected String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath ;
		p = new ProductsRestApi();
		p.setURL(m_url);
		s = new SeasonsRestApi();
		f = new FeaturesRestApi();
		s.setURL(m_url);
		f.setURL(m_url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
	}
	
	//TODO
	/*
	 * for all tests here get the files content
	 * 
	 */


	@Test (description="Create an mtx group in development under ROOT")
	public void createFirstMTX() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		
		String mtx = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		 mtxID1 = f.addFeature(seasonID, mtx, "ROOT", sessionToken);
		 String feature1 = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		 featureID1 = f.addFeature(seasonID, feature1, mtxID1, sessionToken);
		 String feature2 = FileUtils.fileToString(filePath + "feature2.txt", "UTF-8", false);
		 featureID2 = f.addFeature(seasonID, feature2, mtxID1, sessionToken);
		
		//check if files were changed
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");
		
	}
	
	@Test (dependsOnMethods = "createFirstMTX", description="Create an mtx group in development under ROOT")
	public void createSecondMTX() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		
		String mtx = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
		 mtxID2 = f.addFeature(seasonID, mtx, featureID1, sessionToken);
		 String feature1 = FileUtils.fileToString(filePath + "feature3.txt", "UTF-8", false);
		 featureID3 = f.addFeature(seasonID, feature1, mtxID2, sessionToken);
		 String feature2 = FileUtils.fileToString(filePath + "feature4.txt", "UTF-8", false);
		 featureID4 = f.addFeature(seasonID, feature2, mtxID2, sessionToken);
		
		//check if files were changed
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

		
		//check development file content
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		JSONArray features = root.getJSONArray("features");
		
		JSONArray mtx1 = features.getJSONObject(0).getJSONArray("features");
		JSONArray mtx2 = mtx1.getJSONObject(0).getJSONArray("features");
		
		Assert.assertTrue(mtx2.size()==1, "The inner mtx group is not under the parent feature");
		
	}
	
	
	@Test (dependsOnMethods = "createSecondMTX", description="Move feature from development to production")
	public void moveParentFeatureToProd() throws JSONException, IOException, InterruptedException{
		String feature = f.getFeature(featureID1, sessionToken);
		JSONObject json = new JSONObject(feature);
		json.put("stage", "PRODUCTION");
		String response  = f.updateFeature(seasonID, featureID1, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"),  "Feature was not moved to production");
	}
	
	@Test (dependsOnMethods = "moveParentFeatureToProd", description="Add the 3-d feature in production to the innermost mtx")
	public void createFeatureInProd() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("stage", "PRODUCTION");
		json.put("name", "prodFeature");
		featureID5 = f.addFeature(seasonID, json.toString(), mtxID2, sessionToken);
		
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not updated");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");

		
		//check production file content
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseProd.message);
		JSONArray features = root.getJSONArray("features");

		Assert.assertTrue(features.getJSONObject(0).getString("uniqueId").equals(mtxID1), "Incorrect feature uniqueId in the development file");
		Assert.assertTrue(getNumberOfFeature(features)==1, "Incorrect number of children in mtx group");
	
		//check development file content
		root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		features = root.getJSONArray("features");
		Assert.assertTrue(getNumberOfFeature(features)==3, "Incorrect number of children in mtx group");

		
	}
	
	@Test (dependsOnMethods = "createFeatureInProd", description="Update maxFeatureOn")
	public void changeMaxFeaturesOn() throws JSONException, IOException, InterruptedException{
		String dateFormat = f.setDateFormat();
		
		String parent = f.getFeature(mtxID2, sessionToken);
		JSONObject json = new JSONObject(parent);
		json.put("maxFeaturesOn", 3);
		String reponse = f.updateFeature(seasonID, mtxID2, json.toString(), sessionToken);
		Assert.assertFalse(reponse.contains("error"), "maxFeaturesOn was not updated");

		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not updated");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");

		//check production file content
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseProd.message);
		JSONArray rootFeature = root.getJSONArray("features");
		JSONArray mtx1 = rootFeature.getJSONObject(0).getJSONArray("features");
		JSONArray mtx2 = mtx1.getJSONObject(0).getJSONArray("features");
		Assert.assertTrue(mtx2.getJSONObject(0).getInt("maxFeaturesOn")==3, "maxFeaturesOn was not updated in the development file");
	
		//check development file content
		root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		rootFeature = root.getJSONArray("features");
		mtx1 = rootFeature.getJSONObject(0).getJSONArray("features");
		mtx2 = mtx1.getJSONObject(0).getJSONArray("features");
		Assert.assertTrue(mtx2.getJSONObject(0).getInt("maxFeaturesOn")==3, "maxFeaturesOn was not updated in the production file");
	}
	
	
	private int getNumberOfFeature(JSONArray features) throws JSONException{
		//get the number of children in the innermost mtx group
		JSONArray mtx1 = features.getJSONObject(0).getJSONArray("features");
		JSONArray mtx2 = mtx1.getJSONObject(0).getJSONArray("features");
		return mtx2.getJSONObject(0).getJSONArray("features").size();
	}
	

	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
