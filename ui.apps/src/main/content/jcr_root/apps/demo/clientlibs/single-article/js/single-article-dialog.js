(function (document, $) {
    "use strict";

    var CHECKBOX = "input[name='./enablePii']";
    var CSS_FIELD = "input[name='./cssClass']";
    var DELETE_HINT = "input[name='./cssClass@Delete']";

    /**
     * Find the form containing this component's dialog fields.
     */
    function getDialogForm(element) {
        return element && element.closest
            ? element.closest("form")
            : null;
    }

    /**
     * Synchronize the CSS Class field for one dialog form.
     */
    function syncDialog(form) {
        if (!form) {
            return;
        }

        var checkbox = form.querySelector(CHECKBOX);
        var cssField = form.querySelector(CSS_FIELD);

        if (!checkbox || !cssField) {
            return;
        }

        var enabled = checkbox.checked;
        var deleteHint = form.querySelector(DELETE_HINT);

        if (enabled) {
            // Enable the CSS Class field.
            cssField.disabled = false;

            // Remove the delete instruction when the field is enabled.
            if (deleteHint) {
                deleteHint.remove();
            }
        } else {
            // Clear the value and disable the field.
            cssField.value = "";
            cssField.disabled = true;

            // Request deletion of any previously saved cssClass property.
            if (!deleteHint) {
                deleteHint = document.createElement("input");
                deleteHint.type = "hidden";
                deleteHint.name = "./cssClass@Delete";
                deleteHint.value = "true";
                form.appendChild(deleteHint);
            }
        }

        console.log("[Single Article] Dialog state:", {
            checkboxChecked: checkbox.checked,
            cssFieldDisabled: cssField.disabled,
            cssFieldValue: cssField.value,
            deleteHintPresent: !!form.querySelector(DELETE_HINT)
        });
    }

    /**
     * Initialize all matching dialog forms within a given root.
     */
    function initialize(root) {
        if (!root) {
            return;
        }

        if (root.matches && root.matches("form")) {
            syncDialog(root);
        }

        if (root.querySelectorAll) {
            root.querySelectorAll("form").forEach(function (form) {
                if (form.querySelector(CHECKBOX)) {
                    syncDialog(form);
                }
            });
        }

        // The event target itself might contain the fields without a form
        // being directly represented by the event target.
        if (root.matches && root.matches(CHECKBOX)) {
            syncDialog(getDialogForm(root));
        }
    }

    /**
     * Handle checkbox changes.
     */
    document.addEventListener("change", function (event) {
        if (!event.target || !event.target.matches(CHECKBOX)) {
            return;
        }

        syncDialog(getDialogForm(event.target));
    }, true);

    /**
     * Initialize fields when AEM loads dialog content.
     */
    if ($) {
        $(document).on(
            "foundation-contentloaded.singleArticlePII",
            function (event) {
                initialize(event.target);
            }
        );

        $(document).on(
            "dialog-ready.singleArticlePII",
            function () {
                initialize(document);
            }
        );
    }

    /**
     * Ensure the correct state immediately before submission.
     */
    document.addEventListener("submit", function (event) {
        var form = event.target;

        if (form && form.querySelector && form.querySelector(CHECKBOX)) {
            syncDialog(form);
        }
    }, true);

    // Initialize any matching dialog that is already present.
    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", function () {
            initialize(document);
        });
    } else {
        initialize(document);
    }

})(document, window.Granite ? window.Granite.$ : null);