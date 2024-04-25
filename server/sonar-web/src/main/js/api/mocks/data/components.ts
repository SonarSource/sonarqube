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
import { times } from 'lodash';
import { ComponentQualifier } from '~sonar-aligned/types/component';
import { mockComponent } from '../../../helpers/mocks/component';
import {
  mockDuplicatedFile,
  mockDuplication,
  mockDuplicationBlock,
  mockSourceLine,
  mockSourceViewerFile,
} from '../../../helpers/mocks/sources';
import {
  Component,
  Dict,
  DuplicatedFile,
  Duplication,
  SourceLine,
  SourceViewerFile,
} from '../../../types/types';
import {
  FILE1_KEY,
  FILE2_KEY,
  FILE3_KEY,
  FILE4_KEY,
  FILE5_KEY,
  FILE6_KEY,
  FILE7_KEY,
  FILE8_KEY,
  FOLDER1_KEY,
  PARENT_COMPONENT_KEY,
} from './ids';

export interface ComponentTree {
  component: Component;
  ancestors: Component[];
  children: ComponentTree[];
}

export interface SourceFile {
  component: SourceViewerFile;
  lines: SourceLine[];
  duplication?: {
    duplications: Duplication[];
    files: Dict<DuplicatedFile>;
  };
}

export function mockFullComponentTree(
  baseComponent = mockComponent({
    key: PARENT_COMPONENT_KEY,
    name: 'Foo',
    breadcrumbs: [
      { key: PARENT_COMPONENT_KEY, name: 'Foo', qualifier: ComponentQualifier.Project },
    ],
  }),
): ComponentTree {
  const folderComponent = mockComponent({
    key: `${baseComponent.key}:${FOLDER1_KEY}`,
    name: FOLDER1_KEY,
    path: FOLDER1_KEY,
    qualifier: ComponentQualifier.Directory,
    breadcrumbs: [
      ...baseComponent.breadcrumbs,
      {
        key: `${baseComponent.key}:${FOLDER1_KEY}`,
        name: FOLDER1_KEY,
        qualifier: ComponentQualifier.Directory,
      },
    ],
  });
  return {
    component: baseComponent,
    ancestors: [],
    children: [
      {
        component: folderComponent,
        ancestors: [baseComponent],
        children: [
          {
            component: mockComponent({
              key: `${baseComponent.key}:${FOLDER1_KEY}/${FILE7_KEY}`,
              name: FILE7_KEY,
              path: `${FOLDER1_KEY}/${FILE7_KEY}`,
              qualifier: ComponentQualifier.File,
              breadcrumbs: [
                ...folderComponent.breadcrumbs,
                {
                  key: `${baseComponent.key}:${FOLDER1_KEY}/${FILE7_KEY}`,
                  name: FILE7_KEY,
                  qualifier: ComponentQualifier.File,
                },
              ],
            }),
            ancestors: [baseComponent, folderComponent],
            children: [],
          },
          {
            component: mockComponent({
              key: `${baseComponent.key}:${FOLDER1_KEY}/${FILE8_KEY}`,
              name: FILE8_KEY,
              path: `${FOLDER1_KEY}/${FILE8_KEY}`,
              qualifier: ComponentQualifier.File,
              breadcrumbs: [
                ...folderComponent.breadcrumbs,
                {
                  key: `${baseComponent.key}:${FOLDER1_KEY}/${FILE8_KEY}`,
                  name: FILE8_KEY,
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
          key: `${baseComponent.key}:${FILE1_KEY}`,
          name: FILE1_KEY,
          path: FILE1_KEY,
          qualifier: ComponentQualifier.File,
          breadcrumbs: [
            ...baseComponent.breadcrumbs,
            {
              key: `${baseComponent.key}:${FILE1_KEY}`,
              name: FILE1_KEY,
              qualifier: ComponentQualifier.File,
            },
          ],
        }),
        ancestors: [baseComponent],
        children: [],
      },
      {
        component: mockComponent({
          key: `${baseComponent.key}:${FILE2_KEY}`,
          name: FILE2_KEY,
          path: FILE2_KEY,
          qualifier: ComponentQualifier.File,
          breadcrumbs: [
            ...baseComponent.breadcrumbs,
            {
              key: `${baseComponent.key}:${FILE2_KEY}`,
              name: FILE2_KEY,
              qualifier: ComponentQualifier.File,
            },
          ],
        }),
        ancestors: [baseComponent],
        children: [],
      },
      {
        component: mockComponent({
          key: `${baseComponent.key}:${FILE3_KEY}`,
          name: FILE3_KEY,
          path: FILE3_KEY,
          qualifier: ComponentQualifier.File,
          breadcrumbs: [
            ...baseComponent.breadcrumbs,
            {
              key: `${baseComponent.key}:${FILE3_KEY}`,
              name: FILE3_KEY,
              qualifier: ComponentQualifier.File,
            },
          ],
        }),
        ancestors: [baseComponent],
        children: [],
      },
      {
        component: mockComponent({
          key: `${baseComponent.key}:${FILE4_KEY}`,
          name: FILE4_KEY,
          path: FILE4_KEY,
          qualifier: ComponentQualifier.File,
          breadcrumbs: [
            ...baseComponent.breadcrumbs,
            {
              key: `${baseComponent.key}:${FILE4_KEY}`,
              name: FILE4_KEY,
              qualifier: ComponentQualifier.File,
            },
          ],
        }),
        ancestors: [baseComponent],
        children: [],
      },
      {
        component: mockComponent({
          key: `${baseComponent.key}:${FILE5_KEY}`,
          name: FILE5_KEY,
          path: FILE5_KEY,
          qualifier: ComponentQualifier.File,
          breadcrumbs: [
            ...baseComponent.breadcrumbs,
            {
              key: `${baseComponent.key}:${FILE5_KEY}`,
              name: FILE5_KEY,
              qualifier: ComponentQualifier.File,
            },
          ],
        }),
        ancestors: [baseComponent],
        children: [],
      },
      {
        component: mockComponent({
          key: `${baseComponent.key}:${FILE6_KEY}`,
          name: FILE6_KEY,
          path: FILE6_KEY,
          qualifier: ComponentQualifier.File,
          breadcrumbs: [
            ...baseComponent.breadcrumbs,
            {
              key: `${baseComponent.key}:${FILE6_KEY}`,
              name: FILE6_KEY,
              qualifier: ComponentQualifier.File,
            },
          ],
        }),
        ancestors: [baseComponent],
        children: [],
      },
    ],
  };
}

export function mockFullSourceViewerFileList(baseComponentKey = PARENT_COMPONENT_KEY) {
  return [
    {
      component: mockSourceViewerFile(FILE1_KEY, baseComponentKey),
      lines: times(50, (n) =>
        mockSourceLine({
          line: n,
          code: 'function Test() {}',
        }),
      ),
    },
    {
      component: mockSourceViewerFile(`${FOLDER1_KEY}/${FILE7_KEY}`, baseComponentKey),
      lines: times(50, (n) =>
        mockSourceLine({
          line: n,
          code: 'function Test() {}',
        }),
      ),
    },
    {
      component: mockSourceViewerFile(`${FOLDER1_KEY}/${FILE8_KEY}`, baseComponentKey),
      lines: times(50, (n) =>
        mockSourceLine({
          line: n,
          code: 'function Test() {}',
        }),
      ),
    },
    {
      component: mockSourceViewerFile(FILE2_KEY, baseComponentKey),
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
          '1': mockDuplicatedFile({ key: `${baseComponentKey}:${FILE2_KEY}` }),
          '2': mockDuplicatedFile({ key: `${baseComponentKey}:${FILE3_KEY}` }),
        },
      },
    },
    {
      component: mockSourceViewerFile(FILE3_KEY, baseComponentKey),
      lines: times(50, (n) =>
        mockSourceLine({
          line: n,
          code: `\u003cspan class\u003d"cd"\u003eLine ${n}\u003c/span\u003e`,
        }),
      ),
    },
    {
      component: mockSourceViewerFile(FILE5_KEY, baseComponentKey),
      lines: [],
    },
    {
      component: mockSourceViewerFile(FILE6_KEY, baseComponentKey),
      lines: times(200, (n) =>
        mockSourceLine({
          line: n,
          code: `\u003cspan class\u003d"cd"\u003eLine ${n}\u003c/span\u003e`,
        }),
      ),
    },
    {
      component: mockSourceViewerFile(FILE4_KEY, baseComponentKey),
      lines: times(20, (n) =>
        mockSourceLine({
          line: n,
          code: '  <span class="sym-35 sym">symbole</span>',
        }),
      ),
    },
  ] as SourceFile[];
}
