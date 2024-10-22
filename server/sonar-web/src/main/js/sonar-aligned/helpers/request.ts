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

import { RequestData, get, parseJSON } from '../../helpers/request';

interface CustomHeader {
  accept?: string;
  apiVersion?: string;
  isJSON?: boolean;
}

interface RequestOptions {
  bypassRedirect?: boolean;
  customHeaders?: CustomHeader; // used only in SonarCloud
  isExternal?: boolean; // used only in SonarCloud
  useQueryParams?: boolean; // used only in SonarCloud
}

/**
 * Shortcut to do a GET request and return response json
 */
export function getJSON(
  url: string,
  data?: RequestData,
  options: RequestOptions = {},
): Promise<any> {
  return get(url, data, options.bypassRedirect).then(parseJSON);
}
