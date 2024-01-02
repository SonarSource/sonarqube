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
const VERSION_PARSER = /^(\d+\.\d+).*$/;

export function getUrlForDoc(version: string, to: string) {
  const versionPrefix = VERSION_PARSER.exec(version);
  const isSnapshot = version.indexOf('SNAPSHOT') !== -1;
  const docPrefix =
    versionPrefix && versionPrefix.length === 2 && !isSnapshot ? versionPrefix[1] : 'latest';

  return `https://docs.sonarqube.org/${docPrefix}${to}`;
}
