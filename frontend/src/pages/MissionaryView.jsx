import {useEffect, useState} from "react";

export const MissionaryView = () => {
    const [profile, setProfile] = useState(null);
    const [requests, setRequests] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        const fetchProfile = fetch("/api/missionary/profile").then(res => {
            if (!res.ok) throw new Error("Failed to fetch profile");
            return res.json();
        });

        const fetchRequests = fetch("/api/missionary/requests").then(res => {
            if (!res.ok) throw new Error("Failed to fetch requests");
            return res.json();
        });

        Promise.all([fetchProfile, fetchRequests])
            .then(([profileData, requestsData]) => {
                setProfile(profileData);
                setRequests(requestsData);
                setLoading(false);
            })
            .catch(err => {
                setError(err.message);
                setLoading(false);
            });
    }, []);

    const handleRespond = async (requestId, approve) => {
        try {
            const response = await fetch(`/api/missionary/requests/${requestId}/respond?approve=${approve}`, {
                method: 'POST'
            });
            if (response.ok) {
                setRequests(requests.filter(req => req.id !== requestId));
            } else {
                alert("Failed to process request.");
            }
        } catch (err) {
            alert("Error responding to request.");
        }
    };

    const handleToggleReference = async () => {
        try {
            const response = await fetch("/api/missionary/profile/toggle-reference", {
                method: 'POST'
            });
            if (response.ok) {
                const data = await response.json();
                setProfile(prev => ({...prev, isReferenceDisabled: data.isDisabled}));
            } else {
                const errorData = await response.json().catch(() => ({}));
                alert(`Failed to toggle reference status: ${errorData.error || response.statusText}`);
            }
        } catch (err) {
            alert(`Error toggling reference status: ${err.message}`);
        }
    };

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
                    color: profile?.isReferenceDisabled ? "var(--text-muted)" : "var(--accent)",
                    fontSize: "1.5rem",
                    fontWeight: "bold",
                    letterSpacing: "2px",
                    fontFamily: "monospace",
                    opacity: profile?.isReferenceDisabled ? 0.4 : 1
                }}>
                    {profile?.referenceNumber}
                </p>
                <button
                    onClick={handleToggleReference}
                    style={{
                        marginTop: "15px",
                        padding: "5px 15px",
                        borderRadius: "6px",
                        backgroundColor: profile?.isReferenceDisabled ? "var(--accent)" : "transparent",
                        color: profile?.isReferenceDisabled ? "white" : "var(--text-muted)",
                        border: profile?.isReferenceDisabled ? "none" : "1px solid var(--border-input)",
                        cursor: "pointer",
                        fontSize: "0.8rem"
                    }}
                >
                    {profile?.isReferenceDisabled ? "Enable Invite Code" : "Disable Invite Code"}
                </button>
            </div>

            {requests.length > 0 && (
                <div style={{
                    width: "100%",
                    maxWidth: "500px",
                    backgroundColor: "var(--bg-card)",
                    padding: "20px",
                    borderRadius: "12px",
                    border: "1px solid var(--border-input)",
                    marginBottom: "30px"
                }}>
                    <h2 style={{color: "var(--text-h)", fontSize: "1.5rem", marginBottom: "15px", textAlign: "center"}}>
                        Connection Requests
                    </h2>
                    <div style={{display: "flex", flexDirection: "column", gap: "10px"}}>
                        {requests.map(req => (
                            <div key={req.id} style={{
                                display: "flex",
                                justifyContent: "space-between",
                                alignItems: "center",
                                padding: "10px",
                                backgroundColor: "var(--bg-input)",
                                borderRadius: "8px",
                                border: "1px solid var(--border-input)"
                            }}>
                                <span style={{color: "var(--text)", fontWeight: "bold"}}>{req.supporterName}</span>
                                <div style={{display: "flex", gap: "10px"}}>
                                    <button
                                        onClick={() => handleRespond(req.id, true)}
                                        style={{
                                            padding: "5px 10px",
                                            borderRadius: "4px",
                                            backgroundColor: "green",
                                            color: "white",
                                            border: "none",
                                            cursor: "pointer",
                                            fontSize: "0.9rem"
                                        }}
                                    >
                                        Approve
                                    </button>
                                    <button
                                        onClick={() => handleRespond(req.id, false)}
                                        style={{
                                            padding: "5px 10px",
                                            borderRadius: "4px",
                                            backgroundColor: "red",
                                            color: "white",
                                            border: "none",
                                            cursor: "pointer",
                                            fontSize: "0.9rem"
                                        }}
                                    >
                                        Deny
                                    </button>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            )}

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