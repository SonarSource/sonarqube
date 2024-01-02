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
import { cloneDeep, keyBy, times } from 'lodash';
import { RuleDescriptionSections } from '../../apps/coding-rules/rule';
import { mockSnippetsByComponent } from '../../helpers/mocks/sources';
import { RequestData } from '../../helpers/request';
import { getStandards } from '../../helpers/security-standard';
import {
  mockCurrentUser,
  mockLoggedInUser,
  mockPaging,
  mockRawIssue,
  mockRuleDetails,
} from '../../helpers/testMocks';
import {
  IssueType,
  RawFacet,
  RawIssue,
  RawIssuesResponse,
  ReferencedComponent,
} from '../../types/issues';
import { Standards } from '../../types/security';
import {
  Dict,
  FlowType,
  RuleActivation,
  RuleDetails,
  SnippetsByComponent,
} from '../../types/types';
import { NoticeType } from '../../types/users';
import {
  addIssueComment,
  bulkChangeIssues,
  deleteIssueComment,
  editIssueComment,
  getIssueFlowSnippets,
  searchIssues,
  searchIssueTags,
  setIssueAssignee,
  setIssueSeverity,
  setIssueTags,
  setIssueTransition,
  setIssueType,
} from '../issues';
import { getRuleDetails } from '../rules';
import { dismissNotice, getCurrentUser, searchUsers } from '../users';

function mockReferenceComponent(override?: Partial<ReferencedComponent>) {
  return {
    key: 'component1',
    name: 'Component1',
    uuid: 'id1',
    enabled: true,
    ...override,
  };
}

function generateReferenceComponentsForIssues(issueData: IssueData[]) {
  return issueData
    .reduce((componentKeys, response) => {
      const componentKey = response.issue.component;
      if (!componentKeys.includes(componentKey)) {
        return [...componentKeys, componentKey];
      }

      return componentKeys;
    }, [] as string[])
    .map((key) => mockReferenceComponent({ key, enabled: true }));
}

interface IssueData {
  issue: RawIssue;
  snippets: Dict<SnippetsByComponent>;
}

export default class IssuesServiceMock {
  isAdmin = false;
  standards?: Standards;
  defaultList: IssueData[];
  list: IssueData[];

