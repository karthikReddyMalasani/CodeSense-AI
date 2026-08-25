@echo off
setlocal

if "%JAVA_HOME%"=="" set JAVA_HOME=C:\Program Files\Java\jdk-23

"C:\Users\Karth\apache-maven-3.9.9\bin\mvn.cmd" ^
  clean ^
  spring-boot:run ^
  -f backend\pom.xml ^
  "-Dspring-boot.run.jvmArguments=-DDATABASE_URL=%DATABASE_URL% -DDATABASE_USERNAME=%DATABASE_USERNAME% -DDATABASE_PASSWORD=%DATABASE_PASSWORD% -DJWT_SECRET=%JWT_SECRET% -DAI_LLM_PROVIDER=groq -DAI_EMBEDDING_PROVIDER=mock -DGROQ_API_KEY=%GROQ_API_KEY% -DGROQ_MODEL=llama-3.1-8b-instant"
