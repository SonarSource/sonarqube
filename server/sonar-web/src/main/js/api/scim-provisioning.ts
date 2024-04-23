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
import { getJSON, post } from '../helpers/request';
import { throwGlobalError } from '../sonar-aligned/helpers/error';

export function fetchIsScimEnabled(): Promise<boolean> {
  return getJSON('/api/scim_management/status')
    .then((r) => r.enabled)
    .catch(throwGlobalError);
}

export function activateScim(): Promise<void> {
  return post('/api/scim_management/enable').catch(throwGlobalError);
}

export function deactivateScim(): Promise<void> {
  return post('/api/scim_management/disable').catch(throwGlobalError);
}
