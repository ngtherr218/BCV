param([string]$Api='http://127.0.0.1:5084')
# The former global-quest test is superseded by per-quest v3 integration coverage.
& (Join-Path $PSScriptRoot 'test-points-v3-dev-v2.ps1') -Api $Api
