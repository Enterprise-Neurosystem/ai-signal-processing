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

import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.eng.aisp.AISPException;
import org.eng.aisp.AISPLogger;
import org.eng.aisp.classifier.Classification;
import org.eng.aisp.classifier.IFeatureGramClassifier;
import org.eng.aisp.classifier.mvi.MVIRESTClient.DeployedModel;
import org.eng.aisp.classifier.mvi.MVIRESTClient.TrainedModel;
import org.eng.aisp.feature.IFeature;
import org.eng.aisp.feature.IFeatureGram;
import org.eng.aisp.feature.ILabeledFeatureGram;

/**
 * Uses selected MVI REST APIs to implement train and classify.
 * The model is then trained from this instance against those stored spectrograms producing a model identifier that can be used for classification
 * of similar features.  
 * <p>
 * A name may be assigned to the models and datasets and trained models and datasets can be preserved across instances.  As such, new
 * instances can use a past name to connect to a pre-trained model, and even a currently deployed model.
 * 
 * @author 285782897
 * @see <a href="http://public.dhe.ibm.com/systems/power/docs/powerai/api840.html#">http://public.dhe.ibm.com/systems/power/docs/powerai/api840.html#</a>
 */
class MVIFeatureGramClassifier implements Serializable, IFeatureGramClassifier<double[]>, Closeable {

	private static final long serialVersionUID = -8389439584007991237L;
	private String name;
	/** The label on which the model is trained. */
	private String trainedLabel;
	private final MVIRESTClient mviClient;
	/** ID of the data used to train the model.   */
	private String dataSetID;
	/** ID of the trained model that can be deployed.   */
	private String trainedModelID;
	/** ID of the deployed model */
	private transient String deployedModelID;
	/** Holds the trained exported model (as a zip)  */
	private byte[] trainedModel;

	/** Used to scale the features into the range of values allowed for images */
	private double featureValueMinimum = Double.NaN, featureValueScalar = Double.NaN;
	
	/** If true, then leave the most recent data set and trained model in the server */
	private boolean preserveModel;
	
	private transient Thread shutdownHook = null;
	
	private final List<MVIAugmentation> augmentationList;
	
	/**
	 * Create the classifier to use a client that talks to the server using the given attributes. 
	 * @param preserveModel  if true, then save the most recent data set and model on the server that are produced during training.
	 * @param name optional name assigned to data sets and models for this instance.. 
	 * @param host if null, look in {@value MVIClient#MVI_HOST_ENVVAR_NAME} environment variable. If not provided there, the throw an IllegalArgumentException.
	 * @param port if null, look in {@value MVIClient#MVI_PORT_ENVVAR_NAME} environment variable. If not provided there, then use 443. 
	 * @param authToken if null, look in {@value MVIClient#MVI_APIKEY_ENVVAR_NAME} environment variable. If not provided there, the throw an IllegalArgumentException.
	 */
	public MVIFeatureGramClassifier(boolean preserveModel, String name, String mviHost, int mviPort, String authToken, List<MVIAugmentation> augmentations) {
		if (name != null && (name.length() == 0  || name.equals("null")))	// Treat "" as null for use by javascript builder (mvi.jsyt).
			name = null;	 
		this.name = name;
		this.preserveModel = preserveModel;
		mviClient = new MVIRESTClient(mviHost, mviPort, authToken);
		this.augmentationList = augmentations;
		
	}
	
	/**
	 * Installs a JVM shutdown hook to close() this instance (on normal JVM exit).
	 */
	private synchronized void installShutdownHook() {
		if (shutdownHook == null) {
			shutdownHook = new Thread() { 
				public void run() { MVIFeatureGramClassifier.this.close(); };
			};
			Runtime.getRuntime().addShutdownHook(shutdownHook);
		}
	}

	private synchronized void removeShutdownHook() {
		if (shutdownHook != null) {
			try {
				Runtime.getRuntime().removeShutdownHook(shutdownHook);
			} catch (IllegalStateException e) {
				; // Means we were called __during__ the shutdown hook, so we can ignore. 
			}
			shutdownHook = null;
		}
	}

