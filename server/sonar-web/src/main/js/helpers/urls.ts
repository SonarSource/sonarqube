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
import { isNil, omitBy, pick } from 'lodash';
import { getProfilePath } from '../apps/quality-profiles/utils';
import { BranchLike, BranchParameters } from '../types/branch-like';
import { ComponentQualifier, isApplication, isPortfolioLike } from '../types/component';
import { MeasurePageView } from '../types/measures';
import { GraphType } from '../types/project-activity';
import { SecurityStandard } from '../types/security';
import { Dict, HomePage } from '../types/types';
import { getBranchLikeQuery, isBranch, isMainBranch, isPullRequest } from './branch-like';
import { IS_SSR } from './browser';
import { serializeOptionalBoolean } from './query';
import { getBaseUrl } from './system';

export interface Location {
  pathname: string;
  query?: Dict<string | undefined | number>;
}

type Query = Location['query'];

export function getComponentOverviewUrl(
  componentKey: string,
  componentQualifier: ComponentQualifier | string,
  branchParameters?: BranchParameters
) {
  return isPortfolioLike(componentQualifier)
    ? getPortfolioUrl(componentKey)
    : getProjectQueryUrl(componentKey, branchParameters);
}

export function getComponentAdminUrl(
  componentKey: string,
  componentQualifier: ComponentQualifier | string
) {
  if (isPortfolioLike(componentQualifier)) {
    return getPortfolioAdminUrl(componentKey);
  } else if (isApplication(componentQualifier)) {
    return getApplicationAdminUrl(componentKey);
  } else {
    return getProjectUrl(componentKey);
  }
}

export function getProjectUrl(project: string, branch?: string): Location {
  return { pathname: '/dashboard', query: { id: project, branch } };
}

export function getProjectQueryUrl(project: string, branchParameters?: BranchParameters): Location {
  return { pathname: '/dashboard', query: { id: project, ...branchParameters } };
}

export function getPortfolioUrl(key: string): Location {
  return { pathname: '/portfolio', query: { id: key } };
}

export function getPortfolioAdminUrl(key: string) {
  return {
    pathname: '/project/admin/extension/governance/console',
    query: { id: key, qualifier: ComponentQualifier.Portfolio }
  };
}

export function getApplicationAdminUrl(key: string) {
  return {
    pathname: '/project/admin/extension/developer-server/application-console',
    query: { id: key }
  };
}

export function getComponentBackgroundTaskUrl(
  componentKey: string,
  status?: string,
  taskType?: string
): Location {
  return { pathname: '/project/background_tasks', query: { id: componentKey, status, taskType } };
}

export function getBranchLikeUrl(project: string, branchLike?: BranchLike): Location {
  if (isPullRequest(branchLike)) {
    return getPullRequestUrl(project, branchLike.key);
  } else if (isBranch(branchLike) && !isMainBranch(branchLike)) {
    return getBranchUrl(project, branchLike.name);
  } else {
    return getProjectUrl(project);
  }
}

export function getBranchUrl(project: string, branch: string): Location {
  return { pathname: '/dashboard', query: { branch, id: project } };
}

export function getPullRequestUrl(project: string, pullRequest: string): Location {
  return { pathname: '/dashboard', query: { id: project, pullRequest } };
}

/**
 * Generate URL for a global issues page
 */
export function getIssuesUrl(query: Query): Location {
  const pathname = '/issues';
  return { pathname, query };
}

/**
 * Generate URL for a component's issues page
 */
export function getComponentIssuesUrl(componentKey: string, query?: Query): Location {
  return { pathname: '/project/issues', query: { ...(query || {}), id: componentKey } };
}

/**
 * Generate URL for a component's security hotspot page
 */
