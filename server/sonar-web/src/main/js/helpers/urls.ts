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
import { isArray, mapValues, omitBy, pick } from 'lodash';
import { Path, To } from 'react-router-dom';
import { getProfilePath } from '../apps/quality-profiles/utils';
import { DEFAULT_ISSUES_QUERY } from '../components/shared/utils';
import { BranchLike, BranchParameters } from '../types/branch-like';
import { ComponentQualifier, isApplication, isPortfolioLike } from '../types/component';
import { MeasurePageView } from '../types/measures';
import { GraphType } from '../types/project-activity';
import { SecurityStandard } from '../types/security';
import { Dict, RawQuery } from '../types/types';
import { HomePage } from '../types/users';
import { getBranchLikeQuery, isBranch, isMainBranch, isPullRequest } from './branch-like';
import { serializeOptionalBoolean } from './query';
import { getBaseUrl } from './system';

export interface Location {
  pathname: string;
  query?: Dict<string | undefined | number>;
}

export enum CodeScope {
  Overall = 'overall',
  New = 'new',
}

type CodeScopeType = CodeScope.Overall | CodeScope.New;

type Query = Location['query'];

const PROJECT_BASE_URL = '/dashboard';

export function queryToSearch(query: RawQuery = {}) {
  const arrayParams: Array<{ key: string; values: string[] }> = [];

  const stringParams = mapValues(query, (value, key) => {
    // array values are added afterwards
    if (isArray(value)) {
      arrayParams.push({ key, values: value });
      return '';
    }

    return value != null ? `${value}` : '';
  });
  const filteredParams = omitBy(stringParams, (v: string) => v.length === 0);
  const searchParams = new URLSearchParams(filteredParams);

  /*
   * Add each value separately
   * e.g. author: ['a', 'b'] should be serialized as
   * author=a&author=b
   */
  arrayParams.forEach(({ key, values }) => {
    values.forEach((value) => {
      searchParams.append(key, value);
    });
  });

  return `?${searchParams.toString()}`;
}

export function getComponentOverviewUrl(
  componentKey: string,
  componentQualifier: ComponentQualifier | string,
  branchParameters?: BranchParameters,
  codeScope?: CodeScopeType,
) {
  return isPortfolioLike(componentQualifier)
    ? getPortfolioUrl(componentKey)
    : getProjectQueryUrl(componentKey, branchParameters, codeScope);
}

export function getComponentAdminUrl(
  componentKey: string,
  componentQualifier: ComponentQualifier | string,
) {
  if (isPortfolioLike(componentQualifier)) {
    return getPortfolioAdminUrl(componentKey);
  } else if (isApplication(componentQualifier)) {
    return getApplicationAdminUrl(componentKey);
  }
  return getProjectUrl(componentKey);
}

export function getProjectUrl(
  project: string,
  branch?: string,
  codeScope?: CodeScopeType,
): Partial<Path> {
  return {
    pathname: PROJECT_BASE_URL,
    search: queryToSearch({ id: project, branch, ...(codeScope && { code_scope: codeScope }) }),
  };
}

export function getProjectSecurityHotspots(project: string): To {
  return {
    pathname: '/security_hotspots',
    search: queryToSearch({ id: project }),
  };
}

export function getProjectQueryUrl(
  project: string,
  branchParameters?: BranchParameters,
  codeScope?: CodeScopeType,
): To {
  return {
    pathname: PROJECT_BASE_URL,
    search: queryToSearch({
      id: project,
      ...branchParameters,
      ...(codeScope && { code_scope: codeScope }),
    }),
  };
}

export function getPortfolioUrl(key: string): To {
  return { pathname: '/portfolio', search: queryToSearch({ id: key }) };
}

export function getPortfolioAdminUrl(key: string): To {
  return {
    pathname: '/project/admin/extension/governance/console',
    search: queryToSearch({ id: key, qualifier: ComponentQualifier.Portfolio }),
  };
}

export function getApplicationAdminUrl(key: string): To {
  return {
    pathname: '/project/admin/extension/developer-server/application-console',
    search: queryToSearch({ id: key }),
  };
}

export function getComponentBackgroundTaskUrl(
  componentKey: string,
  status?: string,
  taskType?: string,
): Path {
  return {
    pathname: '/project/background_tasks',
    search: queryToSearch({ id: componentKey, status, taskType }),
    hash: '',
  };
}

export function getBranchLikeUrl(project: string, branchLike?: BranchLike): Partial<Path> {
  if (isPullRequest(branchLike)) {
    return getPullRequestUrl(project, branchLike.key);
  } else if (isBranch(branchLike) && !isMainBranch(branchLike)) {
    return getBranchUrl(project, branchLike.name);
  }
  return getProjectUrl(project);
}

