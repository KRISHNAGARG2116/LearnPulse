import React, { createContext, useState, useEffect, ReactNode } from 'react';
import { User, AuthResponse, LoginRequest, RegisterRequest, ApiResponse } from '../types/auth';
import apiClient from '../api/axios';

interface AuthContextType {
  user: User | null;
  accessToken: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (data: LoginRequest) => Promise<AuthResponse>;
  register: (data: RegisterRequest) => Promise<AuthResponse>;
  logout: () => void;
}

export const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [accessToken, setAccessToken] = useState<String | null>(null);
  const [refreshToken, setRefreshToken] = useState<String | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  useEffect(() => {
    // Load persisted auth session on mount
    const storedToken = localStorage.getItem('accessToken');
    const storedRefreshToken = localStorage.getItem('refreshToken');
    const storedUserJson = localStorage.getItem('user');

    if (storedToken && storedUserJson) {
      try {
        const parsedUser = JSON.parse(storedUserJson);
        setUser(parsedUser);
        setAccessToken(storedToken);
        setRefreshToken(storedRefreshToken);
      } catch {
        localStorage.clear();
      }
    }
    setIsLoading(false);
  }, []);

  const handleAuthSuccess = (authData: AuthResponse): User => {
    const userObj: User = {
      userId: authData.userId,
      email: authData.email,
      firstName: authData.firstName,
      lastName: authData.lastName,
      role: authData.role,
    };

    localStorage.setItem('accessToken', authData.accessToken);
    localStorage.setItem('refreshToken', authData.refreshToken);
    localStorage.setItem('user', JSON.stringify(userObj));

    setUser(userObj);
    setAccessToken(authData.accessToken);
    setRefreshToken(authData.refreshToken);

    return userObj;
  };

  const login = async (data: LoginRequest): Promise<AuthResponse> => {
    const response = await apiClient.post<ApiResponse<AuthResponse>>('/auth/login', data);
    const authData = response.data.data;
    handleAuthSuccess(authData);
    return authData;
  };

  const register = async (data: RegisterRequest): Promise<AuthResponse> => {
    const response = await apiClient.post<ApiResponse<AuthResponse>>('/auth/register', data);
    const authData = response.data.data;
    handleAuthSuccess(authData);
    return authData;
  };

  const logout = () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
    setUser(null);
    setAccessToken(null);
    setRefreshToken(null);
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        accessToken: accessToken as string | null,
        refreshToken: refreshToken as string | null,
        isAuthenticated: !!user && !!accessToken,
        isLoading,
        login,
        register,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};
