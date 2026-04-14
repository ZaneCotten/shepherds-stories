import {useEffect, useState} from "react";
import {CommentSection} from "../components/CommentSection";

export const MissionaryView = () => {
    const [profile, setProfile] = useState(null);
    const [requests, setRequests] = useState([]);
    const [posts, setPosts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [newPostTitle, setNewPostTitle] = useState("");
    const [newPostContent, setNewPostContent] = useState("");
    const [postLoading, setPostLoading] = useState(false);
    const [editingPost, setEditingPost] = useState(null);

    useEffect(() => {
        const fetchProfile = fetch("/api/missionary/profile").then(res => {
            if (!res.ok) throw new Error("Failed to fetch profile");
            return res.json();
        });

        const fetchRequests = fetch("/api/missionary/requests").then(res => {
            if (!res.ok) throw new Error("Failed to fetch requests");
            return res.json();
        });

        const fetchPosts = fetch("/api/posts").then(res => {
            if (!res.ok) throw new Error("Failed to fetch posts");
            return res.json();
        });

        Promise.all([fetchProfile, fetchRequests, fetchPosts])
            .then(([profileData, requestsData, postsData]) => {
                setProfile(profileData);
                setRequests(requestsData);
                setPosts(postsData);
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
        } catch {
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

    const handleGenerateNewCode = async () => {
        if (!window.confirm("Are you sure you want to generate a new invite code? The old one will no longer work.")) {
            return;
        }

        try {
            const response = await fetch("/api/missionary/profile/generate-code", {
                method: 'POST'
            });
            if (response.ok) {
                const data = await response.json();
                setProfile(prev => ({
                    ...prev,
                    referenceNumber: data.newCode,
                    isReferenceDisabled: false // Optimistically enable the reference code
                }));
            } else {
                const errorData = await response.json().catch(() => ({}));
                alert(`Failed to generate new code: ${errorData.error || response.statusText}`);
            }
        } catch (err) {
            alert(`Error generating new code: ${err.message}`);
        }
    };

    const handleLogout = async () => {
        try {
            await fetch("/api/auth/logout", {method: 'POST'});
        } catch (err) {
            console.error("Logout error:", err);
        }
        localStorage.removeItem("user");
        window.location.href = "/home";
    };

    const handleCreatePost = async (e) => {
        e.preventDefault();
        if (!newPostTitle || !newPostContent) return;

        setPostLoading(true);
        try {
            const url = editingPost ? `/api/posts/${editingPost.id}` : "/api/posts";
            const method = editingPost ? "PUT" : "POST";
            const response = await fetch(url, {
                method: method,
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    title: newPostTitle,
                    content: newPostContent
                })
            });

            if (response.ok) {
                const data = await response.json();
                if (editingPost) {
                    setPosts(posts.map(p => p.id === data.id ? data : p));
                } else {
                    setPosts([data, ...posts]);
                }
                setNewPostTitle("");
                setNewPostContent("");
                setEditingPost(null);
            } else {
                alert(`Failed to ${editingPost ? 'update' : 'create'} post.`);
            }
        } catch (err) {
            alert(`Error ${editingPost ? 'updating' : 'creating'} post: ${err.message}`);
        } finally {
            setPostLoading(false);
        }
    };

    const startEditing = (post) => {
        setEditingPost(post);
        setNewPostTitle(post.title);
        setNewPostContent(post.content);
        window.scrollTo({top: 0, behavior: 'smooth'});
    };

    const cancelEditing = () => {
        setEditingPost(null);
        setNewPostTitle("");
        setNewPostContent("");
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
                <div style={{display: "flex", gap: "10px", justifyContent: "center", marginTop: "15px"}}>
                    <button
                        onClick={handleToggleReference}
                        style={{
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
                    <button
                        onClick={handleGenerateNewCode}
                        style={{
                            padding: "5px 15px",
                            borderRadius: "6px",
                            backgroundColor: "transparent",
                            color: "var(--text-muted)",
                            border: "1px solid var(--border-input)",
                            cursor: "pointer",
                            fontSize: "0.8rem"
                        }}
                    >
                        Generate New Code
                    </button>
                </div>
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
                    {editingPost ? "Edit Update" : "Post an Update"}
                </h2>
                <form onSubmit={handleCreatePost} style={{display: "flex", flexDirection: "column", gap: "10px"}}>
                    <input
                        type="text"
                        placeholder="Title"
                        value={newPostTitle}
                        onChange={(e) => setNewPostTitle(e.target.value)}
                        required
                        style={{
                            padding: "10px",
                            borderRadius: "8px",
                            border: "1px solid var(--border-input)",
                            backgroundColor: "var(--bg-input)",
                            color: "var(--text-h)"
                        }}
                    />
                    <textarea
                        placeholder="Content"
                        value={newPostContent}
                        onChange={(e) => setNewPostContent(e.target.value)}
                        required
                        style={{
                            padding: "10px",
                            borderRadius: "8px",
                            border: "1px solid var(--border-input)",
                            backgroundColor: "var(--bg-input)",
                            color: "var(--text-h)",
                            minHeight: "100px",
                            resize: "vertical"
                        }}
                    />
                    <div style={{display: "flex", gap: "10px"}}>
                        <button
                            type="submit"
                            disabled={postLoading}
                            style={{
                                flex: 1,
                                padding: "10px 20px",
                                borderRadius: "8px",
                                backgroundColor: "var(--primary)",
                                color: "white",
                                border: "none",
                                cursor: "pointer",
                                fontWeight: "bold",
                                opacity: postLoading ? 0.6 : 1
                            }}
                        >
                            {postLoading ? (editingPost ? "Updating..." : "Posting...") : (editingPost ? "Update Post" : "Post Update")}
                        </button>
                        {editingPost && (
                            <button
                                type="button"
                                onClick={cancelEditing}
                                style={{
                                    padding: "10px 20px",
                                    borderRadius: "8px",
                                    backgroundColor: "var(--bg-input)",
                                    color: "var(--text-h)",
                                    border: "1px solid var(--border-input)",
                                    cursor: "pointer"
                                }}
                            >
                                Cancel
                            </button>
                        )}
                    </div>
                </form>
            </div>

            <div style={{
                width: "100%",
                maxWidth: "500px",
                marginBottom: "30px"
            }}>
                <h2 style={{color: "var(--text-h)", fontSize: "1.5rem", marginBottom: "15px", textAlign: "center"}}>
                    Your Updates
                </h2>
                {posts.length === 0 ? (
                    <p style={{textAlign: "center", color: "var(--text-muted)"}}>No updates yet.</p>
                ) : (
                    <div style={{display: "flex", flexDirection: "column", gap: "15px"}}>
                        {posts.map(post => (
                            <div key={post.id} style={{
                                backgroundColor: "var(--bg-card)",
                                padding: "15px",
                                borderRadius: "12px",
                                border: "1px solid var(--border-input)"
                            }}>
                                <h3 style={{color: "var(--text-h)", marginBottom: "5px"}}>{post.title}</h3>
                                <div style={{
                                    display: "flex",
                                    justifyContent: "space-between",
                                    alignItems: "center",
                                    marginBottom: "10px"
                                }}>
                                    <p style={{color: "var(--text-muted)", fontSize: "0.8rem"}}>
                                        {new Date(post.createdAt).toLocaleString([], {
                                            dateStyle: 'short',
                                            timeStyle: 'short'
                                        })}
                                        {post.updatedAt && new Date(post.updatedAt).getTime() > new Date(post.createdAt).getTime() + 1000 && (
                                            <span style={{marginLeft: "10px", fontStyle: "italic"}}>
                                                (Updated: {new Date(post.updatedAt).toLocaleString([], {
                                                dateStyle: 'short',
                                                timeStyle: 'short'
                                            })})
                                            </span>
                                        )}
                                    </p>
                                    <button
                                        onClick={() => startEditing(post)}
                                        style={{
                                            padding: "3px 8px",
                                            borderRadius: "4px",
                                            backgroundColor: "transparent",
                                            color: "var(--accent)",
                                            border: "1px solid var(--accent)",
                                            cursor: "pointer",
                                            fontSize: "0.75rem"
                                        }}
                                    >
                                        Edit
                                    </button>
                                </div>
                                <p style={{color: "var(--text)", whiteSpace: "pre-wrap"}}>{post.content}</p>
                                <CommentSection postId={post.id}/>
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

export default MissionaryView;