export function getComponentSecurityHotspotsUrl(componentKey: string, query: Query = {}): Location {
  const { branch, pullRequest, sinceLeakPeriod, hotspots, assignedToMe, file } = query;
  return {
    pathname: '/security_hotspots',
    query: {
      id: componentKey,
      branch,
      pullRequest,
      sinceLeakPeriod,
      hotspots,
      assignedToMe,
      file,
      ...pick(query, [
        SecurityStandard.SONARSOURCE,
        SecurityStandard.OWASP_TOP10,
        SecurityStandard.SANS_TOP25,
        SecurityStandard.CWE
      ])
    }
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
}): Location {
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
  return { pathname: '/component_measures', query };
}

export function getComponentDrilldownUrlWithSelection(
  componentKey: string,
  selectionKey: string,
  metric: string,
  branchLike?: BranchLike,
  view?: MeasurePageView
): Location {
  return getComponentDrilldownUrl({
    componentKey,
    selectionKey,
    metric,
    branchLike,
    treemapView: view === 'treemap',
    listView: view === 'list'
  });
}

export function getMeasureTreemapUrl(componentKey: string, metric: string) {
  return getComponentDrilldownUrl({ componentKey, metric, treemapView: true });
}

export function getActivityUrl(component: string, branchLike?: BranchLike, graph?: GraphType) {
  return {
    pathname: '/project/activity',
    query: { id: component, graph, ...getBranchLikeQuery(branchLike) }
  };
}

/**
 * Generate URL for a component's measure history
 */
export function getMeasureHistoryUrl(component: string, metric: string, branchLike?: BranchLike) {
  return {
    pathname: '/project/activity',
    query: {
      id: component,
      graph: 'custom',
      custom_metrics: metric,
      ...getBranchLikeQuery(branchLike)
    }
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
export function getQualityProfileUrl(name: string, language: string): Location {
  return getProfilePath(name, language);
}

export function getQualityGateUrl(key: string): Location {
  return {
    pathname: '/quality_gates/show/' + encodeURIComponent(key)
  };
}

export function getQualityGatesUrl(): Location {
  return {
    pathname: '/quality_gates'
  };
}

export function getGlobalSettingsUrl(
  category?: string,
  query?: Dict<string | undefined | number>
): Location {
  return {
    pathname: '/admin/settings',
    query: { category, ...query }
  };
}

export function getProjectSettingsUrl(id: string, category?: string): Location {
  return {
    pathname: '/project/settings',
    query: { id, category }
  };
}

/**
 * Generate URL for the rules page
 */
export function getRulesUrl(query: Query): Location {
  return { pathname: '/coding_rules', query };
}

/**
 * Generate URL for the rules page filtering only active deprecated rules
 */
export function getDeprecatedActiveRulesUrl(query: Query = {}): Location {
  const baseQuery = { activation: 'true', statuses: 'DEPRECATED' };
  return getRulesUrl({ ...query, ...baseQuery });
}

export function getRuleUrl(rule: string) {
  return getRulesUrl({ open: rule, rule_key: rule });
}

export function getFormattingHelpUrl(): string {
  return getBaseUrl() + '/formatting/help';
}

export function getCodeUrl(
  project: string,
  branchLike?: BranchLike,
  selected?: string,
  line?: number
): Location {
  return {
    pathname: '/code',
    query: { id: project, ...getBranchLikeQuery(branchLike), selected, line: line?.toFixed() }
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
      return { pathname: '/issues', query: { resolved: 'false' } };
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
  if (IS_SSR) {
    throw new Error('No host url available on server side.');
  }
  return window.location.origin + getBaseUrl();
}

export function getPathUrlAsString(path: Location, internal = true): string {
  return `${internal ? getBaseUrl() : getHostUrl()}${path.pathname}?${new URLSearchParams(
    omitBy(path.query, isNil)
  ).toString()}`;
}

export function getReturnUrl(location: { hash?: string; query?: { return_to?: string } }) {
  const returnTo = location.query && location.query['return_to'];
  if (isRelativeUrl(returnTo)) {
    return returnTo + (location.hash ? location.hash : '');
  }
  return getBaseUrl() + '/';
}

export function isRelativeUrl(url?: string): boolean {
  const regex = new RegExp(/^\/[^/\\]/);
  return Boolean(url && regex.test(url));
}
