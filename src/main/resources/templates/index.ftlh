<!DOCTYPE html>
<html lang="es">
  <head>
    <title>Genea Azul - Buscador de personas</title>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/css/bootstrap.min.css" rel="stylesheet" integrity="sha384-rbsA2VBKQhggwzxH7pPCaAqO46MgnOM80zW1RWuH61DGLwZJEdK2Kadq2F9CUG65" crossorigin="anonymous" />
    <link href="css/main.css" rel="stylesheet" />
  </head>

  <body class="container-fluid">
    <div class="h5 mb-0">Genea Azul - Buscador de personas</div>
    <div class="d-flex mt-0 small">
      <div class="me-1">Versi&oacute;n: ${projectVersion} - Personas en el &aacute;rbol:</div>
      <div id="persons-count-container">
        <span class="spinner-border spinner-border-sm" role="status"></span>
      </div>
    </div>
    <div class="mt-3 mb-3">Todos los campos son opcionales, para obtener un mejor resultado complet&aacute; la mayor cantidad de datos posibles.</div>

    <div class="row mb-3">
      <div class="col-xl-auto">
        <div class="card border-danger mb-2" id="individualCard">
          <div class="card-header text-bg-danger">Persona principal</div>
          <div class="card-body">
            <span class="small text-secondary">Sexo:</span>
            <div class="btn-group btn-group-sm" role="group" aria-label="Sexo">
              <input type="radio" class="btn-check" name="individualSex" id="individualSexF" value="F" autocomplete="off" checked="checked" />
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
        <div class="card border-secondary mb-2" id="spouseCard">
          <div class="card-header text-bg-secondary">Pareja</div>
          <div class="card-body">
            <div class="alert alert-light mb-0" role="alert" id="spouseContainerShowBtnContainer">
              <div class="form-check">
                <input class="form-check-input" type="checkbox" id="spouseContainerShowBtn" />
                <label class="form-check-label text-break card-label-max-width d-none d-xl-block" for="spouseContainerShowBtn">Agregar informaci&oacute;n de pareja</label>
                <label class="form-check-label text-break d-xl-none" for="spouseContainerShowBtn">Agregar informaci&oacute;n de pareja</label>
              </div>
            </div>
            <div class="d-none" id="spouseContainer">
              <span class="small text-secondary">Sexo:</span>
              <div class="btn-group btn-group-sm" role="group" aria-label="Sexo">
                <input type="radio" class="btn-check" name="spouseSex" id="spouseSexF" value="F" autocomplete="off" />
                <label class="btn btn-outline-danger" for="spouseSexF">Mujer</label>
                <input type="radio" class="btn-check" name="spouseSex" id="spouseSexM" value="M" autocomplete="off" checked="checked" />
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
        <div class="card border-secondary mb-2">
          <div class="card-header text-bg-secondary">Abuelos/as</div>
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
      <div class="col-xl-auto"></div>
      <div class="col-xl-auto">
        <div class="small mb-3 contact-label-max-width d-none d-xl-block">
          Dejanos tu <b>email</b>, <b>Whatsapp</b> o <b>Instagram</b> y nos pondremos en contacto en caso de encontrar personas relacionadas a tu b&uacute;squeda en un futuro.
        </div>
        <div class="small mb-3 d-xl-none">
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
      <div class="col-xl-auto">
        <div class="card border-dark" id="searchResultCard">
          <div class="card-header text-bg-dark">
            <div>Resultado de la b&uacute;squeda</div>
            <div class="small">
              Los datos de personas vivas serán ocultados (&lt;dato privado&gt;)
            </div>
          </div>
          <div class="card-body overflow-auto"></div>
        </div>
      </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/js/bootstrap.bundle.min.js" integrity="sha384-kenU1KFdBIe4zVF0s0G1M5b4hcpxyD9F7jL+jjXkk+Q2h455rYXK/7HAuoJl+0I4" crossorigin="anonymous"></script>
    <script src="https://cdn.jsdelivr.net/npm/jquery@3.6.3/dist/jquery.min.js"></script>
    <script src="js/main.js"></script>
  </body>

</html>
