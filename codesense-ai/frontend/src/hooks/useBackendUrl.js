import { useAuth } from '../context/AuthContext';
import { useEffect, useState } from 'react';

const BACKEND_URL = import.meta.env.VITE_API_BASE_URL || 'https://codesense-ai-tuo7.onrender.com';

export function useAuthContext() {
  const ctx = useAuth();
  return ctx;
}
