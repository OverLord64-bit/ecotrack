import { NavLink, Navigate, Route, Routes } from 'react-router-dom';
import { NewReportPage } from './pages/NewReportPage';
import { MyReportsPage } from './pages/MyReportsPage';
import { ReportDetailPage } from './pages/ReportDetailPage';
import { PointsMapPage } from './pages/PointsMapPage';
import { AgentDashboardPage } from './pages/AgentDashboardPage';
import { AgentReportPage } from './pages/AgentReportPage';

export function App() {
  return (
    <>
      <header className="topbar">
        <strong>EcoTrack</strong>
        <nav>
          <span className="space">Citizen</span>
          <NavLink to="/citizen/new">Report</NavLink>
          <NavLink to="/citizen/reports">Reports</NavLink>
          <NavLink to="/citizen/map">Collection points</NavLink>
          <span className="space">Agent</span>
          <NavLink to="/agent">Dashboard</NavLink>
        </nav>
      </header>
      <main>
        <Routes>
          <Route path="/" element={<Navigate to="/citizen/reports" replace />} />
          <Route path="/citizen/new" element={<NewReportPage />} />
          <Route path="/citizen/reports" element={<MyReportsPage />} />
          <Route path="/citizen/reports/:id" element={<ReportDetailPage />} />
          <Route path="/citizen/map" element={<PointsMapPage />} />
          <Route path="/agent" element={<AgentDashboardPage />} />
          <Route path="/agent/reports/:id" element={<AgentReportPage />} />
          <Route path="*" element={<p>Page not found.</p>} />
        </Routes>
      </main>
    </>
  );
}
