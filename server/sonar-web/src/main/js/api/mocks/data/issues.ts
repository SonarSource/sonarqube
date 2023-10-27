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

import { keyBy, times } from 'lodash';
import { mockSnippetsByComponent } from '../../../helpers/mocks/sources';
import { mockLoggedInUser, mockRawIssue } from '../../../helpers/testMocks';
import {
  CleanCodeAttributeCategory,
  SoftwareImpactSeverity,
  SoftwareQuality,
} from '../../../types/clean-code-taxonomy';
import {
  IssueActions,
  IssueResolution,
  IssueScope,
  IssueSeverity,
  IssueSimpleStatus,
  IssueStatus,
  IssueTransition,
  IssueType,
  RawIssue,
} from '../../../types/issues';
import { Dict, FlowType, SnippetsByComponent } from '../../../types/types';
import {
  ISSUE_0,
  ISSUE_1,
  ISSUE_101,
  ISSUE_11,
  ISSUE_1101,
  ISSUE_1102,
  ISSUE_1103,
  ISSUE_2,
  ISSUE_3,
  ISSUE_4,
  ISSUE_TO_FILES,
  ISSUE_TO_RULE,
  PARENT_COMPONENT_KEY,
} from './ids';

export interface IssueData {
  issue: RawIssue;
  snippets: Dict<SnippetsByComponent>;
}

