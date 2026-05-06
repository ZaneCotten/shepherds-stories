export const validatePassword = (password) => {
    if (!password || password.length < 8 || password.length > 32) return false;

    let qualifiers = 0;
    if (/[a-z]/.test(password)) qualifiers++;
    if (/[A-Z]/.test(password)) qualifiers++;
    if (/[0-9]/.test(password)) qualifiers++;
    if (/[^a-zA-Z0-9]/.test(password)) qualifiers++;

    return qualifiers >= 3;
};
