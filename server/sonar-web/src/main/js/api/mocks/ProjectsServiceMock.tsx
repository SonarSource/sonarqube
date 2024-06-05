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
import { MeasuresForProjects } from '../../types/measures';
import { Dict } from '../../types/types';
import { ComponentRaw, Facet, getScannableProjects, searchProjects } from '../components';
import { addFavorite } from '../favorites';
import { getMeasuresForProjects } from '../measures';
import { mockFacets, mockProjectMeasures, mockProjects } from './data/projects';

export class ProjectsServiceMock {
  projects: ComponentRaw[];
  measuresByProjectByMetric: Dict<Dict<MeasuresForProjects>> = {};
  facets: Facet[];

  constructor() {
    const { projects, measures, facets } = this.initData();

    this.projects = projects;
    this.measuresByProjectByMetric = measures;
    this.facets = facets;

    jest.mocked(searchProjects).mockImplementation(this.handleSearchProjects);
    jest.mocked(getMeasuresForProjects).mockImplementation(this.handleGetMeasuresForProjects);
    jest.mocked(getScannableProjects).mockImplementation(this.handleGetScannableProjects);
    jest.mocked(addFavorite).mockImplementation(this.handleAddFavorite);
  }

  nclocSort = (a: ComponentRaw, b: ComponentRaw) => {
    const { value: va = '0' } = (this.measuresByProjectByMetric[a.key] ?? { ncloc: { value: 0 } })
      .ncloc;
    const { value: vb = '0' } = (this.measuresByProjectByMetric[b.key] ?? { ncloc: { value: 0 } })
      .ncloc;

    return parseInt(va, 10) - parseInt(vb, 10);
  };

  handleSearchProjects = ({
    ps,
    facets = '',
    filter = '',
    s,
  }: {
    facets: string;
    filter: string;
    ps: number;
    s?: string;
  }) => {
    /*
     * Parse params
     */
    const facetKeys = facets.split(',');

    const filters = filter.split('and');
    const favorite = filters.includes('isFavorite');

    /*
     * Build response
     */
    const results = this.projects.filter((p) => {
      return !favorite || p.isFavorite === favorite;
    });

    const sorted = s === 'ncloc' ? results.sort(this.nclocSort) : results;

    return Promise.resolve({
      components: sorted.slice(0, ps),
      facets: this.facets.filter(({ property }) => {
        return facetKeys.includes(property);
      }),
      paging: {
        pageIndex: 1,
        pageSize: ps,
        total: sorted.length,
      },
    });
  };

  handleGetMeasuresForProjects = (projectKeys: string[] = [], metricKeys: string[] = []) => {
    const measures = projectKeys.flatMap((projectKey) => {
      const projectMeasures = this.measuresByProjectByMetric[projectKey] ?? {};

      return metricKeys.map((key) => projectMeasures[key]).filter((v) => v !== undefined);
    });

    return Promise.resolve(measures);
  };

  handleGetScannableProjects = () => {
    return Promise.resolve({
      projects: [],
    });
  };

  handleAddFavorite = (componentKey: string) => {
    const project = this.projects.find(({ key }) => componentKey === key);

    if (project) {
      project.isFavorite = true;
      return Promise.resolve();
    }

    return Promise.reject('project not found');
  };

  initData = () => {
    const projects = mockProjects();
    const measures = mockProjectMeasures();
    const facets = mockFacets();

    return { projects, measures, facets };
  };

  reset = () => {
    this.initData();
  };
}
