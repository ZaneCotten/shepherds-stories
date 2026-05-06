import React from "react";

export const MissionarySignupForm = ({formData, onChange, passwordError}) => {
    const inputClasses = "block w-full max-w-md mb-4 px-4 py-2 rounded border border-gray-300 focus:outline-none focus:scale-105 focus:border-accent-mid-green transition-all duration-300";
    const passwordClasses = `block w-full max-w-md mb-4 px-4 py-2 rounded border ${passwordError ? 'border-red-500' : 'border-gray-300'} focus:outline-none focus:scale-105 ${passwordError ? 'focus:border-red-500' : 'focus:border-accent-mid-green'} transition-all duration-300`;

    return (
        <>
            <input
                type="email"
                name="email"
                placeholder="Email"
                value={formData.email}
                required={true}
                onChange={onChange}
                className={inputClasses}
                autoFocus
            />
            <input
                type="password"
                name="password"
                placeholder="Password"
                value={formData.password}
                required={true}
                onChange={onChange}
                className={passwordClasses}
            />
            <p className={`text-xs ${passwordError ? 'text-red-500' : 'text-gray-500'} mb-4 w-full max-w-md`}>
                Password must be at least 8 characters long and contain at least 3 of:
                lowercase, uppercase, digit, and symbol.
            </p>
            <input
                type="text"
                name="displayName"
                placeholder="Display Name"
                value={formData.displayName}
                required={true}
                onChange={onChange}
                className={inputClasses}
            />
            <input
                type="text"
                name="region"
                placeholder="Region"
                value={formData.region}
                onChange={onChange}
                className={inputClasses}
            />
            <textarea
                name="biography"
                placeholder="Biography"
                value={formData.biography}
                onChange={onChange}
                className={`${inputClasses} resize-y h-24`}
            />
        </>
    );
};

export default MissionarySignupForm;