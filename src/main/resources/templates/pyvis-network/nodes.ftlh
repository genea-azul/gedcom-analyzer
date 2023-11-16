<!DOCTYPE html>
<html lang="es">
  <head>
    <title>Genea Azul - Buscador de personas</title>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/css/bootstrap.min.css" integrity="sha384-rbsA2VBKQhggwzxH7pPCaAqO46MgnOM80zW1RWuH61DGLwZJEdK2Kadq2F9CUG65" crossorigin="anonymous" />
    <style>
      body {
          height: 100vh;
      }
      .container-fluid {
          margin: 0px !important;
          padding: 0px !important;
      }
      embed {
          width: 100%;
          min-height: 100vh;
      }
    </style>
  </head>

  <body class="container-fluid">
    <div class="min-vh-100 d-flex flex-column">
      <#if obfuscateLiving == true>
        <embed type="text/html" src="/family-tree/${personUuid}/network" />
      <#else>
        <embed type="text/html" src="/family-tree/${personUuid}/network?f=0" />
      </#if>
    </div>
  </body>

</html>