export function getBranchUrl(project: string, branch: string): Partial<Path> {
  return { pathname: PROJECT_BASE_URL, search: queryToSearch({ branch, id: project }) };
}

export function getPullRequestUrl(project: string, pullRequest: string): Partial<Path> {
  return { pathname: PROJECT_BASE_URL, search: queryToSearch({ id: project, pullRequest }) };
}

/**
 * Generate URL for a global issues page
 */
export function getIssuesUrl(query: Query): To {
  const pathname = '/issues';
  return { pathname, search: queryToSearch(query) };
}

/**
 * Generate URL for a component's issues page
 */
export function getComponentIssuesUrl(componentKey: string, query?: Query): Path {
  return {
    pathname: '/project/issues',
    search: queryToSearch({ ...(query || {}), id: componentKey }),
    hash: '',
  };
}

/**
 * Generate URL for a component's security hotspot page
 */
export function getComponentSecurityHotspotsUrl(componentKey: string, query: Query = {}): Path {
  const { branch, pullRequest, inNewCodePeriod, hotspots, assignedToMe, files } = query;
  return {
    pathname: '/security_hotspots',
    search: queryToSearch({
      id: componentKey,
      branch,
      pullRequest,
      inNewCodePeriod,
      hotspots,
      assignedToMe,
      files,
      ...pick(query, [
        SecurityStandard.OWASP_TOP10_2021,
        SecurityStandard.OWASP_TOP10,
        SecurityStandard.SONARSOURCE,
        SecurityStandard.CWE,
        SecurityStandard.PCI_DSS_3_2,
        SecurityStandard.PCI_DSS_4_0,
        SecurityStandard.OWASP_ASVS_4_0,
        'owaspAsvsLevel',
      ]),
    }),
    hash: '',
  };
}

/**
 * Generate URL for a component's drilldown page
 */
export function getComponentDrilldownUrl(options: {
  componentKey: string;
  metric: string;
  branchLike?: BranchLike;
  selectionKey?: string;
  treemapView?: boolean;
  listView?: boolean;
  asc?: boolean;
}): To {
  const { componentKey, metric, branchLike, selectionKey, treemapView, listView, asc } = options;
  const query: Query = { id: componentKey, metric, ...getBranchLikeQuery(branchLike) };
  if (treemapView) {
    query.view = 'treemap';
  }
  if (listView) {
    query.view = 'list';
    query.asc = serializeOptionalBoolean(asc);
  }
  if (selectionKey) {
    query.selected = selectionKey;
  }
  return { pathname: '/component_measures', search: queryToSearch(query) };
}

export function getComponentDrilldownUrlWithSelection(
  componentKey: string,
  selectionKey: string,
  metric: string,
  branchLike?: BranchLike,
  view?: MeasurePageView,
): To {
  return getComponentDrilldownUrl({
    componentKey,
    selectionKey,
    metric,
    branchLike,
    treemapView: view === MeasurePageView.treemap,
    listView: view === MeasurePageView.list,
  });
}

export function getMeasureTreemapUrl(componentKey: string, metric: string) {
  return getComponentDrilldownUrl({ componentKey, metric, treemapView: true });
}

export function getActivityUrl(component: string, branchLike?: BranchLike, graph?: GraphType) {
  return {
    pathname: '/project/activity',
    search: queryToSearch({ id: component, graph, ...getBranchLikeQuery(branchLike) }),
  };
}

/**
 * Generate URL for a component's measure history
 */
export function getMeasureHistoryUrl(component: string, metric: string, branchLike?: BranchLike) {
  return {
    pathname: '/project/activity',
    search: queryToSearch({
      id: component,
      graph: 'custom',
      custom_metrics: metric,
      ...getBranchLikeQuery(branchLike),
    }),
  };
}

/**
 * Generate URL for a component's permissions page
 */
export function getComponentPermissionsUrl(componentKey: string): To {
  return { pathname: '/project_roles', search: queryToSearch({ id: componentKey }) };
}

/**
 * Generate URL for a quality profile
 */
export function getQualityProfileUrl(name: string, language: string): To {
  return getProfilePath(name, language);
}

export function getQualityGateUrl(name: string): To {
  // This is a workaround for the react router bug: https://github.com/remix-run/react-router/issues/10814
  const qualityGateName = name.replace(/%/g, '%25');
  return {
    pathname: '/quality_gates/show/' + encodeURIComponent(qualityGateName),
  };
}

