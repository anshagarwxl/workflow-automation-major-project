import { Link } from 'react-router-dom';
import { Shuffle, User, Users } from 'lucide-react';
import { StatusBadge, EmptyState, timeAgo } from './ui.jsx';

export default function RegistrationTable({ registrations, showEvent = true }) {
  if (!registrations?.length) return <EmptyState title="No registrations">Nothing matches the current filters.</EmptyState>;
  return (
    <div className="overflow-x-auto">
      <table className="min-w-full divide-y divide-slate-100">
        <thead className="bg-slate-50">
          <tr>
            <th className="th">Registration</th>
            {showEvent && <th className="th">Event</th>}
            <th className="th">Members</th>
            <th className="th">Faculty Advisor</th>
            <th className="th">Status</th>
            <th className="th">Updated</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100">
          {registrations.map((r) => (
            <tr key={r.id} className="hover:bg-slate-50">
              <td className="td">
                <Link to={`/registrations/${r.id}`} className="flex items-center gap-2 font-medium text-brand-700 hover:underline">
                  {r.source === 'RANDOM' ? <Shuffle className="h-4 w-4 text-violet-500" /> : r.mode === 'TEAM' ? <Users className="h-4 w-4 text-slate-400" /> : <User className="h-4 w-4 text-slate-400" />}
                  {r.teamName || r.members[0]?.name}
                </Link>
                <p className="font-mono text-xs text-slate-400">{r.id}{r.teamCode && ` · ${r.teamCode}`}</p>
              </td>
              {showEvent && <td className="td">{r.eventName}</td>}
              <td className="td"><span className="text-slate-600">{r.members.map((m) => m.name.split(' ')[0]).join(', ')}</span></td>
              <td className="td">{r.facultyAdvisorName || '–'}</td>
              <td className="td"><StatusBadge status={r.status} />{r.statusReason && r.status !== 'APPROVED' && <p className="mt-1 max-w-xs truncate text-xs text-slate-500" title={r.statusReason}>{r.statusReason}</p>}</td>
              <td className="td whitespace-nowrap text-slate-500">{timeAgo(r.updatedAt)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
