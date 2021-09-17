/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
/* eslint-disable no-console */
function getCustomProperties() {
  const customProperties = {};
  const parseCustomProperties = theme => {
    Object.keys(theme).forEach(key => {
      if (typeof theme[key] === 'object') {
        parseCustomProperties(theme[key]);
      } else if (typeof theme[key] === 'string') {
        if (!customProperties[`--${key}`]) {
          customProperties[`--${key}`] = theme[key];
        } else {
          console.error(
            `Custom CSS property "${key}" already exists with value "${
              customProperties[`--${key}`]
            }".`
          );
          process.exit(1);
        }
      }
    });
  };
  parseCustomProperties(require('../src/main/js/app/theme'));

  return customProperties;
}

module.exports = {
  getCustomProperties
};
