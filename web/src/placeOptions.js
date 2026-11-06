export const MAP_PHOTO_MIN_ZOOM = 11;
export const showMapPhotos = zoom => Number.isFinite(zoom) && zoom >= MAP_PHOTO_MIN_ZOOM;
export const availableCardPlaces = (places = [], currentPlaceId = null) => places.filter(place => !place.card_id || String(place.id) === String(currentPlaceId));
