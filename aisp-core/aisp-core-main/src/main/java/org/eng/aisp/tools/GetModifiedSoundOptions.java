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

import java.util.ArrayList;
import java.util.List;

import org.eng.aisp.IDataWindow.PadType;
import org.eng.aisp.SoundRecording;
import org.eng.aisp.util.BalancedLabeledWindowShuffleIterable;
import org.eng.aisp.util.FixedDurationSoundRecordingShuffleIterable;
import org.eng.util.CommandArgs;
import org.eng.util.IShuffleIterable;
import org.eng.util.IterableIterable;
import org.eng.util.ShufflizingIterable;

/**
 * Extends the super class to support manipulation of the sounds, probably for training/evaluation.
 * @author DavidWood
 *
 */
public class GetModifiedSoundOptions extends GetSoundOptions {
	
	public final static int DEFAULT_CLIPLEN = 0;

	public final static int DEFAULT_CLIPSHIFT = 0;

	private final static String ClipLenOptionsHelp = 
             // xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
			  "  -clipLen int: splits sound recordings up into clips of the given\n"
			+ "      number of milliseconds. Set to 0 to turn off.\n"
			+ "      Defaults to " + DEFAULT_CLIPLEN + ".\n" 
			+ "  -pad (no|zero|duplicate): when clips are shorter than the requests clip\n"
			+ "      padding can be added to make all clips the same length. Some models may\n"
			+ "      require this.  Zero padding sets the added samples to zero.  Duplicate\n"
			+ "      reuses the sound as many times a necessary to set the added samples.\n"
			+ "      No padding removes clips shorter than the requested clip length.\n" 
			+ "      Default is no padding.\n"
		;

	private final static String ClipShiftOptionsHelp = 
            // xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
			  "  -clipShift int : defines the time difference between the start times of\n" 
			+ "      the sub-windows in milliseconds. Not used unless -clipLen option is set.\n"
			+ "      Set to 0 or the clipLen value to define rolling windows. Set to half the\n"
			+ "      clipLen to define sub-windows in which the last half of a window overlaps\n"
			+ "      with the first half of the window following it. In general, this value \n"
			+ "      should be less than the clipLen value.\n" 
			+ "      Defaults to " + DEFAULT_CLIPSHIFT + ".\n" 
		;

		
	private final static String BalanceOptionsHelp = 
		  "  -balance: a flag that causes an equal number of sounds for each label\n"
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
		;
	
	private static final String LabelOptionsHelp = 
		  "  -label <name> : required for most operations (i.e. training, evaluation)\n" 
		+ "      Required if balancing is requested regardless of the operation performed.\n" 
		;
	public static final String OptionsWithoutBalanceHelp =
		GetModifiedSoundOptions.ClipLenOnlyOptionsHelp 
		+ GetModifiedSoundOptions.ClipShiftOptionsHelp 
		+ LabelOptionsHelp
		;

	@SuppressWarnings("hiding")
	public final static String OptionsHelp = 
		GetModifiedSoundOptions.OptionsWithoutBalanceHelp 
		+ BalanceOptionsHelp
		;

	/**
	 * Use this when not enabling rebalancing or requiring a label.
	 */
	public final static String ClipLenOnlyOptionsHelp = 
			GetSoundOptions.OptionsHelp
			+ ClipLenOptionsHelp;

		

	private String label;
	private boolean requireLabel;

	private boolean enableBalancing;
	private boolean enableClipShift;
	private IShuffleIterable<SoundRecording> clippedUnbalancedSounds;
	private int clipLenMsec = DEFAULT_CLIPLEN;
	private int clipShiftMsec = DEFAULT_CLIPLEN;

//	/**
//	 * A convenience on {@link #GetModifiedSoundOptions(boolean, boolean)} that enables balancing of sounds.
//	 */
//	public GetModifiedSoundOptions(boolean requireLabel) {
//		this(requireLabel, true);
//	}

