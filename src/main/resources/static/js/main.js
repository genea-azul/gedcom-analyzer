
$(document).ready(function() {
    var token = $("meta[name='_csrf']").attr("content");
    var header = $("meta[name='_csrf_header']").attr("content");

    if (header) {
        $(document).ajaxSend(function(e, xhr, options) {
            if (options.type == "POST") {
                xhr.setRequestHeader(header, token);
            }
        });
    }
});

var toggleYearOfDeath = function(isAliveComponent, yearOfDeathComponent) {
    $(isAliveComponent).on('change', function() {
        var show = $(isAliveComponent).prop("checked");
        $("div:has(> " + yearOfDeathComponent + ")").toggleClass("d-none", show);
        $(yearOfDeathComponent).prop("disabled", show);
    });
};

var toggleCardColorBySex = function(cardComponent, sexRadioComponent) {
    $("input[type=radio][name=" + sexRadioComponent + "]").on('change', function() {
        var isMale = $(this).val() === "M";
        $(cardComponent).toggleClass("border-secondary", isMale);
        $(cardComponent).toggleClass("border-danger", !isMale);
        $(cardComponent + " div.card-header").toggleClass("text-bg-secondary", isMale);
        $(cardComponent + " div.card-header").toggleClass("text-bg-danger", !isMale);
    });
};

var toggleContainers = function(displayBtnComponent, container1Component, container2Component) {
    $(displayBtnComponent).on('change', function() {
        var show = $(displayBtnComponent).prop("checked");
        $(container1Component).toggleClass("d-none", show);
        $(container2Component).toggleClass("d-none", !show);
    });
};

var trimToNull = function(str) {
    if (isEmpty(str)) {
        return null;
    }
    var trimmed = str.trim();
    return isEmpty(trimmed) ? null : trimmed;
};

var isEmpty = function(str) {
    return str == null || typeof str == "undefined" || str.length == 0;
};

var toNumber = function(str) {
    return isEmpty(str) ? null : parseInt(str);
};

var maxLengthCheck = function(input) {
    if (input.value.length > input.max.length) {
        input.value = input.value.slice(0, input.max.length)
    }
};

$(document).ready(function() {
    toggleYearOfDeath("#paternalGrandfatherIsAlive", "#paternalGrandfatherYearOfDeath");
    toggleYearOfDeath("#paternalGrandmotherIsAlive", "#paternalGrandmotherYearOfDeath");
    toggleYearOfDeath("#maternalGrandfatherIsAlive", "#maternalGrandfatherYearOfDeath");
    toggleYearOfDeath("#maternalGrandmotherIsAlive", "#maternalGrandmotherYearOfDeath");
    toggleYearOfDeath("#fatherIsAlive", "#fatherYearOfDeath");
    toggleYearOfDeath("#motherIsAlive", "#motherYearOfDeath");
    toggleYearOfDeath("#individualIsAlive", "#individualYearOfDeath");
    toggleCardColorBySex("#individualCard", "individualSex");
    toggleContainers("#grandparentsContainerShowBtn", "#grandparentsContainerShowBtnContainer", "#grandparentsContainer");

    $("input[type=number]").on("input", function() {
        maxLengthCheck(this);
    });
});

