@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM Maven Wrapper Start Up Batch script
@setlocal

set MAVEN_PROJECTBASEDIR=%CD%
set WRAPPER_JAR=.mvn\wrapper\maven-wrapper.jar
set MAVEN_USER_HOME=%USERPROFILE%\.m2

if not exist "%WRAPPER_JAR%" (
    echo Downloading Maven Wrapper...
    powershell -Command "Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar' -OutFile '%WRAPPER_JAR%'"
    if not exist "%WRAPPER_JAR%" (
        echo ERROR: Failed to download Maven Wrapper.
        exit /b 1
    )
)

set "JAVA_HOME=%JAVA_HOME%"
if not defined JAVA_HOME (
    echo ERROR: JAVA_HOME is not set.
    exit /b 1
)

"%JAVA_HOME%\bin\java.exe" ^
  -classpath "%WRAPPER_JAR%" ^
  "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" ^
  org.apache.maven.wrapper.MavenWrapperMain ^
  %*
