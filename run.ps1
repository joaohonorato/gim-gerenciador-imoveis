param(
    [ValidateSet('backend', 'frontend', 'e2e', 'all', 'dev')]
    [string]$Target = 'all',
    [switch]$ReuseExisting,
    [switch]$NoInstall,
    [switch]$KeepServices,
    [int]$BackendPort = 8080,
    [int]$FrontendPort = 19006
)

$ErrorActionPreference = 'Stop'

$rootDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$backendDir = Join-Path $rootDir 'backend'
$frontendDir = Join-Path $rootDir 'frontend'
$playwrightConfig = Join-Path $frontendDir 'e2e/playwright.config.ts'
$dockerComposeFile = Join-Path $rootDir 'docker-compose.yml'
$envLocalFile = Join-Path $rootDir '.env.local'

function Write-Step {
    param([string]$Message)
    Write-Host "`n==> $Message" -ForegroundColor Cyan
}

function Import-LocalEnv {
    if (-not (Test-Path $envLocalFile)) {
        return
    }

    Write-Step 'Carregando variaveis de .env.local...'
    foreach ($line in Get-Content $envLocalFile) {
        $trimmed = $line.Trim()
        if (-not $trimmed -or $trimmed.StartsWith('#')) {
            continue
        }

        $parts = $trimmed.Split('=', 2)
        if ($parts.Count -ne 2) {
            continue
        }

        $name = $parts[0].Trim()
        $value = $parts[1].Trim()

        if (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'"))) {
            $value = $value.Substring(1, $value.Length - 2)
        }

        Set-Item -Path "env:$name" -Value $value
    }
}

function Ensure-FrontendDependencies {
    if ($NoInstall) {
        return
    }

    if (-not (Test-Path (Join-Path $frontendDir 'node_modules'))) {
        Write-Step 'Instalando dependencias do frontend (npm install)...'
        Push-Location $frontendDir
        try {
            npm install
            if ($LASTEXITCODE -ne 0) {
                throw 'npm install falhou.'
            }
        }
        finally {
            Pop-Location
        }
    }
}

function Wait-HttpOk {
    param(
        [Parameter(Mandatory = $true)][string]$Url,
        [int]$TimeoutSeconds = 120,
        [string]$Name = 'servico'
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        try {
            $response = Invoke-WebRequest -Uri $Url -Method Get -UseBasicParsing -TimeoutSec 3
            if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 500) {
                Write-Host "$Name pronto em $Url"
                return
            }
        }
        catch {
            # continua tentando ate o timeout
        }

        Start-Sleep -Seconds 2
    }

    throw "Timeout aguardando $Name em $Url"
}

function Ensure-LocalDatabase {
    if (-not (Test-Path $dockerComposeFile)) {
        throw "docker-compose.yml nao encontrado em $dockerComposeFile"
    }

    # Verifica se o daemon do Docker esta disponivel para evitar erro opaco do npipe.
    docker info | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw @"
Docker daemon nao esta em execucao.
- No Windows, inicie o Docker Desktop e aguarde o status "Engine running".
- Depois execute novamente: ./run.ps1 -Target $Target
"@
    }

    Write-Step 'Subindo Postgres local via Docker...'
    Push-Location $rootDir
    try {
        if (Test-Path $envLocalFile) {
            docker compose --env-file $envLocalFile up -d postgres
        }
        else {
            docker compose up -d postgres
        }
        if ($LASTEXITCODE -ne 0) {
            throw 'Falha ao subir Postgres com docker compose.'
        }

        docker compose ps postgres
        if ($LASTEXITCODE -ne 0) {
            throw 'Falha ao verificar status do container postgres.'
        }
    }
    finally {
        Pop-Location
    }
}

function Run-BackendForeground {
    Ensure-LocalDatabase
    Write-Step 'Subindo backend em foreground...'
    Push-Location $backendDir
    try {
        & .\gradlew.bat run -x test
        exit $LASTEXITCODE
    }
    finally {
        Pop-Location
    }
}

function Run-FrontendForeground {
    Ensure-FrontendDependencies
    Write-Step 'Subindo frontend em foreground...'
    Push-Location $frontendDir
    try {
        node .\node_modules\expo\bin\cli start --web --port $FrontendPort
        exit $LASTEXITCODE
    }
    finally {
        Pop-Location
    }
}

