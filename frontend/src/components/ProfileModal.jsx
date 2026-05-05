import React from 'react';

const ProfileModal = ({isOpen, onClose, user}) => {
    if (!isOpen || !user) return null;

    const isMissionary = user.role === 'MISSIONARY' || user.userRole === 'MISSIONARY' || user.authorRole === 'MISSIONARY' || !!user.missionaryName;

    return (
        <div
            className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-in fade-in duration-200"
            onClick={onClose}>
            <div
                className="bg-white rounded-2xl overflow-hidden shadow-2xl max-w-md w-full relative animate-in zoom-in-95 duration-200"
                onClick={(e) => e.stopPropagation()}
            >
                {/* Close Button */}
                <button
                    onClick={onClose}
                    className="absolute top-4 right-4 text-gray-400 hover:text-gray-600 transition-colors z-10 bg-white/80 rounded-full p-1 shadow-sm"
                >
                    <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none"
                         stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <line x1="18" y1="6" x2="6" y2="18"></line>
                        <line x1="6" y1="6" x2="18" y2="18"></line>
                    </svg>
                </button>

                {/* Profile Picture */}
                <div className="aspect-square w-full overflow-hidden bg-gray-100 flex items-center justify-center">
                    {user.profilePictureUrl || user.userProfilePictureUrl || user.authorProfilePictureUrl ? (
                        <img
                            src={user.profilePictureUrl || user.userProfilePictureUrl || user.authorProfilePictureUrl}
                            alt={user.missionaryName || user.userName || user.authorName || (user.firstName && `${user.firstName} ${user.lastName}`) || "Profile"}
                            className="w-full h-full object-cover"
                        />
                    ) : (
                        <div className="w-full h-full flex items-center justify-center bg-accent-light-green/30">
                            <svg xmlns="http://www.w3.org/2000/svg" width="80" height="80" viewBox="0 0 24 24"
                                 fill="none" stroke="#2D5A27" strokeWidth="1" strokeLinecap="round"
                                 strokeLinejoin="round">
                                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                                <circle cx="12" cy="7" r="4"></circle>
                            </svg>
                        </div>
                    )}
                </div>

                {/* Info Section */}
                <div className="p-6">
                    <h2 className="text-2xl font-bold text-accent-dark-green mb-1">
                        {user.missionaryName || user.userName || user.authorName || (user.firstName && `${user.firstName} ${user.lastName}`) || "Unknown User"}
                    </h2>

                    {isMissionary ? (
                        <>
                            <p className="text-accent-mid-green font-semibold flex items-center gap-1 mb-3">
                                <span>📍 {user.locationRegion || user.userLocationRegion || user.authorLocationRegion || "Global"}</span>
                                <span className="text-gray-300 mx-1">•</span>
                                <span
                                    className="uppercase text-[10px] font-bold tracking-widest bg-accent-mid-green/10 px-2 py-0.5 rounded">Missionary</span>
                            </p>
                            {(user.biography || user.userBiography || user.authorBiography) && (
                                <div className="bg-gray-50 rounded-xl p-4 border border-gray-100">
                                    <p className="text-gray-700 text-sm leading-relaxed italic">
                                        "{user.biography || user.userBiography || user.authorBiography}"
                                    </p>
                                </div>
                            )}
                        </>
                    ) : (
                        <p className="text-gray-500 font-medium uppercase text-[10px] font-bold tracking-widest bg-gray-100 px-2 py-0.5 rounded inline-block">
                            Supporter
                        </p>
                    )}
                </div>
            </div>
        </div>
    );
};

export default ProfileModal;
