import React, { useState } from 'react';
import { Car, Lock, User, AlertCircle } from 'lucide-react';
import { login } from '../api/authService';

const Login = () => {
    // State to capture user input
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    
    // State to handle loading and errors
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState('');

    const handleLogin = async (e) => {
        e.preventDefault();
        setError('');
        setIsLoading(true);

        try {
            // This calls your Spring Boot backend via the axios service we built
            await login(username, password);
            
            // If successful, redirect to the dashboard
            // Note: We will build the Dashboard route next
            window.location.href = '/dashboard'; 
        } catch (err) {
            // Handle 401 Unauthorized or 403 Forbidden errors
            setError('Invalid username or password. Please try again.');
            console.error("Login attempt failed:", err);
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="min-h-screen bg-slate-900 flex items-center justify-center p-4">
            <div className="bg-white p-8 rounded-2xl shadow-2xl w-full max-w-md">
                
                {/* Logo and Header */}
                <div className="flex flex-col items-center mb-8">
                    <div className="bg-blue-100 p-3 rounded-full mb-3">
                        <Car size={40} className="text-blue-600" />
                    </div>
                    <h1 className="text-2xl font-bold text-slate-800">AutoHub Management</h1>
                    <p className="text-slate-500 text-sm">Sign in to manage spare parts</p>
                </div>

                {/* Error Alert */}
                {error && (
                    <div className="mb-4 p-3 bg-red-50 border border-red-200 text-red-600 rounded-lg flex items-center gap-2 text-sm">
                        <AlertCircle size={18} />
                        {error}
                    </div>
                )}

                <form onSubmit={handleLogin} className="space-y-5">
                    {/* Username Field */}
                    <div>
                        <label className="block text-sm font-semibold text-slate-700 mb-1">Username</label>
                        <div className="relative">
                            <User className="absolute left-3 top-3 text-slate-400" size={20} />
                            <input 
                                type="text" 
                                required
                                className="w-full pl-10 pr-4 py-3 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none transition disabled:bg-slate-50"
                                placeholder="admin@autohub.co.zw"
                                value={username}
                                onChange={(e) => setUsername(e.target.value)}
                                disabled={isLoading}
                            />
                        </div>
                    </div>

                    {/* Password Field */}
                    <div>
                        <label className="block text-sm font-semibold text-slate-700 mb-1">Password</label>
                        <div className="relative">
                            <Lock className="absolute left-3 top-3 text-slate-400" size={20} />
                            <input 
                                type="password" 
                                required
                                className="w-full pl-10 pr-4 py-3 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none transition disabled:bg-slate-50"
                                placeholder="••••••••"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                disabled={isLoading}
                            />
                        </div>
                    </div>

                    {/* Submit Button */}
                    <button 
                        type="submit"
                        disabled={isLoading}
                        className={`w-full font-bold py-3 rounded-lg shadow-lg transform transition active:scale-95 text-white 
                            ${isLoading ? 'bg-blue-400 cursor-not-allowed' : 'bg-blue-600 hover:bg-blue-700'}`}
                    >
                        {isLoading ? 'Authenticating...' : 'Access Dashboard'}
                    </button>
                </form>
            </div>
        </div>
    );
};

export default Login;