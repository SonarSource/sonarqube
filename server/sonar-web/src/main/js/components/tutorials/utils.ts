/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { AlmSettingsInstance, ProjectAlmBindingResponse } from '../../types/alm-settings';

export function quote(os: string): (s: string) => string {
  return os === 'win' ? (s: string) => `"${s}"` : (s: string) => s;
}

export function mavenPomSnippet(key: string) {
  return `<properties>
  <sonar.projectKey>${key}</sonar.projectKey>
</properties>`;
}

export function buildGradleSnippet(key: string) {
  return `plugins {
  id "org.sonarqube" version "3.2.0"
}

sonarqube {
  properties {
    property "sonar.projectKey", "${key}"
  }
}`;
}

export function getUniqueTokenName(tokens: T.UserToken[], initialTokenName = '') {
  const hasToken = (name: string) => tokens.find(token => token.name === name) !== undefined;

  if (!hasToken(initialTokenName)) {
    return initialTokenName;
  }

  let i = 1;
  while (hasToken(`${initialTokenName} ${i}`)) {
    i++;
  }
  return `${initialTokenName} ${i}`;
}

export function buildGithubLink(
  almBinding: AlmSettingsInstance,
  projectBinding: ProjectAlmBindingResponse
) {
  if (almBinding.url === undefined) {
    return null;
  }

  // strip the api path:
  const urlRoot = almBinding.url
    .replace(/\/api\/v\d+\/?$/, '') // GH Enterprise
    .replace(/^https?:\/\/api\.github\.com/, 'https://github.com') // GH.com
    .replace(/\/$/, '');

  return `${urlRoot}/${projectBinding.repository}`;
}
