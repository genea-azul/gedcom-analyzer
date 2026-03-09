<!DOCTYPE html>
<html lang="es">
  <head>
    <title>&Aacute;rbol geneal&oacute;gico de ${personDisplayName} - Genea Azul</title>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/css/bootstrap.min.css" integrity="sha384-rbsA2VBKQhggwzxH7pPCaAqO46MgnOM80zW1RWuH61DGLwZJEdK2Kadq2F9CUG65" crossorigin="anonymous" />
    <style>
      body {
          height: 100vh;
          margin: 0;
      }
      .container-fluid {
          margin: 0 !important;
          padding: 0 !important;
      }
      embed {
          width: 100%;
          min-height: 100vh;
          display: block;
      }
    </style>
  </head>

  <body class="container-fluid">
    <div class="min-vh-100 d-flex flex-column">
      <#if obfuscateLiving == true>
        <embed type="text/html" src="/family-tree/${personUuid}/network" title="&Aacute;rbol geneal&oacute;gico de ${personDisplayName}" aria-label="&Aacute;rbol geneal&oacute;gico" />
      <#else>
        <embed type="text/html" src="/family-tree/${personUuid}/network?f=0" title="&Aacute;rbol geneal&oacute;gico de ${personDisplayName}" aria-label="&Aacute;rbol geneal&oacute;gico" />
      </#if>
    </div>
  </body>

</html>
