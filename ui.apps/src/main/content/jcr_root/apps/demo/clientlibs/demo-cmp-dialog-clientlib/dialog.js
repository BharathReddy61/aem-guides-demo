
(function (document, $) {
    "use strict";

    var MAX_ITEMS = 4;

    function fieldWrapper(element) {
        return element.closest(".coral-Form-fieldwrapper") ||
               element.closest(".coral-Form-field") ||
               element.parentElement;
    }

    function setVisible(element, visible) {
        if (!element) {
            return;
        }

        var wrapper = fieldWrapper(element);

        if (wrapper) {
            wrapper.hidden = !visible;
        }
    }

    function updateConditionalFields(dialog) {
        var select = dialog.querySelector(
            '[name="./conditionalOption"]'
        );

        var field1 = dialog.querySelector(".demo-field-1");
        var field2 = dialog.querySelector(".demo-field-2");
        var field3 = dialog.querySelector(".demo-field-3");

        if (!select) {
            return;
        }

        var value = select.value;

        setVisible(field1, value === "option1" ||
                           value === "option2" ||
                           value === "option3");

        setVisible(field2, value === "option2" ||
                           value === "option3");

        setVisible(field3, value === "option3");
    }

    function updateRte(dialog) {
        var checkbox = dialog.querySelector(
            '[name="./showRte"]'
        );

        var rte = dialog.querySelector(".demo-rte-field");

        if (!checkbox || !rte) {
            return;
        }

        setVisible(rte, checkbox.checked);
    }

    function updateMultifield(multifield) {
        if (!multifield || !multifield.items) {
            return;
        }

        var count = multifield.items.length;

        var addButton = multifield.querySelector(
            "[coral-multifield-add]"
        );

        if (addButton) {
            addButton.disabled = count >= MAX_ITEMS;
        }

        // Safety guard if an item is added after the limit.
        if (count > MAX_ITEMS) {
            var items = multifield.items;
            var lastItem = items[items.length - 1];

            if (lastItem) {
                lastItem.remove();
            }
        }
    }

    function updateAll(dialog) {
        updateConditionalFields(dialog);
        updateRte(dialog);

        dialog.querySelectorAll(
            "coral-multifield.demo-limited-multifield"
        ).forEach(updateMultifield);
    }

    $(document).on("foundation-contentloaded", function (event) {
        var target = event.target;

        if (target && target.matches &&
            target.matches("coral-multifield")) {
            updateMultifield(target);
        }

        var dialog = document.querySelector(
            "coral-dialog.is-open"
        ) || document;

        updateAll(dialog);
    });

    $(document).on(
        "change",
        '[name="./conditionalOption"], [name="./showRte"]',
        function () {
            var dialog = this.closest("coral-dialog") || document;
            updateAll(dialog);
        }
    );

    $(document).on(
        "coral-collection:add coral-collection:remove",
        "coral-multifield.demo-limited-multifield",
        function () {
            var multifield = this;

            window.setTimeout(function () {
                updateMultifield(multifield);
            }, 0);
        }
    );

})(document, Granite.$);
