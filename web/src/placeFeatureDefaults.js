// Stable standard IDs shared with RecommendationScoring.PlaceTypeFeatures.
// Defaults are editable starting points, not assertions about every place in a type.
export const typeFeatures = {
  101: ['kind__heritage', 'kind__museum'],
  102: ['kind__religious'],
  103: ['kind__structure'],
  104: ['kind__community', 'kind__craft'],
  105: ['kind__market', 'theme__cuisine'],
  106: ['kind__recreation'],
  107: ['kind__agriculture'],
  108: ['landform__coast', 'landform__island', 'landform__lagoon', 'landform__dunes'],
  109: ['landform__river', 'landform__lake', 'landform__waterfall', 'landform__stream'],
  110: ['landform__mountain', 'landform__valley'],
  111: ['landform__cave'],
  112: ['ecosystem__forest', 'ecosystem__flooded_habitat', 'ecosystem__grassland'],
};
// Starting selections for each type; administrators refine alternatives per place.
const defaults = {
  101: ['kind__heritage', 'theme__history', 'theme__culture'],
  102: ['kind__religious', 'theme__spirituality'],
  103: ['kind__structure', 'theme__architecture'],
  104: ['kind__community', 'theme__culture'],
  105: ['kind__market', 'theme__cuisine'],
  106: ['kind__recreation', 'theme__relaxation'],
  107: ['kind__agriculture', 'theme__agriculture'],
  108: ['kind__nature', 'landform__coast'],
  109: ['kind__nature', 'landform__river'],
  110: ['kind__nature', 'landform__mountain'],
  111: ['kind__nature', 'landform__cave', 'theme__geology'],
  112: ['kind__nature', 'ecosystem__forest', 'theme__ecology'],
};
export function suggestedFeatures(typeId, types = [], features = []) {
  if (!types.some(type => String(type.id) === String(typeId))) return [];
  const supported = new Set(features.map(feature => feature.code));
  return (defaults[typeId] || []).filter(code => supported.has(code));
}
export function changePlaceType(data, typeId, suggested = []) {
  return { ...data, place_type_id: typeId, feature_codes: typeId ? [...new Set(suggested)] : [] };
}
