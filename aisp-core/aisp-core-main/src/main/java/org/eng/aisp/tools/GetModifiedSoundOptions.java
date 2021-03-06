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
package org.eng.aisp.tools;

import org.eng.aisp.IDataWindow.PadType;
import org.eng.aisp.SoundRecording;
import org.eng.aisp.util.BalancedLabeledWindowShuffleIterable;
import org.eng.aisp.util.FixedDurationSoundRecordingShuffleIterable;
import org.eng.util.CommandArgs;
import org.eng.util.IShuffleIterable;

/**
 * Extends the super class to support manipulation of the sounds, probably for training/evaluation.
 * @author DavidWood
 *
 */
public class GetModifiedSoundOptions extends GetSoundOptions {
	
	public final static int DEFAULT_CLIPLEN = 0;

	private final static String ClipOptionsHelp = 
			  "  -clipLen double : splits sound recordings up into clips of the given\n"
			+ "      number of milliseconds. Set to 0 to turn off.\n"
			+ "      Defaults to " + DEFAULT_CLIPLEN + "\n" 
			+ "  -pad (no|zero|duplicate): when clips are shorter than the requests clip\n"
			+ "      padding can be added to make all clips the same length. Some models may\n"
			+ "      require this.  Zero padding sets the added samples to zero.  Duplicate\n"
			+ "      reuses the sound as many times a necessary to set the added samples.\n"
			+ "      No padding removes clips shorter than the requested clip length.\n" 
			+ "      Default is no padding.\n"
		;

	@SuppressWarnings("hiding")
	public final static String OptionsHelp = 
		GetModifiedSoundOptions.ClipOnlyOptionsHelp 
		+ "  -balance: a flag that causes an equal number of sounds for each label\n"
		+ "      value using down sampling for evaluation and training if applicable.\n"
		+ "      Equivalent to '-balance-with down'.\n" 
//         xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
		+ "  -balance-with [up|down|N]: causes the sounds to be balanced across label\n"
		+ "      values for the named label. Samples are up/down-sampled to meet the\n"
		+ "      maximum, minumum or given count, of samples per label value, \n"
		+ "      respectively. Up sampling makes copies of under represented samples\n" 
		+ "      and down-sampling randomly selects from across the full set of samples.\n"
		+ "      All balancing is applied after clipping, if requested.\n" 
		// Not sure we understand the implications of this yet, but leave as the default and as a hidden option for train+evaluate.
//		+ "  -no-repeatability: a flag that causes the same set of data to be produced across\n"
//		+ "      data shuffling operations used during training when balancing the data.\n"  
//		+ "      this assures that the folds used the same across the multiple trainings.\n"
//		+ "      In general, this may be desireable, but will cause all data to be pulled\n"
//		+ "      into memory when using the -clipLen option.\n"
		+ "  -label <name> : required for most operations (i.e. training, evaluation)\n" 
		+ "      Required if balancing is requested regardless of the operation performed.\n" 
		;

	/**
	 * Use this when not enabling rebalancing or requiring a label.
	 */
	public final static String ClipOnlyOptionsHelp = 
			GetSoundOptions.OptionsHelp
			+ ClipOptionsHelp;
		

	private String label;
	private boolean requireLabel;

	private boolean enableBalancing;
	private IShuffleIterable<SoundRecording> clippedUnbalancedSounds;
	private int clipLenMsec = DEFAULT_CLIPLEN;

	/**
	 * A convenience on {@link #GetModifiedSoundOptions(boolean, boolean)} that enables balancing of sounds.
	 */
	public GetModifiedSoundOptions(boolean requireLabel) {
		this(requireLabel, true);
	}

	/**
	 * 
	 * @param requireLabel if true, then require the -label option.
	 * @param enableBalancing if true, then enable the -balance options.
	 */
	public GetModifiedSoundOptions(boolean requireLabel, boolean enableBalancing) {
		this.requireLabel = requireLabel;
		this.enableBalancing = enableBalancing;
	}


