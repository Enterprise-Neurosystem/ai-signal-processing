package org.eng.aisp.classifier.mvi;

import java.io.Serializable;

public class MVIAugmentation implements Serializable {
	
	private static final long serialVersionUID = -7663183602496966280L;

	public enum AugmentationEnum {
		GAUSSIAN_BLUR,
		MOTION_BLUR,
		NOISE,
		SHARPNESS
	}
	
	public final static int DEFAULT_MAX = 100;
	
	protected final AugmentationEnum agumentation;
	protected final int maximum;
	protected final int count;
	
	/**
	 * @param agumentation
	 * @param reps
	 * @param maximum
	 */
	public MVIAugmentation(AugmentationEnum augmentation, int reps, int maximum) {
		this.agumentation = augmentation;
		this.maximum = maximum;
		this.count = reps;
	}

	public MVIAugmentation(AugmentationEnum augmentation, int reps) {
		this(augmentation, reps, DEFAULT_MAX);
	}

	/**
	 * @return the agumentation
	 */
	public AugmentationEnum getAgumentation() {
		return agumentation;
	}

	/**
	 * @return the maximum
	 */
	public int getMaximum() {
		return maximum;
	}

	/**
	 * @return the count of augmentations 
	 */
	public int getCount() {
		return count;
	}

	
	
}