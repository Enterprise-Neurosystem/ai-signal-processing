var mfcc = new MFCCFeatureExtractor(25);
var normalizer = new NormalizingFeatureProcessor(true,true, false, true);
var classifier = new GaussianMixtureClassifier(trainingLabel, mfcc,  normalizer, 100, 100, 1, .9);