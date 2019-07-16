/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { getJSON } from 'sonar-ui-common/helpers/request';
import throwGlobalError from '../app/utils/throwGlobalError';

export interface License {
  contactEmail: string;
  edition: string;
  expiresAt: string;
  isExpired: boolean;
  isOfficialDistribution: boolean;
  isSupported: boolean;
  isValidEdition: boolean;
  isValidServerId: boolean;
  loc: number;
  maxLoc: number;
  plugins: string[];
  remainingLocThreshold: number;
  serverId: string;
  type: string;
}

export function isValidLicense(): Promise<{ isValidLicense: boolean }> {
  return getJSON('/api/editions/is_valid_license');
}

export function showLicense(): Promise<License> {
  return getJSON('/api/editions/show_license').catch((response: Response) => {
    if (response && response.status === 404) {
      return Promise.resolve(undefined);
    }
    return throwGlobalError(response);
  });
}
