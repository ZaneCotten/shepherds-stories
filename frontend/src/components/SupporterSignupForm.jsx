import React from "react";

export const SupporterSignupForm = ({formData, onChange}) => {
    return (
        <>
            <h3>Supporter</h3>
            <input
                type="text"
                name="firstName"
                placeholder="First Name"
                value={formData.firstName}
                required={true}
                onChange={onChange}
            />
            <br/>
            <input
                type="text"
                name="lastName"
                placeholder="Last Name"
                value={formData.lastName}
                required={true}
                onChange={onChange}
            />
            <br/>
            <input
                type="email"
                name="email"
                placeholder="Email"
                value={formData.email}
                required={true}
                onChange={onChange}
            />
            <br/>
            <input
                type="password"
                name="password"
                placeholder="Password"
                value={formData.password}
                required={true}
                onChange={onChange}
            />
        </>
    );
};

export default SupporterSignupForm;
