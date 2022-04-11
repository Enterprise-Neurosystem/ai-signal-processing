package org.eng.aisp.classifier.mvi;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eng.aisp.AISPException;
import org.eng.aisp.SoundClip;
import org.eng.aisp.SoundRecording;
import org.eng.aisp.classifier.AbstractFixableFeatureGramClassifier;
import org.eng.aisp.classifier.Classification;
import org.eng.aisp.classifier.IFeatureGramClassifier;
import org.eng.aisp.classifier.IFixableClassifier;
import org.eng.aisp.classifier.IFixedFeatureGramClassifier;
import org.eng.aisp.feature.FeatureGramDescriptor;
import org.eng.aisp.feature.IFeatureGramDescriptor;
import org.eng.aisp.feature.extractor.vector.LogMelFeatureExtractor;
import org.eng.util.ClassUtilities;
import org.eng.util.HttpUtil;
import org.eng.util.IShuffleIterable;

/**
 * A trainable classifier that uses a Maximo Visual Insights (MVI) server, via MVIFeatureGramClassifier instance, to train and classify. 
 * Labeled feature extraction is done in this instance, the resulting feature grams are managed by an instance of MVIFeatureGramClassifier.
 * The model is then trained from this instance against those stored spectrograms producing a model identifier that can be used for classification
 * of similar features.  
 * The bulk of the behavior/configuration is controlled and enabled by {@link #MVIFeatureGramClassifier} and as such you should consult the docs there. 
 * <p>
 * A name may be assigned to the models and datasets and trained models and datasets can be preserved across instances.  As such, new
 * instances can use a past name to connect to a pre-trained model, and even a currently deployed model.
 * @see  {@link #MVIFeatureGramClassifier}
 */
public class MVIClassifier extends AbstractFixableFeatureGramClassifier implements IFixableClassifier<double[]>, Closeable {
	private static final long serialVersionUID = -5663489590441006403L;
	private final MVIFeatureGramClassifier mviClassifier;


	public static final List<MVIAugmentation> DEFAULT_AUGMENTATIONS = null; 

	public static final IFeatureGramDescriptor<double[],double[]> DEFAULT_FEATURE_GRAM_EXTRACTOR = new FeatureGramDescriptor<double[],double[]>(40,20, new LogMelFeatureExtractor(128), null);
//	public static final IFeatureGramDescriptor<double[],double[]> DEFAULT_FEATURE_GRAM_EXTRACTOR = new FeatureGramDescriptor<double[],double[]>(40,0, 
//			double targetSamplingRate,  int minHtz, int maxHtz, boolean norm, boolean useLog, int maxFFTSize
//			new FFTFeatureExtractor(0, 20, 20000, false, true, 512), 
//			null);
	public static final String DEFAULT_NAME = null;
	public static final String DEFAULT_MVI_HOST = null;
	public static final String DEFAULT_MVI_API_KEY = null;
	public static final int DEFAULT_MVI_PORT = 0;
	public static final boolean DEFAULT_PRESERVE_MODEL = false;
	
	/**
	 * Convenience on {@link #MVIClassifier(IFeatureGramDescriptor)} using the default feature gram extractor that preserves the models.
	 * Requires VAPI_HOST/VAPI_PORT or VAPI_BASE_URI and VAPI_TOKEN env vars to be set to point to the MVI server and set the API key for that server.
	 */
	public MVIClassifier() {
		this(DEFAULT_FEATURE_GRAM_EXTRACTOR, DEFAULT_NAME, DEFAULT_PRESERVE_MODEL);
	}

	public MVIClassifier(String name, boolean preserveModel) {
		this(DEFAULT_FEATURE_GRAM_EXTRACTOR, name, preserveModel);
	}
	/**
	 * Create and use env variables (VAPI_HOST/VAPI_BASE_URI , VAPI_TOKEN and optionally VAPI_PORT (defaults to 443)) to define location of MVI server.
	 * @param featureGramExtractor
	 * @param the name to use for data sets and models.  If null, one is generated.
	 * @param preserveModel if true, then leave the most recent data set and trained model on the server.
	 */
	public MVIClassifier(IFeatureGramDescriptor<double[], double[]> featureGramExtractor, String name, boolean preserveModel) {
		this(featureGramExtractor, name, preserveModel, DEFAULT_MVI_HOST, DEFAULT_MVI_PORT, DEFAULT_MVI_API_KEY, DEFAULT_AUGMENTATIONS);
	}

