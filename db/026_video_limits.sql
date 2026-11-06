-- Upgrade the existing duration constraint; preserve every stored video.
ALTER TABLE place_video DROP CONSTRAINT IF EXISTS place_video_duration_seconds_check;
ALTER TABLE place_video ADD CONSTRAINT place_video_duration_seconds_check CHECK(duration_seconds>0 AND duration_seconds<=60);
