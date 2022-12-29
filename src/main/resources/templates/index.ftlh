<!DOCTYPE html>
<html lang="es">
  <head>
    <title>Genea Azul - Buscador de personas</title>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <link href="css/main.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/css/bootstrap.min.css" rel="stylesheet" integrity="sha384-rbsA2VBKQhggwzxH7pPCaAqO46MgnOM80zW1RWuH61DGLwZJEdK2Kadq2F9CUG65" crossorigin="anonymous">
    <link href="https://cdn.jsdelivr.net/npm/jquery.json-viewer@1.5.0/json-viewer/jquery.json-viewer.css" rel="stylesheet" crossorigin="anonymous">
  </head>

  <body class="container-fluid">
    <h3>Genea Azul - Buscador de personas en el &aacute;rbol</h3>
    <p>Todos los campos son opcionales, para obtener un mejor resultado complet&aacute; la mayor cantidad de datos posibles.</p>

    <div class="row">
      <div class="col-lg-auto">

        <div class="row">
          <div class="col-lg-auto">
            <div class="card border-secondary mb-2">
              <div class="card-header text-bg-secondary">Abuelo paterno</div>
              <div class="card-body">
                <div class="form-floating">
                  <input type="text" class="form-control" id="paternalGrandfatherGivenName">
                  <label for="paternalGrandfatherGivenName">Nombre</label>
                </div>
                <div class="form-floating mt-1">
                  <input type="text" class="form-control" id="paternalGrandfatherSurname">
                  <label for="paternalGrandfatherSurname">Apellido</label>
                </div>
                <div class="form-floating mt-1">
                  <input type="number" class="form-control" id="paternalGrandfatherYearOfBirth">
                  <label for="paternalGrandfatherYearOfBirth">A&ntilde;o de nacimiento</label>
                </div>
                <div class="form-check form-switch mt-1">
                  <input class="form-check-input" type="checkbox" role="switch" id="paternalGrandfatherIsAlive" checked>
                  <label class="form-check-label" for="paternalGrandfatherIsAlive">Vive</label>
                </div>
                <div class="form-floating d-none mt-1">
                  <input type="number" class="form-control" id="paternalGrandfatherYearOfDeath" disabled>
                  <label for="paternalGrandfatherYearOfDeath">A&ntilde;o de fallecimiento</label>
                </div>
              </div>
            </div>
          </div>
          <div class="col-lg-auto">
            <div class="card border-danger mb-2">
              <div class="card-header text-bg-danger">Abuela paterna</div>
              <div class="card-body">
                <div class="form-floating">
                  <input type="text" class="form-control" id="paternalGrandmotherGivenName">
                  <label for="paternalGrandmotherGivenName">Nombre</label>
                </div>
                <div class="form-floating mt-1">
                  <input type="text" class="form-control" id="paternalGrandmotherSurname">
                  <label for="paternalGrandmotherSurname">Apellido</label>
                </div>
                <div class="form-floating mt-1">
                  <input type="number" class="form-control" id="paternalGrandmotherYearOfBirth">
                  <label for="paternalGrandmotherYearOfBirth">A&ntilde;o de nacimiento</label>
                </div>
                <div class="form-check form-switch mt-1">
                  <input class="form-check-input" type="checkbox" role="switch" id="paternalGrandmotherIsAlive" checked>
                  <label class="form-check-label" for="paternalGrandmotherIsAlive">Vive</label>
                </div>
                <div class="form-floating d-none mt-1">
                  <input type="number" class="form-control" id="paternalGrandmotherYearOfDeath" disabled>
                  <label for="paternalGrandmotherYearOfDeath">A&ntilde;o de fallecimiento</label>
                </div>
              </div>
            </div>
          </div>
          <div class="col-lg-auto">
            <div class="card border-secondary mb-2">
              <div class="card-header text-bg-secondary">Abuelo materno</div>
              <div class="card-body">
                <div class="form-floating">
                  <input type="text" class="form-control" id="maternalGrandfatherGivenName">
                  <label for="maternalGrandfatherGivenName">Nombre</label>
                </div>
                <div class="form-floating mt-1">
                  <input type="text" class="form-control" id="maternalGrandfatherSurname">
                  <label for="maternalGrandfatherSurname">Apellido</label>
                </div>
                <div class="form-floating mt-1">
                  <input type="number" class="form-control" id="maternalGrandfatherYearOfBirth">
                  <label for="maternalGrandfatherYearOfBirth">A&ntilde;o de nacimiento</label>
                </div>
                <div class="form-check form-switch mt-1">
                  <input class="form-check-input" type="checkbox" role="switch" id="maternalGrandfatherIsAlive" checked>
                  <label class="form-check-label" for="maternalGrandfatherIsAlive">Vive</label>
                </div>
                <div class="form-floating d-none mt-1">
                  <input type="number" class="form-control" id="maternalGrandfatherYearOfDeath" disabled>
                  <label for="maternalGrandfatherYearOfDeath">A&ntilde;o de fallecimiento</label>
                </div>
              </div>
            </div>
          </div>
          <div class="col-lg-auto">
            <div class="card border-danger mb-2">
              <div class="card-header text-bg-danger">Abuela materna</div>
              <div class="card-body">
                <div class="form-floating">
                  <input type="text" class="form-control" id="maternalGrandmotherGivenName">
                  <label for="maternalGrandmotherGivenName">Nombre</label>
                </div>
                <div class="form-floating mt-1">
                  <input type="text" class="form-control" id="maternalGrandmotherSurname">
                  <label for="maternalGrandmotherSurname">Apellido</label>
                </div>
                <div class="form-floating mt-1">
                  <input type="number" class="form-control" id="maternalGrandmotherYearOfBirth">
                  <label for="maternalGrandmotherYearOfBirth">A&ntilde;o de nacimiento</label>
                </div>
                <div class="form-check form-switch mt-1">
                  <input class="form-check-input" type="checkbox" role="switch" id="maternalGrandmotherIsAlive" checked>
                  <label class="form-check-label" for="maternalGrandmotherIsAlive">Vive</label>
                </div>
                <div class="form-floating d-none mt-1">
                  <input type="number" class="form-control" id="maternalGrandmotherYearOfDeath" disabled>
                  <label for="maternalGrandmotherYearOfDeath">A&ntilde;o de fallecimiento</label>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="row justify-content-center">
          <div class="col-lg-auto">
            <div class="card border-danger mb-2" id="individualCard">
              <div class="card-header text-bg-danger">Persona principal</div>
              <div class="card-body">
                <div class="form-check form-check-inline">
                  <input class="form-check-input" type="radio" name="individualSex" id="individualSexM" value="M">
                  <label class="form-check-label" for="individualSexM">Hombre</label>
                </div>
                <div class="form-check form-check-inline mt-1">
                  <input class="form-check-input" type="radio" name="individualSex" id="individualSexF" value="F" checked>
                  <label class="form-check-label" for="individualSexF">Mujer</label>
                </div>
                <div class="form-floating mt-1">
                  <input type="text" class="form-control" id="individualGivenName">
                  <label for="individualGivenName">Nombre</label>
                </div>
                <div class="form-floating mt-1">
                  <input type="text" class="form-control" id="individualSurname">
                  <label for="individualSurname">Apellido</label>
                </div>
                <div class="form-floating mt-1">
                  <input type="number" class="form-control" id="individualYearOfBirth">
                  <label for="individualYearOfBirth">A&ntilde;o de nacimiento</label>
                </div>
                <div class="form-check form-switch mt-1">
                  <input class="form-check-input" type="checkbox" role="switch" id="individualIsAlive" checked>
                  <label class="form-check-label" for="individualIsAlive">Vive</label>
                </div>
                <div class="form-floating d-none mt-1">
                  <input type="number" class="form-control" id="individualYearOfDeath" disabled>
                  <label for="individualYearOfDeath">A&ntilde;o de fallecimiento</label>
                </div>
              </div>
            </div>
          </div>
          <div class="col-lg-auto"></div>
          <div class="col-lg-auto">
            <div class="card border-secondary mb-2">
              <div class="card-header text-bg-secondary">Padre</div>
              <div class="card-body">
                <div class="form-floating">
                  <input type="text" class="form-control" id="fatherGivenName">
                  <label for="fatherGivenName">Nombre</label>
                </div>
                <div class="form-floating mt-1">
                  <input type="text" class="form-control" id="fatherSurname">
                  <label for="fatherSurname">Apellido</label>
                </div>
                <div class="form-floating mt-1">
                  <input type="number" class="form-control" id="fatherYearOfBirth">
                  <label for="fatherYearOfBirth">A&ntilde;o de nacimiento</label>
                </div>
                <div class="form-check form-switch mt-1">
                  <input class="form-check-input" type="checkbox" role="switch" id="fatherIsAlive" checked>
                  <label class="form-check-label" for="fatherIsAlive">Vive</label>
                </div>
                <div class="form-floating d-none mt-1">
                  <input type="number" class="form-control" id="fatherYearOfDeath" disabled>
                  <label for="fatherYearOfDeath">A&ntilde;o de fallecimiento</label>
                </div>
              </div>
            </div>
          </div>
          <div class="col-lg-auto"></div>
          <div class="col-lg-auto">
            <div class="card border-danger mb-2">
              <div class="card-header text-bg-danger">Madre</div>
              <div class="card-body">
                <div class="form-floating">
                  <input type="text" class="form-control" id="motherGivenName">
                  <label for="motherGivenName">Nombre</label>
                </div>
                <div class="form-floating mt-1">
                  <input type="text" class="form-control" id="motherSurname">
                  <label for="motherSurname">Apellido</label>
                </div>
                <div class="form-floating mt-1">
                  <input type="number" class="form-control" id="motherYearOfBirth">
                  <label for="motherYearOfBirth">A&ntilde;o de nacimiento</label>
                </div>
                <div class="form-check form-switch mt-1">
                  <input class="form-check-input" type="checkbox" role="switch" id="motherIsAlive" checked>
                  <label class="form-check-label" for="motherIsAlive">Vive</label>
                </div>
                <div class="form-floating d-none mt-1">
                  <input type="number" class="form-control" id="motherYearOfDeath" disabled>
                  <label for="motherYearOfDeath">A&ntilde;o de fallecimiento</label>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="col-lg-3">
        <div class="small">
          <p>Dejanos tu email, Whatsapp o Istagram y nos pondremos en contacto caso de encontrar personas relacionadas a tu b&uacute;squeda en un futuro.</p>
        </div>
        <div class="row justify-content-center align-items-center mt-2 mb-3">
          <div class="col-auto form-floating">
            <input type="text" class="form-control" id="individualContact" size="30">
            <label for="individualContact">Email, Whatsapp o Instagram</label>
          </div>
          <div class="col-auto">
            <button type="button" class="btn btn-dark" id="searchBtn">Buscar</button>
          </div>
        </div>
        <div class="card border-dark" id="searchResultCard">
          <div class="card-header text-bg-dark">Resultado de la b&uacute;squeda</div>
          <div class="card-body overflow-auto">

          </div>
        </div>
      </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/js/bootstrap.bundle.min.js" integrity="sha384-kenU1KFdBIe4zVF0s0G1M5b4hcpxyD9F7jL+jjXkk+Q2h455rYXK/7HAuoJl+0I4" crossorigin="anonymous"></script>
    <script src="https://cdn.jsdelivr.net/npm/jquery@3.6.3/dist/jquery.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/jquery.json-viewer@1.5.0/json-viewer/jquery.json-viewer.js"></script>
    <script src="js/main.js"></script>
  </body>

</html>
