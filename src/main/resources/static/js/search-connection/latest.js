$(document).ready(function() {
    var searchParams = new URLSearchParams(window.location.search);

    $.ajax({
        type: "GET",
        url: "/api/admin/search/connection/latest",
        contentType: "application/json",
        data: {
            page: searchParams.get("page") || undefined,
            size: searchParams.get("size") || undefined,
            isMatch: searchParams.get("isMatch") || undefined,
            isReviewed: (isToReview ? false : (searchParams.get("isReviewed") || undefined)),
            hasContact: searchParams.get("hasContact") || undefined,
            token: searchParams.get("token") || undefined
        },
        success: function(data) {
            var token = searchParams.get("token");
            data.forEach((element, index) => data[index] = removeEmpty(appendTokenToLinks(element, token)));
            $("#result-container").jsonViewer(data, {collapsed: false, rootCollapsable: false});
        },
        error: function(xhr) {
            console.log(xhr);
            $("#result-container").jsonViewer(JSON.parse("{\"error\": \"!\"}"), {collapsed: false, rootCollapsable: false});

            // Get error details
            try {
                var errorJson;
                if (xhr.status >= 500 && xhr.status < 600) {
                    errorJson = "{\"error\": \"El servidor se est\u00E1 reiniciando, intent\u00E1 de nuevo.\"}";
                } else if (xhr.status == 0) {
                    errorJson = "{\"error\": \"El servidor est\u00E1 ca\u00EDdo, intent\u00E1 de nuevo.\"}";
                } else {
                    errorJson = "{\"error\": \"" + xhr.responseJSON.error + " (" + xhr.responseJSON.message + ")\"}";
                }

                $("#result-container").jsonViewer(JSON.parse(errorJson), {collapsed: false, rootCollapsable: false});

            } catch (ex) {
                console.log(ex);
            }
        }
    });
});

var removeEmpty = function(obj) {
    return Object.entries(obj)
        .filter(([_, v]) => v != null)
        .reduce((acc, [k, v]) => ({ ...acc, [k]: v === Object(v) ? removeEmpty(v) : v }), {});
}

/* Appends &token=... (or ?token=... if the link has no query string yet) to every
   *Link field, so clicking an action link (e.g. markReviewedLink) in the JSON
   viewer carries the same admin token used to load this page. */
var appendTokenToLinks = function(obj, token) {
    if (!token) return obj;
    Object.keys(obj).forEach(function(key) {
        if (key.toLowerCase().endsWith("link") && typeof obj[key] === "string") {
            obj[key] += (obj[key].indexOf("?") === -1 ? "?" : "&") + "token=" + encodeURIComponent(token);
        }
    });
    return obj;
}
