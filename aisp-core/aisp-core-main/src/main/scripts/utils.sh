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
get_os() {
   rh=$(cat /etc/os-release | grep NAME | grep "Red")
   if [ ! -z "$rh" ]; then
     echo "redhat"
     return 0
   fi 
   ub=$(cat /etc/os-release | grep NAME | grep "Ubuntu")
   if [ ! -z "$ub" ]; then
     echo "ubuntu" 
     return 0
   fi
   deb=$(cat /etc/os-release | grep NAME | grep "Debian")
   if [ ! -z "$deb" ]; then
     echo "debian" 
     return 0
   fi  
   echo "unknown"
   return 1
}
install_packages() {
   packages=$*
   os=$(get_os)
   echo $os
   case $os in
    redhat) yum install -y $packages ;;
    ubuntu) 
  	needs_install=
	for package in $packages; do
	  exists=$(dpkg --list $package | grep '^ii' 2>/dev/null)
	  # Try and avoid permission denied error if already installed and not running as root
	  if [ -z "$exists" ]; then	
	    needs_install="$needs_install $package"
	  else
	    echo $package already installed.
	  fi
 	done
	if [ ! -z "$needs_install" ]; then	
	    sudo add-apt-repository -y ppa:deadsnakes/ppa  # for python3.9
	    sudo apt-get update && sudo apt-get -y install --no-install-recommends $needs_install
  	fi
	;;
    debian) 
  	needs_install=
	for package in $packages; do
	  exists=$(dpkg --list $package | grep '^ii' 2>/dev/null)
	  # Try and avoid permission denied error if already installed and not running as root
	  if [ -z "$exists" ]; then
	    echo "$package"
        if [ "$package" != "alsa-base" ]; then	
	    	needs_install="$needs_install $package"
        fi
	  else
	    echo $package already installed.
	  fi
 	done
 	echo "$needs_install"
	if [ ! -z "$needs_install" ]; then	
	    sudo add-apt-repository -y ppa:deadsnakes/ppa  # for python3.9
	    sudo apt-get update && sudo apt-get -y install --no-install-recommends $needs_install
  	fi
	;;
    *) echo Unknown O/S $os; return 1 ;;
  esac
}
