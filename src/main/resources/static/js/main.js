
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

const FAMILY_TREE_PROCESS_PERSONS_BY_SEC = 250;
const FAMILY_TREE_PROCESS_FIXED_DELAY_MILLIS = 3250;
const MIN_MILLIS_TO_DISPLAY_WAIT_COUNT_DOWN = 7500;

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
        $(cardComponent).toggleClass("border-dark", false);
        $(cardComponent + " div.card-header").toggleClass("text-bg-secondary", isMale);
        $(cardComponent + " div.card-header").toggleClass("text-bg-danger", !isMale);
        $(cardComponent + " div.card-header").toggleClass("text-bg-dark", false);
        $(cardComponent + " i.card-header-icon").toggleClass("bi-gender-male", isMale);
        $(cardComponent + " i.card-header-icon").toggleClass("bi-gender-female", !isMale);

        if (relatedCardComponent && relatedSexRadioComponent) {
            var $genderRadio = $("input[type=radio][name=" + relatedSexRadioComponent + "]");
            $genderRadio.filter("[value=M]").prop("checked", !isMale);
            $genderRadio.filter("[value=F]").prop("checked", isMale);

            $(relatedCardComponent).toggleClass("border-secondary", !isMale);
            $(relatedCardComponent).toggleClass("border-danger", isMale);
            $(relatedCardComponent).toggleClass("border-dark", false);
            $(relatedCardComponent + " div.card-header").toggleClass("text-bg-secondary", !isMale);
            $(relatedCardComponent + " div.card-header").toggleClass("text-bg-danger", isMale);
            $(relatedCardComponent + " div.card-header").toggleClass("text-bg-dark", false);
            $(relatedCardComponent + " i.card-header-icon").toggleClass("bi-gender-male", !isMale);
            $(relatedCardComponent + " i.card-header-icon").toggleClass("bi-gender-female", isMale);
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
    toggleYearOfDeath("#spouseIsAlive", "#spouseYearOfDeath");
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
                .html(data.personsCount.toLocaleString("es-AR"));
        },
        error: function(xhr) {
            console.log(xhr);
        }
    });
});

$(document).ready(function() {
    $("#searchBtn").on("click", function(event) {
        event.preventDefault();

        $("#searchBtn").prop("disabled", true);

        var resultComponentLocator = "#searchResultCard div.card-body";
        var $resultComponent = $(resultComponentLocator)
            .html("<p>Buscando...</p>");

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
            $resultComponent.html(
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
                $resultComponent.empty();

                var timeoutMs = 0;
                data.people.forEach((person, index) => {
                    var $personComponent = getPersonComponent(person, index);
                    $resultComponent.append($personComponent);

                    timeoutMs += enableFamilyTreeButtons(person.uuid, person.personsCountInTree, timeoutMs);
                });

                if (data.people.length == 0) {
                    if (data.errors.length > 0) {
                        $resultComponent.html("<p>⚠️ Se produjo un error en la b&uacute;squeda. ⚠️</p>");
                        data.errors.forEach((errorCode, index) => {
                            $resultComponent.append(displayErrorCodeInSpanish(errorCode));
                        });
                    } else if (!data.potentialResults) {
                        if (getSurnamesInRequest(searchFamilyRequest).length == 0) {
                            $resultComponent.html("<p>⚠️ No se encontraron resultados. Por favor ingres&aacute; un apellido. ⚠️</p>");
                        } else {
                            $resultComponent
                                .html("<p>🔎 No se encontraron resultados. 🔍</p>")
                                .append("<p>Edit&aacute; la b&uacute;squeda agregando fechas o completando nombres de padres y parejas.</p>")
                                .append($("<p>")
                                        .addClass("text-center")
                                        .html("&iexcl;Solicitá acceso al &aacute;rbol y carg&aacute; info!")
                                        .append($("<a>")
                                                .addClass("link-secondary text-decoration-none ms-2")
                                                .attr("href", "https://instagram.com/_u/genea.azul")
                                                .attr("target", "_blank")
                                                .attr("title", "@genea.azul")
                                                .append($("<i>")
                                                        .addClass("bi bi-instagram"))));
                            if (!!searchFamilyRequest["individual"]["sex"]) {
                                $resultComponent
                                    .append("<p>Verific&aacute; que el <span class=\"text-danger fw-bold\">sexo</span> de la persona est&eacute; bien seleccionado.</p>");
                            }
                        }
                    } else {
                        $resultComponent
                            .html("<p>⚠️ La b&uacute;squeda es ambigua. ⚠️</p>")
                            .append("<p>Refin&aacute; la b&uacute;squeda agregando fechas o completando nombres de padres y parejas.</p>")
                            .append("<p><b>Potenciales resultados:</b> " + data.potentialResults + "</p>");
                    }
                }

                var surnamesInRequest = getSurnamesInRequest(searchFamilyRequest);

                // append surnames info
                if (surnamesInRequest.length > 0) {
                    $resultComponent.append(
                        $("<div>")
                            .addClass("card border-dark mt-4 ms-1 me-1 mb-1")
                            .attr("id", "searchSurnamesResultCard")
                            .append(
                                $("<div>")
                                    .addClass("card-header text-bg-dark")
                                    .html("Informaci&oacute;n de apellidos"))
                            .append(
                                $("<div>")
                                    .addClass("card-body overflow-auto")
                                    .html(
                                        $("<span>")
                                            .addClass("spinner-border spinner-border-sm")
                                            .attr("role", "status"))));

                    finalizeSearch(function() {
                        setTimeout(function() {
                            searchSurnames(surnamesInRequest);
                        }, 1500);
                    });
                } else {
                    finalizeSearch();
                }
            },
            error: function(xhr) {
                handleError(xhr, resultComponentLocator);
                finalizeSearch();
            }
        });
    });
});

