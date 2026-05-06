import React from 'react';

const ImageModal = ({isOpen, onClose, imageUrl}) => {
    if (!isOpen || !imageUrl) return null;

    return (
        <div
            className="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/90 backdrop-blur-sm animate-in fade-in duration-200"
            onClick={onClose}
        >
            <button
                onClick={onClose}
                className="absolute top-4 right-4 text-white hover:text-gray-300 transition-colors z-[110] bg-black/40 rounded-full p-2"
                aria-label="Close"
            >
                <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none"
                     stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                    <line x1="18" y1="6" x2="6" y2="18"></line>
                    <line x1="6" y1="6" x2="18" y2="18"></line>
                </svg>
            </button>

            <div
                className="relative max-w-full max-h-full flex items-center justify-center animate-in zoom-in-95 duration-200"
                onClick={(e) => e.stopPropagation()}
            >
                <img
                    src={imageUrl}
                    alt="Full size"
                    className="max-w-[95vw] max-h-[95vh] object-contain rounded-lg shadow-2xl cursor-default"
                />
            </div>
        </div>
    );
};

export default ImageModal;
