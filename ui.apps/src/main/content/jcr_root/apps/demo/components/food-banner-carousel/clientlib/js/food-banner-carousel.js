
(function () {
    "use strict";

    function initializeCarousel(root) {
        var slides = Array.prototype.slice.call(
            root.querySelectorAll(".food-banner-carousel__slide")
        );

        if (slides.length === 0) {
            return;
        }

        if (root.getAttribute("data-carousel-ready") === "true") {
            return;
        }

        root.setAttribute("data-carousel-ready", "true");

        var previous = root.querySelector("[data-carousel-prev]");
        var next = root.querySelector("[data-carousel-next]");
        var dots = root.querySelector("[data-carousel-dots]");
        var current = 0;
        var timer = null;

        function showSlide(index) {
            current = (index + slides.length) % slides.length;

            slides.forEach(function (slide, i) {
                var active = i === current;
                slide.style.display = active ? "flex" : "none";
                slide.setAttribute("aria-hidden", active ? "false" : "true");
            });

            if (dots) {
                Array.prototype.forEach.call(dots.children, function (dot, i) {
                    dot.classList.toggle("is-active", i === current);
                });
            }
        }

        function restartTimer() {
            if (timer !== null) {
                window.clearInterval(timer);
            }

            if (slides.length > 1) {
                timer = window.setInterval(function () {
                    showSlide(current + 1);
                }, 5000);
            }
        }

        if (dots) {
            dots.innerHTML = "";

            slides.forEach(function (slide, index) {
                var dot = document.createElement("button");
                dot.type = "button";
                dot.setAttribute("aria-label", "Show banner " + (index + 1));

                dot.addEventListener("click", function () {
                    showSlide(index);
                    restartTimer();
                });

                dots.appendChild(dot);
            });
        }

        if (previous) {
            previous.addEventListener("click", function () {
                showSlide(current - 1);
                restartTimer();
            });
        }

        if (next) {
            next.addEventListener("click", function () {
                showSlide(current + 1);
                restartTimer();
            });
        }

        showSlide(0);
        restartTimer();
    }

    function initializeAll() {
        document.querySelectorAll(".food-banner-carousel").forEach(
            initializeCarousel
        );
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", initializeAll);
    } else {
        initializeAll();
    }

    document.addEventListener("foundation-contentloaded", initializeAll);
})();
