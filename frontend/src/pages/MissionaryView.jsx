import {useNavigate} from "react-router-dom";

export const MissionaryView = () => {

    const handleLogout = () => {
        localStorage.removeItem("user");
        window.location.href = "/home";
    };

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
            <p style={{color: "var(--text)", fontSize: "1.2rem", marginBottom: "30px"}}>
                Welcome to your missionary portal.
            </p>
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