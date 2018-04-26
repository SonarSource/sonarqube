/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
export interface DocumentationEntry {
  title: string;
  order: string;
  name: string;
  relativeName: string;
  children: Array<DocumentationEntry>;
}

export function activeOrChildrenActive(root: string, entry: DocumentationEntry) {
  return root.indexOf(getEntryRoot(entry.relativeName)) === 0;
}

export function getEntryRoot(name: string) {
  if (name.endsWith('index')) {
    return name
      .split('/')
      .slice(0, -1)
      .join('/');
  }
  return name;
}

export function getEntryChildren(
  entries: Array<DocumentationEntry>,
  root?: string
): Array<DocumentationEntry> {
  return entries.filter(entry => {
    const parts = entry.relativeName.split('/');
    const depth = root ? root.split('/').length : 0;
    return (
      (!root || entry.relativeName.indexOf(root) === 0) &&
      ((parts.length === depth + 1 && parts[depth] !== 'index') || parts[depth + 1] === 'index')
    );
  });
}
