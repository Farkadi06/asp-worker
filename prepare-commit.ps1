# PowerShell script to prepare commit for asp-worker architectural refactor
# Run this script from the asp-worker directory

Write-Host "Preparing commit for asp-worker: Architectural Refactor..." -ForegroundColor Green
Write-Host ""

# Check if git is initialized
if (-not (Test-Path .git)) {
    Write-Host "Git repository already initialized in asp-worker." -ForegroundColor Yellow
}

# Stage all changes
Write-Host "Staging changes..." -ForegroundColor Yellow

# Stage modified files
git add src/main/java/com/asp/worker/listener/IngestionQueueListener.java
git add src/main/java/com/asp/worker/service/SqsPollingService.java
git add src/main/java/com/asp/worker/client/HttpIngestionClient.java
git add src/main/resources/application.yml
git add build.gradle.kts
git add docs/01-project-setup.md

# Stage deleted files
git add src/main/java/com/asp/worker/config/S3ClientConfig.java
git add src/main/java/com/asp/worker/service/S3DownloadService.java

Write-Host "Files staged successfully." -ForegroundColor Green
Write-Host ""

# Show status
Write-Host "Staged files:" -ForegroundColor Cyan
git status --short

Write-Host ""
Write-Host "Ready to commit!" -ForegroundColor Green
Write-Host ""
Write-Host "To commit, run:" -ForegroundColor Yellow
Write-Host '  git commit -F COMMIT_MESSAGE.txt' -ForegroundColor White
Write-Host ""

