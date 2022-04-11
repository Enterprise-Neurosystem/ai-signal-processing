package org.eng.aisp.classifier.mvi;

import java.util.ArrayList;
import java.util.List;

import org.eng.aisp.AISPException;
import org.eng.aisp.classifier.AbstractClassifierBuilder;
import org.eng.aisp.feature.IFeatureGramDescriptor;

public class MVIClassifierBuilder extends AbstractClassifierBuilder<double[], double[]> {

	private static final long serialVersionUID = 8294923042175592820L;
	protected String modelName = MVIClassifier.DEFAULT_NAME;
	protected String mviHost = MVIClassifier.DEFAULT_MVI_HOST;
	protected String mviAPIKey  = MVIClassifier.DEFAULT_MVI_API_KEY; 
	protected int mviPort = MVIClassifier.DEFAULT_MVI_PORT;
	protected boolean preserveModel = MVIClassifier.DEFAULT_PRESERVE_MODEL;
	List<MVIAugmentation> augmentationList = null;

	public MVIClassifierBuilder() {
		this.setFeatureGramDescriptor(MVIClassifier.DEFAULT_FEATURE_GRAM_EXTRACTOR);
	}
	
	@Override
	public MVIClassifier build() throws AISPException {
		List<IFeatureGramDescriptor<double[],double[]>> fgeList = this.getFeatureGramExtractors();
		if (fgeList.size() != 1)
			throw new AISPException("Only a single feature gram is supported");
		return new MVIClassifier(fgeList.get(0), this.modelName, this.preserveModel, this.mviHost, this.mviPort, this.mviAPIKey, this.augmentationList);
	}

	/**
	 * @param modelName the modelName to set
	 */
	public MVIClassifierBuilder setModelName(String modelName) {
		this.modelName = modelName;
		return this;
	}

	/**
	 * @param mviHost the mviHost to set
	 */
	public MVIClassifierBuilder setMviHost(String mviHost) {
		this.mviHost = mviHost;
		return this;
	}

	/**
	 * @param mviAPIKey the mviAPIKey to set
	 */
	public MVIClassifierBuilder setMviAPIKey(String mviAPIKey) {
		this.mviAPIKey = mviAPIKey;
		return this;
	}

	/**
	 * @param mviPort the mviPort to set
	 */
	public MVIClassifierBuilder setMviPort(int mviPort) {
		this.mviPort = mviPort;
		return this;
	}

	/**
	 * @param preserveModel the preserveModel to set
	 */
	public MVIClassifierBuilder setPreserveModel(boolean preserveModel) {
		this.preserveModel = preserveModel;
		return this;
	}

	/**
	 * @param augList the augList to set. null turns off augmentation.
	 */
	public MVIClassifierBuilder setAugmentationList(List<MVIAugmentation> augList) {
		this.augmentationList = augList;
		return this;
	}

	public MVIClassifierBuilder addAugmentation(MVIAugmentation aug) {
		if (this.augmentationList == null)
			this.augmentationList = new ArrayList<MVIAugmentation>();
		this.augmentationList.add(aug);
		return this;
	}

	public MVIClassifierBuilder addNoise(int count, int max) {
		MVIAugmentation aug = new MVIAugmentation(MVIAugmentation.AugmentationEnum.NOISE, count, max);
		return this.addAugmentation(aug);
	}
	public MVIClassifierBuilder addNoise(int count) {
		return this.addNoise(count, MVIAugmentation.DEFAULT_MAX);
	}
	public MVIClassifierBuilder addGaussianBlur(int count, int max) {
		MVIAugmentation aug = new MVIAugmentation(MVIAugmentation.AugmentationEnum.GAUSSIAN_BLUR, count, max);
		return this.addAugmentation(aug);
	}
	public MVIClassifierBuilder addGaussianBlur(int count) {
		return this.addGaussianBlur(count, MVIAugmentation.DEFAULT_MAX);
	}
	public MVIClassifierBuilder addMotionBlur(int count, int max) {
		MVIAugmentation aug = new MVIAugmentation(MVIAugmentation.AugmentationEnum.MOTION_BLUR, count, max);
		return this.addAugmentation(aug);
	}
	public MVIClassifierBuilder addMotionBlur(int count) {
		return this.addMotionBlur(count, MVIAugmentation.DEFAULT_MAX);
	}
	public MVIClassifierBuilder addSharpness(int count) {
		return this.addSharpness(count, MVIAugmentation.DEFAULT_MAX);
	}
	public MVIClassifierBuilder addSharpness(int count, int max) {
		MVIAugmentation aug = new MVIAugmentation(MVIAugmentation.AugmentationEnum.SHARPNESS, count, max);
		return this.addAugmentation(aug);
	}
}
