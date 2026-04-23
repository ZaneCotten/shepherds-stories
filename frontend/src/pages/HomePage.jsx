import {Link} from "react-router-dom";
import PublicHeader from "../components/PublicHeader.jsx";

const HomePage = () => (
    <div className="home-page flex flex-col min-h-screen bg-accent-light-green">
        <PublicHeader/>

        <div className="grid flex-1 place-content-center place-items-center text-center p-4 ">
            <div className="max-w-lg bg-accent-mid p-8 bg-accent-mid-green rounded-lg shadow-md">
                <div
                    className="flex items-center justify-center h-24 bg-accent-dark-green rounded-lg text-center my-6">
                    <h1 className="text-header-2 font-bold mb-4 p-6 text-white">Shepherds' Stories</h1>
                </div>
                <p className="text-body-large text-white">
                    Connecting missionaries and supporters through stories that inspire and transform.
                </p>

                <div className="flex justify-center gap-4 mt-6">
                    <div>
                        <Link to="/login"
                              className="inline-block drop-shadow-2xl text-white bg-accent-dark-green px-6 py-2 rounded-lg hover:scale-110 hover:bg-accent-light-green hover:text-white transition-all duration-300">
                            Login
                        </Link>
                    </div>
                    <div>
                        <Link to="/register"
                              className="inline-block drop-shadow-2xl text-white ring-3 ring-accent-dark-green ring-inset px-6 py-2 rounded-lg hover:scale-110 hover:bg-accent-light-green hover:ring-0 hover:text-white transition-all duration-300">
                            Register
                        </Link>
                    </div>
                </div>
            </div>
        </div>
    </div>
);

export default HomePage;