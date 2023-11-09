(function (document, window) {
    // To enable this script to work in IE11 as forEach is not supported on a NodeList
    if (typeof NodeList.prototype.forEach !== 'function') {
        NodeList.prototype.forEach = Array.prototype.forEach;
    }

    document.querySelectorAll('button[data-backlink]')
        .forEach(function (link) {

            if (window.history) {
                // store referrer value to cater for IE
                const docReferrer = document.referrer;

                // hide the backlink if the referrer is on a different domain or the referrer is not set
                if (docReferrer === '' || docReferrer.indexOf(window.location.host) === -1) {
                    $module.classList.add('hmrc-hidden-backlink');
                } else {
                    // prevent resubmit warning
                    if (window.history.replaceState && typeof window.history.replaceState === 'function') {
                        window.history.replaceState(null, null, window.location.href);
                    }

                    link.addEventListener('click', function (event) {
                        event.preventDefault();
                        if (window.history.back && typeof window.history.back === 'function') {
                            window.history.back();
                        }
                    });
                }
            }
        })
})(document, window);