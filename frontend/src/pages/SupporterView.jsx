import {useState, useEffect} from "react";
import {CommentSection} from "../components/CommentSection";
import {MediaCarousel} from "../components/MediaCarousel";
import ProfileModal from "../components/ProfileModal.jsx";
import ChangePasswordForm from "../components/ChangePasswordForm.jsx";

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
    const [activeTab, setActiveTab] = useState("dashboard");
    const [editProfile, setEditProfile] = useState({firstName: "", lastName: ""});
    const [selectedUserProfile, setSelectedUserProfile] = useState(null);
    const [settingsError, setSettingsError] = useState("");
    const [settingsSuccess, setSettingsSuccess] = useState("");

    useEffect(() => {
        const fetchProfile = async () => {
            try {
                const res = await fetch("/api/supporter/profile");
                if (res.ok) {
                    const data = await res.json();
                    setProfile(data);
                    setEditProfile({firstName: data.firstName || "", lastName: data.lastName || ""});
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
                    // Automatically choose initial tab based on connections
                    setActiveTab(prev => {
                        if (prev === "dashboard") {
                            return data.length > 0 ? "feed" : "missionaries";
                        }
                        return prev;
                    });
                }
            } catch (err) {
                console.error("Error fetching missionaries:", err);
                // Fallback for initial load
                setActiveTab(prev => prev === "dashboard" ? "missionaries" : prev);
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
            const urlRes = await fetch(`/api/supporter/profile/picture/upload-url?contentType=${encodeURIComponent(file.type)}`, {
                method: "POST"
            });
            const {uploadUrl, s3Key} = await urlRes.json();

            await fetch(uploadUrl, {
                method: "PUT",
                body: file,
                headers: {"Content-Type": file.type}
            });

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

    const handleUpdateProfile = async (e) => {
        e.preventDefault();
        setSettingsError("");
        setSettingsSuccess("");

        try {
            const res = await fetch("/api/supporter/profile", {
                method: "PUT",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify(editProfile)
            });

            if (res.ok) {
                const data = await res.json();
                setProfile(data);
                setSettingsSuccess("Profile updated successfully!");
                setTimeout(() => setSettingsSuccess(""), 3000);
            } else {
                setSettingsError("Failed to update profile.");
            }
        } catch (err) {
            setSettingsError("Error updating profile: " + err.message);
        }
    };

    const handleRemoveMissionary = async (missionaryId) => {
        if (!window.confirm("Are you sure you want to unfollow this missionary? You will no longer see their updates.")) return;

        try {
            const res = await fetch(`/api/supporter/missionaries/${missionaryId}/remove`, {
                method: 'POST'
            });

            if (res.ok) {
                setMissionaries(prev => prev.filter(m => m.id !== missionaryId));
                setSuccessMessage("Missionary unfollowed successfully.");
                setTimeout(() => setSuccessMessage(""), 3000);
                if (selectedMissionary === missionaryId) {
                    setSelectedMissionary("");
                }
            } else {
                const data = await res.json();
                setError(data.message || "Failed to unfollow missionary.");
            }
        } catch (err) {
            console.error("Error unfollowing missionary:", err);
            setError("Error unfollowing missionary.");
        }
    };

    const tabs = [
        {
            id: 'feed', label: 'Feed', icon: (
                <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none"
                     stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M4 11a9 9 0 0 1 9 9"></path>
                    <path d="M4 4a16 16 0 0 1 16 16"></path>
                    <circle cx="5" cy="19" r="1"></circle>
                </svg>
            )
        },
        {
            id: 'missionaries', label: 'Missionaries', icon: (
                <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none"
                     stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                    <circle cx="9" cy="7" r="4"></circle>
                    <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                    <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
                </svg>
            )
        },
        {
            id: 'settings', label: 'Settings', icon: (
                <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none"
                     stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                    <circle cx="12" cy="12" r="3"></circle>
                    <path
                        d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"></path>
                </svg>
            )
        }
    ];

    return (
        <div className="bg-linear-to-r from-white to-accent-light-green min-h-screen relative">
            <div className="max-w-7xl mx-auto px-4 py-8 flex flex-col items-center">
                <div className="w-full max-w-2xl flex justify-between items-center mb-12">
                    <div className="flex items-center gap-4 text-left">
                        <div
                            className="w-16 h-16 rounded-full overflow-hidden border-2 border-accent-mid-green bg-white flex items-center justify-center shadow-sm cursor-pointer hover:opacity-80 transition-opacity"
                            onClick={() => setActiveTab('settings')}
                        >
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
                        </div>
                        <div>
                            <h1 className="text-3xl font-bold text-accent-dark-green">Supporter Dashboard</h1>
                            <p className="text-accent-mid-green font-medium">
                                Welcome, {profile ? `${profile.firstName} ${profile.lastName}` : "Supporter"}
                            </p>
                        </div>
                    </div>
                    <button
                        onClick={handleLogout}
                        className="px-4 py-2 rounded-full border border-accent-mid-green text-accent-mid-green text-sm font-bold hover:bg-accent-mid-green hover:text-white transition-all duration-200"
                    >
                        Logout
                    </button>
                </div>

                {/* Tabs */}
                <div
                    className="flex flex-wrap justify-center items-center gap-2 p-1 bg-gray-100/50 backdrop-blur-md rounded-2xl mb-8 border border-white/20 sticky top-4 z-40 shadow-sm w-full max-w-2xl mx-auto">
                    {tabs.map(tab => (
                        <button
                            key={tab.id}
                            onClick={() => setActiveTab(tab.id)}
                            className={`flex items-center gap-2 px-6 py-3 rounded-xl text-sm font-bold transition-all duration-300 ${
                                activeTab === tab.id
                                    ? 'bg-white text-accent-dark-green shadow-md scale-105'
                                    : 'text-gray-500 hover:text-accent-mid-green hover:bg-white/50'
                            }`}
                        >
                            {tab.icon}
                            <span className={activeTab === tab.id ? 'block' : 'hidden sm:block'}>{tab.label}</span>
                        </button>
                    ))}
                </div>

                <div className="w-full flex flex-col lg:flex-row-reverse gap-8 items-start">
                    {activeTab === 'feed' && (
                        <div
                            className="w-full lg:w-80 shrink-0 animate-in fade-in slide-in-from-right-4 duration-500">
                            <div className="lg:sticky lg:top-24 space-y-4">
                                <div className="bg-white p-6 rounded-2xl border border-accent-mid-green shadow-sm">
                                    <h2 className="text-xl font-bold text-accent-dark-green mb-4 flex items-center gap-2">
                                        <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20"
                                             viewBox="0 0 24 24"
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
                                                            className="text-[9px] font-bold text-gray-400 uppercase cursor-pointer hover:underline"
                                                            onClick={() => {
                                                                const missionary = missionaries.find(m => m.missionaryName === req.missionaryName);
                                                                if (missionary) setSelectedUserProfile(missionary);
                                                            }}
                                                        >
                                                            {req.missionaryName}
                                                        </span>
                                                    </div>
                                                    <p className="text-xs text-gray-600 line-clamp-3 hover:line-clamp-none transition-all cursor-default">{req.content}</p>
                                                </div>
                                            ))
                                        ) : (
                                            <p className="text-sm italic text-gray-400 text-center py-4">No active
                                                prayer
                                                requests.</p>
                                        )}
                                    </div>
                                </div>
                            </div>
                        </div>
                    )}

                    <div className="flex-1 w-full">
                        {activeTab === 'feed' && (
                            <div
                                className="flex flex-col items-center animate-in fade-in slide-in-from-bottom-4 duration-500">
                                <h2 className="text-3xl font-bold mb-4 text-center text-accent-dark-green">
                                    Missionary Updates
                                </h2>

                                <div className="mb-8 w-full max-w-md">
                                    <div className="relative">
                                        <select
                                            value={selectedMissionary}
                                            onChange={(e) => setSelectedMissionary(e.target.value)}
                                            className="w-full p-3 pl-10 rounded-xl border border-accent-mid-green/30 bg-white text-accent-dark-green font-medium focus:outline-none focus:ring-2 focus:ring-accent-mid-green appearance-none shadow-sm cursor-pointer transition-all hover:border-accent-mid-green"
                                        >
                                            <option value="">All Missionaries</option>
                                            {missionaries.map(m => (
                                                <option key={m.id} value={m.id}>
                                                    {m.missionaryName}
                                                </option>
                                            ))}
                                        </select>
                                        <div
                                            className="absolute left-3 top-1/2 -translate-y-1/2 text-accent-mid-green pointer-events-none">
                                            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18"
                                                 viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"
                                                 strokeLinecap="round" strokeLinejoin="round">
                                                <polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"></polygon>
                                            </svg>
                                        </div>
                                        <div
                                            className="absolute right-3 top-1/2 -translate-y-1/2 text-accent-mid-green pointer-events-none">
                                            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16"
                                                 viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5"
                                                 strokeLinecap="round" strokeLinejoin="round">
                                                <path d="m6 9 6 6 6-6"></path>
                                            </svg>
                                        </div>
                                    </div>
                                </div>

                                {loadingFeed ? (
                                    <p className="text-center text-accent-mid-green">Loading updates...</p>
                                ) : feed.length === 0 ? (
                                    <p className="text-center text-gray-500 italic">
                                        No updates from your connected missionaries yet.
                                    </p>
                                ) : (
                                    <div className="flex flex-col gap-8 w-full">
                                        {feed.map(post => (
                                            <div key={post.id}
                                                 className="bg-white p-8 rounded-2xl shadow-lg border border-accent-light-green">
                                                <div className="flex justify-between items-start mb-6">
                                                    <div className="flex-1">
                                                        {post.title &&
                                                            <h3 className="text-2xl font-bold text-accent-dark-green mb-1">{post.title}</h3>}
                                                        <p className="text-accent-mid-green font-semibold text-sm cursor-pointer hover:underline"
                                                           onClick={() => setSelectedUserProfile(post)}>
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
                                                        {post.updatedAt && new Date(post.updatedAt).getTime() - new Date(post.createdAt).getTime() > 60000 && (
                                                            <p className="text-gray-400 text-[10px] font-bold uppercase tracking-wider mt-0.5">
                                                                Updated
                                                                at {new Date(post.updatedAt).toLocaleString([], {
                                                                dateStyle: 'medium',
                                                                timeStyle: 'short'
                                                            })}
                                                            </p>
                                                        )}
                                                    </div>
                                                </div>

                                                {post.media && post.media.length > 0 && (
                                                    <div className="mb-6">
                                                        <MediaCarousel media={post.media} isPreview={false}/>
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
                                                                <span
                                                                    className="text-xl">{post.liked ? "❤️" : "🤍"}</span>
                                                                <span
                                                                    className="font-semibold">{post.liked ? "Liked" : "Like"}</span>
                                                            </button>
                                                            <span className="text-gray-500 text-sm font-medium">
                                                            {post.likeCount} {post.likeCount === 1 ? "like" : "likes"}
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
                        )}

                        {activeTab === 'missionaries' && (
                            <div
                                className="flex flex-col items-center animate-in fade-in slide-in-from-bottom-4 duration-500">
                                <div
                                    className="search-container bg-accent-light-green p-8 rounded-2xl border border-accent-mid-green w-full max-w-md text-center mb-12 shadow-sm"
                                >
                                    <h2 className="text-2xl font-bold mb-4 text-accent-dark-green">Find a
                                        Missionary</h2>
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
                                            className="px-6 py-2 rounded-lg text-white bg-accent-mid-green font-bold hover:bg-accent-dark-green transition-all duration-300"
                                        >
                                            Connect
                                        </button>
                                    </form>
                                    {error && !error.includes("upload") && <p className="text-red-500 mt-2">{error}</p>}
                                    {successMessage &&
                                        <p className="text-accent-dark-green font-bold mt-4">{successMessage}</p>}
                                </div>

                                {missionaries.length > 0 && (
                                    <div className="w-full max-w-4xl">
                                        <h3 className="text-xl font-bold text-accent-dark-green mb-6 text-center">Your
                                            Connected Missionaries</h3>
                                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                                            {missionaries.map(m => (
                                                <div key={m.id}
                                                     className="bg-white p-6 rounded-2xl border border-accent-mid-green/20 shadow-sm flex items-start gap-4 cursor-pointer hover:shadow-md transition-all group"
                                                     onClick={() => setSelectedUserProfile(m)}>
                                                    <div
                                                        className="w-16 h-16 rounded-full overflow-hidden border border-accent-mid-green/30 shrink-0">
                                                        {m.profilePictureUrl ? (
                                                            <img src={m.profilePictureUrl} alt={m.missionaryName}
                                                                 className="w-full h-full object-cover"/>
                                                        ) : (
                                                            <div
                                                                className="w-full h-full bg-gray-50 flex items-center justify-center text-accent-mid-green">
                                                                <svg xmlns="http://www.w3.org/2000/svg" width="24"
                                                                     height="24" viewBox="0 0 24 24" fill="none"
                                                                     stroke="currentColor" strokeWidth="2"
                                                                     strokeLinecap="round" strokeLinejoin="round">
                                                                    <path
                                                                        d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                                                                    <circle cx="12" cy="7" r="4"></circle>
                                                                </svg>
                                                            </div>
                                                        )}
                                                    </div>
                                                    <div className="flex-1">
                                                        <h4 className="font-bold text-accent-dark-green group-hover:underline">{m.missionaryName}</h4>
                                                        <p className="text-accent-mid-green text-xs font-medium mb-2">📍 {m.locationRegion || "Global"}</p>
                                                        {m.biography &&
                                                            <p className="text-gray-600 text-xs line-clamp-2 italic">"{m.biography}"</p>}
                                                    </div>
                                                    <button
                                                        onClick={(e) => {
                                                            e.stopPropagation();
                                                            handleRemoveMissionary(m.id);
                                                        }}
                                                        className="px-3 py-1.5 text-xs font-bold text-gray-400 hover:text-red-500 hover:bg-red-50 rounded-lg transition-all duration-200 border border-transparent hover:border-red-100"
                                                    >
                                                        Unfollow
                                                    </button>
                                                </div>
                                            ))}
                                        </div>
                                    </div>
                                )}
                            </div>
                        )}

                        {activeTab === 'settings' && (
                            <div
                                className="w-full max-w-2xl mx-auto bg-white p-8 rounded-2xl border border-accent-mid-green shadow-sm animate-in fade-in slide-in-from-bottom-4 duration-500">
                                <h2 className="text-2xl font-bold text-accent-dark-green mb-8">Settings</h2>

                                {settingsError &&
                                    <p className="text-red-500 text-sm mb-6 font-bold">{settingsError}</p>}
                                {settingsSuccess &&
                                    <p className="text-accent-mid-green text-sm mb-6 font-bold">{settingsSuccess}</p>}

                                <div className="mb-12">
                                    <h3 className="text-lg font-bold text-accent-dark-green mb-6">Profile
                                        Information</h3>

                                    <div
                                        className="flex flex-col sm:flex-row items-center gap-6 mb-8 p-6 bg-accent-light-green/20 rounded-2xl border border-accent-mid-green/10">
                                        <div className="relative">
                                            <div
                                                className="w-28 h-28 rounded-full overflow-hidden border-2 border-accent-mid-green bg-white shadow-md flex items-center justify-center">
                                                {profile?.profilePictureUrl ? (
                                                    <img src={profile.profilePictureUrl} alt="Profile"
                                                         className="w-full h-full object-cover"/>
                                                ) : (
                                                    <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48"
                                                         viewBox="0 0 24 24" fill="none" stroke="#2D5A27"
                                                         strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                                        <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                                                        <circle cx="12" cy="7" r="4"></circle>
                                                    </svg>
                                                )}
                                                {isUploading && (
                                                    <div
                                                        className="absolute inset-0 bg-black/40 flex items-center justify-center">
                                                        <div
                                                            className="w-8 h-8 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                                                    </div>
                                                )}
                                            </div>
                                            <label
                                                className="absolute -bottom-2 -right-2 bg-white rounded-full p-3 border border-accent-mid-green cursor-pointer shadow-lg hover:bg-accent-light-green transition-colors">
                                                <input
                                                    id="supporter-photo-upload"
                                                    type="file"
                                                    className="hidden"
                                                    accept="image/*"
                                                    onChange={handleFileChange}
                                                    disabled={isUploading}
                                                />
                                                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20"
                                                     viewBox="0 0 24 24" fill="none" stroke="#2D5A27" strokeWidth="2.5"
                                                     strokeLinecap="round" strokeLinejoin="round">
                                                    <path
                                                        d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"></path>
                                                    <circle cx="12" cy="13" r="4"></circle>
                                                </svg>
                                            </label>
                                        </div>
                                        <div className="text-center sm:text-left">
                                            <h4 className="text-sm font-bold text-accent-dark-green uppercase tracking-widest">Profile
                                                Picture</h4>
                                            <p className="text-xs text-gray-500 mt-1 mb-3">Tap the camera icon or button
                                                below to upload a
                                                new photo.</p>
                                            <button
                                                type="button"
                                                onClick={() => document.getElementById('supporter-photo-upload').click()}
                                                disabled={isUploading}
                                                className="px-4 py-2 bg-white border border-accent-mid-green text-accent-mid-green text-xs font-bold rounded-lg hover:bg-accent-light-green transition-colors disabled:opacity-50"
                                            >
                                                {isUploading ? 'Uploading...' : 'Change Photo'}
                                            </button>
                                            <p className="text-[10px] text-gray-400 mt-2 italic">Recommended: Square
                                                image, max 5MB</p>
                                        </div>
                                    </div>

                                    <form onSubmit={handleUpdateProfile} className="space-y-4">
                                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                                            <div>
                                                <label
                                                    className="text-xs font-black text-accent-mid-green uppercase tracking-widest mb-1 block">First
                                                    Name</label>
                                                <input
                                                    type="text"
                                                    value={editProfile.firstName}
                                                    onChange={(e) => setEditProfile({
                                                        ...editProfile,
                                                        firstName: e.target.value
                                                    })}
                                                    className="w-full p-3 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-accent-mid-green/20 focus:border-accent-mid-green transition-all"
                                                />
                                            </div>
                                            <div>
                                                <label
                                                    className="text-xs font-black text-accent-mid-green uppercase tracking-widest mb-1 block">Last
                                                    Name</label>
                                                <input
                                                    type="text"
                                                    value={editProfile.lastName}
                                                    onChange={(e) => setEditProfile({
                                                        ...editProfile,
                                                        lastName: e.target.value
                                                    })}
                                                    className="w-full p-3 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-accent-mid-green/20 focus:border-accent-mid-green transition-all"
                                                />
                                            </div>
                                        </div>
                                        <button
                                            type="submit"
                                            className="w-full py-3 bg-accent-mid-green text-white rounded-xl font-bold hover:bg-accent-dark-green transition-colors shadow-md mt-4"
                                        >
                                            Save Profile Changes
                                        </button>
                                    </form>
                                    <ChangePasswordForm/>
                                </div>
                            </div>
                        )}
                    </div>
                </div>
            </div>
            <ProfileModal
                isOpen={!!selectedUserProfile}
                onClose={() => setSelectedUserProfile(null)}
                user={selectedUserProfile}
            />
        </div>
    );
};

export default SupporterView;