var finalizeSearch = function(callback = function() {}) {
    $("html, body")
        .animate({ scrollTop: $("#searchResultCard").offset().top }, 600)
        .promise()
        .done(function () {
            callback();
            $("#searchBtn").prop("disabled", false);
        });
};

var enableFamilyTreeButtons = function(personUuid, personsCountInTree, previousTimeoutMs) {

    var currentTimeoutMs = (personsCountInTree || 0) * 1000 / FAMILY_TREE_PROCESS_PERSONS_BY_SEC + FAMILY_TREE_PROCESS_FIXED_DELAY_MILLIS;
    var timeoutMs = previousTimeoutMs + currentTimeoutMs;

    if (timeoutMs >= MIN_MILLIS_TO_DISPLAY_WAIT_COUNT_DOWN) {
        var $countDownComponent = $("<span>")
                .addClass("ms-1")
                .attr("id", "#search-family-tree-wait-countdown-" + personUuid);

        $("#search-family-tree-wait-sign-" + personUuid)
            .append($countDownComponent);

        var timeoutSec = Math.floor(timeoutMs / 1000);
        var interval = setInterval(function() {
          $countDownComponent.html(timeoutSec);
          if (--timeoutSec < 0) {
            clearInterval(interval);
          }
        }, 1000);
    }

    setTimeout(function() {
        $("#search-family-tree-wait-sign-" + personUuid)
            .remove();

        $("#search-family-tree-btn-" + personUuid)
            .removeClass("btn-dark")
            .addClass("btn-outline-light")
            .removeClass("disabled");

        $("#view-family-tree-btn-" + personUuid)
            .removeClass("btn-dark")
            .addClass("btn-outline-light")
            .removeClass("disabled");
    }, timeoutMs);

    return currentTimeoutMs;
};

var searchSurnames = function(surnames) {
    var surnamesComponentLocator = "#searchSurnamesResultCard div.card-body";
    var $surnamesComponent = $(surnamesComponentLocator);

    var searchSurnamesRequest = {
        "surnames": surnames
    };

    $.ajax({
        type: "POST",
        url: "/api/search/surnames",
        dataType: "json",
        contentType: "application/json",
        data: JSON.stringify(searchSurnamesRequest),
        success: function(data) {
            // remove the spinner
            $surnamesComponent.empty();

            data.surnames.forEach((searchSurnameResult, index) => {
                var $surnameComponent = getSurnameComponent(searchSurnameResult, index);
                $surnamesComponent.append($surnameComponent);
            });
        },
        error: function(xhr) {
            handleError(xhr, surnamesComponentLocator);
        }
    });
};