$(document).ready(function() {
    $("#searchBtn").on("click", function() {
        $("#searchBtn").prop("disabled", true);
        var resultComponent = $("#searchResultCard div.card-body");
        resultComponent.html("<p>Buscando...</p>");

        var searchFamilyRequest = {
            "individual": {
                "givenName": trimToNull($("#individualGivenName").val()),
                "surname": trimToNull($("#individualSurname").val()),
                "sex": trimToNull($("input[type=radio][name=individualSex]:checked").val()),
                "isAlive": $("#individualIsAlive").prop("checked"),
                "yearOfBirth": toNumber($("#individualYearOfBirth").val()),
                "yearOfDeath": toNumber($("#individualYearOfDeath:enabled").val())
            },
            "father": {
                "givenName": trimToNull($("#fatherGivenName").val()),
                "surname": trimToNull($("#fatherSurname").val()),
                "sex": "M",
                "isAlive": $("#fatherIsAlive").prop("checked"),
                "yearOfBirth": toNumber($("#fatherYearOfBirth").val()),
                "yearOfDeath": toNumber($("#fatherYearOfDeath:enabled").val())
            },
            "mother": {
                "givenName": trimToNull($("#motherGivenName").val()),
                "surname": trimToNull($("#motherSurname").val()),
                "sex": "F",
                "isAlive": $("#motherIsAlive").prop("checked"),
                "yearOfBirth": toNumber($("#motherYearOfBirth").val()),
                "yearOfDeath": toNumber($("#motherYearOfDeath:enabled").val())
            },
            "paternalGrandfather": {
                "givenName": trimToNull($("#paternalGrandfatherGivenName").val()),
                "surname": trimToNull($("#paternalGrandfatherSurname").val()),
                "sex": "M",
                "isAlive": $("#paternalGrandfatherIsAlive").prop("checked"),
                "yearOfBirth": toNumber($("#paternalGrandfatherYearOfBirth").val()),
                "yearOfDeath": toNumber($("#paternalGrandfatherYearOfDeath:enabled").val())
            },
            "paternalGrandmother": {
                "givenName": trimToNull($("#paternalGrandmotherGivenName").val()),
                "surname": trimToNull($("#paternalGrandmotherSurname").val()),
                "sex": "F",
                "isAlive": $("#paternalGrandmotherIsAlive").prop("checked"),
                "yearOfBirth": toNumber($("#paternalGrandmotherYearOfBirth").val()),
                "yearOfDeath": toNumber($("#paternalGrandmotherYearOfDeath:enabled").val())
            },
            "maternalGrandfather": {
                "givenName": trimToNull($("#maternalGrandfatherGivenName").val()),
                "surname": trimToNull($("#maternalGrandfatherSurname").val()),
                "sex": "M",
                "isAlive": $("#maternalGrandfatherIsAlive").prop("checked"),
                "yearOfBirth": toNumber($("#maternalGrandfatherYearOfBirth").val()),
                "yearOfDeath": toNumber($("#maternalGrandfatherYearOfDeath:enabled").val())
            },
            "maternalGrandmother": {
                "givenName": trimToNull($("#maternalGrandmotherGivenName").val()),
                "surname": trimToNull($("#maternalGrandmotherSurname").val()),
                "sex": "F",
                "isAlive": $("#maternalGrandmotherIsAlive").prop("checked"),
                "yearOfBirth": toNumber($("#maternalGrandmotherYearOfBirth").val()),
                "yearOfDeath": toNumber($("#maternalGrandmotherYearOfDeath:enabled").val())
            },
            "contact": trimToNull($("#individualContact").val())
        };

        postProcessRequest(searchFamilyRequest);

        $.ajax({
            type: "POST",
            url: "/api/search/family",
            dataType: "json",
            contentType: "application/json",
            data: JSON.stringify(searchFamilyRequest),
            success: function(data) {
                // remove the "searching.." message
                resultComponent.empty();

                $(data.people).each(function() {
                    var personComponent = $("<div>");
                    personComponent.jsonViewer(this, {collapsed: true, rootCollapsable: false});
                    resultComponent.append(personComponent);
                });

                if (data.people.length === 0) {
                    resultComponent.append("<p>No se encontraron resultados.</p>");
                }
            },
            error: function(xhr) {
                console.log(xhr);
                resultComponent.html("Error!");

                // Get error details
                try {
                    var errorJson;
                    if (xhr.status >= 500 && xhr.status < 600) {
                        errorJson = JSON.parse("{\"error\": \"El servidor se est\u00E1 reiniciando, intent\u00E1 de nuevo.\"}");
                    } else {
                        errorJson = JSON.parse("{\"error\": \"" + xhr.responseJSON.message + "\"}");
                    }

                    var personComponent = $("<div>");
                    personComponent.jsonViewer(errorJson, {rootCollapsable: false});
                    resultComponent.html(personComponent);

                } catch (ex) {
                    console.log(xhr);
                    console.log(ex);
                }
            },
            complete: function() {
                $("#searchBtn").prop("disabled", false);
                $('html, body').animate({
                    scrollTop: $("#searchResultCard").offset().top
                }, 1000);
            }
        });
    });
});

var postProcessRequest = function(searchFamilyRequest) {
    var personsInRq = [
        "individual",
        "father",
        "mother",
        "paternalGrandfather",
        "paternalGrandmother",
        "maternalGrandfather",
        "maternalGrandmother"
    ];
    $(personsInRq).each(function() {
        if (isPersonEmpty(searchFamilyRequest[this])) {
            delete searchFamilyRequest[this];
        }
    });
};

var isPersonEmpty = function(person) {
    var personProps = [
        "givenName",
        "surname",
        "yearOfBirth",
        "yearOfDeath"
    ];
    for (var prop of personProps) {
        if (person[prop] != null) {
            return false;
        }
    }
    return true;
};
