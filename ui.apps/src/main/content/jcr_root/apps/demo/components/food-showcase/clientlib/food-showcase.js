
(function () {
    "use strict";

    function initializeFoodShowcase(root) {
        if (root.dataset.foodInitialized === "true") {
            return;
        }

        const track = root.querySelector(".food-restaurant-track");

        if (!track) {
            return;
        }

        root.dataset.foodInitialized = "true";

        const search = root.querySelector(".food-search");
        const previousButton = root.querySelector(".food-arrow-prev");
        const nextButton = root.querySelector(".food-arrow-next");

        // Update arrow states based on the current carousel position.
        function updateArrowStates() {
            const maxScroll = track.scrollWidth - track.clientWidth;
            const currentScroll = track.scrollLeft;

            if (previousButton) {
                previousButton.disabled = currentScroll <= 2;
            }

            if (nextButton) {
                nextButton.disabled = maxScroll <= 2 ||
                    currentScroll >= maxScroll - 2;
            }
        }

        // Scroll by approximately one visible card.
        function scrollCarousel(direction) {
            const firstVisibleCard = Array.from(
                track.querySelectorAll(".food-restaurant-card")
            ).find(function (card) {
                return !card.hidden;
            });

            if (!firstVisibleCard) {
                return;
            }

            const styles = window.getComputedStyle(track);
            const gap = parseFloat(styles.columnGap) || 0;
            const distance = firstVisibleCard.getBoundingClientRect().width + gap;

            track.scrollBy({
                left: direction * distance,
                behavior: "smooth"
            });
        }

        if (previousButton) {
            previousButton.addEventListener("click", function () {
                scrollCarousel(-1);
            });
        }

        if (nextButton) {
            nextButton.addEventListener("click", function () {
                scrollCarousel(1);
            });
        }

        track.addEventListener("scroll", updateArrowStates, {
            passive: true
        });

        window.addEventListener("resize", updateArrowStates);

        // Search by restaurant name, cuisine, or card details.
        if (search) {
            search.addEventListener("input", function () {
                const query = search.value.trim().toLowerCase();

                track.querySelectorAll(".food-restaurant-card").forEach(
                    function (card) {
                        card.hidden = !card.textContent
                            .toLowerCase()
                            .includes(query);
                    }
                );

                // Start at the beginning after filtering.
                track.scrollTo({
                    left: 0,
                    behavior: "smooth"
                });

                updateArrowStates();
            });
        }

        // View Menu anchors use their href attributes for normal navigation.
        // Deliberately no click handler or popup logic is attached to them.

        requestAnimationFrame(updateArrowStates);
    }

    function initializeAll() {
        document.querySelectorAll(".food-app").forEach(
            initializeFoodShowcase
        );
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", initializeAll);
    } else {
        initializeAll();
    }

    // Support components inserted dynamically by the AEM page editor.
    if (window.MutationObserver && document.body) {
        new MutationObserver(initializeAll).observe(document.body, {
            childList: true,
            subtree: true
        });
    }
})();
