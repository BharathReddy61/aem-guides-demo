
(function (document, window) {
    "use strict";

    var ENDPOINT = "/bin/demo/contact";
    var CSRF_ENDPOINT = "/libs/granite/csrf/token.json";

    function getCsrfToken() {
        return fetch(CSRF_ENDPOINT, {
            method: "GET",
            credentials: "same-origin"
        }).then(function (response) {
            if (!response.ok) {
                throw new Error("Unable to obtain AEM security token.");
            }
            return response.json();
        }).then(function (data) {
            return data.token;
        });
    }

    function postData(data) {
        return getCsrfToken().then(function (token) {
            return fetch(ENDPOINT, {
                method: "POST",
                credentials: "same-origin",
                headers: {
                    "Content-Type":
                        "application/x-www-form-urlencoded; charset=UTF-8",
                    "CSRF-Token": token
                },
                body: new URLSearchParams(data).toString()
            });
        }).then(function (response) {
            return response.json().then(function (result) {
                if (!response.ok || !result.success) {
                    throw new Error(
                        result.message || "Contact operation failed."
                    );
                }
                return result;
            });
        });
    }

    function showMessage(component, message, isError) {
        var messageElement =
            component.querySelector("[data-contact-message]");

        messageElement.textContent = message;
        messageElement.style.color = isError ? "#b00020" : "#18733b";
    }

    function resetForm(component) {
        var form = component.querySelector("[data-contact-form]");

        form.reset();
        form.querySelector("[data-contact-id]").value = "";

        var submitButton =
            form.querySelector("[data-submit-button]");

        submitButton.textContent = "Save Contact";

        form.querySelector("[data-cancel-edit]").hidden = true;
    }

    function renderContacts(component, contacts) {
        var tbody =
            component.querySelector("[data-contact-table-body]");

        tbody.replaceChildren();

        if (!contacts.length) {
            var emptyRow = document.createElement("tr");
            var emptyCell = document.createElement("td");

            emptyCell.colSpan = 4;
            emptyCell.textContent = "No contact records found.";

            emptyRow.appendChild(emptyCell);
            tbody.appendChild(emptyRow);
            return;
        }

        contacts.forEach(function (contact) {
            var row = document.createElement("tr");

            ["name", "email", "mobile"].forEach(function (key) {
                var cell = document.createElement("td");
                cell.textContent = contact[key] || "";
                row.appendChild(cell);
            });

            var actions = document.createElement("td");

            var editButton = document.createElement("button");
            editButton.type = "button";
            editButton.textContent = "Edit";
            editButton.dataset.action = "edit";
            editButton.dataset.id = contact.id;

            var deleteButton = document.createElement("button");
            deleteButton.type = "button";
            deleteButton.textContent = "Delete";
            deleteButton.dataset.action = "delete";
            deleteButton.dataset.id = contact.id;

            actions.appendChild(editButton);
            actions.appendChild(deleteButton);
            row.appendChild(actions);

            tbody.appendChild(row);
        });
    }

    function loadContacts(component) {
        return fetch(ENDPOINT, {
            method: "GET",
            credentials: "same-origin",
            headers: {
                "Accept": "application/json"
            }
        }).then(function (response) {
            if (!response.ok) {
                throw new Error("Unable to load contact records.");
            }
            return response.json();
        }).then(function (contacts) {
            renderContacts(component, contacts);
            return contacts;
        }).catch(function (error) {
            showMessage(component, error.message, true);
        });
    }

    function initialize(component) {
        var form = component.querySelector("[data-contact-form]");
        var tbody =
            component.querySelector("[data-contact-table-body]");

        if (!form || component.dataset.initialized === "true") {
            return;
        }

        component.dataset.initialized = "true";

        loadContacts(component);

        form.addEventListener("submit", function (event) {
            event.preventDefault();

            if (!form.reportValidity()) {
                return;
            }

            var id =
                form.querySelector("[data-contact-id]").value.trim();

            var data = {
                action: id ? "update" : "create",
                id: id,
                name: form.elements.name.value.trim(),
                email: form.elements.email.value.trim(),
                mobile: form.elements.mobile.value.trim()
            };

            var submitButton =
                form.querySelector("[data-submit-button]");

            submitButton.disabled = true;

            postData(data).then(function (result) {
                showMessage(component, result.message, false);
                resetForm(component);
                return loadContacts(component);
            }).catch(function (error) {
                showMessage(component, error.message, true);
            }).finally(function () {
                submitButton.disabled = false;
            });
        });

        form.querySelector("[data-cancel-edit]")
            .addEventListener("click", function () {
                resetForm(component);
                showMessage(component, "Edit cancelled.", false);
            });

        tbody.addEventListener("click", function (event) {
            var button = event.target.closest("button[data-action]");

            if (!button || !tbody.contains(button)) {
                return;
            }

            var id = button.dataset.id;
            var action = button.dataset.action;

            if (action === "edit") {
                fetch(ENDPOINT, {
                    method: "GET",
                    credentials: "same-origin"
                }).then(function (response) {
                    if (!response.ok) {
                        throw new Error("Unable to load contact.");
                    }
                    return response.json();
                }).then(function (contacts) {
                    var contact = contacts.find(function (item) {
                        return item.id === id;
                    });

                    if (!contact) {
                        throw new Error("Contact not found.");
                    }

                    form.querySelector("[data-contact-id]").value =
                        contact.id;
                    form.elements.name.value = contact.name;
                    form.elements.email.value = contact.email;
                    form.elements.mobile.value = contact.mobile;

                    form.querySelector("[data-submit-button]")
                        .textContent = "Update Contact";

                    form.querySelector("[data-cancel-edit]").hidden =
                        false;

                    showMessage(
                        component,
                        "Editing " + contact.name,
                        false
                    );

                    form.scrollIntoView({
                        behavior: "smooth",
                        block: "center"
                    });
                }).catch(function (error) {
                    showMessage(component, error.message, true);
                });
            }

            if (action === "delete") {
                if (!window.confirm(
                    "Are you sure you want to delete this contact?"
                )) {
                    return;
                }

                postData({
                    action: "delete",
                    id: id
                }).then(function (result) {
                    showMessage(component, result.message, false);
                    return loadContacts(component);
                }).catch(function (error) {
                    showMessage(component, error.message, true);
                });
            }
        });
    }

    function initializeAll() {
        document.querySelectorAll("[data-contact-component]")
            .forEach(initialize);
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", initializeAll);
    } else {
        initializeAll();
    }

})(document, window);
