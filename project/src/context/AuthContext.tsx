import React, { createContext, ReactNode, useContext, useEffect, useState } from 'react';
import { clearAuthToken, getAuthToken, getCurrentUserProfile } from '../services/api';
import { UserProfile } from '../types';

interface AuthContextType {
  isAuthenticated: boolean;
  userProfile: UserProfile | null;
  userLabel: string | null;
  login: () => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

interface AuthProviderProps {
  children: ReactNode;
}

const buildUserLabel = (profile: UserProfile | null) => {
  if (!profile) {
    return null;
  }

  const fullName = profile.fullName?.trim();
  if (fullName) {
    return fullName;
  }

  return profile.email;
};

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(false);
  const [userProfile, setUserProfile] = useState<UserProfile | null>(null);
  const [userLabel, setUserLabel] = useState<string | null>(null);

  const loadCurrentUser = async () => {
    const profile = await getCurrentUserProfile();
    setUserProfile(profile);
    setUserLabel(buildUserLabel(profile));
  };

  useEffect(() => {
    const token = getAuthToken();
    if (!token) {
      setIsAuthenticated(false);
      setUserProfile(null);
      setUserLabel(null);
      return;
    }

    setIsAuthenticated(true);
    loadCurrentUser().catch(() => {
      clearAuthToken();
      setIsAuthenticated(false);
      setUserProfile(null);
      setUserLabel(null);
    });
  }, []);

  const login = async () => {
    setIsAuthenticated(true);
    await loadCurrentUser();
  };

  const logout = () => {
    clearAuthToken();
    setIsAuthenticated(false);
    setUserProfile(null);
    setUserLabel(null);
  };

  return (
    <AuthContext.Provider value={{ isAuthenticated, userProfile, userLabel, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
};
