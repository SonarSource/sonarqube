/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import { stringify } from 'querystring';
import { omitBy, isNil } from 'lodash';
import { isShortLivingBranch } from './branches';
import { getProfilePath } from '../apps/quality-profiles/utils';
import { Branch, HomePage, HomePageType } from '../app/types';

interface Query {
  [x: string]: string | undefined;
}

interface Location {
  pathname: string;
  query?: Query;
}

export function getBaseUrl(): string {
  return (window as any).baseUrl;
}

export function getHostUrl(): string {
  return window.location.origin + getBaseUrl();
}

export function getPathUrlAsString(path: Location): string {
  return `${getBaseUrl()}${path.pathname}?${stringify(omitBy(path.query, isNil))}`;
}

export function getProjectUrl(key: string, branch?: string): Location {
  return { pathname: '/dashboard', query: { id: key, branch } };
}

export function getComponentBackgroundTaskUrl(componentKey: string, status?: string): Location {
  return { pathname: '/project/background_tasks', query: { id: componentKey, status } };
}

export function getProjectBranchUrl(key: string, branch: Branch): Location {
  if (isShortLivingBranch(branch)) {
    return {
      pathname: '/project/issues',
      query: { branch: branch.name, id: key, resolved: 'false' }
    };
  } else if (!branch.isMain) {
    return { pathname: '/dashboard', query: { branch: branch.name, id: key } };
  } else {
    return { pathname: '/dashboard', query: { id: key } };
  }
}

/**
 * Generate URL for a global issues page
 */
export function getIssuesUrl(query: Query, organization?: string): Location {
  const pathname = organization ? `/organizations/${organization}/issues` : '/issues';
  return { pathname, query };
}

/**
 * Generate URL for a component's issues page
 */
export function getComponentIssuesUrl(componentKey: string, query?: Query): Location {
  return { pathname: '/project/issues', query: { ...(query || {}), id: componentKey } };
}

/**
 * Generate URL for a component's drilldown page
 */
export function getComponentDrilldownUrl(
  componentKey: string,
  metric: string,
  branch?: string
): Location {
  return { pathname: '/component_measures', query: { id: componentKey, metric, branch } };
}

export function getMeasureTreemapUrl(component: string, metric: string, branch?: string) {
  return {
    pathname: '/component_measures',
    query: { id: component, metric, branch, view: 'treemap' }
  };
}

/**
 * Generate URL for a component's measure history
 */
export function getMeasureHistoryUrl(component: string, metric: string, branch?: string) {
  return {
    pathname: '/project/activity',
    query: { id: component, graph: 'custom', custom_metrics: metric, branch }
  };
}

/**
 * Generate URL for a component's permissions page
 */
export function getComponentPermissionsUrl(componentKey: string): Location {
  return { pathname: '/project_roles', query: { id: componentKey } };
}

/**
 * Generate URL for a quality profile
 */
export function getQualityProfileUrl(
  name: string,
  language: string,
  organization?: string | null
): Location {
  return getProfilePath(name, language, organization);
}

export function getQualityGateUrl(key: string, organization?: string | null): Location {
  return {
    pathname: getQualityGatesUrl(organization).pathname + '/show/' + encodeURIComponent(key)
  };
}

export function getQualityGatesUrl(organization?: string | null): Location {
  return {
    pathname:
      (organization ? '/organizations/' + encodeURIComponent(organization) : '') + '/quality_gates'
  };
}

/**
 * Generate URL for the rules page
 */
export function getRulesUrl(query: Query, organization: string | null | undefined): Location {
  const pathname = organization ? `/organizations/${organization}/rules` : '/coding_rules';
  return { pathname, query };
}

/**
 * Generate URL for the rules page filtering only active deprecated rules
 */
export function getDeprecatedActiveRulesUrl(
  query: Query = {},
  organization: string | null | undefined
): Location {
  const baseQuery = { activation: 'true', statuses: 'DEPRECATED' };
  return getRulesUrl({ ...query, ...baseQuery }, organization);
}

export function getRuleUrl(rule: string, organization: string | undefined) {
  /* eslint-disable camelcase */
  return getRulesUrl({ open: rule, rule_key: rule }, organization);
  /* eslint-enable camelcase */
}

export function getMarkdownHelpUrl(): string {
  return getBaseUrl() + '/markdown/help';
}

export function getCodeUrl(project: string, branch?: string, selected?: string) {
  return { pathname: '/code', query: { id: project, branch, selected } };
}

export function getOrganizationUrl(organization: string) {
  return `/organizations/${organization}`;
}

export function getHomePageUrl(homepage: HomePage) {
  switch (homepage.type) {
    case HomePageType.Project:
      return getProjectUrl(homepage.parameter!);
    case HomePageType.Organization:
      return getOrganizationUrl(homepage.parameter!);
    case HomePageType.MyProjects:
      return '/projects';
    case HomePageType.MyIssues:
      return { pathname: '/issues', query: { resolved: 'false' } };
  }

  // should never happen, but just in case...
  return '/projects';
}
