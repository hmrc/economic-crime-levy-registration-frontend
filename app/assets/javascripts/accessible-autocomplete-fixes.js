// Addresses various issues with the accessible-autocomplete component, this can be removed once these issues have been addressed in https://github.com/hmrc/play-frontend-hmrc

if (document.querySelector('[data-module="hmrc-accessible-autocomplete"]') != null) {
    var originalSelect = document.querySelector('[data-module="hmrc-accessible-autocomplete"]');

    // =====================================================
    // Polyfill autocomplete once loaded
    // =====================================================
    var checkForLoad = setInterval(checkForAutocompleteLoad, 50);
    var parentForm = upTo(originalSelect, 'form');

    function polyfillAutocomplete() {
        var combo = parentForm.querySelector('[role="combobox"]');

        // =====================================================
        // Update autocomplete once loaded with fallback's aria attributes
        // Ensures hint and error are read out before usage instructions
        // =====================================================
        if (originalSelect && originalSelect.getAttribute('aria-describedby') > "") {
            if (parentForm) {
                if (combo) {
                    combo.setAttribute('aria-describedby', originalSelect.getAttribute('aria-describedby') + ' ' + combo.getAttribute('aria-describedby'));
                }
            }
        }

        // =====================================================
        // Update autocomplete once loaded with error styling if needed
        // This won't work if the autocomplete css is loaded after the frontend library css because
        // the autocomplete's border will override the error class's border (they are both the same specificity)
        // but we can use the class assigned to build a more specific rule
        // =====================================================
        setErrorClass();

        function setErrorClass() {
            if (originalSelect && originalSelect.classList.contains("govuk-select--error")) {
                if (parentForm) {
                    if (combo) {
                        combo.classList.add("govuk-input--error");
                        // Also set up an event listener to check for changes to input so we know when to repeat the copy
                        combo.addEventListener('focus', function () {
                            setErrorClass()
                        });
                        combo.addEventListener('blur', function () {
                            setErrorClass()
                        });
                        combo.addEventListener('change', function () {
                            setErrorClass()
                        });
                    }
                }
            }
        }

        // =====================================================
        // Ensure when user replaces valid answer with a non-valid answer, then valid answer is not retained
        // =====================================================
        var holdSubmit = true;
        parentForm.addEventListener('submit', function (e) {
            if (holdSubmit) {
                e.preventDefault()
                if (originalSelect.querySelectorAll('[selected]').length > 0 || originalSelect.value > "") {

                    var resetSelect = false;

                    if (originalSelect.value) {
                        if (combo.value != originalSelect.querySelector('option[value="' + originalSelect.value + '"]').text) {
                            resetSelect = true;
                        }
                    }
                    if (resetSelect) {
                        originalSelect.value = "";
                        if (originalSelect.querySelectorAll('[selected]').length > 0) {
                            originalSelect.querySelectorAll('[selected]')[0].removeAttribute('selected');
                        }
                    }
                }

                holdSubmit = false;

                HTMLFormElement.prototype.submit.call(parentForm);
            }
        })

    }

    function checkForAutocompleteLoad() {
        if (parentForm.querySelector('[role="combobox"]')) {
            clearInterval(checkForLoad)
            polyfillAutocomplete();
        }
    }
}

// Find first ancestor of el with tagName
// or undefined if not found
function upTo(el, tagName) {
    tagName = tagName.toLowerCase();

    while (el && el.parentNode) {
        el = el.parentNode;
        if (el.tagName && el.tagName.toLowerCase() == tagName) {
            return el;
        }
    }

    // Many DOM methods return null if they don't
    // find the element they are searching for
    // It would be OK to omit the following and just
    // return undefined
    return null;
}
