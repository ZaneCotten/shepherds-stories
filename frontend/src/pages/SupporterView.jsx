import {useState, useEffect} from "react";
import {CommentSection} from "../components/CommentSection";

export const SupporterView = () => {
    const [inviteCode, setInviteCode] = useState("");
    const [successMessage, setSuccessMessage] = useState("");
    const [error, setError] = useState("");
    const [feed, setFeed] = useState([]);
    const [loadingFeed, setLoadingFeed] = useState(true);
    const [missionaries, setMissionaries] = useState([]);
    const [selectedMissionary, setSelectedMissionary] = useState("");
    const [profile, setProfile] = useState(null);
    const [isUploading, setIsUploading] = useState(false);
    const [prayerRequests, setPrayerRequests] = useState([]);

    useEffect(() => {
        const fetchProfile = async () => {
            try {
                const res = await fetch("/api/supporter/profile");
                if (res.ok) {
                    const data = await res.json();
                    setProfile(data);
                }
            } catch (err) {
                console.error("Error fetching supporter profile:", err);
            }
        };
        fetchProfile();
    }, []);

    useEffect(() => {
        const fetchMissionaries = async () => {
            try {
                const res = await fetch("/api/supporter/missionaries");
                if (res.ok) {
                    const data = await res.json();
                    setMissionaries(data);
                }
            } catch (err) {
                console.error("Error fetching missionaries:", err);
            }
        };
        fetchMissionaries();
    }, []);

    useEffect(() => {
        setLoadingFeed(true);
        const url = selectedMissionary
            ? `/api/posts/feed?missionaryId=${selectedMissionary}`
            : "/api/posts/feed";

        fetch(url)
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
    }, [selectedMissionary]);

    useEffect(() => {
        const fetchPrayerRequests = async () => {
            try {
                const res = await fetch("/api/supporter/prayer-requests");
                if (res.ok) {
                    const data = await res.json();
                    setPrayerRequests(data);
                }
            } catch (err) {
                console.error("Error fetching prayer requests:", err);
            }
        };
        fetchPrayerRequests();
    }, [missionaries]); // Re-fetch when connections change

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
                // Refresh missionary list to include the newly connected one (if approved instantly, though usually pending)
                const res = await fetch("/api/supporter/missionaries");
                if (res.ok) {
                    const missionariesData = await res.json();
                    setMissionaries(missionariesData);
                }
            } else if (response.status === 400) {
                const data = await response.json();
                setError(data.message || "Invalid request.");
            } else if (response.status === 401) {
                setError("Your session has expired. Please log in again.");
            } else if (response.status === 403) {
                setError("Access denied. Are you logged in as a missionary? Only supporters can send requests.");
            } else {
                setError("Missionary not found. Please check the code.");
            }
        } catch {
            setError("Failed to send connection request.");
        }
    };

    const handleFileChange = async (e) => {
        const file = e.target.files[0];
        if (!file) return;

        setIsUploading(true);
        try {
            // 1. Get upload URL
            const urlRes = await fetch(`/api/supporter/profile/picture/upload-url?contentType=${encodeURIComponent(file.type)}`, {
                method: "POST"
            });
            const {uploadUrl, s3Key} = await urlRes.json();

            // 2. Upload to S3
            await fetch(uploadUrl, {
                method: "PUT",
                body: file,
                headers: {"Content-Type": file.type}
            });

            // 3. Update profile with S3 key
            const updateRes = await fetch("/api/supporter/profile", {
                method: "PUT",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify({profilePictureUrl: s3Key})
            });

            if (updateRes.ok) {
                const updatedProfile = await updateRes.json();
                setProfile(updatedProfile);
            }
        } catch (err) {
            console.error("Upload failed", err);
            setError("Failed to upload profile picture: " + err.message);
        } finally {
            setIsUploading(false);
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

    return (
        <div className="bg-linear-to-r from-white to-accent-light-green min-h-screen">
            <div className="max-w-7xl mx-auto px-4 py-8 flex flex-col lg:flex-row-reverse gap-8">
                {/* Prayer Requests Side Bar */}
                <div className="w-full lg:w-80 flex-shrink-0">
                    <div className="lg:sticky lg:top-8 space-y-4">
                        <div className="bg-white p-6 rounded-2xl border border-accent-mid-green shadow-sm">
                            <h2 className="text-xl font-bold text-accent-dark-green mb-4 flex items-center gap-2">
                                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24"
                                     fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"
                                     strokeLinejoin="round">
                                    <path
                                        d="M12 21l-8.205-8.205a5.8 5.8 0 0 1 0-8.205 5.8 5.8 0 0 1 8.205 0l.92.92.92-.92a5.8 5.8 0 0 1 8.205 0 5.8 5.8 0 0 1 0 8.205L12 21z"/>
                                </svg>
                                Prayer Requests
                            </h2>
                            <div className="space-y-4 max-h-[70vh] overflow-y-auto pr-2 custom-scrollbar">
                                {prayerRequests.length > 0 ? (
                                    prayerRequests.map(req => (
                                        <div key={req.id}
                                             className="border-b border-gray-100 last:border-0 pb-4 last:pb-0">
                                            <div className="flex justify-between items-start mb-1">
                                                <h4 className="font-bold text-accent-dark-green text-sm">{req.title}</h4>
                                                <span
                                                    className="text-[9px] font-bold text-gray-400 uppercase">{req.missionaryName}</span>
                                            </div>
                                            <p className="text-xs text-gray-600 line-clamp-3 hover:line-clamp-none transition-all cursor-default">{req.content}</p>
                                        </div>
                                    ))
                                ) : (
                                    <p className="text-sm italic text-gray-400 text-center py-4">No active prayer
                                        requests.</p>
                                )}
                            </div>
                        </div>
                    </div>
                </div>

                {/* Main Content */}
                <div className="flex-1 flex flex-col items-center">
                    <div className="w-full max-w-4xl flex justify-between items-center mb-12">
                        <div className="flex items-center gap-4 text-left">
                            <div className="relative group">
                                <div
                                    className="w-16 h-16 rounded-full overflow-hidden border-2 border-accent-mid-green bg-white flex items-center justify-center shadow-sm">
                                    {profile?.profilePictureUrl ? (
                                        <img src={profile.profilePictureUrl} alt="Profile"
                                             className="w-full h-full object-cover"/>
                                    ) : (
                                        <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32"
                                             viewBox="0 0 24 24"
                                             fill="none" stroke="#2D5A27" strokeWidth="2" strokeLinecap="round"
                                             strokeLinejoin="round">
                                            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                                            <circle cx="12" cy="7" r="4"></circle>
                                        </svg>
                                    )}
                                    {isUploading && (
                                        <div className="absolute inset-0 bg-black/40 flex items-center justify-center">
                                            <div
                                                className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                                        </div>
                                    )}
                                </div>
                                <label
                                    className="absolute -bottom-1 -right-1 bg-white rounded-full p-1 border border-accent-mid-green cursor-pointer shadow-sm hover:bg-accent-light-green transition-colors">
                                    <input type="file" className="hidden" accept="image/*" onChange={handleFileChange}
                                           disabled={isUploading}/>
                                    <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24"
                                         fill="none" stroke="#2D5A27" strokeWidth="3" strokeLinecap="round"
                                         strokeLinejoin="round">
                                        <path
                                            d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"></path>
                                        <circle cx="12" cy="13" r="4"></circle>
                                    </svg>
                                </label>
                            </div>
                            <div>
                                <h1 className="text-3xl font-bold text-accent-dark-green">Supporter Dashboard</h1>
                                <p className="text-accent-mid-green font-medium">
                                    Welcome, {profile ? `${profile.firstName} ${profile.lastName}` : "Supporter"}
                                </p>
                                {error && error.includes("upload") &&
                                    <p className="text-red-500 text-xs font-bold mt-1">{error}</p>}
                            </div>
                        </div>
                        <button
                            onClick={handleLogout}
                            className="px-4 py-2 rounded-full border border-accent-mid-green text-accent-mid-green text-sm font-bold hover:bg-accent-mid-green hover:text-white transition-all duration-200"
                        >
                            Logout
                        </button>
                    </div>

                    {missionaries.length > 0 && (
                        <div className="w-full max-w-4xl mb-8">
                            <div className="flex flex-wrap justify-center gap-2 p-1 bg-gray-100 rounded-xl">
                                <button
                                    onClick={() => setSelectedMissionary("")}
                                    className={`px-4 py-2 rounded-lg text-sm font-bold transition-all ${!selectedMissionary ? 'bg-white text-accent-dark-green shadow-sm' : 'text-gray-500 hover:text-gray-700'}`}
                                >
                                    All Updates
                                </button>
                                {missionaries.map(m => (
                                    <button
                                        key={m.id}
                                        onClick={() => setSelectedMissionary(m.id)}
                                        className={`px-4 py-2 rounded-lg text-sm font-bold transition-all flex items-center gap-2 ${selectedMissionary === m.id ? 'bg-white text-accent-dark-green shadow-sm' : 'text-gray-500 hover:text-gray-700'}`}
                                    >
                                        {m.profilePictureUrl ? (
                                            <img src={m.profilePictureUrl} alt={m.missionaryName}
                                                 className="w-5 h-5 rounded-full object-cover"/>
                                        ) : (
                                            <div
                                                className="w-5 h-5 rounded-full bg-gray-200 flex items-center justify-center">
                                                <svg xmlns="http://www.w3.org/2000/svg" width="10" height="10"
                                                     viewBox="0 0 24 24" fill="none" stroke="currentColor"
                                                     strokeWidth="2"
                                                     strokeLinecap="round" strokeLinejoin="round">
                                                    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                                                    <circle cx="12" cy="7" r="4"></circle>
                                                </svg>
                                            </div>
                                        )}
                                        {m.missionaryName}
                                    </button>
                                ))}
                            </div>
                        </div>
                    )}

                    <div
                        className="search-container bg-accent-light-green p-8 rounded-2xl border border-accent-mid-green w-full max-w-md text-center mb-12 shadow-sm"
                    >
                        <h2 className="text-2xl font-bold mb-4 text-accent-dark-green">Find a Missionary</h2>
                        <form
                            onSubmit={handleSearch}
                            className="flex items-center justify-center gap-2"
                        >
                            <input
                                type="text"
                                placeholder="Enter Invite Code"
                                value={inviteCode}
                                onChange={(e) => setInviteCode(e.target.value)}
                                className="flex-1 p-2 rounded-lg border border-accent-mid-green bg-white text-accent-dark-green focus:outline-none focus:ring-2 focus:ring-primary"
                            />
                            <button
                                type="submit"
                                className="px-6 py-2 rounded-lg text-white bg-accent-mid-green font-bold hover:bg-accent-dark-green hover:cursor-pointer transition-all duration-300"
                            >
                                Connect
                            </button>
                        </form>

                        {error && <p className="text-red-500 mt-2">{error}</p>}

                        {successMessage && (
                            <div className="mt-4 text-center">
                                <p className="text-accent-dark-green font-bold text-lg">
                                    {successMessage}
                                </p>
                            </div>
                        )}
                    </div>

                    <div className="w-full max-w-4xl mx-auto">
                        <h2 className="text-3xl font-bold mb-8 text-center text-accent-dark-green">
                            Missionary Updates
                        </h2>

                        {loadingFeed ? (
                            <p className="text-center text-accent-mid-green">Loading updates...</p>
                        ) : feed.length === 0 ? (
                            <p className="text-center text-gray-500 italic">
                                No updates from your connected missionaries yet.
                            </p>
                        ) : (
                            <div className="flex flex-col gap-8">
                                {feed.map(post => (
                                    <div key={post.id}
                                         className="bg-white p-8 rounded-2xl shadow-lg border border-accent-light-green">
                                        <div className="flex justify-between items-start mb-6">
                                            <div className="flex-1">
                                                {post.title &&
                                                    <h3 className="text-2xl font-bold text-accent-dark-green mb-1">{post.title}</h3>}
                                                <p className="text-accent-mid-green font-semibold text-sm">
                                                    By {post.authorName}
                                                </p>
                                            </div>
                                            <div className="text-right">
                                                <p className="text-gray-400 text-xs">
                                                    {new Date(post.createdAt).toLocaleString([], {
                                                        dateStyle: 'medium',
                                                        timeStyle: 'short'
                                                    })}
                                                </p>
                                                {post.updatedAt && new Date(post.updatedAt).getTime() > new Date(post.createdAt).getTime() + 1000 && (
                                                    <p className="text-gray-400 text-[10px] italic">
                                                        (Updated: {new Date(post.updatedAt).toLocaleString([], {
                                                        dateStyle: 'short',
                                                        timeStyle: 'short'
                                                    })})
                                                    </p>
                                                )}
                                            </div>
                                        </div>

                                        {post.media && post.media.length > 0 && (
                                            <div className="grid grid-cols-1 gap-4 mb-6">
                                                {post.media.map(m => (
                                                    <div key={m.id} className="w-full overflow-hidden rounded-xl">
                                                        {m.mediaType === "IMAGE" && (
                                                            <img src={m.url} alt={m.fileName}
                                                                 className="w-full h-auto object-cover"/>
                                                        )}
                                                        {m.mediaType === "VIDEO" && (
                                                            <video controls src={m.url} className="w-full"/>
                                                        )}
                                                        {m.mediaType === "AUDIO" && (
                                                            <audio controls src={m.url} className="w-full mt-2"/>
                                                        )}
                                                        {m.mediaType === "DOCUMENT" && (
                                                            <a href={m.url} target="_blank" rel="noreferrer"
                                                               className="flex items-center gap-3 p-4 bg-accent-light-green rounded-xl text-accent-dark-green font-medium no-underline hover:bg-accent-mid-green hover:text-white transition-colors">
                                                                <span className="text-xl">📄</span> {m.fileName}
                                                            </a>
                                                        )}
                                                    </div>
                                                ))}
                                            </div>
                                        )}

                                        {post.content && (
                                            <p className="text-gray-700 leading-relaxed mb-6 whitespace-pre-wrap">{post.content}</p>
                                        )}

                                        <div className="border-t border-gray-100 pt-6">
                                            <div className="flex items-center justify-between mb-6">
                                                <div className="flex items-center gap-4">
                                                    <button
                                                        onClick={() => handleToggleLike(post.id)}
                                                        className={`flex items-center gap-2 px-4 py-2 rounded-full transition-all duration-200 ${post.liked ? 'bg-red-50 text-red-500' : 'bg-gray-50 text-gray-500 hover:bg-gray-100'}`}
                                                    >
                                                        <span className="text-xl">{post.liked ? "❤️" : "🤍"}</span>
                                                        <span
                                                            className="font-semibold">{post.liked ? "Liked" : "Like"}</span>
                                                    </button>
                                                    <span className="text-gray-500 text-sm font-medium">
                                                {post.lastLikerName ? (
                                                    <>
                                                        Liked by <span
                                                        className="text-accent-dark-green">{post.lastLikerName}</span>
                                                        {post.likeCount > 1 && ` and ${post.likeCount - 1} more`}
                                                    </>
                                                ) : (
                                                    `${post.likeCount} ${post.likeCount === 1 ? "like" : "likes"}`
                                                )}
                                            </span>
                                                </div>
                                            </div>

                                            <CommentSection postId={post.id} postAuthorId={post.authorId}/>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default SupporterView;