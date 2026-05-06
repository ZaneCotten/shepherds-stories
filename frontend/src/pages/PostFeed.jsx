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

    const [editingPostId, setEditingPostId] = useState(null);
    const [editTitle, setEditTitle] = useState("");
    const [editContent, setEditContent] = useState("");
    const [editExistingMedia, setEditExistingMedia] = useState([]);
    const [editNewFiles, setEditNewFiles] = useState([]);
    const [editPreviews, setEditPreviews] = useState([]);

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

    useEffect(() => {
        if (editNewFiles.length === 0) {
            setEditPreviews([]);
            return;
        }
        const urls = editNewFiles.map(file => ({
            url: URL.createObjectURL(file),
            type: file.type
        }));
        setEditPreviews(urls);
        return () => urls.forEach(p => URL.revokeObjectURL(p.url));
    }, [editNewFiles]);

    const handleFileChange = (e) => {
        setAttachedFiles(prev => [...prev, ...Array.from(e.target.files)]);
    };

    const handleEditFileChange = (e) => {
        setEditNewFiles(prev => [...prev, ...Array.from(e.target.files)]);
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

    const handleEditPost = (post) => {
        setEditingPostId(post.id);
        setEditTitle(post.title);
        setEditContent(post.content);
        setEditExistingMedia(post.media || []);
        setEditNewFiles([]);
        setError(null);
    };

    const handleCancelEdit = () => {
        setEditingPostId(null);
        setEditTitle("");
        setEditContent("");
        setEditExistingMedia([]);
        setEditNewFiles([]);
        setError(null);
    };

    const handleRemoveExistingMedia = (mediaId) => {
        setEditExistingMedia(prev => prev.filter(m => m.id !== mediaId));
    };

    const handleUpdatePost = async (e) => {
        e.preventDefault();
        setError(null);
        const trimmedTitle = editTitle.trim();
        const trimmedContent = editContent.trim();

        if (!trimmedTitle) {
            setError("Title is mandatory.");
            return;
        }

        if (!trimmedContent && editExistingMedia.length === 0 && editNewFiles.length === 0) {
            setError("Post must contain either a message or media.");
            return;
        }

        setPostLoading(true);
        try {
            let newlyUploadedMedia = [];
            if (editNewFiles.length > 0) {
                newlyUploadedMedia = await Promise.all(editNewFiles.map(file => uploadFile(file)));
            }

            const allMedia = [...editExistingMedia, ...newlyUploadedMedia];

            const response = await fetch(`/api/posts/${editingPostId}`, {
                method: "PUT",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify({
                    title: editTitle,
                    content: editContent,
                    media: allMedia
                })
            });

            if (response.ok) {
                const data = await response.json();
                setPosts(posts.map(p => p.id === editingPostId ? data : p));
                handleCancelEdit();
            } else {
                const errorData = await response.json().catch(() => ({}));
                setError(errorData.message || "Failed to update post.");
            }
        } catch (err) {
            console.error("Update error:", err);
            setError("An error occurred while updating the post.");
        } finally {
            setPostLoading(false);
        }
    };

    const handleDeletePost = async (postId) => {
        if (!window.confirm("Are you sure you want to delete this post?")) return;

        try {
            const response = await fetch(`/api/posts/${postId}`, {
                method: "DELETE"
            });

            if (response.ok) {
                setPosts(posts.filter(p => p.id !== postId));
            } else {
                alert("Failed to delete post.");
            }
        } catch (err) {
            console.error("Delete error:", err);
            alert("An error occurred while deleting the post.");
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
                        {editingPostId === post.id ? (
                            <form onSubmit={handleUpdatePost} className="flex flex-col gap-4">
                                <input
                                    type="text" placeholder="Update Title" value={editTitle}
                                    onChange={(e) => setEditTitle(e.target.value)}
                                    className="p-3 rounded-xl border border-accent-mid-green bg-white text-accent-dark-green focus:outline-none focus:ring-2 focus:ring-accent-light-green transition-all"
                                />
                                <textarea
                                    placeholder="Share what's happening in your ministry..." value={editContent}
                                    onChange={(e) => setEditContent(e.target.value)}
                                    className="p-3 rounded-xl border border-accent-mid-green bg-white text-accent-dark-green min-h-[120px] focus:outline-none focus:ring-2 focus:ring-accent-light-green transition-all outline-none"
                                />

                                {error && editingPostId === post.id && (
                                    <div
                                        className="p-3 rounded-xl bg-red-50 border border-red-100 text-red-600 text-sm font-medium">
                                        {error}
                                    </div>
                                )}

                                {/* Existing Media Management */}
                                {editExistingMedia.length > 0 && (
                                    <div className="mb-4">
                                        <p className="text-xs font-bold text-accent-mid-green mb-2">Existing Media
                                            (Click to remove)</p>
                                        <div className="flex flex-wrap gap-2">
                                            {editExistingMedia.map((m) => (
                                                <div key={m.id}
                                                     className="relative w-20 h-20 rounded-lg overflow-hidden border border-accent-light-green group cursor-pointer"
                                                     onClick={() => handleRemoveExistingMedia(m.id)}>
                                                    {m.mediaType === "VIDEO" ? (
                                                        <div
                                                            className="w-full h-full bg-gray-100 flex items-center justify-center">
                                                            <svg xmlns="http://www.w3.org/2000/svg" width="24"
                                                                 height="24" viewBox="0 0 24 24" fill="none"
                                                                 stroke="currentColor" strokeWidth="2"
                                                                 strokeLinecap="round" strokeLinejoin="round">
                                                                <polygon points="5 3 19 12 5 21 5 3"></polygon>
                                                            </svg>
                                                        </div>
                                                    ) : (
                                                        <img src={m.url} className="w-full h-full object-cover"
                                                             alt="existing"/>
                                                    )}
                                                    <div
                                                        className="absolute inset-0 bg-red-500/40 opacity-0 group-hover:opacity-100 flex items-center justify-center transition-opacity">
                                                        <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20"
                                                             viewBox="0 0 24 24" fill="none" stroke="white"
                                                             strokeWidth="3" strokeLinecap="round"
                                                             strokeLinejoin="round">
                                                            <line x1="18" y1="6" x2="6" y2="18"></line>
                                                            <line x1="6" y1="6" x2="18" y2="18"></line>
                                                        </svg>
                                                    </div>
                                                </div>
                                            ))}
                                        </div>
                                    </div>
                                )}

                                {/* New Media Previews */}
                                {editPreviews.length > 0 && (
                                    <div className="mb-4">
                                        <div className="flex justify-between items-center mb-2">
                                            <span
                                                className="text-xs font-bold text-accent-mid-green">New Media Preview</span>
                                            <button type="button" onClick={() => setEditNewFiles([])}
                                                    className="text-xs text-red-500 font-bold hover:underline">Clear New
                                            </button>
                                        </div>
                                        <MediaCarousel media={editPreviews} isPreview={true}/>
                                    </div>
                                )}

                                <label
                                    className="p-4 rounded-xl border-2 border-dashed border-accent-mid-green bg-accent-light-green/30 text-accent-dark-green text-center font-bold cursor-pointer hover:bg-accent-light-green/50 transition-colors">
                                    <div className="flex items-center justify-center gap-2">
                                        <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20"
                                             viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"
                                             strokeLinecap="round" strokeLinejoin="round">
                                            <rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect>
                                            <circle cx="8.5" cy="8.5" r="1.5"></circle>
                                            <polyline points="21 15 16 10 5 21"></polyline>
                                        </svg>
                                        Add More Photos or Video
                                    </div>
                                    <input type="file" multiple onChange={handleEditFileChange} className="hidden"
                                           accept="image/*,video/*"/>
                                </label>

                                <div className="flex gap-4">
                                    <button type="submit"
                                            className="flex-1 py-3 rounded-xl font-bold text-white bg-accent-mid-green hover:bg-accent-dark-green transition-all shadow-md">
                                        {postLoading ? "Saving..." : "Save Changes"}
                                    </button>
                                    <button type="button" onClick={handleCancelEdit}
                                            className="px-6 py-3 rounded-xl font-bold text-gray-500 bg-gray-100 hover:bg-gray-200 transition-all">
                                        Cancel
                                    </button>
                                </div>
                            </form>
                        ) : (
                            <>
                                <div className="flex justify-between items-start mb-6">
                                    <div className="flex-1">
                                        <div className="flex items-center gap-3 mb-1">
                                            {post.title &&
                                                <h3 className="text-2xl font-bold text-accent-dark-green">{post.title}</h3>}
                                            <div className="flex gap-2">
                                                <button onClick={() => handleEditPost(post)}
                                                        className="p-1.5 rounded-lg text-gray-400 hover:text-accent-mid-green hover:bg-accent-light-green/30 transition-all"
                                                        title="Edit Post">
                                                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16"
                                                         viewBox="0 0 24 24" fill="none" stroke="currentColor"
                                                         strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                                                        <path
                                                            d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                                                        <path
                                                            d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                                                    </svg>
                                                </button>
                                                <button onClick={() => handleDeletePost(post.id)}
                                                        className="p-1.5 rounded-lg text-gray-400 hover:text-red-500 hover:bg-red-50 transition-all"
                                                        title="Delete Post">
                                                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16"
                                                         viewBox="0 0 24 24" fill="none" stroke="currentColor"
                                                         strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                                                        <polyline points="3 6 5 6 21 6"></polyline>
                                                        <path
                                                            d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                                                        <line x1="10" y1="11" x2="10" y2="17"></line>
                                                        <line x1="14" y1="11" x2="14" y2="17"></line>
                                                    </svg>
                                                </button>
                                            </div>
                                        </div>
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
                                        {post.updatedAt && new Date(post.updatedAt).getTime() - new Date(post.createdAt).getTime() > 60000 && (
                                            <p className="text-gray-400 text-[10px] font-bold uppercase tracking-wider mt-0.5">
                                                Updated at {new Date(post.updatedAt).toLocaleString([], {
                                                dateStyle: 'medium',
                                                timeStyle: 'short'
                                            })}
                                            </p>
                                        )}
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
                            </>
                        )}
                    </div>
                ))}
            </div>
        </div>
    );
};

export default PostFeed;