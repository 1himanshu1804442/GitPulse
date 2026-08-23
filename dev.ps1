# ==============================================================================
# GitPulse - Windows PowerShell 1-Command Concurrent Dev Runner (dev.ps1)
# ==============================================================================
# Features:
#   1. Automatic environment & dependency health checking (Java, Node, Docker)
#   2. Automatic PostgreSQL database check / Docker auto-start / H2 fallback
#   3. Concurrent execution of Spring Boot (8080) and React Vite (3000)
#   4. Synchronized, color-coded unified log streaming
#   5. Graceful shutdown on Ctrl+C (cleans up child jobs/processes)
# ==============================================================================

[CmdletBinding()]
param (
    [Parameter(Mandatory = $false)]
    [ValidateSet("auto", "postgres", "h2")]
    [string]$DbMode = "auto",

    [Parameter(Mandatory = $false)]
    [switch]$SkipFrontend = $false,

    [Parameter(Mandatory = $false)]
    [switch]$SkipBackend = $false,

    [Parameter(Mandatory = $false)]
    [switch]$SkipDb = $false
)

# Set strict mode and error action preference
$ErrorActionPreference = "Continue"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

# Base directories
$ScriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Definition
$BackendDir = Join-Path $ScriptRoot "backend-spring"
$FrontendDir = Join-Path $ScriptRoot "frontend"

# Console Helper Functions
function Write-Header {
    param([string]$Message)
    Write-Host "`n================================================================================" -ForegroundColor Cyan
    Write-Host "  $Message" -ForegroundColor White -BackgroundColor DarkBlue
    Write-Host "================================================================================" -ForegroundColor Cyan
}

function Write-Log {
    param(
        [string]$Prefix,
        [ConsoleColor]$PrefixColor,
        [string]$Message,
        [ConsoleColor]$TextColor = [ConsoleColor]::White
    )
    $Timestamp = Get-Date -Format "HH:mm:ss"
    Write-Host "[$Timestamp] " -NoNewline -ForegroundColor DarkGray
    Write-Host "[$Prefix] " -NoNewline -ForegroundColor $PrefixColor
    Write-Host $Message -ForegroundColor $TextColor
}

function Write-SysLog { param([string]$Message) Write-Log "SYSTEM" Cyan $Message White }
function Write-DbLog  { param([string]$Message) Write-Log "DATABASE" Blue $Message Gray }
function Write-BeLog  { param([string]$Message) Write-Log "BACKEND" Green $Message Gray }
function Write-FeLog  { param([string]$Message) Write-Log "FRONTEND" Magenta $Message Gray }
function Write-WarnLog{ param([string]$Message) Write-Log "WARNING" Yellow $Message Yellow }
function Write-ErrLog { param([string]$Message) Write-Log "ERROR" Red $Message Red }

# ------------------------------------------------------------------------------
# Banner
# ------------------------------------------------------------------------------
Clear-Host
Write-Host @"
  ____ _ _   ____        _          
 / ___(_) |_|  _ \ _   _| |___  ___ 
| |  _| | __| |_) | | | | / __|/ _ \
| |_| | | |_|  __/| |_| | \__ \  __/
 \____|_|\__|_|    \__,_|_|___/\___|
                                    
 GitPulse Concurrent Development Runner
================================================================================
"@ -ForegroundColor Cyan

Write-SysLog "Initializing GitPulse local developer environment..."
Write-SysLog "Workspace root: $ScriptRoot"

# ------------------------------------------------------------------------------
# Step 1: Pre-Flight Check (Java & Node)
# ------------------------------------------------------------------------------
Write-Header "Step 1: Checking Development Prerequisites"

# Check Java
$javaAvailable = $false
try {
    $javaVer = java -version 2>&1 | Out-String
    if ($LASTEXITCODE -eq 0 -or $javaVer -match "version") {
        $javaAvailable = $true
        $firstLine = ($javaVer -split "`n")[0].Trim()
        Write-SysLog "Java detected: $firstLine"
    }
} catch {
    Write-WarnLog "Java runtime was not found in PATH."
}

if (-not $javaAvailable -and -not $SkipBackend) {
    Write-WarnLog "Java is required for Spring Boot backend. Please install JDK 17+ if backend fails."
}

# Check Node & npm
$nodeAvailable = $false
try {
    $nodeVer = node -v 2>&1
    $npmVer = npm -v 2>&1
    Write-SysLog "Node detected: $nodeVer (npm: $npmVer)"
    $nodeAvailable = $true
} catch {
    Write-WarnLog "Node.js was not found in PATH."
}

# ------------------------------------------------------------------------------
# Step 2: Database Detection & Auto-Start
# ------------------------------------------------------------------------------
Write-Header "Step 2: Database Provisioning & Profile Selection"

