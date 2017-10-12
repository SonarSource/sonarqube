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
const fs = require('fs');
const path = require('path');
const { promisify } = require('util');

const readFileAsync = promisify(fs.readFile);

const filename = path.resolve(
  __dirname,
  '../../../../sonar-core/src/main/resources/org/sonar/l10n/core.properties'
);

function getMessages() {
  return readFileAsync(filename, 'utf-8').then(content => {
    const messages = {};
    const lines = content.split('\n');
    lines.forEach(line => {
      const parts = line.split('=');
      if (parts.length > 1) {
        messages[parts[0]] = parts.slice(1).join('=');
      }
    });
    return messages;
  });
}

module.exports = getMessages;
