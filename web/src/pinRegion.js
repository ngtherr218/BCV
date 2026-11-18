const normalize = value => String(value || '').trim().normalize('NFD').replace(/[\u0300-\u036f]/g, '')
  .replace(/đ/gi, 'd').toLowerCase().replace(/^(tinh|thanh pho|tp\.?)\s+/, '')
  .replace(/[^a-z0-9]/g, '');

export function matchPinRegion(address, regions) {
  // Khu vực are the 34 provinces since the 2025 merger: match Goong's current province first,
  // and the pre-merger name only as a fallback (e.g. an older address).
  for (const name of [address.province, address.legacyProvince]) {
    if (!name) continue;
    const matches = regions.filter(region => normalize(region.name) === normalize(name));
    if (matches.length === 1) return matches[0];
  }
  return null;
}
