/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { mockMainBranch } from '../../../../helpers/testMocks';
import loadIssues from '../loadIssues';

jest.mock('../../../../api/issues', () => ({
  searchIssues: jest.fn().mockResolvedValue({
    paging: { pageIndex: 1, pageSize: 500, total: 1 },
    effortTotal: 15,
    debtTotal: 15,
    issues: [
      {
        key: 'AWaqVGl3tut9VbnJvk6M',
        rule: 'squid:S4797',
        component: 'foo.java',
        project: 'org.sonarsource.java:java',
        line: 62,
        hash: '78417dcee7ba927b7e7c9161e29e02b8',
        textRange: { startLine: 62, endLine: 62, startOffset: 93, endOffset: 96 },
        flows: [],
        status: 'OPEN',
        message: 'Make sure this file handling is safe here.',
        assignee: 'luke',
        author: 'luke@sonarsource.com',
        tags: ['cert', 'cwe', 'owasp-a1', 'owasp-a3'],
        transitions: [],
        actions: ['set_tags', 'comment', 'assign'],
        comments: [],
        creationDate: '2016-08-15T15:25:38+0200',
        updateDate: '2018-10-25T10:23:08+0200',
        type: 'SECURITY_HOTSPOT',
        organization: 'default-organization',
        fromHotspot: true
      }
    ],
    components: [
      {
        organization: 'default-organization',
        key: 'org.sonarsource.java:java',
        enabled: true,
        qualifier: 'TRK',
        name: 'SonarJava',
        longName: 'SonarJava'
      },
      {
        organization: 'default-organization',
        key: 'foo.java',
        enabled: true,
        qualifier: 'FIL',
        name: 'foo.java',
        longName: 'Foo.java',
        path: '/foo.java'
      }
    ],
    rules: [
      {
        key: 'squid:S4797',
        name: 'Handling files is security-sensitive',
        lang: 'java',
        status: 'READY',
        langName: 'Java'
      }
    ],
    users: [{ login: 'luke', name: 'Luke', avatar: 'lukavatar', active: true }],
    languages: [{ key: 'java', name: 'Java' }],
    facets: []
  })
}));

describe('loadIssues', () => {
  it('should load issues', async () => {
    const result = await loadIssues('foo.java', 1, 500, mockMainBranch());
    expect(result).toMatchSnapshot();
  });
});
