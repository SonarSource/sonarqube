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
import { cloneDeep, flatMap, map, pick, times } from 'lodash';
import { mockComponent } from '../../helpers/mocks/component';
import {
  mockDuplicatedFile,
  mockDuplication,
  mockDuplicationBlock,
  mockSourceLine,
  mockSourceViewerFile,
} from '../../helpers/mocks/sources';
import { HttpStatus, RequestData } from '../../helpers/request';
import { BranchParameters } from '../../types/branch-like';
import { ComponentQualifier, TreeComponent, Visibility } from '../../types/component';
import {
  Component,
  ComponentMeasure,
  Dict,
  DuplicatedFile,
  Duplication,
  Measure,
  Metric,
  Paging,
  SourceLine,
  SourceViewerFile,
} from '../../types/types';
import {
  getChildren,
  getComponentData,
  getComponentForSourceViewer,
  getComponentTree,
  getDuplications,
  getSources,
  getTree,
  GetTreeParams,
} from '../components';

interface ComponentTree {
  component: Component;
  ancestors: Component[];
  measures?: Measure[];
  children: ComponentTree[];
}

interface SourceFile {
  component: SourceViewerFile;
  lines: SourceLine[];
  duplication?: {
    duplications: Duplication[];
    files: Dict<DuplicatedFile>;
  };
}

function isLeaf(node: ComponentTree) {
  return node.children.length === 0;
}

function listChildComponent(node: ComponentTree): Component[] {
  return map(node.children, (n) => n.component);
}

function listAllComponent(node: ComponentTree): Component[] {
  if (isLeaf(node)) {
    return [node.component];
  }

  return [node.component, ...flatMap(node.children, listAllComponent)];
}

function listLeavesComponent(node: ComponentTree): Component[] {
  if (isLeaf(node)) {
    return [node.component];
  }
  return flatMap(node.children, listLeavesComponent);
}

export default class ComponentsServiceMock {
  failLoadingComponentStatus: HttpStatus | undefined = undefined;
  defaultComponents: ComponentTree[];
  components: ComponentTree[];
  defaultSourceFiles: SourceFile[];
  sourceFiles: SourceFile[];

