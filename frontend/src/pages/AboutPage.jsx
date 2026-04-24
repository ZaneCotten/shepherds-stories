import React from "react";
import PublicHeader from "../components/PublicHeader.jsx";

const AboutPage = () => {
    return (
        <>
            <PublicHeader/>

            <div className="bg-white flex min-h-screen">
                {/* Main Content Column */}
                <div className="w-full p-12 flex flex-col items-center justify-center">
                    <h2 className="mb-8 text-header-1 font-sans:roboto text-accent-mid-green">About Shepherds'
                        Stories</h2>

                    <div className="max-w-lg text-gray-700 space-y-6">
                        <p>
                            Shepherds' Stories is a dedicated platform designed to bridge the gap between missionaries
                            serving in the field and the supporters who walk alongside them.
                        </p>
                        <p>
                            We believe that every testimony of faith, challenge overcome, and culture encountered
                            is a story worth telling. By providing a secure, centralized space for sharing these
                            experiences, we hope to foster deeper connections, more informed prayer, and
                            tangible support for global missions.
                        </p>
                        <p>
                            Our mission is simple: to encourage the Church through the collective narrative of
                            those dedicated to the Great Commission.
                        </p>
                    </div>
                </div>
            </div>
        </>
    );
};

export default AboutPage;