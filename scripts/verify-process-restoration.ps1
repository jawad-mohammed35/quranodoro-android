$ErrorActionPreference='Stop'
$applicationId='com.jawadmohammed.quranodoro'
$scratch=Join-Path $PSScriptRoot '../.tools'
New-Item -ItemType Directory -Force $scratch | Out-Null
function Read-Window {
    & adb shell uiautomator dump --compressed /sdcard/quranodoro-qa.xml | Out-Null
    & adb pull /sdcard/quranodoro-qa.xml "$scratch/window.xml" 2>&1 | Out-Null
    [xml](Get-Content "$scratch/window.xml" -Raw)
}
function Tap-Node([string]$xpath) {
    $document=Read-Window
    $node=$document.SelectSingleNode($xpath)
    if($null -eq $node){throw "UI node missing: $xpath"}
    $coordinates=[regex]::Matches($node.bounds,'\d+') | ForEach-Object {[int]$_.Value}
    & adb shell input tap ([int](($coordinates[0]+$coordinates[2])/2)) ([int](($coordinates[1]+$coordinates[3])/2))
}
function Read-Timer {
    $document=Read-Window
    $node=$document.SelectNodes('//node[@text]') | Where-Object {$_.text -match '^\d{2}:\d{2}$'} | Select-Object -First 1
    if($null -eq $node){throw 'Timer text missing'}
    $parts=$node.text.Split(':'); return [int]$parts[0]*60+[int]$parts[1]
}
& adb shell am start -n "$applicationId/.MainActivity" | Out-Null
Start-Sleep -Seconds 1
Tap-Node "(//node[@text='Focus'])[last()]"
Tap-Node "//node[@text='Start session']"
$before=Read-Timer
$start=Get-Date
& adb shell input keyevent 3
Start-Sleep -Seconds 3
& adb shell am force-stop $applicationId
Start-Sleep -Seconds 1
& adb shell am start -n "$applicationId/.MainActivity" | Out-Null
Start-Sleep -Seconds 1
Tap-Node "(//node[@text='Focus'])[last()]"
$after=Read-Timer
$elapsed=((Get-Date)-$start).TotalSeconds
$difference=$before-$after
if($difference -lt 3 -or [Math]::Abs($difference-$elapsed) -gt 5){throw "Timer restoration mismatch: difference=$difference elapsed=$elapsed"}
Tap-Node "//node[@text='Pause']"
Tap-Node "//node[@text='Reset']"
Tap-Node "//node[@text='Confirm']"
[pscustomobject]@{BeforeSeconds=$before;AfterSeconds=$after;TimerElapsed=$difference;WallElapsed=[Math]::Round($elapsed,2);Passed=$true} | ConvertTo-Json | Set-Content "$scratch/process-restoration.json"
Get-Content "$scratch/process-restoration.json"
