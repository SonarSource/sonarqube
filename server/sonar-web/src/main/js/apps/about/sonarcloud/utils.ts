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

export interface FeaturedProject {
  key: string;
  avatarUrl: string | null;
  organizationKey: string;
  organizationName: string;
  name: string;
  bugs: number;
  codeSmells: number;
  coverage: number;
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

export function requestHomepageData(): Promise<HomepageData> {
  return fetch('/json/homepage.json').then(response => response.json());
}
