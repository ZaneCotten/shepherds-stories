import React, {useState, useEffect} from "react";
import {CommentSection} from "../components/CommentSection";
import {MediaCarousel} from "../components/MediaCarousel";

// --- Main PostFeed Component ---
export const PostFeed = ({posts, setPosts}) => {
    const [newPostTitle, setNewPostTitle] = useState("");
    const [newPostContent, setNewPostContent] = useState("");
    const [attachedFiles, setAttachedFiles] = useState([]);
    const [previews, setPreviews] = useState([]);
    const [postLoading, setPostLoading] = useState(false);
    const [error, setError] = useState(null);

    useEffect(() => {
        if (attachedFiles.length === 0) {
            setPreviews([]);
            return;
        }
        const urls = attachedFiles.map(file => ({
            url: URL.createObjectURL(file),
            type: file.type
        }));
        setPreviews(urls);
        return () => urls.forEach(p => URL.revokeObjectURL(p.url));
    }, [attachedFiles]);

    const handleFileChange = (e) => {
        setAttachedFiles(prev => [...prev, ...Array.from(e.target.files)]);
    };

    const uploadFile = async (file) => {
        const urlParams = new URLSearchParams({fileName: file.name, contentType: file.type});
        const urlResponse = await fetch(`/api/posts/upload-url?${urlParams.toString()}`);
        if (!urlResponse.ok) throw new Error("Upload setup failed");
        const {url, s3Key} = await urlResponse.json();

        const uploadResponse = await fetch(url, {
            method: 'PUT',
            body: file,
            headers: {'Content-Type': file.type}
        });
        if (!uploadResponse.ok) throw new Error("S3 Upload failed");

        let mediaType = "DOCUMENT";
        if (file.type.startsWith("image/")) mediaType = "IMAGE";
        else if (file.type.startsWith("video/")) mediaType = "VIDEO";

        return {s3Key, fileName: file.name, mediaType, orderNumber: 0};
    };

    const handleCreatePost = async (e) => {
        e.preventDefault();
        setError(null);
        const trimmedTitle = newPostTitle.trim();
        const trimmedContent = newPostContent.trim();

        if (!trimmedTitle) {
            setError("Title is mandatory.");
            return;
        }

        // Validate that either content or media is present
        if (!trimmedContent && attachedFiles.length === 0) {
            setError("Post must contain either a message or media.");
            return;
        }

        setPostLoading(true);
        try {
            let uploadedMedia = [];
            if (attachedFiles.length > 0) {
                uploadedMedia = await Promise.all(attachedFiles.map(file => uploadFile(file)));
            }

            const response = await fetch("/api/posts", {
                method: "POST",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify({
                    title: newPostTitle,
                    content: newPostContent,
                    media: uploadedMedia
                })
            });

            if (response.ok) {
                const data = await response.json();
                setPosts([data, ...posts]);
                setNewPostTitle("");
                setNewPostContent("");
                setAttachedFiles([]);
            }
        } catch (err) {
            console.error("Post error:", err);
            setError("An error occurred while creating the post.");
        } finally {
            setPostLoading(false);
        }
    };

    return (
        <div className="w-full max-w-3xl">
            {/* Create Post Card */}
            <div className="bg-white p-8 rounded-2xl shadow-lg border border-accent-mid-green mb-12">
                {/* Red Error Message */}
                {error && (
                    <div
                        className="mb-4 p-3 bg-red-50 border border-red-200 text-red-600 rounded-xl text-sm font-bold flex items-center gap-2">
                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none"
                             stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
                            <circle cx="12" cy="12" r="10"></circle>
                            <line x1="12" y1="8" x2="12" y2="12"></line>
                            <line x1="12" y1="16" x2="12.01" y2="16"></line>
                        </svg>
                        {error}
                    </div>
                )}

                <h2 className="text-2xl font-bold text-accent-dark-green mb-6">Create an Update</h2>

                <form onSubmit={handleCreatePost} className="flex flex-col gap-4">
                    <input
                        type="text" placeholder="Update Title" value={newPostTitle}
                        onChange={(e) => setNewPostTitle(e.target.value)}
                        className="p-3 rounded-xl border border-accent-mid-green bg-white text-accent-dark-green focus:outline-none focus:ring-2 focus:ring-accent-light-green transition-all"
                    />
                    <textarea
                        placeholder="Share what's happening in your ministry..." value={newPostContent}
                        onChange={(e) => setNewPostContent(e.target.value)}
                        className="p-3 rounded-xl border border-accent-mid-green bg-white text-accent-dark-green min-h-[120px] focus:outline-none focus:ring-2 focus:ring-accent-light-green transition-all outline-none"
                    />

                    {/* Preview Carousel */}
                    {previews.length > 0 && (
                        <div className="mb-4">
                            <div className="flex justify-between items-center mb-2">
                                <span className="text-xs font-bold text-accent-mid-green">Media Preview</span>
                                <button type="button" onClick={() => setAttachedFiles([])}
                                        className="text-xs text-red-500 font-bold hover:underline">Clear All
                                </button>
                            </div>
                            <MediaCarousel media={previews} isPreview={true}/>
                        </div>
                    )}

                    <label
                        className="p-4 rounded-xl border-2 border-dashed border-accent-mid-green bg-accent-light-green/30 text-accent-dark-green text-center font-bold cursor-pointer hover:bg-accent-light-green/50 transition-colors">
                        <div className="flex items-center justify-center gap-2">
                            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24"
                                 fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"
                                 strokeLinejoin="round">
                                <rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect>
                                <circle cx="8.5" cy="8.5" r="1.5"></circle>
                                <polyline points="21 15 16 10 5 21"></polyline>
                            </svg>
                            Add Photos or Video
                        </div>
                        <input type="file" multiple onChange={handleFileChange} className="hidden"
                               accept="image/*,video/*"/>
                    </label>

                    <button
                        className="w-full py-4 rounded-xl font-bold text-white bg-accent-mid-green hover:bg-accent-dark-green transition-all shadow-md active:scale-[0.98]">
                        {postLoading ? "Uploading..." : "Post Update"}
                    </button>
                </form>
            </div>

            {/* Post Feed List */}
            <div className="flex flex-col gap-8">
                {posts.map((post) => (
                    <div key={post.id} className="bg-white p-8 rounded-2xl shadow-lg border border-accent-light-green">
                        <div className="flex justify-between items-start mb-6">
                            <div className="flex-1">
                                {post.title &&
                                    <h3 className="text-2xl font-bold text-accent-dark-green mb-1">{post.title}</h3>}
                                <p className="text-accent-mid-green font-semibold text-sm">
                                    By {post.authorName}
                                </p>
                            </div>
                            <div className="text-right">
                                <p className="text-gray-400 text-[10px] font-bold uppercase tracking-wider">
                                    {new Date(post.createdAt).toLocaleString([], {
                                        dateStyle: 'medium',
                                        timeStyle: 'short'
                                    })}
                                </p>
                            </div>
                        </div>

                        <p className="text-gray-700 whitespace-pre-wrap mb-6 leading-relaxed">{post.content}</p>

                        {/* THE CAROUSEL FOR SAVED POSTS */}
                        {post.media?.length > 0 && (
                            <div className="mb-8">
                                <MediaCarousel media={post.media} isPreview={false}/>
                            </div>
                        )}

                        <CommentSection postId={post.id} postAuthorId={post.authorId}/>
                    </div>
                ))}
            </div>
        </div>
    );
};

export default PostFeed;