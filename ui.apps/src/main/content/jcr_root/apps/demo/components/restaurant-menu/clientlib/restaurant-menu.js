
(() => {
    const KEY = "demo.restaurantMenu.cart.v3";
    const $ = (s, root = document) => root.querySelector(s);
    const $$ = (s, root = document) => [...root.querySelectorAll(s)];
    const money = n => "₹" + Number(n).toFixed(2);

    let cart = {};
    try {
        cart = JSON.parse(localStorage.getItem(KEY) || "{}") || {};
    } catch (e) {
        cart = {};
    }

    function save() {
        try {
            localStorage.setItem(KEY, JSON.stringify(cart));
        } catch (e) {
            console.warn("Cart could not be saved in browser storage.");
        }
        render();
    }

    function openCart() {
        const panel = $("[data-food-cart]");
        const overlay = $("[data-food-overlay]");
        if (!panel || !overlay) {
            console.error("Cart panel or overlay is missing from HTL.");
            return;
        }
        panel.hidden = false;
        overlay.hidden = false;
        panel.setAttribute("aria-hidden", "false");
        $$("[data-cart-toggle]").forEach(b =>
            b.setAttribute("aria-expanded", "true"));
        document.body.classList.add("food-cart-open");
    }

    function closeCart() {
        const panel = $("[data-food-cart]");
        const overlay = $("[data-food-overlay]");
        if (panel) {
            panel.hidden = true;
            panel.setAttribute("aria-hidden", "true");
        }
        if (overlay) overlay.hidden = true;
        $$("[data-cart-toggle]").forEach(b =>
            b.setAttribute("aria-expanded", "false"));
        document.body.classList.remove("food-cart-open");
    }

    function render() {
        const entries = Object.entries(cart).filter(
            ([, item]) => item && item.quantity > 0
        );
        const count = entries.reduce((n, [, item]) => n + item.quantity, 0);
        const total = entries.reduce(
            (n, [, item]) => n + item.price * item.quantity, 0
        );

        $$("[data-cart-count], [data-food-cart-count]").forEach(
            el => el.textContent = count
        );

        const totalEl = $("[data-food-total]");
        if (totalEl) totalEl.textContent = money(total);

        const lines = $("[data-food-cart-lines]");
        if (lines) {
            lines.replaceChildren();

            entries.forEach(([id, item]) => {
                const row = document.createElement("div");
                row.className = "food-cart-line";

                const info = document.createElement("div");
                const name = document.createElement("strong");
                name.textContent = item.name;

                const price = document.createElement("p");
                price.textContent =
                    money(item.price) + " each · " + money(item.price * item.quantity) + " total";
                info.append(name, price);

                const controls = document.createElement("div");
                controls.className = "food-cart-line-controls";

                const minus = document.createElement("button");
                minus.type = "button";
                minus.textContent = "−";
                minus.dataset.foodDrawerDecrease = id;
                minus.setAttribute("aria-label", "Decrease quantity");

                const qty = document.createElement("span");
                qty.textContent = item.quantity;

                const plus = document.createElement("button");
                plus.type = "button";
                plus.textContent = "+";
                plus.dataset.foodDrawerIncrease = id;
                plus.setAttribute("aria-label", "Increase quantity");

                controls.append(minus, qty, plus);

                const remove = document.createElement("button");
                remove.type = "button";
                remove.className = "food-cart-remove";
                remove.textContent = "Remove";
                remove.dataset.foodRemove = id;

                row.append(info, controls, remove);
                lines.append(row);
            });
        }

        const empty = $("[data-food-cart-empty]");
        if (empty) empty.hidden = count > 0;

        const checkout = $("[data-food-checkout]");
        if (checkout) checkout.disabled = count === 0;

        $$("[data-food-card]").forEach(card => {
            const qty = cart[card.dataset.itemId]?.quantity || 0;
            const add = $("[data-food-add]", card);
            const controls = $("[data-food-quantity-control]", card);
            const label = $("[data-food-quantity]", card);
            if (add) add.hidden = qty > 0;
            if (controls) controls.hidden = qty === 0;
            if (label) label.textContent = qty || 1;
        });
    }

    function change(id, delta) {
        if (!cart[id]) return;
        cart[id].quantity += delta;
        if (cart[id].quantity <= 0) delete cart[id];
        save();
    }

    document.addEventListener("click", event => {
        const target = event.target;

        if (target.closest("[data-cart-toggle]")) {
            const panel = $("[data-food-cart]");
            if (panel && !panel.hidden) closeCart();
            else openCart();
            return;
        }

        if (target.closest("[data-food-close]") ||
            target.closest("[data-food-overlay]")) {
            closeCart();
            return;
        }

        const add = target.closest("[data-food-add]");
        const inc = target.closest("[data-food-increase]");
        const dec = target.closest("[data-food-decrease]");

        if (add || inc || dec) {
            const card = target.closest("[data-food-card]");
            if (!card) return;

            const id = card.dataset.itemId;
            const price = Number(card.dataset.itemPrice);
            if (!id || !Number.isFinite(price) || price < 0) {
                console.error("Invalid menu item ID or price:", card);
                return;
            }

            if (!cart[id]) {
                cart[id] = {
                    name: card.dataset.itemName || "Menu item",
                    price: price,
                    quantity: 0
                };
            }

            cart[id].quantity += dec ? -1 : 1;
            if (cart[id].quantity <= 0) delete cart[id];
            save();
            return;
        }

        const plus = target.closest("[data-food-drawer-increase]");
        const minus = target.closest("[data-food-drawer-decrease]");

        if (plus || minus) {
            change(
                plus ? plus.dataset.foodDrawerIncrease :
                       minus.dataset.foodDrawerDecrease,
                plus ? 1 : -1
            );
            return;
        }

        const remove = target.closest("[data-food-remove]");
        if (remove) {
            delete cart[remove.dataset.foodRemove];
            save();
            return;
        }

        if (target.closest("[data-food-clear]")) {
            cart = {};
            save();
        }
    });

    document.addEventListener("keydown", event => {
        if (event.key === "Escape") closeCart();
    });

    let activeCategory = "all";
    const search = $("[data-food-search]");

    function filterCards() {
        const query = (search?.value || "").trim().toLowerCase();
        let visible = 0;

        $$("[data-food-card]").forEach(card => {
            const name = (card.dataset.itemName || "").toLowerCase();
            const category = (card.dataset.itemCategory || "").toLowerCase();

            const textMatch = !query ||
                (name + " " + category).includes(query);

            let categoryMatch = activeCategory === "all";

            if (activeCategory === "other") {
                categoryMatch = !["pizza", "burger", "dessert"].some(
                    value => category.includes(value)
                );
            } else if (activeCategory !== "all") {
                categoryMatch = category.includes(activeCategory);
            }

            card.hidden = !(textMatch && categoryMatch);
            if (!card.hidden) visible++;
        });

        const noResults = $("[data-food-no-results]");
        if (noResults) noResults.hidden = visible > 0;
    }

    if (search) search.addEventListener("input", filterCards);

    $$("[data-food-filter]").forEach(button => {
        button.addEventListener("click", () => {
            activeCategory = button.dataset.foodFilter;
            $$("[data-food-filter]").forEach(b =>
                b.classList.toggle("is-active", b === button));
            filterCards();
        });
    });

    window.addEventListener("storage", event => {
        if (event.key === KEY) {
            try {
                cart = JSON.parse(event.newValue || "{}") || {};
            } catch (e) {
                cart = {};
            }
            render();
        }
    });

    render();
})();
