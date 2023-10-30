/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { cloneDeep, uniqueId } from 'lodash';
import { RuleDescriptionSections } from '../../apps/coding-rules/rule';

import {
  ISSUE_TYPES,
  RESOLUTIONS,
  SEVERITIES,
  SOURCE_SCOPES,
  STATUSES,
} from '../../helpers/constants';
import { mockIssueAuthors, mockIssueChangelog } from '../../helpers/mocks/issues';
import { RequestData } from '../../helpers/request';
import { getStandards } from '../../helpers/security-standard';
import { mockLoggedInUser, mockPaging, mockRuleDetails } from '../../helpers/testMocks';
import {
  CleanCodeAttributeCategory,
  SoftwareImpactSeverity,
  SoftwareQuality,
} from '../../types/clean-code-taxonomy';
import { SearchRulesResponse } from '../../types/coding-rules';
import {
  ASSIGNEE_ME,
  IssueSimpleStatus,
  IssueStatus,
  IssueTransition,
  IssueType,
  ListIssuesResponse,
  RawFacet,
  RawIssue,
  RawIssuesResponse,
  ReferencedComponent,
} from '../../types/issues';
import { SearchRulesQuery } from '../../types/rules';
import { Standards } from '../../types/security';
import { Dict, Rule, RuleActivation, RuleDetails, SnippetsByComponent } from '../../types/types';
import { LoggedInUser, NoticeType, RestUser } from '../../types/users';
import {
  addIssueComment,
  bulkChangeIssues,
  deleteIssueComment,
  editIssueComment,
  getIssueChangelog,
  getIssueFlowSnippets,
  listIssues,
  searchIssueAuthors,
  searchIssueTags,
  searchIssues,
  setIssueAssignee,
  setIssueSeverity,
  setIssueTags,
  setIssueTransition,
  setIssueType,
} from '../issues';
import { getRuleDetails, searchRules } from '../rules';
import { dismissNotice, getCurrentUser, getUsers } from '../users';
import { IssueData, mockIssuesList } from './data/issues';
import { mockRuleList } from './data/rules';

jest.mock('../../api/issues');
// The following 2 mocks are needed, because IssuesServiceMock mocks more than it should.
// This should be removed once IssuesServiceMock is cleaned up.
jest.mock('../../api/rules');
jest.mock('../../api/users');

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

export default class IssuesServiceMock {
  isAdmin = false;
  currentUser: LoggedInUser;
  standards?: Standards;
  defaultList: IssueData[];
  rulesList: Rule[];
  list: IssueData[];

  constructor() {
    this.currentUser = mockLoggedInUser();
    this.defaultList = mockIssuesList();
    this.rulesList = mockRuleList();

    this.list = cloneDeep(this.defaultList);

    jest.mocked(addIssueComment).mockImplementation(this.handleAddComment);
    jest.mocked(bulkChangeIssues).mockImplementation(this.handleBulkChangeIssues);
    jest.mocked(deleteIssueComment).mockImplementation(this.handleDeleteComment);
    jest.mocked(dismissNotice).mockImplementation(this.handleDismissNotification);
    jest.mocked(editIssueComment).mockImplementation(this.handleEditComment);
    jest.mocked(getCurrentUser).mockImplementation(this.handleGetCurrentUser);
    jest.mocked(getIssueChangelog).mockImplementation(this.handleGetIssueChangelog);
    jest.mocked(getIssueFlowSnippets).mockImplementation(this.handleGetIssueFlowSnippets);
    jest.mocked(getRuleDetails).mockImplementation(this.handleGetRuleDetails);
    jest.mocked(listIssues).mockImplementation(this.handleListIssues);
    jest.mocked(searchIssueAuthors).mockImplementation(this.handleSearchIssueAuthors);
    jest.mocked(searchIssues).mockImplementation(this.handleSearchIssues);
    jest.mocked(searchIssueTags).mockImplementation(this.handleSearchIssueTags);
    jest.mocked(searchRules).mockImplementation(this.handleSearchRules);
    jest.mocked(getUsers).mockImplementation(this.handleGetUsers);
    jest.mocked(setIssueAssignee).mockImplementation(this.handleSetIssueAssignee);
    jest.mocked(setIssueSeverity).mockImplementation(this.handleSetIssueSeverity);
    jest.mocked(setIssueTags).mockImplementation(this.handleSetIssueTags);
    jest.mocked(setIssueTransition).mockImplementation(this.handleSetIssueTransition);
    jest.mocked(setIssueType).mockImplementation(this.handleSetIssueType);
  }

