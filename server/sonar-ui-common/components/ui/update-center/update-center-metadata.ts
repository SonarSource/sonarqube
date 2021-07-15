/*
 * Sonar UI Common
 * Copyright (C) 2019-2020 SonarSource SA
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

export interface MetaDataInformation {
  category?: string;
  isSonarSourceCommercial?: boolean;
  issueTrackerURL?: string;
  key?: string;
  license?: string;
  name: string;
  organization?: {
    name: string;
    url?: string;
  };
  sourcesURL?: string;
  versions?: MetaDataVersionInformation[];
}

export interface MetaDataVersionInformation {
  archived?: boolean;
  changeLogUrl?: string;
  compatibility?: string;
  date?: string;
  description?: string;
  downloadURL?: string | AdvancedDownloadUrl[];
  version: string;
}

export interface AdvancedDownloadUrl {
  label?: string;
  url: string;
}
