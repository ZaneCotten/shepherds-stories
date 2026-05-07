import React, {useState, useEffect} from 'react';

export const PrayerRequestManager = () => {
    const [prayerRequests, setPrayerRequests] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    const [isCreating, setIsCreating] = useState(false);
    const [editingId, setEditingId] = useState(null);

    const [formData, setFormData] = useState({
        title: "",
        content: "",
        isAnswered: false
    });

    const fetchPrayerRequests = async () => {
        try {
            const res = await fetch("/api/missionary/prayer-requests");
            if (res.ok) {
                const data = await res.json();
                setPrayerRequests(data);
            } else {
                setError("Failed to fetch prayer requests.");
            }
        } catch (err) {
            setError("Error fetching prayer requests.");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchPrayerRequests();
    }, []);

    const handleInputChange = (e) => {
        const {name, value, type, checked} = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: type === 'checkbox' ? checked : value
        }));
    };

    const resetForm = () => {
        setFormData({title: "", content: "", isAnswered: false});
        setIsCreating(false);
        setEditingId(null);
        setError("");
    };

    const handleCreate = async (e) => {
        e.preventDefault();
        setError("");
        if (!formData.title.trim() || !formData.content.trim()) {
            setError("Title and content are required.");
            return;
        }

        try {
            const res = await fetch("/api/missionary/prayer-requests", {
                method: "POST",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify(formData)
            });
            if (res.ok) {
                await fetchPrayerRequests();
                resetForm();
            } else {
                setError("Failed to create prayer request.");
            }
        } catch (err) {
            setError("Error creating prayer request.");
        }
    };

    const handleUpdate = async (e) => {
        e.preventDefault();
        setError("");
        if (!formData.title.trim() || !formData.content.trim()) {
            setError("Title and content are required.");
            return;
        }

        try {
            const res = await fetch(`/api/missionary/prayer-requests/${editingId}`, {
                method: "PUT",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify(formData)
            });
            if (res.ok) {
                await fetchPrayerRequests();
                resetForm();
            } else {
                setError("Failed to update prayer request.");
            }
        } catch (err) {
            setError("Error updating prayer request.");
        }
    };

    const handleDelete = async (id) => {
        if (!window.confirm("Are you sure you want to delete this prayer request?")) return;
        try {
            const res = await fetch(`/api/missionary/prayer-requests/${id}`, {
                method: "DELETE"
            });
            if (res.ok) {
                setPrayerRequests(prev => prev.filter(r => r.id !== id));
            } else {
                setError("Failed to delete prayer request.");
            }
        } catch (err) {
            setError("Error deleting prayer request.");
        }
    };

    const toggleAnswered = async (request) => {
        try {
            const res = await fetch(`/api/missionary/prayer-requests/${request.id}`, {
                method: "PUT",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify({
                    ...request,
                    isAnswered: !request.isAnswered
                })
            });
            if (res.ok) {
                const updated = await res.json();
                setPrayerRequests(prev => prev.map(r => r.id === request.id ? updated : r));
            }
        } catch (err) {
            setError("Error updating prayer request status.");
        }
    };

    const startEdit = (request) => {
        setEditingId(request.id);
        setFormData({
            title: request.title,
            content: request.content,
            isAnswered: request.isAnswered
        });
        setIsCreating(false);
    };

    if (loading) return <div className="text-center py-10 text-accent-mid-green font-bold">Loading prayer
        requests...</div>;

    return (
        <div className="w-full max-w-3xl animate-in fade-in slide-in-from-bottom-4 duration-500">
            <div className="flex justify-between items-center mb-8">
                <h2 className="text-2xl font-bold text-accent-dark-green">Prayer Requests</h2>
                {!isCreating && !editingId && (
                    <button
                        onClick={() => setIsCreating(true)}
                        className="px-6 py-2 bg-accent-mid-green text-white rounded-xl font-bold hover:bg-accent-dark-green transition shadow-md flex items-center gap-2"
                    >
                        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none"
                             stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
                            <line x1="12" y1="5" x2="12" y2="19"></line>
                            <line x1="5" y1="12" x2="19" y2="12"></line>
                        </svg>
                        New Request
                    </button>
                )}
            </div>

            {error && (
                <div
                    className="mb-6 p-4 bg-red-50 border border-red-200 text-red-600 rounded-xl text-sm font-bold flex items-center gap-2">
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none"
                         stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
                        <circle cx="12" cy="12" r="10"></circle>
                        <line x1="12" y1="8" x2="12" y2="12"></line>
                        <line x1="12" y1="16" x2="12.01" y2="16"></line>
                    </svg>
                    {error}
                </div>
            )}

            {(isCreating || editingId) && (
                <div
                    className="bg-white p-8 rounded-2xl shadow-lg border border-accent-mid-green mb-12 animate-in zoom-in-95 duration-200">
                    <h3 className="text-xl font-bold text-accent-dark-green mb-6">
                        {editingId ? "Edit Prayer Request" : "New Prayer Request"}
                    </h3>
                    <form onSubmit={editingId ? handleUpdate : handleCreate} className="flex flex-col gap-4">
                        <div>
                            <label
                                className="text-xs font-black text-accent-mid-green uppercase tracking-widest mb-1 block">Title</label>
                            <input
                                type="text"
                                name="title"
                                value={formData.title}
                                onChange={handleInputChange}
                                placeholder="Request Title"
                                className="w-full p-3 rounded-xl border border-accent-mid-green bg-white text-accent-dark-green focus:outline-none focus:ring-2 focus:ring-accent-light-green transition-all"
                            />
                        </div>
                        <div>
                            <label
                                className="text-xs font-black text-accent-mid-green uppercase tracking-widest mb-1 block">Content</label>
                            <textarea
                                name="content"
                                value={formData.content}
                                onChange={handleInputChange}
                                placeholder="Details of your prayer request..."
                                className="w-full p-3 rounded-xl border border-accent-mid-green bg-white text-accent-dark-green min-h-[120px] focus:outline-none focus:ring-2 focus:ring-accent-light-green transition-all"
                            />
                        </div>
                        <div className="flex items-center gap-3 py-2">
                            <input
                                type="checkbox"
                                id="isAnswered"
                                name="isAnswered"
                                checked={formData.isAnswered}
                                onChange={handleInputChange}
                                className="w-5 h-5 accent-accent-mid-green rounded border-gray-300 focus:ring-accent-mid-green"
                            />
                            <label htmlFor="isAnswered"
                                   className="text-sm font-bold text-accent-dark-green cursor-pointer">
                                Mark as Answered
                            </label>
                        </div>
                        <div className="flex gap-4 mt-2">
                            <button
                                type="submit"
                                className="flex-1 py-3 bg-accent-mid-green text-white rounded-xl font-bold hover:bg-accent-dark-green transition shadow-md"
                            >
                                {editingId ? "Save Changes" : "Create Request"}
                            </button>
                            <button
                                type="button"
                                onClick={resetForm}
                                className="px-6 py-3 bg-gray-100 text-gray-500 rounded-xl font-bold hover:bg-gray-200 transition"
                            >
                                Cancel
                            </button>
                        </div>
                    </form>
                </div>
            )}

            <div className="flex flex-col gap-6">
                {prayerRequests.length === 0 ? (
                    <div
                        className="bg-white p-12 rounded-2xl text-center border border-dashed border-accent-mid-green text-gray-500 italic shadow-sm">
                        You haven't created any prayer requests yet.
                    </div>
                ) : (
                    prayerRequests.map((request) => (
                        <div key={request.id}
                             className={`bg-white p-8 rounded-2xl shadow-lg border ${request.isAnswered ? 'border-gray-200 opacity-75' : 'border-accent-light-green'}`}>
                            <div className="flex justify-between items-start mb-4">
                                <div className="flex-1">
                                    <div className="flex items-center gap-3 mb-1">
                                        <h3 className={`text-xl font-bold ${request.isAnswered ? 'text-gray-500 line-through' : 'text-accent-dark-green'}`}>
                                            {request.title}
                                        </h3>
                                        {request.isAnswered && (
                                            <span
                                                className="px-2 py-0.5 bg-accent-mid-green/10 text-accent-mid-green text-[10px] font-bold uppercase tracking-widest rounded">
                                                Answered
                                            </span>
                                        )}
                                    </div>
                                    <p className="text-gray-400 text-[10px] font-bold uppercase tracking-wider">
                                        Created {new Date(request.createdAt).toLocaleDateString([], {dateStyle: 'medium'})}
                                    </p>
                                </div>
                                <div className="flex gap-2">
                                    <button
                                        onClick={() => toggleAnswered(request)}
                                        className={`p-1.5 rounded-lg transition-all ${request.isAnswered ? 'text-accent-mid-green hover:bg-accent-light-green/30' : 'text-gray-400 hover:text-accent-mid-green hover:bg-accent-light-green/30'}`}
                                        title={request.isAnswered ? "Mark as Unanswered" : "Mark as Answered"}
                                    >
                                        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18"
                                             viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5"
                                             strokeLinecap="round" strokeLinejoin="round">
                                            {request.isAnswered ? (
                                                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                                            ) : (
                                                <circle cx="12" cy="12" r="10"></circle>
                                            )}
                                            <polyline points="22 4 12 14.01 9 11.01"></polyline>
                                        </svg>
                                    </button>
                                    <button
                                        onClick={() => startEdit(request)}
                                        className="p-1.5 rounded-lg text-gray-400 hover:text-accent-mid-green hover:bg-accent-light-green/30 transition-all"
                                        title="Edit"
                                    >
                                        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18"
                                             viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5"
                                             strokeLinecap="round" strokeLinejoin="round">
                                            <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                                            <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                                        </svg>
                                    </button>
                                    <button
                                        onClick={() => handleDelete(request.id)}
                                        className="p-1.5 rounded-lg text-gray-400 hover:text-red-500 hover:bg-red-50 transition-all"
                                        title="Delete"
                                    >
                                        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18"
                                             viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5"
                                             strokeLinecap="round" strokeLinejoin="round">
                                            <polyline points="3 6 5 6 21 6"></polyline>
                                            <path
                                                d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                                        </svg>
                                    </button>
                                </div>
                            </div>
                            <p className={`whitespace-pre-wrap leading-relaxed ${request.isAnswered ? 'text-gray-400' : 'text-gray-700'}`}>
                                {request.content}
                            </p>
                        </div>
                    ))
                )}
            </div>
        </div>
    );
};

export default PrayerRequestManager;
