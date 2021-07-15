/*
 * Sonar UI Common
 * Copyright (C) 2019-2020 SonarSource SA
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

import { MetaDataInformation, MetaDataVersionInformation } from '../update-center-metadata';

export function mockMetaDataVersionInformation(
  overrides?: Partial<MetaDataVersionInformation>
): MetaDataVersionInformation {
  return {
    version: '5.13',
    date: '2019-05-31',
    compatibility: '6.7',
    archived: false,
    downloadURL: 'https://example.com/sonar-java-plugin-5.13.0.18197.jar',
    changeLogUrl: 'https://example.com/sonar-java-plugin/release',
    ...overrides,
  };
}

export function mockMetaDataInformation(
  overrides?: Partial<MetaDataInformation>
): MetaDataInformation {
  return {
    name: 'SonarJava',
    key: 'java',
    isSonarSourceCommercial: true,
    organization: {
      name: 'SonarSource',
      url: 'http://www.sonarsource.com/',
    },
    category: 'Languages',
    license: 'SonarSource',
    issueTrackerURL: 'https://jira.sonarsource.com/browse/SONARJAVA',
    sourcesURL: 'https://github.com/SonarSource/sonar-java',
    versions: [
      mockMetaDataVersionInformation({ version: '2.0' }),
      mockMetaDataVersionInformation({ version: '1.0', archived: true }),
    ],
    ...overrides,
  };
}