var searchFamilyTree = function(event) {
    event.preventDefault();

    $(event.data.errorLocator).addClass("d-none");

    var searchParams = new URLSearchParams(window.location.search);
    var filter = searchParams.get("f");
    var obfuscate = (filter !== "0" ? "" : "?obfuscateLiving=false");

    var domain = window.location.protocol + '//' + window.location.host;
    var win = window.open(domain + "/api/search/family-tree/" + event.data.personUuid + "/plainPdf" + obfuscate, "_blank");
    if (!win) {
        handleError("El navegador bloque&oacute; la descarga, por favor intent&aacute; desde otro como Chrome.", event.data.errorLocator);
    }
}

var handleError = function(xhr, componentLocator) {
    var $component = $(componentLocator)
        .removeClass("d-none")
        .html("Error!");

    // Get error details
    try {
        var errorMsg = null;
        if (xhr.status !== undefined) {
            if (xhr.status >= 500 && xhr.status < 600) {
                errorMsg = "El servidor se est&aacute; reiniciando, intent&aacute; de nuevo.";
            } else if (xhr.status == 0) {
                errorMsg = "El servidor est&aacute; ca&iacute;do, intent&aacute; de nuevo.";
            } else if (xhr.responseJSON !== undefined) {
                if (xhr.responseJSON.errors !== undefined && xhr.responseJSON.errors.length > 0) {
                    errorMsg = xhr.responseJSON.errors[0].defaultMessage;
                } else if (xhr.responseJSON.message !== undefined) {
                    errorMsg = xhr.responseJSON.message;
                } else if (xhr.responseJSON.error !== undefined) {
                    if (xhr.responseJSON.error === "Bad Request") {
                        errorMsg = "Alguno de los valores ingresados es incorrecto";
                    } else {
                        errorMsg = xhr.responseJSON.error;
                    }
                }
            } else if (xhr.message !== undefined) {
                errorMsg = xhr.message;
            } else if (xhr.error !== undefined) {
                if (xhr.error === "Bad Request") {
                    errorMsg = "Alguno de los valores ingresados es incorrecto";
                } else {
                    errorMsg = xhr.error;
                }
            }
        } else if (xhr.message !== undefined) {
            errorMsg = xhr.message;
        } else {
            errorMsg = JSON.stringify(xhr);
        }

        if (errorMsg !== null) {
            $component.html(
                $("<div>")
                    .html("<p><b>Error:</b> " + errorMsg + "</p>"));
        }

    } catch (ex) {
        console.log(ex);
        $component.html(
            $("<div>")
                .html("<p><b>Error:</b> " + JSON.stringify(ex) + "</p>"));
    }
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

    var searchParams = new URLSearchParams(window.location.search);
    if (searchParams.get("f") === "0") {
        searchFamilyRequest["obfuscateLiving"] = false;
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

var getSurnamesInRequest = function(searchFamilyRequest) {
    var surnames = [];
    for (var person of personsInRq) {
        if (!!searchFamilyRequest[person] && !!searchFamilyRequest[person].surname) {
            if ($.inArray(searchFamilyRequest[person].surname, surnames) == -1) {
                surnames.push(searchFamilyRequest[person].surname);
            }
        }
    }
    return surnames;
};

var getPersonComponent = function(person, index) {
    var $card = $("<div>")
            .addClass("card")
            .attr("id", "person-card-" + person.uuid);

    if (index > 0) {
        $card.addClass("mt-2");
    }

    if (person.sex == "M") {
        $card.addClass("border-secondary text-bg-secondary");
    } else if (person.sex == "F") {
        $card.addClass("border-danger text-bg-danger");
    } else {
        $card.addClass("border-light text-bg-light");
    }

    var $cardBody = $("<div>").addClass("card-body small");

    $cardBody.append(
        $("<div>")
            .addClass(person.aka == null ? "h6" : "h6 mb-0")
            .html(displayNameInSpanish(person.name)));

    if (person.aka != null) {
        $cardBody.append(
            $("<div>")
                .addClass("small mb-2")
                .html(displayNameInSpanish(person.aka)));
    }

    var $birthDeath = $("<div>")
        .addClass("mt-1");

    if (person.dateOfBirth != null) {
        $birthDeath.html("n. " + displayDateInSpanish(person.dateOfBirth));
    } else if (person.dateOfDeath != null) {
        $birthDeath.html("?");
    }

    if (person.dateOfDeath != null) {
        $birthDeath.append(" - f. " + displayDateInSpanish(person.dateOfDeath));
    } else {
        if (person.dateOfBirth != null) {
            $birthDeath.append(" - ");
        }
        if (person.isAlive) {
            $birthDeath.append("Vive");
        } else {
            $birthDeath.append(person.sex == "F" ? "Fallecida" : "Fallecido");
        }
    }

    $cardBody.append($birthDeath);

    if (person.placeOfBirth != null) {
        $cardBody.append(
            $("<div>")
                .addClass("mt-1")
                .html("Pa&iacute;s de nacimiento: " + person.placeOfBirth));
    }

    if (person.parents.length > 0) {
        var $parents = $("<ul>")
            .addClass("mb-0");

        person.parents.forEach((parent, index) => {
            $parents.append(
                $("<li>")
                    .html(
                        $("<b>")
                            .html(displayNameInSpanish(parent.name)))
                    .append(!parent.referenceType
                        ? ""
                        : " (" + displayReferenceTypeInSpanish(parent.referenceType, parent.sex) + ")"));
        });

        $cardBody.append(
            $("<div>")
                .addClass("mt-1")
                .html("Padres: ")
                .append($parents));
    }

    if (person.spouses.length > 0) {
        var $spouses = $("<ul>")
            .addClass("mb-0");

        person.spouses.forEach((spouseWithChildren, index) => {
            $spouses.append(
                $("<li>")
                    .html(
                        $("<b>")
                            .html(displayNameInSpanish(spouseWithChildren.name))));

            if (spouseWithChildren.children.length > 0) {
                var $children = $("<ul>")
                    .addClass("mb-0");

                spouseWithChildren.children.forEach((child, index) => {
                    $children.append(
                        $("<li>")
                            .html(displayNameInSpanish(child.name))
                            .append(!child.referenceType
                                ? ""
                                : " (" + displayReferenceTypeInSpanish(child.referenceType, child.sex) + ")"));
                });

                $spouses.append($children);
            }
        });

        $cardBody.append(
            $("<div>")
                .addClass("mt-1")
                .html("Parejas: ")
                .append($spouses));
    }

    var hasPersonsCountInTree = person.personsCountInTree != null;
    var hasSurnamesCountInTree = person.surnamesCountInTree != null;
    var hasAncestryGenerations = person.ancestryGenerations != null
            && (person.ancestryGenerations.ascending > 0 || person.ancestryGenerations.directDescending > 0);
    var hasMaxDistantRelationship = person.maxDistantRelationship != null;
    var hasDistinguishedPersonsInTree = person.distinguishedPersonsInTree != null && person.distinguishedPersonsInTree.length > 0;

    if (hasPersonsCountInTree || hasSurnamesCountInTree || hasAncestryGenerations || hasMaxDistantRelationship || hasDistinguishedPersonsInTree) {
        var $treeInfo = $("<ul>")
            .addClass("mb-0");

        if (hasAncestryGenerations) {
            $treeInfo.append(
                $("<li>")
                    .html("Ascendencia: " + getCardinal(person.ancestryGenerations.ascending, "generaci&oacute;n", "generaciones")));

            $treeInfo.append(
                $("<li>")
                    .html("Descendencia: " + getCardinal(person.ancestryGenerations.directDescending, "generaci&oacute;n", "generaciones")));
        }

        if (hasPersonsCountInTree) {
            $treeInfo.append(
                $("<li>")
                    .html("Cantidad de familiares: <b>" + person.personsCountInTree + "</b>"));
        }

        if (hasSurnamesCountInTree) {
            $treeInfo.append(
                $("<li>")
                    .html("Cantidad de apellidos: <b>" + person.surnamesCountInTree + "</b>"));
        }

        if (hasMaxDistantRelationship) {
            $treeInfo
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

        if (hasDistinguishedPersonsInTree) {
            var $distinguishedPeople = $("<div>")
                .addClass("mb-0");

            person.distinguishedPersonsInTree.forEach((nameAndPicture, index) => {
                var $profileImage = (nameAndPicture.file)
                    ? $("<img>")
                        .attr("src", nameAndPicture.file)
                        .attr("alt", nameAndPicture.name + " (profile picture)")
                        .addClass("profile-picture-small")
                    : $("<i>")
                        .attr("style", "font-size: 32px")
                        .addClass("bi bi-person");

                var $distinguishedPerson = $("<div>")
                    .addClass("mb-1")
                    .append(
                        $("<div>")
                            .addClass("row gx-1")
                            .append(
                                $("<div>")
                                    .addClass("col-3 col-sm-2 d-flex align-items-center justify-content-center")
                                    .attr("style", "height: 50px")
                                    .html($profileImage))
                            .append(
                                $("<div>")
                                    .addClass("col-9 col-sm-10 d-flex align-items-center")
                                    .text(nameAndPicture.name)));

                $distinguishedPeople
                    .append($distinguishedPerson);
            });

            $treeInfo
                .append(
                    $("<li>")
                        .html("Personas destacadas relacionadas:"))
                .append($distinguishedPeople);
        }

        $cardBody.append(
            $("<div>")
                .addClass("mt-1")
                .html("Informaci&oacute;n en el &aacute;rbol: ")
                .append($treeInfo));
    }

    if (person.ancestryCountries.length > 0) {
        var $countries = $("<ul>")
            .addClass("mb-0");

        person.ancestryCountries.forEach((countryName, index) => {
            $countries.append(
                $("<li>")
                    .html(countryName));
        });

        $cardBody.append(
            $("<div>")
                .addClass("mt-1")
                .html("Pa&iacute;ses en su ascendencia: ")
                .append($countries));
    }

    $cardBody
        .append(
            $("<div>")
                .addClass("mt-2 text-center")
                .attr("id", "search-family-tree-wait-sign-" + person.uuid)
                .append(
                    $("<span>")
                        .addClass("spinner-border spinner-border-sm me-1")
                        .attr("role", "status"))
                .append("Generando datos de familiares,<br>esper&aacute; unos segundos..."))
        .append(
            $("<div>")
                .addClass("mt-2 text-center")
                .html(
                    $("<a>")
                        .addClass("btn btn-sm btn-dark search-family-tree-btn disabled")
                        .attr("id", "search-family-tree-btn-" + person.uuid)
                        .attr("role", "button")
                        .attr("href", "javascript:void(0)")
                        .attr("tabindex", "-1")
                        .on(
                            "click",
                            {
                                personUuid: person.uuid,
                                btnLocator: "#search-family-tree-btn-" + person.uuid,
                                errorLocator: "#search-family-tree-error-" + person.uuid
                            },
                            searchFamilyTree)
                        .html("Descargar listado de familiares (PDF)")))
        .append(
            $("<div>")
                .addClass("mt-1 text-center")
                .html(
                    $("<a>")
                        .addClass("btn btn-sm btn-dark view-family-tree-btn disabled")
                        .attr("id", "view-family-tree-btn-" + person.uuid)
                        .attr("role", "button")
                        .attr("href", "/family-tree/" + person.uuid
                            + (new URLSearchParams(window.location.search).get("f") !== "0" ? "" : "?f=0"))
                        .attr("target", "_blank")
                        .html("Ver &aacute;rbol geneal&oacute;gico online")))
        .append(
            $("<div>")
                .addClass("d-none text-center mt-2")
                .attr("id", "search-family-tree-error-" + person.uuid));

    return $card.html($cardBody);
};

var getSurnameComponent = function(searchSurnameResult, index) {
    var $card = $("<div>")
        .addClass("card border-default text-bg-light");

    if (index > 0) {
        $card.addClass("mt-2");
    }

    var $cardHeader = $("<div>")
        .addClass("card-header")
        .html(searchSurnameResult.surname);

    var $cardBody = $("<div>")
        .addClass("card-body small");

    if (searchSurnameResult.variants.length > 0) {
        var $variants = $("<ul>")
            .addClass("mb-0");

        searchSurnameResult.variants.forEach((surnameVariant, index) => {
            $variants.append(
                $("<li>")
                    .html(surnameVariant));
        });

        $cardBody.append(
            $("<div>")
                .addClass("mt-1")
                .html("Variantes: ")
                .append($variants));
    }

    $cardBody.append(
        $("<div>")
            .addClass("mt-1")
            .html("Cantidad de personas: " + searchSurnameResult.frequency));

    if (searchSurnameResult.countries.length > 0) {
        var $countries = $("<ul>")
            .addClass("mb-0");

        searchSurnameResult.countries.forEach((countryName, index) => {
            $countries.append(
                $("<li>")
                    .html(countryName));
        });

        $cardBody.append(
            $("<div>")
                .addClass("mt-1")
                .html("Pa&iacute;ses: ")
                .append($countries));
    }

    if (searchSurnameResult.firstSeenYear != null && searchSurnameResult.lastSeenYear != null) {
          $cardBody.append(
              $("<div>")
                  .addClass("mt-1")
                  .html("Rango de a&ntilde;os de eventos: " + searchSurnameResult.firstSeenYear + "-" + searchSurnameResult.lastSeenYear));
    }

    return $card
        .append($cardHeader)
        .append($cardBody);
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

var displayReferenceTypeInSpanish = function(referenceType, sex) {
    if (referenceType == "ADOPTED_CHILD") {
        return sex == "F" ? "adoptiva" : "adoptivo";
    }
    if (referenceType == "FOSTER_CHILD") {
        return "de crianza";
    }
    if (referenceType == "ADOPTIVE_PARENT") {
        return sex == "F" ? "adoptiva" : "adoptivo";
    }
    if (referenceType == "FORTER_PARENT") {
        return "de crianza";
    }
    return "";
};

var displayRelationshipInSpanish = function(relationship) {
    if (relationship.referenceType == "SELF") {
        return "<b>esta persona</b>";
    }

    var separated = (relationship.isSeparated ? "ex-" : "");

    if (relationship.referenceType == "SPOUSE") {
        return "<b>" + separated + "pareja</b>";
    }

    var spousePrefix = (relationship.isInLaw ? separated + "pareja de " : "");

    if (relationship.referenceType == "PARENT") {
        if (relationship.generation == 1) {
            var sexSuffix = getSexSuffixInSpanish(relationship);
            var relationshipName = sexSuffix == "o" ? "padre" : "madre";
            var adoptionSuffix = getAdoptionSuffixInSpanish(relationship.adoptionType, sexSuffix);
            return "<b>" + spousePrefix + relationshipName + adoptionSuffix + "</b>";
        }

        var sexSuffix = getSexSuffixInSpanish(relationship);
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
            or = "<br>&nbsp; (" + spousePrefix + "ancestro directo de " + relationship.generation + " generaciones)";
        }

        return "<b>" + spousePrefix + relationshipName + gradeSuffix + "</b>" + or;
    }

    if (relationship.referenceType == "CHILD") {
        if (relationship.generation == 1) {
            if (relationship.isInLaw && relationship.adoptionType == null) {
                return "<b>" + (relationship.personSex == "M" ? separated + "yerno" : separated + "nuera") + "</b>";
            }
            var sexSuffix = getSexSuffixInSpanish(relationship);
            var adoptionSuffix = getAdoptionSuffixInSpanish(relationship.adoptionType, sexSuffix);
            return "<b>" + spousePrefix + "hij" + sexSuffix + adoptionSuffix + "</b>";
        }

        var sexSuffix = getSexSuffixInSpanish(relationship);
        var gradeSuffix = getGradeSuffixInSpanish(relationship.generation - 4, sexSuffix);

        var relationshipName;
        if (relationship.generation == 2) {
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
            or = "<br>&nbsp; (" + spousePrefix + "descendiente directo de " + relationship.generation + " generaciones)";
        }

        return "<b>" + spousePrefix + relationshipName + gradeSuffix + "</b>" + or;
    }

    if (relationship.referenceType == "SIBLING") {
        if (relationship.isInLaw && !relationship.isHalf) {
            return relationship.personSex == "M" ? separated + "cu&ntilde;ado" : separated + "cu&ntilde;ada";
        }

        var halfPrefix = relationship.isHalf ? "medio-" : "";
        var sexSuffix = getSexSuffixInSpanish(relationship);

        var relationshipName = "herman" + sexSuffix;
        return "<b>" + spousePrefix + halfPrefix + relationshipName + "</b>";
    }

    if (relationship.referenceType == "COUSIN") {
        var halfPrefix = relationship.isHalf ? "medio-" : "";
        var sexSuffix = getSexSuffixInSpanish(relationship);
        var gradeSuffix = getGradeSuffixInSpanish(relationship.grade, sexSuffix);

        var relationshipName = "prim" + sexSuffix;
        return "<b>" + spousePrefix + halfPrefix + relationshipName + gradeSuffix + "</b>";
    }

    if (relationship.referenceType == "PIBLING") {
        var halfPrefix = relationship.isHalf ? "medio-" : "";
        var sexSuffix = getSexSuffixInSpanish(relationship);
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
            var relationshipNameOr1 = (relationshipName2 == "")
                ? getTreeSideInSpanish(relationship.treeSides, "padre/madre")
                : relationshipName2.substring(0, relationshipName2.length - 1) + "o/a";
            var relationshipNameOr2;
            if (relationship.grade == 1) {
                relationshipNameOr2 = "herman" + sexSuffix;
            } else {
                relationshipNameOr2 = "prim" + sexSuffix;
            }
            var gradeSuffixOr = getGradeSuffixInSpanish(relationship.grade - 1, sexSuffix);
            or = "<br>&nbsp; (" + spousePrefix + halfPrefix + relationshipNameOr2 + gradeSuffixOr + " de " + relationshipNameOr1 + ")";
        }

        return "<b>" + spousePrefix + halfPrefix + relationshipName1 + relationshipName2 + gradeSuffix + "</b>" + or;
    }

    if (relationship.referenceType == "NIBLING") {
        var halfPrefix = relationship.isHalf ? "medio-" : "";
        var sexSuffix = getSexSuffixInSpanish(relationship);
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
            or = "<br>&nbsp; (" + spousePrefix + relationshipNameOr1 + " de " + halfPrefix + relationshipNameOr2 + gradeSuffixOr + ")";
        }

        return "<b>" + spousePrefix + halfPrefix + relationshipName1 + relationshipName2 + gradeSuffix + "</b>" + or;
    }

    return "<b>familiar</b>";
};

var getTreeSideInSpanish = function(treeSides, defaultValue) {
    if (!treeSides) {
        return defaultValue;
    }
    if (["FATHER", "MOTHER"].every(side => treeSides.includes(side))) {
        return "padre/madre";
    }
    if (treeSides.includes("FATHER")) {
        return "padre";
    }
    if (treeSides.includes("MOTHER")) {
        return "madre";
    }
    return defaultValue;
};

var getSexSuffixInSpanish = function(relationship) {
    if (relationship.isInLaw) {
        return (relationship.spouseSex == "M" ? "o" : "a");
    } else {
        return (relationship.personSex == "M" ? "o" : "a");
    }
};

var getGradeSuffixInSpanish = function(grade, sexSuffix) {
    if (grade <= 1) {
        return "";
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

var getAdoptionSuffixInSpanish = function(adoptionType, sexSuffix) {
    if (!adoptionType) {
        return "";
    }
    if (adoptionType == "ADOPTIVE") {
        return " adoptiv" + sexSuffix;
    }
    if (referenceType == "FOSTER") {
        return " de crianza";
    }
    return "";
};

var displayErrorCodeInSpanish = function(errorCode) {
    if (errorCode == "TOO-MANY-REQUESTS") {
        return "<p>Realizaste demasiadas consultas en la &uacute;ltima hora, por favor esper&aacute; unos minutos o contactanos en redes sociales: <b>@genea.azul</b></p>"
            + "<p>Este es un proyecto de investigaci&oacute;n sin fines de lucro, no hay costo de servicio. Solicitá acceso al árbol y ayudanos a completar datos. 😊</p>";
    }
    return errorCode;
};
