$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

function Resolve-JavaHome {
    if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME "bin\java.exe"))) {
        return $env:JAVA_HOME
    }
    $java = Get-Command java -ErrorAction SilentlyContinue
    if ($java) {
        $home = Split-Path (Split-Path $java.Source)
        if (Test-Path (Join-Path $home "bin\java.exe")) {
            return $home
        }
    }
    $candidates = @(
        Get-ChildItem "C:\Program Files\Microsoft\jdk-21*" -Directory -ErrorAction SilentlyContinue
        Get-ChildItem "C:\Program Files\Eclipse Adoptium\jdk-21*" -Directory -ErrorAction SilentlyContinue
        Get-ChildItem "C:\Program Files\Java\jdk-21*" -Directory -ErrorAction SilentlyContinue
    )
    if ($candidates) {
        return $candidates[0].FullName
    }
    throw "JDK 21 introuvable. Installe un JDK et relance."
}

$env:JAVA_HOME = Resolve-JavaHome
Write-Host "JAVA_HOME = $env:JAVA_HOME"
Write-Host "Compilation + lancement de MoodTune..."
& "$PSScriptRoot\mvnw.cmd" javafx:run
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}
