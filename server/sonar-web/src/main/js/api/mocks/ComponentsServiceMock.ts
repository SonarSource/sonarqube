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
import { cloneDeep, pick } from 'lodash';
import { BranchParameters } from '~sonar-aligned/types/branch-like';
import { DEFAULT_METRICS } from '../../helpers/mocks/metrics';
import { HttpStatus, RequestData } from '../../helpers/request';
import { mockMetric } from '../../helpers/testMocks';
import { isDefined } from '../../helpers/types';
import { TreeComponent, Visibility } from '../../types/component';
import {
  Component,
  ComponentMeasure,
  Dict,
  DuplicatedFile,
  Duplication,
  Metric,
  Paging,
} from '../../types/types';
import {
  ComponentRaw,
  GetTreeParams,
  changeKey,
  doesComponentExists,
  getBreadcrumbs,
  getChildren,
  getComponentData,
  getComponentForSourceViewer,
  getComponentLeaves,
  getComponentTree,
  getDuplications,
  getSources,
  getTree,
  searchProjects,
  setApplicationTags,
  setProjectTags,
} from '../components';
import {
  ComponentTree,
  SourceFile,
  mockFullComponentTree,
  mockFullSourceViewerFileList,
} from './data/components';
import { mockIssuesList } from './data/issues';
import { MeasureRecords, mockFullMeasureData } from './data/measures';
import { mockProjects } from './data/projects';
import { listAllComponent, listChildComponent, listLeavesComponent } from './data/utils';

jest.mock('../components');

export default class ComponentsServiceMock {
  failLoadingComponentStatus: HttpStatus | undefined = undefined;
  defaultComponents: ComponentTree[];
  defaultProjects: ComponentRaw[];
  components: ComponentTree[];
  defaultSourceFiles: SourceFile[];
  sourceFiles: SourceFile[];
  defaultMeasures: MeasureRecords;
  measures: MeasureRecords;
  projects: ComponentRaw[];

  constructor(components?: ComponentTree[], sourceFiles?: SourceFile[], measures?: MeasureRecords) {
    this.defaultComponents = components || [mockFullComponentTree()];
    this.defaultSourceFiles = sourceFiles || mockFullSourceViewerFileList();
    const issueList = mockIssuesList();
    this.defaultMeasures =
      measures ||
      this.defaultComponents.reduce(
        (acc, tree) => ({ ...acc, ...mockFullMeasureData(tree, issueList) }),
        {},
      );
    this.defaultProjects = mockProjects();

    this.components = cloneDeep(this.defaultComponents);
    this.sourceFiles = cloneDeep(this.defaultSourceFiles);
    this.measures = cloneDeep(this.defaultMeasures);
    this.projects = cloneDeep(this.defaultProjects);

    jest.mocked(getComponentTree).mockImplementation(this.handleGetComponentTree);
    jest.mocked(getChildren).mockImplementation(this.handleGetChildren);
    jest.mocked(getTree).mockImplementation(this.handleGetTree);
    jest.mocked(getComponentData).mockImplementation(this.handleGetComponentData);
    jest
      .mocked(getComponentForSourceViewer)
      .mockImplementation(this.handleGetComponentForSourceViewer);
    jest.mocked(getDuplications).mockImplementation(this.handleGetDuplications);
    jest.mocked(getSources).mockImplementation(this.handleGetSources);
    jest.mocked(changeKey).mockImplementation(this.handleChangeKey);
    jest.mocked(getComponentLeaves).mockImplementation(this.handleGetComponentLeaves);
    jest.mocked(getBreadcrumbs).mockImplementation(this.handleGetBreadcrumbs);
    jest.mocked(setProjectTags).mockImplementation(this.handleSetProjectTags);
    jest.mocked(setApplicationTags).mockImplementation(this.handleSetApplicationTags);
    jest.mocked(searchProjects).mockImplementation(this.handleSearchProjects);
    jest.mocked(doesComponentExists).mockImplementation(this.handleDoesComponentExists);
  }

  handleSearchProjects: typeof searchProjects = (data) => {
    const pageIndex = data.p ?? 1;
    const pageSize = data.ps ?? 100;

    const components = this.projects
      .filter((c) => {
        if (data.filter && data.filter.startsWith('query')) {
          const query = data.filter.split('query=')[1];
          return c.key.includes(query) || c.name.includes(query);
        }
      })
      .map((c) => c);

    return this.reply({
      components: components.slice((pageIndex - 1) * pageSize, pageIndex * pageSize),
      facets: [],
      paging: {
        pageSize,
        pageIndex,
        total: components.length,
      },
    });
  };

