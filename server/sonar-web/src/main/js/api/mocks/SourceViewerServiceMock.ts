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
/* eslint-disable no-template-curly-in-string */

import { cloneDeep, pick, times } from 'lodash';
import {
  getComponentData,
  getComponentForSourceViewer,
  getDuplications,
  getSources
} from '../../api/components';
import { mockSourceLine } from '../../helpers/mocks/sources';
import { BranchParameters } from '../../types/branch-like';
import { Dict } from '../../types/types';

function mockSourceFileView(name: string) {
  return {
    component: {
      key: `project:${name}`,
      name,
      qualifier: 'FIL',
      path: name,
      language: 'js',
      analysisDate: '2019-08-08T12:15:12+0200',
      leakPeriodDate: '2018-08-07T11:22:22+0200',
      version: '1.2-SNAPSHOT',
      needIssueSync: false
    },
    sourceFileView: {
      key: `project:${name}`,
      uuid: 'AWMgNpveti8CNlpVyHAm',
      path: name,
      name,
      longName: name,
      q: 'FIL',
      project: 'project',
      projectName: 'Test project',
      fav: false,
      canMarkAsFavorite: true,
      measures: { lines: '0', issues: '0' }
    }
  };
}

const ANCESTORS = [
  {
    key: 'project',
    name: 'Test project',
    description: '',
    qualifier: 'TRK',
    analysisDate: '2019-08-08T12:15:12+0200',
    tags: [],
    visibility: 'public',
    leakPeriodDate: '2018-08-07T11:22:22+0200',
    version: '1.2-SNAPSHOT',
    needIssueSync: false
  }
];
const FILES: Dict<any> = {
  'project:test3.js': {
    ...mockSourceFileView('test3.js'),
    sources: [],
    ancestors: ANCESTORS
  },
  'project:test2.js': {
    ...mockSourceFileView('test2.js'),
    sources: times(200, n =>
      mockSourceLine({
        line: n,
        code: `\u003cspan class\u003d"cd"\u003eLine ${n}\u003c/span\u003e`
      })
    ),
    ancestors: ANCESTORS
  },
  'project:test.js': {
    ...mockSourceFileView('test.js'),
    sources: [
      {
        line: 1,
        code: '\u003cspan class\u003d"cd"\u003e/*\u003c/span\u003e',
        scmRevision: 'f09ee6b610528aa37b7b51be395c93524cebae8f',
        scmAuthor: 'stas.vilchik@sonarsource.com',
        scmDate: '2018-07-10T20:21:20+0200',
        duplicated: false,
        isNew: false,
        lineHits: 1,
        coveredConditions: 1
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
        conditions: 1
      },
      {
        line: 3,
        code: '\u003cspan class\u003d"cd"\u003e * Copyright\u003c/span\u003e',
        scmRevision: '89a3d21bc28f2fa6201b5e8b1185d5358481b3dd',
        scmAuthor: 'pierre.guillot@sonarsource.com',
        scmDate: '2022-01-28T21:03:07+0100',
        duplicated: false,
        isNew: false,
        lineHits: 1
      },
      {
        line: 4,
        code:
          '\u003cspan class\u003d"cd"\u003e * mailto:info AT sonarsource DOT com\u003c/span\u003e',
        scmRevision: 'f09ee6b610528aa37b7b51be395c93524cebae8f',
        scmAuthor: 'stas.vilchik@sonarsource.com',
        duplicated: false,
        isNew: false,
        lineHits: 1,
        conditions: 1,
        coveredConditions: 1
      },
      {
        line: 5,
        code: '\u003cspan class\u003d"cd"\u003e * 5\u003c/span\u003e',
        scmRevision: 'f04ee6b610528aa37b7b51be395c93524cebae8f',
        duplicated: false,
        isNew: false,
        lineHits: 2,
        conditions: 2,
        coveredConditions: 1
      },
      {
        line: 6,
        code: '\u003cspan class\u003d"cd"\u003e * 6\u003c/span\u003e',
        scmRevision: 'f04ee6b610528aa37b7b51be395c93524cebae8f',
        duplicated: false,
        isNew: false,
        lineHits: 0
      },
      {
        line: 7,
        code: '\u003cspan class\u003d"cd"\u003e * 7\u003c/span\u003e',
        scmRevision: 'f04ee6b610528aa37b7b51be395c93524cebae8f',
        duplicated: true,
        isNew: true
      },
      {
        code:
          '\u003cspan class\u003d"cd"\u003e * This program is free software; you can redistribute it and/or\u003c/span\u003e',
        scmRevision: 'f09ee6b610528aa37b7b51be395c93524cebae8f',
        scmAuthor: 'stas.vilchik@sonarsource.com',
        scmDate: '2018-07-10T20:21:20+0200',
        duplicated: false,
        isNew: false
      }
    ],
    ancestors: ANCESTORS,
    duplications: [
      {
        blocks: [
          { from: 7, size: 1, _ref: '1' },
          { from: 1, size: 1, _ref: '2' }
        ]
      }
    ],
    files: {
      '1': {
        key: 'project:test.js',
        name: 'test.js',
        uuid: 'AX8NSmj8EGYw5-dyy63J',
        project: 'project',
        projectUuid: 'AX7juKJqVeQLJMPyb_b-',
        projectName: 'project'
      },
      '2': {
        key: 'project:test2.js',
        name: 'test2.js',
        uuid: 'BX8NSmj8EGYw5-dyy63J',
        project: 'project',
        projectUuid: 'AX7juKJqVeQLJMPyb_b-',
        projectName: 'project'
      }
    }
  }
};

export class SourceViewerServiceMock {
  constructor() {
    (getComponentData as jest.Mock).mockImplementation(this.handleGetComponentData);
    (getComponentForSourceViewer as jest.Mock).mockImplementation(
      this.handleGetComponentForSourceViewer
    );
    (getDuplications as jest.Mock).mockImplementation(this.handleGetDuplications);
    (getSources as jest.Mock).mockImplementation(this.handleGetSources);
  }

  getHugeFile(): string {
    return 'project:test2.js';
  }

  getEmptyFile(): string {
    return 'project:test3.js';
  }

  getFileWithSource(): string {
    return 'project:test.js';
  }

  handleGetSources = (data: { key: string; from?: number; to?: number } & BranchParameters) => {
    const { sources } = FILES[data.key];
    const from = data.from || 1;
    const to = data.to || sources.length;
    return this.reply(sources.slice(from - 1, to));
  };

  handleGetDuplications = (data: { key: string } & BranchParameters) => {
    return this.reply(pick(FILES[data.key], ['duplications', 'files']));
  };

  handleGetComponentForSourceViewer = (data: { component: string } & BranchParameters) => {
    return this.reply(FILES[data.component]['sourceFileView']);
  };

  handleGetComponentData = (data: { component: string } & BranchParameters) => {
    return this.reply(pick(FILES[data.component], ['component', 'ancestor']));
  };

  reply<T>(response: T): Promise<T> {
    return Promise.resolve(cloneDeep(response));
  }
}
