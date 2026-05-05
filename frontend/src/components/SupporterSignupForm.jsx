import React from "react";

export const SupporterSignupForm = ({formData, onChange}) => {
    const inputClasses = "block w-md mb-4 px-4 py-2 rounded border border-gray-300 focus:outline-none focus:scale-105 focus:border-accent-mid-green transition-all duration-300";


    return (
        <>
            <h3 className="text-h">Supporter</h3>
            <input
                type="text"
                name="firstName"
                placeholder="First Name"
                value={formData.firstName}
                required={true}
                onChange={onChange}
                className={inputClasses}
                autoFocus
            />
            <input
                type="text"
                name="lastName"
                placeholder="Last Name"
                value={formData.lastName}
                required={true}
                onChange={onChange}
                className={inputClasses}
            />
            <input
                type="email"
                name="email"
                placeholder="Email"
                value={formData.email}
                required={true}
                onChange={onChange}
                className={inputClasses}
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
        </>
    );
};

export default SupporterSignupForm;
