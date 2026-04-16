import {Link} from "react-router-dom";

const HomePage = () => (
    <div style={{
        minHeight: "100vh",
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        justifyContent: "center",
        padding: "40px 16px",
        textAlign: "center"
    }}>
        <h1 style={{fontSize: "4rem", marginBottom: "40px"}}>Shepherds' Stories</h1>
        <p style={{fontSize: "1.5rem", maxWidth: "800px", marginBottom: "40px", color: "var(--text)"}}>
            Connecting missionaries and supporters through stories that inspire and transform.
        </p>
        <div style={{display: "flex", gap: "20px"}}>
            <Link
                to="/login"
                style={{
                    padding: "12px 24px",
                    borderRadius: "8px",
                    backgroundColor: "var(--primary)",
                    color: "white",
                    textDecoration: "none",
                    fontWeight: "bold"
                }}
            >
                Login
            </Link>
            <Link
                to="/register"
                style={{
                    padding: "12px 24px",
                    borderRadius: "8px",
                    border: "1px solid var(--border-input)",
                    color: "var(--text-h)",
                    textDecoration: "none",
                    fontWeight: "bold"
                }}
            >
                Register
            </Link>
        </div>
    </div>
);

export default HomePage;