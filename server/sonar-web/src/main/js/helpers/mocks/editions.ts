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
import { License } from '../../types/editions';

export function mockLicense(override?: Partial<License>) {
  return {
    contactEmail: 'contact@sonarsource.com',
    edition: 'Developer Edition',
    expiresAt: '2018-05-18',
    isExpired: false,
    isValidEdition: true,
    isValidServerId: true,
    isOfficialDistribution: true,
    isSupported: false,
    canActivateGracePeriod: false,
    loc: 120085,
    maxLoc: 500000,
    plugins: ['Branches', 'PLI language'],
    remainingLocThreshold: 490000,
    serverId: 'AU-TpxcA-iU5OvuD2FL0',
    type: 'production',
    ...override,
  };
}