  reset = () => {
    this.list = cloneDeep(this.defaultList);
    this.currentUser = mockLoggedInUser();
  };

  setCurrentUser = (user: LoggedInUser) => {
    this.currentUser = user;
  };

  setIssueList = (list: IssueData[]) => {
    this.list = list;
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
    // For now we only check for issue type and status change.
    this.list
      .filter((i) => issueKeys.includes(i.issue.key))
      .forEach((data) => {
        data.issue.type = query.set_type ?? data.issue.type;
        data.issue.status = query.do_transition ?? data.issue.status;
      });
    return this.reply(undefined);
  };

  handleGetIssueFlowSnippets = (issueKey: string): Promise<Dict<SnippetsByComponent>> => {
    const issue = this.list.find((i) => i.issue.key === issueKey);
    if (issue === undefined) {
      return Promise.reject({ errors: [{ msg: `No issue has been found for id ${issueKey}` }] });
    }
    return this.reply(issue.snippets);
  };

  handleSearchRules = (req: SearchRulesQuery): Promise<SearchRulesResponse> => {
    const rules = this.rulesList.filter((rule) => {
      const query = req.q?.toLowerCase() || '';
      const nameMatches = rule.name.toLowerCase().includes(query);
      const keyMatches = rule.key.toLowerCase().includes(query);
      const isTypeRight = req.types?.includes(rule.type);
      return isTypeRight && (nameMatches || keyMatches);
    });
    return this.reply({
      rules,
      paging: mockPaging({
        total: rules.length,
        pageIndex: 1,
        pageSize: 30,
      }),
    });
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

  mockFacetDetailResponse = (query: RequestData): RawFacet[] => {
    const facets = (query.facets ?? '').split(',');
    const cleanCodeCategories: CleanCodeAttributeCategory[] = (
      query.cleanCodeAttributeCategories ?? Object.values(CleanCodeAttributeCategory).join(',')
    ).split(',');
    return facets.map((name: string): RawFacet => {
      if (name === 'owaspTop10-2021') {
        return this.owasp2021FacetList();
      }

      if (name === 'codeVariants') {
        return {
          property: 'codeVariants',
          values: this.list.reduce(
            (acc, { issue }) => {
              if (issue.codeVariants?.length) {
                issue.codeVariants.forEach((codeVariant) => {
                  const item = acc.find(({ val }) => val === codeVariant);
                  if (item) {
                    item.count++;
                  } else {
                    acc.push({
                      val: codeVariant,
                      count: 1,
                    });
                  }
                });
              }
              return acc;
            },
            [] as RawFacet['values'],
          ),
        };
      }

      if (name === 'languages') {
        const counters = {
          [CleanCodeAttributeCategory.Intentional]: { java: 4100, ts: 500 },
          [CleanCodeAttributeCategory.Consistent]: { java: 100, ts: 200 },
          [CleanCodeAttributeCategory.Adaptable]: { java: 21000, ts: 2000 },
          [CleanCodeAttributeCategory.Responsible]: { java: 111, ts: 674 },
        };
        return {
          property: name,
          values: [
            {
              val: 'java',
              count: cleanCodeCategories.reduce<number>(
                (acc, category) => acc + counters[category].java,
                0,
              ),
            },
            {
              val: 'ts',
              count: cleanCodeCategories.reduce<number>(
                (acc, category) => acc + counters[category].ts,
                0,
              ),
            },
          ],
        };
      }

      return {
        property: name,
        values: (
          {
            resolutions: RESOLUTIONS,
            severities: SEVERITIES,
            statuses: STATUSES,
            types: ISSUE_TYPES,
            scopes: SOURCE_SCOPES.map(({ scope }) => scope),
            projects: ['org.project1', 'org.project2'],
            impactSoftwareQualities: Object.values(SoftwareQuality),
            impactSeverities: Object.values(SoftwareImpactSeverity),
            cleanCodeAttributeCategories: cleanCodeCategories,
            tags: ['unused', 'confusing'],
            rules: ['simpleRuleId', 'advancedRuleId', 'other'],
            assignees: ['email1@sonarsource.com', 'email2@sonarsource.com'],
            author: ['email3@sonarsource.com', 'email4@sonarsource.com'],
          }[name] ?? []
        ).map((val) => ({
          val,
          count: 1, // if 0, the facet can't be clicked in tests
        })),
      };
    });
  };

  handleListIssues = (query: RequestData): Promise<ListIssuesResponse> => {
    const filteredList = this.list
      .filter((item) => !query.types || query.types.split(',').includes(item.issue.type))
      .filter(
        (item) =>
          !query.inNewCodePeriod || new Date(item.issue.creationDate) > new Date('2023-01-10'),
      );

    // Splice list items according to paging using a fixed page size
    const pageIndex = query.p || 1;
    const pageSize = 7;
    const listItems = filteredList.slice((pageIndex - 1) * pageSize, pageIndex * pageSize);

    // Generate response
    return this.reply({
      components: generateReferenceComponentsForIssues(filteredList),
      issues: listItems.map((line) => line.issue),
      paging: mockPaging({
        pageIndex,
        pageSize,
        total: filteredList.length,
      }),
      rules: this.rulesList,
    });
  };

  handleSearchIssues = (query: RequestData): Promise<RawIssuesResponse> => {
    const facets = this.mockFacetDetailResponse(query);

    // Filter list (only supports assignee, type and severity)
    const filteredList = this.list
      .filter((item) => {
        if (!query.cleanCodeAttributeCategories) {
          return true;
        }

        return query.cleanCodeAttributeCategories
          .split(',')
          .includes(item.issue.cleanCodeAttributeCategory);
      })
      .filter((item) => {
        if (!query.impactSoftwareQualities) {
          return true;
        }

        return item.issue.impacts.some(({ softwareQuality }) =>
          query.impactSoftwareQualities.split(',').includes(softwareQuality),
        );
      })
      .filter((item) => {
        if (!query.impactSeverities) {
          return true;
        }

        return item.issue.impacts.some(({ severity }) =>
          query.impactSeverities.split(',').includes(severity),
        );
      })
      .filter((item) => {
        if (!query.assignees) {
          return true;
        }
        if (query.assignees === ASSIGNEE_ME) {
          return item.issue.assignee === mockLoggedInUser().login;
        }
        return query.assignees.split(',').includes(item.issue.assignee);
      })
      .filter((item) => {
        if (!query.tags) {
          return true;
        }
        if (!item.issue.tags) {
          return false;
        }
        return item.issue.tags.some((tag) => query.tags?.split(',').includes(tag));
      })
      .filter(
        (item) =>
          !query.createdBefore ||
          new Date(item.issue.creationDate) <= new Date(query.createdBefore),
      )
      .filter(
        (item) =>
          !query.createdAfter || new Date(item.issue.creationDate) >= new Date(query.createdAfter),
      )
      .filter((item) => !query.types || query.types.split(',').includes(item.issue.type))
      .filter(
        (item) => !query.severities || query.severities.split(',').includes(item.issue.severity),
      )
      .filter((item) => !query.scopes || query.scopes.split(',').includes(item.issue.scope))
      .filter((item) => !query.statuses || query.statuses.split(',').includes(item.issue.status))
      .filter((item) => !query.projects || query.projects.split(',').includes(item.issue.project))
      .filter((item) => !query.rules || query.rules.split(',').includes(item.issue.rule))
      .filter(
        (item) =>
          !query.resolutions || query.resolutions.split(',').includes(item.issue.resolution),
      )
      .filter(
        (item) =>
          !query.inNewCodePeriod || new Date(item.issue.creationDate) > new Date('2023-01-10'),
      )
      .filter((item) => {
        if (!query.codeVariants) {
          return true;
        }
        if (!item.issue.codeVariants) {
          return false;
        }
        return item.issue.codeVariants.some(
          (codeVariant) => query.codeVariants?.split(',').includes(codeVariant),
        );
      });

    // Splice list items according to paging using a fixed page size
    const pageIndex = query.p || 1;
    const pageSize = 7;
    const listItems = filteredList.slice((pageIndex - 1) * pageSize, pageIndex * pageSize);

    // Generate response
    return this.reply({
      components: generateReferenceComponentsForIssues(filteredList),
      effortTotal: 199629,
      facets,
      issues: listItems.map((line) => line.issue),
      languages: [{ name: 'java' }, { name: 'python' }, { name: 'ts' }],
      paging: mockPaging({
        pageIndex,
        pageSize,
        total: filteredList.length,
      }),
      rules: this.rulesList,
      users: [
        { login: 'login0' },
        { login: 'login1', name: 'Login 1' },
        { login: 'login2', name: 'Login 2' },
      ],
    });
  };

  handleGetCurrentUser = () => {
    return this.reply(this.currentUser);
  };

  handleDismissNotification = (noticeType: NoticeType) => {
    if ([NoticeType.EDUCATION_PRINCIPLES, NoticeType.ISSUE_GUIDE].includes(noticeType)) {
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
    return this.getActionsResponse(
      { assignee: data.assignee === '_me' ? this.currentUser.login : data.assignee },
      data.issue,
    );
  };

  handleSetIssueTransition = (data: { issue: string; transition: string }) => {
    const simpleStatusMap: { [key: string]: IssueSimpleStatus } = {
      [IssueTransition.Accept]: IssueSimpleStatus.Accepted,
      [IssueTransition.Confirm]: IssueSimpleStatus.Confirmed,
      [IssueTransition.UnConfirm]: IssueSimpleStatus.Open,
      [IssueTransition.Resolve]: IssueSimpleStatus.Fixed,
      [IssueTransition.WontFix]: IssueSimpleStatus.Accepted,
      [IssueTransition.FalsePositive]: IssueSimpleStatus.FalsePositive,
    };

    const transitionMap: Dict<IssueTransition[]> = {
      [IssueSimpleStatus.Open]: [
        IssueTransition.Accept,
        IssueTransition.Confirm,
        IssueTransition.Resolve,
        IssueTransition.FalsePositive,
        IssueTransition.WontFix,
      ],
      [IssueSimpleStatus.Confirmed]: [
        IssueTransition.Accept,
        IssueTransition.Resolve,
        IssueTransition.UnConfirm,
        IssueTransition.FalsePositive,
        IssueTransition.WontFix,
      ],
      [IssueSimpleStatus.FalsePositive]: [IssueTransition.Reopen],
      [IssueSimpleStatus.Accepted]: [IssueTransition.Reopen],
      [IssueSimpleStatus.Fixed]: [IssueTransition.Reopen],
    };

    return this.getActionsResponse(
      {
        simpleStatus: simpleStatusMap[data.transition],
        transitions: transitionMap[simpleStatusMap[data.transition]],
      },
      data.issue,
    );
  };

  handleSetIssueTags = (data: { issue: string; tags: string }) => {
    const tagsArr = data.tags.split(',');
    return this.getActionsResponse({ tags: tagsArr }, data.issue);
  };

  handleAddComment = (data: { issue: string; text: string }) => {
    return this.getActionsResponse(
      {
        comments: [
          {
            createdAt: '2022-07-28T11:30:04+0200',
            htmlText: data.text,
            key: uniqueId(),
            login: 'admin',
            markdown: data.text,
            updatable: true,
          },
        ],
      },
      data.issue,
    );
  };

  handleEditComment = (data: { comment: string; text: string }) => {
    const issueKey = this.list.find((i) => i.issue.comments?.some((c) => c.key === data.comment))
      ?.issue.key;
    if (!issueKey) {
      throw new Error(`Couldn't find issue for comment ${data.comment}`);
    }
    return this.getActionsResponse(
      {
        comments: [
          {
            createdAt: '2022-07-28T11:30:04+0200',
            htmlText: data.text,
            key: data.comment,
            login: 'admin',
            markdown: data.text,
            updatable: true,
          },
        ],
      },
      issueKey,
    );
  };

  handleDeleteComment = (data: { comment: string }) => {
    const issue = this.list.find((i) => i.issue.comments?.some((c) => c.key === data.comment))
      ?.issue;
    if (!issue) {
      throw new Error(`Couldn't find issue for comment ${data.comment}`);
    }
    return this.getActionsResponse(
      {
        comments: issue.comments?.filter((c) => c.key !== data.comment),
      },
      issue.key,
    );
  };

  handleGetUsers = () => {
    return this.reply({
      page: mockPaging(),
      users: [mockLoggedInUser() as unknown as RestUser],
    });
  };

  handleSearchIssueAuthors = () => {
    return this.reply(mockIssueAuthors());
  };

  handleSearchIssueTags = () => {
    return this.reply(['accessibility', 'android', 'unused']);
  };

  handleGetIssueChangelog = (_issue: string) => {
    return this.reply({
      changelog: [
        mockIssueChangelog({
          creationDate: '2018-09-01',
          diffs: [
            {
              key: 'status',
              newValue: IssueStatus.Reopened,
              oldValue: IssueStatus.Confirmed,
            },
          ],
        }),
        mockIssueChangelog({
          creationDate: '2018-10-01',
          diffs: [
            {
              key: 'assign',
              newValue: 'darth.vader',
              oldValue: 'luke.skywalker',
            },
          ],
        }),
      ],
    });
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
