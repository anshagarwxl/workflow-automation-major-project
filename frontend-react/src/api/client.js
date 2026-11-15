/** Thin fetch wrapper around the Spring Boot REST API. Errors carry the backend's message and field details. */
export class ApiError extends Error {
  constructor(message, status, details = []) {
    super(message);
    this.status = status;
    this.details = details;
  }
}

async function request(method, path, body) {
  const res = await fetch(`/api${path}`, {
    method,
    headers: body ? { 'Content-Type': 'application/json' } : undefined,
    body: body ? JSON.stringify(body) : undefined,
  });
  if (res.status === 204) return null;
  const text = await res.text();
  const data = text ? (res.headers.get('content-type')?.includes('json') ? JSON.parse(text) : text) : null;
  if (!res.ok) {
    throw new ApiError(data?.message || `Request failed (${res.status})`, res.status, data?.details || []);
  }
  return data;
}

const qs = (params) => {
  const p = Object.entries(params).filter(([, v]) => v !== undefined && v !== null && v !== '');
  return p.length ? `?${new URLSearchParams(p)}` : '';
};

export const api = {
  dashboard: () => request('GET', '/dashboard'),
  events: () => request('GET', '/events'),
  event: (id) => request('GET', `/events/${id}`),
  createEvent: (body) => request('POST', '/events', body),
  closeEvent: (id, actor) => request('POST', `/events/${id}/close${qs({ actor })}`),
  pool: (id) => request('GET', `/events/${id}/pool`),
  allocation: (id) => request('GET', `/events/${id}/allocation`),
  approvedTeams: (eventId) => request('GET', `/approved-teams${qs({ eventId })}`),

  students: (q) => request('GET', `/students${qs({ q })}`),
  student: (regNo) => request('GET', `/students/${encodeURIComponent(regNo)}`),
  advisors: () => request('GET', '/faculty-advisors'),

  submitRegistration: (body) => request('POST', '/registrations', body),
  registrations: (params = {}) => request('GET', `/registrations${qs(params)}`),
  registration: (id) => request('GET', `/registrations/${id}`),
  registrationTracker: (id) => request('GET', `/registrations/${id}/tracker`),
  instanceTracker: (key) => request('GET', `/process-instances/${key}/tracker`),
  instanceXml: (key) => request('GET', `/process-instances/${key}/xml`),

  tasks: (params) => request('GET', `/tasks${qs(params)}`),
  task: (key) => request('GET', `/tasks/${key}`),
  claimTask: (key, assignee) => request('POST', `/tasks/${key}/assignment`, { assignee }),
  unclaimTask: (key) => request('DELETE', `/tasks/${key}/assignment`),
  completeTask: (key, body) => request('POST', `/tasks/${key}/completion`, body),

  settings: () => request('GET', '/admin/settings'),
  updateSettings: (body) => request('PUT', '/admin/settings', body),
};
