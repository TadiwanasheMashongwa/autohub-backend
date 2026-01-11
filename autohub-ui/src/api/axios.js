import axios from 'axios';

// This creates a custom "Caller" for your backend
const api = axios.create({
  baseURL: '/api', // This points to your Vite Proxy
  headers: {
    'Content-Type': 'application/json'
  }
});

// This "Interceptor" automatically attaches your token 
// to every request once you are logged in.
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export default api;