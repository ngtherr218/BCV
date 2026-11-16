param([string]$Api='http://127.0.0.1:5082')
$ErrorActionPreference='Stop'
$uri=[Uri]$Api
if($uri.Host -notin @('localhost','127.0.0.1') -or $uri.Port -notin @(5082,5084)){throw 'Local v2 API only.'}
$root=Split-Path $PSScriptRoot -Parent
$credentials=Get-Content (Join-Path $root 'output/dev-v2/admin-credentials.json')|ConvertFrom-Json
$headers=@{}
$checks=[Collections.Generic.List[string]]::new()
$fixtures=[Collections.Generic.List[object]]::new()
function Call($method,$path,$body=$null){
    $arguments=@{Uri="$Api/api/admin/$path";Method=$method;Headers=$headers;ContentType='application/json';TimeoutSec=30}
    if($null -ne $body){$arguments.Body=ConvertTo-Json $body -Depth 10 -Compress}
    Invoke-RestMethod @arguments
}
function Check($condition,$message){if(!$condition){throw "FAIL: $message"};$checks.Add("PASS: $message")}
try{
    if((Invoke-RestMethod "$Api/health").database -cne 'vietnam_explorer_dev_v2'){throw 'Wrong database; no login or fixture writes performed.'}
    $auth=Call POST login @{email=$credentials.email;password=$credentials.password}
    $headers=@{Authorization="Bearer $($auth.token)"}
    $overview=Call GET exploration/overview
    Check ($overview.database -eq 'vietnam_explorer_dev_v2') 'Verify target database identity before fixture writes'
    foreach($resource in @('regions','places','place-types','cards','collections','quests','items')){
        $null=Call GET "data/${resource}?page=1&pageSize=5"
        $checks.Add("PASS: Admin reads $resource catalog")
    }
    $null=Call GET lookups
    $checks.Add('PASS: Admin reads shared lookups')
    $name='QA-local-v2-'+[Guid]::NewGuid().ToString('N')
    $bodies=@{
        regions=@{name=$name;description='Isolated draft QA fixture';status='DRAFT'}
        collections=@{name=$name;description='Isolated draft QA fixture';status='DRAFT';card_ids=@()}
        quests=@{reward_points=20;name=$name;quest_type='WEEKLY';condition_type='STEP_COUNT';target_value=12345;cycle_type='WEEKLY';status='DRAFT'}
    }
    foreach($resource in @('regions','collections','quests')){
        $body=$bodies[$resource]
        $created=Call POST "data/$resource" $body
        $fixtures.Add(@{resource=$resource;id=$created.id})
        $detail=Call GET "data/$resource/$($created.id)"
        Check ($detail.name -eq $name -and $detail.status -eq 'DRAFT') "Create and read isolated $resource draft"
        $body.name="$name-edited"
        $null=Call PUT "data/$resource/$($created.id)" $body
        $detail=Call GET "data/$resource/$($created.id)"
        Check ($detail.name -eq "$name-edited") "Update $resource draft persists"
        $eligibility=Call GET "data/$resource/$($created.id)/delete-check"
        Check $eligibility.canDelete "Unlinked $resource fixture is eligible for deletion"
    }
    $response=Invoke-WebRequest "$Api/api/admin/exploration/overview" -Headers $headers
    Check ($response.Headers['Cache-Control'] -contains 'no-store') 'Admin response prevents caching'
} finally {
    for($index=$fixtures.Count-1;$index -ge 0;$index--){
        $fixture=$fixtures[$index]
        $null=Call DELETE "data/$($fixture.resource)/$($fixture.id)"
        $checks.Add("PASS: Removed only owned $($fixture.resource) fixture $($fixture.id)")
    }
    $credentials=$null
    $headers=@{}
    $checks | Set-Content (Join-Path $root 'output/dev-v2/verification/admin-crud-latest.txt')
    $checks
}
