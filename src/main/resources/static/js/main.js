
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

var toggleCardColorBySex = function(cardComponent, sexRadioComponent, relatedCardComponent, relatedSexRadioComponent) {
    $("input[type=radio][name=" + sexRadioComponent + "]").on("change", function() {
        var isMale = $(this).val() === "M";
        $(cardComponent).toggleClass("border-secondary", isMale);
        $(cardComponent).toggleClass("border-danger", !isMale);
        $(cardComponent + " div.card-header").toggleClass("text-bg-secondary", isMale);
        $(cardComponent + " div.card-header").toggleClass("text-bg-danger", !isMale);

        if (relatedCardComponent && relatedSexRadioComponent) {
            var $genderRadio = $("input[type=radio][name=" + relatedSexRadioComponent + "]");
            $genderRadio.filter("[value=M]").prop("checked", !isMale);
            $genderRadio.filter("[value=F]").prop("checked", isMale);

            $(relatedCardComponent).toggleClass("border-secondary", !isMale);
            $(relatedCardComponent).toggleClass("border-danger", isMale);
            $(relatedCardComponent + " div.card-header").toggleClass("text-bg-secondary", !isMale);
            $(relatedCardComponent + " div.card-header").toggleClass("text-bg-danger", isMale);
        }
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
    toggleCardColorBySex("#individualCard", "individualSex", "#spouseCard", "spouseSex");
    toggleCardColorBySex("#spouseCard", "spouseSex");
    toggleContainers("#grandparentsContainerShowBtn", "#grandparentsContainerShowBtnContainer", "#grandparentsContainer");
    toggleContainers("#spouseContainerShowBtn", "#spouseContainerShowBtnContainer", "#spouseContainer");

    $("input[type=number]").on("input", function() {
        maxLengthCheck(this);
    });
});

$(document).ready(function() {
    $.ajax({
        type: "GET",
        url: "/api/gedcom-analyzer/metadata",
        contentType: "application/json",
        success: function(data) {
            $("#persons-count-container")
                .html(data.personsCount);
        },
        error: function(xhr) {
            console.log(xhr);
        }
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
                "yearOfDeath": toNumber($("#individualYearOfDeath:enabled").val()),
                "placeOfBirth": trimToNull($("#individualPlaceOfBirth").val())
            },
            "spouse": {
                "givenName": trimToNull($("#spouseGivenName").val()),
                "surname": trimToNull($("#spouseSurname").val()),
                "sex": trimToNull($("input[type=radio][name=spouseSex]:checked").val()),
                "isAlive": $("#spouseIsAlive").prop("checked"),
                "yearOfBirth": toNumber($("#spouseYearOfBirth").val()),
                "yearOfDeath": toNumber($("#spouseYearOfDeath:enabled").val()),
                "placeOfBirth": trimToNull($("#spousePlaceOfBirth").val())
            },
            "father": {
                "givenName": trimToNull($("#fatherGivenName").val()),
                "surname": trimToNull($("#fatherSurname").val()),
                "sex": "M",
                "isAlive": $("#fatherIsAlive").prop("checked"),
                "yearOfBirth": toNumber($("#fatherYearOfBirth").val()),
                "yearOfDeath": toNumber($("#fatherYearOfDeath:enabled").val()),
                "placeOfBirth": trimToNull($("#fatherPlaceOfBirth").val())
            },
            "mother": {
                "givenName": trimToNull($("#motherGivenName").val()),
                "surname": trimToNull($("#motherSurname").val()),
                "sex": "F",
                "isAlive": $("#motherIsAlive").prop("checked"),
                "yearOfBirth": toNumber($("#motherYearOfBirth").val()),
                "yearOfDeath": toNumber($("#motherYearOfDeath:enabled").val()),
                "placeOfBirth": trimToNull($("#motherPlaceOfBirth").val())
            },
            "paternalGrandfather": {
                "givenName": trimToNull($("#paternalGrandfatherGivenName").val()),
                "surname": trimToNull($("#paternalGrandfatherSurname").val()),
                "sex": "M",
                "isAlive": $("#paternalGrandfatherIsAlive").prop("checked"),
                "yearOfBirth": toNumber($("#paternalGrandfatherYearOfBirth").val()),
                "yearOfDeath": toNumber($("#paternalGrandfatherYearOfDeath:enabled").val()),
                "placeOfBirth": trimToNull($("#paternalGrandfatherPlaceOfBirth").val())
            },
            "paternalGrandmother": {
                "givenName": trimToNull($("#paternalGrandmotherGivenName").val()),
                "surname": trimToNull($("#paternalGrandmotherSurname").val()),
                "sex": "F",
                "isAlive": $("#paternalGrandmotherIsAlive").prop("checked"),
                "yearOfBirth": toNumber($("#paternalGrandmotherYearOfBirth").val()),
                "yearOfDeath": toNumber($("#paternalGrandmotherYearOfDeath:enabled").val()),
                "placeOfBirth": trimToNull($("#paternalGrandmotherPlaceOfBirth").val())
            },
            "maternalGrandfather": {
                "givenName": trimToNull($("#maternalGrandfatherGivenName").val()),
                "surname": trimToNull($("#maternalGrandfatherSurname").val()),
                "sex": "M",
                "isAlive": $("#maternalGrandfatherIsAlive").prop("checked"),
                "yearOfBirth": toNumber($("#maternalGrandfatherYearOfBirth").val()),
                "yearOfDeath": toNumber($("#maternalGrandfatherYearOfDeath:enabled").val()),
                "placeOfBirth": trimToNull($("#maternalGrandfatherPlaceOfBirth").val())
            },
            "maternalGrandmother": {
                "givenName": trimToNull($("#maternalGrandmotherGivenName").val()),
                "surname": trimToNull($("#maternalGrandmotherSurname").val()),
                "sex": "F",
                "isAlive": $("#maternalGrandmotherIsAlive").prop("checked"),
                "yearOfBirth": toNumber($("#maternalGrandmotherYearOfBirth").val()),
                "yearOfDeath": toNumber($("#maternalGrandmotherYearOfDeath:enabled").val()),
                "placeOfBirth": trimToNull($("#maternalGrandmotherPlaceOfBirth").val())
            },
            "contact": trimToNull($("#individualContact").val())
        };

        postProcessRequest(searchFamilyRequest);

        if (isRequestEmpty(searchFamilyRequest)) {
            var errorMsg = "<p><b>Error:</b> Llen&aacute; por lo menos un dato.</p>";
            resultComponent.html(
                $("<div>")
                    .html(errorMsg));
            finalizeSearch();
            return;
        }

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
                    if (data.errors.length > 0) {
                        resultComponent.html("<p>Se produjo un error en la b&uacute;squeda. \u2639</p>");
                        data.errors.forEach((errorCode, index) => {
                            resultComponent.append(displayErrorCodeInSpanish(errorCode));
                        });
                    } else if (!data.potentialResults) {
                        if (isMissingSurname(searchFamilyRequest)) {
                            resultComponent.html("<p>No se encontraron resultados. Por favor ingres&aacute; un apellido.</p>");
                        } else {
                            resultComponent
                                .html("<p>No se encontraron resultados. \u2639</p>")
                                .append("<p>Refin&aacute; la b&uacute;squeda agregando fechas o completando nombres de padres y parejas.</p>");
                        }
                    } else {
                        resultComponent
                            .html("<p>La b&uacute;squeda es ambigua.</p>")
                            .append("<p>Refin&aacute; la b&uacute;squeda agregando fechas o completando nombres de padres y parejas.</p>")
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
                        errorMsg = "<p><b>Error:</b> El servidor se est&aacute; reiniciando, intent&aacute; de nuevo.</p>";
                    } else if (xhr.status == 0) {
                        errorMsg = "<p><b>Error:</b> El servidor est&aacute; ca&iacute;do, intent&aacute; de nuevo.</p>";
                    } else {
                        errorMsg = "<p><b>Error:</b> " + xhr.responseJSON.message + "</p>";
                    }

                    resultComponent.html(
                        $("<div>")
                            .html(errorMsg));

                } catch (ex) {
                    console.log(ex);
                }
            },
            complete: function() {
                finalizeSearch();
            }
        });
    });
});

