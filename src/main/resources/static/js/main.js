
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

$(document).ready(function() {
    toggleYearOfDeath("#paternalGrandfatherIsAlive", "#paternalGrandfatherYearOfDeath");
    toggleYearOfDeath("#paternalGrandmotherIsAlive", "#paternalGrandmotherYearOfDeath");
    toggleYearOfDeath("#maternalGrandfatherIsAlive", "#maternalGrandfatherYearOfDeath");
    toggleYearOfDeath("#maternalGrandmotherIsAlive", "#maternalGrandmotherYearOfDeath");
    toggleYearOfDeath("#fatherIsAlive", "#fatherYearOfDeath");
    toggleYearOfDeath("#motherIsAlive", "#motherYearOfDeath");
    toggleYearOfDeath("#individualIsAlive", "#individualYearOfDeath");
    toggleCardColorBySex("#individualCard", "individualSex");

    $("#searchBtn").on("click", function() {
        $("#searchBtn").prop("disabled", true);
        var resultComponent = $("#searchResultCard div.card-body");
        resultComponent.html("<p>Buscando...</p>");

        var searchFamily = JSON.stringify({
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
                "givenName": trimToNull($("#paternalGandfatherGivenName").val()),
                "surname": trimToNull($("#paternalGandfatherSurname").val()),
                "sex": "M",
                "isAlive": $("#paternalGandfatherIsAlive").prop("checked"),
                "yearOfBirth": toNumber($("#paternalGandfatherYearOfBirth").val()),
                "yearOfDeath": toNumber($("#paternalGandfatherYearOfDeath:enabled").val())
            },
            "paternalGrandmother": {
                "givenName": trimToNull($("#paternalGandmotherGivenName").val()),
                "surname": trimToNull($("#paternalGandmotherSurname").val()),
                "sex": "F",
                "isAlive": $("#paternalGandmotherIsAlive").prop("checked"),
                "yearOfBirth": toNumber($("#paternalGandmotherYearOfBirth").val()),
                "yearOfDeath": toNumber($("#paternalGandmotherYearOfDeath:enabled").val())
            },
            "maternalGrandfather": {
                "givenName": trimToNull($("#maternalGandfatherGivenName").val()),
                "surname": trimToNull($("#maternalGandfatherSurname").val()),
                "sex": "M",
                "isAlive": $("#maternalGandfatherIsAlive").prop("checked"),
                "yearOfBirth": toNumber($("#maternalGandfatherYearOfBirth").val()),
                "yearOfDeath": toNumber($("#maternalGandfatherYearOfDeath:enabled").val())
            },
            "maternalGrandmother": {
                "givenName": trimToNull($("#maternalGandmotherGivenName").val()),
                "surname": trimToNull($("#maternalGandmotherSurname").val()),
                "sex": "F",
                "isAlive": $("#maternalGandmotherIsAlive").prop("checked"),
                "yearOfBirth": toNumber($("#maternalGandmotherYearOfBirth").val()),
                "yearOfDeath": toNumber($("#maternalGandmotherYearOfDeath:enabled").val())
            },
            "contact": trimToNull($("#individualContact").val())
        });

        $.ajax({
            type: "POST",
            url: "/api/search/family",
            dataType: "json",
            contentType: "application/json",
            data: searchFamily,
            success: function(data) {
                resultComponent.empty(); // remove the "searching.." message
                $(data.people).each(function() {
                    var personComponent = $("<div>");
                    personComponent.jsonViewer(this, {collapsed: true, rootCollapsable: false});
                    resultComponent.append(personComponent);
                });
                if (data.people.length === 0) {
                    resultComponent.append("<p>No se encontraron resultados.</p>");
                }
            },
            error: function(error) {
                resultComponent.html("Error!");

                // Get error details
                var errorJson = JSON.parse("{\"error\": \"" + error.responseJSON.message + "\"}");
                var personComponent = $("<div>");
                personComponent.jsonViewer(errorJson, {rootCollapsable: false});
                resultComponent.html(personComponent);
            },
            complete: function() {
                $("#searchBtn").prop("disabled", false);
            }
        });
    });
});