	/**
	 * 
	 * @param featureGramExtractor
	 * @param preserveModel if true, then leave the most recent data set and trained model on the server.
	 * @param the name to use for data sets and models.  If null or empty, one is generated.
	 * @param mviHost if null, then use VAPI_HOST or VAPI_BASE_URI  env var value.
	 * @param mviPort if 0 or less and VAPI_BASE_URI  is not set, then use VAPI_PORT env var value. Defaults to 443 and so https.
	 * @param mviApiKey if null, then use VAPI_TOKEN env var value.
	 * @param augList option list of augmentations.  Can be null or empty.
	 */
	public MVIClassifier(IFeatureGramDescriptor<double[], double[]> featureGramExtractor, String name, boolean preserveModel, String mviHost, int mviPort, String mviApiKey, List<MVIAugmentation> augList) {
		super(false, 	// preShuffle
				null,	// transform	
				Arrays.asList(featureGramExtractor),
				false,	// useMemoryCache
				false,	// useDiskCache
				false	// softReferenceFeatures
				);
		mviClassifier = new MVIFeatureGramClassifier(preserveModel, name, mviHost,mviPort, mviApiKey, augList);
	}
	
	@Override
	protected IFeatureGramClassifier<double[]> newFeatureGramClassifier() {
		return this.mviClassifier;
	}

	/**
	 * @return the given instance since it does not maintain any large training-specific state. 
	 */
	@Override
	protected IFixedFeatureGramClassifier<double[]> newFixedFeatureGramClassifier( IFeatureGramClassifier<double[]> fgc) {
		return fgc;
	}	
	
	@Override
	public void close() throws IOException {
		this.mviClassifier.close();
	}

	@Override
	protected void finalize() throws Throwable {
		this.close();
		super.finalize();
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((mviClassifier == null) ? 0 : mviClassifier.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof MVIClassifier))
			return false;
		MVIClassifier other = (MVIClassifier) obj;
		if (mviClassifier == null) {
			if (other.mviClassifier != null)
				return false;
		} else if (!mviClassifier.equals(other.mviClassifier))
			return false;
		return true;
	}

		
	/**
	 * Requires VAPI_HOST/VAPI_BASE_URI and VAPI_TOKEN to be set in the env vars.
	 * @param args
	 * @throws IOException
	 * @throws AISPException
	 * @throws ClassNotFoundException
	 */
	public static void main(String[] args) throws IOException, AISPException, ClassNotFoundException {
		String soundDir = "/dev/sounds/class-sounds";
		String labelName = "source";
//		System.setProperty("httputil.SSLCertificateVerification.enabled", "false");
		HttpUtil.setLogging(System.out, true);
		MVIClassifierBuilder builder =new MVIClassifierBuilder();
		builder.setModelName("test-main")
//			.addNoise(1,10)
//			.addMotionBlur(2,20)
//			.addGaussianBlur(3,30)
//			.addSharpness(4,40)
			;
		MVIClassifier classifier = builder.build(); 
//		classifier = new MVIClassifier("test-main", false);	// VAPI_HOST and VAPI_TOKEN env vars expected here
//		MVIClassifier classifier = new MVIClassifier("test-main", true);	// VAPI_HOST and VAPI_TOKEN env vars expected here
			
		IShuffleIterable<SoundRecording> sounds = SoundRecording.readMetaDataSounds(soundDir);
		classifier.train(labelName, sounds);
		byte[] serialization = ClassUtilities.serialize(classifier);
		classifier.close();	// simulate normal JVM exit 
		classifier = (MVIClassifier) ClassUtilities.deserialize(serialization);
		for (SoundRecording sr : sounds) {
			SoundClip clip = sr.getDataWindow();
			Map<String,Classification> cl = classifier.classify(clip);
			System.out.println(cl);
			break;
		}
		
//		// Connect to a previously trained model already on the server.
//		classifier.close();
//		classifier = new MVIClassifier("test-main", true);	// VAPI_HOST and VAPI_TOKEN env vars expected here
//		for (SoundRecording sr : sounds) {
//			SoundClip clip = sr.getDataWindow();
//			Map<String,Classification> cl = classifier.classify(clip);
//			System.out.println(cl);
//			break;
//		}
	}


}
