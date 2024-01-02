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
import { Standards } from '../types/security';

export function getStandards(): Promise<Standards> {
  return import('./standards.json').then((x) => x.default);
}

export function renderCWECategory(standards: Standards, category: string): string {
  const record = standards.cwe[category];
  if (!record) {
    return `CWE-${category}`;
  } else if (category === 'unknown') {
    return record.title;
  }
  return `CWE-${category} - ${record.title}`;
}

export function renderOwaspTop10Category(
  standards: Standards,
  category: string,
  withPrefix = false,
): string {
  return renderOwaspCategory('owaspTop10', standards, category, withPrefix);
}

export function renderOwaspTop102021Category(
  standards: Standards,
  category: string,
  withPrefix = false,
): string {
  return renderOwaspCategory('owaspTop10-2021', standards, category, withPrefix);
}

function renderOwaspCategory(
  type: 'owaspTop10' | 'owaspTop10-2021',
  standards: Standards,
  category: string,
  withPrefix: boolean,
) {
  const record = standards[type][category];
  if (!record) {
    return addPrefix(category.toUpperCase(), 'OWASP', withPrefix);
  }
  return addPrefix(`${category.toUpperCase()} - ${record.title}`, 'OWASP', withPrefix);
}

export function renderSonarSourceSecurityCategory(
  standards: Standards,
  category: string,
  withPrefix = false,
): string {
  const record = standards.sonarsourceSecurity[category];
  if (!record) {
    return addPrefix(category.toUpperCase(), 'SONAR', withPrefix);
  } else if (category === 'others') {
    return record.title;
  }
  return addPrefix(record.title, 'SONAR', withPrefix);
}

export function renderPciDss32Category(standards: Standards, category: string): string {
  const record = standards['pciDss-3.2'][category];
  if (!record) {
    return category;
  }
  return `${category} - ${record.title}`;
}

export function renderPciDss40Category(standards: Standards, category: string): string {
  const record = standards['pciDss-4.0'][category];
  if (!record) {
    return category;
  }
  return `${category} - ${record.title}`;
}

export function renderOwaspAsvs40Category(standards: Standards, category: string): string {
  const record = standards['owaspAsvs-4.0'][category];
  if (!record) {
    return category;
  }
  const levelInfo = record.level ? ` (Level ${record.level})` : '';
  return `${category} - ${record.title}${levelInfo}`;
}

function addPrefix(title: string, prefix: string, withPrefix: boolean) {
  return withPrefix ? `${prefix} ${title}` : title;
}
