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
import { getJSON } from '../../../helpers/request';

export interface FeaturedProject {
  key: string;
  avatarUrl: string | null;
  organizationKey: string;
  organizationName: string;
  name: string;
  bugs: number;
  codeSmells: number;
  coverage?: number;
  duplications: number;
  gateStatus: string;
  languages: string[];
  maintainabilityRating: number;
  ncloc: number;
  reliabilityRating: number;
  securityRating: number;
  vulnerabilities: number;
}

export interface HomepageData {
  generatedAt: string;
  publicProjects: number;
  publicLoc: number;
  rules: number;
  featuredProjects: FeaturedProject[];
  newPullRequests7d: number;
}

export const LANGUAGES = [
  { name: 'Java', file: 'java.svg', width: 65 },
  { name: 'JavaScript', file: 'js.svg', width: 60 },
  { name: 'TypeScript', file: 'ts.svg', width: 100 },
  { name: 'C#', file: 'csharp.svg', width: 60 },
  { name: 'Python', file: 'python.svg', width: 65 },
  { name: 'C++', file: 'c-c-plus-plus.svg', width: 53 },
  { name: 'Go', file: 'go.svg', width: 91 },
  { name: 'Kotlin', file: 'kotlin.svg', width: 42 },
  { name: 'Ruby', file: 'ruby.svg', width: 43 },
  { name: 'Swift', file: 'swift.svg', width: 64 },
  { name: 'ABAP', file: 'abap.svg', width: 62 },
  { name: 'Apex', file: 'apex.svg', width: 62 },
  { name: 'Flex', file: 'flex.png', width: 85 },
  { name: 'CSS', file: 'css.svg', width: 40 },
  { name: 'HTML', file: 'html5.svg', width: 40 },
  { name: 'Objective-C', file: 'obj-c.svg', width: 63 },
  { name: 'PHP', file: 'php.svg', width: 57 },
  { name: 'Scala', file: 'scala.svg', width: 29 },
  { name: 'T-SQL', file: 't-sql.svg', width: 53 },
  { name: 'PL/SQL', file: 'pl-sql.svg', width: 65 },
  { name: 'VB', file: 'vb.svg', width: 55 },
  { name: 'XML', file: 'xml.svg', width: 67 },
  { name: 'COBOL', file: 'cobol.svg', width: 65 }
];

export function requestHomepageData(url: string): Promise<HomepageData> {
  return getJSON(url);
}
