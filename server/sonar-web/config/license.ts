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

import { Dependency } from 'rollup-plugin-license';

// To be always synced with https://saas-eu.whitesourcesoftware.com/Wss/WSS.html#!policyDetails;policy=3131;org=318388
// Below is the list of approved front-end licenses extracted from Mend
export const ALLOWED_LICENSES = [
  '(MPL-2.0 OR Apache-2.0)', // Multiple licenses. Added specifically for dompurify as we have ignored this in Mend
  '0BSD',
  'Apache-2.0',
  'BSD-2-Clause',
  'BSD-3-Clause',
  'ISC',
  'LGPL-3.0',
  'MIT',
];

// Just for Sprig currently, it has an Apache-2 license that isn't correctly parsed by the plugin
export const ALLOWED_LICENSE_TEXT = ['http://www.apache.org/licenses/LICENSE-2.0'];

// Generates license information with necessary details.
// A package which has a valid license that the plugin is unable read will default to MIT
export const generateLicenseText = (dependency: Dependency) => {
  const { author, homepage, license, licenseText, name, repository, version } = dependency;
  const lines: string[] = [];

  lines.push(`Name: ${name}`);
  lines.push(`Version: ${version}`);

  if (license) {
    lines.push(`License: ${license}`);
  }

  if (typeof repository === 'string') {
    lines.push(`Repository: ${repository}`);
  } else if (repository?.url) {
    lines.push(`Repository: ${repository.url}`);
  } else if (homepage) {
    lines.push(`Homepage: ${homepage}`);
  }

  if (author) {
    lines.push(`Author: ${author.text()}`);
  }

  if (licenseText) {
    lines.push(`License Copyright:`);
    lines.push(`${licenseText}`);
  }

  return lines.join('\n');
};
