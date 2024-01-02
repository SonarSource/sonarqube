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
const fs = require('fs-extra');
const path = require('path');
const paths = require('./paths');
const htmlTemplate = require('./indexHtmlTemplate');

function extractHash(filename) {
  const regexp = /out([\w]+)\./;
  const result = filename.match(regexp);
  if (!result) {
    throw Error('filename format error: could not extract hash');
  }

  return result[1];
}

/*
 * This plugin generates a index.html file from the template,
 * injecting the right hash values to the imported js and css files
 */
module.exports = () => ({
  name: 'html-plugin',
  setup({ onEnd }) {
    onEnd(result => {
      const files = result.metafile.outputs;

      let cssHash;
      let jsHash;
      for (const filename in files) {
        if (filename.endsWith('css')) {
          cssHash = extractHash(filename);
        } else if (filename.endsWith('js')) {
          jsHash = extractHash(filename);
        }
      }

      const htmlContents = htmlTemplate(cssHash, jsHash);

      fs.writeFile(path.join(paths.appBuild, 'index.html'), htmlContents);
    });
  }
});
