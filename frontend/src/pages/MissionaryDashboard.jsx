import React, {useState, useEffect} from 'react';
import PostFeed from "./PostFeed.jsx";
import ConnectionRequests from "./ConnectionRequests.jsx";
import SupporterList from "./SupporterList.jsx";
import BannedUsers from "./BannedUsers.jsx";

const ExportManager = () => {
    const [dateRange, setDateRange] = useState({
        start: "",
        end: ""
    });
    const [exportError, setExportError] = useState("");
    const [options, setOptions] = useState({
        includeSupporters: true,
        includeSupporterCount: true,
        includeRequests: true,
        includePrayers: true,
        includePosts: true,
        includeComments: true,
        includeAllComments: false,
        includeLikes: true
    });

    const labelMap = {
        includeSupporters: "Supporters",
        includeSupporterCount: "Supporter Count",
        includeRequests: "Connection Requests",
        includePrayers: "Prayer Requests",
        includePosts: "Updates & Posts",
        includeComments: "Comments on Posts",
        includeAllComments: "All Comments (Separate)",
        includeLikes: "Like Counts"
    };

    const handleExport = () => {
        const anySelected = Object.values(options).some(val => val === true);
        if (!anySelected) {
            setExportError("Please select at least one option to export.");
            return;
        }

        setExportError("");

        let url = "/api/missionary/export/csv?";
        if (dateRange?.start) url += `startDate=${encodeURIComponent(dateRange.start)}&`;
        if (dateRange?.end) url += `endDate=${encodeURIComponent(dateRange.end)}&`;
        url += `includeSupporters=${options.includeSupporters}&`;
        url += `includeSupporterCount=${options.includeSupporterCount}&`;
        url += `includeRequests=${options.includeRequests}&`;
        url += `includePrayers=${options.includePrayers}&`;
        url += `includePosts=${options.includePosts}&`;
        url += `includeComments=${options.includeComments}&`;
        url += `includeAllComments=${options.includeAllComments}&`;
        url += `includeLikes=${options.includeLikes}`;

        window.location.href = url;
    };

    const toggleOption = (opt) => {
        setExportError("");
        setOptions(prev => {
            const next = {...prev, [opt]: !prev[opt]};
            if (opt === 'includeComments' && next.includeComments) {
                next.includeAllComments = false;
            } else if (opt === 'includeAllComments' && next.includeAllComments) {
                next.includeComments = false;
            }
            return next;
        });
    };

    return (
        <div className="mt-8">
            <hr className="border-gray-100 mb-8"/>
            <h3 className="text-lg font-bold text-accent-dark-green mb-4">Export Data</h3>
            <p className="text-gray-600 mb-8 text-sm">Select data types and an optional date range to export your
                ministry data as a CSV file.</p>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-8">
                <div className="flex flex-col gap-2">
                    <label className="text-xs font-black text-accent-mid-green uppercase tracking-widest mb-1 block">Start
                        Date</label>
                    <input
                        type="date"
                        value={dateRange.start}
                        onChange={(e) => setDateRange(prev => ({...prev, start: e.target.value}))}
                        className="w-full p-3 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-accent-mid-green/20 focus:border-accent-mid-green transition-all"
                    />
                </div>
                <div className="flex flex-col gap-2">
                    <label className="text-xs font-black text-accent-mid-green uppercase tracking-widest mb-1 block">End
                        Date</label>
                    <input
                        type="date"
                        value={dateRange.end}
                        onChange={(e) => setDateRange(prev => ({...prev, end: e.target.value}))}
                        className="w-full p-3 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-accent-mid-green/20 focus:border-accent-mid-green transition-all"
                    />
                </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-8">
                {Object.keys(options).map(opt => (
                    <label key={opt} className="flex items-center gap-3 cursor-pointer group">
                        <div
                            onClick={() => toggleOption(opt)}
                            className={`w-5 h-5 rounded border transition-all flex items-center justify-center ${options[opt] ? 'bg-accent-mid-green border-accent-mid-green' : 'border-gray-200 group-hover:border-accent-mid-green'}`}
                        >
                            {options[opt] && <span className="text-white text-[10px]">✓</span>}
                        </div>
                        <span className="text-sm text-gray-600">
                            {labelMap[opt] || opt.replace(/([A-Z])/g, ' $1').toLowerCase()}
                        </span>
                    </label>
                ))}
            </div>

            {exportError && <p className="text-red-500 text-sm mb-4 font-bold">{exportError}</p>}

            <button
                onClick={handleExport}
                className="w-full py-3 bg-white border-2 border-accent-mid-green text-accent-mid-green rounded-xl font-bold hover:bg-accent-light-green transition-all shadow-sm flex items-center justify-center gap-2"
            >
                <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none"
                     stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                    <polyline points="7 10 12 15 17 10"></polyline>
                    <line x1="12" y1="15" x2="12" y2="3"></line>
                </svg>
                Download CSV Export
            </button>
        </div>
    );
};

