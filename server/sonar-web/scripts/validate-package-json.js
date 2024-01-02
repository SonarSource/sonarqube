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
const { dependencies, devDependencies } = require('../package.json');

const dependenciesArray = Object.entries(dependencies);
const devDependenciesArray = Object.entries(devDependencies);

const violatingDependencies = [...dependenciesArray, ...devDependenciesArray].filter(
  ([id, version]) => !/^\d+\.\d+\.\d+$/.test(version)
);

if (violatingDependencies.length > 0) {
  throw new Error(
    `Following dependencies must be locked to an exact version:
${violatingDependencies.map(([id, version]) => ` - "${id}": "${version}"`).join('\n')}
`
  );
}
