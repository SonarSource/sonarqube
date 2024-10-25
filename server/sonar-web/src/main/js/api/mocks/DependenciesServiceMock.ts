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

import { cloneDeep } from 'lodash';
import { DependenciesResponse } from '../../types/dependencies';
import { getDependencies } from '../dependencies';

jest.mock('../dependencies');

export const DEFAULT_DEPENDENCIES_MOCK: DependenciesResponse = {
  page: {
    pageIndex: 1,
    pageSize: 50,
    total: 0,
  },
  dependencies: [],
};

export default class DependenciesServiceMock {
  #defaultDependenciesData: DependenciesResponse = DEFAULT_DEPENDENCIES_MOCK;

  constructor() {
    jest.mocked(getDependencies).mockImplementation(this.handleGetDependencies);
  }

  reset = () => {
    this.#defaultDependenciesData = cloneDeep(DEFAULT_DEPENDENCIES_MOCK);
    return this;
  };

  setDefaultDependencies = (response: DependenciesResponse) => {
    this.#defaultDependenciesData = response;
  };

  handleGetDependencies = (data: { pageParam: number; q?: string }) => {
    const { pageSize } = this.#defaultDependenciesData.page;
    const totalDependencies = this.#defaultDependenciesData.dependencies.length;

    if (pageSize < totalDependencies) {
      const startIndex = (data.pageParam - 1) * pageSize;
      const endIndex = startIndex + pageSize;

      return Promise.resolve({
        ...this.#defaultDependenciesData,
        dependencies: this.#defaultDependenciesData.dependencies.slice(startIndex, endIndex),
      });
    }

    return Promise.resolve({
      ...this.#defaultDependenciesData,
      dependencies: this.#defaultDependenciesData.dependencies.filter(
        (dependency) =>
          typeof data.q !== 'string' ||
          dependency.name.toLowerCase().includes(data.q.toLowerCase()),
      ),
    });
  };
}
