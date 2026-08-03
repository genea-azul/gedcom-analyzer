$(document).ready(function() {
    var searchParams = new URLSearchParams(window.location.search);

    $.ajax({
        type: "GET",
        url: "/api/admin/tree-builder/latest",
        contentType: "application/json",
        data: {
            page: searchParams.get("page") || undefined,
            size: searchParams.get("size") || undefined,
            token: searchParams.get("token") || undefined
        },
        success: function(data) {
            data.forEach((element, index) => {
                if (element.payload) {
                    try { element.payload = JSON.parse(element.payload); } catch (e) {}
                }
                data[index] = removeEmpty(element);
            });
            $("#result-container").jsonViewer(data, {collapsed: false, rootCollapsable: false});
        },
        error: function(xhr) {
            console.log(xhr);
            $("#result-container").jsonViewer(JSON.parse("{\"error\": \"!\"}"), {collapsed: false, rootCollapsable: false});

            try {
                var errorJson;
                if (xhr.status >= 500 && xhr.status < 600) {
                    errorJson = "{\"error\": \"El servidor se está reiniciando, intentá de nuevo.\"}";
                } else if (xhr.status == 0) {
                    errorJson = "{\"error\": \"El servidor está caído, intentá de nuevo.\"}";
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
