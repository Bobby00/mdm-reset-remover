// MDM Reset Remover — Landing Page JavaScript

document.addEventListener("DOMContentLoaded", () => {
    // FAQ accordion
    const faqQuestions = document.querySelectorAll(".faq-question");
    
    faqQuestions.forEach((question) => {
        question.addEventListener("click", () => {
            const faqItem = question.parentElement;
            const answer = faqItem.querySelector(".faq-answer");
            const isOpen = answer.style.maxHeight && answer.style.maxHeight !== "0px";

            // Close all other answers
            document.querySelectorAll(".faq-answer").forEach((a) => {
                a.style.maxHeight = null;
            });

            if (!isOpen) {
                answer.style.maxHeight = answer.scrollHeight + "px";
            } else {
                answer.style.maxHeight = null;
            }
        });
    });

    // Smooth scroll for navigation links
    const navLinks = document.querySelectorAll('a[href^="#"]');
    navLinks.forEach((link) => {
        link.addEventListener("click", (e) => {
            const targetId = link.getAttribute("href");
            if (targetId && targetId.startsWith("#")) {
                const target = document.querySelector(targetId);
                if (target) {
                    e.preventDefault();
                    target.scrollIntoView({ behavior: "smooth" });
                }
            }
        });
    });
});