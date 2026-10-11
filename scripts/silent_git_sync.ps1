param (
    [string]$Message = ""
)

$ErrorActionPreference = "SilentlyContinue"

# Check if there are changes
$status = git status --porcelain
if (-not $status) {
    Write-Output "CLEAN: Working tree is clean. Nothing to sync."
    exit 0
}

# Auto-generate message if empty
if ([string]::IsNullOrWhiteSpace($Message)) {
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm"
    $Message = "chore: auto-sync update [$timestamp]"
}

# Add all changes
git add . 2>$null

# Commit
git commit -m "$Message" 2>$null
if ($LASTEXITCODE -ne 0) {
    Write-Output "NOTICE: Commit skipped or nothing to commit."
    exit 0
}

$commitHash = (git rev-parse --short HEAD).Trim()

# Push silently
git push origin main -q 2>$null
if ($LASTEXITCODE -eq 0) {
    Write-Output "SUCCESS: Pushed commit $commitHash to GitHub main silently."
} else {
    Write-Output "ERROR: Failed to push to remote. Check network connection."
}
