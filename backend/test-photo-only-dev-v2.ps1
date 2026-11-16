param([string]$Api='http://127.0.0.1:5082')
$ErrorActionPreference='Stop'
if($Api -cne 'http://127.0.0.1:5082'){throw 'Local v2 only'}
if((Invoke-RestMethod "$Api/health").database -cne 'vietnam_explorer_dev_v2'){throw 'Wrong database'}
$headers=@{}
function Call($method,$path,$body=$null){$args=@{Uri="$Api/$path";Method=$method;Headers=$headers;ContentType='application/json'};if($null -ne $body){$args.Body=$body|ConvertTo-Json -Compress};Invoke-RestMethod @args}
function Check($ok,$name){if(!$ok){throw "FAIL: $name"};Write-Output "PASS: $name"}
$password='Photo-'+[Guid]::NewGuid().ToString('N')+'!'
$account=Call POST api/account/register @{email=('photo-only-'+[Guid]::NewGuid().ToString('N')+'@example.invalid');password=$password;confirmPassword=$password}
$headers=@{Authorization="Bearer $($account.token)"}
$null=Call PUT api/interests @{placeTypeIds=@(201)}
$null=Call PUT api/activity-level @{activityLevel='HIGH'}
$null=Call PUT api/starting-point @{source='SUGGESTED'}
$page=Call GET 'api/explore/places?pageSize=10'
$place=@($page.places|Where-Object {$_.distanceKm -gt 0.01})[0]
$trip=Call POST api/explore/journeys @{placeId=$place.placeId;replaceActive=$false}
$null=Call POST api/explore/sync-steps @{journeyId=$trip.journey.journeyId;cumulativeSteps=$trip.journey.requiredSteps}
Add-Type -AssemblyName System.Drawing
$image=[Drawing.Bitmap]::new(16,16)
$stream=[IO.MemoryStream]::new()
try{$image.Save($stream,[Drawing.Imaging.ImageFormat]::Jpeg);$bytes=$stream.ToArray()}finally{$image.Dispose();$stream.Dispose()}
$upload=Invoke-RestMethod "$Api/api/places/$($place.placeId)/media?caption=Photo-only-QA" -Method Post -Headers $headers -ContentType image/jpeg -Body $bytes
$items=(Call GET "api/places/$($place.placeId)/media").items
$item=@($items|Where-Object caption -eq 'Photo-only-QA')[0]
Check ($item.mimeType -eq 'image/jpeg' -and $item.status -eq 'PENDING') 'Photo upload remains available'
foreach($mime in @('video/mp4','video/webm')){$response=Invoke-WebRequest "$Api/api/places/$($place.placeId)/media" -Method Post -Headers $headers -ContentType $mime -Body $bytes -SkipHttpErrorCheck;Check ($response.StatusCode -eq 400) "$mime upload rejected"}
foreach($path in @('api/admin/place-videos/1',"api/explore/places/$($place.placeId)/videos/1")){$response=Invoke-WebRequest "$Api/$path" -Headers $headers -SkipHttpErrorCheck;Check ($response.StatusCode -eq 404) 'Removed video endpoint returns 404'}
$credential=Get-Content output/dev-v2/admin-credentials.json|ConvertFrom-Json
$admin=Call POST api/admin/login @{email=$credential.email;password=$credential.password}
$userHeaders=$headers
$headers=@{Authorization="Bearer $($admin.token)"}
$null=Call POST "api/admin/community-media/$($item.mediaId)/review" @{status='APPROVED';expectedStatus='PENDING'}
$headers=$userHeaders
$approved=(Call GET "api/places/$($place.placeId)/media?approvedOnly=true").items
Check (@($approved|Where-Object mediaId -eq $item.mediaId).Count -eq 1) 'Photo moderation remains available'
$response=Invoke-WebRequest "$Api$($item.contentUrl)" -Headers $headers
Check ($response.StatusCode -eq 200) 'Approved photo content remains readable'
$headers=@{Authorization="Bearer $($admin.token)"}
$null=Call POST "api/admin/community-media/$($item.mediaId)/review" @{status='REMOVED';expectedStatus='APPROVED';note='Cleanup isolated photo-only QA fixture'}
Write-Output 'Only QA photo removed; fixture account/journey retained as synthetic test evidence.'
