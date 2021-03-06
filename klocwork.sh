#!/bin/bash
# TERM_DISPLAY_MODE can be "plain" or "color"
TERM_DISPLAY_MODE=color
TERM_STATUS_COLUMN=60
TERM_COLOR_GREEN="\\033[1;32m"
TERM_COLOR_CYAN="\\033[1;36m"
TERM_COLOR_RED="\\033[1;31m"
TERM_COLOR_YELLOW="\\033[1;33m"
TERM_COLOR_NORMAL="\\033[0;39m"

export BUILD_SPECS_DIRECTORY=/home/robot/kw_build_specs
export TABLES_DIRECTORY=/home/robot/kwtables
export KLOCWORK_PROJECT=dcg_security-director
export KLOCWORK_SERVER_URL=https://klocwork-jf18.devtools.intel.com:8160
export MAIN_PROJECT_SPEC=director.out
export KW_HOME=/home/robot/kw10.4/bin
PATH=$PATH:$KW_HOME

#Declare Associative Array for c projects
declare -A projectsArray
check=0
#Assign key value pairs, which correspond to [specFileName] = projectPath



#==========================================Functions==========================================
initialize() {
   mkdir -p "${BUILD_SPECS_DIRECTORY}"
   mkdir -p "${TABLES_DIRECTORY}"
}

generateBuildSpecs() {
   ant ready clean
   kwmaven --output "${BUILD_SPECS_DIRECTORY}/${MAIN_PROJECT_SPEC}" -DskipTests=true install
}

buildProject() {
   #Construct the kwbuildproject command with the cprojects appended
   kwBuildProjectCommand="kwbuildproject --url \"${KLOCWORK_SERVER_URL}/${KLOCWORK_PROJECT}\" --tables-directory \"${TABLES_DIRECTORY}\" --force \"${BUILD_SPECS_DIRECTORY}/$MAIN_PROJECT_SPEC\""
   #Run the kwbuildproject command
   eval $kwBuildProjectCommand
}

uploadResults() {
  kwadmin --url "${KLOCWORK_SERVER_URL}" load "${KLOCWORK_PROJECT}" "${TABLES_DIRECTORY}"
}

usageCheck(){
   while [[ $# -gt 1 ]]
      do
      key="$1"
      echo $key
   done
	echo $0
	echo $1
   if [ "$0" == "-h" ]; then
      echo "Usage: This klockwork script requires the following assumptions to be met:\n1. "
      exit 0
   fi
}

execPrereqCheck(){
   
   if [ -z "$BUILD_SPECS_DIRECTORY" ]; then
      echo_failure "BUILD_SPECS_DIRECTORY Env. variable is missing (i.e. /home/klocwork/kw_build_specs)"
     # return 1
	check=1
   fi
   if [ -z "$TABLES_DIRECTORY" ]; then
      echo_failure "TABLES_DIRECTORY Env. variable is missing (i.e. /home/klocwork/kwtables)"
     # return 1
	check=1
   fi
   if [ -z "$KLOCWORK_PROJECT" ]; then
      echo_failure "KLOCWORK_PROJECT Env. variable is missing (i.e. dcg_security-director)"
#      return 1
	check=1
   fi
   if [ -z "$KLOCWORK_SERVER_URL" ]; then
      echo_failure "KLOCWORK_SERVER_URL Env. variable is missing (i.e. https://klocwork-jf18.devtools.intel.com:8160)"
#      return 1
	check=1
   fi
   if [ -z "$MAIN_PROJECT_SPEC" ]; then
      echo_failure "MAIN_PROJECT_SPEC Env. variable is missing (i.e. director.out)"
#      return 1
	check=1
   fi
}

# Environment:
# - TERM_DISPLAY_MODE
# - TERM_DISPLAY_GREEN
# - TERM_DISPLAY_NORMAL
echo_success() {
  if [ "$TERM_DISPLAY_MODE" = "color" ]; then echo -en "${TERM_COLOR_GREEN}"; fi
  echo ${@:-"[  OK  ]"}
  if [ "$TERM_DISPLAY_MODE" = "color" ]; then echo -en "${TERM_COLOR_NORMAL}"; fi
  return 0
}

# Environment:
# - TERM_DISPLAY_MODE
# - TERM_DISPLAY_RED
# - TERM_DISPLAY_NORMAL
echo_failure() {
  if [ "$TERM_DISPLAY_MODE" = "color" ]; then echo -en "${TERM_COLOR_RED}"; fi
  echo ${@:-"[FAILED]"}
  if [ "$TERM_DISPLAY_MODE" = "color" ]; then echo -en "${TERM_COLOR_NORMAL}"; fi
  return 1
}

# Environment:
# - TERM_DISPLAY_MODE
# - TERM_DISPLAY_YELLOW
# - TERM_DISPLAY_NORMAL
echo_warning() {
  if [ "$TERM_DISPLAY_MODE" = "color" ]; then echo -en "${TERM_COLOR_YELLOW}"; fi
  echo ${@:-"[WARNING]"}
  if [ "$TERM_DISPLAY_MODE" = "color" ]; then echo -en "${TERM_COLOR_NORMAL}"; fi
  return 1
}

checkCommands() {
    matchadmin=$(which kwadmin | wc -l)
    matchbuild=$(which kwbuildproject | wc -l)
    matchmvn=$(which kwmaven | wc -l)
    if [ $matchadmin  -lt 1 ]; then echo_failure "The kwadmin Command is missing"
	check=1
        elif [ $matchbuild  -lt 1 ]; then echo_failure "The kwbuildproject Command is missing"
	check=1
            elif [ $matchmvn  -lt 1 ]; then echo_failure "The kwmaven Command is missing"
	 check=1
    fi
}

displayUsage() {
    
	echo "Usage:"
	echo -e "This script allows you to run the klocwork scan for the director projects. The script needs the following environment variables set before running: \n"
	echo -e " *BUILD_SPECS_DIRECTORY (i.e. /home/klocwork/kw_build_specs) \n *TABLES_DIRECTORY (i.e. /home/klocwork/kwtables) \n *KLOCWORK_PROJECT (i.e. dcg_security-director) \n *KLOCWORK_SERVER_URL (i.e. https://klocwork-jf18.devtools.intel.com:8160) \n *MAIN_PROJECT_SPEC (i.e. director.out) \n"
	echo "You also need to make sure your system is has the following Klocwork commands installed/available:"
	echo -e " *kwadmin \n *kwbuildproject \n *kwmaven"
}

#==========================================Script Execution==========================================
#usageCheck
#Check if prereqs

if [ -n "$1" ] && [ "$1" != "-h" ]; then 
exit -1; fi 
if [ "$1" == "-h" ]; then displayUsage
exit -1; fi
checkCommands
execPrereqCheck
if [ $check -eq 1 ]; then 
exit -1; fi

	echo "Running Initialize..."
	initialize
	echo "Running Generate Build Specs..."
	generateBuildSpecs
	echo "Running Build Project..."
	buildProject
	echo "Running Upload Results..."
#	uploadResults

	echo_success "Finished execution..."


