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
package org.eng.util;

import java.util.Random;


	public class SingleFrequencySignalGenerator implements ISignalGenerator {
			protected final int samplingRate;
			protected final double baseHtz;
			protected final double offset;
			protected double angleFactor; 
			protected final double amp;
			protected final static double MAX_VALUE = 1;
			protected final static double MAX_NOISE_PERCENT = .05;
			protected final static double FREQ_NOISE_PERCENT = .10;  //in % of htz, uniformly distributed within [-FREQ_NOISE_RANGE*htz / 2, FREQ_NOISE_RANGE*htz / 2)
			protected final Random rand; 
			protected final boolean addWhiteNoise; 
			protected int nextCalls = 0;
			protected double currentHtz;
			private boolean addFreqNoise;
			private final int maxSampleCount; 
			
			/**
			 * A convenience on {@link #SingleFrequencySignalGenerator(double, int, double, double, double, boolean, boolean)} with unlimited data produced.
			 */
			public SingleFrequencySignalGenerator(int samplingRate, double amp, double offset, double htz, boolean addWhiteNoise, boolean addFreqNoise) {
				this(0,samplingRate, amp, offset, htz, addWhiteNoise, addFreqNoise);
			}

			/**
			 * Create a signal that ranges from -(amp+offset..amp+offset).
			 * @param durationMsec this together with the sampling rate determines the number of samples to returned before {@link #hasNext()} returns false.
			 * if 0 or less, then no limit.
			 * @param samplingRate the number of samples per second.
			 * @param amp a number between 0 and 1 to scale the signal down from -1..1 to -amp..amp before shifting to the offset. 
			 * @param offset a number between 0 and that signals the offset relative to .5. 
			 * @param htz
			 * @param addWhiteNoise
			 * @throws IllegalArgumentException if resulting signal will go outside the range of -1..1
			 */
			public SingleFrequencySignalGenerator(double durationMsec, int samplingRate, double amp, double offset, double htz, boolean addWhiteNoise, boolean addFreqNoise) {
				if (amp <= 0 || amp > 1) 
					throw new IllegalArgumentException("amp must be in the range 0..1");
				if (offset >= MAX_VALUE)
					throw new IllegalArgumentException("offset is too large");
				if (durationMsec <= 0)
					this.maxSampleCount = 0;
				else
					this.maxSampleCount =  (int)(durationMsec / 1000.0 * samplingRate) + 1;

				this.samplingRate = samplingRate;
				this.baseHtz= htz;
				this.currentHtz = htz;
				this.addWhiteNoise = addWhiteNoise;
				this.addFreqNoise = addFreqNoise;
				
				this.rand = new Random((long)(samplingRate * amp * htz));
//				this.rand = new Random(System.currentTimeMillis());
				angleFactor = 2.0 * Math.PI / samplingRate;
				
				this.amp = amp;	
				if (Math.abs(amp) + Math.abs(offset) > 1)
					throw new IllegalArgumentException("Amplitude plus offset is larger than 1.");
				this.offset = offset;
					
			}
	
			@Override
			public boolean hasNext() {
				return maxSampleCount <= 0 || nextCalls <= maxSampleCount;
				
			}
	
			protected static Double getNext(double htz, double phase, double amp, double offset, int samplingRate, Random rand) {
				double value = 0;
				
				if (htz != 0)
					value = Math.cos(phase);
				
				if (rand != null) {
					double noise = 2 * (rand.nextDouble() - 0.5) * MAX_NOISE_PERCENT; 
					value += noise;
//					value = Math.min(Math.max(value, -1), 1);
				}
				double r = (amp * value) + offset;
				if (r > 1)
					r = 1;
				else if (r < -1)
					r = -1;
//				if (r < -1 || r > 1)  
//					throw new RuntimeException("r is bad");
				return r;	
			}
			
			/**
			 * Calculate the next signal value if the given frequency value is used.
			 * @return
			 */
			protected double nextSignalValue(double htz) {
				double phase = nextCalls * angleFactor * htz;
	//			System.out.println("phase2=" + (360.0/Math.PI * phase) % 360);
				double r =  getNext(htz, phase, amp, offset, samplingRate, addWhiteNoise ? rand : null); 
				return r;
			}
	
			/**
			 * Every 3 cycles choose a new frequence if addWhiteNoise is true.
			 */
			protected void updateCurrentHtz() {
				if (addFreqNoise && nextCalls % (baseHtz*3) == 0) {
					// Set a new frequency
					double freqNoise = FREQ_NOISE_PERCENT * (2 * rand.nextDouble() - 1) * baseHtz;
					currentHtz += freqNoise;
				}
			}
	
			@Override
			public Double next() {
				updateCurrentHtz();
	//			System.out.println("phase=" + (360.0/Math.PI * phase) % 360);
				double r =  nextSignalValue(currentHtz);
				nextCalls++;
				return r;
			}

			@Override
			public void remove() {
				throw new RuntimeException("not supposed to be used");
			}
		}