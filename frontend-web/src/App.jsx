import React from 'react';
import { Routes, Route } from 'react-router-dom';
import HomePage from './pages/HomePage';

function App() {
  return (
    <Routes>
      <Route path="/" element={<HomePage />} />
      <Route
        path="*"
        element={
          <div className="min-h-screen flex items-center justify-center bg-quizlet-bg">
            <div className="text-center">
              <h2 className="text-4xl font-extrabold text-quizlet-textDark">404</h2>
              <p className="mt-2 text-quizlet-textMuted">Trang không tồn tại hoặc đang phát triển</p>
            </div>
          </div>
        }
      />
    </Routes>
  );
}

export default App;