  constructor(components?: ComponentTree[], sourceFiles?: SourceFile[]) {
    const baseComponent = mockComponent({
      key: 'foo',
      name: 'Foo',
      breadcrumbs: [{ key: 'foo', name: 'Foo', qualifier: ComponentQualifier.Project }],
    });
    const folderComponent = mockComponent({
      key: 'foo:folderA',
      name: 'folderA',
      path: 'folderA',
      qualifier: ComponentQualifier.Directory,
      breadcrumbs: [
        ...baseComponent.breadcrumbs,
        { key: 'foo:folderA', name: 'folderA', qualifier: ComponentQualifier.Directory },
      ],
    });
    this.defaultComponents = components || [
      {
        component: baseComponent,
        ancestors: [],
        children: [
          {
            component: folderComponent,
            ancestors: [baseComponent],
            children: [
              {
                component: mockComponent({
                  key: 'foo:folderA/out.tsx',
                  name: 'out.tsx',
                  path: 'folderA/out.tsx',
                  qualifier: ComponentQualifier.File,
                  breadcrumbs: [
                    ...folderComponent.breadcrumbs,
                    {
                      key: 'foo:folderA/out.tsx',
                      name: 'out.tsx',
                      qualifier: ComponentQualifier.File,
                    },
                  ],
                }),
                ancestors: [baseComponent, folderComponent],
                children: [],
              },
            ],
          },
          {
            component: mockComponent({
              key: 'foo:index.tsx',
              name: 'index.tsx',
              path: 'index.tsx',
              qualifier: ComponentQualifier.File,
              breadcrumbs: [
                ...baseComponent.breadcrumbs,
                { key: 'foo:index.tsx', name: 'index.tsx', qualifier: ComponentQualifier.File },
              ],
            }),
            ancestors: [baseComponent],
            children: [],
          },
          {
            component: mockComponent({
              key: 'foo:test1.js',
              name: 'test1.js',
              path: 'test1.js',
              qualifier: ComponentQualifier.File,
              breadcrumbs: [
                ...baseComponent.breadcrumbs,
                { key: 'foo:test1.js', name: 'test1.js', qualifier: ComponentQualifier.File },
              ],
            }),
            ancestors: [baseComponent],
            children: [],
          },
          {
            component: mockComponent({
              key: 'foo:test2.js',
              name: 'test2.js',
              path: 'test2.js',
              qualifier: ComponentQualifier.File,
              breadcrumbs: [
                ...baseComponent.breadcrumbs,
                { key: 'foo:test2.js', name: 'test2.js', qualifier: ComponentQualifier.File },
              ],
            }),
            ancestors: [baseComponent],
            children: [],
          },
          {
            component: mockComponent({
              key: 'foo:testSymb.tsx',
              name: 'testSymb.tsx',
              path: 'testSymb.tsx',
              qualifier: ComponentQualifier.File,
              breadcrumbs: [
                ...baseComponent.breadcrumbs,
                {
                  key: 'foo:testSymb.tsx',
                  name: 'testSymb.tsx',
                  qualifier: ComponentQualifier.File,
                },
              ],
            }),
            ancestors: [baseComponent],
            children: [],
          },
          {
            component: mockComponent({
              key: 'foo:empty.js',
              name: 'empty.js',
              path: 'empty.js',
              qualifier: ComponentQualifier.File,
              breadcrumbs: [
                ...baseComponent.breadcrumbs,
                { key: 'foo:empty.js', name: 'empty.js', qualifier: ComponentQualifier.File },
              ],
            }),
            ancestors: [baseComponent],
            children: [],
          },
          {
            component: mockComponent({
              key: 'foo:huge.js',
              name: 'huge.js',
              path: 'huge.js',
              qualifier: ComponentQualifier.File,
              breadcrumbs: [
                ...baseComponent.breadcrumbs,
                { key: 'foo:huge.js', name: 'huge.js', qualifier: ComponentQualifier.File },
              ],
            }),
            ancestors: [baseComponent],
            children: [],
          },
        ],
      },
    ];

    this.defaultSourceFiles =
      sourceFiles ||
      ([
        {
          component: mockSourceViewerFile('index.tsx', 'foo'),
          lines: times(50, (n) =>
            mockSourceLine({
              line: n,
              code: 'function Test() {}',
            })
          ),
        },
        {
          component: mockSourceViewerFile('folderA/out.tsx', 'foo'),
          lines: times(50, (n) =>
            mockSourceLine({
              line: n,
              code: 'function Test() {}',
            })
          ),
        },
        {
          component: mockSourceViewerFile('test1.js', 'foo'),
          lines: [
            {
              line: 1,
              code: '\u003cspan class\u003d"cd"\u003e/*\u003c/span\u003e',
              scmRevision: 'f09ee6b610528aa37b7b51be395c93524cebae8f',
              scmAuthor: 'stas.vilchik@sonarsource.com',
              scmDate: '2018-07-10T20:21:20+0200',
              duplicated: false,
              isNew: false,
              lineHits: 1,
              coveredConditions: 1,
            },
            {
              line: 2,
              code: '\u003cspan class\u003d"cd"\u003e * SonarQube\u003c/span\u003e',
              scmRevision: 'f09ee6b610528aa37b7b51be395c93524cebae8f',
              scmAuthor: 'stas.vilchik@sonarsource.com',
              scmDate: '2018-07-10T20:21:20+0200',
              duplicated: false,
              isNew: false,
              lineHits: 0,
              conditions: 1,
            },
            {
              line: 3,
              code: '\u003cspan class\u003d"cd"\u003e * Copyright\u003c/span\u003e',
              scmRevision: '89a3d21bc28f2fa6201b5e8b1185d5358481b3dd',
              scmAuthor: 'pierre.guillot@sonarsource.com',
              scmDate: '2022-01-28T21:03:07+0100',
              duplicated: false,
              isNew: false,
              lineHits: 1,
            },
            {
              line: 4,
              code: '\u003cspan class\u003d"cd"\u003e * mailto:info AT sonarsource DOT com\u003c/span\u003e',
              scmRevision: 'f09ee6b610528aa37b7b51be395c93524cebae8f',
              scmAuthor: 'stas.vilchik@sonarsource.com',
              duplicated: false,
              isNew: false,
              lineHits: 1,
              conditions: 1,
              coveredConditions: 1,
            },
            {
              line: 5,
              code: '\u003cspan class\u003d"cd"\u003e * 5\u003c/span\u003e',
              scmRevision: 'f04ee6b610528aa37b7b51be395c93524cebae8f',
              duplicated: false,
              isNew: false,
              lineHits: 2,
              conditions: 2,
              coveredConditions: 1,
            },
            {
              line: 6,
              code: '\u003cspan class\u003d"cd"\u003e * 6\u003c/span\u003e',
              scmRevision: 'f04ee6b610528aa37b7b51be395c93524cebae8f',
              duplicated: false,
              isNew: false,
              lineHits: 0,
            },
            {
              line: 7,
              code: '\u003cspan class\u003d"cd"\u003e * 7\u003c/span\u003e',
              scmRevision: 'f04ee6b610528aa37b7b51be395c93524cebae8f',
              duplicated: true,
              isNew: true,
            },
            {
              code: '\u003cspan class\u003d"cd"\u003e * This program is free software; you can redistribute it and/or\u003c/span\u003e',
              scmRevision: 'f09ee6b610528aa37b7b51be395c93524cebae8f',
              scmAuthor: 'stas.vilchik@sonarsource.com',
              scmDate: '2018-07-10T20:21:20+0200',
              duplicated: false,
              isNew: false,
            },
          ],
          duplication: {
            duplications: [
              mockDuplication({
                blocks: [
                  mockDuplicationBlock({ from: 7, size: 1, _ref: '1' }),
                  mockDuplicationBlock({ from: 1, size: 1, _ref: '2' }),
                ],
              }),
            ],
            files: {
              '1': mockDuplicatedFile({ key: 'foo:test1.js' }),
              '2': mockDuplicatedFile({ key: 'foo:test2.js' }),
            },
          },
        },
        {
          component: mockSourceViewerFile('test2.js', 'foo'),
          lines: times(50, (n) =>
            mockSourceLine({
              line: n,
              code: `\u003cspan class\u003d"cd"\u003eLine ${n}\u003c/span\u003e`,
            })
          ),
        },
        {
          component: mockSourceViewerFile('empty.js', 'foo'),
          lines: [],
        },
        {
          component: mockSourceViewerFile('huge.js', 'foo'),
          lines: times(200, (n) =>
            mockSourceLine({
              line: n,
              code: `\u003cspan class\u003d"cd"\u003eLine ${n}\u003c/span\u003e`,
            })
          ),
        },
        {
          component: mockSourceViewerFile('testSymb.tsx', 'foo'),
          lines: times(20, (n) =>
            mockSourceLine({
              line: n,
              code: '  <span class="sym-35 sym">symbole</span>',
            })
          ),
        },
      ] as SourceFile[]);

    this.components = cloneDeep(this.defaultComponents);
    this.sourceFiles = cloneDeep(this.defaultSourceFiles);

    (getComponentTree as jest.Mock).mockImplementation(this.handleGetComponentTree);
    (getChildren as jest.Mock).mockImplementation(this.handleGetChildren);
    (getTree as jest.Mock).mockImplementation(this.handleGetTree);
    (getComponentData as jest.Mock).mockImplementation(this.handleGetComponentData);
    (getComponentForSourceViewer as jest.Mock).mockImplementation(
      this.handleGetComponentForSourceViewer
    );
    (getDuplications as jest.Mock).mockImplementation(this.handleGetDuplications);
    (getSources as jest.Mock).mockImplementation(this.handleGetSources);
  }

