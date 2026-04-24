import {NavLink} from "react-router-dom";

export const PublicHeader = () => {
    return (
        <>
            <header className="public-header drop-shadow-md">
                <div className="flex justify-between items-center px-4 py-6 bg-primary text-white">
                    <h1 className="text-header-4 font-bold inline-block hover:scale-103 transition-all duration-300">
                        <NavLink to="/home">
                            Shepherds' Stories
                        </NavLink>
                    </h1>
                    <nav className="public-header-nav" style={{marginRight: '50px'}}>
                        <ul className="flex space-x-8">
                            <li>
                                <NavLink
                                    to="/home"
                                    className=" inline-block hover:scale-105 hover:text-accent-light-green duration-300"
                                    style={({isActive}) => ({
                                        textDecoration: isActive ? 'underline' : 'none'
                                    })}
                                >
                                    Home
                                </NavLink>
                            </li>

                            <li>
                                <NavLink
                                    to="/about"
                                    className="inline-block hover:scale-105 hover:text-accent-light-green duration-300"
                                    style={({isActive}) => ({
                                        textDecoration: isActive ? 'underline' : 'none'
                                    })}
                                >
                                    About
                                </NavLink>
                            </li>
                            <li>
                                <NavLink
                                    to="/register"
                                    className="inline-block hover:scale-105 hover:text-accent-light-green duration-300"
                                    style={({isActive}) => ({
                                        textDecoration: isActive ? 'underline' : 'none'
                                    })}
                                >
                                    Sign up
                                </NavLink>
                            </li>
                            <li>
                                <NavLink
                                    to="/login"
                                    className="inline-block hover:scale-105 hover:text-accent-light-green duration-300"
                                    style={({isActive}) => ({
                                        textDecoration: isActive ? 'underline' : 'none'
                                    })}
                                >
                                    Log in
                                </NavLink>
                            </li>
                        </ul>
                    </nav>
                </div>
            </header>
        </>
    )
}
export default PublicHeader