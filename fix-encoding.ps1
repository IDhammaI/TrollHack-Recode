$root = 'C:\Users\JIAQI-Work\Desktop\TrollHack-Recode\src\main'
$utf8 = New-Object System.Text.UTF8Encoding($false)
Get-ChildItem -Recurse -File -Path $root | Where-Object { $_.Extension -in '.kt','.java','.json' } | ForEach-Object {
  $b = [System.IO.File]::ReadAllBytes($_.FullName)
  if ($b.Length -lt 2) { return }
  $enc = $null
  if ($b[0] -eq 0xFF -and $b[1] -eq 0xFE) { $enc = [System.Text.Encoding]::Unicode }
  elseif ($b[0] -eq 0xFE -and $b[1] -eq 0xFF) { $enc = [System.Text.Encoding]::BigEndianUnicode }
  elseif ($b.Length -ge 3 -and $b[0] -eq 0xEF -and $b[1] -eq 0xBB -and $b[2] -eq 0xBF) { $enc = New-Object System.Text.UTF8Encoding($true) }
  else {
    $nulls = 0
    $limit = [Math]::Min($b.Length, 4000)
    for ($i = 0; $i -lt $limit; $i++) { if ($b[$i] -eq 0) { $nulls++ } }
    if ($nulls -gt ($limit / 4)) {
      if ($b[0] -eq 0) { $enc = [System.Text.Encoding]::BigEndianUnicode } else { $enc = [System.Text.Encoding]::Unicode }
    }
  }
  if ($enc) {
    $t = $enc.GetString($b).TrimStart([char]0xFEFF)
    [System.IO.File]::WriteAllText($_.FullName, $t, $utf8)
    Write-Output ('FIXED ' + $_.FullName)
  }
}
Write-Output 'SCAN-DONE'
