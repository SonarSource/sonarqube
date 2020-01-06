/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import { getChildren } from '../../../api/components';
import { mockMainBranch, mockPullRequest } from '../../../helpers/mocks/branch-like';
import { addComponent, addComponentChildren, getComponentBreadcrumbs } from '../bucket';
import { getCodeMetrics, loadMoreChildren, retrieveComponentChildren } from '../utils';

jest.mock('../../../api/components', () => ({
  getChildren: jest.fn().mockRejectedValue({})
}));

jest.mock('../bucket', () => ({
  addComponent: jest.fn(),
  addComponentBreadcrumbs: jest.fn(),
  addComponentChildren: jest.fn(),
  getComponent: jest.fn(),
  getComponentBreadcrumbs: jest.fn(),
  getComponentChildren: jest.fn()
}));

beforeEach(() => {
  jest.clearAllMocks();
});

describe('getCodeMetrics', () => {
  it('should return the right metrics for portfolios', () => {
    expect(getCodeMetrics('VW')).toMatchSnapshot();
    expect(getCodeMetrics('VW', undefined, { includeQGStatus: true })).toMatchSnapshot();
  });

  it('should return the right metrics for apps', () => {
    expect(getCodeMetrics('APP')).toMatchSnapshot();
  });

  it('should return the right metrics for projects', () => {
    expect(getCodeMetrics('TRK', mockMainBranch())).toMatchSnapshot();
    expect(getCodeMetrics('TRK', mockPullRequest())).toMatchSnapshot();
  });
});

describe('retrieveComponentChildren', () => {
  it('should retrieve children correctly', async () => {
    const components = [{}, {}];
    (getChildren as jest.Mock).mockResolvedValueOnce({
      components,
      paging: { total: 2, pageIndex: 0 }
    });

    await retrieveComponentChildren('key', 'TRK', mockMainBranch());

    expect(addComponentChildren).toHaveBeenCalledWith('key', components, 2, 0);
    expect(addComponent).toHaveBeenCalledTimes(2);
    expect(getComponentBreadcrumbs).toHaveBeenCalledWith('key');
  });
});

describe('loadMoreChildren', () => {
  it('should load more children', async () => {
    const components = [{}, {}, {}];
    (getChildren as jest.Mock).mockResolvedValueOnce({
      components,
      paging: { total: 6, pageIndex: 1 }
    });

    await loadMoreChildren('key', 1, 'TRK', mockMainBranch());

    expect(addComponentChildren).toHaveBeenCalledWith('key', components, 6, 1);
    expect(addComponent).toHaveBeenCalledTimes(3);
    expect(getComponentBreadcrumbs).toHaveBeenCalledWith('key');
  });
});
