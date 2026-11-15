import { createContext, useContext, useEffect, useState } from 'react';

/**
 * Demo personas instead of a login: Student (by registration number), Faculty Advisor (by FA id, which is also
 * the Camunda task assignee) or Event Coordinator (candidate group 'event-coordinators').
 */
const PersonaContext = createContext(null);
const KEY = 'eventreg.persona';
const DEFAULT = { role: 'STUDENT', id: 'RA2411003010001', name: 'Aarav Sharma' };

function load() {
  try {
    return JSON.parse(localStorage.getItem(KEY)) || DEFAULT;
  } catch {
    return DEFAULT;
  }
}

export function PersonaProvider({ children }) {
  const [persona, setPersona] = useState(load);
  useEffect(() => {
    try { localStorage.setItem(KEY, JSON.stringify(persona)); } catch { /* storage unavailable */ }
  }, [persona]);
  return <PersonaContext.Provider value={{ persona, setPersona }}>{children}</PersonaContext.Provider>;
}

export const usePersona = () => useContext(PersonaContext);

export const ROLE_LABELS = {
  STUDENT: 'Student',
  FACULTY_ADVISOR: 'Faculty Advisor',
  EVENT_COORDINATOR: 'Event Coordinator',
};
