param([switch]$FullCatalogue)
$ErrorActionPreference='Stop'
$catalogue=Get-Content "$PSScriptRoot/../app/src/main/java/com/jawadmohammed/quranodoro/data/Catalogue.kt" -Raw
$reciters=[regex]::Matches($catalogue,'Reciter\("([^"]+)","[^"]+","[^"]+","([^"]+)"\)')
$surahs=if($FullCatalogue){1..114}else{@(1,18,114)}
$results=foreach($r in $reciters){
    foreach($surah in $surahs){
        $url=$r.Groups[2].Value+'/'+$surah.ToString('000')+'.mp3'
        try {
            $response=Invoke-WebRequest -Uri $url -Method Get -Headers @{Range='bytes=0-255'} -TimeoutSec 25
            $bytes=$response.RawContentLength
            [pscustomobject]@{Reciter=$r.Groups[1].Value;Surah=$surah;Status=[int]$response.StatusCode;ContentType=$response.Headers['Content-Type'];Bytes=$bytes;Passed=([int]$response.StatusCode -in 200,206 -and $bytes -gt 0)}
        } catch { [pscustomobject]@{Reciter=$r.Groups[1].Value;Surah=$surah;Status=0;ContentType='';Bytes=0;Passed=$false;Error=$_.Exception.Message} }
    }
}
$results | ConvertTo-Json | Set-Content "$PSScriptRoot/../audio-verification.json"
$results | Format-Table Reciter,Surah,Status,ContentType,Bytes,Passed
if($results.Passed -contains $false){exit 1}
