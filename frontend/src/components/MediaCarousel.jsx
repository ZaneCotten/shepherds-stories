import React, {useState} from "react";

export const MediaCarousel = ({media, isPreview = false}) => {
    const [currentIndex, setCurrentIndex] = useState(0);

    if (!media || media.length === 0) return null;

    const next = (e) => {
        e.stopPropagation();
        setCurrentIndex((prev) => (prev + 1 === media.length ? 0 : prev + 1));
    };

    const prev = (e) => {
        e.stopPropagation();
        setCurrentIndex((prev) => (prev === 0 ? media.length - 1 : prev - 1));
    };

    const currentItem = media[currentIndex];

    return (
        <div className="relative w-full aspect-video bg-black rounded-xl overflow-hidden group">
            {/* The Media Item */}
            <div className="w-full h-full flex items-center justify-center">
                {isPreview ? (
                    // Rendering for File Objects (Previews)
                    currentItem.type?.startsWith("video/") ? (
                        <video src={currentItem.url} className="max-h-full"/>
                    ) : (
                        <img src={currentItem.url} className="w-full h-full object-cover" alt="preview"/>
                    )
                ) : (
                    // Rendering for Saved S3 Keys (Live Posts)
                    currentItem.mediaType === "VIDEO" ? (
                        <video src={currentItem.url} controls className="max-h-full"/>
                    ) : (
                        <img src={currentItem.url} className="w-full h-full object-cover"
                             alt="content"/>
                    )
                )}
            </div>

            {/* Navigation Arrows (Only show if more than 1 item) */}
            {media.length > 1 && (
                <>
                    <button
                        onClick={prev}
                        className="absolute left-2 top-1/2 -translate-y-1/2 bg-white/20 hover:bg-white/40 text-white w-10 h-10 rounded-full backdrop-blur-md transition-all opacity-0 group-hover:opacity-100 flex items-center justify-center font-bold text-xl"
                    >
                        &#10094;
                    </button>
                    <button
                        onClick={next}
                        className="absolute right-2 top-1/2 -translate-y-1/2 bg-white/20 hover:bg-white/40 text-white w-10 h-10 rounded-full backdrop-blur-md transition-all opacity-0 group-hover:opacity-100 flex items-center justify-center font-bold text-xl"
                    >
                        &#10095;
                    </button>

                    {/* Dot Indicators */}
                    <div className="absolute bottom-4 left-1/2 -translate-x-1/2 flex gap-1.5">
                        {media.map((_, i) => (
                            <div
                                key={i}
                                className={`w-2 h-2 rounded-full transition-all ${i === currentIndex ? "bg-white scale-125" : "bg-white/40"}`}
                            />
                        ))}
                    </div>
                </>
            )}
        </div>
    );
};
