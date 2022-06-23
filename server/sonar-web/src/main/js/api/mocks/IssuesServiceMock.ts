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
import { cloneDeep, keyBy, range, times } from 'lodash';
import { RuleDescriptionSections } from '../../apps/coding-rules/rule';
import {
  mockSnippetsByComponent,
  mockSourceLine,
  mockSourceViewerFile
} from '../../helpers/mocks/sources';
import { RequestData } from '../../helpers/request';
import { getStandards } from '../../helpers/security-standard';
import { mockPaging, mockRawIssue, mockRuleDetails } from '../../helpers/testMocks';
import { BranchParameters } from '../../types/branch-like';
import { RawFacet, RawIssue, RawIssuesResponse, ReferencedComponent } from '../../types/issues';
import { Standards } from '../../types/security';
import {
  Dict,
  RuleActivation,
  RuleDetails,
  SnippetsByComponent,
  SourceViewerFile
} from '../../types/types';
import { getComponentForSourceViewer, getSources } from '../components';
import { getIssueFlowSnippets, searchIssues } from '../issues';
import { getRuleDetails } from '../rules';

function mockReferenceComponent(override?: Partial<ReferencedComponent>) {
  return {
    key: 'component1',
    name: 'Component1',
    uuid: 'id1',
    ...override
  };
}

interface IssueData {
  issue: RawIssue;
  snippets: Dict<SnippetsByComponent>;
}

export default class IssuesServiceMock {
  isAdmin = false;
  standards?: Standards;
  sourceViewerFiles: SourceViewerFile[];
  list: IssueData[];

  constructor() {
    this.sourceViewerFiles = [
      mockSourceViewerFile('file.foo', 'project'),
      mockSourceViewerFile('file.bar', 'project')
    ];
    this.list = [
      {
        issue: mockRawIssue(false, {
          key: 'issue0',
          component: 'project:file.foo',
          message: 'Issue on file',
          rule: 'simpleRuleId',
          textRange: undefined,
          line: undefined
        }),
        snippets: {}
      },
      {
        issue: mockRawIssue(false, {
          key: 'issue1',
          component: 'project:file.foo',
          message: 'Fix this',
          rule: 'simpleRuleId',
          textRange: {
            startLine: 10,
            endLine: 10,
            startOffset: 0,
            endOffset: 2
          },
          flows: [
            {
              locations: [
                {
                  component: 'project:file.foo',
                  textRange: {
                    startLine: 1,
                    endLine: 1,
                    startOffset: 0,
                    endOffset: 1
                  }
                }
              ]
            },
            {
              locations: [
                {
                  component: 'project:file.bar',
                  textRange: {
                    startLine: 20,
                    endLine: 20,
                    startOffset: 0,
                    endOffset: 1
                  }
                }
              ]
            }
          ]
        }),
        snippets: keyBy(
          [
            mockSnippetsByComponent(
              'file.foo',
              'project',
              times(40, i => i + 1)
            ),
            mockSnippetsByComponent(
              'file.bar',
              'project',
              times(40, i => i + 1)
            )
          ],
          'component.key'
        )
      },
      {
        issue: mockRawIssue(false, {
          key: 'issue2',
          component: 'project:file.bar',
          message: 'Fix that',
          rule: 'advancedRuleId',
          textRange: {
            startLine: 25,
            endLine: 25,
            startOffset: 0,
            endOffset: 1
          }
        }),
        snippets: keyBy(
          [
            mockSnippetsByComponent(
              'file.bar',
              'project',
              times(40, i => i + 20)
            )
          ],
          'component.key'
        )
      },
      {
        issue: mockRawIssue(false, {
          key: 'issue3',
          component: 'project:file.bar',
          message: 'Second issue',
          rule: 'other',
          textRange: {
            startLine: 28,
            endLine: 28,
            startOffset: 0,
            endOffset: 1
          }
        }),
        snippets: keyBy(
          [
            mockSnippetsByComponent(
              'file.bar',
              'project',
              times(40, i => i + 20)
            )
          ],
          'component.key'
        )
      }
    ];
    (searchIssues as jest.Mock).mockImplementation(this.handleSearchIssues);
    (getRuleDetails as jest.Mock).mockImplementation(this.handleGetRuleDetails);
    (getIssueFlowSnippets as jest.Mock).mockImplementation(this.handleGetIssueFlowSnippets);
    (getSources as jest.Mock).mockImplementation(this.handleGetSources);
    (getComponentForSourceViewer as jest.Mock).mockImplementation(
      this.handleGetComponentForSourceViewer
    );
  }

