/*******************************************************************************
 * Copyright [2022] [IBM]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.eng.aisp.classifier.mvi;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.eng.aisp.AISPProperties;
import org.eng.aisp.classifier.Classification;
import org.eng.aisp.client.BaseHttpClient;
import org.eng.util.HttpUtil;
import org.eng.util.HttpUtil.HttpResponse;
import org.eng.util.MultipartUtility;

import com.google.gson.Gson;

public class MVIRESTClient implements Serializable {

	private static final long serialVersionUID = 68840760456321307L;

	public final static String MVI_HOST_ENVVAR_NAME = "VAPI_HOST";
	public final static String MVI_PORT_ENVVAR_NAME = "VAPI_PORT";
	public final static String MVI_APIKEY_ENVVAR_NAME = "VAPI_TOKEN";
	private static final String MVI_API_URL_ENVVAR_NAME = "VAPI_BASE_URI";

	public final static boolean Verbose = AISPProperties.instance().getProperty("mvi.client.verbose", false);

	
	private final static String MVI_IMAGE_FORMAT_PROPERTY_NAME = "mvi.client.image.format";
	private final static String DEFAULT_MVI_IMAGE_FORMAT = "png";	// jpg also works
	private final static String IMAGE_FORMAT = AISPProperties.instance().getProperty(MVI_IMAGE_FORMAT_PROPERTY_NAME, DEFAULT_MVI_IMAGE_FORMAT); 

//	private final static String IMAGE_FORMAT = "jpg";	// lossy. not good.
	private final static String IMAGE_MIME_TYPE = "image/" + IMAGE_FORMAT;
	private final static String IMAGE_NAME = "noname." + IMAGE_FORMAT;

	private String dataSetsURL;
	private String dlTasksURL;
	private String trainedModelsURL;
	private String dltasksURL;
	private String dlapisURL;
	private String webapisURL;
	private Map<String,String>headers = new HashMap<String,String>();
	
	private transient CategoryCache categoryCache; 
	
	private class CategoryCache {
		/**
		 * A map of data set ids, keyed to maps of category names key to category ids.
		 * datasetid -> category_name -> category_id
		 */
		private final Map<String,Map<String,String>> categories = new HashMap<>();

		/**
		 * Get and create if necessary the id of the given category/label in the given dataset.
		 * @param dsID
		 * @param labelValue
		 * @return
		 * @throws IOException
		 */
		public String getEstablishedCategoryID(String dsID, String labelValue) throws IOException {
			Map<String,String> categories = this.categories.get(dsID);
			String categoryID; 
			if (categories == null) 
				categories = this.loadCategories(dsID);
			categoryID = categories.get(labelValue);
			if (categoryID == null)
				categoryID = this.addCategory(dsID, labelValue);
			return categoryID;
		}

		/**
		 * Add the given category/label value to the given data set if it doesn't already exist.
		 * @param dsID
		 * @param labelValue
		 * @return id of category/label.
		 * @throws IOException
		 */
		private String addCategory(String dsID, String labelValue) throws IOException {
			Map<String,String> dsCategories = this.categories.get(dsID);
			if (dsCategories == null) {
				dsCategories = new HashMap<String,String>();
				this.categories.put(dsID, dsCategories);
			}
			String id = dsCategories.get(labelValue);
			if (id == null) {
				id = MVIRESTClient.this.createCategory(dsID, labelValue);
				dsCategories.put(labelValue,id);
			}
			return id;
		}

		private Map<String, String> loadCategories(String dsID) throws IOException {
			Map<String, String> dsCategories = MVIRESTClient.this.getCategories(dsID);
			this.categories.put(dsID, dsCategories);
			return dsCategories;
		}

	}
	
	private void initCategoryCache() {
		if (categoryCache == null) {
			categoryCache = new CategoryCache();
		}
	}
	
	/**
	 * Create the instance reading the connection info from {@value #MVI_HOST_ENVVAR_NAME}, {@value #MVI_PORT_ENVVAR_NAME} (or 443 if not present) 
	 * or {@value #MVI_API_URL_ENVVAR_NAME}, and {@value #MVI_APIKEY_ENVVAR_NAME}
	 * environment variables.
	 */
	public MVIRESTClient() {
		this(null,0, null);

	}
	
	private void vlog(String msg) {
		if (Verbose)  {
			System.out.println(this.getClass().getSimpleName() + ": " + msg);
			System.out.flush();
		}
	}

	/**
	 * Create the instance to talk to the given host over the given port using the given api key.
	 * @param host if null, look in {@value #MVI_HOST_ENVVAR_NAME} and {@value #MVI_API_URL_ENVVAR_NAME} environment variables. If not provided there, the throw an IllegalArgumentException.
	 * @param port if null and {@value #MVI_API_URL_ENVVAR_NAME} env var is not set, look in {@value #MVI_PORT_ENVVAR_NAME} environment variable. If not provided there, then use 443. 
	 * @param apiKey if null, look in {@value #MVI_APIKEY_ENVVAR_NAME} environment variable. If not provided there, the throw an IllegalArgumentException.
	 */
	public MVIRESTClient(String host, int port, String apiKey) {
		String baseAPIURL = getBaseAPIURL(host, port);
		if (apiKey == null)
			apiKey = getEnvVar("MVI API key", MVI_APIKEY_ENVVAR_NAME, true);
		vlog("Establishing connection to MVI server at " + baseAPIURL);
		dataSetsURL = baseAPIURL + "datasets";
		dlTasksURL = baseAPIURL + "dltasks";
		trainedModelsURL = baseAPIURL + "trained-models";
		dlapisURL = baseAPIURL + "dlapis";
		dltasksURL = baseAPIURL + "dltasks";
		webapisURL = baseAPIURL + "webapis";
		headers.put("X-Auth-Token", apiKey) ;
	}

	/**
	 * @param host
	 * @param port
	 * @return
	 * @throws NumberFormatException
	 */
	private static String getBaseAPIURL(String host, int port) throws NumberFormatException {

		String baseAPIURL = null;
		if (host == null) {
			host = getEnvVar("MVI host", MVI_HOST_ENVVAR_NAME, true);
			if (host == null)  {
				baseAPIURL = getEnvVar("MVI host", MVI_API_URL_ENVVAR_NAME, true);
				if (baseAPIURL == null)
					throw new IllegalArgumentException("MVI host/api must be specified with one of " 
							+MVI_HOST_ENVVAR_NAME + " or " + MVI_API_URL_ENVVAR_NAME  + " env vars.");
				return baseAPIURL;
			}
		}
		if (port <= 0) {
			String ps = getEnvVar("MVI port", MVI_PORT_ENVVAR_NAME, false);
			if (ps == null)
				port = 443;
			else
				port = Integer.parseInt(ps);
		}
		BaseHttpClient client = new BaseHttpClient(host, port, "api");
		baseAPIURL = client.getBaseAPIURL();
		return baseAPIURL;
	}

	private static String getEnvVar(String paramName, String envVarName, boolean failIfNotFound) {
		String value = System.getenv(envVarName);
		if (value == null && failIfNotFound) 
			throw new IllegalArgumentException(paramName + " parameter default was not found in environment variable " + envVarName);
		return value;
	}

	private static class CreateDataSetRequest {
		public String name;
		public CreateDataSetRequest(String name) {
			this.name = name;
		}
	}
	private static class CreateDataSetResponse {
		public String result;
		public String dataset_id;
		public String failure;
		public CreateDataSetResponse() { }
	}

	public String createDataSet(String name) throws IOException {
		vlog("Begin creating data set with name " + name);
//			POST https://ibmrobotics.visualinspection.mas-robotics-f72ef11f3ab089a8c677044eb28292cd-0000.us-east.containers.appdomain.cloud/api/datasets 
//	{User-Agent: python-requests/2.22.0,Accept-Encoding: gzip, deflate,Accept: */*,Connection: keep-alive,accept-language: en-US,
//	X-Auth-Token: 2meV-uoiG-vmvg-250d,Content-Length: 23,Content-Type: application/json} b'{"name": "cli-dataset"}'
//		String tt = HttpUtil.jsonGET(dataSetsURL, headers, String.class); 
		CreateDataSetResponse resp = HttpUtil.jsonPOST(dataSetsURL, headers, new CreateDataSetRequest(name), CreateDataSetResponse.class); 
		if (resp == null || resp.result == null || !resp.result.equals("success"))
			throw new IOException ("Could not create data set to store spectrograms: " + resp.failure);
		vlog("Done creating data set with name " + name + ". Id is" + resp.dataset_id);
		return resp.dataset_id;
	}

	private static class GetCategoriesResponse {
		public String _id;
		public String dataset_id;
		public String name;
		public GetCategoriesResponse() { }
	}


	public Map<String,String> getCategories(String dsID) throws IOException {
		String url = this.dataSetsURL + "/" + dsID + "/categories";
//		GetCategoriesResponse resp = HttpUtil.jsonGET(url, headers, GetCategoriesResponse.class); 
		List<GetCategoriesResponse> respList = HttpUtil.jsonGETList(url, headers, GetCategoriesResponse.class); 
//		if (resp == null || resp.result == null || !resp.result.equals("success"))
//			throw new IOException ("Could not get data set categories: " + resp.failure);
		Map<String,String> cats = new HashMap<String,String>(); 
		for (GetCategoriesResponse gcr : respList) {
			cats.put(gcr.name, gcr._id);
		}
		return cats;
	}
	
	private static class CreateCategoryRequest {
		public String name;
		public CreateCategoryRequest(String name) {
			this.name = name;
		}
	}
	private static class CreateCategoryResponse {
		public String result;
		public String dataset_category_id;
		public String failure;
		public CreateCategoryResponse() { }
	}

	/**
	 * Create the name category in the given dataset and return its id.
	 * @param dsID
	 * @param categoryName
	 * @return the id of the category
	 * @throws IOException
	 */
	public String createCategory(String dsID, String categoryName) throws IOException {
		String url = this.dataSetsURL + "/" + dsID + "/categories";
		CreateCategoryResponse resp = HttpUtil.jsonPOST(url, headers, new CreateCategoryRequest(categoryName), CreateCategoryResponse.class); 
		if (resp == null || resp.result == null || !resp.result.equals("success"))
			throw new IOException ("Could not create category: " + resp.failure);
//		this.getCategories(dsID);	// debugging
		return resp.dataset_category_id;
	}
	
	private static int rgbDoubleToInt(double r, double g, double b) {
		return ((int) (0xff * b)) | (((int) (0xff * g)) << 8 ) | (((int) (0xff * r)) << 16 ) ;
	}
	
	private static byte[] getSpectrogramAsImage(double[][] spectrogram, String imageFormat) throws IOException {
		int feVecSize = spectrogram[0].length;
		int numWind = spectrogram.length;

		BufferedImage image = new BufferedImage(numWind, feVecSize, BufferedImage.TYPE_INT_RGB);
		
		for(int j = 0; j<numWind; j++){
		    for(int k = 0; k<feVecSize; k++){
		    	double datum = spectrogram[j][k];
		    	if ((datum < 0.0) || (datum > 1.0))
		    		throw new IllegalArgumentException("The input parameter 'spectrogram' must be between 0.0 and 1.0.");
		    	int value = Color.HSBtoRGB( (float)(datum), (float)(1.0), (float)(1.0));
//		    	int value = Color.HSBtoRGB( (float)(datum * 0.67 + 0.67), (float)(1.0), (float)(1.0));
//		    	int value = Color.HSBtoRGB( (float)(-(float)j/numWind * 0.85 + 0.67), (float)(1.0), (float)(1.0));  //Used for generating the full range, for testing
//		    	int value = Color.HSBtoRGB( (float)(-datum * 0.85 + 0.67), (float)(1.0), (float)(1.0));  //This looks (visually) better and seems to be what is most commonly used
//		    	int value = rgbDoubleToInt(datum, datum, datum);  //This is for generating black/white images
		        image.setRGB(j, k, value);
		    }
	    }
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ImageIO.write(image, imageFormat, bos);
		return bos.toByteArray();
	}
	
	private static class BoundingBox {
		public int xmin, ymin, xmax, ymax;

		public BoundingBox() {
			this(0,0,0,0);
		}

		public BoundingBox(int xmin, int ymin, int xmax, int ymax) {
			this.xmin = xmin;
			this.ymin = ymin;
			this.xmax = xmax;
			this.ymax = ymax;
		}
		
	}

	private static class Label {
		public String name;
		public BoundingBox bndbox;

		public Label(String name, BoundingBox bndbox) {
			this.name = name;
			this.bndbox = bndbox;
		}
		
	}
	
	private static Gson gson = new Gson();

	/**
	 * Add the given spectrogram as an image to the given data set and attach the given label value.
	 * @param dsID id of data set to add the spectrogram image to.
	 * @param spectrogram the spectrogram. 1st dimension is feature index (i.e. time) with each element holding a single feature for the given time. 2nd dimension is feature vector index.
	 * @param labelValue the label to assign to the whole spectrogram
	 * @throws IOException 
	 */
	public void addFile(String dsID, double[][] spectrogram, String labelValue) throws IOException {
		vlog("Begin Adding file with label value " + labelValue + " to data set with id " + dsID);
		initCategoryCache();
		String category_id = this.categoryCache.getEstablishedCategoryID(dsID, labelValue);
//		POST https://ibmrobotics.visualinspection.mas-robotics-f72ef11f3ab089a8c677044eb28292cd-0000.us-east.containers.appdomain.cloud/api/datasets/20af5200-840d-4653-baec-67fe52b94478/files 
//			{User-Agent: python-requests/2.22.0,Accept-Encoding: gzip, deflate,Accept: */*,Connection: keep-alive,accept-language: en-US,
//			X-Auth-Token: 2meV-uoiG-vmvg-250d,Content-Length: 8364,Content-Type: 
//			multipart/form-data; boundary=da3ec8a82ad6c4ed9f904dd1f2cae9a6} b'--da3ec8a82ad6c4ed9f904dd1f2cae9a6\r\n
//			Content-Disposition: form-data; name="labels"; filename="labels"\r\n\r\n[ { "name" : "black.jpg", "bndbox":["xmin":0,"ymin":0,"xmax":100,"ymax":100] } ]\r\n
//		--da3ec8a82ad6c4ed9f904dd1f2cae9a6\r\nContent-Disposition: form-data; name="files"; filename="black.jpg"\r\n ....
		String url = this.dataSetsURL + "/" + dsID + "/files";
		MultipartUtility mpu = new MultipartUtility(url, headers, "UTF-8");	
//		BoundingBox bbox = new BoundingBox(0,0, spectrogram.length, spectrogram[0].length); 
//		Label label = new Label(labelValue, bbox);
//		mpu.addFormField("label", gson.toJson(label), "application/json"); 
		mpu.addFormField("category_name", labelValue, "text/plain");
		mpu.addFormField("category_id", category_id, "text/plain");
		byte[] image = getSpectrogramAsImage(spectrogram, IMAGE_FORMAT);
		mpu.addFilePart("files", IMAGE_NAME, IMAGE_MIME_TYPE, new ByteArrayInputStream(image));
		HttpResponse resp = mpu.finish();
		if (!resp.isSuccess())
			throw new IOException("Could not store spectrogram: " + resp.getErrorString());
		vlog("Done adding file with label value " + labelValue + " to data set with id " + dsID);
	}
	


	private static class DeleteDataSetResponse {
		String result;
		String fault;
		DeleteDataSetResponse() { }
	}

	public void deleteDataSet(String dsID) throws IOException {
		vlog("Begin deleting data set with id " + dsID);
		DeleteDataSetResponse resp = HttpUtil.jsonDELETE(dataSetsURL + "/" + dsID, headers, null, DeleteDataSetResponse.class); 
		if (resp.result == null || !resp.result.equals("success"))
			throw new IOException("Could not delete data set: " + resp.fault);
		vlog("Done deleting data set with id " + dsID);
	}


	private static class TrainModelRequest {
		String action;
		String usage;
		String dataset_id;
		String name;
		public TrainModelRequest(String action, String usage, String dataset_id, String name) {
			this.action = action;
			this.usage = usage;
			this.dataset_id = dataset_id;
			this.name = name;
		}
		
	}
	
	private static class TrainResponse {
		String result;
		String fail;
		String task_id;
		public TrainResponse() {}
	}
	/**
	 * Train a model on a given data set.
	 * @param dataSetID
	 * @param modelType
	 * @return the id of the trained (not deployed) model.
	 * @throws IOException 
	 */
	public String trainModel(String dataSetID, String modelType, String name) throws IOException {
		vlog("Begin training model named " + name);
//		POST https://ibmrobotics.visualinspection.mas-robotics-f72ef11f3ab089a8c677044eb28292cd-0000.us-east.containers.appdomain.cloud/api/dltasks/ 
//		{User-Agent: python-requests/2.22.0,Accept-Encoding: gzip, deflate,Accept: */*,Connection: keep-alive,accept-language: en-US,X-Auth-Token: 2meV-uoiG-vmvg-250d,
//		Content-Length: 72,Content-Type: application/json} b'{"name": "foo", "dataset_id": "123", "usage": "cic", "action": "create"}
		TrainModelRequest req = new TrainModelRequest("create", modelType, dataSetID, name);
		TrainResponse resp = HttpUtil.jsonPOST(dlTasksURL, headers, req, TrainResponse.class);
		if (resp.result == null || !resp.result.equals("success"))
			throw new IOException("Could not delete data set: " + resp.fail);
		try {
			waitForTrainingTask(resp.task_id);
		} catch (IOException e) {
			// If there was an error while waiting, assume the model did not get trained and clean it up.
			TrainedModel tm = this.getTrainedModelByName(name);
			if (tm != null && tm._id != null)
				this.deleteTrainedModel(tm._id);
			throw e;
		}
		TrainedModel found = getTrainedModelByName(name);
		if (found != null)
			vlog("Done training model. id is " + found._id);
		else
			vlog("Done training model. Model not found (was training stopped externally).");

		return found == null ? null : found._id;
	}

	/**
	 * @param name
	 * @return
	 * @throws IOException
	 */
	public TrainedModel getTrainedModelByName(String name) throws IOException {
		List<TrainedModel> models = this.getTrainedModels();
		TrainedModel found = null;
		for (TrainedModel tm: models) {
			if (tm.name.equals(name)) {
				found = tm; 
				break;
			}
		}
		return found;
	}

	TrainedModel getTrainedModelByID(String id) throws IOException {
		List<TrainedModel> models = this.getTrainedModels();
		TrainedModel found = null;
		for (TrainedModel tm: models) {
			if (tm._id.equals(id)) {
				found = tm; 
				break;
			}
		}
		return found;
	}	

	private static class TrainingStatus {
		String type;
		String iter;
		String total_iter;
		String loss;
		String remaining_time;
		String progress;
		
	}

	private static class TrainingStatusResponse {
		String result;
		String status;
		List<TrainingStatus> data;
		@Override
		public String toString() {
			final int maxLen = 8;
			return "TrainingStatusResponse [result=" + result + ", status=" + status + ", data="
					+ (data != null ? data.subList(0, Math.min(data.size(), maxLen)) : null) + "]";
		}
	}

	private List<TrainingStatus> waitForTrainingTask(String task_id) throws IOException {
		boolean done = false;
		TrainingStatusResponse resp = null; 
		int consecutiveTaskStatusFailures = 0;
		int loops = 0;
		do {
			// We have seen a 'fail' status when task transitions from training to trained, but the model seems to be trained in the UI.
			// So don't fail unless we see more than 1 consecutive failures.
			try {
				resp = getTaskStatus(task_id, TrainingStatusResponse.class);
				if (resp.result == null || !resp.result.equals("success") || resp.status == null || resp.status.equals("failed")) {
					consecutiveTaskStatusFailures++;
				} else {
					consecutiveTaskStatusFailures = 0;
				}
			} catch (IOException e) {
				// Actually, the 'fail' status comes back as a 500 response code, which causes an IOException.
				consecutiveTaskStatusFailures++;
			}
			if (consecutiveTaskStatusFailures > 2)
				throw new IOException("Failure to retrieve training task status");
//			vlog(resp);
			done = resp.status != null && resp.status.equals("trained");
			if (!done) {
				if (loops % 10 == 0)
					vlog("Waiting for training to complete.");
				sleep(1000);
			}
			loops++;
		} while (!done);
		return resp.data;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((dataSetsURL == null) ? 0 : dataSetsURL.hashCode());
		result = prime * result + ((dlTasksURL == null) ? 0 : dlTasksURL.hashCode());
		result = prime * result + ((dlapisURL == null) ? 0 : dlapisURL.hashCode());
		result = prime * result + ((dltasksURL == null) ? 0 : dltasksURL.hashCode());
		result = prime * result + ((headers == null) ? 0 : headers.hashCode());
		result = prime * result + ((trainedModelsURL == null) ? 0 : trainedModelsURL.hashCode());
		result = prime * result + ((webapisURL == null) ? 0 : webapisURL.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof MVIRESTClient))
			return false;
		MVIRESTClient other = (MVIRESTClient) obj;
		if (dataSetsURL == null) {
			if (other.dataSetsURL != null)
				return false;
		} else if (!dataSetsURL.equals(other.dataSetsURL))
			return false;
		if (dlTasksURL == null) {
			if (other.dlTasksURL != null)
				return false;
		} else if (!dlTasksURL.equals(other.dlTasksURL))
			return false;
		if (dlapisURL == null) {
			if (other.dlapisURL != null)
				return false;
		} else if (!dlapisURL.equals(other.dlapisURL))
			return false;
		if (dltasksURL == null) {
			if (other.dltasksURL != null)
				return false;
		} else if (!dltasksURL.equals(other.dltasksURL))
			return false;
		if (headers == null) {
			if (other.headers != null)
				return false;
		} else if (!headers.equals(other.headers))
			return false;
		if (trainedModelsURL == null) {
			if (other.trainedModelsURL != null)
				return false;
		} else if (!trainedModelsURL.equals(other.trainedModelsURL))
			return false;
		if (webapisURL == null) {
			if (other.webapisURL != null)
				return false;
		} else if (!webapisURL.equals(other.webapisURL))
			return false;
		return true;
	}

	private void sleep(int msec) {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	public <T> T getTaskStatus(String taskID, Class<T> taskResponseClass) throws IOException {
		String url = this.dltasksURL + "/" + taskID + "/status";
		T resp = HttpUtil.jsonGET(url, headers, taskResponseClass);
		return resp; 
	}
	
	public static class DeployedModel {
		String _id;
		String name;
		String status;
		String dataset_id;
		String usage;
		String nn_arch;
//		Object strategy;
		// data_set_summary,...
	}

	public List<DeployedModel> getDeployedModels() throws IOException {
		String url = this.webapisURL;
		List<DeployedModel> resp = HttpUtil.jsonGETList(url, headers, DeployedModel.class);
		return resp; 
	}
	
	public DeployedModel getDeployedModelByName(String name) throws IOException {
		List<DeployedModel> models = this.getDeployedModels();
		DeployedModel found = null;
		for (DeployedModel tm: models) {
			if (tm.name.equals(name)) {
				found = tm; 
				break;
			}
		}
		return found;
	}
	
	public static class TrainedModel {
		String _id;
		String name;
		String dataset_id;
		String usage;
		String nn_arch;
		String method;
//		Object strategy;
		// data_set_summary,...
	}

	public List<TrainedModel> getTrainedModels() throws IOException {
		String url = this.trainedModelsURL;
		List<TrainedModel> resp = HttpUtil.jsonGETList(url, headers, TrainedModel.class);
		return resp; 
	}
	
	private static class DeleteModelResponse {
		String result;
		String fault;
		public DeleteModelResponse() {}
	}
	public void deleteTrainedModel(String trainedModelID) throws IOException {
		vlog("Begin deleting trained model with id " + trainedModelID);
		String url = this.trainedModelsURL + "/" + trainedModelID; 
		DeleteModelResponse resp = HttpUtil.jsonDELETE(url, headers, null, DeleteModelResponse.class);
		if (resp != null && !resp.result.equals("success")) 
			throw new IOException("Could not undeploy model with id " + trainedModelID + ":" + resp.fault);
		vlog("Done deleting trained model with id " + trainedModelID);
		
	}
	
	private static class DeployModelRequest {
		String trained_model_id;
		String name;
		String usage;
		/**
		 * @param trained_model_id
		 * @param name
		 * @param usage
		 */
		public DeployModelRequest(String trained_model_id, String name, String usage) {
			this.trained_model_id = trained_model_id;
			this.name = name;
			this.usage = usage;
		}


		
	}
	private static class DeployModelResponse {
		String result;
		// String container_id;
		String webapi_id;
		String fault;	
	}
	/**
	 * Deploy  trained model. 
	 * @param trainedModelID id of a trained model to deploy.
	 * @return the id of the deployed model.
	 * @throws IOException 
	 */
	public String deployModel(String trainedModelID) throws IOException {
		vlog("Begin deploying model with id " + trainedModelID);
//		dawood@riley10:~/mvi$ mvi --httpdetail trained-models deploy --modelid=f5af496d-4cf5-4257-a179-1eab200ba88d
//				{
//				  "result": "success",
//				  "container_id": "82303394-3299-4d76-b9b0-2ff4f0fa3e95",
//				  "webapi_id": "f5af496d-4cf5-4257-a179-1eab200ba88d"
//				}
//				========   HTTP Detail   ========
//				    HTTP status code : 200
//				    HTTP request:
//				POST https://ibmrobotics.visualinspection.mas-robotics-f72ef11f3ab089a8c677044eb28292cd-0000.us-east.containers.appdomain.cloud/api/webapis 
//		{User-Agent: python-requests/2.22.0,Accept-Encoding: gzip, deflate,Accept: */*,Connection: keep-alive,accept-language: en-US,X-Auth-Token: 2meV-uoiG-vmvg-250d,Content-Length: 153,Content-Type: application/json} 
//		b'{"trained_model_id": "f5af496d-4cf5-4257-a179-1eab200ba88d", "name": "IAI-MVIFeatureGramClassifier-1a5ff6e9-4fd3-4ef5-b7ed-f3538355558a", "usage": "cic"}'
//				========
		TrainedModel model = getTrainedModelByID(trainedModelID);
		if (model == null)
			throw new IllegalArgumentException("Model with ID " + trainedModelID  + " does not exist.");
		String url = this.webapisURL;
		DeployModelRequest req = new DeployModelRequest(trainedModelID, model.name, model.usage);
		DeployModelResponse resp = HttpUtil.jsonPOST(url, headers, req, DeployModelResponse.class);
		if (resp.result == null || !resp.result.equals("success"))
			throw new IOException("Could not deploy model: " + resp.fault);
		waitForDeployModelReadiness(resp.webapi_id);
		vlog("Done deploying model. Deployed id is " + resp.webapi_id);
		return resp.webapi_id;	
	}

	private static class DeployedModelStatusResponse {
		String _id, accel_type, accuracy, name, usage;
		String status;
	}

	private DeployedModelStatusResponse getDeployedModelStatus(String deployedModelID) throws IOException {
		String url = this.webapisURL + "/" + deployedModelID; 
//		String tt= HttpUtil.jsonGET(url, headers, String.class);
		DeployedModelStatusResponse resp = HttpUtil.jsonGET(url, headers, DeployedModelStatusResponse.class);
//		List<DeployedModelStatusResponse> respList = HttpUtil.jsonGETList(url, headers, DeployedModelStatusResponse.class);
//		if (respList == null || respList.size() != 1)
//			throw new IOException("Could not get status of deployed model with id " + deployedModelID);
//j		DeployedModelStatusResponse	resp = respList.get(0);
		return resp;
	}

	private void waitForDeployModelReadiness(String deployedModelID) throws IOException {
		boolean done = false;
		int loops = 0;
		do {
			DeployedModelStatusResponse resp = getDeployedModelStatus(deployedModelID);
			done = resp.status != null && resp.status.equals("ready");
			if (!done)  {
				if (loops % 10 == 0) 
					vlog("Waiting for model with id " + deployedModelID + " to be ready");
				sleep(1000);
			}
			loops++;
		} while (!done);
	}
	

	/**
	 * Wait for the data set to be present and unlocked.
	 * @param dataset_id
	 * @throws IOException
	 */
	private DataSetStatus waitForDataSetByID(String dataset_id) throws IOException {
		boolean done = false;
		int loops = 0;
		DataSetStatus ds;
		do {
			ds = getDataSetByID(dataset_id);
			done = ds != null && ds.locked == 0;
			if (!done)  {
				if (loops % 10 == 0) 
					vlog("Waiting for data set with id " + dataset_id + " to be ready");
				sleep(1000);
			}
			loops++;
		} while (!done);
		return ds;
	}
	
	/**
	 * Wait for the data set to be present and unlocked.
	 * @param dataset_id
	 * @throws IOException
	 */
	private DataSetStatus waitForDataSetByName(String name) throws IOException {
		boolean done = false;
		int loops = 0;
		DataSetStatus ds; 
		do {
			ds = getDataSetByName(name);
			done = ds != null && ds.locked == 0;
			if (!done)  {
				if (loops % 10 == 0) 
					vlog("Waiting for data set with name " + name + " to be ready");
				sleep(1000);
			}
			loops++;
		} while (!done);
		return ds;
	}
	
	
	
//	private static class StatusResponse {
//		String status;
//		public StatusResponse() {}
//	}
//
//	private <T extends StatusResponse> void waitForTask(String taskID, Class<T> clazz) throws IOException {
//		boolean done = false;
//		int loops = 0;
//		do {
//			T resp = this.getTaskStatus(taskID, clazz);
//			done = resp.status != null && resp.status.equals("completed");
//			if (!done)  {
//				if (loops % 10 == 0) 
//					vlog("Waiting for task with id " + taskID + " to be ready");
//				sleep(1000);
//			}
//			loops++;
//		} while (!done);
//		
//	}

	private static class DataSetStatus {
		String _id;
		String name;
		String usage;
		int locked;
		int total_file_count;
		// Others here, but these are the ones we might care about, for now.
	}

	private DataSetStatus getDataSetByID(String dataset_id) throws IOException {
		do {
			List<DataSetStatus> dssList = getDataSets();
			for (DataSetStatus dss : dssList) {
				if (dss._id.equals(dataset_id))
					return dss;
			}
		} while (true);
	}
	
	private DataSetStatus getDataSetByName(String name) throws IOException {
		do {
			List<DataSetStatus> dssList = getDataSets();
			for (DataSetStatus dss : dssList) {
				if (dss.name.equals(name))
					return dss;
			}
		} while (true);
	}

	public List<DataSetStatus> getDataSets() throws IOException {
		String url = this.dataSetsURL;
		List<DataSetStatus> respList = HttpUtil.jsonGETList(url, headers, DataSetStatus.class); 
//		if (resp == null || resp.result == null || !resp.result.equals("success"))
//			throw new IOException ("Could not get data set categories: " + resp.failure);
		return respList;
	}

	private static class UndeployResponse {
		String result;
		public UndeployResponse() {}
	}

	public void undeployModel(String deployedModelID) throws IOException {
		vlog("Begin undeploying model with id " + deployedModelID);
		String url = this.webapisURL + "/" + deployedModelID; 
		UndeployResponse resp = HttpUtil.jsonDELETE(url, headers, null, UndeployResponse.class);
		if (resp != null && !resp.result.equals("success")) 
			throw new IOException("Could not undeploy model with id " + deployedModelID);
		vlog("Done undeploying model with id " + deployedModelID);
		
	}
	private static class InferenceResult {
		String name;	// label value/category
		double confidence;
	}
	private static class InferenceResponse {
		String webAPIId;
		String imageUrl;
		String imageMd5;
		List<InferenceResult> classified;
		String saveInferenceResult;
	}
	/**
	 * @param deployedModelID
	 * @param spectrogram the spectrogram. 1st dimension is feature index (i.e. time) with each element holding a single feature for the given time. 2nd dimension is feature vector index.
	 * @throws IOException 
	 */
	public List<Classification> classify(String deployedModelID, double[][] spectrogram, String trainingLabel) throws IOException {
		vlog("Begin classifying with deployed model id " + deployedModelID);
//		POST https://ibmrobotics.visualinspection.mas-robotics-f72ef11f3ab089a8c)677044eb28292cd-0000.us-east.containers.appdomain.cloud/api/dlapis/123 
//			{User-Agent: python-requests/2.22.0,Accept-Encoding: gzip, deflate,Accept: */*,Connection: keep-alive,accept-language: en-US,
//			X-Auth-Token: 2meV-uoiG-vmvg-250d,Content-Length: 8178,Content-Type: multipart/form-data; boundary=9e6b35d275dcb15adbdd1432ebf25da2} 
//			b'--9e6b35d275dcb15adbdd1432ebf25da2\r\nContent-Disposition: form-data; name="files"; filename="black.jpg"...
		String url = this.dlapisURL + "/" + deployedModelID;
		MultipartUtility mpu = new MultipartUtility(url, headers, "UTF-8");	
		byte[] image = getSpectrogramAsImage(spectrogram, IMAGE_FORMAT);
		mpu.addFilePart("files", IMAGE_NAME, IMAGE_MIME_TYPE, new ByteArrayInputStream(image));
		HttpResponse resp = mpu.finish();
		if (!resp.isSuccess())
			throw new IOException("Could not classify spectrogram: " + resp.getErrorString());	
		InferenceResponse iresp = gson.fromJson(resp.getBodyString(), InferenceResponse.class);
		List<Classification> clist = new ArrayList<Classification>();
		for (InferenceResult ir : iresp.classified) {
			String value = ir.name.equals("_negative_") ? Classification.UndefinedLabelValue : ir.name;
			Classification cl = new Classification(trainingLabel, value, ir.confidence);
			clist.add(cl);
		}
		if (clist.size() == 0) {	// Not sure why this happens, but it was seen on the new MVI server - dawood 1/2022
			// Treat it as unknown
			Classification cl = new Classification(trainingLabel, Classification.UndefinedLabelValue, 1.0);
			clist.add(cl);
		}
		vlog("Done classifying with deployed model id " + deployedModelID + ". Results are " + clist);
		return clist;
	}

	private static class ImportModelResponse {
		String result;
		String trained_model_id;
	}
	/**
	 * Import a previously exported model to the server.
	 * To use the model needs to be deployed.
	 * @param trainedModel
	 * @return the id of the newly imported trained model.
	 * @throws IOException
	 */
	public String importModel(byte[] trainedModel) throws IOException {
		vlog("Begin importing model");
//		POST https://ibmrobotics.visualinspection.mas-robotics-f72ef11f3ab089a8c677044eb28292cd-0000.us-east.containers.appdomain.cloud/api/trained-models/import 
//			{User-Agent: python-requests/2.22.0,Accept-Encoding: gzip, deflate,Accept: */*,Connection: keep-alive,accept-language: en-US,
//			X-Auth-Token: 2meV-uoiG-vmvg-250d,Content-Length: 706,Content-Type: multipart/form-data; boundary=e6c91dfff028a5607a5ff02c35318812} 
//			b'--e6c91dfff028a5607a5ff02c35318812\r\nContent-Disposition: form-data; name="files"; filename="tt.sh"\r\...
		String url = this.trainedModelsURL + "/import";
		MultipartUtility mpu = new MultipartUtility(url, headers, "UTF-8");	
		mpu.addFilePart("files", "no-name.zip", "application/zip", new ByteArrayInputStream(trainedModel));
		HttpResponse resp = mpu.finish();
		if (!resp.isSuccess())
			throw new IOException("Could not store spectrogram: " + resp.getErrorString());	
		ImportModelResponse imr = gson.fromJson(resp.getBodyString(), ImportModelResponse.class);
		if (imr.result == null || !imr.result.equals("success"))
			throw new IOException("Could not import trained model");
		vlog("Done importing model. Id is " + imr.trained_model_id);
		return imr.trained_model_id;
	}

	/**
	 * Get the blob the represents the trained model with the given id.
	 * @param modelID id of a trained model in the server.
	 * @return byte array that can be used in calls to {@link #importModel(byte[])}.
	 * @throws IOException
	 */
	public byte[] exportModel(String modelID) throws IOException {
		vlog("Begin exporting model with id " + modelID);
//		GET https://ibmrobotics.visualinspection.mas-robotics-f72ef11f3ab089a8c677044eb28292cd-0000.us-east.containers.appdomain.cloud/api/trained-models/633d854c-cdf3-49ae-8784-30384e66afb8/export 
//			{User-Agent: python-requests/2.22.0,Accept-Encoding: gzip, deflate,Accept: */*,Connection: keep-alive,accept-language: en-US,X-Auth-Token: 2meV-uoiG-vmvg-250d}
		String url = this.trainedModelsURL + "/" + modelID + "/export";
		HttpResponse resp = HttpUtil.GET(url, headers);
		if (!resp.isSuccess())
			throw new IOException("Could not get model with id " + modelID);
		vlog("Done exporting model with id " + modelID);
		return resp.getBody();
	}

	private static class AugmentationParameter {
		int max, reps;

		public AugmentationParameter(int max, int reps) {
			this.max = max;
			this.reps = reps;
		}
	}

	private static class AugmentationParameters {
		AugmentationParameter noise;
		AugmentationParameter sharpness;
		AugmentationParameter gaussian_blur;
		AugmentationParameter motion_blur;

		public AugmentationParameters(AugmentationParameter noise, AugmentationParameter sharpness,
				AugmentationParameter gaussian_blur, AugmentationParameter motion_blur) {
			this.noise = noise;
			this.sharpness = sharpness;
			this.gaussian_blur = gaussian_blur;
			this.motion_blur = motion_blur;
		}
		
	}

	private static class AugmentationRequest {
		final String action = "preprocess";
		final String detector = "data_augmentation";
		String dataset_name;
		AugmentationParameters parameters;
		public AugmentationRequest(String dataset_name, AugmentationParameters parameters) {
			this.dataset_name = dataset_name;
			this.parameters = parameters;
		}
		
	}

	private static class AugmentationResponse {
		String result;
		String id;
		String dataset_id;
		AugmentationResponse() {}
	}

	public String createAugmentedDataset(String datasetID, String newDataSetName, List<MVIAugmentation> augmentationList) throws IOException {
//		HttpUtil.setLogging(System.out, Verbose);
		vlog("Begin augmenting data set. Id is" + datasetID);
		DataSetStatus ds = waitForDataSetByID(datasetID);	// Make sure the source data set is not locked
		if (ds == null)
			throw new IOException("Data set with id " + datasetID + " does not exist.");


		AugmentationRequest request = createAugmentationRequest(newDataSetName, augmentationList);
		String url = dataSetsURL + "/" + datasetID + "/action";
		int originalHttpTimeout = HttpUtil.getTimeoutMsec();
		try {
			// The MVI server does not response until the initial copy of the source data set is created, which can take longer than the network timeout. 
			// It seems it may only response after it has create the data set with the original sounds. If the original data set is large this
			// can take too long. So, we set the timeout specially for this request and restore it afterwards. 
			int fileCount = ds.total_file_count;
			HttpUtil.setTimeoutMsec(Math.max(originalHttpTimeout, fileCount * 500));
			AugmentationResponse resp = HttpUtil.jsonPOST(url, headers, request, AugmentationResponse.class); 
			if (resp == null || resp.result == null || !resp.result.equals("success"))
				throw new IOException ("Could not create augmented data set named " + newDataSetName + " from dataset with id: " + datasetID);
			datasetID = resp.dataset_id;
		} finally {
			HttpUtil.setTimeoutMsec(originalHttpTimeout);
		}
		waitForDataSetByID(datasetID);

		vlog("Done augmenting data set. Augmented data set id is" + datasetID);
		return datasetID; 
	}




	private AugmentationRequest createAugmentationRequest(String newDataSetName, List<MVIAugmentation> augmentationList) {
		AugmentationParameter noise = null;
		AugmentationParameter sharpness = null;
		AugmentationParameter gaussian_blur = null;
		AugmentationParameter motion_blur = null;
		for (MVIAugmentation aug: augmentationList) {
			int max = aug.getMaximum();
			int reps = aug.getCount();
			switch(aug.getAgumentation()) {
				case NOISE: noise = new AugmentationParameter(max, reps); break;
				case SHARPNESS: sharpness = new AugmentationParameter(max, reps); break;
				case GAUSSIAN_BLUR: gaussian_blur = new AugmentationParameter(max, reps); break;
				case MOTION_BLUR: motion_blur = new AugmentationParameter(max, reps); break;
				default:
					throw new IllegalArgumentException("Unknown augmentaiton " + aug.getAgumentation());
			}
		}
		AugmentationParameters ap = new AugmentationParameters(noise, sharpness, gaussian_blur, motion_blur);
		AugmentationRequest req = new AugmentationRequest(newDataSetName, ap);
		return req;

	}

	private static class RenameDataSetRequest {
		final String action = "rename";
		String data;	// new name
		public RenameDataSetRequest(String dataset_name) {
			this.data = dataset_name;
		}
	}

	private static class RenameDataSetResponse {
		String result;
		public RenameDataSetResponse() {}
	}

	public void renameDataset(String datasetID, String newDataSetName) throws IOException {
		vlog("Begin renaming data set with id " + datasetID + " to " +  newDataSetName);
		String tmpName = newDataSetName + UUID.randomUUID().toString();
		RenameDataSetRequest request = new RenameDataSetRequest(newDataSetName); 
		String url = dataSetsURL + "/" + datasetID + "/action";
		RenameDataSetResponse resp = HttpUtil.jsonPOST(url, headers, request, RenameDataSetResponse.class); 
		if (resp == null || resp.result == null || !resp.result.equals("success"))
			throw new IOException ("Could not rename data set with id " + datasetID + " to  " + newDataSetName);
		vlog("Done renaming data set with id " + datasetID + " to " +  newDataSetName);
	}

}
