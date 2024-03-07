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
import { BugIcon, CodeSmellIcon, SecurityHotspotIcon, VulnerabilityIcon } from 'design-system';
import { flatten, sortBy } from 'lodash';
import { IssueType, RawIssue } from '../types/issues';
import { MetricKey } from '../types/metrics';
import { Dict, Flow, FlowLocation, FlowType, Issue, TextRange } from '../types/types';
import { UserBase } from '../types/users';
import { ISSUE_TYPES } from './constants';
import { SoftwareQuality } from '../types/clean-code-taxonomy';

interface Rule {}

interface Component {
  key: string;
  name: string;
}

export function sortByType<T extends Pick<Issue, 'type'>>(issues: T[]): T[] {
  return sortBy(issues, (issue) => ISSUE_TYPES.indexOf(issue.type as IssueType));
}

function injectRelational(
  issue: Dict<any>,
  source: any[] | undefined,
  baseField: string,
  lookupField: string,
) {
  const newFields: Dict<any> = {};
  const baseValue = issue[baseField];
  if (baseValue !== undefined && source !== undefined) {
    const lookupValue = source.find((candidate) => candidate[lookupField] === baseValue);
    if (lookupValue != null) {
      Object.keys(lookupValue).forEach((key) => {
        const newKey = baseField + key.charAt(0).toUpperCase() + key.slice(1);
        newFields[newKey] = lookupValue[key];
      });
    }
  }
  return newFields;
}

function injectCommentsRelational(issue: RawIssue, users?: UserBase[]) {
  if (!issue.comments) {
    return {};
  }
  const comments = issue.comments.map((comment) => {
    const commentWithAuthor = { ...comment, author: comment.login, login: undefined };
    return {
      ...commentWithAuthor,
      ...injectRelational(commentWithAuthor, users, 'author', 'login'),
    };
  });
  return { comments };
}

function prepareClosed(issue: RawIssue) {
  return issue.status === 'CLOSED'
    ? { flows: [], line: undefined, textRange: undefined, secondaryLocations: [] }
    : {};
}

function ensureTextRange(issue: RawIssue): { textRange?: TextRange } {
  return issue.line && !issue.textRange
    ? {
        textRange: {
          startLine: issue.line,
          endLine: issue.line,
          startOffset: 0,
          endOffset: 999999,
        },
      }
    : {};
}

function reverseLocations(locations: FlowLocation[]): FlowLocation[] {
  const x = [...locations];
  x.reverse();
  return x;
}

const FLOW_ORDER_MAP = {
  [FlowType.DATA]: 0,
  [FlowType.EXECUTION]: 1,
};

function splitFlows(
  issue: RawIssue,
  components: Component[] = [],
): { secondaryLocations: FlowLocation[]; flows: FlowLocation[][]; flowsWithType: Flow[] } {
  if (issue.flows?.some((flow) => flow.type !== undefined)) {
    const flowsWithType = issue.flows.filter((flow) => flow.type !== undefined) as Flow[];
    flowsWithType.sort((f1, f2) => FLOW_ORDER_MAP[f1.type] - FLOW_ORDER_MAP[f2.type]);

    return {
      flows: [],
      flowsWithType,
      secondaryLocations: [],
    };
  }

  const parsedFlows: FlowLocation[][] = (issue.flows ?? [])
    .filter((flow) => flow.locations !== undefined)
    .map((flow) => flow.locations!.filter((location) => location.textRange != null))
    .map((flow) =>
      flow.map((location) => {
        const component = components.find((component) => component.key === location.component);
        return { ...location, componentName: component?.name };
      }),
    );

  const onlySecondaryLocations = parsedFlows.every((flow) => flow.length === 1);

  return onlySecondaryLocations
    ? { secondaryLocations: orderLocations(flatten(parsedFlows)), flowsWithType: [], flows: [] }
    : {
        secondaryLocations: [],
        flowsWithType: [],
        flows: parsedFlows.map(reverseLocations),
      };
}

function orderLocations(locations: FlowLocation[]) {
  return sortBy(
    locations,
    (location) => location.textRange?.startLine,
    (location) => location.textRange?.startOffset,
  );
}

export function parseIssueFromResponse(
  issue: RawIssue,
  components?: Component[],
  users?: UserBase[],
  rules?: Rule[],
): Issue {
  return {
    ...issue,
    ...injectRelational(issue, components, 'component', 'key'),
    ...injectRelational(issue, components, 'project', 'key'),
    ...injectRelational(issue, rules, 'rule', 'key'),
    ...injectRelational(issue, users, 'assignee', 'login'),
    ...injectCommentsRelational(issue, users),
    ...splitFlows(issue, components),
    ...prepareClosed(issue),
    ...ensureTextRange(issue),
  } as Issue;
}

export function getIssueTypeBySoftwareQuality(quality: SoftwareQuality): IssueType {
  const map = {
    [SoftwareQuality.Maintainability]: IssueType.CodeSmell,
    [SoftwareQuality.Security]: IssueType.Vulnerability,
    [SoftwareQuality.Reliability]: IssueType.Bug,
  };

  return map[quality];
}

export const SOFTWARE_QUALITIES_METRIC_KEYS_MAP = {
  [SoftwareQuality.Security]: {
    metric: MetricKey.security_issues,
    deprecatedMetric: MetricKey.vulnerabilities,
    rating: MetricKey.security_rating,
    newRating: MetricKey.new_security_rating,
  },
  [SoftwareQuality.Reliability]: {
    metric: MetricKey.reliability_issues,
    deprecatedMetric: MetricKey.bugs,
    rating: MetricKey.reliability_rating,
    newRating: MetricKey.new_reliability_rating,
  },
  [SoftwareQuality.Maintainability]: {
    metric: MetricKey.maintainability_issues,
    deprecatedMetric: MetricKey.code_smells,
    rating: MetricKey.sqale_rating,
    newRating: MetricKey.new_maintainability_rating,
  },
};

export const ISSUETYPE_METRIC_KEYS_MAP = {
  [IssueType.CodeSmell]: {
    metric: MetricKey.code_smells,
    newMetric: MetricKey.new_code_smells,
    rating: MetricKey.sqale_rating,
    newRating: MetricKey.new_maintainability_rating,
    ratingName: 'Maintainability',
    iconClass: CodeSmellIcon,
  },
  [IssueType.Vulnerability]: {
    metric: MetricKey.vulnerabilities,
    newMetric: MetricKey.new_vulnerabilities,
    rating: MetricKey.security_rating,
    newRating: MetricKey.new_security_rating,
    ratingName: 'Security',
    iconClass: VulnerabilityIcon,
  },
  [IssueType.Bug]: {
    metric: MetricKey.bugs,
    newMetric: MetricKey.new_bugs,
    rating: MetricKey.reliability_rating,
    newRating: MetricKey.new_reliability_rating,
    ratingName: 'Reliability',
    iconClass: BugIcon,
  },
  [IssueType.SecurityHotspot]: {
    metric: MetricKey.security_hotspots,
    newMetric: MetricKey.new_security_hotspots,
    rating: MetricKey.security_review_rating,
    newRating: MetricKey.new_security_review_rating,
    ratingName: 'SecurityReview',
    iconClass: SecurityHotspotIcon,
  },
};