  constructor() {
    this.defaultList = [
      {
        issue: mockRawIssue(false, {
          key: 'issue11',
          component: 'foo:test1.js',
          message: 'FlowIssue',
          rule: 'simpleRuleId',
          textRange: {
            startLine: 10,
            endLine: 10,
            startOffset: 0,
            endOffset: 2,
          },
          flows: [
            {
              type: FlowType.DATA,
              description: 'Backtracking 1',
              locations: [
                {
                  component: 'foo:test1.js',
                  msg: 'Data location 1',
                  textRange: {
                    startLine: 20,
                    endLine: 20,
                    startOffset: 0,
                    endOffset: 1,
                  },
                },
                {
                  component: 'foo:test1.js',
                  msg: 'Data location 2',
                  textRange: {
                    startLine: 21,
                    endLine: 21,
                    startOffset: 0,
                    endOffset: 1,
                  },
                },
              ],
            },
            {
              type: FlowType.EXECUTION,
              locations: [
                {
                  component: 'foo:test2.js',
                  msg: 'Execution location 1',
                  textRange: {
                    startLine: 20,
                    endLine: 20,
                    startOffset: 0,
                    endOffset: 1,
                  },
                },
                {
                  component: 'foo:test2.js',
                  msg: 'Execution location 2',
                  textRange: {
                    startLine: 22,
                    endLine: 22,
                    startOffset: 0,
                    endOffset: 1,
                  },
                },
                {
                  component: 'foo:test2.js',
                  msg: 'Execution location 3',
                  textRange: {
                    startLine: 5,
                    endLine: 5,
                    startOffset: 0,
                    endOffset: 1,
                  },
                },
              ],
            },
          ],
        }),
        snippets: keyBy(
          [
            mockSnippetsByComponent(
              'test1.js',
              'foo',
              times(40, (i) => i + 1)
            ),
            mockSnippetsByComponent(
              'test2.js',
              'foo',
              times(40, (i) => i + 1)
            ),
          ],
          'component.key'
        ),
      },
      {
        issue: mockRawIssue(false, {
          key: 'issue0',
          component: 'foo:test1.js',
          message: 'Issue on file',
          rule: 'simpleRuleId',
          textRange: undefined,
          line: undefined,
        }),
        snippets: {},
      },
      {
        issue: mockRawIssue(false, {
          key: 'issue1',
          component: 'foo:test1.js',
          message: 'Fix this',
          rule: 'simpleRuleId',
          textRange: {
            startLine: 10,
            endLine: 10,
            startOffset: 0,
            endOffset: 2,
          },
          flows: [
            {
              locations: [
                {
                  component: 'foo:test1.js',
                  msg: 'location 1',
                  textRange: {
                    startLine: 1,
                    endLine: 1,
                    startOffset: 0,
                    endOffset: 1,
                  },
                },
              ],
            },
            {
              locations: [
                {
                  component: 'foo:test2.js',
                  msg: 'location 2',
                  textRange: {
                    startLine: 20,
                    endLine: 20,
                    startOffset: 0,
                    endOffset: 1,
                  },
                },
              ],
            },
          ],
        }),
        snippets: keyBy(
          [
            mockSnippetsByComponent(
              'test1.js',
              'foo',
              times(40, (i) => i + 1)
            ),
            mockSnippetsByComponent(
              'test2.js',
              'foo',
              times(40, (i) => i + 1)
            ),
          ],
          'component.key'
        ),
      },
      {
        issue: mockRawIssue(false, {
          actions: ['set_type', 'set_tags', 'comment', 'set_severity', 'assign'],
          transitions: ['confirm', 'resolve', 'falsepositive', 'wontfix'],
          key: 'issue2',
          component: 'foo:test2.js',
          message: 'Fix that',
          rule: 'advancedRuleId',
          textRange: {
            startLine: 25,
            endLine: 25,
            startOffset: 0,
            endOffset: 1,
          },
          ruleDescriptionContextKey: 'spring',
        }),
        snippets: keyBy(
          [
            mockSnippetsByComponent(
              'test2.js',
              'foo',
              times(40, (i) => i + 20)
            ),
          ],
          'component.key'
        ),
      },
      {
        issue: mockRawIssue(false, {
          key: 'issue3',
          component: 'foo:test2.js',
          message: 'Second issue',
          rule: 'other',
          textRange: {
            startLine: 28,
            endLine: 28,
            startOffset: 0,
            endOffset: 1,
          },
        }),
        snippets: keyBy(
          [
            mockSnippetsByComponent(
              'test2.js',
              'foo',
              times(40, (i) => i + 20)
            ),
          ],
          'component.key'
        ),
      },
      {
        issue: mockRawIssue(false, {
          actions: ['set_type', 'set_tags', 'comment', 'set_severity', 'assign'],
          transitions: ['confirm', 'resolve', 'falsepositive', 'wontfix'],
          key: 'issue4',
          component: 'foo:test2.js',
          message: 'Issue with tags',
          rule: 'external_eslint_repo:no-div-regex',
          textRange: {
            startLine: 25,
            endLine: 25,
            startOffset: 0,
            endOffset: 1,
          },
          ruleDescriptionContextKey: 'spring',
          ruleStatus: 'DEPRECATED',
          quickFixAvailable: true,
        }),
        snippets: keyBy(
          [
            mockSnippetsByComponent(
              'test2.js',
              'foo',
              times(40, (i) => i + 20)
            ),
          ],
          'component.key'
        ),
      },
    ];

    this.list = cloneDeep(this.defaultList);

    (searchIssues as jest.Mock).mockImplementation(this.handleSearchIssues);
    (getRuleDetails as jest.Mock).mockImplementation(this.handleGetRuleDetails);
    (getIssueFlowSnippets as jest.Mock).mockImplementation(this.handleGetIssueFlowSnippets);
    (bulkChangeIssues as jest.Mock).mockImplementation(this.handleBulkChangeIssues);
    (getCurrentUser as jest.Mock).mockImplementation(this.handleGetCurrentUser);
    (dismissNotice as jest.Mock).mockImplementation(this.handleDismissNotification);
    (setIssueType as jest.Mock).mockImplementation(this.handleSetIssueType);
    (setIssueAssignee as jest.Mock).mockImplementation(this.handleSetIssueAssignee);
    (setIssueSeverity as jest.Mock).mockImplementation(this.handleSetIssueSeverity);
    (setIssueTransition as jest.Mock).mockImplementation(this.handleSetIssueTransition);
    (setIssueTags as jest.Mock).mockImplementation(this.handleSetIssueTags);
    (addIssueComment as jest.Mock).mockImplementation(this.handleAddComment);
    (editIssueComment as jest.Mock).mockImplementation(this.handleEditComment);
    (deleteIssueComment as jest.Mock).mockImplementation(this.handleDeleteComment);
    (searchUsers as jest.Mock).mockImplementation(this.handleSearchUsers);
    (searchIssueTags as jest.Mock).mockImplementation(this.handleSearchIssueTags);
  }