	/**
	 * 
	 * @param requireLabel if true, then require the -label option.
	 * @param enableBalancing if true, then enable the -balance options.
	 */
	public GetModifiedSoundOptions(boolean requireLabel, boolean enableBalancing, boolean enableClipShift) {
		this.requireLabel = requireLabel;
		this.enableBalancing = enableBalancing;
		this.enableClipShift = enableClipShift;
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

		if (enableClipShift) {
			if (cmdargs.hasArgument("clipShift") && !cmdargs.hasArgument("clipLen")) {
				System.err.println("clipShift argument is only valid with the clipLen argument.");
				return false;
			}
			this.clipShiftMsec = cmdargs.getOption("clipShift", DEFAULT_CLIPSHIFT); 
		}
		this.clipLenMsec = cmdargs.getOption("clipLen", DEFAULT_CLIPLEN); 

		PadType padType; 
		boolean repeatableShuffle = !cmdargs.getFlag("no-repeatability"); 
		
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


		
		this.sounds = this.getRequestedSounds(this.sounds,this.label, repeatableShuffle, clipLenMsec, this.clipShiftMsec, padType, balancedLabels,balancedCount, useUpSampling);

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
	 * @param clipShiftMsec2 
	 * @param padType
	 * @param balancedLabels
	 * @param balancedCount 
	 * @param useUpSampling
	 * @return
	 */
    // xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
	// Sounds will be clipped every 50 msec into 100.0 msec clips (padding=NoPad)
	private IShuffleIterable<SoundRecording> getRequestedSounds(IShuffleIterable<SoundRecording> sounds,
			String trainingLabel, boolean repeatableShuffle, int clipLenMsec, int clipShiftMsec, PadType padType,
			boolean balancedLabels, int balancedCount, boolean useUpSampling) {
		if (clipLenMsec > 0) { 
			String msg;
			if (clipShiftMsec != 0)
				msg = "Sounds will be clipped every " + clipShiftMsec + " msec into " + clipLenMsec + " msec clips (padding=" + padType.name() + ")";
			else
				msg = "Sounds will be clipped every " + clipLenMsec + " msec into " + clipLenMsec + " msec clips (padding=" + padType.name() + ")";
			System.out.println(msg);
			sounds = new FixedDurationSoundRecordingShuffleIterable(sounds, clipLenMsec, clipShiftMsec, padType);
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
		if (balancedLabels) 
			sounds = getBalancedSounds(sounds, trainingLabel, balancedCount, useUpSampling);
		return sounds;
	}


	/**
	 * Get a iterable of balanced sounds for 1 or more training labels.
	 * @param sounds
	 * @param trainingLabel 1 or more training labels separated by commas.
	 * @param balancedCount
	 * @param useUpSampling
	 * @return a single iterable that balances label values within each training label.
	 */
	private IShuffleIterable<SoundRecording> getBalancedSounds(IShuffleIterable<SoundRecording> sounds,
			String trainingLabel, int balancedCount, boolean useUpSampling) {
		// This the usual case, a single training label.
		if (!trainingLabel.contains(","))
			return  this.getSingleLabelBalancedSounds(sounds, trainingLabel, balancedCount, useUpSampling);
		
		// Support models that take a list of comma-spearated labels
		String[] trainingLabels = trainingLabel.split(",");
		List<IShuffleIterable<SoundRecording>> soundList = new ArrayList<>();
		for (String label: trainingLabels) {
			IShuffleIterable<SoundRecording> iter = this.getSingleLabelBalancedSounds(sounds, label, balancedCount, useUpSampling);
			soundList.add(iter);
		}
		// This is not optimal as we don't have a ShuffleIterableIterable and so have to use IterableIterable with ShufflizingIterable.
		Iterable<SoundRecording> allSounds = new IterableIterable<SoundRecording>(soundList);
		sounds = new ShufflizingIterable<SoundRecording>(allSounds);
		return sounds;

	}

	/**
	 * Get a set of balanced sounds for the given training label which is assumed not to be a multi-label (csv separated).
	 * @param sounds
	 * @param trainingLabel
	 * @param balancedCount
	 * @param useUpSampling
	 * @return
	 */
	private IShuffleIterable<SoundRecording> getSingleLabelBalancedSounds(IShuffleIterable<SoundRecording> sounds,
			String trainingLabel, int balancedCount, boolean useUpSampling) {
		if (trainingLabel.contains(","))
			throw new IllegalArgumentException("Training label must not be component/multi, but contains a comma indicating such.");
		
		//			TrainingSetInfo tsi = TrainingSetInfo.getInfo(sounds);
//			int minLabels = findMinLabelCount(tsi, trainingLabel); 
//			System.out.println("Using a maximum of " + minLabels + " per label value to train and evaluate model.");
//			sounds = new BoundedLabeledWindowShuffleIterable<SoundRecording>(sounds, repeatableShuffle, trainingLabel, minLabels);
//			sounds = new BoundedLabelValueWindowShuffleIterable<SoundRecording>(sounds, repeatableShuffle, trainingLabel, minLabels);
		if (balancedCount > 0)  {
			System.out.println("Balancing training data with " + balancedCount + " samples per " + trainingLabel + " label value.");
			sounds = new BalancedLabeledWindowShuffleIterable<SoundRecording>(sounds, trainingLabel, balancedCount); 
		} else {
			System.out.println("Balancing training data using " + (useUpSampling ? "up sampling." : "down sampling on label " + trainingLabel));
			sounds = new BalancedLabeledWindowShuffleIterable<SoundRecording>(sounds, trainingLabel, useUpSampling);
		}
//			System.out.println(TrainingSetInfo.getInfo(sounds).prettyFormat());
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
