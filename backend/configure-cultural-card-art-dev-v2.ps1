param([switch]$Apply)
$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
$root=Split-Path $PSScriptRoot -Parent
$output=Join-Path $root 'output/dev-v2/card-art'
New-Item -ItemType Directory -Force $output | Out-Null
$catalog=@(
    @{place=2;name='Tháp Chăm Ponagar';region='KHÁNH HÒA';icon='tower'},
    @{place=6;name='Ngọc đảo Phú Quốc';region='KIÊN GIANG';icon='island'},
    @{place=11;name='Nhà gỗ Tây Nguyên';region='KON TUM';icon='church'},
    @{place=31;name='Cờ trên mây Fansipan';region='LÀO CAI';icon='flag'},
    @{place=51;name='Thuyền khám phá Tràng An';region='NINH BÌNH';icon='river'},
    @{place=71;name='Thạch nhũ Phong Nha';region='QUẢNG BÌNH';icon='cave'},
    @{place=76;name='Đèn lồng Hội An';region='QUẢNG NAM';icon='lantern'},
    @{place=86;name='Thuyền buồm Hạ Long';region='QUẢNG NINH';icon='sail'},
    @{place=283;name='Khuê Văn Các';region='HÀ NỘI';icon='pavilion'},
    @{place=21;name='Sắc hoa Đà Lạt';region='LÂM ĐỒNG';icon='flower'}
)
$teal=[Drawing.ColorTranslator]::FromHtml('#176B5B')
$gold=[Drawing.ColorTranslator]::FromHtml('#C58B42')
$cream=[Drawing.ColorTranslator]::FromHtml('#F7F1E4')
$pale=[Drawing.ColorTranslator]::FromHtml('#E2EADD')
$brush=[Drawing.SolidBrush]::new($teal)
$accent=[Drawing.SolidBrush]::new($gold)
$paper=[Drawing.SolidBrush]::new($cream)
$tint=[Drawing.SolidBrush]::new($pale)
$pen=[Drawing.Pen]::new($teal,8)
$line=[Drawing.Pen]::new($gold,3)
$heading=[Drawing.Font]::new('Segoe UI',25,[Drawing.FontStyle]::Bold)
$small=[Drawing.Font]::new('Segoe UI',14)
$center=[Drawing.StringFormat]::new()
$center.Alignment=[Drawing.StringAlignment]::Center
$center.LineAlignment=[Drawing.StringAlignment]::Center
function Poly($points,$fill=$brush){
    $vertices=[Drawing.PointF[]]@($points | ForEach-Object { [Drawing.PointF]::new($_[0],$_[1]) })
    $graphics.FillPolygon($fill,$vertices)
}
try {
    foreach($card in $catalog){
        $bitmap=[Drawing.Bitmap]::new(640,800)
        $graphics=[Drawing.Graphics]::FromImage($bitmap)
        try {
            $graphics.SmoothingMode=[Drawing.Drawing2D.SmoothingMode]::AntiAlias
            $graphics.TextRenderingHint=[Drawing.Text.TextRenderingHint]::AntiAliasGridFit
            $graphics.Clear($cream)
            $graphics.DrawRectangle($line,24,24,592,752)
            $graphics.DrawString('BƯỚC CHÂN VIỆT',$small,$brush,[Drawing.RectangleF]::new(50,55,540,35),$center)
            $graphics.DrawString($card.region,$small,$accent,[Drawing.RectangleF]::new(50,100,540,35),$center)
            $graphics.FillEllipse($tint,100,180,440,440)
            switch($card.icon){
                'tower' {
                    Poly @(@(230,530),@(245,300),@(395,300),@(410,530))
                    Poly @(@(240,300),@(270,265),@(370,265),@(400,300)) $accent
                    Poly @(@(270,265),@(290,230),@(350,230),@(370,265))
                    $graphics.FillRectangle($paper,295,425,50,105)
                    foreach($y in @(340,375)){$graphics.DrawLine($line,250,$y,390,$y)}
                }
                'island' {
                    $graphics.FillEllipse($accent,190,470,270,55)
                    $graphics.DrawBezier($pen,330,485,345,410,335,350,300,310)
                    foreach($tip in @(@(215,320),@(245,265),@(320,250),@(390,295),@(405,355))){$graphics.DrawBezier($pen,300,310,300,290,$tip[0],$tip[1],$tip[0],$tip[1])}
                    $graphics.DrawArc($line,185,510,280,45,0,180)
                }
                'church' {
                    $graphics.FillRectangle($brush,215,375,210,155)
                    Poly @(@(190,380),@(320,290),@(450,380)) $accent
                    $graphics.FillRectangle($brush,280,280,80,100)
                    Poly @(@(260,285),@(320,205),@(380,285))
                    $graphics.FillRectangle($paper,300,450,40,80)
                    $graphics.DrawLine($line,320,220,320,185)
                    $graphics.DrawLine($line,305,198,335,198)
                }
                'flag' {
                    Poly @(@(155,530),@(300,310),@(365,420),@(415,350),@(495,530))
                    Poly @(@(265,365),@(300,310),@(335,365)) $paper
                    $graphics.DrawLine($pen,300,310,300,220)
                    Poly @(@(304,220),@(400,235),@(304,268)) $accent
                }
                'river' {
                    Poly @(@(155,445),@(220,270),@(285,445)) $tint
                    Poly @(@(365,445),@(420,255),@(490,445)) $brush
                    Poly @(@(200,455),@(440,455),@(405,500),@(235,500)) $accent
                    $graphics.DrawLine($pen,340,415,405,525)
                    $graphics.DrawArc($line,180,505,280,40,0,180)
                }
                'cave' {
                    Poly @(@(170,270),@(470,270),@(470,355),@(430,325),@(390,430),@(355,320),@(310,390),@(275,325),@(240,460),@(205,320),@(170,350))
                    Poly @(@(190,530),@(235,420),@(270,530),@(325,455),@(365,530),@(420,435),@(455,530)) $accent
                }
                'lantern' {
                    $graphics.DrawLine($pen,320,205,320,260)
                    $graphics.FillEllipse($accent,210,265,220,235)
                    $graphics.DrawEllipse($pen,255,265,130,235)
                    $graphics.DrawLine($pen,320,265,320,500)
                    $graphics.FillRectangle($brush,265,250,110,22)
                    $graphics.FillRectangle($brush,265,495,110,22)
                    foreach($x in @(290,320,350)){$graphics.DrawLine($line,$x,517,$x,570)}
                }
                'sail' {
                    Poly @(@(175,475),@(465,475),@(420,525),@(215,525))
                    $graphics.DrawLine($pen,320,260,320,475)
                    Poly @(@(305,280),@(210,455),@(305,455)) $accent
                    Poly @(@(338,245),@(435,450),@(338,450)) $accent
                    $graphics.DrawArc($line,175,530,290,35,0,180)
                }
                'pavilion' {
                    $graphics.FillRectangle($brush,220,340,200,100)
                    Poly @(@(175,340),@(250,305),@(320,265),@(390,305),@(465,340)) $accent
                    $graphics.FillEllipse($paper,290,355,60,60)
                    foreach($x in @(235,385)){$graphics.FillRectangle($brush,$x,440,20,100)}
                    $graphics.FillRectangle($accent,210,535,220,15)
                }
                'flower' {
                    $graphics.DrawLine($pen,320,400,320,555)
                    $graphics.FillEllipse($brush,245,470,75,35)
                    $graphics.FillEllipse($brush,325,500,75,35)
                    foreach($angle in @(0,60,120,180,240,300)){
                        $radians=$angle*[Math]::PI/180
                        $graphics.FillEllipse($accent,[single](320+70*[Math]::Cos($radians)-43),[single](345+70*[Math]::Sin($radians)-43),86,86)
                    }
                    $graphics.FillEllipse($brush,282,307,76,76)
                }
            }
            $graphics.DrawString($card.name,$heading,$brush,[Drawing.RectangleF]::new(55,635,530,92),$center)
            $graphics.DrawString('DẤU ẤN VĂN HÓA · BỘ SƯU TẬP',$small,$accent,[Drawing.RectangleF]::new(50,730,540,30),$center)
            $bitmap.Save((Join-Path $output "$($card.place).jpg"),[Drawing.Imaging.ImageFormat]::Jpeg)
        } finally {$graphics.Dispose();$bitmap.Dispose()}
    }
} finally {foreach($resource in @($brush,$accent,$paper,$tint,$pen,$line,$heading,$small,$center)){$resource.Dispose()}}
Write-Output 'Generated 10 original geometric cultural-card illustrations locally.'
if(!$Apply){return}
$api='http://127.0.0.1:5082/api/admin'
if((Invoke-RestMethod 'http://127.0.0.1:5082/health').database -cne 'vietnam_explorer_dev_v2'){throw 'Wrong database; no login or card writes performed.'}
$credentials=Get-Content (Join-Path $root 'output/dev-v2/admin-credentials.json')|ConvertFrom-Json
$auth=Invoke-RestMethod "$api/login" -Method Post -ContentType application/json -Body (@{email=$credentials.email;password=$credentials.password}|ConvertTo-Json)
$headers=@{Authorization="Bearer $($auth.token)"}
$overview=Invoke-RestMethod "$api/exploration/overview" -Headers $headers
if($overview.database -cne 'vietnam_explorer_dev_v2'){throw 'Wrong database; no card was changed.'}
$backups=@()
foreach($card in $catalog){
    $matches=@($overview.cards | Where-Object { $_.placeId -eq $card.place -and $_.name -ceq $card.name })
    if($matches.Count -ne 1){throw "Expected one cultural card for place $($card.place); no cards changed."}
    $detail=Invoke-RestMethod "$api/data/cards/$($matches[0].cardId)" -Headers $headers
    $backups+=@{id=$matches[0].cardId;detail=$detail;place=$card.place}
}
$backup=Join-Path $output ('before-'+(Get-Date -Format 'yyyyMMdd-HHmmss')+'.json')
$backups|ConvertTo-Json -Depth 20|Set-Content $backup
foreach($entry in $backups){
    # Verify identity again before each write. No SQL/schema migration is executed.
    if((Invoke-RestMethod "$api/exploration/overview" -Headers $headers).database -cne 'vietnam_explorer_dev_v2'){throw 'Target changed; abort.'}
    $bytes=[IO.File]::ReadAllBytes((Join-Path $output "$($entry.place).jpg"))
    $image=Invoke-RestMethod "$api/images?purpose=card" -Headers $headers -Method Post -ContentType image/jpeg -Body $bytes
    $body=@{name=$entry.detail.name;description=$entry.detail.description;status=$entry.detail.status;place_id=$entry.detail.place_id;image_key=$image.url}
    $null=Invoke-RestMethod "$api/data/cards/$($entry.id)" -Headers $headers -Method Put -ContentType application/json -Body ($body|ConvertTo-Json -Depth 5)
    $actual=Invoke-RestMethod "$api/data/cards/$($entry.id)" -Headers $headers
    if($actual.image_key -cne $image.url -or $actual.place_id -ne $entry.detail.place_id){throw 'Card verification failed; original values are in the backup.'}
    Write-Output "PASS: Cultural artwork saved and verified for place $($entry.place), card $($entry.id)."
}
Write-Output 'Only existing cultural-card images were changed on local v2; names, descriptions, status, links and ownership were retained. Backup stored in output/dev-v2/card-art.'
$credentials=$null
