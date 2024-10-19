/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
module.exports = (cssHash, jsHash) => `
<!DOCTYPE html>
<html lang="en">

<head>
    <meta http-equiv="content-type" content="text/html; charset=UTF-8" charset="UTF-8" />
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <link rel="apple-touch-icon" href="%WEB_CONTEXT%/apple-touch-icon.png">
    <link rel="apple-touch-icon" sizes="57x57" href="%WEB_CONTEXT%/apple-touch-icon-57x57.png">
    <link rel="apple-touch-icon" sizes="60x60" href="%WEB_CONTEXT%/apple-touch-icon-60x60.png">
    <link rel="apple-touch-icon" sizes="72x72" href="%WEB_CONTEXT%/apple-touch-icon-72x72.png">
    <link rel="apple-touch-icon" sizes="76x76" href="%WEB_CONTEXT%/apple-touch-icon-76x76.png">
    <link rel="apple-touch-icon" sizes="114x114" href="%WEB_CONTEXT%/apple-touch-icon-114x114.png">
    <link rel="apple-touch-icon" sizes="120x120" href="%WEB_CONTEXT%/apple-touch-icon-120x120.png">
    <link rel="apple-touch-icon" sizes="144x144" href="%WEB_CONTEXT%/apple-touch-icon-144x144.png">
    <link rel="apple-touch-icon" sizes="152x152" href="%WEB_CONTEXT%/apple-touch-icon-152x152.png">
    <link rel="apple-touch-icon" sizes="180x180" href="%WEB_CONTEXT%/apple-touch-icon-180x180.png">
    <link rel="icon" type="image/x-icon" href="%WEB_CONTEXT%/favicon.ico">
    <meta name="application-name" content="SonarQube" />
    <meta name="msapplication-TileColor" content="#FFFFFF" />
    <meta name="msapplication-TileImage" content="%WEB_CONTEXT%/mstile-512x512.png" />
    <title>%INSTANCE%</title>

    <!-- Google Tag Manager -->
    <script>
      if (window.location.hostname.includes('codescan.io')) {
        (function(w,d,s,l,i){w[l]=w[l]||[];w[l].push({'gtm.start':
        new Date().getTime(),event:'gtm.js'});var f=d.getElementsByTagName(s)[0],
        j=d.createElement(s),dl=l!='dataLayer'?'&l='+l:'';j.async=true;j.src=
        'https://www.googletagmanager.com/gtm.js?id='+i+dl;f.parentNode.insertBefore(j,f);
        })(window,document,'script','dataLayer','GTM-TGN67LR');
      }
    </script>
    <!-- End Google Tag Manager -->

    <!-- Pendo.io -->
    <script>
      if (window.location.hostname.includes('codescan.io') || window.location.hostname.includes('autorabit.com')) {
        (function(p,e,n,d,o){var v,w,x,y,z;o=p[d]=p[d]||{};o._q=o._q||[];
        v=['initialize','identify','updateOptions','pageLoad','track'];for(w=0,x=v.length;w<x;++w)(function(m){
          o[m]=o[m]||function(){o._q[m===v[0]?'unshift':'push']([m].concat([].slice.call(arguments,0)));};})(v[w]);
        y=e.createElement(n);y.async=!0;y.src='https://cdn.pendo.io/agent/static/6c4b3816-7b04-42d8-6025-bd01283d95a3/pendo.js';
        z=e.getElementsByTagName(n)[0];z.parentNode.insertBefore(y,z);})(window,document,'script','pendo');
      }
    </script>
    <!-- End Pendo.io -->

    <link rel="stylesheet" href="%WEB_CONTEXT%/js/out${cssHash}.css" />
</head>

<body>
    <!-- Google Tag Manager (noscript) -->
    <noscript><iframe src="https://www.googletagmanager.com/ns.html?id=GTM-TGN67LR"
      height="0" width="0" style="display:none;visibility:hidden"></iframe></noscript>
    <!-- End Google Tag Manager (noscript) -->

    <div id="content" data-base-url="%WEB_CONTEXT%" data-server-status="%SERVER_STATUS%" data-instance="%INSTANCE%" data-official="%OFFICIAL%">
        <div class="global-loading">
            <i class="global-loading-spinner"></i>
            <span aria-live="polite" class="global-loading-text">Loading...</span>
        </div>
    </div>

    <script type="module" src="%WEB_CONTEXT%/js/out${jsHash}.js"></script>
</body>

</html>
`;