function Start-BackendBackground {
    Ensure-LocalDatabase
    Write-Step 'Subindo backend em background...'
    $process = Start-Process -FilePath 'pwsh' -WorkingDirectory $backendDir -ArgumentList @(
        '-NoProfile',
        '-ExecutionPolicy', 'Bypass',
        '-Command',
        './gradlew.bat run -x test'
    ) -PassThru

    Wait-HttpOk -Url "http://localhost:$BackendPort/auth/ping" -Name 'backend'
    return $process
}

function Start-FrontendBackground {
    Ensure-FrontendDependencies
    Write-Step 'Subindo frontend em background...'
    $process = Start-Process -FilePath 'pwsh' -WorkingDirectory $frontendDir -ArgumentList @(
        '-NoProfile',
        '-ExecutionPolicy', 'Bypass',
        '-Command',
        "node .\\node_modules\\expo\\bin\\cli start --web --port $FrontendPort"
    ) -PassThru

    Wait-HttpOk -Url "http://localhost:$FrontendPort" -Name 'frontend'
    return $process
}

function Run-E2E {
    Push-Location $frontendDir
    try {
        Write-Step 'Executando E2E (Playwright)...'
        $env:E2E_DISABLE_WEBSERVER = '1'
        node .\node_modules\@playwright\test\cli.js test --config $playwrightConfig
        if ($LASTEXITCODE -ne 0) {
            throw "Playwright falhou com exit code $LASTEXITCODE"
        }
    }
    finally {
        Pop-Location
    }
}

function Run-Dev {
    $backendProcess = $null
    $frontendProcess = $null
    try {
        $backendProcess = Start-BackendBackground
        $frontendProcess = Start-FrontendBackground
        Write-Host "`nBackend e frontend no ar (sem E2E). Pressione Ctrl+C para encerrar." -ForegroundColor Green
        Wait-Process -Id $backendProcess.Id, $frontendProcess.Id
    }
    finally {
        if (-not $KeepServices) {
            Stop-ProcessSafe -Process $frontendProcess -Name 'frontend'
            Stop-ProcessSafe -Process $backendProcess -Name 'backend'
        }
    }
}

function Stop-ProcessSafe {
    param([System.Diagnostics.Process]$Process, [string]$Name)

    if (-not $Process) {
        return
    }

    try {
        if (-not $Process.HasExited) {
            Write-Step "Encerrando $Name..."
            Stop-Process -Id $Process.Id -Force -ErrorAction SilentlyContinue
        }
    }
    catch {
        Write-Warning "Nao foi possivel encerrar $Name (PID $($Process.Id))."
    }
}

Import-LocalEnv

switch ($Target) {
    'backend' {
        Run-BackendForeground
    }
    'frontend' {
        Run-FrontendForeground
    }
    'e2e' {
        if ($ReuseExisting) {
            Ensure-FrontendDependencies
            Run-E2E
            Write-Host 'E2E concluido usando servicos existentes.' -ForegroundColor Green
            exit 0
        }

        $backendProcess = $null
        $frontendProcess = $null
        try {
            $backendProcess = Start-BackendBackground
            $frontendProcess = Start-FrontendBackground
            Run-E2E
            Write-Host 'E2E concluido com sucesso.' -ForegroundColor Green

            if ($KeepServices) {
                Write-Host 'Servicos mantidos em execucao (--KeepServices).' -ForegroundColor Yellow
            }
            else {
                Stop-ProcessSafe -Process $frontendProcess -Name 'frontend'
                Stop-ProcessSafe -Process $backendProcess -Name 'backend'
            }

            exit 0
        }
        catch {
            Write-Error $_
            Stop-ProcessSafe -Process $frontendProcess -Name 'frontend'
            Stop-ProcessSafe -Process $backendProcess -Name 'backend'
            exit 1
        }
    }
    'all' {
        $backendProcess = $null
        $frontendProcess = $null
        try {
            $backendProcess = Start-BackendBackground
            $frontendProcess = Start-FrontendBackground
            Run-E2E
            Write-Host 'Fluxo all concluido (backend + frontend + e2e).' -ForegroundColor Green

            if ($KeepServices) {
                Write-Host 'Servicos mantidos em execucao (--KeepServices).' -ForegroundColor Yellow
            }
            else {
                Stop-ProcessSafe -Process $frontendProcess -Name 'frontend'
                Stop-ProcessSafe -Process $backendProcess -Name 'backend'
            }

            exit 0
        }
        catch {
            Write-Error $_
            Stop-ProcessSafe -Process $frontendProcess -Name 'frontend'
            Stop-ProcessSafe -Process $backendProcess -Name 'backend'
            exit 1
        }
    }
    'dev' {
        Run-Dev
    }
}
