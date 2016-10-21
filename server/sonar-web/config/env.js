/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
// Grab NODE_ENV and REACT_APP_* environment variables and prepare them to be
// injected into the application via DefinePlugin in Webpack configuration.

var REACT_APP = /^REACT_APP_/i;

function getClientEnvironment (publicUrl) {
  return Object
      .keys(process.env)
      .filter(key => REACT_APP.test(key))
      .reduce((env, key) => {
        env['process.env.' + key] = JSON.stringify(process.env[key]);
        return env;
      }, {
        // Useful for determining whether we’re running in production mode.
        // Most importantly, it switches React into the correct mode.
        'process.env.NODE_ENV': JSON.stringify(
            process.env.NODE_ENV || 'development'
        ),
        // Useful for resolving the correct path to static assets in `public`.
        // For example, <img src={process.env.PUBLIC_URL + '/img/logo.png'} />.
        // This should only be used as an escape hatch. Normally you would put
        // images into the `src` and `import` them in code to get their paths.
        'process.env.PUBLIC_URL': JSON.stringify(publicUrl)
      });
}

module.exports = getClientEnvironment;

