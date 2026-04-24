import React from "react";

export const MissionarySignupForm = ({formData, onChange}) => {
    const inputClasses = "block w-md mb-4 px-4 py-2 rounded border border-gray-300 focus:outline-none focus:scale-105 focus:border-accent-mid-green transition-all duration-300";

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
                className={inputClasses}
            />
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