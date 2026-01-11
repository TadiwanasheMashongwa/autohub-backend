import api from './axios';

export const login = async (username, password) => {
  // We call your Spring Boot endpoint
  const response = await api.post('/auth/login', { username, password });
  
  // If the backend sends a token, we save it in the browser's memory
  if (response.data.token) {
    localStorage.setItem('token', response.data.token);
  }
  
  return response.data;
};