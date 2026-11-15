import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import App from './App.jsx';
import { PersonaProvider } from './context/PersonaContext.jsx';
import { ToastProvider } from './context/ToastContext.jsx';
import 'bpmn-js/dist/assets/diagram-js.css';
import 'bpmn-js/dist/assets/bpmn-js.css';
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn.css';
import './index.css';

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <BrowserRouter>
      <ToastProvider>
        <PersonaProvider>
          <App />
        </PersonaProvider>
      </ToastProvider>
    </BrowserRouter>
  </React.StrictMode>,
);
