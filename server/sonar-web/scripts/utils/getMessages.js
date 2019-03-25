/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
const fs = require('fs');
const path = require('path');
const { promisify } = require('util');

const readFileAsync = promisify(fs.readFile);

const filename = '../../../../sonar-core/src/main/resources/org/sonar/l10n/core.properties';

const extensionsFilenames = [
  '../../../../private/core-extension-billing/src/main/resources/org/sonar/l10n/billing.properties',
  '../../../../private/core-extension-governance/src/main/resources/org/sonar/l10n/governance.properties',
  '../../../../private/core-extension-license/src/main/resources/org/sonar/l10n/license.properties',
  '../../../../private/core-extension-developer-server/src/main/resources/org/sonar/l10n/developer-server.properties'
];

function getFileMessage(filename) {
  return readFileAsync(path.resolve(__dirname, filename), 'utf-8').then(
    content => {
      const messages = {};
      const lines = content.split('\n');
      lines.forEach(line => {
        const parts = line.split('=');
        if (parts.length > 1) {
          messages[parts[0]] = parts.slice(1).join('=');
        }
      });
      return messages;
    },
    () => ({})
  );
}

function getMessages(l10nExtensions) {
  return Promise.all(
    [filename, ...extensionsFilenames].map(filename => getFileMessage(filename))
  ).then(filesMessages => filesMessages.reduce((acc, messages) => ({ ...acc, ...messages }), {}));
}

module.exports = getMessages;