	@Override
	public synchronized void train(String trainingLabel, Iterable<? extends ILabeledFeatureGram<double[]>[]> features) throws AISPException {
		// Make sure we try and clean up the server when this instance is done being used.
		this.installShutdownHook();

		/** If this is a 2nd call to this method, we need to remove any existing models from the server */
		this.trainedLabel = null;
		this.trainedModel = null;
		clearServer(false);

		setFeatureMinMax(features);

		if (name == null)
			name = "IAI-" + this.getClass().getSimpleName() + "-" + UUID.randomUUID().toString();
		this.dataSetID = storeAugmentedFeatures(trainingLabel, features, name);

		try {
			trainedModelID = mviClient.trainModel(dataSetID, "cic", name); 
			this.trainedModel = mviClient.exportModel(trainedModelID);	// Export so we can import it after close() or deserialization. 
//			this.deployedModelID = mviClient.deployModel(trainedModelID);
		} catch (IOException e) {
			clearServer(false);
			throw new AISPException("Could not train and/or deploy model: " + e.getMessage(), e);
		} finally {
			;
		}
		this.trainedLabel = trainingLabel;	// Signals the training is complete.
	}



	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((augmentationList == null) ? 0 : augmentationList.hashCode());
		result = prime * result + ((dataSetID == null) ? 0 : dataSetID.hashCode());
		long temp;
		temp = Double.doubleToLongBits(featureValueMinimum);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(featureValueScalar);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((mviClient == null) ? 0 : mviClient.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + (preserveModel ? 1231 : 1237);
		result = prime * result + ((trainedLabel == null) ? 0 : trainedLabel.hashCode());
		result = prime * result + Arrays.hashCode(trainedModel);
		result = prime * result + ((trainedModelID == null) ? 0 : trainedModelID.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof MVIFeatureGramClassifier))
			return false;
		MVIFeatureGramClassifier other = (MVIFeatureGramClassifier) obj;
		if (augmentationList == null) {
			if (other.augmentationList != null)
				return false;
		} else if (!augmentationList.equals(other.augmentationList))
			return false;
		if (dataSetID == null) {
			if (other.dataSetID != null)
				return false;
		} else if (!dataSetID.equals(other.dataSetID))
			return false;
		if (Double.doubleToLongBits(featureValueMinimum) != Double.doubleToLongBits(other.featureValueMinimum))
			return false;
		if (Double.doubleToLongBits(featureValueScalar) != Double.doubleToLongBits(other.featureValueScalar))
			return false;
		if (mviClient == null) {
			if (other.mviClient != null)
				return false;
		} else if (!mviClient.equals(other.mviClient))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (preserveModel != other.preserveModel)
			return false;
		if (trainedLabel == null) {
			if (other.trainedLabel != null)
				return false;
		} else if (!trainedLabel.equals(other.trainedLabel))
			return false;
		if (!Arrays.equals(trainedModel, other.trainedModel))
			return false;
		if (trainedModelID == null) {
			if (other.trainedModelID != null)
				return false;
		} else if (!trainedModelID.equals(other.trainedModelID))
			return false;
		return true;
	}

	/**
	 * Remove server-side content according to {@link #preserveModel}. 
	 * Always delete the deployed model, but only the data set and trained model if preserveModel is false.
	 */
	private synchronized void clearServer(boolean preserveModel) {
		// Must delete deployed model before deleting the trained model.
		if (this.deployedModelID != null) {
			try {
				this.mviClient.undeployModel(this.deployedModelID);
			} catch (IOException e) {
				; // ignore
			}
			this.deployedModelID = null;
		}
		if (!preserveModel) {
			if (this.trainedModelID != null) {
				try {
					this.mviClient.deleteTrainedModel(this.trainedModelID);
				} catch (IOException e) {
					AISPLogger.logger.severe("Could not delete trained model: " + e.getMessage());
				}
				this.trainedModelID = null;
			}
			if (this.dataSetID != null) {
				try {
					this. mviClient.deleteDataSet(this.dataSetID);
				} catch (IOException e) {
					AISPLogger.logger.severe("Could not delete data set: " + e.getMessage());
				}
				this.dataSetID = null;
			}
		}
	}

	/**
	 * Find the min and max values of all the features and store them in {@link #minFeatureValue} and {@link #maxFeatureValue}.
	 * @param featureGramArray
	 */
	private void setFeatureMinMax(Iterable<? extends ILabeledFeatureGram<double[]>[]> featureGramArray) {
		double minVal = Double.MAX_VALUE;
		double maxVal = -Double.MAX_VALUE;
		for (ILabeledFeatureGram<double[]>[] lfga : featureGramArray) {
			if (lfga.length > 1)
				throw new IllegalArgumentException("More than one feature gram is not supported");
			for (IFeature<double[]> fe : lfga[0].getFeatureGram().getFeatures()) {
				double[] feData = fe.getData();
				for (int i=0; i<feData.length; i++) {
					double f = feData[i];
					if (f < minVal)
						minVal = f;
					if (f > maxVal)
						maxVal = f;
				}
			}
		}
		this.featureValueMinimum = minVal; 
		this.featureValueScalar= 1.0 / (maxVal - minVal); 
	}

	private String storeAugmentedFeatures(String trainingLabel, Iterable<? extends ILabeledFeatureGram<double[]>[]> features, String name2) throws AISPException {
		boolean augmenting = this.augmentationList != null && this.augmentationList.size() > 0;
		String datasetID = this.storeFeatures(trainingLabel, features, augmenting ? name + "(pre-augmentation)" : name);
		if (!augmenting)
			return datasetID;
		
		String augDatasetID;
		try {
			augDatasetID = this.mviClient.createAugmentedDataset(datasetID, this.name, augmentationList);
			return augDatasetID;
		} catch (IOException e) {
			throw new AISPException("Could not create augmented data set: " + e.getMessage());
		} finally {
			try {
				this.mviClient.deleteDataSet(datasetID);
			} catch (IOException e) {
				throw new AISPException("Could not delete unaugmented data set: " + e.getMessage());
			}
		}
	}

	private String storeFeatures(String trainingLabel, Iterable<? extends ILabeledFeatureGram<double[]>[]> features, String dataSetName) throws AISPException {
		String dsID = null; 
		try {
			dsID = mviClient.createDataSet(dataSetName);
			for (ILabeledFeatureGram<double[]>[] lfga : features) {
				if (lfga.length > 1)
					throw new AISPException("Only single feature/spectrogram supported");
				ILabeledFeatureGram<double[]> labeledFG = lfga[0];
				Properties labels = labeledFG.getLabels();
				String labelValue = labels.getProperty(trainingLabel);
				if (labelValue != null) {
					IFeatureGram<double[]> fg = labeledFG.getFeatureGram();
					double[][] spectrogram = getSpectrogram(fg);
					mviClient.addFile(dsID, spectrogram, labelValue);
				}
			}
		} catch (IOException e) {
			try {
				if (dsID != null)
					mviClient.deleteDataSet(dsID);
			} catch (IOException e2) {
				; // drop this one.
			}
			throw new AISPException("Could not store images in a data set: " + e.getMessage(), e);
		}
		return dsID;
	}

	@Override
	public List<Classification> classify(IFeatureGram<double[]>[] features) throws AISPException {
		if (this.trainedLabel == null)
			throw new AISPException("Model has not been trained yet.");
		// Make sure our model is available on the client.
		deployModel();
		// Get the spectrogram we will classify.
		double[][] spectrogram = getSpectrogram(features[0]);
		
		// Do the classification on the server.
		List<Classification> clist;
		try {
			clist = mviClient.classify(this.deployedModelID, spectrogram, this.trainedLabel);
		} catch (IOException e) {
			throw new AISPException("Could not classify sound: " + e.getMessage(),e);
		}
		return clist;
	}

	/**
	 * Make sure our model is deployed on the server.
	 * Returns with {@link #deployedModelID} set to the deployed model.
	 * @throws AISPException
	 */
	private synchronized void deployModel() throws AISPException {
		// Make sure we try and remove anything we have put on or already placed on the server.
		this.installShutdownHook();
		
		// Now make sure the model is deployed.
		if (this.deployedModelID == null) {
			try {
				// Make sure the model with our name is not already deployed.
				DeployedModel dm = this.mviClient.getDeployedModelByName(this.name);
				if (dm != null)  {
					// It's already deployed.
					this.deployedModelID = dm._id;
					return;
				}
				// Make sure our trainedModelID is still valid - it may not be if serialized, closed(), then deserialized
				if (this.trainedModelID != null) {
					TrainedModel md = this.mviClient.getTrainedModelByID(this.trainedModelID);
					if (md == null)
						this.trainedModelID = null;	// Trained model id we have is no longer valid.
				}
				// Need to do a deploy of a trained model, so establish a trained model id.
				if (trainedModelID == null)  {
					// See if a model with our name is already trained and available.
					TrainedModel tm = this.mviClient.getTrainedModelByName(this.name);
					if (tm != null) {
						this.trainedModelID = tm._id;
					} else {
						// Put our exported model up on the server. 
						this.trainedModelID = this.mviClient.importModel(this.trainedModel);
					}
				} 
					
				// Deploy the trained model.
				this.deployedModelID = this.mviClient.deployModel(trainedModelID);
			} catch (IOException e) {
				throw new AISPException("Could not deploy model: " + e.getMessage(), e);
			}
		}
	}
	

	@Override
	public String getTrainedLabel() {
		return this.trainedLabel;
	}

	/**
	 * Get the spectrogram scaled into the range [0,1] based on the scaling factors determine the last call to {@link #train(String, Iterable)}.
	 * @param fg
	 * @return a matrix of features, 1st dim is time, 2nd dim is feature (as per IFeature.
	 */
	public double[][] getSpectrogram(IFeatureGram<double[]> fg) {
		if (Double.isNaN(featureValueMinimum))
			throw new RuntimeException("Unexpected call to this method prior to setting feature scaling parameters (i.e. train())");
		IFeature<double[]>[] features = fg.getFeatures();
		int numWind = features.length; 
		int featureLen = features[0].getData().length;
		double[][] spectrogram = new double[numWind][featureLen];
//		OnlineStats stats = new OnlineStats();
		for (int i=0 ; i<numWind ; i++) {
			double[] fe = features[i].getData();
			for (int j=0; j<featureLen; j++) {
				spectrogram[i][j] = (fe[j] - this.featureValueMinimum) * this.featureValueScalar;
				if (spectrogram[i][j] > 1.0) spectrogram[i][j] = 1.0;
				else if (spectrogram[i][j] < 0.0) spectrogram[i][j] = 0.0;
			}
//			stats.addSamples(spectrogram[i]);
		}
//		AISPLogger.logger.info("Spectrogram stats: " + stats);
		return spectrogram;
	}

	
	@Override
	public void close() {
		removeShutdownHook();
		clearServer(this.preserveModel);
	}
	
	@Override
	protected void finalize() throws Throwable {
		this.close();
		super.finalize();
	}
	
	

}