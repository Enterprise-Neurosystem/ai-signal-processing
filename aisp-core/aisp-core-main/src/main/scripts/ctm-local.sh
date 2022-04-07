#!/bin/bash
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
# Shows how to capture sound from the local environment, train a model on
# those sounds and then to begin monitoring the environment against the
# trained model. All classification results are sent to an IOT Platform 
# configured in iotp.props. 
# All sounds are stored in the file system, training is done on the local
# cpus and the model is stored locally in the file system. classification 
# is done locally. 
##############################################################################
# Start normal motor and capture 20 5 second sound clips in the local file system.
capture -label status=normal -dir ./sounds -n 2
# Start abnormal motor and capture 20 clips again
capture -label status=abnormal -dir ./sounds -n 2
# Train the model on the 40 sounds
train -label status -sound-dir ./sounds -output motor.cfr 
# Start monitoring the environment using the new motor model store in the file system and send events to iotp
classify -monitor -file motor.cfr -iotp-properties iotp.props -user cognitiveear@gmail.com -sensor-name rpi2

