import {useState, useEffect} from "react";

export const CommentSection = ({postId}) => {
    const [comments, setComments] = useState([]);
    const [newComment, setNewComment] = useState("");
    const [loading, setLoading] = useState(true);
    const [submitting, setSubmitting] = useState(false);

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

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!newComment.trim()) return;

        setSubmitting(true);
        try {
            const response = await fetch(`/api/posts/${postId}/comments`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({content: newComment})
            });

            if (response.ok) {
                const data = await response.json();
                setComments([...comments, data]);
                setNewComment("");
            } else {
                alert("Failed to add comment.");
            }
        } catch (err) {
            alert("Error adding comment: " + err.message);
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <div style={{marginTop: "20px", borderTop: "1px solid var(--border-input)", paddingTop: "15px"}}>
            <h4 style={{color: "var(--text-h)", marginBottom: "10px", fontSize: "1rem"}}>Comments</h4>

            {loading ? (
                <p style={{fontSize: "0.8rem", color: "var(--text-muted)"}}>Loading comments...</p>
            ) : comments.length === 0 ? (
                <p style={{fontSize: "0.8rem", color: "var(--text-muted)", marginBottom: "15px"}}>No comments yet. Be
                    the first to comment!</p>
            ) : (
                <div style={{display: "flex", flexDirection: "column", gap: "10px", marginBottom: "15px"}}>
                    {comments.map(comment => (
                        <div key={comment.id} style={{
                            padding: "8px 12px",
                            backgroundColor: "var(--bg-input)",
                            borderRadius: "8px",
                            border: "1px solid var(--border-input)"
                        }}>
                            <div style={{display: "flex", justifyContent: "space-between", marginBottom: "4px"}}>
                                <span style={{
                                    fontWeight: "bold",
                                    fontSize: "0.85rem",
                                    color: "var(--accent)"
                                }}>{comment.userName}</span>
                                <span style={{fontSize: "0.75rem", color: "var(--text-muted)"}}>
                                    {new Date(comment.createdAt).toLocaleString([], {
                                        dateStyle: 'short',
                                        timeStyle: 'short'
                                    })}
                                </span>
                            </div>
                            <p style={{
                                margin: 0,
                                fontSize: "0.9rem",
                                color: "var(--text)",
                                whiteSpace: "pre-wrap"
                            }}>{comment.content}</p>
                        </div>
                    ))}
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
