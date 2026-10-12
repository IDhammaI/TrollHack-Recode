param([string]$Root = ".")
$fixed = 0
$enc = New-Object System.Text.UTF8Encoding($false)
Get-ChildItem -Recurse -Include *.kt,*.java,*.json "$Root\src" | ForEach-Object {
  $bytes = [System.IO.File]::ReadAllBytes($_.FullName)
  if ($bytes.Length -lt 3) { return }
  if ($bytes[0] -eq 0xFF -and $bytes[1] -eq 0xFE) {
    $text = [System.Text.Encoding]::Unicode.GetString($bytes, 2, $bytes.Length - 2)
    [System.IO.File]::WriteAllText($_.FullName, $text, $enc); $script:fixed++
  } elseif ($bytes[1] -eq 0 -and $bytes[3] -eq 0) {
    $text = [System.Text.Encoding]::Unicode.GetString($bytes)
    [System.IO.File]::WriteAllText($_.FullName, $text, $enc); $script:fixed++
  } elseif ($bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF) {
    $text = [System.Text.Encoding]::UTF8.GetString($bytes, 3, $bytes.Length - 3)
    [System.IO.File]::WriteAllText($_.FullName, $text, $enc); $script:fixed++
  }
}
Write-Output "Encoding fixed: $fixed files"
