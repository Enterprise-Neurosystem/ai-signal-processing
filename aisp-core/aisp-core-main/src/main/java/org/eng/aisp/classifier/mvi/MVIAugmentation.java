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