  findComponentTree = (key: string, from?: ComponentTree) => {
    let tree: ComponentTree | undefined;
    const recurse = (node: ComponentTree): boolean => {
      if (node.component.key === key) {
        tree = node;
        return true;
      }
      return node.children.some((child) => recurse(child));
    };

    if (from !== undefined) {
      recurse(from);
      return tree;
    }

    for (let i = 0, len = this.components.length; i < len; i++) {
      if (recurse(this.components[i])) {
        return tree;
      }
    }

    throw new Error(`Couldn't find component tree for key ${key}`);
  };

  findSourceFile = (key: string): SourceFile => {
    const sourceFile = this.sourceFiles.find((s) => s.component.key === key);
    if (sourceFile) {
      return sourceFile;
    }
    throw new Error(`Couldn't find source file for key ${key}`);
  };

  registerComponent = (component: Component, ancestors: Component[] = []) => {
    this.components.push({ component, ancestors, children: [] });
  };

  registerComponentTree = (componentTree: ComponentTree, replace = true) => {
    if (replace) {
      this.components = [];
    }
    this.components.push(componentTree);
  };

  registerComponentMeasures = (measures: MeasureRecords) => {
    this.measures = measures;
  };

  setFailLoadingComponentStatus = (status: HttpStatus.Forbidden | HttpStatus.NotFound) => {
    this.failLoadingComponentStatus = status;
  };

  getHugeFileKey = () => {
    const { sourceFile } = this.sourceFiles.reduce(
      (acc, sourceFile) => {
        if (sourceFile.lines.length > acc.size) {
          return {
            sourceFile,
            size: sourceFile.lines.length,
          };
        }
        return acc;
      },
      { sourceFile: undefined, size: -Infinity },
    );
    if (sourceFile) {
      return sourceFile.component.key;
    }
    throw new Error('Could not find a large source file');
  };

  getEmptyFileKey = () => {
    const sourceFile = this.sourceFiles.find((sourceFile) => {
      if (sourceFile.lines.length === 0) {
        return sourceFile;
      }
    });
    if (sourceFile) {
      return sourceFile.component.key;
    }
    throw new Error('Could not find an empty source file');
  };

  getNonEmptyFileKey = (preferredKey = 'foo:test1.js') => {
    let sourceFile = this.sourceFiles.find((sourceFile) => {
      if (sourceFile.component.key === preferredKey) {
        return sourceFile;
      }
    });

    if (!sourceFile) {
      sourceFile = this.sourceFiles.find((sourceFile) => {
        if (sourceFile.lines.length > 0) {
          return sourceFile;
        }
      });
    }

    if (sourceFile) {
      return sourceFile.component.key;
    }
    throw new Error('Could not find a non-empty source file');
  };

  reset = () => {
    this.components = cloneDeep(this.defaultComponents);
    this.sourceFiles = cloneDeep(this.defaultSourceFiles);
    this.measures = cloneDeep(this.defaultMeasures);
  };

  handleGetChildren = (
    component: string,
    metrics: string[] = [],
    data: RequestData = {},
  ): Promise<{
    baseComponent: ComponentMeasure;
    components: ComponentMeasure[];
    metrics: Metric[];
    paging: Paging;
  }> => {
    return this.handleGetComponentTree('children', component, metrics, data);
  };

  handleGetComponentTree = (
    strategy: string,
    key: string,
    metricKeys: string[] = [],
    { p = 1, ps = 100 }: RequestData = {},
  ): Promise<{
    baseComponent: ComponentMeasure;
    components: ComponentMeasure[];
    metrics: Metric[];
    paging: Paging;
  }> => {
    const base = this.findComponentTree(key);
    let components: Component[] = [];
    if (base === undefined) {
      return Promise.reject({
        errors: [{ msg: `No component has been found for id ${key}` }],
      });
    }
    if (strategy === 'all' || strategy === '') {
      components = listAllComponent(base);
    } else if (strategy === 'children') {
      components = listChildComponent(base);
    } else if (strategy === 'leaves') {
      components = listLeavesComponent(base);
    }

    const componentsMeasures: ComponentMeasure[] = components.map((c) => {
      return {
        measures: metricKeys
          .map((metric) => this.measures[c.key] && this.measures[c.key][metric])
          .filter(isDefined),
        ...pick(c, ['analysisDate', 'key', 'name', 'qualifier']),
      };
    });

    return this.reply({
      baseComponent: base.component,
      components: componentsMeasures.slice(ps * (p - 1), ps * (p - 1) + ps),
      metrics: metricKeys.map((metric) => DEFAULT_METRICS[metric] ?? mockMetric({ key: metric })),
      paging: {
        pageSize: ps,
        pageIndex: p,
        total: componentsMeasures.length,
      },
    });
  };

