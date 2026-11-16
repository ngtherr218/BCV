param([string]$Api = 'http://127.0.0.1:5082', [switch]$ExerciseAwards)
$ErrorActionPreference='Stop'
$uri=[Uri]$Api
if ($uri.Host -notin @('127.0.0.1','localhost') -or $uri.Port -notin @(5082,5084)) { throw 'Tests only allow local v2 APIs on port 5082 or isolated test port 5084.' }
$checks=[Collections.Generic.List[string]]::new()
function Check([bool]$condition,[string]$message) {
    if(!$condition){throw "FAIL: $message"}
    $checks.Add("PASS: $message")
}
$script:headers=@{}
function Call([string]$method,[string]$path,$body=$null) {
    $arguments=@{Uri="$Api/$path";Method=$method;Headers=$script:headers;ContentType='application/json';TimeoutSec=30}
    if($null -ne $body){$arguments.Body=$body|ConvertTo-Json -Depth 8 -Compress}
    Invoke-RestMethod @arguments
}
try {
    $health=Call GET 'health'
    Check ($health.status -eq 'ok' -and $health.database -ceq 'vietnam_explorer_dev_v2') 'Backend local health and target database verified before fixture writes'
    $status=(Invoke-WebRequest "$Api/api/explore/places" -SkipHttpErrorCheck).StatusCode
    Check ($status -eq 401) 'Search requires authentication'
    $email='v2-check-'+[Guid]::NewGuid().ToString('N')+'@example.invalid'
    $password='V2test-'+[Guid]::NewGuid().ToString('N')+'!'
    $account=Call POST 'api/account/register' @{email=$email;password=$password;confirmPassword=$password}
    Check (![string]::IsNullOrEmpty($account.token)) 'Register isolated test account through real API'
    $script:headers=@{Authorization="Bearer $($account.token)"}
    $catalog=Call GET 'api/collections'
    $album=Call GET 'api/collections/all-cards'
    Check ($album.totalCards -eq $catalog.totalCards -and $album.cards.Count -eq $album.totalCards) 'All-card album includes every active card independently of themed collection publication'
    Check (@($album.cards|Where-Object {!$_.owned -and ![string]::IsNullOrEmpty($_.imageUrl)}).Count -gt 0) 'Unowned active cards expose artwork for preview'
    $afterPreview=Call GET 'api/collections'
    Check ($afterPreview.ownedCards -eq $catalog.ownedCards) 'Album preview cannot award card ownership'
    $null=Call PUT 'api/interests' @{placeTypeIds=@(201)}
    $null=Call PUT 'api/activity-level' @{activityLevel='HIGH'}
    $null=Call PUT 'api/starting-point' @{source='SUGGESTED'}
    $options=Call GET 'api/explore/filters'
    Check ($options.placeTypes.Count -gt 0 -and $options.interests.Count -eq 12) 'Filter options use twelve standard place types'
    $interests=Call GET 'api/interests'
    Check ($interests.placeTypes.Count -eq 12 -and (@($interests.placeTypes.id|Sort-Object -Unique).Count -eq 12)) 'Profile offers twelve distinct standard interests'
    Check ($interests.requiresReselection -and $interests.selectedPlaceTypeIds.Count -eq 0) 'Legacy six-group selection remains stored while UI requests reselection'
    $null=Call PUT 'api/interests' @{placeTypeIds=@(101,108,112)}
    $savedInterests=Call GET 'api/interests'
    Check (($savedInterests.selectedPlaceTypeIds -join ',') -eq '101,108,112' -and !$savedInterests.requiresReselection) 'Twelve-type selections persist and reload'
    $newRecommendations=Call GET 'api/explore/recommendations'
    Check ($newRecommendations.places.Count -le 3) 'New standard interests integrate with recommendation model'
    $interestFilter=Call GET 'api/explore/places?interestGroupId=108&pageSize=50'
    Check ($interestFilter.total -gt 0 -and @($interestFilter.places|Where-Object {!$_.matchesInterests}).Count -eq 0) 'New coast/island interest filter agrees with user preference matching'
    $multiple=Call GET 'api/explore/places?interestGroupIds=108,111&sort=interests&pageSize=50'
    Check ($multiple.total -ge $interestFilter.total -and @($multiple.places.placeId|Sort-Object -Unique).Count -eq $multiple.places.Count) 'Multiple checked destination categories combine without duplicate places'
    $seenOther=$false;$ordered=$true
    foreach($place in $multiple.places){if(!$place.matchesInterests){$seenOther=$true}elseif($seenOther){$ordered=$false}}
    Check $ordered 'Preference sort places matching interests before other filtered destinations'
    $invalidTypes=Invoke-WebRequest "$Api/api/explore/places?interestGroupIds=108,999" -Headers $headers -SkipHttpErrorCheck
    Check ($invalidTypes.StatusCode -eq 400) 'Unknown checked category is rejected'
    $tooMany=Invoke-WebRequest "$Api/api/interests" -Method Put -Headers $headers -ContentType application/json -Body '{"placeTypeIds":[101,102,103,104]}' -SkipHttpErrorCheck
    Check ($tooMany.StatusCode -eq 400 -and ((Call GET 'api/interests').selectedPlaceTypeIds -join ',') -eq '101,108,112') 'Four interests rejected without overwriting saved choices'
    $null=Call PUT 'api/interests' @{placeTypeIds=@(201)}
    $page=Call GET 'api/explore/places?pageSize=10'
    Check ($page.total -gt 10 -and $page.places.Count -eq 10) 'Search pagination and total'
    $page2=Call GET 'api/explore/places?pageSize=10&page=2'
    $ids=@($page.places.placeId)+@($page2.places.placeId)
    Check (($ids|Sort-Object -Unique).Count -eq 20) 'Search pages contain no duplicate destinations'
    $outside=Call GET 'api/explore/places?interestGroupId=204&pageSize=50'
    Check ($outside.total -gt 0) 'User can search destinations outside selected heritage interests'
    $combined=Call GET 'api/explore/places?minDistanceKm=0&maxDistanceKm=100&maxEstimatedDays=20&sort=estimatedDays'
    Check (@($combined.places|Where-Object {$_.distanceKm -gt 100 -or $_.estimatedDays -gt 20}).Count -eq 0) 'Combined distance and time filters'
    $bad=Invoke-WebRequest "$Api/api/explore/places?minDistanceKm=20&maxDistanceKm=1" -Headers $headers -SkipHttpErrorCheck
    Check ($bad.StatusCode -eq 400) 'Invalid reversed range is rejected'
    $distant=Call GET 'api/explore/places?page=99999'
    Check ($distant.places.Count -eq 0 -and $distant.total -eq $page.total) 'Empty page retains accurate total'
    $passportBefore=Call GET 'api/passport'
    # Use a configured cultural-card destination, rather than assume every imported draft card is enabled.
    $cardDestinations=Call GET ('api/explore/places?q='+[Uri]::EscapeDataString('Văn Miếu'))
    $candidate=@($cardDestinations.places|Where-Object {$_.distanceKm -gt 0.01 -and !$_.isDiscovered -and $_.imageUrl})[0]
    if(!$candidate){throw 'Configure an active cultural-card destination for this fixture before exercising awards.'}
    $detail=Call GET "api/explore/places/$($candidate.placeId)"
    Check (!$detail.isDiscovered -and $detail.dailyStepTarget -eq 10000 -and $detail.estimatedDays -eq [Math]::Ceiling($detail.requiredSteps/10000.0)) 'Preview effort uses current activity and unchanged step formula'
    $null=Call PUT 'api/activity-level' @{activityLevel='LOW'}
    $low=Call GET "api/explore/places/$($candidate.placeId)"
    Check ($low.requiredSteps -eq $detail.requiredSteps -and $low.dailyStepTarget -eq 3000 -and $low.estimatedDays -ge $detail.estimatedDays) 'Activity changes duration without changing required journey steps'
    $null=Call PUT 'api/activity-level' @{activityLevel='HIGH'}
    $passportAfter=Call GET 'api/passport'
    Check (($passportBefore.regions.discovered|Measure-Object -Sum).Sum -eq ($passportAfter.regions.discovered|Measure-Object -Sum).Sum) 'Preview does not record discovery or passport stamps'
    Check (@($passportAfter.regions|Where-Object {$_.total -eq 0 -and $_.hasBadge}).Count -eq 0) 'Empty regions cannot earn a badge'
    $created=Call POST 'api/explore/journeys' @{placeId=$candidate.placeId;replaceActive=$false}
    Check ($created.journey.status -eq 'IN_PROGRESS') 'Existing create-journey flow remains usable'
    $recommendations=Call GET 'api/explore/recommendations'
    Check ($recommendations.places.Count -le 3 -and (@($recommendations.places.placeId|Sort-Object -Unique).Count -eq $recommendations.places.Count)) 'Recommendation API returns up to three distinct valid candidates'
    $weekly=Call GET 'api/quests'
    Check ($weekly.weeklyCycle -match '^\d{4}-W\d{2}$' -and $weekly.weekly.Count -gt 0) 'Weekly quest API uses real ISO cycle and configured quests'
    $admin=Invoke-WebRequest "$Api/api/admin/data/places" -Headers $headers -SkipHttpErrorCheck
    Check ($admin.StatusCode -eq 403) 'Ordinary user cannot access admin place catalog'
    $ranking=Call GET 'api/leaderboard'
    Check ($ranking.me.points -eq 0 -and $null -eq $ranking.me.rank) 'New user has no historical ranking points'
    $rankJson=$ranking|ConvertTo-Json -Depth 8 -Compress
    Check ($rankJson -notmatch 'email|password|latitude|longitude|fullName') 'Leaderboard excludes private account and location fields'
    $badWeek=Invoke-WebRequest "$Api/api/leaderboard?week=2026-W54" -Headers $headers -SkipHttpErrorCheck
    Check ($badWeek.StatusCode -eq 400) 'Invalid ISO week is rejected'
    if($ExerciseAwards) {
        # Explicit synthetic integration fixture, not real sensor/Health Connect evidence.
        $collectionBefore=Call GET 'api/collections'
        $half=[int][Math]::Floor($created.journey.requiredSteps/2)
        $partial=Call POST 'api/explore/sync-steps' @{journeyId=$created.journey.journeyId;cumulativeSteps=$half}
        $partialAgain=Call POST 'api/explore/sync-steps' @{journeyId=$created.journey.journeyId;cumulativeSteps=$half}
        Check ($partial.journey.recordedSteps -eq $partialAgain.journey.recordedSteps) 'Repeated cumulative step fixture cannot duplicate progress'
        $client=[Net.Http.HttpClient]::new()
        try {
            $client.DefaultRequestHeaders.Authorization=[Net.Http.Headers.AuthenticationHeaderValue]::new('Bearer',$account.token)
            $body=@{journeyId=$created.journey.journeyId;cumulativeSteps=$created.journey.requiredSteps}|ConvertTo-Json -Compress
            $first=$client.PostAsync("$Api/api/explore/sync-steps",[Net.Http.StringContent]::new($body,[Text.Encoding]::UTF8,'application/json'))
            $second=$client.PostAsync("$Api/api/explore/sync-steps",[Net.Http.StringContent]::new($body,[Text.Encoding]::UTF8,'application/json'))
            [Threading.Tasks.Task]::WaitAll([Threading.Tasks.Task[]]@($first,$second))
            Check ($first.Result.IsSuccessStatusCode -and $second.Result.IsSuccessStatusCode) 'Concurrent completion requests both resolve successfully'
            $complete=$first.Result.Content.ReadAsStringAsync().GetAwaiter().GetResult()|ConvertFrom-Json
        } finally { $client.Dispose() }
        Check ($complete.journey.status -eq 'COMPLETED' -and $complete.journey.awardedCard.cardId -gt 0) 'Journey completion confirms card award'
        Check ($complete.journey.rankingPoints -eq 150) 'Completion response confirms 150 unified discovery points'
        $receiptRoute="api/journeys/$($created.journey.journeyId)/completion"
        Check ((Invoke-WebRequest "$Api/$receiptRoute" -SkipHttpErrorCheck).StatusCode -eq 401) 'Completion receipt requires authentication'
        $receipt=Call GET $receiptRoute
        Check ($receipt.journeyId -eq $created.journey.journeyId -and $receipt.status -eq 'COMPLETED' -and $receipt.completedAt) 'Read-only completion receipt confirms the owned completed journey'
        Check ($receipt.rankingPoints -eq 150 -and $receipt.recordedSteps -eq $complete.journey.recordedSteps) 'Receipt returns actual awarded points and recorded journey steps'
        Check ($receipt.card.cardId -eq $complete.journey.awardedCard.cardId -and $receipt.card.newlyAwarded) 'Receipt shows only the card actually received at completion'
        $outsider=Call POST 'api/account/register' @{email=('receipt-'+[guid]::NewGuid().ToString('N')+'@example.invalid');password=$password;confirmPassword=$password}
        $private=Invoke-WebRequest "$Api/$receiptRoute" -Headers @{Authorization="Bearer $($outsider.token)"} -SkipHttpErrorCheck
        Check ($private.StatusCode -eq 400 -and ($private.Content|ConvertFrom-Json).code -eq 'JOURNEY_NOT_FOUND') 'Other users cannot read completion receipts'
        $null=Call POST 'api/explore/sync-steps' @{journeyId=$created.journey.journeyId;cumulativeSteps=$created.journey.requiredSteps}
        $collectionAfter=Call GET 'api/collections'
        Check ($collectionAfter.ownedCards -eq $collectionBefore.ownedCards+1) 'Repeated completion grants one collectible card'
        $stamped=Call GET 'api/passport'
        Check (($stamped.regions.discovered|Measure-Object -Sum).Sum -eq ($passportBefore.regions.discovered|Measure-Object -Sum).Sum+1) 'Completion adds exactly one passport stamp'
        $score=Call GET 'api/leaderboard'
        Check ($score.me.points -ge 150 -and $score.me.rank -gt 0) 'First walking completion records approved first-place and journey points'
        $region=@($stamped.regions|Where-Object {@($_.stamps.placeId) -contains $candidate.placeId})[0]
        $regional=Call GET "api/leaderboard?regionId=$($region.regionId)"
        Check ($regional.me.points -eq 150) 'Regional ranking counts destination events, excluding regionless quest points'
        $null=Call GET 'api/quests'
        $inventory=Call GET 'api/inventory'
        $null=Call GET 'api/quests'
        $inventoryAgain=Call GET 'api/inventory'
        Check ($inventory.totalQuantity -gt 0 -and $inventoryAgain.totalQuantity -eq $inventory.totalQuantity) 'Weekly quest reward pays once across repeated reads'
        $scoreAgain=Call GET 'api/leaderboard'
        Check ($scoreAgain.me.points -eq $score.me.points) 'Repeated sync, leaderboard and quest reads cannot add points'
        $null=Call GET $receiptRoute;$null=Call GET $receiptRoute
        Check ((Call GET 'api/leaderboard').me.points -eq $score.me.points) 'Opening and retrying completion receipts never awards points'
        $flightOptions=Call GET 'api/items/flight-ticket'
        $flightDestination=@($page2.places|Where-Object {!$_.isDiscovered})[0]
        $null=Call POST 'api/items/flight-ticket/use' @{placeId=$flightDestination.placeId}
        $afterFlight=Call GET 'api/leaderboard'
        Check ($afterFlight.me.points -eq $scoreAgain.me.points) 'Flight unlock and its discovery quests do not earn walking points'
        foreach($discoveredId in @($candidate.placeId,$flightDestination.placeId)) {
            $rejected=Invoke-WebRequest "$Api/api/explore/journeys" -Method Post -Headers $script:headers -ContentType application/json -Body (@{placeId=$discoveredId;replaceActive=$false}|ConvertTo-Json) -SkipHttpErrorCheck
            Check ($rejected.StatusCode -eq 400 -and ($rejected.Content|ConvertFrom-Json).code -eq 'PLACE_ALREADY_DISCOVERED') 'Walking and flight discoveries cannot be selected for another journey'
        }
        $regionalAgain=Call GET "api/leaderboard?regionId=$($region.regionId)"
        Check ($regionalAgain.me.points -eq 150) 'Rejected revisits leave destination ranking points unchanged'
        $selectable=Call GET 'api/explore/places?onlyUndiscovered=true&pageSize=50'
        Check (@($selectable.places|Where-Object {$_.isDiscovered -or $_.requiredSteps -le 0}).Count -eq 0) 'Destination picker excludes discovered places and current zero-step location before pagination'
        Check (@($selectable.places|Where-Object {$_.placeId -in @($candidate.placeId,$flightDestination.placeId)}).Count -eq 0) 'Both walking and flight discoveries disappear from selectable destinations'
        $nextPlaces=Call GET 'api/explore/places?pageSize=50'
        $nextPlace=@($nextPlaces.places|Where-Object {!$_.isDiscovered -and $_.distanceKm -gt 0.001})[0]
        $nextJourney=Call POST 'api/explore/journeys' @{placeId=$nextPlace.placeId;replaceActive=$false}
        $replaced=Invoke-WebRequest "$Api/api/explore/journeys" -Method Post -Headers $script:headers -ContentType application/json -Body (@{placeId=$candidate.placeId;replaceActive=$true}|ConvertTo-Json) -SkipHttpErrorCheck
        Check ($replaced.StatusCode -eq 400 -and ($replaced.Content|ConvertFrom-Json).code -eq 'PLACE_ALREADY_DISCOVERED') 'Replace-active cannot bypass the discovered-place restriction'
        $current=Call GET 'api/explore'
        Check ($current.journey.journeyId -eq $nextJourney.journey.journeyId -and $current.journey.status -eq 'IN_PROGRESS') 'Rejected discovered destination preserves the existing active journey'
        Check ((Call GET $receiptRoute).journeyId -eq $created.journey.journeyId) 'Historical completion remains readable while another journey is active'
        $unconfirmed=Invoke-WebRequest "$Api/api/journeys/$($nextJourney.journey.journeyId)/completion" -Headers $script:headers -SkipHttpErrorCheck
        Check ($unconfirmed.StatusCode -eq 400 -and ($unconfirmed.Content|ConvertFrom-Json).code -eq 'JOURNEY_NOT_COMPLETED') 'An unfinished journey cannot produce a success receipt'
        # Emulate an in-progress journey created by the old backend, scoped to this QA account.
        $fixturePath=Join-Path (Split-Path $PSScriptRoot -Parent) 'output/dev-v2/verification/stale-discovered-journey.sql'
        $fixtureId=[long]$nextJourney.journey.journeyId
        $fixturePlace=[long]$candidate.placeId
        [IO.File]::WriteAllText($fixturePath,"UPDATE public.journey j SET destination_place_id=$fixturePlace FROM public.app_user u WHERE j.user_id=u.user_id AND u.email='$email' AND u.email LIKE '%@example.invalid' AND j.journey_id=$fixtureId AND j.status='IN_PROGRESS';")
        & (Join-Path $PSScriptRoot 'invoke-dev-v2-sql.ps1') -SqlFile $fixturePath | Out-Null
        $oldCompletion=Invoke-WebRequest "$Api/api/explore/sync-steps" -Method Post -Headers $script:headers -ContentType application/json -Body (@{journeyId=$fixtureId;cumulativeSteps=$nextJourney.journey.requiredSteps}|ConvertTo-Json) -SkipHttpErrorCheck
        Check ($oldCompletion.StatusCode -eq 400 -and ($oldCompletion.Content|ConvertFrom-Json).code -eq 'PLACE_ALREADY_DISCOVERED') 'Legacy in-progress revisit cannot complete through sync-steps'
        $afterRejectedCompletion=Call GET 'api/explore'
        Check ($afterRejectedCompletion.journey.status -eq 'IN_PROGRESS' -and $afterRejectedCompletion.journey.recordedSteps -eq 0) 'Rejected legacy completion rolls back journey progress'
        Check ((Call GET 'api/leaderboard').me.points -eq $afterFlight.me.points) 'Rejected legacy completion cannot award journey, discovery or quest points'
    }
    Write-Output ($checks -join "`n")
    $root=Split-Path $PSScriptRoot -Parent
    [IO.File]::WriteAllText((Join-Path $root 'docs/technical/local_api_test_results.md'),"# Local API integration checks`n`n"+($checks -join "`n`n")+"`n`nTest fixture account created through registration on v2, using example.invalid. No real user copied. Synthetic step fixtures submitted: $ExerciseAwards. This does not verify physical sensor/Health Connect. Test credentials/tokens not exported.`n")
}
catch {
    Write-Output ($checks -join "`n")
    throw
}
