import {Link, NavLink} from "react-router-dom";

export const PublicHeader = () => {
    return (
        <>
            <header className="public-header">
                <div className="flex justify-between items-center px-4 py-6 bg-primary text-white">
                    <p className="text-header-2 font-bold">
                        <NavLink to="/home">
                            Shepherds' Stories
                        </NavLink>
                    </p>
                    <nav className="public-header-nav" style={{marginRight: '50px'}}>
                        <ul className="flex space-x-8">
                            <li>
                                <NavLink
                                    to="/home"
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