var finalizeSearch = function() {
    $("#searchBtn").prop("disabled", false);
    $("html, body").animate({
        scrollTop: $("#searchResultCard").offset().top
    }, 600);
};

var personsInRq = [
    "individual",
    "spouse",
    "father",
    "mother",
    "paternalGrandfather",
    "paternalGrandmother",
    "maternalGrandfather",
    "maternalGrandmother"
];

var personProps = [
    "givenName",
    "surname",
    "yearOfBirth",
    "yearOfDeath"
];

var postProcessRequest = function(searchFamilyRequest) {
    for (var person of personsInRq) {
        if (isPersonEmpty(searchFamilyRequest[person])) {
            delete searchFamilyRequest[person];
        }
    }
};

var isPersonEmpty = function(person) {
    for (var prop of personProps) {
        if (person[prop] != null) {
            return false;
        }
    }
    return true;
};

var isRequestEmpty = function(searchFamilyRequest) {
    for (var person of personsInRq) {
        if (!!searchFamilyRequest[person]) {
            return false;
        }
    }
    return true;
};

var isMissingSurname = function(searchFamilyRequest) {
    for (var person of personsInRq) {
        if (!!searchFamilyRequest[person] && !!searchFamilyRequest[person].surname) {
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
            .html(displayNameInSpanish(person.name)));

    if (person.aka != null) {
        cardBody.append(
            $("<div>")
                .addClass("small mb-2")
                .html(displayNameInSpanish(person.aka)));
    }

    var birthDeath = $("<div>")
        .addClass("mt-1");

    if (person.dateOfBirth != null) {
        birthDeath.html("n. " + displayDateInSpanish(person.dateOfBirth));
    } else if (person.dateOfDeath != null) {
        birthDeath.html("?");
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

    if (person.parents.length > 0) {
        var parents = $("<ul>")
            .addClass("mb-0");

        person.parents.forEach((parentName, index) => {
            parents.append(
                $("<li>")
                    .html(
                        $("<b>")
                            .html(displayNameInSpanish(parentName))));
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
                            .html(displayNameInSpanish(spouseWithChildren.name))));

            if (spouseWithChildren.children.length > 0) {
                var children = $("<ul>")
                    .addClass("mb-0");

                spouseWithChildren.children.forEach((childName, index) => {
                    children.append(
                        $("<li>")
                            .html(displayNameInSpanish(childName)));
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

    var hasAncestryGenerations = person.ancestryGenerations != null
            && (person.ancestryGenerations.ascending > 0 || person.ancestryGenerations.descending > 0);
    var hasNumberOfPeopleInTree = person.numberOfPeopleInTree != null;
    var hasMaxDistantRelationship = person.maxDistantRelationship != null;

    if (hasAncestryGenerations || hasNumberOfPeopleInTree || hasMaxDistantRelationship) {
        var treeInfo = $("<ul>")
            .addClass("mb-0");

        if (hasAncestryGenerations) {
            treeInfo.append(
                $("<li>")
                    .html("Ascendencia: " + getCardinal(person.ancestryGenerations.ascending, "generaci&oacute;n", "generaciones")));

            treeInfo.append(
                $("<li>")
                    .html("Descendencia: " + getCardinal(person.ancestryGenerations.descending, "generaci&oacute;n", "generaciones")));
        }

        if (hasNumberOfPeopleInTree) {
            treeInfo.append(
                $("<li>")
                    .html("Cantidad de familiares: <b>" + person.numberOfPeopleInTree + "</b>"));
        }

        if (hasMaxDistantRelationship) {
            treeInfo
                .append(
                    $("<li>")
                        .html("Relaci&oacute;n m&aacute;s distante:"))
                .append(
                    $("<ul>")
                        .addClass("mb-0")
                        .append(
                            $("<li>")
                                .html(displayRelationshipInSpanish(person.maxDistantRelationship)))
                        .append(
                            $("<li>")
                                .html(displayNameInSpanish(person.maxDistantRelationship.personName))));
        }

        cardBody.append(
            $("<div>")
                .addClass("mt-1")
                .html("Informaci&oacute;n en el &aacute;rbol: ")
                .append(treeInfo));
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

    return card.html(cardBody);
};

var getCardinal = function(num, singular, plural) {
    return "<b>" + num + "</b> " + (num == 1 ? singular : plural);
}

var displayNameInSpanish = function(name) {
    // Only the name is private, surname is not obfuscated
    name = name.replace("<private>", "&lt;nombre privado&gt;");
    name = name.replace("<no name>", "&lt;nombre desconocido&gt;");
    return name.replace("<no spouse>", "&lt;sin pareja&gt;");
};

var displayDateInSpanish = function(date) {
    // Only the date of birth is private, date of death is not obfuscated
    if (date == "<private>") {
        return "&lt;fecha de nac. privada&gt;";
    }

    date = date.replaceAll("BET", "entre");
    date = date.replaceAll("AND", "y");
    date = date.replaceAll("ABT", "aprox.");
    date = date.replaceAll("EST", "se estima");
    date = date.replaceAll("BEF", "antes de");
    date = date.replaceAll("AFT", "despu&eacute;s de");

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

// Spouses are not considered
var displayRelationshipInSpanish = function(relationship) {
    if (relationship.referenceType == "SELF") {
        return "<b>esta persona</b>";
    }

    if (relationship.referenceType == "PARENT") {
        if (relationship.generation == 1) {
            return relationship.personSex == "M" ? "padre" : "madre";
        }

        var sexSuffix = (relationship.personSex == "M" ? "o" : "a");
        var gradeSuffix = getGradeSuffixInSpanish(relationship.generation - 4, sexSuffix);

        var relationshipName;
        if (relationship.generation == 2) {
            relationshipName = "abuel" + sexSuffix;
        } else if (relationship.generation == 3) {
            relationshipName = "bisabuel" + sexSuffix;
        } else if (relationship.generation == 4) {
            relationshipName = "tatarabuel" + sexSuffix;
        } else {
            relationshipName = "trastatarabuel" + sexSuffix;
        }

        var or = "";
        if (relationship.generation >= 6) {
            or = "<br>&nbsp; (ancestro directo de " + relationship.generation + " gener.)";
        }

        return "<b>" + relationshipName + gradeSuffix + "</b>" + or;
    }

    if (relationship.referenceType == "CHILD") {
        var sexSuffix = (relationship.personSex == "M" ? "o" : "a");
        var gradeSuffix = getGradeSuffixInSpanish(relationship.generation - 4, sexSuffix);

        var relationshipName;
        if (relationship.generation == 1) {
            relationshipName = "hij" + sexSuffix;
        } else if (relationship.generation == 2) {
            relationshipName = "niet" + sexSuffix;
        } else if (relationship.generation == 3) {
            relationshipName = "bisniet" + sexSuffix;
        } else if (relationship.generation == 4) {
            relationshipName = "tataraniet" + sexSuffix;
        } else {
            relationshipName = "trastataraniet" + sexSuffix;
        }

        var or = "";
        if (relationship.generation >= 6) {
            or = "<br>&nbsp; (descendiente direct" + sexSuffix + " de " + relationship.generation + " gener.)";
        }

        return "<b>" + relationshipName + gradeSuffix + "</b>" + or;
    }

    if (relationship.referenceType == "SIBLING") {
        var halfPrefix = relationship.isHalf ? "medio-" : "";
        var sexSuffix = (relationship.personSex == "M" ? "o" : "a");

        var relationshipName = "prim" + sexSuffix;
        return "<b>" + halfPrefix + relationshipName + "</b>";
    }

    if (relationship.referenceType == "COUSIN") {
        var halfPrefix = relationship.isHalf ? "medio-" : "";
        var sexSuffix = (relationship.personSex == "M" ? "o" : "a");
        var gradeSuffix = getGradeSuffixInSpanish(relationship.grade, sexSuffix);

        var relationshipName = "prim" + sexSuffix;
        return "<b>" + halfPrefix + relationshipName + gradeSuffix + "</b>";
    }

    if (relationship.referenceType == "PIBLING") {
        var halfPrefix = relationship.isHalf ? "medio-" : "";
        var sexSuffix = (relationship.personSex == "M" ? "o" : "a");
        var gradeSuffix = getGradeSuffixInSpanish(relationship.grade, sexSuffix);

        var relationshipName1 = "t&iacute;" + sexSuffix + (relationship.generation > 1 ? "-" : "");
        var relationshipName2;

        if (relationship.generation == 1) {
            relationshipName2 = "";
        } else if (relationship.generation == 2) {
            relationshipName2 = "abuel" + sexSuffix;
        } else if (relationship.generation == 3) {
            relationshipName2 = "bisabuel" + sexSuffix;
        } else if (relationship.generation == 4) {
            relationshipName2 = "tatarabuel" + sexSuffix;
        } else {
            relationshipName2 = "trastatarabuel" + sexSuffix;
        }

        var or = "";
        if (relationship.generation == 1 && relationship.grade >= 2 || relationship.generation >= 2) {
            var relationshipNameOr1 = (relationshipName2 == "") ? "padre/madre" : relationshipName2.substring(0, relationshipName2.length - 1) + "o/a";
            var relationshipNameOr2;
            if (relationship.grade == 1) {
                relationshipNameOr2 = "herman" + sexSuffix;
            } else {
                relationshipNameOr2 = "prim" + sexSuffix;
            }
            var gradeSuffixOr = getGradeSuffixInSpanish(relationship.grade - 1, sexSuffix);
            or = "<br>&nbsp; (" + halfPrefix + relationshipNameOr2 + gradeSuffixOr + " de " + relationshipNameOr1 + ")";
        }

        return "<b>" + halfPrefix + relationshipName1 + relationshipName2 + gradeSuffix + "</b>" + or;
    }

    if (relationship.referenceType == "NIBLING") {
        var halfPrefix = relationship.isHalf ? "medio-" : "";
        var sexSuffix = (relationship.personSex == "M" ? "o" : "a");
        var gradeSuffix = getGradeSuffixInSpanish(relationship.grade, sexSuffix);

        var relationshipName1 = "sobrin" + sexSuffix + (relationship.generation > 1 ? "-" : "");
        var relationshipName2;

        if (relationship.generation == 1) {
            relationshipName2 = "";
        } else if (relationship.generation == 2) {
            relationshipName2 = "niet" + sexSuffix;
        } else if (relationship.generation == 3) {
            relationshipName2 = "bisniet" + sexSuffix;
        } else if (relationship.generation == 4) {
            relationshipName2 = "tataraniet" + sexSuffix;
        } else {
            relationshipName2 = "trastataraniet" + sexSuffix;
        }

        var or = "";
        if (relationship.generation == 1 && relationship.grade >= 2 || relationship.generation >= 2) {
            var relationshipNameOr1 = (relationshipName2 == "") ? "hij" + sexSuffix : relationshipName2;
            var relationshipNameOr2;
            if (relationship.grade == 1) {
                relationshipNameOr2 = "hermano/a";
            } else {
                relationshipNameOr2 = "primo/a";
            }
            var gradeSuffixOr = getGradeSuffixInSpanish(relationship.grade - 1, "o/a");
            or = "<br>&nbsp; (" + relationshipNameOr1 + " de " + halfPrefix + relationshipNameOr2 + gradeSuffixOr + ")";
        }

        return "<b>" + halfPrefix + relationshipName1 + relationshipName2 + gradeSuffix + "</b>" + or;
    }

    return "<b>familiar</b>";
}

var getGradeSuffixInSpanish = function(grade, sexSuffix) {
    if (grade <= 1) {
        return ""
    }
    if (grade == 2) {
        return " segund" + sexSuffix;
    }
    if (grade == 3) {
        return " tercer" + sexSuffix;
    }
    if (grade == 4) {
        return " cuart" + sexSuffix;
    }
    if (grade == 5) {
        return " quint" + sexSuffix;
    }
    if (grade == 6) {
        return " sext" + sexSuffix;
    }
    if (grade == 7) {
        return " s&eacute;ptim" + sexSuffix;
    }
    if (grade == 8) {
        return " octav" + sexSuffix;
    }
    if (grade == 9) {
        return " noven" + sexSuffix;
    }
    return " de " + grade + "&deg; grado";
};

var displayErrorCodeInSpanish = function(errorCode) {
    if (errorCode == "TOO-MANY-REQUESTS") {
        return "<p>Realizaste demasiadas consultas en la &uacute;ltima hora, por favor esper&aacute; unos minutos o contactanos en redes sociales: <b>@genea.azul</b></p>";
    }
    return errorCode;
};
