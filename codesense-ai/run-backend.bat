@echo off
setlocal

if "%JAVA_HOME%"=="" set JAVA_HOME=C:\Program Files\Java\jdk-23

if "%DATABASE_URL%"=="" set DATABASE_URL=jdbc:postgresql://localhost:5432/codesense
if "%DATABASE_USERNAME%"=="" set DATABASE_USERNAME=codesense
if "%DATABASE_PASSWORD%"=="" set DATABASE_PASSWORD=password
if "%JWT_SECRET%"=="" set JWT_SECRET=please-change-this-secret-key-in-production-it-must-be-at-least-64-characters-long
if "%AI_LLM_PROVIDER%"=="" set AI_LLM_PROVIDER=gemini
if "%AI_EMBEDDING_PROVIDER%"=="" set AI_EMBEDDING_PROVIDER=mock
if "%GEMINI_MODEL%"=="" set GEMINI_MODEL=gemini-2.5-flash

echo Starting CodeSense AI Backend...
echo Database URL: %DATABASE_URL%

call "%~dp0backend\mvnw.cmd" clean spring-boot:run -f "%~dp0backend\pom.xml" "-Dspring-boot.run.jvmArguments=-DDATABASE_URL=%DATABASE_URL% -DDATABASE_USERNAME=%DATABASE_USERNAME% -DDATABASE_PASSWORD=%DATABASE_PASSWORD% -DJWT_SECRET=%JWT_SECRET% -DAI_LLM_PROVIDER=%AI_LLM_PROVIDER% -DAI_EMBEDDING_PROVIDER=%AI_EMBEDDING_PROVIDER% -DGEMINI_API_KEY=%GEMINI_API_KEY% -DGEMINI_MODEL=%GEMINI_MODEL%"

