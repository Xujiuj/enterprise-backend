@echo off
setlocal

set "APP_HOME=%~dp0..\.."
set "JAR_PATH=%APP_HOME%\ruoyi-admin\target\ruoyi-admin.jar"
set "CONFIG_DIR=%APP_HOME%\config\"

java %JAVA_OPTS% -jar "%JAR_PATH%" --spring.profiles.active=prod --spring.config.additional-location="optional:file:%CONFIG_DIR%"

endlocal
