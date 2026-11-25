import { FormEvent, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import { WASTE_CATEGORIES, WasteCategory } from '../api/types';
import { ErrorBox } from '../components/Loading';
import { MapPicker } from '../components/MapPicker';

const MAX_PHOTO = 5 * 1024 * 1024;

export function NewReportPage() {
  const navigate = useNavigate();
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [category, setCategory] = useState<WasteCategory>('HOUSEHOLD');
  const [address, setAddress] = useState('');
  const [position, setPosition] = useState<{ lat: number; lng: number }>();
  const [photo, setPhoto] = useState<File>();
  const [errors, setErrors] = useState<string[]>([]);
  const [apiError, setApiError] = useState<unknown>();
  const [busy, setBusy] = useState(false);

  function validate(): string[] {
    const found: string[] = [];
    if (description.trim().length < 5) found.push('Describe the problem (5 characters minimum).');
    if (!position) found.push('Click on the map to set the location.');
    if (photo && !['image/jpeg', 'image/png', 'image/webp'].includes(photo.type)) found.push('The photo must be a JPEG, PNG or WebP image.');
    if (photo && photo.size > MAX_PHOTO) found.push('The photo must be smaller than 5 MB.');
    return found;
  }

  async function submit(e: FormEvent) {
    e.preventDefault();
    const found = validate();
    setErrors(found);
    if (found.length > 0 || !position) return;
    setBusy(true);
    setApiError(undefined);
    try {
      const report = await api.createReport({
        reporterName: name || undefined, description: description.trim(), wasteCategory: category,
        latitude: position.lat, longitude: position.lng, address: address || undefined,
      });
      if (photo) await api.uploadPhoto(report.id, photo);
      navigate(`/citizen/reports/${report.id}`);
    } catch (err) {
      setApiError(err);
    } finally {
      setBusy(false);
    }
  }

  return (
    <form onSubmit={submit} className="card form">
      <h1>Report illegal dumping</h1>
      <label>Your name (optional)<input value={name} onChange={(e) => setName(e.target.value)} maxLength={120} /></label>
      <label>Type of waste
        <select value={category} onChange={(e) => setCategory(e.target.value as WasteCategory)}>
          {WASTE_CATEGORIES.map((c) => <option key={c}>{c}</option>)}
        </select>
      </label>
      <label>Description<textarea value={description} onChange={(e) => setDescription(e.target.value)} rows={3} maxLength={2000} /></label>
      <label>Address (optional)<input value={address} onChange={(e) => setAddress(e.target.value)} maxLength={200} /></label>
      <div>
        <p>Location {position ? `(${position.lat.toFixed(5)}, ${position.lng.toFixed(5)})` : '(click on the map)'}</p>
        <MapPicker lat={position?.lat} lng={position?.lng} onPick={(lat, lng) => setPosition({ lat, lng })} />
      </div>
      <label>Photo (optional)<input type="file" accept="image/jpeg,image/png,image/webp" onChange={(e) => setPhoto(e.target.files?.[0])} /></label>
      {errors.length > 0 && <div className="error" role="alert"><ul>{errors.map((m) => <li key={m}>{m}</li>)}</ul></div>}
      <ErrorBox error={apiError} />
      <button disabled={busy}>{busy ? 'Sending…' : 'Send report'}</button>
    </form>
  );
}
