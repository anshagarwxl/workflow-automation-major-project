import { useEffect, useState } from 'react';
import { NavLink, Outlet } from 'react-router-dom';
import clsx from 'clsx';
import {
  LayoutDashboard, CalendarDays, UserPlus, ListChecks, Inbox, Trophy, SlidersHorizontal, Workflow, Menu, X,
} from 'lucide-react';
import { api } from '../api/client.js';
import { usePersona, ROLE_LABELS } from '../context/PersonaContext.jsx';

const NAV = [
  { to: '/', label: 'Dashboard', icon: LayoutDashboard, roles: ['STUDENT', 'FACULTY_ADVISOR', 'EVENT_COORDINATOR'] },
  { to: '/events', label: 'Events', icon: CalendarDays, roles: ['STUDENT', 'FACULTY_ADVISOR', 'EVENT_COORDINATOR'] },
  { to: '/register', label: 'Register', icon: UserPlus, roles: ['STUDENT'] },
  { to: '/registrations', label: 'Registrations', studentLabel: 'My registrations', icon: ListChecks, roles: ['STUDENT', 'FACULTY_ADVISOR', 'EVENT_COORDINATOR'] },
  { to: '/tasks', label: 'Tasklist', icon: Inbox, roles: ['FACULTY_ADVISOR', 'EVENT_COORDINATOR'] },
  { to: '/approved-teams', label: 'Approved teams', icon: Trophy, roles: ['STUDENT', 'FACULTY_ADVISOR', 'EVENT_COORDINATOR'] },
  { to: '/control', label: 'Control center', icon: SlidersHorizontal, roles: ['EVENT_COORDINATOR'] },
];

function PersonaSwitcher() {
  const { persona, setPersona } = usePersona();
  const [advisors, setAdvisors] = useState([]);
  const [students, setStudents] = useState([]);
  useEffect(() => {
    api.advisors().then(setAdvisors).catch(() => {});
    api.students('').then(setStudents).catch(() => {});
  }, []);

  const changeRole = (role) => {
    if (role === 'STUDENT') setPersona({ role, id: students[0]?.regNo || 'RA2411003010001', name: students[0]?.name || 'Student' });
    if (role === 'FACULTY_ADVISOR') setPersona({ role, id: advisors[0]?.id || 'FA-CSE-01', name: advisors[0]?.name || 'Faculty Advisor' });
    if (role === 'EVENT_COORDINATOR') setPersona({ role, id: 'coordinator', name: 'Event Coordination Cell' });
  };

  return (
    <div className="flex flex-wrap items-center gap-2">
      <span className="hidden text-xs font-medium text-slate-500 sm:inline">Acting as</span>
      <select className="input !w-auto !py-1.5" value={persona.role} onChange={(e) => changeRole(e.target.value)} aria-label="Role">
        {Object.entries(ROLE_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
      </select>
      {persona.role === 'STUDENT' && (
        <select className="input !w-56 !py-1.5" value={persona.id} aria-label="Student"
          onChange={(e) => { const s = students.find((x) => x.regNo === e.target.value); setPersona({ role: 'STUDENT', id: s.regNo, name: s.name }); }}>
          {students.map((s) => <option key={s.regNo} value={s.regNo}>{s.name} · {s.regNo.slice(-4)}</option>)}
        </select>
      )}
      {persona.role === 'FACULTY_ADVISOR' && (
        <select className="input !w-56 !py-1.5" value={persona.id} aria-label="Faculty Advisor"
          onChange={(e) => { const a = advisors.find((x) => x.id === e.target.value); setPersona({ role: 'FACULTY_ADVISOR', id: a.id, name: a.name }); }}>
          {advisors.map((a) => <option key={a.id} value={a.id}>{a.name} ({a.id})</option>)}
        </select>
      )}
    </div>
  );
}

export default function Layout() {
  const { persona } = usePersona();
  const [open, setOpen] = useState(false);
  const items = NAV.filter((n) => n.roles.includes(persona.role));

  const nav = (
    <nav className="flex flex-col gap-0.5">
      {items.map(({ to, label, studentLabel, icon: Icon }) => (
        <NavLink key={to} to={to} end={to === '/'} onClick={() => setOpen(false)}
          className={({ isActive }) => clsx('flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition',
            isActive ? 'bg-brand-50 text-brand-700' : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900')}>
          <Icon className="h-4 w-4" />
          {persona.role === 'STUDENT' && studentLabel ? studentLabel : label}
        </NavLink>
      ))}
    </nav>
  );

  return (
    <div className="min-h-screen">
      <aside className={clsx('fixed inset-y-0 left-0 z-40 w-64 border-r border-slate-200 bg-white px-4 py-5 transition-transform lg:translate-x-0',
        open ? 'translate-x-0' : '-translate-x-full')}>
        <div className="mb-8 flex items-center gap-2.5 px-2">
          <div className="rounded-lg bg-brand-600 p-1.5"><Workflow className="h-5 w-5 text-white" /></div>
          <div>
            <p className="text-sm font-semibold leading-tight text-slate-900">Event Team</p>
            <p className="text-sm font-semibold leading-tight text-slate-900">Registration</p>
          </div>
          <button className="ml-auto lg:hidden" onClick={() => setOpen(false)} aria-label="Close menu"><X className="h-5 w-5" /></button>
        </div>
        {nav}
        <div className="absolute inset-x-4 bottom-5 rounded-lg bg-slate-50 p-3 text-xs text-slate-500">
          Powered by <span className="font-medium text-slate-700">Camunda 8</span>, Spring Boot and React.
        </div>
      </aside>
      {open && <div className="fixed inset-0 z-30 bg-slate-900/30 lg:hidden" onClick={() => setOpen(false)} />}
      <div className="lg:pl-64">
        <header className="sticky top-0 z-20 flex flex-wrap items-center gap-3 border-b border-slate-200 bg-white/90 px-4 py-3 backdrop-blur sm:px-6">
          <button className="lg:hidden" onClick={() => setOpen(true)} aria-label="Open menu"><Menu className="h-5 w-5" /></button>
          <div className="ml-auto flex items-center gap-3">
            <PersonaSwitcher />
            <div className="hidden items-center gap-2 border-l border-slate-200 pl-3 md:flex">
              <div className="flex h-8 w-8 items-center justify-center rounded-full bg-brand-100 text-xs font-semibold text-brand-700">
                {persona.name?.split(' ').map((p) => p[0]).slice(0, 2).join('')}
              </div>
              <div className="leading-tight">
                <p className="text-sm font-medium text-slate-900">{persona.name}</p>
                <p className="text-xs text-slate-500">{ROLE_LABELS[persona.role]}</p>
              </div>
            </div>
          </div>
        </header>
        <main className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
