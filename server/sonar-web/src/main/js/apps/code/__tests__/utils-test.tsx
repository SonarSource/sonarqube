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
import { getBreadcrumbs, getChildren, getComponent } from '../../../api/components';
import { mockMainBranch, mockPullRequest } from '../../../helpers/mocks/branch-like';
import {
  addComponent,
  addComponentBreadcrumbs,
  addComponentChildren,
  getComponentBreadcrumbs,
} from '../bucket';
import {
  getCodeMetrics,
  loadMoreChildren,
  mostCommonPrefix,
  retrieveComponent,
  retrieveComponentChildren,
} from '../utils';

jest.mock('../../../api/components', () => ({
  getBreadcrumbs: jest.fn().mockRejectedValue({}),
  getChildren: jest.fn().mockRejectedValue({}),
  getComponent: jest.fn().mockRejectedValue({}),
}));

jest.mock('../bucket', () => ({
  addComponent: jest.fn(),
  addComponentBreadcrumbs: jest.fn(),
  addComponentChildren: jest.fn(),
  getComponent: jest.fn(),
  getComponentBreadcrumbs: jest.fn(),
  getComponentChildren: jest.fn(),
}));

beforeEach(() => {
  jest.clearAllMocks();
});

describe('getCodeMetrics', () => {
  it('should return the right metrics for portfolios', () => {
    expect(getCodeMetrics('VW')).toMatchSnapshot();
    expect(getCodeMetrics('VW', undefined, { includeQGStatus: true })).toMatchSnapshot();
    expect(
      getCodeMetrics('VW', undefined, { includeQGStatus: true, newCode: true })
    ).toMatchSnapshot();
    expect(
      getCodeMetrics('VW', undefined, { includeQGStatus: true, newCode: false })
    ).toMatchSnapshot();
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
      paging: { total: 2, pageIndex: 0 },
    });

    await retrieveComponentChildren('key', 'TRK', { mounted: true }, mockMainBranch());

    expect(addComponentChildren).toHaveBeenCalledWith('key', components, 2, 0);
    expect(addComponent).toHaveBeenCalledTimes(2);
    expect(getComponentBreadcrumbs).toHaveBeenCalledWith('key');
  });
});

describe('retrieveComponent', () => {
  it('should update bucket when component is mounted', async () => {
    const components = [{}, {}];
    (getChildren as jest.Mock).mockResolvedValueOnce({
      components,
      paging: { total: 2, pageIndex: 0 },
    });
    (getComponent as jest.Mock).mockResolvedValueOnce({
      component: {},
    });
    (getBreadcrumbs as jest.Mock).mockResolvedValueOnce([]);

    await retrieveComponent('key', 'TRK', { mounted: true }, mockMainBranch());

    expect(addComponentChildren).toHaveBeenCalled();
    expect(addComponent).toHaveBeenCalledTimes(3);
    expect(addComponentBreadcrumbs).toHaveBeenCalled();
  });

  it('should not update bucket when component is not mounted', async () => {
    const components = [{}, {}];
    (getChildren as jest.Mock).mockResolvedValueOnce({
      components,
      paging: { total: 2, pageIndex: 0 },
    });
    (getComponent as jest.Mock).mockResolvedValueOnce({
      component: {},
    });
    (getBreadcrumbs as jest.Mock).mockResolvedValueOnce([]);

    await retrieveComponent('key', 'TRK', { mounted: false }, mockMainBranch());

    expect(addComponentChildren).not.toHaveBeenCalled();
    expect(addComponent).not.toHaveBeenCalled();
    expect(addComponentBreadcrumbs).not.toHaveBeenCalled();
  });
});

describe('loadMoreChildren', () => {
  it('should load more children', async () => {
    const components = [{}, {}, {}];
    (getChildren as jest.Mock).mockResolvedValueOnce({
      components,
      paging: { total: 6, pageIndex: 1 },
    });

    await loadMoreChildren('key', 1, 'TRK', { mounted: true }, mockMainBranch());

    expect(addComponentChildren).toHaveBeenCalledWith('key', components, 6, 1);
    expect(addComponent).toHaveBeenCalledTimes(3);
    expect(getComponentBreadcrumbs).toHaveBeenCalledWith('key');
  });
});

describe('#mostCommonPrefix', () => {
  it('should correctly find the common path prefix', () => {
    expect(mostCommonPrefix(['src/main/ts/tests', 'src/main/java/tests'])).toEqual('src/main/');
    expect(mostCommonPrefix(['src/main/ts/app', 'src/main/ts/app'])).toEqual('src/main/ts/');
    expect(mostCommonPrefix(['src/main/ts', 'lib/main/ts'])).toEqual('');
  });
});
