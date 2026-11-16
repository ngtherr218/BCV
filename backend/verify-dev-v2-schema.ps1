$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$docs=Join-Path $root 'docs/technical'
$before=Get-Content (Join-Path $docs 'database_schema_before.snapshot.json') -Raw|ConvertFrom-Json
$after=Get-Content (Join-Path $docs 'database_schema_after.snapshot.json') -Raw|ConvertFrom-Json
$source=Get-Content (Join-Path $docs 'database_schema_source_recheck.snapshot.json') -Raw|ConvertFrom-Json
$allowedTables=@('interest_group','user_preference_interest','recommendation_rating_snapshot','place_type_feature','community_media','media_report','explorer_score_event','explorer_reward_config')
$allowedColumns=@('app_user.last_login_at','app_user.media_upload_blocked','user_card.seen_at','notification.hidden_at','place.feature_source','recommendation_config.settings','quest.reward_points','quest.description','quest_progress.awarded_points','quest_progress.points_settled')
$allowedConstraints=@('place.place_feature_source_check','app_user.app_user_media_upload_blocked_not_null','place.place_feature_source_not_null','recommendation_config.recommendation_config_settings_not_null','quest.quest_reward_points_range','quest.quest_reward_points_not_null','quest_progress.quest_progress_awarded_points_not_null','quest_progress.quest_progress_points_settled_not_null')
$issues=[Collections.Generic.List[string]]::new()
function Json($value){ConvertTo-Json -InputObject $value -Depth 20 -Compress}
function NormalizeConstraint([string]$value){
    # PostgreSQL's dump/restore deparser distributes a varchar[] -> text[] cast.
    # Only this exact, enumerated equivalent status expression is normalized.
    $value.Replace("ANY ((ARRAY['IN_PROGRESS'::character varying, 'COMPLETED'::character varying, 'ABANDONED'::character varying])::text[])","ANY (ARRAY[('IN_PROGRESS'::character varying)::text, ('COMPLETED'::character varying)::text, ('ABANDONED'::character varying)::text])")
}
if((Json $before) -cne (Json $source)){$issues.Add('Source schema or row counts changed between read-only snapshots.')}
foreach($table in $before){
    $new=@($after|Where-Object table -eq $table.table)
    if($new.Count -ne 1){$issues.Add("Missing old table: $($table.table)");continue}
    foreach($column in $table.columns){
        $match=@($new[0].columns|Where-Object {$_[0] -ceq $column[0]})
        if($match.Count -ne 1 -or (Json $column) -cne (Json $match[0])){$issues.Add("Changed old column: $($table.table).$($column[0])")}
    }
    foreach($column in $new[0].columns){
        if($column[0] -cnotin @($table.columns|ForEach-Object {$_[0]}) -and "$($table.table).$($column[0])" -cnotin $allowedColumns){$issues.Add("Unapproved column: $($table.table).$($column[0])")}
    }
    foreach($constraint in $table.constraints){
        $match=@($new[0].constraints|Where-Object {$_[0] -ceq $constraint[0]})
        if($match.Count -ne 1 -or (NormalizeConstraint (Json $constraint)) -cne (NormalizeConstraint (Json $match[0]))){$issues.Add("Changed old constraint: $($table.table).$($constraint[0])")}
    }
    foreach($constraint in $new[0].constraints){
        if($constraint[0] -cnotin @($table.constraints|ForEach-Object {$_[0]}) -and "$($table.table).$($constraint[0])" -cnotin $allowedConstraints){$issues.Add("Unapproved constraint: $($table.table).$($constraint[0])")}
    }
    foreach($index in $table.indexes){
        $match=@($new[0].indexes|Where-Object {$_[0] -ceq $index[0]})
        if($match.Count -ne 1 -or (Json $index) -cne (Json $match[0])){$issues.Add("Changed old index: $($table.table).$($index[0])")}
    }
    foreach($index in $new[0].indexes){
        if($index[0] -cnotin @($table.indexes|ForEach-Object {$_[0]}) -and "$($table.table).$($index[0])" -cne 'recommendation_config.one_active_recommendation_config'){$issues.Add("Unapproved index: $($table.table).$($index[0])")}
    }
}
$newTables=@($after|Where-Object {$_.table -cnotin $before.table}|ForEach-Object table)
foreach($name in $newTables){if($name -cnotin $allowedTables){$issues.Add("Unapproved table: $name")}}
foreach($name in $allowedTables){if($name -cnotin $newTables){$issues.Add("Approved table missing: $name")}}
$report=@('# Đối chiếu schema local v2','',"Captured UTC: $([DateTimeOffset]::UtcNow.ToString('O'))",'',"Source before/recheck: $($before.Count)/$($source.Count) public tables. Target: $($after.Count).",'','Approved additions: 8 tables (place_video removed at explicit user request on 11/11/2026), 10 columns (including per-quest points, description and settled reward snapshots), CHECKs on a new column and one partial UNIQUE index on an old table. New-table PK/FK/UNIQUE/index definitions are in the snapshots and approval SQL.','','Known restore difference: journey_status_check distributes the varchar-array cast to individual text elements. The accepted status set remains IN_PROGRESS/COMPLETED/ABANDONED. Original and restored definitions remain available in both snapshots. No corrective ALTER was executed.','')
if($issues.Count){$report+=@('FAIL')+@($issues);[IO.File]::WriteAllText((Join-Path $docs 'schema_comparison_results.md'),($report -join "`n"));throw ($issues -join "`n")}
$report+=@('PASS: Source bcv schema AND per-table row counts equal the initial snapshot.','PASS: All old table/column types, NULL constraints, defaults, identity metadata, PK/FK/UNIQUE/CHECK/index definitions preserved, except the documented equivalent deparser representation.','PASS: Added tables/columns/constraints/indexes on old tables match the approved allowlist.','This comparison does not export personal rows or prove every newly added application behavior. New business tables were initially empty before target-only reference seeding and integration fixtures.')
[IO.File]::WriteAllText((Join-Path $docs 'schema_comparison_results.md'),($report -join "`n"))
Write-Output ($report[-4..-1] -join "`n")
