import React, {useState} from 'react';
import {BrowserRouter as Router, Routes, Route, Navigate} from 'react-router-dom';
import LoginPage from './pages/LoginPage.jsx';
import RegisterPage from './pages/RegisterPage.jsx';
import HomePage from './pages/HomePage.jsx';
import MissionaryView from "./pages/MissionaryView.jsx";
import SupporterView from "./pages/SupporterView.jsx";
import OAuthRoleSelectionPage from "./pages/OAuthRoleSelectionPage.jsx";
import OAuthCallbackPage from "./pages/OAuthCallbackPage.jsx";

function App() {
    const [user, setUser] = useState(() => {
        const saved = localStorage.getItem("user");
        return saved ? JSON.parse(saved) : null;
    });

    const handleLogin = (nextUser) => {
        if (nextUser) {
            localStorage.setItem("user", JSON.stringify(nextUser));
            setUser(nextUser);
            return;
        }
        localStorage.removeItem("user");
        setUser(null);
    };


    return (
        <Router>
            <Routes>
                {/* Public Routes */}
                <Route path="/login" element={<LoginPage onLogin={handleLogin}/>}/>
                <Route path="/register" element={<RegisterPage onLogin={handleLogin}/>}/>
                <Route path="/register/select-role" element={<OAuthRoleSelectionPage onLogin={handleLogin}/>}/>
                <Route path="/oauth/callback" element={<OAuthCallbackPage onLogin={handleLogin}/>}/>
                <Route path="/home" element={<HomePage/>}/>

                {/* Route Protection based on Role */}
                <Route
                    path="/missionary/*"
                    element={user?.role === 'MISSIONARY' ? <MissionaryView/> : <Navigate to="/login"/>}
                />

                <Route
                    path="/supporter/*"
                    element={user?.role === 'SUPPORTER' ? <SupporterView/> : <Navigate to="/login"/>}
                />

                {/* Default Redirect */}
                <Route path="*" element={<Navigate to="/home"/>}/>
            </Routes>
        </Router>
    );
}

export default App;
