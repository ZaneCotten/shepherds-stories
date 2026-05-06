import React, {useState} from 'react';
import ProfileModal from "../components/ProfileModal.jsx";

export const ConnectionRequests = ({requests, setRequests}) => {
    const [error, setError] = useState("");
    const [selectedUser, setSelectedUser] = useState(null);
    const handleRespond = async (requestId, approve) => {
        setError("");
        try {
            const response = await fetch(`/api/missionary/requests/${requestId}/respond?approve=${approve}`, {
                method: 'POST'
            });
            if (response.ok) {
                setRequests(requests.filter(req => req.id !== requestId));
            } else {
                setError("Failed to respond to request.");
            }
        } catch {
            setError("Error responding.");
        }
    };

    return (
        <div className="w-full max-w-2xl animate-in fade-in slide-in-from-bottom-4 duration-500">
            <h2 className="text-2xl font-bold mb-6 text-accent-dark-green">Pending Connection Requests</h2>
            {error && <p className="text-red-500 text-sm mb-4 font-bold">{error}</p>}
            {requests.length === 0 ? (
                <div
                    className="bg-white p-12 rounded-2xl text-center border border-dashed border-accent-mid-green text-gray-500 italic shadow-sm">
                    No new requests.
                </div>
            ) : (
                <div className="flex flex-col gap-4">
                    {requests.map((req) => (
                        <div key={req.id}
                             className="flex flex-col sm:flex-row justify-between sm:items-center gap-4 p-6 bg-white rounded-2xl border border-accent-light-green shadow-lg hover:shadow-xl transition-shadow">
                            <span
                                className="text-lg font-bold text-accent-dark-green cursor-pointer hover:underline"
                                onClick={() => setSelectedUser({
                                    userName: req.supporterName,
                                    profilePictureUrl: req.profilePictureUrl,
                                    role: 'SUPPORTER'
                                })}
                            >
                                {req.supporterName}
                            </span>
                            <div className="flex flex-col sm:flex-row gap-3 w-full sm:w-auto">
                                <button onClick={() => handleRespond(req.id, true)}
                                        className="w-full sm:w-auto px-6 py-2 rounded-xl bg-accent-mid-green text-white text-sm font-bold hover:bg-accent-dark-green transition shadow-md active:scale-95">Approve
                                </button>
                                <button onClick={() => handleRespond(req.id, false)}
                                        className="px-6 py-2 w-full sm:w-auto rounded-xl border border-red-200 text-red-500 text-sm font-bold hover:bg-red-50 transition active:scale-95">Deny
                                </button>
                            </div>
                        </div>
                    ))}
                </div>
            )}
            <ProfileModal
                isOpen={!!selectedUser}
                onClose={() => setSelectedUser(null)}
                user={selectedUser}
            />
        </div>
    );
};

export default ConnectionRequests;