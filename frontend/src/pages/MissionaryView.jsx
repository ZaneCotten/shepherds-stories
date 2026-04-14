import {useEffect, useState} from "react";
import {useNavigate} from "react-router-dom";

export const MissionaryView = () => {
    const [profile, setProfile] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        fetch("/api/missionary/profile")
            .then(res => {
                if (!res.ok) {
                    throw new Error("Failed to fetch profile");
                }
                return res.json();
            })
            .then(data => {
                setProfile(data);
                setLoading(false);
            })
            .catch(err => {
                setError(err.message);
                setLoading(false);
            });
    }, []);

    const handleLogout = () => {
        localStorage.removeItem("user");
        window.location.href = "/home";
    };

    if (loading) return <div style={{padding: "40px", textAlign: "center"}}>Loading...</div>;
    if (error) return <div style={{padding: "40px", textAlign: "center", color: "red"}}>Error: {error}</div>;

    return (
        <div style={{
            minHeight: "100vh",
            padding: "40px",
            display: "flex",
            flexDirection: "column",
            alignItems: "center",
            justifyContent: "center"
        }}>
            <h1 style={{color: "var(--text-h)", fontSize: "3rem", marginBottom: "20px"}}>
                Missionary Dashboard
            </h1>
            <p style={{color: "var(--text)", fontSize: "1.2rem", marginBottom: "10px"}}>
                Welcome, {profile?.missionaryName || "Missionary"}.
            </p>
            <div style={{
                backgroundColor: "var(--bg-card)",
                padding: "20px",
                borderRadius: "12px",
                border: "1px solid var(--border-input)",
                marginBottom: "30px",
                textAlign: "center"
            }}>
                <p style={{color: "var(--text-muted)", fontSize: "1rem", marginBottom: "5px"}}>Your Invite Code</p>
                <p style={{
                    color: "var(--accent)",
                    fontSize: "1.5rem",
                    fontWeight: "bold",
                    letterSpacing: "2px",
                    fontFamily: "monospace"
                }}>
                    {profile?.referenceNumber}
                </p>
            </div>
            <button
                onClick={handleLogout}
                style={{
                    padding: "10px 20px",
                    borderRadius: "8px",
                    backgroundColor: "var(--bg-input)",
                    color: "var(--text-h)",
                    border: "1px solid var(--border-input)",
                    cursor: "pointer"
                }}
            >
                Logout
            </button>
        </div>
    );
};

export default MissionaryView;