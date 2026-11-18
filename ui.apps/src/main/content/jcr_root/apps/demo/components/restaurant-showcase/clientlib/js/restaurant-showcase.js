(function () {
    "use strict";

    function initializeCarousel(root) {
        var track = root.querySelector("[data-carousel-track]");
        var previous = root.querySelector("[data-carousel-prev]");
        var next = root.querySelector("[data-carousel-next]");

        if (!track || !previous || !next || root.dataset.carouselReady) {
            return;
        }

        root.dataset.carouselReady = "true";

        function updateButtons() {
            var maxScroll = track.scrollWidth - track.clientWidth;
            var hasOverflow = maxScroll > 2;

            previous.hidden = !hasOverflow;
            next.hidden = !hasOverflow;
            previous.disabled = !hasOverflow || track.scrollLeft <= 2;
            next.disabled = !hasOverflow ||
                track.scrollLeft >= maxScroll - 2;
        }

        function scrollByCard(direction) {
            var card = track.querySelector(".restaurant-card");

            if (!card) {
                updateButtons();
                return;
            }

            var trackStyle = window.getComputedStyle(track);
            var gap = parseFloat(trackStyle.columnGap) || 20;
            var distance = card.getBoundingClientRect().width + gap;

            track.scrollBy({
                left: direction * distance,
                behavior: "smooth"
            });
        }

        previous.addEventListener("click", function () {
            scrollByCard(-1);
        });

        next.addEventListener("click", function () {
            scrollByCard(1);
        });

        track.addEventListener("scroll", updateButtons, { passive: true });
        window.addEventListener("resize", updateButtons);

        if ("ResizeObserver" in window) {
            new ResizeObserver(updateButtons).observe(track);
        }

        updateButtons();
    }

    function initializeAll() {
        document.querySelectorAll("[data-restaurant-carousel]")
            .forEach(initializeCarousel);
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", initializeAll);
    } else {
        initializeAll();
    }
})();
