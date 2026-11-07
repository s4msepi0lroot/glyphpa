@echo off
setlocal
set GRADLE_VERSION=8.9
where gradle >nul 2>nul
if %ERRORLEVEL% EQU 0 (
  gradle %*
  exit /b %ERRORLEVEL%
)
set GRADLE_BASE=%USERPROFILE%\.gradle\tapglyph-distributions
set GRADLE_HOME=%GRADLE_BASE%\gradle-%GRADLE_VERSION%
if not exist "%GRADLE_HOME%\bin\gradle.bat" (
  if not exist "%GRADLE_BASE%" mkdir "%GRADLE_BASE%"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile '%GRADLE_BASE%\gradle-%GRADLE_VERSION%-bin.zip'; Expand-Archive -Force '%GRADLE_BASE%\gradle-%GRADLE_VERSION%-bin.zip' '%GRADLE_BASE%'"
  if errorlevel 1 exit /b 1
)
call "%GRADLE_HOME%\bin\gradle.bat" %*
endlocal
