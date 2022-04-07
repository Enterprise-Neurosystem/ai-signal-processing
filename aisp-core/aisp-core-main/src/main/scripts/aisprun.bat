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

if "%AISP_HOME%" == "" goto no_home_error

REM This is how to get the output of a command into a variable. 
REM Equivalent in shell is XXX_CP=`caajars`
REM for /F %%X in ('caajars.bat') do SET XXX_CP=%%X
set CLASSPATH=%AISP_HOME%/lib/*;%CLASSPATH%

REM Separate out the "-Dfoo=bar" parameters to make sure they go at the beginning.
:checkparams
REM Remove "" from argument, which must include them otherwise DOS splits on the '=' sign.
set PARAM=%~1
if "%PARAM%" == "" goto endcheckparams
if "%PARAM:~0,2%" == "-D" goto DO_D_PARAM
set NON_D_PARAMS=%NON_D_PARAMS% "%PARAM%"
SHIFT
goto checkparams
:DO_D_PARAM
set D_PARAMS=%D_PARAMS% "%PARAM%"
SHIFT
goto checkparams
:endcheckparams


set WPML_HOME=%AISP_HOME%
REM Do the actual work!
REM @echo ON
java %JAVA_OPTIONS%  %D_PARAMS% -Djava.util.logging.config.file=%AISP_HOME%/lib/caa.properties -Dlogback.configurationFile=%AISP_HOME%/lib/logback.xml %NON_D_PARAMS%
goto end

:no_home_error
echo AISP_HOME environment variable must be set.

:end
@rem End local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" endlocal
