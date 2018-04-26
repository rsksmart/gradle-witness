#!/bin/bash

GRADLE_WRAPPER="0f49043be582d7a39b671f924c66bd9337b92fa88ff5951225acc60560053067"
DOWNLOADED_HASH1=''
DOWNLOAD_FILE1=$(dd if=/dev/urandom bs=64 count=1 2>/dev/null| od -t x8 -A none  | tr -d ' '\\n)
unamestr=`uname`

function downloadJar(){
	platform
	curl https://deps.rsklabs.io/gradle-wrapper.jar -o ~/$DOWNLOAD_FILE1
	if [[ $PLATFORM == 'linux' || $PLATFORM == 'windows' ]]; then
		DOWNLOADED_HASH1=$(sha256sum ~/${DOWNLOAD_FILE1} | cut -d' ' -f1)
	elif [[ $PLATFORM == 'mac' ]]; then
		DOWNLOADED_HASH1=$(shasum -a 256 ~/${DOWNLOAD_FILE1} | cut -d' ' -f1)
	fi
	if [[ $GRADLE_WRAPPER != $DOWNLOADED_HASH1 ]]; then
		rm -f ~/${DOWNLOAD_FILE1}
		exit 1
	else
		mv ~/${DOWNLOAD_FILE1} ./gradle/wrapper/gradle-wrapper.jar
		rm -f ~/${DOWNLOAD_FILE1}
	fi
}

function platform() {
	if [[ "$unamestr" == 'Linux' ]]; then
		PLATFORM='linux'
	elif [[ "$unamestr" == 'Darwin' ]]; then
		PLATFORM='mac'
	elif [[ "$unamestr" =~ 'MINGW' ]]; then
		PLATFORM='windows'
	else
			echo -e "\e[1m\e[31m[ ERROR ]\e[0m UNRECOGNIZED PLATFORM"
			exit 2
	fi
}

downloadJar

exit 0