  reset = () => {
    this.list = cloneDeep(this.defaultList);
  };

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
      values: [{ val: 'a1', count: 0 }],
    };
  }

  setIsAdmin(isAdmin: boolean) {
    this.isAdmin = isAdmin;
  }

  handleBulkChangeIssues = (issueKeys: string[], query: RequestData) => {
    //For now we only check for issue type change.
    this.list
      .filter((i) => issueKeys.includes(i.issue.key))
      .forEach((data) => {
        data.issue.type = query.set_type;
      });
    return this.reply({});
  };

  handleGetIssueFlowSnippets = (issueKey: string): Promise<Dict<SnippetsByComponent>> => {
    const issue = this.list.find((i) => i.issue.key === issueKey);
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
          htmlNote: '<h1>Extended Description</h1>',
          educationPrinciples: ['defense_in_depth'],
          descriptionSections: [
            { key: RuleDescriptionSections.INTRODUCTION, content: '<h1>Into</h1>' },
            { key: RuleDescriptionSections.ROOT_CAUSE, content: '<h1>Because</h1>' },
            { key: RuleDescriptionSections.HOW_TO_FIX, content: '<h1>Fix with</h1>' },
            {
              content: '<p> Context 1 content<p>',
              key: RuleDescriptionSections.HOW_TO_FIX,
              context: {
                key: 'spring',
                displayName: 'Spring',
              },
            },
            {
              content: '<p> Context 2 content<p>',
              key: RuleDescriptionSections.HOW_TO_FIX,
              context: {
                key: 'context_2',
                displayName: 'Context 2',
              },
            },
            {
              content: '<p> Context 3 content<p>',
              key: RuleDescriptionSections.HOW_TO_FIX,
              context: {
                key: 'context_3',
                displayName: 'Context 3',
              },
            },
            { key: RuleDescriptionSections.RESOURCES, content: '<h1>Link</h1>' },
          ],
        }),
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
            content: '<h1>Default</h1> Default description',
          },
        ],
      }),
    });
  };

  handleSearchIssues = (query: RequestData): Promise<RawIssuesResponse> => {
    const facets = (query.facets ?? '').split(',').map((name: string) => {
      if (name === 'owaspTop10-2021') {
        return this.owasp2021FacetList();
      }
      return {
        property: name,
        values: [],
      };
    });
    return this.reply({
      components: generateReferenceComponentsForIssues(this.list),
      effortTotal: 199629,
      facets,
      issues: this.list.map((line) => line.issue),
      languages: [],
      paging: mockPaging(),
    });
  };

  handleGetCurrentUser = () => {
    return this.reply(mockCurrentUser());
  };

  handleDismissNotification = (noticeType: NoticeType) => {
    if (noticeType === NoticeType.EDUCATION_PRINCIPLES) {
      return this.reply(true);
    }

    return Promise.reject();
  };

  handleSetIssueType = (data: { issue: string; type: IssueType }) => {
    return this.getActionsResponse({ type: data.type }, data.issue);
  };

  handleSetIssueSeverity = (data: { issue: string; severity: string }) => {
    return this.getActionsResponse({ severity: data.severity }, data.issue);
  };

  handleSetIssueAssignee = (data: { issue: string; assignee?: string }) => {
    return this.getActionsResponse({ assignee: data.assignee }, data.issue);
  };

  handleSetIssueTransition = (data: { issue: string; transition: string }) => {
    const statusMap: { [key: string]: string } = {
      confirm: 'CONFIRMED',
      unconfirm: 'REOPENED',
      resolve: 'RESOLVED',
      wontfix: 'RESOLVED',
      falsepositive: 'RESOLVED',
    };
    const transitionMap: Dict<string[]> = {
      REOPENED: ['confirm', 'resolve', 'falsepositive', 'wontfix'],
      OPEN: ['confirm', 'resolve', 'falsepositive', 'wontfix'],
      CONFIRMED: ['resolve', 'unconfirm', 'falsepositive', 'wontfix'],
      RESOLVED: ['reopen'],
    };

    const resolutionMap: Dict<string> = {
      wontfix: 'WONTFIX',
      falsepositive: 'FALSE-POSITIVE',
    };

    return this.getActionsResponse(
      {
        status: statusMap[data.transition],
        transitions: transitionMap[statusMap[data.transition]],
        resolution: resolutionMap[data.transition],
      },
      data.issue
    );
  };

  handleSetIssueTags = (data: { issue: string; tags: string }) => {
    const tagsArr = data.tags.split(',');
    return this.getActionsResponse({ tags: tagsArr }, data.issue);
  };

  handleAddComment = (data: { issue: string; text: string }) => {
    // For comment its little more complex to get comment Id
    return this.getActionsResponse(
      {
        comments: [
          {
            createdAt: '2022-07-28T11:30:04+0200',
            htmlText: data.text,
            key: '1234',
            login: 'admin',
            markdown: data.text,
            updatable: true,
          },
        ],
      },
      data.issue
    );
  };

  handleEditComment = (data: { comment: string; text: string }) => {
    // For comment its little more complex to get comment Id
    return this.getActionsResponse(
      {
        comments: [
          {
            createdAt: '2022-07-28T11:30:04+0200',
            htmlText: data.text,
            key: '1234',
            login: 'admin',
            markdown: data.text,
            updatable: true,
          },
        ],
      },
      'issue2'
    );
  };

  handleDeleteComment = () => {
    // For comment its little more complex to get comment Id
    return this.getActionsResponse(
      {
        comments: [],
      },
      'issue2'
    );
  };

  handleSearchUsers = () => {
    return this.reply({ users: [mockLoggedInUser()] });
  };

  handleSearchIssueTags = () => {
    return this.reply(['accessibility', 'android']);
  };

  getActionsResponse = (overrides: Partial<RawIssue>, issueKey: string) => {
    const issueDataSelected = this.list.find((l) => l.issue.key === issueKey);

    if (!issueDataSelected) {
      throw new Error(`Coulnd't find issue for key ${issueKey}`);
    }

    issueDataSelected.issue = {
      ...issueDataSelected.issue,
      ...overrides,
    };

    return this.reply({
      issue: issueDataSelected.issue,
    });
  };

  reply<T>(response: T): Promise<T> {
    return Promise.resolve(cloneDeep(response));
  }
}