$SelectedSpringProfile = "postgres"

if ($SkipDb) {
    Write-SysLog "Skipping database setup as requested."
} else {
    function Test-PortOpen([string]$HostName, [int]$Port) {
        try {
            $tcpClient = New-Object System.Net.Sockets.TcpClient
            $asyncResult = $tcpClient.BeginConnect($HostName, $Port, $null, $null)
            $waitSuccess = $asyncResult.AsyncWaitHandle.WaitOne(1500, $false)
            if ($waitSuccess -and $tcpClient.Connected) {
                $tcpClient.EndConnect($asyncResult)
                $tcpClient.Close()
                return $true
            }
            $tcpClient.Close()
            return $false
        } catch {
            return $false
        }
    }

    $pgRunning = Test-PortOpen "localhost" 5432

    if ($DbMode -eq "h2") {
        Write-DbLog "H2 profile explicitly requested. Using in-memory H2 database."
        $SelectedSpringProfile = "h2"
    } elseif ($pgRunning) {
        Write-DbLog "PostgreSQL instance detected on localhost:5432. Connecting via 'postgres' profile."
        $SelectedSpringProfile = "postgres"
    } else {
        # Check if Docker is running
        $dockerAvailable = $false
        try {
            $dockerInfo = docker info 2>&1
            if ($LASTEXITCODE -eq 0) {
                $dockerAvailable = $true
            }
        } catch {
            $dockerAvailable = $false
        }

        if ($dockerAvailable -and ($DbMode -eq "auto" -or $DbMode -eq "postgres")) {
            Write-DbLog "Docker daemon is active. Starting PostgreSQL container via Docker Compose..."
            Set-Location $ScriptRoot
            docker compose up -d postgres

            Write-DbLog "Waiting for PostgreSQL container to become ready..."
            $retries = 10
            while ($retries -gt 0) {
                Start-Sleep -Seconds 2
                if (Test-PortOpen "localhost" 5432) {
                    Write-DbLog "PostgreSQL container is up and listening on port 5432!"
                    $SelectedSpringProfile = "postgres"
                    $pgRunning = $true
                    break
                }
                $retries--
                Write-DbLog "Waiting for port 5432... ($retries retries left)"
            }
        }

        if (-not $pgRunning) {
            Write-WarnLog "PostgreSQL is not reachable on port 5432 and Docker container could not be started."
            Write-WarnLog "Automatically falling back to Spring Boot in-memory H2 profile ('h2')."
            Write-WarnLog "No data will be lost during this session; H2 console will be available at http://localhost:8080/h2-console"
            $SelectedSpringProfile = "h2"
        }
    }
}

Write-SysLog "Active Spring Boot Profile: [$SelectedSpringProfile]"

# ------------------------------------------------------------------------------
# Step 3: Frontend Dependency Verification
# ------------------------------------------------------------------------------
if (-not $SkipFrontend -and (Test-Path $FrontendDir)) {
    $nodeModulesDir = Join-Path $FrontendDir "node_modules"
    if (-not (Test-Path $nodeModulesDir)) {
        Write-FeLog "node_modules not found in frontend directory. Running npm install..."
        Push-Location $FrontendDir
        try {
            npm install
        } finally {
            Pop-Location
        }
    }
}

# ------------------------------------------------------------------------------
# Step 4: Concurrently Launch Services with Unified Streaming Logs
# ------------------------------------------------------------------------------
Write-Header "Step 3: Launching Spring Boot & React Vite Concurrently"
Write-SysLog "Press [Ctrl+C] at any time to gracefully stop all services."
Write-SysLog "URLs:"
Write-SysLog "  -> Frontend UI:      http://localhost:3000"
Write-SysLog "  -> Backend API:     http://localhost:8080/api"
Write-SysLog "  -> Swagger UI:      http://localhost:8080/swagger-ui.html"
Write-SysLog "  -> Spring Actuator: http://localhost:8080/actuator/health"
Write-Host "--------------------------------------------------------------------------------`n" -ForegroundColor DarkGray

# Process tracking for cleanup
$processesToKill = [System.Collections.Generic.List[System.Diagnostics.Process]]::new()

# Global Lock Object for Synchronized Console Output
$consoleLock = [System.Object]::new()

