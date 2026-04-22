import {Link} from "react-router-dom";
import PublicHeader from "../components/PublicHeader.jsx";

const HomePage = () => (
    <div className="home-page">
        <PublicHeader/>
        <h1>Shepherds' Stories</h1>
        <p>Connecting missionaries and supporters through stories that inspire and transform.</p>
        <div className="action-buttons">
            <Link to="/login" className="button primary border-4 border-cyan-500 px-4 py-2  rounded-4xl">Login</Link>
            <Link to="/register" className="button secondary">Register</Link>
        </div>
    </div>
);

export default HomePage;