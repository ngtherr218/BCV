param([string]$FromEmail = 'quanggm7a1@gmail.com', [string]$FromName = 'Buoc Chan Viet')
$ErrorActionPreference = 'Stop'
$address = [System.Net.Mail.MailAddress]::new($FromEmail)
if ($address.Address -ne $FromEmail) { throw 'Enter the sender email address without a display name.' }
$secure = Read-Host 'New Brevo API key (hidden input; use an API key, not SMTP key)' -AsSecureString
$plain = [System.Net.NetworkCredential]::new('', $secure).Password.Trim()
if (-not $plain.StartsWith('xkeysib-')) { throw 'Expected a Brevo API key starting with xkeysib-.' }
try {
    [Environment]::SetEnvironmentVariable('Brevo__ApiKey', $plain, 'User')
    [Environment]::SetEnvironmentVariable('Brevo__FromEmail', $FromEmail, 'User')
    [Environment]::SetEnvironmentVariable('Brevo__FromName', $FromName, 'User')
    [Environment]::SetEnvironmentVariable('Email__RequireDelivery', 'true', 'User')
    Write-Host "Brevo saved for this Windows user. Sender: $FromEmail. Restart backend/run-local.ps1."
    Write-Host 'No key was written to the repository or printed.'
} finally { $plain = $null; $secure = $null }
