@echo off
setlocal

set "APP_HOME=%~dp0..\.."
set "JAR_PATH=%APP_HOME%\ruoyi-admin\target\ruoyi-admin.jar"

java -jar "%JAR_PATH%" --enterprise-setup %*

endlocal
