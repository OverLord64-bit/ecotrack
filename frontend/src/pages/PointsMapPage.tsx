import { useEffect, useState } from 'react';
import { CircleMarker, MapContainer, Popup, TileLayer } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import { api } from '../api/client';
import type { CollectionPoint } from '../api/types';
import { ErrorBox, Loading } from '../components/Loading';

export function PointsMapPage() {
  const [points, setPoints] = useState<CollectionPoint[]>();
  const [error, setError] = useState<unknown>();
  useEffect(() => { api.collectionPoints().then(setPoints).catch(setError); }, []);

  if (error) return <ErrorBox error={error} />;
  if (!points) return <Loading />;
  return (
    <div className="card">
      <h1>Collection points</h1>
      <MapContainer center={[36.83, 10.2]} zoom={11} className="map">
        <TileLayer attribution="&copy; OpenStreetMap contributors" url="https://tile.openstreetmap.org/{z}/{x}/{y}.png" />
        {points.filter((p) => p.active).map((p) => (
          <CircleMarker key={p.id} center={[p.latitude, p.longitude]} radius={10}>
            <Popup><strong>{p.name}</strong><br />{p.address}<br />{p.wasteTypes.join(', ')}</Popup>
          </CircleMarker>
        ))}
      </MapContainer>
      <ul>{points.map((p) => <li key={p.id}>{p.name} — {p.wasteTypes.join(', ')}</li>)}</ul>
    </div>
  );
}
