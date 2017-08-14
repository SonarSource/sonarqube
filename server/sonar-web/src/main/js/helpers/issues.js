/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
// @flow
import { flatten, sortBy } from 'lodash';
import { SEVERITIES } from './constants';
/*:: import type { Issue, FlowLocation } from '../components/issue/types'; */

/*::
type TextRange = {
  startLine: number,
  endLine: number,
  startOffset: number,
  endOffset: number
};
*/

/*::
type Comment = {
  login: string
};
*/

/*::
type User = {
  login: string
};
*/

/*::
type RawIssue = {
  assignee?: string,
  author: string,
  comments?: Array<Comment>,
  component: string,
  flows?: Array<{
    locations?: Array<{ msg: string, textRange?: TextRange }>
  }>,
  key: string,
  line?: number,
  project: string,
  rule: string,
  status: string,
  subProject?: string,
  textRange?: TextRange
};
*/

export function sortBySeverity(issues /*: Array<*> */) {
  return sortBy(issues, issue => SEVERITIES.indexOf(issue.severity));
}

function injectRelational(
  issue /*: RawIssue | Comment */,
  source /*: ?Array<*> */,
  baseField /*: string */,
  lookupField /*: string */
) {
  const newFields = {};
  const baseValue = issue[baseField];
  if (baseValue != null && source != null) {
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

function injectCommentsRelational(issue /*: RawIssue */, users /*: ?Array<User> */) {
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

function prepareClosed(issue /*: RawIssue */) {
  return issue.status === 'CLOSED' ? { flows: undefined } : {};
}

function ensureTextRange(issue /*: RawIssue */) {
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

function reverseLocations(locations /*: Array<*> */) {
  const x = [...locations];
  x.reverse();
  return x;
}

function splitFlows(
  issue /*: RawIssue */
  // $FlowFixMe textRange is not null
) /*: { secondaryLocations: Array<FlowLocation>, flows: Array<Array<FlowLocation>> } */ {
  const parsedFlows = (issue.flows || [])
    .filter(flow => flow.locations != null)
    // $FlowFixMe flow.locations is not null
    .map(flow => flow.locations.filter(location => location.textRange != null));

  const onlySecondaryLocations = parsedFlows.every(flow => flow.length === 1);

  return onlySecondaryLocations
    ? { secondaryLocations: flatten(parsedFlows), flows: [] }
    : { secondaryLocations: [], flows: parsedFlows.map(reverseLocations) };
}

export function parseIssueFromResponse(
  issue /*: Object */,
  components /*: ?Array<*> */,
  users /*: ?Array<*> */,
  rules /*: ?Array<*> */
) /*: Issue */ {
  const { secondaryLocations, flows } = splitFlows(issue);
  return {
    ...issue,
    ...injectRelational(issue, components, 'component', 'key'),
    ...injectRelational(issue, components, 'project', 'key'),
    ...injectRelational(issue, components, 'subProject', 'key'),
    ...injectRelational(issue, rules, 'rule', 'key'),
    ...injectRelational(issue, users, 'assignee', 'login'),
    ...injectCommentsRelational(issue, users),
    ...prepareClosed(issue),
    ...ensureTextRange(issue),
    secondaryLocations,
    flows
  };
}
