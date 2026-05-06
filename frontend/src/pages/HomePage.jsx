import {Link} from "react-router-dom";
import PublicHeader from "../components/PublicHeader.jsx";

const HomePage = () => (
    <>
        <PublicHeader/>
        <div className="bg-white flex min-h-screen">
            {/* Main Content Column */}
            <div className="w-full flex flex-col items-center justify-center p-6">
                <h1 className="text-header-1 font-sans:roboto text-center text-accent-mid-green mb-8">
                    Shepherds' Stories
                </h1>

                <p className="text-body-large text-gray-700 max-w-md text-center mb-10">
                    Connecting missionaries and supporters through stories that inspire and transform.
                </p>

                <div className="flex flex-col gap-4 w-full max-w-sm">
                    <Link to="/login"
                          className="w-full text-center px-4 py-3 rounded bg-accent-mid-green text-white hover:bg-accent-light-green hover:scale-105 drop-shadow-md transition-all duration-300">
                        Login
                    </Link>
                    <Link to="/register"
                          className="w-full text-center px-4 py-3 rounded border border-accent-mid-green text-accent-mid-green hover:bg-gray-50 hover:scale-105 drop-shadow-md transition-all duration-300">
                        Register
                    </Link>
                </div>
            </div>
        </div>
    </>
);

export default HomePage;