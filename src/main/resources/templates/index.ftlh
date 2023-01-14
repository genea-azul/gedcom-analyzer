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
      <div class="me-1">Versi&oacute;n: 0.7.3 - Personas en el &aacute;rbol:</div>
      <div id="persons-count-container">
        <span class="spinner-border spinner-border-sm" role="status"></span>
      </div>
    </div>
    <div class="mt-3 mb-3">Todos los campos son opcionales, para obtener un mejor resultado complet&aacute; la mayor cantidad de datos posibles.</div>

    <div class="row">
      <div class="col-xl-auto mb-2">

        <div class="row justify-content-center">
          <div class="col-xl-auto"></div>
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
                  <input type="text" class="form-control" id="individualGivenName" />
                  <label for="individualGivenName">Nombre</label>
                </div>
                <div class="form-floating mt-1">
                  <input type="text" class="form-control" id="individualSurname" />
                  <label for="individualSurname">Apellido</label>
                </div>
                <div class="form-floating mt-1">
                  <input type="number" class="form-control" id="individualYearOfBirth" max="2050" />
                  <label for="individualYearOfBirth">A&ntilde;o de nacimiento</label>
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
          <div class="col-xl-auto"></div>
          <div class="col-xl-auto">
            <div class="card border-secondary mb-2">
              <div class="card-header text-bg-secondary">Padre</div>
              <div class="card-body">
                <div class="form-floating">
                  <input type="text" class="form-control" id="fatherGivenName" />
                  <label for="fatherGivenName">Nombre</label>
                </div>
                <div class="form-floating mt-1">
                  <input type="text" class="form-control" id="fatherSurname" />
                  <label for="fatherSurname">Apellido</label>
                </div>
                <div class="form-floating mt-1">
                  <input type="number" class="form-control" id="fatherYearOfBirth" max="2050" />
                  <label for="fatherYearOfBirth">A&ntilde;o de nacimiento</label>
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
          <div class="col-xl-auto"></div>
          <div class="col-xl-auto">
            <div class="card border-danger mb-2">
              <div class="card-header text-bg-danger">Madre</div>
              <div class="card-body">
                <div class="form-floating">
                  <input type="text" class="form-control" id="motherGivenName" />
                  <label for="motherGivenName">Nombre</label>
                </div>
                <div class="form-floating mt-1">
                  <input type="text" class="form-control" id="motherSurname" />
                  <label for="motherSurname">Apellido</label>
                </div>
                <div class="form-floating mt-1">
                  <input type="number" class="form-control" id="motherYearOfBirth" max="2050" />
                  <label for="motherYearOfBirth">A&ntilde;o de nacimiento</label>
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
          <div class="col-xl-auto"></div>
        </div>

        <div class="row mt-2 mb-2" id="grandparentsContainerShowBtnContainer">
          <div class="col-xl-auto">
            <div class="form-check">
              <input class="form-check-input" type="checkbox" id="grandparentsContainerShowBtn" />
              <label class="form-check-label" for="grandparentsContainerShowBtn">Agregar informaci&oacute;n de abuelos</label>
            </div>
          </div>
        </div>

        <div class="row justify-content-center d-none" id="grandparentsContainer">
          <div class="col-xl-auto">
            <div class="card border-secondary mb-2">
              <div class="card-header text-bg-secondary">Abuelo paterno</div>
              <div class="card-body">
                <div class="form-floating">
                  <input type="text" class="form-control" id="paternalGrandfatherGivenName" />
                  <label for="paternalGrandfatherGivenName">Nombre</label>
                </div>
                <div class="form-floating mt-1">
                  <input type="text" class="form-control" id="paternalGrandfatherSurname" />
                  <label for="paternalGrandfatherSurname">Apellido</label>
                </div>
                <div class="form-floating mt-1">
                  <input type="number" class="form-control" id="paternalGrandfatherYearOfBirth" max="2050" />
                  <label for="paternalGrandfatherYearOfBirth">A&ntilde;o de nacimiento</label>
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
                  <input type="text" class="form-control" id="paternalGrandmotherGivenName" />
                  <label for="paternalGrandmotherGivenName">Nombre</label>
                </div>
                <div class="form-floating mt-1">
                  <input type="text" class="form-control" id="paternalGrandmotherSurname" />
                  <label for="paternalGrandmotherSurname">Apellido</label>
                </div>
                <div class="form-floating mt-1">
                  <input type="number" class="form-control" id="paternalGrandmotherYearOfBirth" max="2050" />
                  <label for="paternalGrandmotherYearOfBirth">A&ntilde;o de nacimiento</label>
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
                  <input type="text" class="form-control" id="maternalGrandfatherGivenName" />
                  <label for="maternalGrandfatherGivenName">Nombre</label>
                </div>
                <div class="form-floating mt-1">
                  <input type="text" class="form-control" id="maternalGrandfatherSurname" />
                  <label for="maternalGrandfatherSurname">Apellido</label>
                </div>
                <div class="form-floating mt-1">
                  <input type="number" class="form-control" id="maternalGrandfatherYearOfBirth" max="2050" />
                  <label for="maternalGrandfatherYearOfBirth">A&ntilde;o de nacimiento</label>
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
                  <input type="text" class="form-control" id="maternalGrandmotherGivenName" />
                  <label for="maternalGrandmotherGivenName">Nombre</label>
                </div>
                <div class="form-floating mt-1">
                  <input type="text" class="form-control" id="maternalGrandmotherSurname" />
                  <label for="maternalGrandmotherSurname">Apellido</label>
                </div>
                <div class="form-floating mt-1">
                  <input type="number" class="form-control" id="maternalGrandmotherYearOfBirth" max="2050" />
                  <label for="maternalGrandmotherYearOfBirth">A&ntilde;o de nacimiento</label>
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

      </div>

      <div class="col-xl-4 mb-2">
        <div class="small mb-3">
          Dejanos tu email, Whatsapp o Instagram y nos pondremos en contacto en caso de encontrar personas relacionadas a tu b&uacute;squeda en un futuro.
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