export const MissionaryDashboard = () => {
    const [activeTab, setActiveTab] = useState("feed");
    const [profile, setProfile] = useState(null);
    const [requests, setRequests] = useState([]);
    const [posts, setPosts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [isUploading, setIsUploading] = useState(false);
    const [settingsError, setSettingsError] = useState("");
    const [settingsSuccess, setSettingsSuccess] = useState("");
    const [showConfirmNewCode, setShowConfirmNewCode] = useState(false);
    const [copied, setCopied] = useState(false);
    const [editProfile, setEditProfile] = useState({
        missionaryName: "",
        locationRegion: "",
        biography: ""
    });

    const handleCopy = () => {
        if (profile?.referenceNumber) {
            navigator.clipboard.writeText(profile.referenceNumber);
            setCopied(true);
            setTimeout(() => setCopied(false), 2000);
        }
    };

    const handleShare = () => {
        if (navigator.share && profile?.referenceNumber) {
            navigator.share({
                title: "My Missionary Invite Code",
                text: `Join me on Shepherds' Stories using my invite code: ${profile.referenceNumber}`,
                url: window.location.origin + "/register?code=" + profile.referenceNumber
            }).catch(console.error);
        } else {
            handleCopy();
        }
    };

    useEffect(() => {
        const fetchData = async () => {
            try {
                const [profRes, reqRes, postRes] = await Promise.all([
                    fetch("/api/missionary/profile"),
                    fetch("/api/missionary/requests"),
                    fetch("/api/posts")
                ]);

                if (profRes.ok) {
                    const data = await profRes.json();
                    setProfile(data);
                    setEditProfile({
                        missionaryName: data.missionaryName || "",
                        locationRegion: data.locationRegion || "",
                        biography: data.biography || ""
                    });
                }
                if (reqRes.ok) setRequests(await reqRes.json());
                if (postRes.ok) setPosts(await postRes.json());
            } catch (err) {
                console.error("Error fetching dashboard data:", err);
            } finally {
                setLoading(false);
            }
        };
        fetchData();
    }, []);

    const handleUpdateProfile = async (e) => {
        e.preventDefault();
        setSettingsError("");
        setSettingsSuccess("");
        try {
            const res = await fetch("/api/missionary/profile", {
                method: "PUT",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify(editProfile)
            });
            if (res.ok) {
                const updated = await res.json();
                setProfile(updated);
                setSettingsSuccess("Profile updated successfully!");
                setTimeout(() => setSettingsSuccess(""), 3000);
            } else {
                setSettingsError("Failed to update profile.");
            }
        } catch (err) {
            setSettingsError("Error updating profile: " + err.message);
        }
    };

    const handleLogout = async () => {
        await fetch("/api/auth/logout", {method: 'POST'});
        localStorage.removeItem("user");
        window.location.href = "/home";
    };

    const handleFileChange = async (e) => {
        const file = e.target.files[0];
        if (!file) return;
        setSettingsError("");
        setIsUploading(true);
        try {
            const urlRes = await fetch(`/api/missionary/profile/picture/upload-url?contentType=${encodeURIComponent(file.type)}`, {
                method: "POST"
            });
            const {uploadUrl, s3Key} = await urlRes.json();
            await fetch(uploadUrl, {method: "PUT", body: file, headers: {"Content-Type": file.type}});
            const updateRes = await fetch("/api/missionary/profile", {
                method: "PUT",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify({profilePictureUrl: s3Key})
            });
            if (updateRes.ok) setProfile(await updateRes.json());
        } catch (err) {
            console.error("Upload failed", err);
            setSettingsError("Failed to upload profile picture: " + err.message);
        } finally {
            setIsUploading(false);
        }
    };

    const toggleReference = async () => {
        setSettingsError("");
        const res = await fetch("/api/missionary/profile/toggle-reference", {method: 'POST'});
        if (res.ok) {
            const data = await res.json();
            setProfile(prev => ({...prev, isReferenceDisabled: data.isDisabled}));
        } else {
            setSettingsError("Failed to update code status.");
        }
    };

    const generateNewCode = async () => {
        setSettingsError("");
        try {
            const res = await fetch("/api/missionary/profile/generate-code", {method: 'POST'});
            if (res.ok) {
                const data = await res.json();
                setProfile(prev => ({...prev, referenceNumber: data.newCode, isReferenceDisabled: false}));
                setShowConfirmNewCode(false);
            } else {
                setSettingsError("Failed to generate new code.");
            }
        } catch (err) {
            setSettingsError("Error generating new code: " + err.message);
        }
    };

    if (loading) return (
        <div className="min-h-screen flex items-center justify-center bg-accent-light-green">
            <div className="text-2xl font-bold text-accent-dark-green animate-pulse">Loading Shepherd's Dashboard...
            </div>
        </div>
    );

    return (
        <div className="bg-linear-to-r from-white to-accent-light-green min-h-screen">
            <div className="max-w-7xl mx-auto px-4 py-8">
                {/* Header */}
                <div className="flex justify-between items-center mb-12 w-full max-w-4xl">
                    <div className="flex items-center gap-4">
                        <div className="relative group">
                            <div
                                className="w-16 h-16 rounded-full overflow-hidden border-2 border-accent-mid-green bg-white shadow-sm flex items-center justify-center">
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
                            <h1 className="text-3xl font-bold text-accent-dark-green">{profile?.missionaryName || "Missionary Dashboard"}</h1>
                            <div className="flex flex-col gap-1">
                                <p className="text-accent-mid-green font-medium flex items-center gap-2">
                                    <span>📍 {profile?.locationRegion || "Global"}</span>
                                    <span className="text-gray-300">•</span>
                                    <span
                                        className="uppercase text-[10px] font-bold tracking-widest bg-accent-mid-green/10 px-2 py-0.5 rounded">Missionary</span>
                                </p>
                                {profile?.biography && (
                                    <p className="text-sm text-gray-600 max-w-xl italic line-clamp-2"
                                       title={profile.biography}>
                                        {profile.biography}
                                    </p>
                                )}
                            </div>
                        </div>
                    </div>
                    <button onClick={handleLogout}
                            className="px-4 py-2 rounded-full border border-accent-mid-green text-accent-mid-green text-sm font-bold hover:bg-accent-mid-green hover:text-white transition-all duration-200">
                        Logout
                    </button>
                </div>

                {/* Tabs */}
                <div className="flex flex-wrap justify-center gap-2 mb-12 p-1 bg-gray-100 rounded-xl w-fit mx-auto">
                    {[
                        {
                            id: 'feed', label: 'Updates Feed', icon: (
                                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24"
                                     fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"
                                     strokeLinejoin="round">
                                    <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                                    <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                                </svg>
                            )
                        },
                        {
                            id: 'supporters', label: 'Supporters', icon: (
                                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24"
                                     fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"
                                     strokeLinejoin="round">
                                    <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                                    <circle cx="9" cy="7" r="4"></circle>
                                    <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                                    <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
                                </svg>
                            )
                        },
                        {
                            id: 'requests',
                            label: `Requests ${requests.length > 0 ? `(${requests.length})` : ''}`,
                            icon: (
                                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24"
                                     fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"
                                     strokeLinejoin="round">
                                    <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path>
                                    <path d="M13.73 21a2 2 0 0 1-3.46 0"></path>
                                </svg>
                            )
                        },
                        {
                            id: 'settings', label: 'Settings', icon: (
                                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24"
                                     fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"
                                     strokeLinejoin="round">
                                    <circle cx="12" cy="12" r="3"></circle>
                                    <path
                                        d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33 1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82 1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"></path>
                                </svg>
                            )
                        }
                    ].map(tab => (
                        <button
                            key={tab.id}
                            onClick={() => setActiveTab(tab.id)}
                            className={`px-4 py-2 rounded-lg text-sm font-bold transition-all flex items-center gap-2 ${activeTab === tab.id ? 'bg-white text-accent-dark-green shadow-sm' : 'text-gray-500 hover:text-gray-700'}`}
                        >
                            {tab.icon}
                            {tab.label}
                        </button>
                    ))}
                </div>

                {/* Content */}
                <div className="flex flex-col items-center">
                    {activeTab === 'feed' && <PostFeed posts={posts} setPosts={setPosts}/>}
                    {activeTab === 'requests' && <ConnectionRequests requests={requests} setRequests={setRequests}/>}
                    {activeTab === 'supporters' && <SupporterList/>}

                    {activeTab === 'settings' && (
                        <div
                            className="w-full max-w-2xl bg-white p-8 rounded-2xl border border-accent-mid-green shadow-sm animate-in fade-in slide-in-from-bottom-4 duration-500">
                            <h2 className="text-2xl font-bold text-accent-dark-green mb-8">Settings</h2>

                            {settingsError && <p className="text-red-500 text-sm mb-6 font-bold">{settingsError}</p>}
                            {settingsSuccess &&
                                <p className="text-accent-mid-green text-sm mb-6 font-bold">{settingsSuccess}</p>}

                            <div className="mb-12">
                                <h3 className="text-lg font-bold text-accent-dark-green mb-4">Profile Information</h3>
                                <form onSubmit={handleUpdateProfile} className="space-y-4">
                                    <div>
                                        <label
                                            className="text-xs font-black text-accent-mid-green uppercase tracking-widest mb-1 block">Display
                                            Name</label>
                                        <input
                                            type="text"
                                            value={editProfile.missionaryName}
                                            onChange={(e) => setEditProfile({
                                                ...editProfile,
                                                missionaryName: e.target.value
                                            })}
                                            className="w-full p-3 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-accent-mid-green/20 focus:border-accent-mid-green transition-all"
                                            placeholder="Your name or ministry name"
                                        />
                                    </div>
                                    <div>
                                        <label
                                            className="text-xs font-black text-accent-mid-green uppercase tracking-widest mb-1 block">Region
                                            / Location</label>
                                        <input
                                            type="text"
                                            value={editProfile.locationRegion}
                                            onChange={(e) => setEditProfile({
                                                ...editProfile,
                                                locationRegion: e.target.value
                                            })}
                                            className="w-full p-3 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-accent-mid-green/20 focus:border-accent-mid-green transition-all"
                                            placeholder="e.g. Southeast Asia, Nairobi, etc."
                                        />
                                    </div>
                                    <div>
                                        <label
                                            className="text-xs font-black text-accent-mid-green uppercase tracking-widest mb-1 block">Biography</label>
                                        <textarea
                                            value={editProfile.biography}
                                            onChange={(e) => setEditProfile({
                                                ...editProfile,
                                                biography: e.target.value
                                            })}
                                            rows="4"
                                            className="w-full p-3 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-accent-mid-green/20 focus:border-accent-mid-green transition-all resize-none"
                                            placeholder="Tell your supporters about your ministry..."
                                        />
                                    </div>
                                    <button
                                        type="submit"
                                        className="w-full py-3 bg-accent-mid-green text-white rounded-xl font-bold hover:bg-accent-dark-green transition-colors shadow-md"
                                    >
                                        Save Profile Changes
                                    </button>
                                </form>
                            </div>

                            <hr className="border-gray-100 mb-8"/>

                            <h3 className="text-lg font-bold text-accent-dark-green mb-4">Connection Settings</h3>

                            <div
                                className="p-6 bg-accent-light-green/30 rounded-2xl border border-accent-mid-green/20 mb-8">
                                <label
                                    className="text-xs font-black text-accent-mid-green uppercase tracking-widest mb-2 block">Your
                                    Active Invite Code</label>
                                <div className="flex items-center justify-between gap-4">
                                    <div className="flex items-center gap-4">
                                        <span
                                            className="text-3xl font-mono font-black text-accent-dark-green tracking-tighter">
                                            {profile?.referenceNumber || "---"}
                                        </span>
                                        {profile?.referenceNumber && (
                                            <div className="flex gap-2">
                                                <button
                                                    onClick={handleCopy}
                                                    className="p-2 rounded-lg bg-white border border-accent-mid-green/20 text-accent-mid-green hover:bg-accent-light-green transition-colors shadow-sm"
                                                    title="Copy to clipboard"
                                                >
                                                    {copied ? (
                                                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16"
                                                             viewBox="0 0 24 24" fill="none" stroke="currentColor"
                                                             strokeWidth="3" strokeLinecap="round"
                                                             strokeLinejoin="round">
                                                            <polyline points="20 6 9 17 4 12"></polyline>
                                                        </svg>
                                                    ) : (
                                                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16"
                                                             viewBox="0 0 24 24" fill="none" stroke="currentColor"
                                                             strokeWidth="2.5" strokeLinecap="round"
                                                             strokeLinejoin="round">
                                                            <rect x="9" y="9" width="13" height="13" rx="2"
                                                                  ry="2"></rect>
                                                            <path
                                                                d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
                                                        </svg>
                                                    )}
                                                </button>
                                                <button
                                                    onClick={handleShare}
                                                    className="p-2 rounded-lg bg-white border border-accent-mid-green/20 text-accent-mid-green hover:bg-accent-light-green transition-colors shadow-sm"
                                                    title="Share invite code"
                                                >
                                                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16"
                                                         viewBox="0 0 24 24" fill="none" stroke="currentColor"
                                                         strokeWidth="2.5" strokeLinecap="round"
                                                         strokeLinejoin="round">
                                                        <circle cx="18" cy="5" r="3"></circle>
                                                        <circle cx="6" cy="12" r="3"></circle>
                                                        <circle cx="18" cy="19" r="3"></circle>
                                                        <line x1="8.59" y1="13.51" x2="15.42" y2="17.49"></line>
                                                        <line x1="15.41" y1="6.51" x2="8.59" y2="10.49"></line>
                                                    </svg>
                                                </button>
                                            </div>
                                        )}
                                    </div>
                                    {!showConfirmNewCode ? (
                                        <button onClick={() => setShowConfirmNewCode(true)}
                                                className="text-xs font-bold text-accent-mid-green underline hover:text-accent-dark-green">
                                            Regenerate
                                        </button>
                                    ) : (
                                        <div
                                            className="flex flex-col items-end gap-2 animate-in fade-in zoom-in-95 duration-200">
                                            <p className="text-[10px] font-bold text-red-600 uppercase tracking-tight text-right leading-tight">
                                                This will invalidate<br/>your current code.
                                            </p>
                                            <div className="flex gap-3">
                                                <button onClick={() => setShowConfirmNewCode(false)}
                                                        className="text-[10px] font-bold text-gray-400 hover:text-gray-600 uppercase tracking-widest">
                                                    Cancel
                                                </button>
                                                <button onClick={generateNewCode}
                                                        className="text-[10px] font-bold text-red-600 hover:text-red-800 uppercase tracking-widest underline decoration-2 underline-offset-2">
                                                    Confirm
                                                </button>
                                            </div>
                                        </div>
                                    )}
                                </div>
                            </div>

                            <div className="flex items-center justify-between p-4 rounded-xl border border-gray-100">
                                <div>
                                    <h4 className="font-bold text-accent-dark-green">Code Status</h4>
                                    <p className="text-sm text-gray-500">{profile?.isReferenceDisabled ? "Hidden from new supporters" : "Visible and active"}</p>
                                </div>
                                <button
                                    onClick={toggleReference}
                                    className={`px-6 py-2 rounded-lg font-bold hover:bg-accent-light-green transition-all duration-300 ${profile?.isReferenceDisabled ? 'bg-accent-mid-green text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'}`}
                                >
                                    {profile?.isReferenceDisabled ? "Enable Code" : "Disable Code"}
                                </button>
                            </div>

                            <hr className="border-gray-100 mb-8"/>

                            <h3 className="text-lg font-bold text-accent-dark-green mb-4">Banned Users</h3>
                            <p className="text-gray-600 mb-6 text-sm">Review users you have banned from connecting with
                                you. You can unban them to allow them to request a connection again.</p>
                            <BannedUsers/>

                            <ExportManager/>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};