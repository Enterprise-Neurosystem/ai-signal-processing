#*******************************************************************************
# * Copyright [2022] [IBM]
# *
# * Licensed under the Apache License, Version 2.0 (the "License");
# * you may not use this file except in compliance with the License.
# * You may obtain a copy of the License at
# *
# *     http://www.apache.org/licenses/LICENSE-2.0
# *
# * Unless required by applicable law or agreed to in writing, software
# * distributed under the License is distributed on an "AS IS" BASIS,
# * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# * See the License for the specific language governing permissions and
# * limitations under the License.
# *******************************************************************************
from argparse import ArgumentDefaultsHelpFormatter
from argparse import RawTextHelpFormatter
import argparse
import os
import sys
import wave


def as_metadata(wav_file, start_msec, end_msec, label_name, label_value):
    if start_msec is None or end_msec is None:
        print("{},{}={},".format(wav_file, label_name,label_value))
    else: 
        print("{}[{}-{}],{}={},".format(wav_file, start_msec, end_msec, label_name,label_value))

if __name__ == '__main__':
    argp = argparse.ArgumentParser(description=
        #xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
        'Converts an AISP metadata labels file to 1 or more Audacity label files.n' 
        'The input Audacity label file contains 1 or more rows, each\n' 
        'The input metadata file has segmented audio labeling. For example,'
        '  mysound1.wav[1300-2100],state=abnormal,\n'
        '  mysound1.wav[2100-3000],state=normal,\n'
        '  mysound2.wav[1000-4000],state=abnormal,\n'
        'Each wav file listed in the metadata file will have a corresponding .txt'
        'file with the same base name as the .wav file.'
        'The output is 3 white-space separated columns. Each row defines a labeled\n'
        'segment. Columns are defined as follows:\n'
        '  1: the offset in seconds of the start of the segment\n'
        '  2: the offset in seconds of the end of the segment\n'
        '  3: the single label value to assign to the segment\n' 
        ,formatter_class=RawTextHelpFormatter)
    argp.add_argument('metadata_file', help='Specifies name of the metadata file to be converted to 1 or more Audacity files', default=None, 
                      type=str, nargs=1)
    # argp.add_argument('-wav', help='Specifies name of the wav file to which the labels apply', default=None, required=True, type=str)
    # argp.add_argument('-audacity', help='Specifies the name of the Audacity labels file. Default is labels.txt.', default="labels.txt", type=str)
    # argp.add_argument('-label', help='The label name to use in the output. Default is state.', default='state', type=str)
    # argp.add_argument('-gap-label-value', help='The label value to apply to the gaps in the labels specified in the Audacity labels file. Default is None and will not be applied.', default=None, type=str)
    args = argp.parse_args()
    metadata_file = args.metadata_file[0]
    segmentations = {}
    with open(metadata_file) as in_file: 
        for line in in_file: 
            fields=line.split(',')
            file = fields[0] 
            label=fields[1]
            if ';' in label:
                label = label.split(';')    # Take only the first label, for now.
                label = label[0]            
            label = label.split('=')[1] #  label value
            segment = [ label ]                # The whole file
            if '[' in file:
                sp = file.split('[')
                file = sp[0]
                right = sp[1] 
                numbers = right.split(']')[0]
                numbers = numbers.split('-')
                start = int(numbers[0]) / 1000.0   
                end = int(numbers[1]) / 1000.0
                segment = [label, start, end]
            else:
                print("No segment information for file " + file + ". Skipping.")
            segments = segmentations.get(file)
            if segments is None:
                segments = [ segment ] 
            else:
                segments.append(segment)
            segmentations[file] = segments
        
    for file, segments in segmentations.items():
            basename = os.path.basename(file)
            labels_file = basename.replace(".wav",".txt")
            with open(labels_file,'w') as in_file:
                for segment in segments: 
                    if len(segment) > 1:   #segmented
                        label = segment[0]
                        start = segment[1]
                        end = segment[2]
                        in_file.write("\t" + str(start) + "\t" + str(end) + "\t" + label + "\n")
            print("Wrote " + labels_file)
