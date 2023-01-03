/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { ProjectKeyValidationResult } from '../types/component';
import { PROJECT_KEY_MAX_LEN } from './constants';

export function validateProjectKey(projectKey: string): ProjectKeyValidationResult {
  // This is the regex used on the backend:
  //   [\p{Alnum}\-_.:]*[\p{Alpha}\-_.:]+[\p{Alnum}\-_.:]*
  // See sonar-core/src/main/java/org/sonar/core/component/ComponentKeys.java
  const regex = /^[\w\-.:]*[a-z\-_.:]+[\w\-.:]*$/i;
  if (projectKey.length === 0) {
    return ProjectKeyValidationResult.Empty;
  } else if (projectKey.length > PROJECT_KEY_MAX_LEN) {
    return ProjectKeyValidationResult.TooLong;
  } else if (regex.test(projectKey)) {
    return ProjectKeyValidationResult.Valid;
  } else {
    return /^[0-9]+$/.test(projectKey)
      ? ProjectKeyValidationResult.OnlyDigits
      : ProjectKeyValidationResult.InvalidChar;
  }
}
