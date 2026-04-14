import {useState, useEffect} from "react";

export const CommentSection = ({postId, postAuthorId}) => {
    const [comments, setComments] = useState([]);
    const [newComment, setNewComment] = useState("");
    const [loading, setLoading] = useState(true);
    const [submitting, setSubmitting] = useState(false);
    const [editingCommentId, setEditingCommentId] = useState(null);
    const [editContent, setEditContent] = useState("");
    const [replyingToCommentId, setReplyingToCommentId] = useState(null);
    const [replyContent, setReplyContent] = useState("");

    const currentUser = JSON.parse(localStorage.getItem("user") || "{}");
    const currentUserId = currentUser?.id || currentUser?.userId; // Handle both potential field names

    useEffect(() => {
        fetch(`/api/posts/${postId}/comments`)
            .then(res => {
                if (!res.ok) throw new Error("Failed to fetch comments");
                return res.json();
            })
            .then(data => {
                setComments(data);
                setLoading(false);
            })
            .catch(err => {
                console.error(err);
                setLoading(false);
            });
    }, [postId]);

    const handleSubmit = async (e, parentCommentId = null) => {
        if (e) e.preventDefault();
        const content = parentCommentId ? replyContent : newComment;
        if (!content.trim()) return;

        setSubmitting(true);
        try {
            const response = await fetch(`/api/posts/${postId}/comments`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    content: content,
                    parentCommentId: parentCommentId
                })
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
            } else {
                const errorData = await response.json().catch(() => ({}));
                alert(errorData.error || "Failed to add comment.");
            }
        } catch (err) {
            alert("Error adding comment: " + err.message);
        } finally {
            setSubmitting(false);
        }
    };

    const handleUpdate = async (commentId) => {
        if (!editContent.trim()) return;
        try {
            const response = await fetch(`/api/posts/${postId}/comments/${commentId}`, {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({content: editContent})
            });

            if (response.ok) {
                const updatedComment = await response.json();
                setComments(comments.map(c => String(c.id) === String(commentId) ? updatedComment : c));
                setEditingCommentId(null);
            } else {
                const errorData = await response.json().catch(() => ({}));
                alert(errorData.error || "Failed to update comment.");
            }
        } catch (err) {
            alert("Error updating comment: " + err.message);
        }
    };

    const handleDelete = async (commentId) => {
        if (!window.confirm("Are you sure you want to delete this comment?")) return;
        try {
            const response = await fetch(`/api/posts/${postId}/comments/${commentId}`, {
                method: "DELETE"
            });

            if (response.ok) {
                // Refetch comments to ensure UI stays in sync with recursive backend deletion
                const res = await fetch(`/api/posts/${postId}/comments`);
                if (res.ok) {
                    const data = await res.json();
                    setComments(data);
                }
            } else {
                const errorData = await response.json().catch(() => ({}));
                alert(errorData.error || "Failed to delete comment.");
            }
        } catch (err) {
            alert("Error deleting comment: " + err.message);
        }
    };

    const startEditing = (comment) => {
        setEditingCommentId(comment.id);
        setEditContent(comment.content);
        setReplyingToCommentId(null);
    };

    const startReplying = (commentId) => {
        setReplyingToCommentId(commentId);
        setReplyContent("");
        setEditingCommentId(null);
    };

    const renderComment = (comment, depth = 0) => {
        const canEdit = String(comment.userId) === String(currentUserId);
        const canDelete = canEdit || String(postAuthorId) === String(currentUserId);
        const isEditing = editingCommentId === comment.id;
        const isReplying = replyingToCommentId === comment.id;
        const replies = getReplies(comment.id);

        return (
            <div key={comment.id} style={{display: "flex", flexDirection: "column", gap: "10px"}}>
                <div style={{
                    padding: "8px 12px",
                    backgroundColor: "var(--bg-input)",
                    borderRadius: "8px",
                    border: "1px solid var(--border-input)",
                    marginLeft: depth > 0 ? `${Math.min(depth, 5) * 20}px` : "0",
                    position: "relative"
                }}>
                    {depth > 0 && (
                        <div style={{
                            position: "absolute",
                            left: "-12px",
                            top: "16px",
                            width: "12px",
                            height: "1px",
                            backgroundColor: "var(--border-input)"
                        }}/>
                    )}
                    <div style={{display: "flex", justifyContent: "space-between", marginBottom: "4px"}}>
                        <span style={{
                            fontWeight: "bold",
                            fontSize: "0.85rem",
                            color: "var(--accent)"
                        }}>{comment.userName}</span>
                        <div style={{display: "flex", alignItems: "center", gap: "8px"}}>
                            <span style={{fontSize: "0.75rem", color: "var(--text-muted)"}}>
                                {new Date(comment.createdAt).toLocaleString([], {
                                    dateStyle: 'short',
                                    timeStyle: 'short'
                                })}
                                {comment.edited && !comment.isDeleted && (
                                    <span style={{fontStyle: 'italic', marginLeft: '4px'}}
                                          title={`Updated at: ${new Date(comment.updatedAt).toLocaleString()}`}>
                                        (edited)
                                    </span>
                                )}
                            </span>
                            <div style={{display: "flex", gap: "5px"}}>
                                {!isEditing && (
                                    <button
                                        onClick={() => startReplying(comment.id)}
                                        style={{
                                            background: "none",
                                            border: "none",
                                            color: "var(--accent)",
                                            fontSize: "0.7rem",
                                            cursor: "pointer",
                                            padding: 0
                                        }}
                                    >Reply</button>
                                )}
                                {canEdit && !isEditing && !comment.isDeleted && (
                                    <>
                                        <button
                                            onClick={() => startEditing(comment)}
                                            style={{
                                                background: "none",
                                                border: "none",
                                                color: "var(--accent)",
                                                fontSize: "0.7rem",
                                                cursor: "pointer",
                                                padding: 0
                                            }}
                                        >Edit
                                        </button>
                                    </>
                                )}
                                {canDelete && !isEditing && !comment.isDeleted && (
                                    <button
                                        onClick={() => handleDelete(comment.id)}
                                        style={{
                                            background: "none",
                                            border: "none",
                                            color: "red",
                                            fontSize: "0.7rem",
                                            cursor: "pointer",
                                            padding: 0
                                        }}
                                    >Delete
                                    </button>
                                )}
                            </div>
                        </div>
                    </div>

                    {isEditing ? (
                        <div style={{display: "flex", flexDirection: "column", gap: "5px"}}>
                        <textarea
                            value={editContent}
                            onChange={(e) => setEditContent(e.target.value)}
                            style={{
                                width: "100%",
                                padding: "5px",
                                borderRadius: "4px",
                                border: "1px solid var(--border-input)",
                                backgroundColor: "var(--bg-card)",
                                color: "var(--text)",
                                fontSize: "0.9rem",
                                minHeight: "50px",
                                resize: "vertical"
                            }}
                        />
                            <div style={{display: "flex", gap: "5px"}}>
                                <button
                                    onClick={() => handleUpdate(comment.id)}
                                    style={{
                                        padding: "2px 8px",
                                        borderRadius: "4px",
                                        backgroundColor: "var(--accent-primary)",
                                        color: "white",
                                        border: "none",
                                        fontSize: "0.75rem",
                                        cursor: "pointer"
                                    }}
                                >Save
                                </button>
                                <button
                                    onClick={() => setEditingCommentId(null)}
                                    style={{
                                        padding: "2px 8px",
                                        borderRadius: "4px",
                                        backgroundColor: "var(--bg-input)",
                                        color: "var(--text)",
                                        border: "1px solid var(--border-input)",
                                        fontSize: "0.75rem",
                                        cursor: "pointer"
                                    }}
                                >Cancel
                                </button>
                            </div>
                        </div>
                    ) : (
                        <p style={{
                            margin: 0,
                            fontSize: "0.9rem",
                            color: "var(--text)",
                            whiteSpace: "pre-wrap",
                            fontStyle: comment.isDeleted ? "italic" : "normal",
                            opacity: comment.isDeleted ? 0.7 : 1
                        }}>{comment.content}</p>
                    )}

                    {isReplying && (
                        <div style={{marginTop: "8px", display: "flex", flexDirection: "column", gap: "5px"}}>
                        <textarea
                            placeholder="Write a reply..."
                            value={replyContent}
                            onChange={(e) => setReplyContent(e.target.value)}
                            style={{
                                width: "100%",
                                padding: "5px",
                                borderRadius: "4px",
                                border: "1px solid var(--border-input)",
                                backgroundColor: "var(--bg-card)",
                                color: "var(--text)",
                                fontSize: "0.85rem",
                                minHeight: "40px",
                                resize: "vertical"
                            }}
                        />
                            <div style={{display: "flex", gap: "5px"}}>
                                <button
                                    onClick={() => handleSubmit(null, comment.id)}
                                    disabled={submitting || !replyContent.trim()}
                                    style={{
                                        padding: "2px 8px",
                                        borderRadius: "4px",
                                        backgroundColor: "var(--accent-primary)",
                                        color: "white",
                                        border: "none",
                                        fontSize: "0.75rem",
                                        cursor: "pointer",
                                        opacity: submitting || !replyContent.trim() ? 0.6 : 1
                                    }}
                                >{submitting ? "..." : "Reply"}</button>
                                <button
                                    onClick={() => setReplyingToCommentId(null)}
                                    style={{
                                        padding: "2px 8px",
                                        borderRadius: "4px",
                                        backgroundColor: "var(--bg-input)",
                                        color: "var(--text)",
                                        border: "1px solid var(--border-input)",
                                        fontSize: "0.75rem",
                                        cursor: "pointer"
                                    }}
                                >Cancel
                                </button>
                            </div>
                        </div>
                    )}
                </div>
                {replies.length > 0 && (
                    <div style={{display: "flex", flexDirection: "column", gap: "10px", marginTop: "10px"}}>
                        {replies.map(reply => renderComment(reply, depth + 1))}
                    </div>
                )}
            </div>
        );
    };

    const rootComments = comments.filter(c => !c.parentCommentId);
    const getReplies = (parentId) => comments.filter(c => c.parentCommentId === parentId);

    return (
        <div style={{marginTop: "20px", borderTop: "1px solid var(--border-input)", paddingTop: "15px"}}>
            <h4 style={{color: "var(--text-h)", marginBottom: "10px", fontSize: "1rem"}}>Comments</h4>

            {loading ? (
                <p style={{fontSize: "0.8rem", color: "var(--text-muted)"}}>Loading comments...</p>
            ) : rootComments.length === 0 ? (
                <p style={{fontSize: "0.8rem", color: "var(--text-muted)", marginBottom: "15px"}}>No comments yet. Be
                    the first to comment!</p>
            ) : (
                <div style={{display: "flex", flexDirection: "column", gap: "10px", marginBottom: "15px"}}>
                    {rootComments.map(comment => renderComment(comment))}
                </div>
            )}

            <form onSubmit={handleSubmit} style={{display: "flex", gap: "8px"}}>
                <input
                    type="text"
                    placeholder="Add a comment..."
                    value={newComment}
                    onChange={(e) => setNewComment(e.target.value)}
                    style={{
                        flex: 1,
                        padding: "8px 12px",
                        borderRadius: "20px",
                        border: "1px solid var(--border-input)",
                        backgroundColor: "var(--bg-input)",
                        color: "var(--text-h)",
                        fontSize: "0.9rem"
                    }}
                />
                <button
                    type="submit"
                    disabled={submitting || !newComment.trim()}
                    style={{
                        padding: "8px 16px",
                        borderRadius: "20px",
                        backgroundColor: "var(--accent-primary)",
                        color: "white",
                        border: "none",
                        cursor: "pointer",
                        fontSize: "0.85rem",
                        fontWeight: "bold",
                        opacity: submitting || !newComment.trim() ? 0.6 : 1
                    }}
                >
                    {submitting ? "..." : "Post"}
                </button>
            </form>
        </div>
    );
};
