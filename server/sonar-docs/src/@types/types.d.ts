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

export type Dict<T> = { [key: string]: T };

export interface DocVersion {
  current: boolean;
  value: string;
}

export type DocNavigationItem = string | DocsNavigationBlock | DocsNavigationExternalLink;

export interface DocsNavigationBlock {
  title: string;
  children: DocNavigationItem[];
}

export interface DocsNavigationExternalLink {
  title: string;
  url: string;
}

export type PluginMetaDataInfo = {
  category?: string;
  isSonarSourceCommercial: boolean;
  issueTrackerURL?: string;
  key?: string;
  license?: string;
  name: string;
  organization?: {
    name: string;
    url?: string;
  };
  sourcesURL?: string;
  versions?: PluginVersionInfo[];
};

export type PluginVersionInfo = {
  archived?: boolean;
  changeLogUrl?: string;
  compatibility?: string;
  date?: string;
  description?: string;
  downloadURL?: string;
  version: string;
};

export interface SearchResult {
  exactMatch?: boolean;
  highlights: { [field: string]: [number, number][] };
  longestTerm: string;
  page: {
    id: string;
    text: string;
    title: string;
    url: string;
  };
  query: string;
}