	/**
	 * Parse the options and establish the return value of {@link #getSounds()}.
	 * @param cmdargs
	 * @return true on now option errors.  false and issue an error message on stderr if an option error.
	 */
	public boolean parseOptions(CommandArgs cmdargs) {
		if (!super.parseOptions(cmdargs))
			return false;
		
		// Get the name of the model to use.
		this.label = cmdargs.getOption("label");
		if (requireLabel && this.label == null) {
			System.err.println("Label value must be specified with -label option");
			return false;
		}

		this.clipLenMsec = cmdargs.getOption("clipLen", DEFAULT_CLIPLEN); 

		PadType padType; 
		boolean repeatableShuffle = !cmdargs.getFlag("no-repeatabilitye"); 
		
		// Padding option
		String padOption = cmdargs.getOption("pad", "no");
		if (padOption.equals("zero"))
			padType = PadType.ZeroPad;
		else if (padOption.equals("duplicate"))
			padType = PadType.DuplicatePad;
		else
			padType = PadType.NoPad;

		boolean balancedLabels;
		boolean useUpSampling; 
		int balancedCount = 0;
		if (this.enableBalancing) {
			String balanceWith= cmdargs.getOption("balance-with"); 
			balancedLabels = cmdargs.getFlag("balance") || cmdargs.getFlag("balanced") || balanceWith != null;
			if (balancedLabels) {
				useUpSampling = true;	// The default
				if (balanceWith != null) {
					if (balanceWith.equals("down")) {
						useUpSampling = false; 
					} else if (balanceWith.equals("up")) {
						useUpSampling = true; 
					} else {
						try {
							balancedCount = Integer.parseInt(balanceWith);
						} catch (Exception e) {
							System.err.println("Could not parse balance-with option: " + balanceWith);
							return false;
						}
					}
				}
			} else {
				useUpSampling = false;
			}
			
			if (balancedLabels && this.label == null) {
				System.err.println("Label value must be specified with -label option when balancing sounds.");
				return false;
			}

		} else {
			useUpSampling = false;
			balancedLabels = false;
		}


		
		this.sounds = this.getRequestedSounds(this.sounds,this.label, repeatableShuffle, clipLenMsec, padType, balancedLabels,balancedCount, useUpSampling);

		return sounds != null;
	}

	/**
	 * Get the clipped sounds prior to being balanced. 
	 * @return
	 */
	public IShuffleIterable<SoundRecording> getClippedSounds() {
		return this.clippedUnbalancedSounds;
	}

	/**
	 * @param sounds
	 * @param trainingLabel
	 * @param repeatableShuffle
	 * @param clipLenMsec
	 * @param padType
	 * @param balancedLabels
	 * @param balancedCount 
	 * @param useUpSampling
	 * @return
	 */
	private IShuffleIterable<SoundRecording> getRequestedSounds(IShuffleIterable<SoundRecording> sounds,
			String trainingLabel, boolean repeatableShuffle, double clipLenMsec, PadType padType,
			boolean balancedLabels, int balancedCount, boolean useUpSampling) {
		if (clipLenMsec > 0) { 
			System.out.println("Sounds will be clipped into " + clipLenMsec + " millisecond clips (padding=" + padType.name() + ").");
			sounds = new FixedDurationSoundRecordingShuffleIterable(sounds, clipLenMsec, padType);
//			System.out.println(TrainingSetInfo.getInfo(sounds).prettyFormat());
//			if (repeatableShuffle && balancedLabels) {
//				System.err.println("NOTE: repeatable shufflability (the default) while balancing labels and clipping\n"
//								  +"      requires bringing all data into memory at once instead of streaming.  \n"
//								  +"      To disable repeatable shufflability use the -no-repeatability option.");
//				System.out.println(TrainingSetInfo.getInfo(sounds).prettyFormat());
//				sounds = new ShufflizingIterable<SoundRecording>(sounds);
//				System.out.println(TrainingSetInfo.getInfo(sounds).prettyFormat());
//			}
		}
		this.clippedUnbalancedSounds = sounds;
		if (balancedLabels) {
//			TrainingSetInfo tsi = TrainingSetInfo.getInfo(sounds);
//			int minLabels = findMinLabelCount(tsi, trainingLabel); 
//			System.out.println("Using a maximum of " + minLabels + " per label value to train and evaluate model.");
//			sounds = new BoundedLabeledWindowShuffleIterable<SoundRecording>(sounds, repeatableShuffle, trainingLabel, minLabels);
//			sounds = new BoundedLabelValueWindowShuffleIterable<SoundRecording>(sounds, repeatableShuffle, trainingLabel, minLabels);
			if (balancedCount > 0)  {
				System.out.println("Balancing training data with " + balancedCount + " samples per label value.");
				sounds = new BalancedLabeledWindowShuffleIterable<SoundRecording>(sounds, trainingLabel, balancedCount); 
			} else {
				System.out.println("Balancing training data using " + (useUpSampling ? "up sampling." : "down sampling."));
				sounds = new BalancedLabeledWindowShuffleIterable<SoundRecording>(sounds, trainingLabel, useUpSampling);
			}
//			System.out.println(TrainingSetInfo.getInfo(sounds).prettyFormat());
		}
		return sounds;
	}

	public String getLabel() {
		return this.label;
	}

	/**
	 * Get the non-zero clip length if specified in the options.
	 * @return 0 if not being clipped.
	 */
	public int getClipLenMsec() {
		return this.clipLenMsec; 
	}	
}
