import {useState, useEffect} from "react";
import ProfileModal from "./ProfileModal.jsx";

export const CommentSection = ({postId, postAuthorId}) => {
    const [comments, setComments] = useState([]);
    const [newComment, setNewComment] = useState("");
    const [loading, setLoading] = useState(true);
    const [submitting, setSubmitting] = useState(false);
    const [editingCommentId, setEditingCommentId] = useState(null);
    const [editContent, setEditContent] = useState("");
    const [replyingToCommentId, setReplyingToCommentId] = useState(null);
    const [replyContent, setReplyContent] = useState("");

    const [error, setError] = useState("");
    const [deletingCommentId, setDeletingCommentId] = useState(null);
    const [selectedUserProfile, setSelectedUserProfile] = useState(null);

    const currentUser = JSON.parse(localStorage.getItem("user") || "{}");
    const currentUserId = currentUser?.id || currentUser?.userId;

    useEffect(() => {
        const fetchComments = async () => {
            try {
                const res = await fetch(`/api/posts/${postId}/comments`);
                if (res.ok) {
                    const data = await res.json();
                    setComments(data);
                }
            } catch (err) {
                console.error(err);
            } finally {
                setLoading(false);
            }
        };
        fetchComments();
    }, [postId]);

    const handleSubmit = async (e, parentCommentId = null) => {
        if (e) e.preventDefault();
        const content = parentCommentId ? replyContent : newComment;
        if (!content.trim()) return;

        setError("");
        setSubmitting(true);
        try {
            const response = await fetch(`/api/posts/${postId}/comments`, {
                method: "POST",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify({content: content, parentCommentId: parentCommentId})
            });

            if (response.ok) {
                const data = await response.json();
                setComments([...comments, data]);
                if (parentCommentId) {
                    setReplyContent("");
                    setReplyingToCommentId(null);
                } else {
                    setNewComment("");
                }
            }
        } catch (err) {
            setError("Error adding comment: " + err.message);
        } finally {
            setSubmitting(false);
        }
    };

    const handleUpdate = async (commentId) => {
        if (!editContent.trim()) return;
        setError("");
        try {
            const response = await fetch(`/api/posts/${postId}/comments/${commentId}`, {
                method: "PUT",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify({content: editContent})
            });

            if (response.ok) {
                const updatedComment = await response.json();
                setComments(comments.map(c => String(c.id) === String(commentId) ? updatedComment : c));
                setEditingCommentId(null);
            }
        } catch (err) {
            setError("Error updating comment: " + err.message);
        }
    };

    const handleToggleLike = async (commentId) => {
        try {
            const response = await fetch(`/api/posts/${postId}/comments/${commentId}/like`, {
                method: "POST"
            });
            if (response.ok) {
                const updatedComment = await response.json();
                setComments(comments.map(c => String(c.id) === String(commentId) ? updatedComment : c));
            }
        } catch (err) {
            console.error("Error toggling like:", err);
        }
    };

    const handleDelete = async (commentId) => {
        setError("");
        try {
            const response = await fetch(`/api/posts/${postId}/comments/${commentId}`, {
                method: "DELETE"
            });
            if (response.ok) {
                const res = await fetch(`/api/posts/${postId}/comments`);
                if (res.ok) {
                    setComments(await res.json());
                    setDeletingCommentId(null);
                }
            }
        } catch (err) {
            setError("Error deleting comment: " + err.message);
        }
    };

    const renderComment = (comment, depth = 0) => {
        const canEdit = String(comment.userId) === String(currentUserId);
        const canDelete = canEdit || String(postAuthorId) === String(currentUserId);
        const isEditing = editingCommentId === comment.id;
        const isReplying = replyingToCommentId === comment.id;
        const replies = comments.filter(c => c.parentCommentId === comment.id);

        return (
            <div key={comment.id} className="flex flex-col gap-1 mt-2">
                <div className="flex gap-2 items-start">
                    <div
                        className="w-8 h-8 rounded-full overflow-hidden border border-accent-mid-green bg-gray-100 flex items-center justify-center shrink-0 cursor-pointer hover:opacity-80 transition-opacity"
                        onClick={() => setSelectedUserProfile(comment)}
                    >
                        {comment.userProfilePictureUrl ? (
                            <img src={comment.userProfilePictureUrl} alt={comment.userName}
                                 className="w-full h-full object-cover"/>
                        ) : (
                            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24"
                                 fill="none" stroke="#2D5A27" strokeWidth="2" strokeLinecap="round"
                                 strokeLinejoin="round">
                                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                                <circle cx="12" cy="7" r="4"></circle>
                            </svg>
                        )}
                    </div>
                    <div className="flex flex-col max-w-[90%]">
                        {/* Comment Bubble */}
                        <div className="bg-gray-100 px-3 py-2 rounded-2xl relative">
                            <div
                                className="font-bold text-accent-dark-green text-body-small hover:underline cursor-pointer"
                                onClick={() => setSelectedUserProfile(comment)}
                            >
                                {comment.userName}
                            </div>

                            {isEditing ? (
                                <div className="mt-1">
                                    <textarea
                                        value={editContent}
                                        onChange={(e) => setEditContent(e.target.value)}
                                        className="w-full p-2 border border-accent-light-green rounded-lg text-sm focus:outline-none"
                                    />
                                    <div className="flex gap-2 mt-1">
                                        <button onClick={() => handleUpdate(comment.id)}
                                                className="text-[11px] font-bold text-accent-mid-green">Save
                                        </button>
                                        <button onClick={() => setEditingCommentId(null)}
                                                className="text-[11px] font-bold text-gray-500">Cancel
                                        </button>
                                    </div>
                                </div>
                            ) : (
                                <p className={`text-body-small leading-snug ${comment.isDeleted ? 'text-gray-400 italic' : 'text-gray-800'}`}>
                                    {comment.content}
                                </p>
                            )}

                            {/* Floating Like Count */}
                            {comment.likeCount > 0 && (
                                <div
                                    className="absolute -right-2 -bottom-2 bg-white shadow-sm border border-gray-100 rounded-full px-1.5 py-0.5 flex items-center gap-1">
                                    <span className="text-[10px]">❤️</span>
                                    <span className="text-[10px] font-medium text-gray-500">{comment.likeCount}</span>
                                </div>
                            )}
                        </div>

                        {/* Action Row */}
                        {!comment.isDeleted && !isEditing && (
                            <div className="flex items-center gap-3 px-3 mt-0.5">
                                <button onClick={() => handleToggleLike(comment.id)}
                                        className={`text-[12px] font-bold hover:underline ${comment.liked ? 'text-red-500' : 'text-gray-500'}`}>Like
                                </button>
                                <button onClick={() => {
                                    setReplyingToCommentId(comment.id);
                                    setReplyContent("")
                                }} className="text-[12px] font-bold text-gray-500 hover:underline">Reply
                                </button>
                                {canEdit && <button onClick={() => {
                                    setEditingCommentId(comment.id);
                                    setEditContent(comment.content)
                                }} className="text-[12px] font-bold text-gray-500 hover:underline">Edit</button>}
                                {canDelete && (
                                    deletingCommentId === comment.id ? (
                                        <div
                                            className="flex items-center gap-1.5 animate-in fade-in zoom-in-95 duration-200">
                                            <span
                                                className="text-[10px] font-bold text-red-600 uppercase">Delete?</span>
                                            <button onClick={() => setDeletingCommentId(null)}
                                                    className="text-[10px] font-bold text-gray-400 uppercase">No
                                            </button>
                                            <button onClick={() => handleDelete(comment.id)}
                                                    className="text-[10px] font-bold text-red-600 uppercase underline">Yes
                                            </button>
                                        </div>
                                    ) : (
                                        <button onClick={() => setDeletingCommentId(comment.id)}
                                                className="text-[12px] font-bold text-gray-500 hover:underline">Delete</button>
                                    )
                                )}
                                <span
                                    className="text-[11px] text-gray-400">{new Date(comment.createdAt).toLocaleDateString()}</span>
                            </div>
                        )}
                    </div>
                </div>

                {isReplying && (
                    <div className="ml-8 mt-1 flex flex-col gap-2">
                        <input
                            autoFocus
                            placeholder="Write a reply..."
                            value={replyContent}
                            onChange={(e) => setReplyContent(e.target.value)}
                            onKeyDown={(e) => e.key === 'Enter' && handleSubmit(null, comment.id)}
                            className="p-2 border border-gray-200 rounded-2xl bg-gray-50 text-body-small focus:outline-none focus:ring-1 focus:ring-accent-mid-green"
                        />
                        <span className="text-[10px] text-gray-400 px-2">Press Enter to post</span>
                    </div>
                )}

                {replies.length > 0 && (
                    <div className="ml-4 pl-4 border-l-2 border-gray-100">
                        {replies.map(reply => renderComment(reply, depth + 1))}
                    </div>
                )}
            </div>
        );
    };

    const rootComments = comments.filter(c => !c.parentCommentId);

    return (
        <div className="mt-4 pt-4 border-t border-gray-100">
            {loading ? (
                <p className="text-gray-400 text-xs">Loading...</p>
            ) : (
                <div className="flex flex-col gap-3">
                    {error && <p className="text-red-500 text-xs font-bold">{error}</p>}
                    {rootComments.map(comment => renderComment(comment))}

                    {/* New Comment Input */}
                    <form onSubmit={handleSubmit} className="mt-4 flex items-center gap-2">
                        <input
                            type="text"
                            placeholder="Write a comment..."
                            value={newComment}
                            onChange={(e) => setNewComment(e.target.value)}
                            className="flex-1 p-2 bg-gray-100 rounded-2xl text-body-small border-none focus:ring-1 focus:ring-accent-mid-green outline-none"
                        />
                        {newComment.trim() && (
                            <button type="submit" disabled={submitting}
                                    className="text-accent-mid-green font-bold text-xs">Post</button>
                        )}
                    </form>
                </div>
            )}
            <ProfileModal
                isOpen={!!selectedUserProfile}
                onClose={() => setSelectedUserProfile(null)}
                user={selectedUserProfile}
            />
        </div>
    );
};