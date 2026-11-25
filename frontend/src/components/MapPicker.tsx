import { MapContainer, CircleMarker, TileLayer, useMapEvents } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';

function Clicks({ onPick }: { onPick: (lat: number, lng: number) => void }) {
  useMapEvents({ click: (e) => onPick(e.latlng.lat, e.latlng.lng) });
  return null;
}

/** Click on the map to choose the location of a report. */
export function MapPicker({ lat, lng, onPick }: { lat?: number; lng?: number; onPick: (lat: number, lng: number) => void }) {
  return (
    <MapContainer center={[lat ?? 36.8065, lng ?? 10.1815]} zoom={13} className="map">
      <TileLayer attribution="&copy; OpenStreetMap contributors" url="https://tile.openstreetmap.org/{z}/{x}/{y}.png" />
      <Clicks onPick={onPick} />
      {lat !== undefined && lng !== undefined && <CircleMarker center={[lat, lng]} radius={9} />}
    </MapContainer>
  );
}
