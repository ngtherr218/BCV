// Use the region's existing destinations to frame its area without a geocoding request.
export function regionViewport(regionId, places = []) {
  if (regionId === '' || regionId == null) return null;
  const points = places.filter(place => String(place.region_id) === String(regionId) &&
    place.latitude !== '' && place.latitude != null && place.longitude !== '' && place.longitude != null &&
    Number.isFinite(Number(place.latitude)) && Number.isFinite(Number(place.longitude)) &&
    Math.abs(Number(place.latitude)) <= 90 && Math.abs(Number(place.longitude)) <= 180);
  if (!points.length) return null;
  const lngs = points.map(point => Number(point.longitude));
  const lats = points.map(point => Number(point.latitude));
  return [[Math.min(...lngs), Math.min(...lats)], [Math.max(...lngs), Math.max(...lats)]];
}
