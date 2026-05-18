import { useState, useCallback } from 'react';
import type { LoginResponse } from '../types';

const TOKEN_KEY = 'token';
const USER_KEY = 'userInfo';

export function useAuth() {
  const [userInfo, setUserInfo] = useState<LoginResponse | null>(() => {
    const stored = localStorage.getItem(USER_KEY);
    return stored ? JSON.parse(stored) : null;
  });

  const saveAuth = useCallback((data: LoginResponse) => {
    localStorage.setItem(TOKEN_KEY, data.token);
    localStorage.setItem(USER_KEY, JSON.stringify(data));
    setUserInfo(data);
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    setUserInfo(null);
  }, []);

  const isAdmin = userInfo?.role === 'ADMIN';
  const isLeader = userInfo?.role === 'LEADER';

  return { userInfo, saveAuth, logout, isAdmin, isLeader };
}
