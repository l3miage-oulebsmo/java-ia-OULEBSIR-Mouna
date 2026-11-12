@echo off
title MoodTune Maven Wrapper
if "%HOME%"=="" set "HOME=%HOMEDRIVE%%HOMEPATH%"
set ERROR_CODE=0
setlocal

if not "%JAVA_HOME%"=="" goto OkJHome
for /d %%J in ("C:\Program Files\Microsoft\jdk-21*") do set "JAVA_HOME=%%J"
for /d %%J in ("C:\Program Files\Eclipse Adoptium\jdk-21*") do set "JAVA_HOME=%%J"
for /d %%J in ("C:\Program Files\Java\jdk-21*") do set "JAVA_HOME=%%J"
if not "%JAVA_HOME%"=="" goto OkJHome
echo JAVA_HOME introuvable. Installez un JDK 21. >&2
goto error

:OkJHome
if exist "%JAVA_HOME%\bin\java.exe" goto init
echo JAVA_HOME invalide : %JAVA_HOME% >&2
goto error

:init
set EXEC_DIR=%CD%
set WDIR=%EXEC_DIR%
:findBaseDir
if exist "%WDIR%\.mvn" goto baseDirFound
cd ..
if "%WDIR%"=="%CD%" goto baseDirNotFound
set WDIR=%CD%
goto findBaseDir

:baseDirFound
set MAVEN_PROJECTBASEDIR=%WDIR%
cd "%EXEC_DIR%"
goto endDetectBaseDir

:baseDirNotFound
set MAVEN_PROJECTBASEDIR=%EXEC_DIR%
cd "%EXEC_DIR%"

:endDetectBaseDir
SET MAVEN_JAVA_EXE="%JAVA_HOME%\bin\java.exe"
set WRAPPER_JAR="%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"
set WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain
set WRAPPER_URL=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar

if exist %WRAPPER_JAR% goto run
powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; (New-Object System.Net.WebClient).DownloadFile('%WRAPPER_URL%', '%WRAPPER_JAR%')"
if ERRORLEVEL 1 goto error

:run
%MAVEN_JAVA_EXE% -classpath %WRAPPER_JAR% "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" %WRAPPER_LAUNCHER% %*
if ERRORLEVEL 1 goto error
goto end

:error
set ERROR_CODE=1

:end
endlocal & exit /B %ERROR_CODE%
