import {NavLink} from "react-router-dom";
import React, {useState} from 'react';

export const PublicHeader = () => {
    const [isMenuOpen, setIsMenuOpen] = useState(false);

    const toggleMenu = () => {
        setIsMenuOpen(!isMenuOpen);
    };

    const navLinks = [
        {to: "/home", label: "Home"},
        {to: "/about", label: "About"},
        {to: "/register", label: "Sign up"},
        {to: "/login", label: "Log in"}
    ];

    return (
        <>
            <header className="public-header drop-shadow-md relative z-50">
                <div className="flex justify-between items-center px-4 py-6 bg-primary text-white">
                    <h1 className="text-header-4 font-bold inline-block hover:scale-103 transition-all duration-300">
                        <NavLink to="/home">
                            Shepherds' Stories
                        </NavLink>
                    </h1>

                    {/* Hamburger Button */}
                    <button
                        className="md:hidden flex flex-col justify-center items-center w-8 h-8 focus:outline-none"
                        onClick={toggleMenu}
                        aria-label="Toggle menu"
                    >
                        <span
                            className={`bg-white block transition-all duration-300 h-0.5 w-6 rounded-sm ${isMenuOpen ? 'rotate-45 translate-y-1' : '-translate-y-1'}`}></span>
                        <span
                            className={`bg-white block transition-all duration-300 h-0.5 w-6 rounded-sm my-0.5 ${isMenuOpen ? 'opacity-0' : 'opacity-100'}`}></span>
                        <span
                            className={`bg-white block transition-all duration-300 h-0.5 w-6 rounded-sm ${isMenuOpen ? '-rotate-45 -translate-y-1' : 'translate-y-1'}`}></span>
                    </button>

                    {/* Desktop Nav */}
                    <nav className="hidden md:block public-header-nav" style={{marginRight: '50px'}}>
                        <ul className="flex space-x-8">
                            {navLinks.map((link) => (
                                <li key={link.to}>
                                    <NavLink
                                        to={link.to}
                                        className="inline-block hover:scale-105 hover:text-accent-light-green duration-300"
                                        style={({isActive}) => ({
                                            textDecoration: isActive ? 'underline' : 'none'
                                        })}
                                    >
                                        {link.label}
                                    </NavLink>
                                </li>
                            ))}
                        </ul>
                    </nav>
                </div>

                {/* Mobile Nav Dropdown */}
                <div
                    className={`md:hidden bg-primary text-white overflow-hidden transition-all duration-300 ease-in-out ${isMenuOpen ? 'max-h-64 border-t border-white/10' : 'max-h-0'}`}>
                    <nav className="flex flex-col items-center py-6 space-y-6">
                        {navLinks.map((link) => (
                            <NavLink
                                key={link.to}
                                to={link.to}
                                onClick={() => setIsMenuOpen(false)}
                                className="text-lg font-medium hover:text-accent-light-green transition-colors duration-300"
                                style={({isActive}) => ({
                                    textDecoration: isActive ? 'underline' : 'none'
                                })}
                            >
                                {link.label}
                            </NavLink>
                        ))}
                    </nav>
                </div>
            </header>
        </>
    )
}
export default PublicHeader