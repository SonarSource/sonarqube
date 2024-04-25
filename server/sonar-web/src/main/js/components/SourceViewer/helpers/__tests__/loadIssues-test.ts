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
import { ComponentQualifier } from '~sonar-aligned/types/component';
import { mockMainBranch } from '../../../../helpers/mocks/branch-like';
import { mockRawIssue } from '../../../../helpers/testMocks';
import { IssueStatus, IssueType } from '../../../../types/issues';
import loadIssues from '../loadIssues';

const mockListResolvedValue = {
  components: [
    {
      enabled: true,
      key: 'org.sonarsource.java:java',
      longName: 'SonarJava',
      name: 'SonarJava',
      qualifier: ComponentQualifier.Project,
    },
    {
      enabled: true,
      key: 'foo.java',
      longName: 'Foo.java',
      name: 'foo.java',
      path: '/foo.java',
      qualifier: ComponentQualifier.File,
    },
  ],
  issues: [
    mockRawIssue(false, {
      actions: ['set_tags', 'comment', 'assign'],
      assignee: 'luke',
      author: 'luke@sonarsource.com',
      comments: [],
      component: 'foo.java',
      creationDate: '2016-08-15T15:25:38+0200',
      flows: [],
      key: 'AWaqVGl3tut9VbnJvk6M',
      line: 62,
      message: 'Make sure this file handling is safe here.',
      project: 'org.sonarsource.java:java',
      rule: 'squid:S4797',
      status: 'OPEN',
      issueStatus: IssueStatus.Open,
      tags: ['cert', 'cwe', 'owasp-a1', 'owasp-a3'],
      textRange: { startLine: 62, endLine: 62, startOffset: 93, endOffset: 96 },
      transitions: [],
      type: IssueType.SecurityHotspot,
    }),
  ],
  paging: { pageIndex: 1, pageSize: 500, total: 1 },
};

const mockSearchResolvedValue = {
  ...mockListResolvedValue,
  debtTotal: 15,
  effortTotal: 15,
  facets: [],
  languages: [{ key: 'java', name: 'Java' }],
  rules: [
    {
      key: 'squid:S4797',
      lang: 'java',
      langName: 'Java',
      name: 'Handling files is security-sensitive',
      status: 'READY',
    },
  ],
  users: [{ active: true, avatar: 'lukavatar', login: 'luke', name: 'Luke' }],
};

jest.mock('../../../../api/issues', () => ({
  listIssues: jest.fn().mockImplementation(() => Promise.resolve(mockListResolvedValue)),
  searchIssues: jest.fn().mockImplementation(() => Promise.resolve(mockSearchResolvedValue)),
}));

describe('loadIssues', () => {
  it('should load issues with searchIssues if not re-indexing', async () => {
    const result = await loadIssues('foo.java', mockMainBranch());

    expect(result).toMatchSnapshot();
  });

  it('should load issues with listIssues if re-indexing', async () => {
    const result = await loadIssues('foo.java', mockMainBranch(), true);
    expect(result).toMatchSnapshot();
  });
});