export function mockIssuesList(baseComponentKey = PARENT_COMPONENT_KEY): IssueData[] {
  return [
    {
      issue: mockRawIssue(false, {
        key: ISSUE_101,
        component: `${baseComponentKey}:${ISSUE_TO_FILES[ISSUE_101][0]}`,
        creationDate: '2023-01-05T09:36:01+0100',
        message: 'Issue with no location message',
        cleanCodeAttributeCategory: CleanCodeAttributeCategory.Consistent,
        type: IssueType.Vulnerability,
        rule: ISSUE_TO_RULE[ISSUE_101],
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
                component: `${baseComponentKey}:${ISSUE_TO_FILES[ISSUE_101][0]}`,
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
                component: `${baseComponentKey}:${ISSUE_TO_FILES[ISSUE_101][1]}`,
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
        resolution: IssueResolution.WontFix,
        scope: IssueScope.Main,
        tags: ['tag0', 'tag1'],
      }),
      snippets: keyBy(
        [
          mockSnippetsByComponent(
            ISSUE_TO_FILES[ISSUE_101][0],
            baseComponentKey,
            times(40, (i) => i + 1),
          ),
          mockSnippetsByComponent(
            ISSUE_TO_FILES[ISSUE_101][1],
            baseComponentKey,
            times(40, (i) => i + 1),
          ),
        ],
        'component.key',
      ),
    },
    {
      issue: mockRawIssue(false, {
        key: ISSUE_11,
        component: `${baseComponentKey}:${ISSUE_TO_FILES[ISSUE_11][0]}`,
        creationDate: '2022-01-01T09:36:01+0100',
        message: 'FlowIssue',
        type: IssueType.CodeSmell,
        severity: IssueSeverity.Minor,
        rule: ISSUE_TO_RULE[ISSUE_11],
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
                component: `${baseComponentKey}:${ISSUE_TO_FILES[ISSUE_11][0]}`,
                msg: 'Data location 1',
                textRange: {
                  startLine: 20,
                  endLine: 20,
                  startOffset: 0,
                  endOffset: 1,
                },
              },
              {
                component: `${baseComponentKey}:${ISSUE_TO_FILES[ISSUE_11][0]}`,
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
                component: `${baseComponentKey}:${ISSUE_TO_FILES[ISSUE_11][1]}`,
                msg: 'Execution location 1',
                textRange: {
                  startLine: 20,
                  endLine: 20,
                  startOffset: 0,
                  endOffset: 1,
                },
              },
              {
                component: `${baseComponentKey}:${ISSUE_TO_FILES[ISSUE_11][1]}`,
                msg: 'Execution location 2',
                textRange: {
                  startLine: 22,
                  endLine: 22,
                  startOffset: 0,
                  endOffset: 1,
                },
              },
              {
                component: `${baseComponentKey}:${ISSUE_TO_FILES[ISSUE_11][1]}`,
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
        tags: ['tag1'],
      }),
      snippets: keyBy(
        [
          mockSnippetsByComponent(
            ISSUE_TO_FILES[ISSUE_11][0],
            baseComponentKey,
            times(40, (i) => i + 1),
          ),
          mockSnippetsByComponent(
            ISSUE_TO_FILES[ISSUE_11][1],
            baseComponentKey,
            times(40, (i) => i + 1),
          ),
        ],
        'component.key',
      ),
    },
    {
      issue: mockRawIssue(false, {
        key: ISSUE_0,
        component: `${baseComponentKey}:${ISSUE_TO_FILES[ISSUE_0][0]}`,
        message: 'Issue on file',
        assignee: mockLoggedInUser().login,
        rule: ISSUE_TO_RULE[ISSUE_0],
        textRange: undefined,
        line: undefined,
        scope: IssueScope.Test,
      }),
      snippets: {},
    },
    {
      issue: mockRawIssue(false, {
        key: ISSUE_1,
        component: `${baseComponentKey}:${ISSUE_TO_FILES[ISSUE_1][0]}`,
        message: 'Fix this',
        type: IssueType.Vulnerability,
        scope: IssueScope.Test,
        rule: ISSUE_TO_RULE[ISSUE_1],
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
                component: `${baseComponentKey}:${ISSUE_TO_FILES[ISSUE_1][0]}`,
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
                component: `${baseComponentKey}:${ISSUE_TO_FILES[ISSUE_1][0]}`,
                msg: 'location 2',
                textRange: {
                  startLine: 50,
                  endLine: 50,
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
            ISSUE_TO_FILES[ISSUE_1][0],
            baseComponentKey,
            times(80, (i) => i + 1),
          ),
          mockSnippetsByComponent(
            ISSUE_TO_FILES[ISSUE_1][0],
            baseComponentKey,
            times(80, (i) => i + 1),
          ),
        ],
        'component.key',
      ),
    },
    {
      issue: mockRawIssue(false, {
        key: ISSUE_2,
        actions: Object.values(IssueActions),
        transitions: [
          IssueTransition.Accept,
          IssueTransition.Confirm,
          IssueTransition.Resolve,
          IssueTransition.FalsePositive,
          IssueTransition.WontFix,
        ],
        component: `${baseComponentKey}:${ISSUE_TO_FILES[ISSUE_2][0]}`,
        message: 'Fix that',
        rule: ISSUE_TO_RULE[ISSUE_2],
        textRange: {
          startLine: 25,
          endLine: 25,
          startOffset: 0,
          endOffset: 1,
        },
        impacts: [
          { softwareQuality: SoftwareQuality.Security, severity: SoftwareImpactSeverity.High },
        ],
        resolution: IssueResolution.Unresolved,
        status: IssueStatus.Open,
        simpleStatus: IssueSimpleStatus.Open,
        ruleDescriptionContextKey: 'spring',
      }),
      snippets: keyBy(
        [
          mockSnippetsByComponent(
            ISSUE_TO_FILES[ISSUE_2][0],
            baseComponentKey,
            times(40, (i) => i + 20),
          ),
        ],
        'component.key',
      ),
    },
    {
      issue: mockRawIssue(false, {
        key: ISSUE_3,
        component: `${baseComponentKey}:${ISSUE_TO_FILES[ISSUE_3][0]}`,
        message: 'Second issue',
        rule: ISSUE_TO_RULE[ISSUE_3],
        textRange: {
          startLine: 28,
          endLine: 28,
          startOffset: 0,
          endOffset: 1,
        },
        resolution: IssueResolution.Fixed,
        status: IssueStatus.Confirmed,
        simpleStatus: IssueSimpleStatus.Confirmed,
      }),
      snippets: keyBy(
        [
          mockSnippetsByComponent(
            ISSUE_TO_FILES[ISSUE_3][0],
            baseComponentKey,
            times(40, (i) => i + 20),
          ),
        ],
        'component.key',
      ),
    },
    {
      issue: mockRawIssue(false, {
        key: ISSUE_4,
        actions: Object.values(IssueActions),
        transitions: [
          IssueTransition.Confirm,
          IssueTransition.Resolve,
          IssueTransition.FalsePositive,
          IssueTransition.WontFix,
        ],
        component: `${baseComponentKey}:${ISSUE_TO_FILES[ISSUE_4][0]}`,
        message: 'Issue with tags',
        rule: ISSUE_TO_RULE[ISSUE_4],
        textRange: {
          startLine: 25,
          endLine: 25,
          startOffset: 0,
          endOffset: 1,
        },
        ruleDescriptionContextKey: 'spring',
        ruleStatus: 'DEPRECATED',
        quickFixAvailable: true,
        tags: ['unused'],
        codeVariants: ['variant 1', 'variant 2'],
        project: 'org.project2',
        assignee: 'email1@sonarsource.com',
        author: 'email3@sonarsource.com',
      }),
      snippets: keyBy(
        [
          mockSnippetsByComponent(
            ISSUE_TO_FILES[ISSUE_4][0],
            baseComponentKey,
            times(40, (i) => i + 20),
          ),
        ],
        'component.key',
      ),
    },
    {
      issue: mockRawIssue(false, {
        key: ISSUE_1101,
        component: `${baseComponentKey}:${ISSUE_TO_FILES[ISSUE_1101][0]}`,
        message: 'Issue on page 2',
        impacts: [
          {
            softwareQuality: SoftwareQuality.Maintainability,
            severity: SoftwareImpactSeverity.High,
          },
        ],
        rule: ISSUE_TO_RULE[ISSUE_1101],
        textRange: undefined,
        line: undefined,
      }),
      snippets: {},
    },
    {
      issue: mockRawIssue(false, {
        key: ISSUE_1102,
        component: `${baseComponentKey}:${ISSUE_TO_FILES[ISSUE_1102][0]}`,
        message: 'Issue inside folderA',
        creationDate: '2022-01-15T09:36:01+0100',
        type: IssueType.CodeSmell,
        rule: ISSUE_TO_RULE[ISSUE_1102],
        textRange: {
          startLine: 10,
          endLine: 10,
          startOffset: 0,
          endOffset: 2,
        },
      }),
      snippets: {},
    },
    {
      issue: mockRawIssue(false, {
        key: ISSUE_1103,
        component: `${baseComponentKey}:${ISSUE_TO_FILES[ISSUE_1103][0]}`,
        creationDate: '2022-01-15T09:36:01+0100',
        message: 'Issue inside folderA',
        type: IssueType.CodeSmell,
        rule: ISSUE_TO_RULE[ISSUE_1103],
        textRange: {
          startLine: 10,
          endLine: 10,
          startOffset: 0,
          endOffset: 2,
        },
      }),
      snippets: {},
    },
  ];
}