  handleGetTree = ({
    component: key,
    q = '',
    qualifiers,
  }: GetTreeParams & { qualifiers?: string }): Promise<{
    baseComponent: TreeComponent;
    components: TreeComponent[];
    paging: Paging;
  }> => {
    const base = this.findComponentTree(key);
    if (base === undefined) {
      return Promise.reject({
        errors: [{ msg: `No component has been found for key ${key}` }],
      });
    }
    const components: TreeComponent[] = listAllComponent(base)
      .filter(({ name, key }) => name.includes(q) || key.includes(q))
      .filter(({ qualifier }) => (qualifiers?.length ? qualifiers.includes(qualifier) : true))
      .map((c) => ({ ...c, visibility: Visibility.Public }));

    return this.reply({
      baseComponent: { ...base.component, visibility: Visibility.Public },
      components,
      paging: {
        pageIndex: 1,
        pageSize: 100,
        total: components.length,
      },
    });
  };

  handleGetComponentData = (data: { component: string } & BranchParameters) => {
    if (this.failLoadingComponentStatus !== undefined) {
      return Promise.reject({ status: this.failLoadingComponentStatus });
    }
    const tree = this.findComponentTree(data.component);
    if (tree) {
      const { component, ancestors } = tree;
      return this.reply({ component, ancestors } as {
        component: ComponentRaw;
        ancestors: ComponentRaw[];
      });
    }
    throw new Error(`Couldn't find component with key ${data.component}`);
  };

  handleGetComponentForSourceViewer = ({ component }: { component: string } & BranchParameters) => {
    const sourceFile = this.findSourceFile(component);
    return this.reply(sourceFile.component);
  };

  handleGetDuplications = ({
    key,
  }: { key: string } & BranchParameters): Promise<{
    duplications: Duplication[];
    files: Dict<DuplicatedFile>;
  }> => {
    const { duplication } = this.findSourceFile(key);
    if (duplication) {
      return this.reply(duplication);
    }
    return this.reply({ duplications: [], files: {} });
  };

  handleGetSources = (data: { key: string; from?: number; to?: number } & BranchParameters) => {
    const { lines } = this.findSourceFile(data.key);
    const from = data.from || 1;
    const to = data.to || lines.length;
    return this.reply(lines.slice(from - 1, to));
  };

  handleChangeKey = (data: { from: string; to: string }) => {
    const treeItem = this.components.find(({ component }) => component.key === data.from);
    if (treeItem) {
      treeItem.component.key = data.to;
      return this.reply(undefined);
    }
    return Promise.reject({ status: 404, message: 'Component not found' });
  };

  handleGetComponentLeaves = (
    component: string,
    metrics: string[] = [],
    data: RequestData = {},
  ): Promise<{
    baseComponent: ComponentMeasure;
    components: ComponentMeasure[];
    metrics: Metric[];
    paging: Paging;
  }> => {
    return this.handleGetComponentTree('leaves', component, metrics, data);
  };

  handleGetBreadcrumbs = ({ component: key }: { component: string } & BranchParameters) => {
    const base = this.findComponentTree(key);
    if (base === undefined) {
      return Promise.reject({
        errors: [{ msg: `No component has been found for id ${key}` }],
      });
    }
    return this.reply([...(base.ancestors as ComponentRaw[]), base.component as ComponentRaw]);
  };

  handleSetProjectTags: typeof setProjectTags = ({ project, tags }) => {
    const base = this.findComponentTree(project);
    if (base !== undefined) {
      base.component.tags = tags.split(',');
    }
    return this.reply();
  };

  handleSetApplicationTags: typeof setApplicationTags = ({ application, tags }) => {
    const base = this.findComponentTree(application);
    if (base !== undefined) {
      base.component.tags = tags.split(',');
    }
    return this.reply();
  };

  handleDoesComponentExists: typeof doesComponentExists = ({ component }) => {
    const exists = this.components.some(({ component: { key } }) => key === component);
    return this.reply(exists);
  };

  reply<T>(): Promise<void>;
  reply<T>(response: T): Promise<T>;
  reply<T>(response?: T): Promise<T | void> {
    return Promise.resolve(response ? cloneDeep(response) : undefined);
  }
}
