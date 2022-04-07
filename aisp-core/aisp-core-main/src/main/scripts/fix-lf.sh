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
# This is used to fix CR/LF issues on script files in this directory.
# It seems that sometimes these files can have a ^M embedded in them
# so that when running on Linux systems. they fail with an error message.
# This can be fixed by running this script in this directory as follows
# bash ./fix-lf.sh
files=$(ls | grep -v '\.bat' | grep -v '\.sh')
for f in $files; do
    echo -n Updating file $f
    cat $f | sed -e 's/\//g' > $f.tmp
    mv $f.tmp $f
    chmod +x $f
    echo " done."
done

 
