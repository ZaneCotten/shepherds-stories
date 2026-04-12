import React, {useState} from 'react';
import {BrowserRouter as Router, Routes, Route, Navigate} from 'react-router-dom';
import LoginPage from './pages/LoginPage.jsx';
import RegisterPage from './pages/RegisterPage.jsx';
import HomePage from './pages/HomePage.jsx';
import MissionaryView from "./pages/MissionaryView.jsx";
import SupporterView from "./pages/SupporterView.jsx";

function App() {
    const [user, setUser] = useState(null);

    return (
        <Router>
            <Routes>
                {/* Public Routes */}
                <Route path="/login" element={<LoginPage onLogin={setUser}/>}/>
                <Route path="/register" element={<RegisterPage/>}/>
                <Route path="/home" element={<HomePage/>}/>

                {/* Route Protection based on Role */}
                <Route
                    path="/missionary/*"
                    element={user?.role === 'ROLE_MISSIONARY' ? <MissionaryView/> : <Navigate to="/login"/>}
                />

                <Route
                    path="/supporter/*"
                    element={user?.role === 'ROLE_SUPPORTER' ? <SupporterView/> : <Navigate to="/login"/>}
                />

                {/* Default Redirect */}
                <Route path="*" element={<Navigate to="/home"/>}/>
            </Routes>
        </Router>
    );
}

export default App;