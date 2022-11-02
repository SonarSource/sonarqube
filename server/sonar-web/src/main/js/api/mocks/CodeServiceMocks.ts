/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { cloneDeep, flatMap, map } from 'lodash';
import { mockComponent, mockComponentMeasure } from '../../helpers/mocks/component';
import { ComponentQualifier } from '../../types/component';
import { RawIssuesResponse } from '../../types/issues';
import { ComponentMeasure, Metric, Paging } from '../../types/types';
import { getChildren, getComponentTree } from '../components';
import { searchIssues } from '../issues';

interface ComponentTree {
  component: ComponentMeasure;
  child: ComponentTree[];
}

function isLeaf(node: ComponentTree) {
  return node.child.length === 0;
}

function listChildComponent(node: ComponentTree): ComponentMeasure[] {
  return map(node.child, (n) => n.component);
}

function listAllComponent(node: ComponentTree): ComponentMeasure[] {
  if (isLeaf(node)) {
    return [node.component];
  }

  return [node.component, ...flatMap(node.child, listAllComponent)];
}

function listLeavesComponent(node: ComponentTree): ComponentMeasure[] {
  if (isLeaf(node)) {
    return [node.component];
  }
  return flatMap(node.child, listLeavesComponent);
}

export default class CodeServiceMock {
  componentTree: ComponentTree;

  constructor() {
    this.componentTree = {
      component: mockComponentMeasure(),
      child: [
        {
          component: mockComponentMeasure(false, {
            key: 'foo:folerA',
            name: 'folderA',
            path: 'folderA',
            qualifier: ComponentQualifier.Directory,
          }),
          child: [
            {
              component: mockComponentMeasure(true, {
                key: 'foo:folderA/out.tsx',
                name: 'out.tsx',
                path: 'folderA/out.tsx',
              }),
              child: [],
            },
          ],
        },
        {
          component: mockComponentMeasure(true, {
            key: 'foo:index.tsx',
            name: 'index.tsx',
            path: 'index.tsx',
          }),
          child: [],
        },
      ],
    };
    (getComponentTree as jest.Mock).mockImplementation(this.handleGetComponentTree);
    (getChildren as jest.Mock).mockImplementation(this.handleGetChildren);
    (searchIssues as jest.Mock).mockImplementation(this.handleSearchIssue);
  }

  findBaseComponent(key: string, from = this.componentTree): ComponentTree | undefined {
    if (from.component.key === key) {
      return from;
    }
    return from.child.find((node) => this.findBaseComponent(key, node));
  }

  handleGetChildren = (
    component: string
  ): Promise<{
    baseComponent: ComponentMeasure;
    components: ComponentMeasure[];
    metrics: Metric[];
    paging: Paging;
  }> => {
    return this.handleGetComponentTree('children', component);
  };

  handleGetComponentTree = (
    strategy: string,
    component: string
  ): Promise<{
    baseComponent: ComponentMeasure;
    components: ComponentMeasure[];
    metrics: Metric[];
    paging: Paging;
  }> => {
    const base = this.findBaseComponent(component);
    let components: ComponentMeasure[] = [];
    if (base === undefined) {
      return Promise.reject({
        errors: [{ msg: `No component has been found for id ${component}` }],
      });
    }
    if (strategy === 'all' || strategy === '') {
      components = listAllComponent(base);
    } else if (strategy === 'children') {
      components = listChildComponent(base);
    } else if (strategy === 'leaves') {
      components = listLeavesComponent(base);
    }

    return this.reply({
      baseComponent: base.component,
      components,
      metrics: [],
      paging: {
        pageIndex: 1,
        pageSize: 100,
        total: components.length,
      },
    });
  };

  handleSearchIssue = (): Promise<RawIssuesResponse> => {
    return this.reply({
      components: [],
      effortTotal: 1,
      facets: [],
      issues: [],
      languages: [],
      paging: { total: 0, pageIndex: 1, pageSize: 100 },
    });
  };

  getRootComponent() {
    return mockComponent(this.componentTree.component);
  }

  reply<T>(response: T): Promise<T> {
    return Promise.resolve(cloneDeep(response));
  }
}
