<!DOCTYPE html>
<html lang="es">
  <head>
    <title>Genea Azul - Buscador de personas</title>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/css/bootstrap.min.css" integrity="sha384-rbsA2VBKQhggwzxH7pPCaAqO46MgnOM80zW1RWuH61DGLwZJEdK2Kadq2F9CUG65" crossorigin="anonymous" />
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.5/font/bootstrap-icons.css" />
    <link rel="stylesheet" href="/css/main.css?v${projectVersion}" />
  </head>

  <body class="container">

    <img src="img/genea-azul-logo.png?v${projectVersion}" width="120" height="60" class="rounded float-start mt-2" alt="Genea Azul logo" />

    <div class="mt-2 mb-0 text-end">
      <a class="link-secondary text-decoration-none ms-1 me-1" href="https://instagram.com/_u/genea.azul" target="_blank" title="@genea.azul">
        <i class="bi bi-instagram"></i>
      </a>
      <a class="link-secondary text-decoration-none ms-1 me-1" href="https://facebook.com/genea.azul" target="_blank" title="@genea.azul">
        <i class="bi bi-facebook"></i>
      </a>
      <a class="link-secondary text-decoration-none ms-1 me-1" href="mailto:genea.azul@gmail.com" target="_blank" title="genea.azul@gmail.com">
        <i class="bi bi-envelope"></i>
      </a>
    </div>

    <div class="text-center">
      <div class="h3 m-0 d-none d-xl-block">
        Buscador de personas
      </div>
      <div class="h5 m-0 d-xl-none">
        Buscador de personas
      </div>
      <div class="small">
        Personas en el &aacute;rbol:
        <span id="persons-count-container">
          <span class="spinner-border spinner-border-sm" role="status"></span>
        </span>
      </div>
    </div>

    <div class="small mt-2 mb-3">Todos los campos son opcionales, para obtener un mejor resultado complet&aacute; la mayor cantidad de datos posibles.</div>

    <div class="row mb-3">
      <div class="col-xl-auto">
        <div class="card border-dark mb-2" id="individualCard">
          <div class="card-header text-bg-dark">
            <i class="card-header-icon float-end"></i>
            Persona principal
          </div>
          <div class="card-body">
            <span class="small text-secondary d-none">Sexo:</span>
            <div class="btn-group btn-group-sm d-none" role="group" aria-label="Sexo">
              <input type="radio" class="btn-check" name="individualSex" id="individualSexF" value="F" autocomplete="off" />
              <label class="btn btn-outline-danger" for="individualSexF">Mujer</label>
              <input type="radio" class="btn-check" name="individualSex" id="individualSexM" value="M" autocomplete="off" />
              <label class="btn btn-outline-secondary" for="individualSexM">Hombre</label>
            </div>
            <div class="form-floating mt-1">
              <input type="text" class="form-control" id="individualGivenName" maxlength="60" />
              <label for="individualGivenName">Nombre</label>
            </div>
            <div class="form-floating mt-1">
              <input type="text" class="form-control" id="individualSurname" maxlength="60" />
              <label for="individualSurname">Apellido</label>
            </div>
            <div class="form-floating mt-1">
              <input type="number" class="form-control" id="individualYearOfBirth" max="2050" />
              <label for="individualYearOfBirth">A&ntilde;o de nacimiento</label>
            </div>
            <div class="form-floating mt-1">
              <input type="text" class="form-control" id="individualPlaceOfBirth" maxlength="80" />
              <label for="individualPlaceOfBirth">Lugar de nacimiento</label>
            </div>
            <div class="form-check mt-1">
              <input class="form-check-input" type="checkbox" id="individualIsAlive" checked="checked" />
              <label class="form-check-label" for="individualIsAlive">Vive</label>
            </div>
            <div class="form-floating mt-1">
              <input type="number" class="form-control" id="individualYearOfDeath" max="2050" disabled="disabled" />
              <label for="individualYearOfDeath">A&ntilde;o de fallecimiento</label>
            </div>
          </div>
        </div>
      </div>
      <div class="col-xl-auto">
        <div class="card border-dark mb-2" id="spouseCard">
          <div class="card-header text-bg-dark">
            <i class="card-header-icon float-end"></i>
            Pareja
          </div>
          <div class="card-body">
            <div class="alert alert-light mb-0" role="alert" id="spouseContainerShowBtnContainer">
              <div class="form-check">
                <input class="form-check-input" type="checkbox" id="spouseContainerShowBtn" />
                <label class="form-check-label text-break card-label-max-width d-none d-xl-block" for="spouseContainerShowBtn">Agregar informaci&oacute;n de pareja</label>
                <label class="form-check-label text-break d-xl-none" for="spouseContainerShowBtn">Agregar informaci&oacute;n de pareja</label>
              </div>
            </div>
            <div class="d-none" id="spouseContainer">
              <span class="small text-secondary d-none">Sexo:</span>
              <div class="btn-group btn-group-sm d-none" role="group" aria-label="Sexo">
                <input type="radio" class="btn-check" name="spouseSex" id="spouseSexF" value="F" autocomplete="off" />
                <label class="btn btn-outline-danger" for="spouseSexF">Mujer</label>
                <input type="radio" class="btn-check" name="spouseSex" id="spouseSexM" value="M" autocomplete="off" />
                <label class="btn btn-outline-secondary" for="spouseSexM">Hombre</label>
              </div>
              <div class="form-floating mt-1">
                <input type="text" class="form-control" id="spouseGivenName" maxlength="60" />
                <label for="spouseGivenName">Nombre</label>
              </div>
              <div class="form-floating mt-1">
                <input type="text" class="form-control" id="spouseSurname" maxlength="60" />
                <label for="spouseSurname">Apellido</label>
              </div>
              <div class="form-floating mt-1">
                <input type="number" class="form-control" id="spouseYearOfBirth" max="2050" />
                <label for="spouseYearOfBirth">A&ntilde;o de nacimiento</label>
              </div>
              <div class="form-floating mt-1">
                <input type="text" class="form-control" id="spousePlaceOfBirth" maxlength="80" />
                <label for="spousePlaceOfBirth">Lugar de nacimiento</label>
              </div>
              <div class="form-check mt-1">
                <input class="form-check-input" type="checkbox" id="spouseIsAlive" checked="checked" />
                <label class="form-check-label" for="spouseIsAlive">Vive</label>
              </div>
              <div class="form-floating mt-1">
                <input type="number" class="form-control" id="spouseYearOfDeath" max="2050" disabled="disabled" />
                <label for="spouseYearOfDeath">A&ntilde;o de fallecimiento</label>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div class="col-xl-auto">
        <div class="card border-secondary mb-2">
          <div class="card-header text-bg-secondary">Padre</div>
          <div class="card-body">
            <div class="form-floating">
              <input type="text" class="form-control" id="fatherGivenName" maxlength="60" />
              <label for="fatherGivenName">Nombre</label>
            </div>
            <div class="form-floating mt-1">
              <input type="text" class="form-control" id="fatherSurname" maxlength="60" />
              <label for="fatherSurname">Apellido</label>
            </div>
            <div class="form-floating mt-1">
              <input type="number" class="form-control" id="fatherYearOfBirth" max="2050" />
              <label for="fatherYearOfBirth">A&ntilde;o de nacimiento</label>
            </div>
            <div class="form-floating mt-1">
              <input type="text" class="form-control" id="fatherPlaceOfBirth" maxlength="80" />
              <label for="fatherPlaceOfBirth">Lugar de nacimiento</label>
            </div>
            <div class="form-check mt-1">
              <input class="form-check-input" type="checkbox" id="fatherIsAlive" checked="checked" />
              <label class="form-check-label" for="fatherIsAlive">Vive</label>
            </div>
            <div class="form-floating mt-1">
              <input type="number" class="form-control" id="fatherYearOfDeath" max="2050" disabled="disabled" />
              <label for="fatherYearOfDeath">A&ntilde;o de fallecimiento</label>
            </div>
          </div>
        </div>
      </div>
      <div class="col-xl-auto">
        <div class="card border-danger mb-2">
          <div class="card-header text-bg-danger">Madre</div>
          <div class="card-body">
            <div class="form-floating">
              <input type="text" class="form-control" id="motherGivenName" maxlength="60" />
              <label for="motherGivenName">Nombre</label>
            </div>
            <div class="form-floating mt-1">
              <input type="text" class="form-control" id="motherSurname" maxlength="60" />
              <label for="motherSurname">Apellido</label>
            </div>
            <div class="form-floating mt-1">
              <input type="number" class="form-control" id="motherYearOfBirth" max="2050" />
              <label for="motherYearOfBirth">A&ntilde;o de nacimiento</label>
            </div>
            <div class="form-floating mt-1">
              <input type="text" class="form-control" id="motherPlaceOfBirth" maxlength="80" />
              <label for="motherPlaceOfBirth">Lugar de nacimiento</label>
            </div>
            <div class="form-check mt-1">
              <input class="form-check-input" type="checkbox" id="motherIsAlive" checked="checked" />
              <label class="form-check-label" for="motherIsAlive">Vive</label>
            </div>
            <div class="form-floating mt-1">
              <input type="number" class="form-control" id="motherYearOfDeath" max="2050" disabled="disabled" />
              <label for="motherYearOfDeath">A&ntilde;o de fallecimiento</label>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="row mb-3" id="grandparentsContainerShowBtnContainer">
      <div class="col-xl-auto">
        <div class="card border-dark mb-2">
          <div class="card-header text-bg-dark">Abuelos/as</div>
          <div class="card-body">
            <div class="alert alert-light mb-0" role="alert">
              <div class="form-check">
                <input class="form-check-input" type="checkbox" id="grandparentsContainerShowBtn" />
                <label class="form-check-label" for="grandparentsContainerShowBtn">Agregar informaci&oacute;n de abuelos/as</label>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="row mb-3 d-none" id="grandparentsContainer">
      <div class="col-xl-auto">
        <div class="card border-secondary mb-2">
          <div class="card-header text-bg-secondary">Abuelo paterno</div>
          <div class="card-body">
            <div class="form-floating">
              <input type="text" class="form-control" id="paternalGrandfatherGivenName" maxlength="60" />
              <label for="paternalGrandfatherGivenName">Nombre</label>
            </div>
            <div class="form-floating mt-1">
              <input type="text" class="form-control" id="paternalGrandfatherSurname" maxlength="60" />
              <label for="paternalGrandfatherSurname">Apellido</label>
            </div>
            <div class="form-floating mt-1">
              <input type="number" class="form-control" id="paternalGrandfatherYearOfBirth" max="2050" />
              <label for="paternalGrandfatherYearOfBirth">A&ntilde;o de nacimiento</label>
            </div>
            <div class="form-floating mt-1">
              <input type="text" class="form-control" id="paternalGrandfatherPlaceOfBirth" maxlength="80" />
              <label for="paternalGrandfatherPlaceOfBirth">Lugar de nacimiento</label>
            </div>
            <div class="form-check mt-1">
              <input class="form-check-input" type="checkbox" id="paternalGrandfatherIsAlive" checked="checked" />
              <label class="form-check-label" for="paternalGrandfatherIsAlive">Vive</label>
            </div>
            <div class="form-floating mt-1">
              <input type="number" class="form-control" id="paternalGrandfatherYearOfDeath" max="2050" disabled="disabled" />
              <label for="paternalGrandfatherYearOfDeath">A&ntilde;o de fallecimiento</label>
            </div>
          </div>
        </div>
      </div>
      <div class="col-xl-auto">
        <div class="card border-danger mb-2">
          <div class="card-header text-bg-danger">Abuela paterna</div>
          <div class="card-body">
            <div class="form-floating">
              <input type="text" class="form-control" id="paternalGrandmotherGivenName" maxlength="60" />
              <label for="paternalGrandmotherGivenName">Nombre</label>
            </div>
            <div class="form-floating mt-1">
              <input type="text" class="form-control" id="paternalGrandmotherSurname" maxlength="60" />
              <label for="paternalGrandmotherSurname">Apellido</label>
            </div>
            <div class="form-floating mt-1">
              <input type="number" class="form-control" id="paternalGrandmotherYearOfBirth" max="2050" />
              <label for="paternalGrandmotherYearOfBirth">A&ntilde;o de nacimiento</label>
            </div>
            <div class="form-floating mt-1">
              <input type="text" class="form-control" id="paternalGrandmotherPlaceOfBirth" maxlength="80" />
              <label for="paternalGrandmotherPlaceOfBirth">Lugar de nacimiento</label>
            </div>
            <div class="form-check mt-1">
              <input class="form-check-input" type="checkbox" id="paternalGrandmotherIsAlive" checked="checked" />
              <label class="form-check-label" for="paternalGrandmotherIsAlive">Vive</label>
            </div>
            <div class="form-floating mt-1">
              <input type="number" class="form-control" id="paternalGrandmotherYearOfDeath" max="2050" disabled="disabled" />
              <label for="paternalGrandmotherYearOfDeath">A&ntilde;o de fallecimiento</label>
            </div>
          </div>
        </div>
      </div>
      <div class="col-xl-auto">
        <div class="card border-secondary mb-2">
          <div class="card-header text-bg-secondary">Abuelo materno</div>
          <div class="card-body">
            <div class="form-floating">
              <input type="text" class="form-control" id="maternalGrandfatherGivenName" maxlength="60" />
              <label for="maternalGrandfatherGivenName">Nombre</label>
            </div>
            <div class="form-floating mt-1">
              <input type="text" class="form-control" id="maternalGrandfatherSurname" maxlength="60" />
              <label for="maternalGrandfatherSurname">Apellido</label>
            </div>
            <div class="form-floating mt-1">
              <input type="number" class="form-control" id="maternalGrandfatherYearOfBirth" max="2050" />
              <label for="maternalGrandfatherYearOfBirth">A&ntilde;o de nacimiento</label>
            </div>
            <div class="form-floating mt-1">
              <input type="text" class="form-control" id="maternalGrandfatherPlaceOfBirth" maxlength="80" />
              <label for="maternalGrandfatherPlaceOfBirth">Lugar de nacimiento</label>
            </div>
            <div class="form-check mt-1">
              <input class="form-check-input" type="checkbox" id="maternalGrandfatherIsAlive" checked="checked" />
              <label class="form-check-label" for="maternalGrandfatherIsAlive">Vive</label>
            </div>
            <div class="form-floating mt-1">
              <input type="number" class="form-control" id="maternalGrandfatherYearOfDeath" max="2050" disabled="disabled" />
              <label for="maternalGrandfatherYearOfDeath">A&ntilde;o de fallecimiento</label>
            </div>
          </div>
        </div>
      </div>
      <div class="col-xl-auto">
        <div class="card border-danger mb-2">
          <div class="card-header text-bg-danger">Abuela materna</div>
          <div class="card-body">
            <div class="form-floating">
              <input type="text" class="form-control" id="maternalGrandmotherGivenName" maxlength="60" />
              <label for="maternalGrandmotherGivenName">Nombre</label>
            </div>
            <div class="form-floating mt-1">
              <input type="text" class="form-control" id="maternalGrandmotherSurname" maxlength="60" />
              <label for="maternalGrandmotherSurname">Apellido</label>
            </div>
            <div class="form-floating mt-1">
              <input type="number" class="form-control" id="maternalGrandmotherYearOfBirth" max="2050" />
              <label for="maternalGrandmotherYearOfBirth">A&ntilde;o de nacimiento</label>
            </div>
            <div class="form-floating mt-1">
              <input type="text" class="form-control" id="maternalGrandmotherPlaceOfBirth" maxlength="80" />
              <label for="maternalGrandmotherPlaceOfBirth">Lugar de nacimiento</label>
            </div>
            <div class="form-check mt-1">
              <input class="form-check-input" type="checkbox" id="maternalGrandmotherIsAlive" checked="checked" />
              <label class="form-check-label" for="maternalGrandmotherIsAlive">Vive</label>
            </div>
            <div class="form-floating mt-1">
              <input type="number" class="form-control" id="maternalGrandmotherYearOfDeath" max="2050" disabled="disabled" />
              <label for="maternalGrandmotherYearOfDeath">A&ntilde;o de fallecimiento</label>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="row mb-3">
      <div class="col-xl-auto col-xxl-auto"></div>
      <div class="col-xl-5 col-xxl-4">
        <div class="small mb-3 text-center">
          Dejanos tu <b>email</b>, <b>Whatsapp</b> o <b>Instagram</b> y nos pondremos en contacto en caso de encontrar personas relacionadas a tu b&uacute;squeda en un futuro.
        </div>
        <div class="row justify-content-center align-items-center">
          <div class="col-auto mb-3">
            <div class="form-floating">
              <input type="text" class="form-control" id="individualContact" size="24" />
              <label for="individualContact">Email, Whatsapp o Instagram</label>
            </div>
          </div>
          <div class="col-auto mb-3">
            <button type="button" class="btn btn-dark" id="searchBtn">Buscar</button>
          </div>
        </div>
      </div>
      <div class="col-xl-5 col-xxl-4">
        <div class="card border-dark" id="searchResultCard">
          <div class="card-header text-bg-dark">
            <div>Resultado de la b&uacute;squeda</div>
            <div class="small">
              <#if obfuscateLiving == true>
                Los datos de personas vivas <b>ser&aacute;n ocultados</b>
              </#if>
            </div>
          </div>
          <div class="card-body overflow-auto"></div>
        </div>
      </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/js/bootstrap.min.js" integrity="sha384-cuYeSxntonz0PPNlHhBs68uyIAVpIIOZZ5JqeqvYYIcEL727kskC66kF92t6Xl2V" crossorigin="anonymous"></script>
    <script src="https://cdn.jsdelivr.net/npm/jquery@3.7.0/dist/jquery.min.js"></script>
    <script src="js/main.js?v${projectVersion}"></script>
  </body>

</html>
