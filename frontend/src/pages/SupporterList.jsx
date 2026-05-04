import {useState, useEffect} from "react";

const SupporterList = () => {
    const [supporters, setSupporters] = useState([]);
    const [loading, setLoading] = useState(true);
    const [searchTerm, setSearchTerm] = useState("");
    const [error, setError] = useState("");
    const [removingSupporterId, setRemovingSupporterId] = useState(null);
    const [banningSupporterId, setBanningSupporterId] = useState(null);

    useEffect(() => {
        fetchSupporters();
    }, []);

    const fetchSupporters = async () => {
        try {
            const res = await fetch("/api/missionary/supporters");
            if (res.ok) {
                const data = await res.json();
                setSupporters(data);
            }
        } catch (err) {
            console.error("Failed to fetch supporters:", err);
        } finally {
            setLoading(false);
        }
    };

    const handleRemove = async (supporterId) => {
        setError("");
        try {
            const res = await fetch(`/api/missionary/supporters/${supporterId}/remove`, {method: "POST"});
            if (res.ok) {
                setSupporters(supporters.filter(s => s.id !== supporterId));
                setRemovingSupporterId(null);
            } else {
                setError("Failed to remove supporter");
            }
        } catch (err) {
            console.error("Remove error:", err);
            setError("Failed to remove supporter: " + err.message);
        }
    };

    const handleBan = async (supporterId) => {
        setError("");
        try {
            const res = await fetch(`/api/missionary/supporters/${supporterId}/ban`, {method: "POST"});
            if (res.ok) {
                setSupporters(supporters.filter(s => s.id !== supporterId));
                setBanningSupporterId(null);
            } else {
                setError("Failed to ban supporter");
            }
        } catch (err) {
            console.error("Ban error:", err);
            setError("Failed to ban supporter: " + err.message);
        }
    };

    const filteredSupporters = supporters.filter(supporter => {
        const fullName = `${supporter.firstName} ${supporter.lastName}`.toLowerCase();
        return fullName.includes(searchTerm.toLowerCase());
    });

    if (loading) return <div className="text-accent-mid-green font-bold">Loading supporters...</div>;

    return (
        <div className="w-full max-w-2xl animate-in fade-in slide-in-from-bottom-4 duration-500">
            <h2 className="text-2xl font-bold text-accent-dark-green mb-6">Your Supporters</h2>

            {error && <p className="text-red-500 text-sm mb-4 font-bold">{error}</p>}

            {supporters.length > 0 && (
                <div className="mb-6 relative">
                    <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                        <svg className="h-5 w-5 text-accent-mid-green" xmlns="http://www.w3.org/2000/svg"
                             viewBox="0 0 20 20" fill="currentColor">
                            <path fillRule="evenodd"
                                  d="M8 4a4 4 0 100 8 4 4 0 000-8zM2 8a6 6 0 1110.89 3.476l4.817 4.817a1 1 0 01-1.414 1.414l-4.816-4.816A6 6 0 012 8z"
                                  clipRule="evenodd"/>
                        </svg>
                    </div>
                    <input
                        type="text"
                        placeholder="Search supporters by name..."
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                        className="block w-full pl-11 pr-4 py-3 border-2 border-accent-light-green rounded-2xl leading-5 bg-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-accent-mid-green focus:border-accent-mid-green sm:text-sm transition-all shadow-md"
                    />
                </div>
            )}

            {supporters.length === 0 ? (
                <div
                    className="bg-white p-12 rounded-2xl border border-dashed border-accent-mid-green text-center text-gray-500 italic shadow-sm">
                    No active supporters yet.
                </div>
            ) : (
                <div className="grid gap-4">
                    {filteredSupporters.length === 0 ? (
                        <div
                            className="bg-white p-8 rounded-2xl border border-dashed border-gray-200 text-center text-gray-400 italic">
                            No supporters match your search.
                        </div>
                    ) : (
                        filteredSupporters.map(supporter => (
                            <div key={supporter.id}
                                 className="bg-white p-6 rounded-2xl border border-accent-light-green flex items-center justify-between shadow-lg hover:shadow-xl transition-all">
                                <div className="flex items-center gap-4">
                                    <div
                                        className="w-14 h-14 rounded-full overflow-hidden border-2 border-accent-mid-green bg-white flex-shrink-0 shadow-sm">
                                        {supporter.profilePictureUrl ? (
                                            <img src={supporter.profilePictureUrl} alt=""
                                                 className="w-full h-full object-cover"/>
                                        ) : (
                                            <div
                                                className="w-full h-full flex items-center justify-center bg-accent-light-green/30 text-accent-mid-green">
                                                <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24"
                                                     viewBox="0 0 24 24" fill="none" stroke="currentColor"
                                                     strokeWidth="2"
                                                     strokeLinecap="round" strokeLinejoin="round">
                                                    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                                                    <circle cx="12" cy="7" r="4"></circle>
                                                </svg>
                                            </div>
                                        )}
                                    </div>
                                    <div>
                                        <p className="text-lg font-bold text-accent-dark-green leading-none">{supporter.firstName} {supporter.lastName}</p>
                                        <p className="text-accent-mid-green text-xs mt-1 font-medium">Active
                                            Supporter</p>
                                    </div>
                                </div>
                                <div className="flex gap-2">
                                    {removingSupporterId === supporter.id ? (
                                        <div
                                            className="flex items-center gap-2 animate-in fade-in zoom-in-95 duration-200">
                                            <span className="text-[10px] font-bold text-red-600 uppercase">Sure?</span>
                                            <button onClick={() => setRemovingSupporterId(null)}
                                                    className="text-[10px] font-bold text-gray-400 uppercase">No
                                            </button>
                                            <button onClick={() => handleRemove(supporter.id)}
                                                    className="text-[10px] font-bold text-red-600 uppercase underline">Yes
                                            </button>
                                        </div>
                                    ) : banningSupporterId === supporter.id ? (
                                        <div
                                            className="flex items-center gap-2 animate-in fade-in zoom-in-95 duration-200">
                                            <span className="text-[10px] font-bold text-red-600 uppercase">BAN?</span>
                                            <button onClick={() => setBanningSupporterId(null)}
                                                    className="text-[10px] font-bold text-gray-400 uppercase">No
                                            </button>
                                            <button onClick={() => handleBan(supporter.id)}
                                                    className="text-[10px] font-bold text-red-600 uppercase underline">Yes
                                            </button>
                                        </div>
                                    ) : (
                                        <>
                                            <button
                                                onClick={() => setRemovingSupporterId(supporter.id)}
                                                className="px-4 py-2 rounded-xl text-sm font-bold text-accent-mid-green hover:bg-accent-light-green/50 border border-accent-mid-green/30 transition-colors"
                                            >
                                                Remove
                                            </button>
                                            <button
                                                onClick={() => setBanningSupporterId(supporter.id)}
                                                className="px-4 py-2 rounded-xl text-sm font-bold text-red-500 hover:bg-red-50 border border-red-100 transition-colors"
                                            >
                                                Ban
                                            </button>
                                        </>
                                    )}
                                </div>
                            </div>
                        ))
                    )}
                </div>
            )}
        </div>
    );
};

export default SupporterList;
