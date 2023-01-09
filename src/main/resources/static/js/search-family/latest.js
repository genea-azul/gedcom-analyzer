$(document).ready(function() {
    var searchParams = new URLSearchParams(window.location.search);

    $.ajax({
        type: "GET",
        url: "/api/search/family/latest",
        contentType: "application/json",
        data: {
            page: searchParams.get("page"),
            size: searchParams.get("size")
        },
        success: function(data) {
            data.forEach((element, index) => data[index] = removeEmpty(element));
            $("#result-container").jsonViewer(data, {collapsed: false, rootCollapsable: false});
        },
        error: function(xhr) {
            console.log(xhr);
            $("#result-container").jsonViewer("{\"error\": \"!\"}", {collapsed: false, rootCollapsable: false});

            // Get error details
            try {
                var errorJson;
                if (xhr.status >= 500 && xhr.status < 600) {
                    errorJson = "{\"error\": \"El servidor se est\u00E1 reiniciando, intent\u00E1 de nuevo.\"}";
                } else if (xhr.status == 0) {
                    errorJson = "{\"error\": \"El servidor est\u00E1 ca\u00EDdo, intent\u00E1 de nuevo.\"}";
                } else {
                    errorJson = "{\"error\": \"" + xhr.responseJSON.message + "\"}";
                }

                $("#result-container").jsonViewer(errorJson, {collapsed: false, rootCollapsable: false});

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
