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

import { OpenAPIV3 } from 'openapi-types';
import { throwGlobalError } from '~sonar-aligned/helpers/error';
import { getJSON } from '~sonar-aligned/helpers/request';
import { WebApi } from '../types/types';

interface RawDomain {
  actions: WebApi.Action[];
  deprecatedSince?: string;
  description: string;
  internal: boolean;
  path: string;
  since?: string;
}

export function fetchWebApi(showInternal = true): Promise<RawDomain[]> {
  return getJSON('/api/webservices/list', { include_internals: showInternal })
    .then((r) => r.webServices)
    .catch(throwGlobalError);
}

export function fetchResponseExample(domain: string, action: string): Promise<WebApi.Example> {
  return getJSON('/api/webservices/response_example', { controller: domain, action }).catch(
    throwGlobalError,
  );
}

export function fetchOpenAPI(): Promise<OpenAPIV3.Document> {
  return getJSON('/api/v2/api-docs').catch(throwGlobalError);
}
