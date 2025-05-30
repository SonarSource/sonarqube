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

import { Dict } from './types';

export enum SecurityStandard {
  OWASP_TOP10_2021 = 'owaspTop10-2021',
  OWASP_TOP10 = 'owaspTop10',
  SONARSOURCE = 'sonarsourceSecurity',
  CWE = 'cwe',
  CVSS = 'cvss',
  PCI_DSS_3_2 = 'pciDss-3.2',
  PCI_DSS_4_0 = 'pciDss-4.0',
  OWASP_ASVS_4_0 = 'owaspAsvs-4.0',
  STIG_ASD_V5R3 = 'stig-ASD_V5R3',
  CASA = 'casa',
}

export type StandardType = SecurityStandard;

export type Standards = {
  [key in StandardType]: Dict<{ description?: string; level?: string; title: string }>;
};
