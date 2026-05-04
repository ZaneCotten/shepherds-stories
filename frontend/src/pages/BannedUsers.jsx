import React, {useState, useEffect} from 'react';

const BannedUsers = () => {
    const [bannedUsers, setBannedUsers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [unbanningId, setUnbanningId] = useState(null);

    const fetchBannedUsers = async () => {
        setLoading(true);
        try {
            const res = await fetch("/api/missionary/banned-supporters");
            if (res.ok) {
                setBannedUsers(await res.json());
            } else {
                setError("Failed to load banned users");
            }
        } catch (err) {
            setError("Error loading banned users: " + err.message);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchBannedUsers();
    }, []);

    const handleUnban = async (supporterId) => {
        setUnbanningId(supporterId);
        try {
            const res = await fetch(`/api/missionary/supporters/${supporterId}/unban`, {
                method: "POST"
            });
            if (res.ok) {
                setBannedUsers(prev => prev.filter(u => u.id !== supporterId));
            } else {
                alert("Failed to unban user");
            }
        } catch (err) {
            alert("Error unbanning user: " + err.message);
        } finally {
            setUnbanningId(null);
        }
    };

    if (loading) return <div className="text-sm text-gray-500 animate-pulse">Loading banned users...</div>;
    if (error) return <div className="text-sm text-red-500">{error}</div>;
    if (bannedUsers.length === 0) return <p className="text-sm text-gray-500 italic">No banned users found.</p>;

    return (
        <div className="space-y-4">
            {bannedUsers.map(user => (
                <div key={user.id}
                     className="flex items-center justify-between p-4 bg-white border border-gray-100 rounded-xl shadow-sm">
                    <div className="flex items-center gap-4">
                        <div className="w-12 h-12 rounded-full overflow-hidden border border-gray-200 bg-gray-50">
                            {user.profilePictureUrl ? (
                                <img src={user.profilePictureUrl} alt={user.firstName}
                                     className="w-full h-full object-cover"/>
                            ) : (
                                <div className="w-full h-full flex items-center justify-center text-gray-400">
                                    <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24"
                                         fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"
                                         strokeLinejoin="round">
                                        <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                                        <circle cx="12" cy="7" r="4"></circle>
                                    </svg>
                                </div>
                            )}
                        </div>
                        <div>
                            <h4 className="font-bold text-gray-900">{user.firstName} {user.lastName}</h4>
                            <p className="text-xs text-gray-500">
                                Banned on: {new Date(user.bannedAt).toLocaleDateString()}
                            </p>
                        </div>
                    </div>
                    <button
                        onClick={() => handleUnban(user.id)}
                        disabled={unbanningId === user.id}
                        className="px-4 py-2 text-xs font-bold text-accent-mid-green border border-accent-mid-green rounded-lg hover:bg-accent-light-green transition-colors disabled:opacity-50"
                    >
                        {unbanningId === user.id ? "Unbanning..." : "Unban"}
                    </button>
                </div>
            ))}
        </div>
    );
};

export default BannedUsers;