  findComponentTree = (key: string, from?: ComponentTree): ComponentTree | undefined => {
    const recurse = (node: ComponentTree): ComponentTree | undefined => {
      if (node.component.key === key) {
        return node;
      }
      return node.children.find((child) => recurse(child));
    };

    if (from === undefined) {
      for (let i = 0, len = this.components.length; i < len; i++) {
        const tree = recurse(this.components[i]);
        if (tree) {
          return tree;
        }
      }
      throw new Error(`Couldn't find component tree for key ${key}`);
    }

    return recurse(from);
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
      { sourceFile: undefined, size: -Infinity }
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
  };

  handleGetChildren = (
    component: string,
    metrics: string[] = [],
    data: RequestData = {}
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
    _metrics: string[] = [],
    { p = 1, ps = 100 }: RequestData = {}
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
        measures: this.findComponentTree(c.key, base)?.measures,
        ...pick(c, ['analysisDate', 'key', 'name', 'qualifier']),
      };
    });

    return this.reply({
      baseComponent: base.component,
      components: componentsMeasures.slice(ps * (p - 1), ps * (p - 1) + ps),
      metrics: [],
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
      return this.reply({ component, ancestors });
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

  reply<T>(response: T): Promise<T> {
    return Promise.resolve(cloneDeep(response));
  }
}
