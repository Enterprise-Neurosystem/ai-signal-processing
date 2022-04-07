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
import argparse
from argparse import RawTextHelpFormatter
from argparse import ArgumentDefaultsHelpFormatter
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
        'Converts a labels file produced by Audacity to metadata.csv format on the\n' 
        'standard output.  The input Audacity label file contains 1 or more rows, each\n' 
        'with 3 white-space separated columns. Each row defines a labeled segment and\n'
        'is converted to a single row in the metadata output. Columns are defined as \n'
        'follows:\n'
        '  1: the offset in seconds of the start of the segment\n'
        '  2: the offset in seconds of the end of the segment\n'
        '  3: the single label value to assign to the segment\n' 
        'For example, converting the following labels.txt:\n'
        '  1.3  2.1 abnormal\n'
        '  3.0  4.0 abnormal\n'
        'using options --wav mysound.wav --gap-label-value normal --label state\n'
        'produces:\n'
        '  mysound.wav[0-1300],state=normal,\n'
        '  mysound.wav[1300-2100],state=abnormal,\n'
        '  mysound.wav[2100-3000],state=normal,\n'
        '  mysound.wav[3000-4000],state=abnormal,\n'
        '\nNote that a trailing gap segment is currently not emitted.\n'
        ,formatter_class=RawTextHelpFormatter)
    argp.add_argument('-wav', help='Specifies name of the wav file to which the labels apply', default=None, required=True, type=str)
    argp.add_argument('-audacity', help='Specifies the name of the Audacity labels file. Default is labels.txt.', default="labels.txt", type=str)
    argp.add_argument('-label', help='The label name to use in the output. Default is state.', default='state', type=str)
    argp.add_argument('-gap-label-value', help='The label value to apply to the gaps in the labels specified in the Audacity labels file. Default is None and will not be applied.', default=None, type=str)
    has_freq = False
    args = argp.parse_args()
    labels_file=args.audacity
    wav_file=args.wav
    with wave.open(wav_file,'r') as wave:
        clip_len_msec = int(1000 * wave.getnframes() / wave.getframerate())
    label_name=args.label
    gap_label=args.gap_label_value
    last_end = 0
    with open(labels_file) as in_file: 
        for line in in_file: 
            fields=line.split('\t')
            if '\\' in fields[0]:   # Skip frequency information, for now.
                has_freq = True
                continue
            start_msec = int(float(fields[0]) * 1000)
            end_msec   = int(float(fields[1]) * 1000);
            label = fields[2].rstrip('\n')
            if gap_label is not None and start_msec > last_end:
                # Echo gap label
                as_metadata(wav_file, last_end, start_msec, label_name, gap_label)
            as_metadata(wav_file, start_msec, end_msec, label_name, label)
            last_end = end_msec 
    if gap_label is not None and last_end < clip_len_msec:
        as_metadata(wav_file, last_end, clip_len_msec, label_name, gap_label)
    if has_freq: 
        print("Labels file contains frequency ranges.  These are not included in the output.", file=sys.stderr)
