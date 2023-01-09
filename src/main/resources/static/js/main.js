
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
    $(isAliveComponent).on("change", function() {
        var show = $(isAliveComponent).prop("checked");
        $(yearOfDeathComponent)
            .val("")
            .prop("disabled", show);
    });
};

var toggleCardColorBySex = function(cardComponent, sexRadioComponent) {
    $("input[type=radio][name=" + sexRadioComponent + "]").on("change", function() {
        var isMale = $(this).val() === "M";
        $(cardComponent).toggleClass("border-secondary", isMale);
        $(cardComponent).toggleClass("border-danger", !isMale);
        $(cardComponent + " div.card-header").toggleClass("text-bg-secondary", isMale);
        $(cardComponent + " div.card-header").toggleClass("text-bg-danger", !isMale);
    });
};

var toggleContainers = function(displayBtnComponent, container1Component, container2Component) {
    $(displayBtnComponent).on("change", function() {
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

                data.people.forEach((person, index) => {
                    var personComponent = getPersonComponent(person, index);
                    resultComponent.append(personComponent);
                });

                if (data.people.length == 0) {
                    if (!data.potentialResults) {
                        resultComponent.html("<p>No se encontraron resultados. \u2639</p>");
                    } else {
                        resultComponent
                            .html("<p>La b\u00FAsqueda es ambigua.</p>")
                            .append("<p>Refin\u00E1 la b\u00FAsqueda agregando fechas o completando nombres de padres y parejas.</p>")
                            .append("<p><b>Potenciales resultados:</b> " + data.potentialResults + "</p>");
                    }
                }
            },
            error: function(xhr) {
                console.log(xhr);
                resultComponent.html("Error!");

                // Get error details
                try {
                    var errorMsg;
                    if (xhr.status >= 500 && xhr.status < 600) {
                        errorMsg = "<b>Error:</b> El servidor se est\u00E1 reiniciando, intent\u00E1 de nuevo.";
                    } else if (xhr.status == 0) {
                        errorMsg = "<b>Error:</b> El servidor est\u00E1 ca\u00EDdo, intent\u00E1 de nuevo.";
                    } else {
                        errorMsg = "<b>Error:</b> " + xhr.responseJSON.message;
                    }

                    resultComponent.html(
                        $("<div>")
                            .html(errorMsg));

                } catch (ex) {
                    console.log(ex);
                }
            },
            complete: function() {
                $("#searchBtn").prop("disabled", false);
                $("html, body").animate({
                    scrollTop: $("#searchResultCard").offset().top
                }, 600);
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

var getPersonComponent = function(person, index) {
    var card = $("<div>").addClass("card");

    if (index > 0) {
        card.addClass("mt-2");
    }

    var cardBody = $("<div>").addClass("card-body small");

    if (person.sex == "M") {
        card.addClass("border-secondary");
        cardBody.addClass("text-bg-secondary");
    } else if (person.sex == "F") {
        card.addClass("border-danger");
        cardBody.addClass("text-bg-danger");
    } else {
        card.addClass("border-light");
        cardBody.addClass("text-bg-light");
    }

    cardBody.append(
        $("<div>")
            .addClass(person.aka == null ? "h6" : "h6 mb-0")
            .text(displayNameInSpanish(person.name)));

    if (person.aka != null) {
        cardBody.append(
            $("<div>")
                .addClass("small mb-2")
                .text(displayNameInSpanish(person.aka)));
    }

    var birthDeath = $("<div>")
        .addClass("mt-1");

    if (person.dateOfBirth != null) {
        birthDeath.text("n. " + displayDateInSpanish(person.dateOfBirth));
    } else if (person.dateOfDeath != null) {
        birthDeath.text("?");
    }

    if (person.dateOfDeath != null) {
        birthDeath.append(" - f. " + displayDateInSpanish(person.dateOfDeath));
    } else {
        if (person.dateOfBirth != null) {
            birthDeath.append(" - ");
        }
        if (person.isAlive) {
            birthDeath.append("Vive");
        } else {
            birthDeath.append(person.sex == "F" ? "Fallecida" : "Fallecido");
        }
    }

    cardBody.append(birthDeath);

    if (person.placeOfBirth != null) {
        cardBody.append(
            $("<div>")
                .addClass("mt-1")
                .html("Pa&iacute;s de nacimiento: " + person.placeOfBirth));
    }

    if (person.ancestryCountries.length > 0) {
        var countries = $("<ul>")
            .addClass("mb-0");

        person.ancestryCountries.forEach((countryName, index) => {
            countries.append(
                $("<li>")
                    .html(countryName));
        });

        cardBody.append(
            $("<div>")
                .addClass("mt-1")
                .html("Pa&iacute;ses en su ascendencia: ")
                .append(countries));
    }

    if (person.parents.length > 0) {
        var parents = $("<ul>")
            .addClass("mb-0");

        person.parents.forEach((parentName, index) => {
            parents.append(
                $("<li>")
                    .html(
                        $("<b>")
                            .text(displayNameInSpanish(parentName))));
        });

        cardBody.append(
            $("<div>")
                .addClass("mt-1")
                .html("Padres: ")
                .append(parents));
    }

    if (person.spouses.length > 0) {
        var spouses = $("<ul>")
            .addClass("mb-0");

        person.spouses.forEach((spouseWithChildren, index) => {
            spouses.append(
                $("<li>")
                    .html(
                        $("<b>")
                            .text(displayNameInSpanish(spouseWithChildren.name))));

            if (spouseWithChildren.children.length > 0) {
                var children = $("<ul>")
                    .addClass("mb-0");

                spouseWithChildren.children.forEach((childName, index) => {
                    children.append(
                        $("<li>")
                            .text(displayNameInSpanish(childName)));
                });

                spouses.append(children);
            }
        });

        cardBody.append(
            $("<div>")
                .addClass("mt-1")
                .html("Parejas: ")
                .append(spouses));
    }

    return card.html(cardBody);
};

var displayNameInSpanish = function(name) {
    // Only the name is private, surname is not obfuscated
    name = name.replace("<private>", "<nombre privado>");
    name = name.replace("<no name>", "<nombre desconocido>");
    return name.replace("<no spouse>", "<sin pareja>");
};

var displayDateInSpanish = function(date) {
    // Only the date of birth is private, date of death is not obfuscated
    if (date == "<private>") {
        return "<fecha de nac. privada>";
    }

    date = date.replaceAll("BET", "entre");
    date = date.replaceAll("AND", "y");
    date = date.replaceAll("ABT", "aprox.");
    date = date.replaceAll("EST", "se estima");
    date = date.replaceAll("BEF", "antes de");
    date = date.replaceAll("AFT", "despu\u00E9s de");

    date = date.replace(/(\d+) JAN/g, "$1 de ene de");
    date = date.replace(/(\d+) FEB/g, "$1 de feb de");
    date = date.replace(/(\d+) MAR/g, "$1 de mar de");
    date = date.replace(/(\d+) APR/g, "$1 de abr de");
    date = date.replace(/(\d+) MAY/g, "$1 de may de");
    date = date.replace(/(\d+) JUN/g, "$1 de jun de");
    date = date.replace(/(\d+) JUL/g, "$1 de jul de");
    date = date.replace(/(\d+) AUG/g, "$1 de ago de");
    date = date.replace(/(\d+) SEP/g, "$1 de sep de");
    date = date.replace(/(\d+) OCT/g, "$1 de oct de");
    date = date.replace(/(\d+) NOV/g, "$1 de nov de");
    date = date.replace(/(\d+) DEC/g, "$1 de dic de");

    date = date.replaceAll("JAN", "ene de");
    date = date.replaceAll("FEB", "feb de");
    date = date.replaceAll("MAR", "mar de");
    date = date.replaceAll("APR", "abr de");
    date = date.replaceAll("MAY", "may de");
    date = date.replaceAll("JUN", "jun de");
    date = date.replaceAll("JUL", "jul de");
    date = date.replaceAll("AUG", "ago de");
    date = date.replaceAll("SEP", "sep de");
    date = date.replaceAll("OCT", "oct de");
    date = date.replaceAll("NOV", "nov de");
    date = date.replaceAll("DEC", "dic de");

    return date;
};
