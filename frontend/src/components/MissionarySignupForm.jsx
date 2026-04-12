import React from "react";

export const MissionarySignupForm = ({formData, onChange}) => {
    return (
        <>
            <h3>Missionary</h3>
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
            <br/>
            <input
                type="text"
                name="displayName"
                placeholder="Display Name"
                value={formData.displayName}
                required={true}
                onChange={onChange}
            />
            <br/>
            <input
                type="text"
                name="region"
                placeholder="Region"
                value={formData.region}
                onChange={onChange}
            />
            <br/>
            <textarea
                name="biography"
                placeholder="Biography"
                value={formData.biography}
                onChange={onChange}
            />
        </>
    );
};

export default MissionarySignupForm;
