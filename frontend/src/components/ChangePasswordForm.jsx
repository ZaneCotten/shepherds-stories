import React, {useState} from 'react';
import {validatePassword} from '../utils/passwordValidator';

const ChangePasswordForm = () => {
    const [currentPassword, setCurrentPassword] = useState('');
    const [newPassword, setNewPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');
    const [showConfirm, setShowConfirm] = useState(false);
    const [isLoading, setIsLoading] = useState(false);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setSuccess('');

        if (!validatePassword(newPassword)) {
            setError('New password does not meet requirements: 8-32 characters, and at least 3 of: lowercase, uppercase, number, symbol.');
            return;
        }

        if (newPassword !== confirmPassword) {
            setError('New passwords do not match.');
            return;
        }

        if (!showConfirm) {
            setShowConfirm(true);
            return;
        }

        setIsLoading(true);
        try {
            const response = await fetch('/api/user/change-password', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({currentPassword, newPassword}),
            });

            // Try to parse JSON, if it fails use a default message
            let data;
            const contentType = response.headers.get("content-type");
            if (contentType && contentType.indexOf("application/json") !== -1) {
                data = await response.json();
            } else {
                data = {message: await response.text()};
            }

            if (response.ok) {
                setSuccess('Password changed successfully!');
                setCurrentPassword('');
                setNewPassword('');
                setConfirmPassword('');
                setShowConfirm(false);
                // Clear success message after 5 seconds
                setTimeout(() => setSuccess(''), 5000);
            } else {
                setError(data.error || data.message || 'Failed to change password.');
                setShowConfirm(false);
            }
        } catch {
            setError('An error occurred. Please try again.');
            setShowConfirm(false);
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="mt-12 pt-12 border-t border-gray-100">
            <h3 className="text-lg font-bold text-accent-dark-green mb-6">Security</h3>
            <form onSubmit={handleSubmit} className="space-y-4">
                {error && (
                    <div className="p-3 bg-red-50 border border-red-100 rounded-xl">
                        <p className="text-red-500 text-sm font-bold">{error}</p>
                    </div>
                )}
                {success && (
                    <div className="p-3 bg-accent-light-green/20 border border-accent-mid-green/20 rounded-xl">
                        <p className="text-accent-mid-green text-sm font-bold">{success}</p>
                    </div>
                )}

                <div>
                    <label className="text-xs font-black text-accent-mid-green uppercase tracking-widest mb-1 block">Current
                        Password</label>
                    <input
                        type="password"
                        value={currentPassword}
                        onChange={(e) => {
                            setCurrentPassword(e.target.value);
                            if (showConfirm) setShowConfirm(false);
                        }}
                        className="w-full p-3 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-accent-mid-green/20 focus:border-accent-mid-green transition-all"
                        required
                        autoComplete="current-password"
                    />
                </div>

                <div>
                    <label className="text-xs font-black text-accent-mid-green uppercase tracking-widest mb-1 block">New
                        Password</label>
                    <input
                        type="password"
                        value={newPassword}
                        onChange={(e) => {
                            setNewPassword(e.target.value);
                            if (showConfirm) setShowConfirm(false);
                        }}
                        className="w-full p-3 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-accent-mid-green/20 focus:border-accent-mid-green transition-all"
                        required
                        autoComplete="new-password"
                    />
                    <p className="text-[10px] text-gray-400 mt-1 italic">
                        Must be 8-32 characters with at least 3 of: lowercase, uppercase, number, symbol.
                    </p>
                </div>

                <div>
                    <label className="text-xs font-black text-accent-mid-green uppercase tracking-widest mb-1 block">Confirm
                        New Password</label>
                    <input
                        type="password"
                        value={confirmPassword}
                        onChange={(e) => {
                            setConfirmPassword(e.target.value);
                            if (showConfirm) setShowConfirm(false);
                        }}
                        className="w-full p-3 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-accent-mid-green/20 focus:border-accent-mid-green transition-all"
                        required
                        autoComplete="new-password"
                    />
                </div>

                <div className="pt-2">
                    {!showConfirm ? (
                        <button
                            type="submit"
                            className="w-full py-3 bg-white border-2 border-accent-mid-green text-accent-mid-green rounded-xl font-bold hover:bg-accent-light-green transition-colors"
                        >
                            Update Password
                        </button>
                    ) : (
                        <div
                            className="p-4 bg-accent-light-green/10 border border-accent-mid-green/20 rounded-2xl animate-in fade-in zoom-in duration-300">
                            <p className="text-sm font-bold text-center text-accent-dark-green mb-4">Are you sure you
                                want to change your password?</p>
                            <div className="flex gap-4">
                                <button
                                    type="button"
                                    onClick={() => setShowConfirm(false)}
                                    className="flex-1 py-3 bg-gray-100 text-gray-600 rounded-xl font-bold hover:bg-gray-200 transition-colors"
                                >
                                    Cancel
                                </button>
                                <button
                                    type="submit"
                                    disabled={isLoading}
                                    className="flex-1 py-3 bg-accent-mid-green text-white rounded-xl font-bold hover:bg-accent-dark-green transition-colors shadow-md disabled:opacity-50"
                                >
                                    {isLoading ? 'Updating...' : 'Yes, Update'}
                                </button>
                            </div>
                        </div>
                    )}
                </div>
            </form>
        </div>
    );
};

export default ChangePasswordForm;
