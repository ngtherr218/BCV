const normalize = value => String(value || '').trim().normalize('NFD').replace(/[\u0300-\u036f]/g, '')
  .replace(/đ/gi, 'd').toLowerCase().replace(/^(tinh|thanh pho|tp\.?)\s+/, '')
  .replace(/[^a-z0-9]/g, '');

export function matchPinRegion(address, regions) {
  // Existing datasets use pre-merger regions. Prefer the old province when present
  // so a point in former Hải Dương isn't silently assigned to former Hải Phòng.
  const name = address.legacyProvince || address.province;
  if (!name) return null;
  const matches = regions.filter(region => normalize(region.name) === normalize(name));
  return matches.length === 1 ? matches[0] : null;
}
