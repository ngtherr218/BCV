// The database is the source of truth, including administrator-created types.
export function suggestedFeatures(typeId, types = [], features = []) {
  const type = types.find(type => String(type.id) === String(typeId));
  const supported = new Set(features.map(feature => feature.code));
  return (type?.feature_codes || []).filter(code => supported.has(code));
}
export function changePlaceType(data, typeId, suggested = []) {
  return { ...data, place_type_id: typeId, feature_source: 'TYPE', feature_codes: [...new Set(suggested)] };
}