function Start-LoggedProcess {
    param (
        [string]$Name,
        [ConsoleColor]$Color,
        [string]$WorkingDirectory,
        [string]$Executable,
        [string]$Arguments,
        [hashtable]$EnvVars = @{}
    )

    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = $Executable
    $psi.Arguments = $Arguments
    $psi.WorkingDirectory = $WorkingDirectory
    $psi.RedirectStandardOutput = $true
    $psi.RedirectStandardError = $true
    $psi.UseShellExecute = $false
    $psi.CreateNoWindow = $true

    foreach ($key in $EnvVars.Keys) {
        $psi.EnvironmentVariables[$key] = $EnvVars[$key]
    }

    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $psi
    $process.EnableRaisingEvents = $true

    # Event handler for Standard Output
    $outEvent = Register-ObjectEvent -InputObject $process -EventName "OutputDataReceived" -Action {
        if (-not [string]::IsNullOrEmpty($EventArgs.Data)) {
            $time = Get-Date -Format "HH:mm:ss"
            [System.Threading.Monitor]::Enter($consoleLock)
            try {
                Write-Host "[$time] " -NoNewline -ForegroundColor DarkGray
                Write-Host "[$($Event.MessageData.Name)] " -NoNewline -ForegroundColor $($Event.MessageData.Color)
                Write-Host $EventArgs.Data -ForegroundColor White
            } finally {
                [System.Threading.Monitor]::Exit($consoleLock)
            }
        }
    } -MessageData @{ Name = $Name; Color = $Color }

    # Event handler for Standard Error
    $errEvent = Register-ObjectEvent -InputObject $process -EventName "ErrorDataReceived" -Action {
        if (-not [string]::IsNullOrEmpty($EventArgs.Data)) {
            $time = Get-Date -Format "HH:mm:ss"
            [System.Threading.Monitor]::Enter($consoleLock)
            try {
                Write-Host "[$time] " -NoNewline -ForegroundColor DarkGray
                Write-Host "[$($Event.MessageData.Name)] " -NoNewline -ForegroundColor $($Event.MessageData.Color)
                Write-Host $EventArgs.Data -ForegroundColor Yellow
            } finally {
                [System.Threading.Monitor]::Exit($consoleLock)
            }
        }
    } -MessageData @{ Name = $Name; Color = $Color }

    [void]$process.Start()
    $process.BeginOutputReadLine()
    $process.BeginErrorReadLine()

    return @{
        Process = $process
        OutEvent = $outEvent
        ErrEvent = $errEvent
    }
}

$launchedServices = [System.Collections.Generic.List[hashtable]]::new()

try {
    # 1. Launch Backend
    if (-not $SkipBackend -and (Test-Path $BackendDir)) {
        Write-BeLog "Starting Java Spring Boot application..."
        $mvnCmd = "mvn"
        $mvnwPath = Join-Path $BackendDir "mvnw.cmd"
        if (Test-Path $mvnwPath) {
            $mvnCmd = $mvnwPath
        }

        $backendEnv = @{
            "SPRING_PROFILES_ACTIVE" = $SelectedSpringProfile
        }

        # Command to run spring boot
        $backendService = Start-LoggedProcess `
            -Name "BACKEND" `
            -Color Green `
            -WorkingDirectory $BackendDir `
            -Executable "cmd.exe" `
            -Arguments "/c `"$mvnCmd spring-boot:run`"" `
            -EnvVars $backendEnv

        $launchedServices.Add($backendService)
        $processesToKill.Add($backendService.Process)
    }

    # 2. Launch Frontend
    if (-not $SkipFrontend -and (Test-Path $FrontendDir)) {
        Write-FeLog "Starting React Vite dev server on port 3000..."
        $frontendService = Start-LoggedProcess `
            -Name "FRONTEND" `
            -Color Magenta `
            -WorkingDirectory $FrontendDir `
            -Executable "cmd.exe" `
            -Arguments "/c npm run dev -- --port 3000 --host"

        $launchedServices.Add($frontendService)
        $processesToKill.Add($frontendService.Process)
    }

    # Keep script alive and monitor process lifecycle
    while ($true) {
        Start-Sleep -Seconds 1

        # Check if any process exited unexpectedly
        foreach ($srv in $launchedServices) {
            if ($srv.Process.HasExited) {
                Write-WarnLog "A service process exited with code $($srv.Process.ExitCode)."
            }
        }
    }
} finally {
    Write-Host "`n"
    Write-SysLog "Shutting down all GitPulse processes and background tasks..."

    foreach ($srv in $launchedServices) {
        try {
            if (-not $srv.Process.HasExited) {
                # Stop process tree on Windows
                taskkill /F /T /PID $srv.Process.Id 2>$null | Out-Null
            }
            Unregister-Event -SourceIdentifier $srv.OutEvent.Name -ErrorAction SilentlyContinue
            Unregister-Event -SourceIdentifier $srv.ErrEvent.Name -ErrorAction SilentlyContinue
        } catch {
            # Ignore cleanup errors
        }
    }

    Write-SysLog "All services stopped successfully. Goodbye!"
}
