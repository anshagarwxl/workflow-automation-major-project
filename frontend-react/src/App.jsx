import { Route, Routes } from 'react-router-dom';
import Layout from './components/Layout.jsx';
import Dashboard from './pages/Dashboard.jsx';
import Events from './pages/Events.jsx';
import EventDetail from './pages/EventDetail.jsx';
import Register from './pages/Register.jsx';
import Registrations from './pages/Registrations.jsx';
import RegistrationTracker from './pages/RegistrationTracker.jsx';
import InstanceTracker from './pages/InstanceTracker.jsx';
import Tasklist from './pages/Tasklist.jsx';
import ApprovedTeams from './pages/ApprovedTeams.jsx';
import ControlCenter from './pages/ControlCenter.jsx';
import { EmptyState } from './components/ui.jsx';

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<Dashboard />} />
        <Route path="events" element={<Events />} />
        <Route path="events/:id" element={<EventDetail />} />
        <Route path="register" element={<Register />} />
        <Route path="registrations" element={<Registrations />} />
        <Route path="registrations/:id" element={<RegistrationTracker />} />
        <Route path="instances/:key" element={<InstanceTracker />} />
        <Route path="tasks" element={<Tasklist />} />
        <Route path="tasks/:key" element={<Tasklist />} />
        <Route path="approved-teams" element={<ApprovedTeams />} />
        <Route path="control" element={<ControlCenter />} />
        <Route path="*" element={<EmptyState title="Page not found">Use the menu to continue.</EmptyState>} />
      </Route>
    </Routes>
  );
}
