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
import { flatten, sortBy } from 'lodash';
import { SEVERITIES } from './constants';

interface Comment {
  login: string;
  [x: string]: any;
}

interface User {
  login: string;
}

interface Rule {}

interface Component {
  key: string;
  name: string;
}

interface IssueBase {
  severity: string;
  [x: string]: any;
}

export interface RawIssue extends IssueBase {
  assignee?: string;
  author?: string;
  comments?: Array<Comment>;
  component: string;
  flows?: Array<{
    // `componentName` is not available in RawIssue
    locations?: Array<T.Omit<T.FlowLocation, 'componentName'>>;
  }>;
  key: string;
  line?: number;
  project: string;
  rule: string;
  status: string;
  subProject?: string;
  textRange?: T.TextRange;
}

export function sortBySeverity(issues: T.Issue[]): T.Issue[] {
  return sortBy(issues, issue => SEVERITIES.indexOf(issue.severity));
}

function injectRelational(
  issue: T.Dict<any>,
  source: any[] | undefined,
  baseField: string,
  lookupField: string
) {
  const newFields: T.Dict<any> = {};
  const baseValue = issue[baseField];
  if (baseValue !== undefined && source !== undefined) {
    const lookupValue = source.find(candidate => candidate[lookupField] === baseValue);
    if (lookupValue != null) {
      Object.keys(lookupValue).forEach(key => {
        const newKey = baseField + key.charAt(0).toUpperCase() + key.slice(1);
        newFields[newKey] = lookupValue[key];
      });
    }
  }
  return newFields;
}

function injectCommentsRelational(issue: RawIssue, users?: User[]) {
  if (!issue.comments) {
    return {};
  }
  const comments = issue.comments.map(comment => {
    const commentWithAuthor = { ...comment, author: comment.login, login: undefined };
    return {
      ...commentWithAuthor,
      ...injectRelational(commentWithAuthor, users, 'author', 'login')
    };
  });
  return { comments };
}

function prepareClosed(
  issue: RawIssue,
  secondaryLocations: T.FlowLocation[],
  flows: T.FlowLocation[][]
) {
  return issue.status === 'CLOSED'
    ? { flows: [], line: undefined, textRange: undefined, secondaryLocations: [] }
    : { flows, secondaryLocations };
}

function ensureTextRange(issue: RawIssue): { textRange?: T.TextRange } {
  return issue.line && !issue.textRange
    ? {
        textRange: {
          startLine: issue.line,
          endLine: issue.line,
          startOffset: 0,
          endOffset: 999999
        }
      }
    : {};
}

function reverseLocations(locations: T.FlowLocation[]): T.FlowLocation[] {
  const x = [...locations];
  x.reverse();
  return x;
}

function splitFlows(
  issue: RawIssue,
  components: Component[] = []
): { secondaryLocations: T.FlowLocation[]; flows: T.FlowLocation[][] } {
  const parsedFlows: T.FlowLocation[][] = (issue.flows || [])
    .filter(flow => flow.locations !== undefined)
    .map(flow => flow.locations!.filter(location => location.textRange != null))
    .map(flow =>
      flow.map(location => {
        const component = components.find(component => component.key === location.component);
        return { ...location, componentName: component && component.name };
      })
    );

  const onlySecondaryLocations = parsedFlows.every(flow => flow.length === 1);

  return onlySecondaryLocations
    ? { secondaryLocations: orderLocations(flatten(parsedFlows)), flows: [] }
    : { secondaryLocations: [], flows: parsedFlows.map(reverseLocations) };
}

function orderLocations(locations: T.FlowLocation[]) {
  return sortBy(
    locations,
    location => location.textRange && location.textRange.startLine,
    location => location.textRange && location.textRange.startOffset
  );
}

export function parseIssueFromResponse(
  issue: RawIssue,
  components?: Component[],
  users?: User[],
  rules?: Rule[]
): T.Issue {
  const { secondaryLocations, flows } = splitFlows(issue, components);
  return {
    ...issue,
    ...injectRelational(issue, components, 'component', 'key'),
    ...injectRelational(issue, components, 'project', 'key'),
    ...injectRelational(issue, components, 'subProject', 'key'),
    ...injectRelational(issue, rules, 'rule', 'key'),
    ...injectRelational(issue, users, 'assignee', 'login'),
    ...injectCommentsRelational(issue, users),
    ...prepareClosed(issue, secondaryLocations, flows),
    ...ensureTextRange(issue)
  } as T.Issue;
}