/**
 * Generate URL for the project tutorial page
 */
export function getProjectTutorialLocation(
  project: string,
  selectedTutorial?: string,
): Partial<Path> {
  return {
    pathname: '/tutorials',
    search: queryToSearch({ id: project, selectedTutorial }),
  };
}

/**
 * Generate URL for the project creation page
 */
export function getCreateProjectModeLocation(mode?: string): Partial<Path> {
  return {
    search: queryToSearch({ mode }),
  };
}

export function getQualityGatesUrl(): To {
  return {
    pathname: '/quality_gates',
  };
}

export function getGlobalSettingsUrl(
  category?: string,
  query?: Dict<string | undefined | number>,
): Partial<Path> {
  return {
    pathname: '/admin/settings',
    search: queryToSearch({ category, ...query }),
  };
}

export function getProjectSettingsUrl(id: string, category?: string): Partial<Path> {
  return {
    pathname: '/project/settings',
    search: queryToSearch({ id, category }),
  };
}

/**
 * Generate URL for the rules page
 */
export function getRulesUrl(query: Query): Partial<Path> {
  return { pathname: '/coding_rules', search: queryToSearch(query) };
}

/**
 * Generate URL for the rules page filtering only active deprecated rules
 */
export function getDeprecatedActiveRulesUrl(query: Query = {}): To {
  const baseQuery = { activation: 'true', statuses: 'DEPRECATED' };
  return getRulesUrl({ ...query, ...baseQuery });
}

export function getRuleUrl(rule: string) {
  return getRulesUrl({ open: rule, rule_key: rule });
}

export function getFormattingHelpUrl(): string {
  return '/formatting/help';
}

export function getCodeUrl(
  project: string,
  branchLike?: BranchLike,
  selected?: string,
  line?: number,
): Partial<Path> {
  return {
    pathname: '/code',
    search: queryToSearch({
      id: project,
      ...getBranchLikeQuery(branchLike),
      selected,
      line: line?.toFixed(),
    }),
  };
}

export function getHomePageUrl(homepage: HomePage) {
  switch (homepage.type) {
    case 'APPLICATION':
      return homepage.branch
        ? getProjectUrl(homepage.component, homepage.branch)
        : getProjectUrl(homepage.component);
    case 'PROJECT':
      return homepage.branch
        ? getBranchUrl(homepage.component, homepage.branch)
        : getProjectUrl(homepage.component);
    case 'PORTFOLIO':
      return getPortfolioUrl(homepage.component);
    case 'PORTFOLIOS':
      return '/portfolios';
    case 'MY_PROJECTS':
      return '/projects';
    case 'ISSUES':
    case 'MY_ISSUES':
      return { pathname: '/issues', query: DEFAULT_ISSUES_QUERY };
  }

  // should never happen, but just in case...
  return '/projects';
}

export function convertGithubApiUrlToLink(url: string) {
  return url
    .replace(/^https?:\/\/api\.github\.com/, 'https://github.com') // GH.com
    .replace(/\/api\/v\d+\/?$/, ''); // GH Enterprise
}

export function stripTrailingSlash(url: string) {
  return url.replace(/\/$/, '');
}

export function getHostUrl(): string {
  return window.location.origin + getBaseUrl();
}

export function getPathUrlAsString(path: Partial<Path>, internal = true): string {
  return `${internal ? getBaseUrl() : getHostUrl()}${path.pathname ?? '/'}${path.search ?? ''}`;
}

export function getReturnUrl(location: { hash?: string; query?: { return_to?: string } }) {
  const returnTo = location.query && location.query['return_to'];

  if (isRelativeUrl(returnTo)) {
    return returnTo + (location.hash ? location.hash : '');
  }
  return `${getBaseUrl()}/`;
}

export function isRelativeUrl(url?: string): boolean {
  const regex = new RegExp(/^\/[^/\\]/);
  return Boolean(url && regex.test(url));
}

export function searchParamsToQuery(searchParams: URLSearchParams, omitKey: string[] = []) {
  const result: RawQuery = {};

  searchParams.forEach((value, key) => {
    if (omitKey.includes(key)) {
      return;
    }
    if (result[key]) {
      result[key] = ([] as string[]).concat(result[key], value);
    } else {
      result[key] = value;
    }
  });

  return result;
}

export function convertToTo(link: string | Location) {
  if (linkIsLocation(link)) {
    return { pathname: link.pathname, search: queryToSearch(link.query) } as Partial<Path>;
  }
  return link;
}

function linkIsLocation(link: string | Location): link is Location {
  return (link as Location).query !== undefined;
}
