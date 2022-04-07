@ECHO OFF 
REM ******************************************************************************
REM * Copyright [2022] [IBM]
REM *
REM * Licensed under the Apache License, Version 2.0 (the "License");
REM * you may not use this file except in compliance with the License.
REM * You may obtain a copy of the License at
REM *
REM *     http://www.apache.org/licenses/LICENSE-2.0
REM *
REM * Unless required by applicable law or agreed to in writing, software
REM * distributed under the License is distributed on an "AS IS" BASIS,
REM * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
REM * See the License for the specific language governing permissions and
REM * limitations under the License.
REM *******************************************************************************

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal
set CAACLASS=org.eng.aisp.tools.Train

REM Try and set AISP_HOME and PATH automatically assuming this script is being run from AISP_HOME/bin
if NOT [%AISP_HOME%] == [] goto homeset
REM %~dp0 is the directory containing this file (see http://www.microsoft.com/resources/documentation/windows/xp/all/proddocs/en-us/percent.mspx)
set AISP_HOME=%~dp0..
echo Setting AISP_HOME automatically to %AISP_HOME%
set PATH=%AISP_HOME%\bin;%PATH%
:homeset

REM Do the actual work!
aisprun %CAACLASS%  %*

@rem End local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" endlocal
