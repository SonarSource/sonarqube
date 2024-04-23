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
import { getJSON, post, postJSON } from '../helpers/request';
import { throwGlobalError } from '../sonar-aligned/helpers/error';
import { Application, ApplicationPeriod } from '../types/application';
import { Visibility } from '../types/component';

export function getApplicationLeak(
  application: string,
  branch?: string,
): Promise<ApplicationPeriod[]> {
  return getJSON('/api/applications/show_leak', { application, branch }).then(
    (r) => r.leaks,
    throwGlobalError,
  );
}

export function getApplicationDetails(application: string, branch?: string): Promise<Application> {
  return getJSON('/api/applications/show', { application, branch }).then(
    (r) => r.application,
    throwGlobalError,
  );
}

export function createApplication(
  name: string,
  description: string,
  key: string | undefined,
  visibility: string,
): Promise<{
  application: {
    description?: string;
    key: string;
    name: string;
    visibility: Visibility;
  };
}> {
  return postJSON('/api/applications/create', { description, key, name, visibility }).catch(
    throwGlobalError,
  );
}

export function deleteApplication(application: string) {
  return post('/api/applications/delete', { application }).catch(throwGlobalError);
}
