# e-commerce

## Backend build

Use JDK 27 and the included Maven wrapper (Maven 3.10.0). Spring Boot
4.1.2 manages the Spring dependencies and Lombok 1.18.48; Connector/J is
updated separately in `backend/pom.xml`.

In PowerShell, select your installed JDK before building:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-27.0.1.0.3'
cd backend
.\mvnw.cmd -version
.\mvnw.cmd -U clean verify
```

In IntelliJ IDEA, select JDK 27 for both the project SDK and the Maven
importer/runner, then reload all Maven projects. Setting `java.version` in
the POM selects the compiler target; it does not select the JDK running Maven.

## JWT signing key

Running the application requires `APP_JWT_SECRET`, a Base64-encoded random
key of at least 32 bytes. For a local PowerShell session, run this from `backend`:

```powershell
$jwtKey = New-Object byte[] 32
$jwtRandom = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$jwtRandom.GetBytes($jwtKey)
$jwtRandom.Dispose()
$env:APP_JWT_SECRET = [Convert]::ToBase64String($jwtKey)
.\mvnw.cmd spring-boot:run
```

For deployments, supply a persistent secret through the environment; changing
the key invalidates existing tokens. The context test generates its own temporary
key and does not require this environment variable.
