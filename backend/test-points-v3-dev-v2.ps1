param([string]$Api='http://127.0.0.1:5084')
$ErrorActionPreference='Stop'
if(([uri]$Api).Host -notin @('localhost','127.0.0.1') -or ([uri]$Api).Port -ne 5084){throw 'Isolated API only'}
$checks=[Collections.Generic.List[string]]::new();$fixtures=[Collections.Generic.List[long]]::new();$changed=$null;$before=$null;$uid=0
function Check($value,$message){if(!$value){throw "FAIL: $message"};$checks.Add("PASS: $message")}
function Call($method,$route,$body=$null,$headers=$script:user){$args=@{Uri="$Api/$route";Method=$method;Headers=$headers;ContentType='application/json';TimeoutSec=30};if($null -ne $body){$args.Body=$body|ConvertTo-Json -Depth 8};Invoke-RestMethod @args}
function Reject($method,$route,$body,$headers=$script:admin){$r=Invoke-WebRequest "$Api/$route" -Method $method -Headers $headers -ContentType application/json -Body ($body|ConvertTo-Json -Depth 8) -SkipHttpErrorCheck;return $r.StatusCode}
function Sql($text){$p=Join-Path $PSScriptRoot '../output/dev-v2/verification/points-v3-fixture.sql';[IO.File]::WriteAllText($p,$text);& (Join-Path $PSScriptRoot 'invoke-dev-v2-sql.ps1') -SqlFile $p|Out-Null}
function Quest($cycle,$points,$item=$null){$body=@{name="QA points $cycle $([guid]::NewGuid().ToString('N'))";description='Isolated integration fixture';quest_type=$cycle;cycle_type=$cycle;condition_type='COMPLETE_JOURNEY';target_value=1;reward_points=$points;status='ACTIVE';reward_item_id=$item;reward_quantity=$(if($item){1}else{$null})};$id=(Call POST 'api/admin/data/quests' $body $script:admin).id;$fixtures.Add([long]$id);return Call GET "api/admin/data/quests/$id" $null $script:admin}
try{
 Check ((Call GET 'health' $null @{}).database -ceq 'vietnam_explorer_dev_v2') 'Target database verified before writes'
 $creds=Get-Content (Join-Path $PSScriptRoot '../output/dev-v2/admin-credentials.json')|ConvertFrom-Json
 $auth=Call POST 'api/admin/login' @{email=$creds.email;password=$creds.password} @{};$script:admin=@{Authorization="Bearer $($auth.token)"};$creds=$null
 $before=Call GET 'api/admin/exploration/reward-settings' $null $admin
 Check ($before.discoveryPoints -ge 0 -and !$before.PSObject.Properties['questPoints']) 'Configuration exposes one discovery value and no shared quest value'
 $changed=Call PUT 'api/admin/exploration/reward-settings' @{discoveryPoints=173;revision=$before.revision} $admin
 Check ($changed.discoveryPoints -eq 173) 'Admin can configure unified discovery points'
 Check ((Reject PUT 'api/admin/exploration/reward-settings' @{discoveryPoints=-1;revision=$changed.revision}) -eq 400) 'Negative discovery value rejected'
 Check ((Reject PUT 'api/admin/exploration/reward-settings' @{discoveryPoints=200001;revision=$changed.revision}) -eq 400) 'Excessive discovery value rejected'
 Check ((Reject PUT 'api/admin/exploration/reward-settings' @{discoveryPoints=1.5;revision=$changed.revision}) -eq 400) 'Fractional discovery value rejected'
 Check ((Reject PUT 'api/admin/exploration/reward-settings' @{questPoints=20;journeyPoints=50;firstPlacePoints=100;revision=$changed.revision}) -eq 400) 'Legacy configuration payload cannot silently overwrite unified points'
 Check ((Reject PUT 'api/admin/exploration/reward-settings' @{discoveryPoints=1;revision=$before.revision}) -eq 400) 'Stale configuration revision rejected'
 $items=Call GET 'api/admin/lookups' $null $admin;$itemId=@($items.items)[0].id
 $daily=Quest DAILY 17 $itemId;$weekly=Quest WEEKLY 83;$zero=Quest DAILY 0;$itemOnly=Quest DAILY 0 $itemId
 Check ($daily.reward_points -eq 17 -and $weekly.reward_points -eq 83) 'Daily and weekly quests store independent reward values'
 Check ($zero.reward_points -eq 0) 'Zero-point quest supported'
 Check ($itemOnly.reward_points -eq 0 -and $itemOnly.reward_item_id -eq $itemId) 'Item-only quest can be configured independently from points'
 foreach($bad in @(-1,100001,1.5,'abc')){$copy=@{};foreach($p in $daily.PSObject.Properties){$copy[$p.Name]=$p.Value};$copy.reward_points=$bad;Check ((Reject POST 'api/admin/data/quests' $copy) -eq 400) "Invalid quest points rejected: $bad"}
 $filtered=Call GET 'api/admin/data/quests?cycle=DAILY&pageSize=100' $null $admin
 Check (@($filtered.items|Where-Object cycle_type -ne DAILY).Count -eq 0) 'Admin list filters daily quests and includes reward points'
 $password='Points-'+[guid]::NewGuid().ToString('N')+'!';$account=Call POST 'api/account/register' @{email=('points-'+[guid]::NewGuid().ToString('N')+'@example.invalid');password=$password;confirmPassword=$password} @{}
 $script:user=@{Authorization="Bearer $($account.token)"};$uid=[long]$account.user.userId
 Check ($uid -gt 0) 'Isolated QA account created'
 Check ((Reject PUT 'api/admin/exploration/reward-settings' @{discoveryPoints=1;revision=$changed.revision} $user) -eq 403) 'User cannot change reward settings'
 $null=Call PUT 'api/interests' @{placeTypeIds=@(101)};$null=Call PUT 'api/activity-level' @{activityLevel='HIGH'};$null=Call PUT 'api/starting-point' @{source='SUGGESTED'}
 function NewQaJourney(){ $active=(Call GET 'api/explore').journey; $places=Call GET 'api/explore/places?pageSize=50';$p=@($places.places|Where-Object {!$_.isDiscovered -and $_.distanceKm -gt 0.001 -and $_.placeId -ne $active.destinationPlaceId})[0];return (Call POST 'api/explore/journeys' @{placeId=$p.placeId;replaceActive=$true}).journey }
 $abandoned=NewQaJourney;$journey=NewQaJourney
 Check ((Call GET 'api/leaderboard').me.points -eq 0) 'Creating and abandoning journeys does not award points'
 $null=Call POST 'api/explore/sync-steps' @{journeyId=$abandoned.journeyId;cumulativeSteps=$abandoned.requiredSteps}
 Check ((Call GET 'api/leaderboard').me.points -eq 0) 'Abandoned journey cannot award discovery or quest points'
 $null=Call POST 'api/explore/sync-steps' @{journeyId=$journey.journeyId;cumulativeSteps=1}
 Check ((Call GET 'api/leaderboard').me.points -eq 0) 'Incomplete journey and unfinished quests do not award points'
 $client=[Net.Http.HttpClient]::new();try{$client.DefaultRequestHeaders.Authorization=[Net.Http.Headers.AuthenticationHeaderValue]::new('Bearer',$account.token);$body=@{journeyId=$journey.journeyId;cumulativeSteps=$journey.requiredSteps}|ConvertTo-Json;$a=$client.PostAsync("$Api/api/explore/sync-steps",[Net.Http.StringContent]::new($body,[Text.Encoding]::UTF8,'application/json'));$b=$client.PostAsync("$Api/api/explore/sync-steps",[Net.Http.StringContent]::new($body,[Text.Encoding]::UTF8,'application/json'));[Threading.Tasks.Task]::WaitAll([Threading.Tasks.Task[]]@($a,$b));Check ($a.Result.IsSuccessStatusCode -and $b.Result.IsSuccessStatusCode) 'Concurrent duplicate completions resolve safely';$complete=$a.Result.Content.ReadAsStringAsync().GetAwaiter().GetResult()|ConvertFrom-Json}finally{$client.Dispose()}
 Check ($complete.journey.rankingPoints -eq 173) 'Completion returns one actual discovery reward'
 $quests=Call GET 'api/quests';$all=@($quests.daily)+@($quests.weekly)
 Check (@($all|Where-Object {$_.questId -eq $daily.quest_id})[0].awardedRankingPoints -eq 17) 'Daily quest awards its configured 17 points'
 Check (@($all|Where-Object {$_.questId -eq $weekly.quest_id})[0].awardedRankingPoints -eq 83) 'Weekly quest awards its configured 83 points'
 Check (@($all|Where-Object {$_.questId -eq $zero.quest_id})[0].awardedRankingPoints -eq 0) 'Completed zero-point quest settles at zero'
 $only=@($all|Where-Object {$_.questId -eq $itemOnly.quest_id})[0]
 Check ($only.awardedRankingPoints -eq 0 -and $only.rewardReceived) 'Item-only completion receives its item without awarding ranking points'
 $sum=($all|Measure-Object awardedRankingPoints -Sum).Sum;$score=(Call GET 'api/leaderboard').me.points
 Check ($score -eq 173+$sum) 'Leaderboard equals independent discovery plus every actual quest reward'
 $events=Call GET 'api/admin/exploration/events?eventType=DISCOVERY&pageSize=50' $null $admin
 Check (@($events.items|Where-Object userId -eq $uid).Count -eq 1) 'Exactly one unified discovery ledger event exists'
 $null=Call POST 'api/explore/sync-steps' @{journeyId=$journey.journeyId;cumulativeSteps=$journey.requiredSteps}
 Check ((Call GET 'api/leaderboard').me.points -eq $score) 'Repeated completion and reads cannot award points twice'
 $inventory=Call GET 'api/inventory';$null=Call GET 'api/quests';$again=Call GET 'api/inventory'
 Check ($inventory.totalQuantity -gt 0 -and $inventory.totalQuantity -eq $again.totalQuantity) 'Item reward remains functional and pays once'
 $daily.reward_points=41;$null=Call PUT "api/admin/data/quests/$($daily.quest_id)" $daily $admin
 $zero.reward_points=59;$null=Call PUT "api/admin/data/quests/$($zero.quest_id)" $zero $admin
 $past=Call GET 'api/quests';$pd=@($past.daily|Where-Object questId -eq $daily.quest_id)[0];$pz=@($past.daily|Where-Object questId -eq $zero.quest_id)[0]
 Check ($pd.rankingPoints -eq 41 -and $pd.awardedRankingPoints -eq 17) 'Editing points after progress preserves historical 17-point snapshot'
 Check ($pz.rankingPoints -eq 59 -and $pz.awardedRankingPoints -eq 0) 'Editing a completed zero-point quest cannot retrospectively award it'
 Check ((Call GET 'api/leaderboard').me.points -eq $score) 'Reward edits leave ledger totals unchanged'
 # Move only this QA account's fixture cycles to the past; production clock is unchanged.
 Sql "UPDATE quest_progress SET cycle_key='qa-previous-cycle' WHERE user_id=$uid AND quest_id IN ($($daily.quest_id),$($weekly.quest_id),$($zero.quest_id));"
 $new=NewQaJourney;$done=Call POST 'api/explore/sync-steps' @{journeyId=$new.journeyId;cumulativeSteps=$new.requiredSteps}
 $next=Call GET 'api/quests';$nd=@($next.daily|Where-Object questId -eq $daily.quest_id)[0];$nw=@($next.weekly|Where-Object questId -eq $weekly.quest_id)[0]
 Check ($nd.awardedRankingPoints -eq 41 -and $nw.awardedRankingPoints -eq 83) 'A new cycle awards new snapshots independently for daily and weekly quests'
 $after=(Call GET 'api/leaderboard').me.points
 # Inject a DB failure scoped to this QA user, then confirm transaction rollback.
 $fail=NewQaJourney
 try{Sql "ALTER TABLE explorer_score_event ADD CONSTRAINT qa_points_failure CHECK(user_id<>$uid) NOT VALID;"
  $status=Reject POST 'api/explore/sync-steps' @{journeyId=$fail.journeyId;cumulativeSteps=$fail.requiredSteps} $user
  Check ($status -eq 500) 'Injected ledger failure rejects completion'
  $rolled=(Call GET 'api/explore').journey
  Check ($rolled.status -eq 'IN_PROGRESS' -and $rolled.recordedSteps -eq 0) 'Failed score transaction rolls back journey progress'
  Check ((Call GET 'api/leaderboard').me.points -eq $after) 'Failed score transaction leaves total unchanged'
  Check (!(Call GET "api/explore/places/$($fail.destinationPlaceId)").isDiscovered) 'Failed score transaction rolls back discovery and card unlock'
 }finally{Sql 'ALTER TABLE explorer_score_event DROP CONSTRAINT IF EXISTS qa_points_failure;'}
 $retry=Call POST 'api/explore/sync-steps' @{journeyId=$fail.journeyId;cumulativeSteps=$fail.requiredSteps}
 Check ($retry.journey.rankingPoints -eq 173) 'Retry after rollback awards discovery exactly once'
 $changed=Call PUT 'api/admin/exploration/reward-settings' @{discoveryPoints=0;revision=$changed.revision} $admin
 $noPoints=NewQaJourney;$noReward=Call POST 'api/explore/sync-steps' @{journeyId=$noPoints.journeyId;cumulativeSteps=$noPoints.requiredSteps}
 Check ($noReward.journey.rankingPoints -eq 0) 'Zero discovery configuration disables only discovery points'
 $tickets=Call GET 'api/items/flight-ticket';$flightPlaces=Call GET "api/items/flight-ticket/regions/$($tickets.regions[0].regionId)/places"
 $dest=@($flightPlaces.places|Where-Object {!$_.discovered})[0]
 $preFlight=(Call GET 'api/leaderboard').me.points;$null=Call POST 'api/items/flight-ticket/use' @{placeId=$dest.placeId}
 Check ((Call GET 'api/leaderboard').me.points -eq $preFlight) 'Flight unlock grants no discovery or quest ranking points'
 $changed=Call PUT 'api/admin/exploration/reward-settings' @{discoveryPoints=173;revision=$changed.revision} $admin
 Check ((Call GET "api/explore/places/$($journey.destinationPlaceId)").isDiscovered) 'Viewing discovered content preserves unlock without creating awards'
}finally{
 if($before -and $changed){$current=Call GET 'api/admin/exploration/reward-settings' $null $admin;if($current.revision -eq $changed.revision){$null=Call PUT 'api/admin/exploration/reward-settings' @{discoveryPoints=$before.discoveryPoints;revision=$current.revision} $admin}else{throw 'Concurrent admin edit detected; not restoring over it'}}
 foreach($id in $fixtures){$q=Call GET "api/admin/data/quests/$id" $null $admin;$q.status='INACTIVE';$null=Call PUT "api/admin/data/quests/$id" $q $admin}
 $checks -join "`n"
}
