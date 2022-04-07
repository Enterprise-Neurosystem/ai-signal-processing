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
# All sounds are stored in the server, training is done on the server and 
# the model is stored in the server. Classification is done in the server. 
##############################################################################
# start normal motor and capture 20 5 second sound clips in the server with the name motor
capture -label status=normal -name motor -host localhost -port 8080 -user cognitiveear@gmail.com -pwd ac0ustic -n 2 
# Start abnormal motor and capture 20 clips again
capture -label status=abnormal -name motor -n 2 -host localhost -port 8080 -user cognitiveear@gmail.com -pwd ac0ustic 
# Train the model on the 40 sounds
train -label status -sound-name motor -train remote -model-name motor -host localhost -port 8080 -user cognitiveear@gmail.com 
# Start monitoring the environment using the motor model stored in the server and send events to iotp
classify -monitor -name motor -iotp-properties iotp.props -sensor-name rpi2 -host localhost -port 8080 -user cognitiveear@gmail.com 
