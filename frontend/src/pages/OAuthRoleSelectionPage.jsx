import React, {useMemo, useState} from "react";
import axios from "axios";
import {Link, useNavigate, useSearchParams} from "react-router-dom";

const OAuthRoleSelectionPage = ({onLogin}) => {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const [role, setRole] = useState("SUPPORTER");
    const [error, setError] = useState("");
    const [isLoading, setIsLoading] = useState(false);

    const email = useMemo(() => searchParams.get("email") || "", [searchParams]);
    const provider = useMemo(
        () => (searchParams.get("provider") || "GOOGLE").toUpperCase(),
        [searchParams]
    );
    const name = useMemo(() => searchParams.get("name") || "", [searchParams]);
    const givenName = useMemo(() => searchParams.get("given_name") || "", [searchParams]);
    const familyName = useMemo(() => searchParams.get("family_name") || "", [searchParams]);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError("");
        setIsLoading(true);

        try {
            const response = await axios.post("/api/auth/register-social", {
                email,
                role,
                authProvider: provider,
                displayName: name,
                firstName: givenName,
                lastName: familyName
            });

            const userData = {
                id: response.data.id,
                username: response.data.username,
                role: response.data.role
            };
            localStorage.setItem("user", JSON.stringify(userData));
            onLogin(userData);
            navigate(userData.role === "MISSIONARY" ? "/missionary" : "/supporter", {replace: true});
        } catch (err) {
            const body = err.response?.data;
            const message = typeof body === "string"
                ? body
                : body?.message || body?.error || "Social registration failed";
            setError(message);
        } finally {
            setIsLoading(false);
        }
    };

    if (!email) {
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
                        maxWidth: "460px",
                        padding: "28px",
                        backgroundColor: "var(--bg-card)",
                        color: "var(--text)",
                        borderRadius: "12px",
                        border: "1px solid var(--border)",
                        textAlign: "center"
                    }}
                >
                    <h2 style={{color: "var(--text-h)"}}>Missing OAuth Data</h2>
                    <p>We could not find your OAuth email. Start again from login.</p>
                    <Link to="/login" style={{color: "var(--accent)"}}>Back to Login</Link>
                </div>
            </div>
        );
    }

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
                    maxWidth: "460px",
                    padding: "28px",
                    backgroundColor: "var(--bg-card)",
                    color: "var(--text)",
                    borderRadius: "12px",
                    border: "1px solid var(--border)"
                }}
            >
                <h2 style={{color: "var(--text-h)", textAlign: "center"}}>Complete Your Sign Up</h2>
                <div style={{
                    marginBottom: "20px",
                    padding: "12px",
                    backgroundColor: "var(--bg-input)",
                    borderRadius: "8px",
                    border: "1px solid var(--border)"
                }}>
                    <p style={{margin: "4px 0"}}><strong>Email:</strong> {email}</p>
                    <p style={{margin: "4px 0"}}><strong>Provider:</strong> {provider}</p>
                </div>
                {error && <div style={{color: "var(--error)", marginBottom: "12px", textAlign: "center"}}>{error}</div>}
                <form onSubmit={handleSubmit}>
                    <label htmlFor="oauth-role" style={{color: "var(--text-h)", fontWeight: "bold"}}>Choose your
                        role</label>
                    <select
                        id="oauth-role"
                        value={role}
                        onChange={(e) => setRole(e.target.value)}
                        disabled={isLoading}
                        style={{
                            width: "100%",
                            marginTop: "10px",
                            padding: "12px",
                            borderRadius: "8px",
                            border: "1px solid var(--border-input)",
                            backgroundColor: "var(--bg-input)",
                            color: "var(--text-h)"
                        }}
                    >
                        <option value="SUPPORTER">Supporter</option>
                        <option value="MISSIONARY">Missionary</option>
                    </select>

                    <button
                        type="submit"
                        disabled={isLoading}
                        style={{
                            width: "100%",
                            marginTop: "20px",
                            padding: "12px",
                            borderRadius: "8px",
                            border: "none",
                            backgroundColor: "var(--primary)",
                            color: "white",
                            cursor: "pointer",
                            fontWeight: "bold"
                        }}
                    >
                        {isLoading ? "Completing..." : "Complete Sign Up"}
                    </button>
                </form>
                <div style={{marginTop: "16px", textAlign: "center"}}>
                    <Link to="/login" style={{color: "var(--accent)", textDecoration: "none"}}>Back to Login</Link>
                </div>
            </div>
        </div>
    );
};

export default OAuthRoleSelectionPage;
