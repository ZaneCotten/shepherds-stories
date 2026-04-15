import {useState, useEffect} from "react";
import {CommentSection} from "../components/CommentSection";

export const SupporterView = () => {
    const [inviteCode, setInviteCode] = useState("");
    const [successMessage, setSuccessMessage] = useState("");
    const [error, setError] = useState("");
    const [feed, setFeed] = useState([]);
    const [loadingFeed, setLoadingFeed] = useState(true);
    const [profile, setProfile] = useState(null);
    const [profilePictureLoading, setProfilePictureLoading] = useState(false);

    useEffect(() => {
        const fetchProfile = fetch("/api/profile")
            .then(res => {
                if (!res.ok) throw new Error("Failed to fetch profile");
                return res.json();
            })
            .then(data => setProfile(data))
            .catch(err => console.error("Error fetching profile:", err));

        const fetchFeed = fetch("/api/posts/feed")
            .then(res => {
                if (!res.ok) throw new Error("Failed to fetch feed");
                return res.json();
            })
            .then(data => {
                setFeed(data);
                setLoadingFeed(false);
            })
            .catch(() => {
                setLoadingFeed(false);
            });

        Promise.all([fetchProfile, fetchFeed]);
    }, []);

    const handleLogout = async () => {
        try {
            await fetch("/api/auth/logout", {method: 'POST'});
        } catch (err) {
            console.error("Logout error:", err);
        }
        localStorage.removeItem("user");
        window.location.href = "/home";
    };

    const handleSearch = async (e) => {
        e.preventDefault();
        setError("");
        setSuccessMessage("");

        try {
            const response = await fetch(`/api/supporter/send-request?code=${inviteCode}`, {
                method: 'POST'
            });
            if (response.ok) {
                const data = await response.json();
                setSuccessMessage(data.message || "Request sent!");
            } else if (response.status === 400) {
                const data = await response.json();
                setError(data.message || "Invalid request.");
            } else if (response.status === 403) {
                setError("Access denied. Are you logged in as a missionary? Only supporters can send requests.");
            } else {
                setError("Missionary not found. Please check the code.");
            }
        } catch {
            setError("Failed to send connection request.");
        }
    };

    const handleToggleLike = async (postId) => {
        try {
            const response = await fetch(`/api/posts/${postId}/like`, {
                method: 'POST'
            });
            if (response.ok) {
                const updatedPost = await response.json();
                setFeed(prevFeed => prevFeed.map(post => post.id === postId ? updatedPost : post));
            }
        } catch (err) {
            console.error("Error toggling like:", err);
        }
    };

    const handleProfilePictureChange = async (e) => {
        const file = e.target.files[0];
        if (!file) return;

        setProfilePictureLoading(true);
        try {
            // 1. Get Upload URL
            const urlParams = new URLSearchParams({contentType: file.type});
            const urlResponse = await fetch(`/api/profile/upload-url?${urlParams.toString()}`);
            if (!urlResponse.ok) throw new Error("Failed to get upload URL");
            const {uploadUrl, key} = await urlResponse.json();

            // 2. Upload to S3
            const uploadResponse = await fetch(uploadUrl, {
                method: 'PUT',
                body: file,
                headers: {'Content-Type': file.type}
            });
            if (!uploadResponse.ok) throw new Error("Failed to upload to S3");

            // 3. Update Profile in DB
            const updateResponse = await fetch("/api/profile/picture", {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({key})
            });

            if (updateResponse.ok) {
                const updatedProfile = await updateResponse.json();
                setProfile(prev => ({
                    ...prev,
                    profilePictureUrl: updatedProfile.profilePictureUrl
                }));
            } else {
                alert("Failed to update profile picture in database.");
            }
        } catch (err) {
            console.error("Profile picture upload error:", err);
            alert(`Error uploading profile picture: ${err.message}`);
        } finally {
            setProfilePictureLoading(false);
        }
    };

    return (
        <div style={{
            minHeight: "100vh",
            padding: "40px",
            display: "flex",
            flexDirection: "column",
            alignItems: "center",
            justifyContent: "flex-start",
            backgroundColor: "var(--bg-app)"
        }}>
            <h1 style={{color: "var(--text-h)", fontSize: "3rem", marginBottom: "20px"}}>
                Supporter Dashboard
            </h1>

            <div style={{
                position: "relative",
                marginBottom: "20px",
                display: "flex",
                flexDirection: "column",
                alignItems: "center"
            }}>
                <div style={{
                    width: "100px",
                    height: "100px",
                    borderRadius: "50%",
                    overflow: "hidden",
                    border: "3px solid var(--accent)",
                    backgroundColor: "var(--bg-card)",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    position: "relative"
                }}>
                    {profile?.profilePictureUrl ? (
                        <img
                            src={profile.profilePictureUrl}
                            alt="Profile"
                            style={{width: "100%", height: "100%", objectFit: "cover"}}
                        />
                    ) : (
                        <span style={{fontSize: "2.5rem", color: "var(--text-muted)"}}>
                            {profile?.displayName?.charAt(0) || "S"}
                        </span>
                    )}
                    {profilePictureLoading && (
                        <div style={{
                            position: "absolute",
                            inset: 0,
                            backgroundColor: "rgba(0,0,0,0.5)",
                            display: "flex",
                            alignItems: "center",
                            justifyContent: "center",
                            color: "white",
                            fontSize: "0.7rem"
                        }}>
                            ...
                        </div>
                    )}
                </div>
                <label style={{
                    marginTop: "10px",
                    color: "var(--accent)",
                    cursor: "pointer",
                    fontSize: "0.8rem",
                    fontWeight: "bold",
                    textDecoration: "underline"
                }}>
                    {profile?.profilePictureUrl ? "Change Photo" : "Upload Photo"}
                    <input
                        type="file"
                        accept="image/*"
                        onChange={handleProfilePictureChange}
                        style={{display: "none"}}
                    />
                </label>
            </div>

            <div style={{
                backgroundColor: "var(--bg-card)",
                padding: "30px",
                borderRadius: "12px",
                border: "1px solid var(--border-input)",
                width: "100%",
                maxWidth: "500px",
                textAlign: "center",
                marginBottom: "30px"
            }}>
                <h2 style={{color: "var(--text-h)", marginBottom: "20px"}}>Find a Missionary</h2>
                <form onSubmit={handleSearch} style={{display: "flex", gap: "10px", marginBottom: "20px"}}>
                    <input
                        type="text"
                        placeholder="Enter Invite Code"
                        value={inviteCode}
                        onChange={(e) => setInviteCode(e.target.value)}
                        style={{
                            flex: 1,
                            padding: "10px",
                            borderRadius: "8px",
                            border: "1px solid var(--border-input)",
                            backgroundColor: "var(--bg-input)",
                            color: "var(--text-h)"
                        }}
                    />
                    <button
                        type="submit"
                        style={{
                            padding: "10px 20px",
                            borderRadius: "8px",
                            backgroundColor: "var(--accent-primary)",
                            color: "white",
                            border: "none",
                            cursor: "pointer",
                            fontWeight: "bold"
                        }}
                    >
                        Connect
                    </button>
                </form>

                {error && <p style={{color: "red", marginTop: "10px"}}>{error}</p>}

                {successMessage && (
                    <div style={{
                        marginTop: "20px",
                        padding: "20px",
                        backgroundColor: "var(--bg-input)",
                        borderRadius: "8px",
                        border: "1px solid var(--accent-primary)"
                    }}>
                        <p style={{color: "var(--accent-primary)", fontSize: "1.2rem", fontWeight: "bold"}}>
                            {successMessage}
                        </p>
                    </div>
                )}
            </div>

            <div style={{
                width: "100%",
                maxWidth: "600px",
                marginBottom: "40px"
            }}>
                <h2 style={{color: "var(--text-h)", fontSize: "2rem", marginBottom: "20px", textAlign: "center"}}>
                    Missionary Updates
                </h2>
                {loadingFeed ? (
                    <p style={{textAlign: "center"}}>Loading updates...</p>
                ) : feed.length === 0 ? (
                    <p style={{textAlign: "center", color: "var(--text-muted)"}}>
                        No updates from your connected missionaries yet.
                    </p>
                ) : (
                    <div style={{display: "flex", flexDirection: "column", gap: "20px"}}>
                        {feed.map(post => (
                            <div key={post.id} style={{
                                backgroundColor: "var(--bg-card)",
                                padding: "20px",
                                borderRadius: "12px",
                                border: "1px solid var(--border-input)"
                            }}>
                                <div style={{
                                    display: "flex",
                                    justifyContent: "space-between",
                                    alignItems: "flex-start",
                                    marginBottom: "10px"
                                }}>
                                    <div style={{display: "flex", gap: "12px"}}>
                                        {post.profilePictureUrl ? (
                                            <img
                                                src={post.profilePictureUrl}
                                                alt=""
                                                style={{
                                                    width: "40px",
                                                    height: "40px",
                                                    borderRadius: "50%",
                                                    objectFit: "cover",
                                                    border: "2px solid var(--accent)"
                                                }}
                                            />
                                        ) : (
                                            <div style={{
                                                width: "40px",
                                                height: "40px",
                                                borderRadius: "50%",
                                                backgroundColor: "var(--bg-input)",
                                                border: "1px solid var(--border-input)",
                                                display: "flex",
                                                alignItems: "center",
                                                justifyContent: "center",
                                                fontSize: "1rem",
                                                color: "var(--text-muted)"
                                            }}>
                                                {post.authorName?.charAt(0) || "M"}
                                            </div>
                                        )}
                                        <div>
                                            {post.title &&
                                                <h3 style={{color: "var(--text-h)", margin: 0}}>{post.title}</h3>}
                                            <p style={{
                                                color: "var(--accent)",
                                                fontWeight: "bold",
                                                fontSize: "0.9rem",
                                                margin: "2px 0"
                                            }}>
                                                {post.authorName}
                                            </p>
                                        </div>
                                    </div>
                                    <div style={{textAlign: "right", color: "var(--text-muted)", fontSize: "0.8rem"}}>
                                        {new Date(post.createdAt).toLocaleString([], {
                                            dateStyle: 'short',
                                            timeStyle: 'short'
                                        })}
                                        {post.updatedAt && new Date(post.updatedAt).getTime() > new Date(post.createdAt).getTime() + 1000 && (
                                            <div style={{fontStyle: "italic", marginTop: "2px"}}>
                                                (Updated: {new Date(post.updatedAt).toLocaleString([], {
                                                dateStyle: 'short',
                                                timeStyle: 'short'
                                            })})
                                            </div>
                                        )}
                                    </div>
                                </div>
                                {post.media && post.media.length > 0 && (
                                    <div style={{
                                        display: "flex",
                                        flexDirection: "column",
                                        gap: "10px",
                                        marginBottom: "10px",
                                        marginTop: "10px"
                                    }}>
                                        {post.media.map(m => (
                                            <div key={m.id} style={{width: "100%"}}>
                                                {m.mediaType === "IMAGE" && (
                                                    <img src={m.url} alt={m.fileName} style={{
                                                        width: "100%",
                                                        borderRadius: "8px",
                                                        maxHeight: "300px",
                                                        objectFit: "cover"
                                                    }}/>
                                                )}
                                                {m.mediaType === "VIDEO" && (
                                                    <video controls src={m.url} style={{
                                                        width: "100%",
                                                        borderRadius: "8px",
                                                        maxHeight: "300px"
                                                    }}/>
                                                )}
                                                {m.mediaType === "AUDIO" && (
                                                    <audio controls src={m.url} style={{width: "100%"}}/>
                                                )}
                                                {m.mediaType === "DOCUMENT" && (
                                                    <a href={m.url} target="_blank" rel="noreferrer" style={{
                                                        display: "flex",
                                                        alignItems: "center",
                                                        gap: "10px",
                                                        padding: "10px",
                                                        backgroundColor: "var(--bg-input)",
                                                        borderRadius: "8px",
                                                        textDecoration: "none",
                                                        color: "var(--accent)"
                                                    }}>
                                                        📎 {m.fileName}
                                                    </a>
                                                )}
                                            </div>
                                        ))}
                                    </div>
                                )}
                                {post.content && (
                                    <p style={{
                                        color: "var(--text)",
                                        whiteSpace: "pre-wrap",
                                        marginBottom: "20px"
                                    }}>{post.content}</p>
                                )}

                                <CommentSection postId={post.id} postAuthorId={post.authorId}/>

                                <div style={{
                                    display: "flex",
                                    alignItems: "center",
                                    gap: "10px",
                                    marginTop: "10px",
                                    borderTop: "1px solid var(--border-input)",
                                    paddingTop: "10px"
                                }}>
                                    <button
                                        onClick={() => handleToggleLike(post.id)}
                                        style={{
                                            backgroundColor: post.liked ? "var(--accent-primary)" : "transparent",
                                            color: post.liked ? "white" : "var(--text-h)",
                                            border: "1px solid var(--border-input)",
                                            borderRadius: "8px",
                                            padding: "5px 15px",
                                            cursor: "pointer",
                                            fontWeight: "bold",
                                            display: "flex",
                                            alignItems: "center",
                                            gap: "5px",
                                            transition: "all 0.2s"
                                        }}
                                    >
                                        <span>{post.liked ? "❤️" : "🤍"}</span>
                                        <span>Like</span>
                                    </button>
                                    <span style={{color: "var(--text-muted)", fontSize: "0.9rem"}}>
                                        {post.lastLikerName ? (
                                            <>
                                                Liked by <strong>{post.lastLikerName}</strong>
                                                {post.likeCount > 1 && ` and ${post.likeCount - 1} more`}
                                            </>
                                        ) : (
                                            `${post.likeCount} ${post.likeCount === 1 ? "like" : "likes"}`
                                        )}
                                    </span>
                                </div>
                            </div>
                        ))}
                    </div>
                )}
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

export default SupporterView;