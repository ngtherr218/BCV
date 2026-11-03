import React, { useEffect, useRef, useState } from 'react';
import maplibregl from 'maplibre-gl';
import 'maplibre-gl/dist/maplibre-gl.css';
import { regionViewport } from './regionViewport';

export default function CoordinatePicker({ latitude, longitude, regionId, places, preferRegion, change }) {
  const container = useRef(null), map = useRef(null), marker = useRef(null);
  const latest = useRef(change);
  latest.current = change;
  const [error, setError] = useState('');
  const valid = latitude !== '' && latitude != null && longitude !== '' && longitude != null &&
    Number.isFinite(Number(latitude)) && Number.isFinite(Number(longitude)) &&
    Math.abs(Number(latitude)) <= 90 && Math.abs(Number(longitude)) <= 180;
  useEffect(() => {
    if (!__GOONG_MAP_KEY__) { setError('Chưa cấu hình khóa bản đồ Goong. Bạn vẫn có thể nhập tọa độ bên dưới.'); return; }
    let instance, observer;
    try {
      instance = new maplibregl.Map({ container: container.current,
        style: `https://tiles.goong.io/assets/goong_map_web.json?api_key=${__GOONG_MAP_KEY__}`,
        center: valid ? [Number(longitude), Number(latitude)] : [106.1, 16.2], zoom: valid ? 14 : 4.7 });
      map.current = instance;
      instance.addControl(new maplibregl.NavigationControl(), 'top-right');
      const select = point => latest.current({ latitude: Number(point.lat.toFixed(6)), longitude: Number(point.lng.toFixed(6)) });
      marker.current = new maplibregl.Marker({ color: '#07836d', draggable: true });
      marker.current.on('dragend', () => select(marker.current.getLngLat()));
      instance.on('click', event => select(event.lngLat));
      instance.on('error', () => setError('Không tải được bản đồ. Kiểm tra kết nối hoặc nhập tọa độ bên dưới.'));
      instance.on('load', () => setError(''));
      observer = new ResizeObserver(() => instance.resize());
      observer.observe(container.current);
    } catch { setError('Không mở được bản đồ. Bạn vẫn có thể nhập tọa độ bên dưới.'); }
    return () => { observer?.disconnect(); marker.current?.remove(); instance?.remove(); map.current = null; };
  }, []);
  useEffect(() => {
    if (!map.current || !marker.current) return;
    if (valid) marker.current.setLngLat([Number(longitude), Number(latitude)]).addTo(map.current);
    else marker.current.remove();
    const bounds = regionViewport(regionId, places);
    if (bounds && (preferRegion || !valid)) {
      map.current.fitBounds(bounds, { padding: 40, maxZoom: 12, duration: 250 });
    } else if (valid && !preferRegion) {
      map.current.easeTo({ center: [Number(longitude), Number(latitude)], zoom: 14, duration: 250 });
    } else {
      map.current.easeTo({ center: [106.1, 16.2], zoom: 4.7, duration: 250 });
    }
  }, [latitude, longitude, valid, regionId, places, preferRegion]);
  return <section className="coordinate-picker" aria-label="Chọn vị trí địa danh trên bản đồ">
    <strong>Vị trí trên bản đồ</strong>
    <p>Bấm trên bản đồ hoặc kéo ghim để chọn vị trí. Vĩ độ, kinh độ và khu vực sẽ tự cập nhật.</p>
    {regionId && !regionViewport(regionId, places) && <p>Khu vực chưa có địa danh để xác định khung nhìn. Hãy tìm vị trí trên bản đồ hoặc nhập tọa độ.</p>}
    <div ref={container} className="coordinate-map" />
    <small aria-live="polite">{valid ? `Đã chọn: ${latitude}, ${longitude}` : 'Chưa chọn vị trí.'}</small>
    {error && <p className="field-error" role="alert">{error}</p>}
  </section>;
}
