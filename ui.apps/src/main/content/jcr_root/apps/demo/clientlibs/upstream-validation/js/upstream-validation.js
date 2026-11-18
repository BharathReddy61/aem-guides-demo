
(function (document, window) {
    "use strict";

    /**
     * Convert HTML into readable text.
     * Empty HTML such as <p><br></p> is treated as unauthored.
     */
    function hasMeaningfulContent(value) {
        if (value === null || value === undefined) {
            return false;
        }

        var text = String(value)
            .replace(/<br\s*\/?>/gi, "")
            .replace(/<\/p>/gi, "")
            .replace(/<[^>]*>/g, "")
            .replace(/&nbsp;/gi, " ")
            .replace(/&#160;/gi, " ")
            .replace(/&amp;/gi, "&")
            .trim();

        return text.length > 0;
    }

    /**
     * Find the actual field component inside a dialog field wrapper.
     * Outer Coral components are selected before their internal inputs.
     */
    function getFieldRoot(wrapper) {
        var selector = [
            "coral-datepicker",
            "coral-numberinput",
            "foundation-autocomplete",
            ".cq-RichText",
            "coral-select",
            "coral-checkbox",
            "coral-switch",
            "coral-textfield",
            "input.coral-Form-field:not([type='hidden'])",
            "textarea.coral-Form-field",
            "select.coral-Form-field"
        ].join(",");

        return wrapper.querySelector(selector);
    }

    /**
     * Get the visible label dynamically.
     */
    function getFieldLabel(wrapper, field) {
        var label = wrapper.querySelector(
            ".coral-Form-fieldlabel, label"
        );

        if (label && label.textContent.trim()) {
            return label.textContent.trim();
        }

        return field.getAttribute("aria-label") ||
            field.getAttribute("name") ||
            "Unnamed field";
    }

    /**
     * Check whether a Pathfield contains a selected path.
     */
    function isPathfieldAuthored(field) {
        var tagList = field.querySelector("coral-taglist");

        if (tagList) {
            var tags = tagList.querySelectorAll("coral-tag");

            for (var i = 0; i < tags.length; i++) {
                var tagValue = tags[i].value ||
                    tags[i].getAttribute("value") ||
                    tags[i].textContent;

                if (hasMeaningfulContent(tagValue)) {
                    return true;
                }
            }
        }

        return hasMeaningfulContent(field.value);
    }

    /**
     * Check whether RichText contains actual content.
     */
    function isRichTextAuthored(wrapper) {
        var richTextInput = wrapper.querySelector(
            "[data-cq-richtext-input='true']"
        );

        if (richTextInput &&
            hasMeaningfulContent(richTextInput.value)) {
            return true;
        }

        var editable = wrapper.querySelector(
            ".cq-RichText-editable, [contenteditable='true']"
        );

        if (editable &&
            hasMeaningfulContent(editable.innerHTML)) {
            return true;
        }

        return false;
    }

    /**
     * Check whether a normal or Coral field has a value.
     */
    function isFieldAuthored(wrapper, field) {
        var tagName = field.tagName.toLowerCase();

        // RichText
        if (wrapper.querySelector(".cq-RichText")) {
            return isRichTextAuthored(wrapper);
        }

        // Pathfield
        if (tagName === "foundation-autocomplete") {
            return isPathfieldAuthored(field);
        }

        // Checkbox and switch
        if (tagName === "coral-checkbox" ||
            tagName === "coral-switch") {
            return field.checked === true;
        }

        // Standard checkbox/radio
        if (field.type === "checkbox" ||
            field.type === "radio") {
            return field.checked === true;
        }

        // Multi-select
        if (field.tagName === "SELECT" && field.multiple) {
            return Array.from(field.selectedOptions).some(
                function (option) {
                    return hasMeaningfulContent(option.value);
                }
            );
        }

        // Coral Datefield, Numberfield, and other Coral fields
        if ("value" in field) {
            return hasMeaningfulContent(field.value);
        }

        // Fallback for other supported fields
        var namedInput = wrapper.querySelector(
            "input[name]:not([type='hidden']), textarea[name], select[name]"
        );

        return namedInput
            ? hasMeaningfulContent(namedInput.value)
            : false;
    }

    /**
     * Dynamically discover all dialog fields.
     * No field names are hardcoded.
     */
    function getDialogFields(dialog) {
        var wrappers = dialog.querySelectorAll(
            ".coral-Form-fieldwrapper"
        );

        var fields = [];

        wrappers.forEach(function (wrapper) {
            var field = getFieldRoot(wrapper);

            if (!field || field.disabled) {
                return;
            }

            fields.push({
                wrapper: wrapper,
                field: field,
                label: getFieldLabel(wrapper, field)
            });
        });

        return fields;
    }

    /**
     * Build a readable missing-fields message.
     */
    function formatMissingFields(names) {
        if (names.length === 1) {
            return names[0] + " is not authored in the cq:dialog.";
        }

        if (names.length === 2) {
            return names[0] + " and " + names[1] +
                " are not authored in the cq:dialog.";
        }

        var last = names.pop();

        return names.join(", ") + ", and " + last +
            " are not authored in the cq:dialog.";
    }

    /**
     * Validate all discovered fields before saving.
     */
    function validateDialog(dialog) {
        if (!dialog) {
            return true;
        }

        var fields = getDialogFields(dialog);
        var missingFields = [];

        console.log("========== TASK-18 VALIDATION ==========");
        console.log("Fields discovered:", fields.length);

        fields.forEach(function (item) {
            var authored = isFieldAuthored(
                item.wrapper,
                item.field
            );

            console.log(
                item.label + ": " +
                (authored ? "AUTHORED" : "MISSING")
            );

            if (!authored) {
                missingFields.push(item.label);
            }
        });

        // No fields discovered or no fields authored
        if (fields.length === 0 ||
            missingFields.length === fields.length) {

            window.alert(
                "None of the fields are authored in the cq:dialog."
            );

            return false;
        }

        // Some fields are missing
        if (missingFields.length > 0) {
            window.alert(formatMissingFields(missingFields));
            return false;
        }

        console.log("All dialog fields are authored.");
        return true;
    }

    /**
     * Identify the active component dialog.
     */
    function getActiveDialog(button) {
        return button.closest("coral-dialog") ||
            document.querySelector("coral-dialog[open]");
    }

    /**
     * Intercept the Done button before AEM saves the dialog.
     */
    document.addEventListener("click", function (event) {
        var target = event.target;

        if (!target || !target.closest) {
            return;
        }

        var button = target.closest(".cq-dialog-submit");

        if (!button) {
            return;
        }

        var dialog = getActiveDialog(button);

        if (!validateDialog(dialog)) {
            event.preventDefault();
            event.stopPropagation();
            event.stopImmediatePropagation();
        }
    }, true);

    console.log("TASK-18 Upstream validation script loaded.");

})(document, window);