  async getStandards(): Promise<Standards> {
    if (this.standards) {
      return this.standards;
    }
    this.standards = await getStandards();
    return this.standards;
  }

  owasp2021FacetList(): RawFacet {
    return {
      property: 'owaspTop10-2021',
      values: [{ val: 'a1', count: 0 }]
    };
  }

  setIsAdmin(isAdmin: boolean) {
    this.isAdmin = isAdmin;
  }

  handleGetSources = (data: { key: string; from?: number; to?: number } & BranchParameters) => {
    return this.reply(range(data.from || 1, data.to || 10).map(line => mockSourceLine({ line })));
  };

  handleGetComponentForSourceViewer = (data: { component: string } & BranchParameters) => {
    const file = this.sourceViewerFiles.find(f => f.key === data.component);
    if (file === undefined) {
      return Promise.reject({
        errors: [{ msg: `No source file has been found for id ${data.component}` }]
      });
    }

    return this.reply(file);
  };

  handleGetIssueFlowSnippets = (issueKey: string): Promise<Dict<SnippetsByComponent>> => {
    const issue = this.list.find(i => i.issue.key === issueKey);
    if (issue === undefined) {
      return Promise.reject({ errors: [{ msg: `No issue has been found for id ${issueKey}` }] });
    }
    return this.reply(issue.snippets);
  };

  handleGetRuleDetails = (parameters: {
    actives?: boolean;
    key: string;
  }): Promise<{ actives?: RuleActivation[]; rule: RuleDetails }> => {
    if (parameters.key === 'advancedRuleId') {
      return this.reply({
        rule: mockRuleDetails({
          key: parameters.key,
          name: 'Advanced rule',
          descriptionSections: [
            { key: RuleDescriptionSections.INTRODUCTION, content: '<h1>Into</h1>' },
            { key: RuleDescriptionSections.ROOT_CAUSE, content: '<h1>Because</h1>' },
            { key: RuleDescriptionSections.HOW_TO_FIX, content: '<h1>Fix with</h1>' },
            {
              content: '<p> Context 1 content<p>',
              key: RuleDescriptionSections.HOW_TO_FIX,
              context: {
                displayName: 'Spring'
              }
            },
            {
              content: '<p> Context 2 content<p>',
              key: RuleDescriptionSections.HOW_TO_FIX,
              context: {
                displayName: 'Context 2'
              }
            },
            {
              content: '<p> Context 3 content<p>',
              key: RuleDescriptionSections.HOW_TO_FIX,
              context: {
                displayName: 'Context 3'
              }
            },
            { key: RuleDescriptionSections.RESOURCES, content: '<h1>Link</h1>' }
          ]
        })
      });
    }
    return this.reply({
      rule: mockRuleDetails({
        key: parameters.key,
        name: 'Simple rule',
        htmlNote: '<h1>Note</h1>',
        descriptionSections: [
          {
            key: RuleDescriptionSections.DEFAULT,
            content: '<h1>Default</h1> Default description'
          }
        ]
      })
    });
  };

  handleSearchIssues = (query: RequestData): Promise<RawIssuesResponse> => {
    const facets = (query.facets ?? '').split(',').map((name: string) => {
      if (name === 'owaspTop10-2021') {
        return this.owasp2021FacetList();
      }
      return {
        property: name,
        values: []
      };
    });
    return this.reply({
      components: [mockReferenceComponent()],
      effortTotal: 199629,
      facets,
      issues: this.list.map(line => line.issue),
      languages: [],
      paging: mockPaging()
    });
  };

  reply<T>(response: T): Promise<T> {
    return Promise.resolve(cloneDeep(response));
  }
}
