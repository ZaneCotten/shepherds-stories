import React, {useEffect} from "react";
import {useNavigate, useSearchParams} from "react-router-dom";

const OAuthCallbackPage = ({onLogin}) => {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();

    useEffect(() => {
        const username = (searchParams.get("username") || "").trim();
        const role = (searchParams.get("role") || "").trim();
        const normalizedRole = role.replace("ROLE_", "").trim().toUpperCase();

        if (normalizedRole === "MISSIONARY" || normalizedRole === "SUPPORTER") {
            const userData = {username, role: normalizedRole};
            localStorage.setItem("user", JSON.stringify(userData));
            // Full page reload ensures App.jsx initializes from localStorage correctly
            window.location.replace(normalizedRole === "MISSIONARY" ? "/missionary" : "/supporter");
            return;
        }

        navigate("/login", {replace: true});
    }, [onLogin, searchParams, navigate]);

    return (
        <div style={{
            minHeight: "100vh",
            padding: "40px 16px",
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
        }}>
            <div
                style={{
                    width: "100%",
                    maxWidth: "420px",
                    padding: "28px",
                    backgroundColor: "var(--bg-card)",
                    color: "var(--text)",
                    borderRadius: "12px",
                    border: "1px solid var(--border)",
                    textAlign: "center"
                }}
            >
                <h2 style={{color: "var(--text-h)"}}>Completing sign in...</h2>
            </div>
        </div>
    );
};

export default OAuthCallbackPage;
