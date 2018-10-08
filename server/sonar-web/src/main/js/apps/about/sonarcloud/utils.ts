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

export interface Project {
  key: string;
  measures: { [key: string]: string };
  name: string;
  organization?: { avatar: string; name: string };
}

// TODO Get this from an external source
const PROJECTS = [
  {
    key: 'org.hibernate.search.v6poc:hibernate-search-parent',
    name: 'Hibernate Search 6 POC Parent POM',
    measures: {
      bugs: '39',
      reliability_rating: '1',
      vulnerabilities: '5',
      security_rating: '2',
      code_smells: '1',
      sqale_rating: '1.0',
      coverage: '89.9',
      duplicated_lines_density: '1.1',
      ncloc: '336726',
      ncloc_language_distribution: 'java=175123;js=26382'
    },
    organization: {
      avatar: '',
      name: 'Hibernate'
    }
  },
  {
    key: 'com.github.sdorra:web-resources',
    name: 'Web Resources',
    measures: {
      bugs: '39',
      reliability_rating: '1',
      vulnerabilities: '5',
      security_rating: '2',
      code_smells: '1',
      sqale_rating: '1.0',
      coverage: '89.9',
      duplicated_lines_density: '1.1',
      ncloc: '336726',
      ncloc_language_distribution: 'java=175123;js=26382'
    },
    organization: {
      avatar: '',
      name: 'sdorra-github'
    }
  },
  {
    key: 'vyos:vyos-1x',
    name: 'vyos-1x',
    measures: {
      bugs: '39',
      reliability_rating: '1',
      vulnerabilities: '5',
      security_rating: '2',
      code_smells: '1',
      sqale_rating: '1.0',
      coverage: '89.9',
      duplicated_lines_density: '1.1',
      ncloc: '336726',
      ncloc_language_distribution: 'java=175123;js=26382'
    },
    organization: {
      avatar: '',
      name: 'vyos'
    }
  },
  {
    key: 'sonarlint-visualstudio',
    name: 'SonarLint for Visual Studio',
    measures: {
      bugs: '39',
      reliability_rating: '1',
      vulnerabilities: '5',
      security_rating: '2',
      code_smells: '1',
      sqale_rating: '1.0',
      coverage: '89.9',
      duplicated_lines_density: '1.1',
      ncloc: '336726',
      ncloc_language_distribution: 'java=175123;js=26382'
    },
    organization: {
      avatar: '',
      name: 'sonarsource'
    }
  },
  {
    key: 'sample-1',
    name: 'Dummy project',
    measures: {
      bugs: '39',
      reliability_rating: '1',
      vulnerabilities: '5',
      security_rating: '2',
      code_smells: '1',
      sqale_rating: '1.0',
      coverage: '89.9',
      duplicated_lines_density: '1.1',
      ncloc: '336726',
      ncloc_language_distribution: 'java=175123;js=26382'
    },
    organization: {
      avatar: '',
      name: 'sonarsource'
    }
  }
];

export function requestFeaturedProjects(): Promise<Project[]> {
  return Promise.resolve(PROJECTS);
}
