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
import { getBreadcrumbs, getChildren, getComponent } from '../../api/components';
import { getBranchLikeQuery, isPullRequest, isShortLivingBranch } from '../../helpers/branches';
import {
  addComponent,
  addComponentBreadcrumbs,
  addComponentChildren,
  getComponent as getComponentFromBucket,
  getComponentBreadcrumbs,
  getComponentChildren
} from './bucket';

const METRICS = [
  'ncloc',
  'bugs',
  'vulnerabilities',
  'code_smells',
  'security_hotspots',
  'coverage',
  'duplicated_lines_density'
];

const APPLICATION_METRICS = ['alert_status', ...METRICS];

const PORTFOLIO_METRICS = [
  'releasability_rating',
  'reliability_rating',
  'security_rating',
  'security_review_rating',
  'sqale_rating',
  'ncloc'
];

const LEAK_METRICS = [
  'new_lines',
  'bugs',
  'vulnerabilities',
  'code_smells',
  'security_hotspots',
  'new_coverage',
  'new_duplicated_lines_density'
];

const PAGE_SIZE = 100;

interface Children {
  components: T.ComponentMeasure[];
  page: number;
  total: number;
}

function prepareChildren(r: any): Children {
  return {
    components: r.components,
    total: r.paging.total,
    page: r.paging.pageIndex
  };
}

export function showLeakMeasure(branchLike?: T.BranchLike) {
  return isShortLivingBranch(branchLike) || isPullRequest(branchLike);
}

function skipRootDir(breadcrumbs: T.ComponentMeasure[]) {
  return breadcrumbs.filter(component => {
    return !(component.qualifier === 'DIR' && component.name === '/');
  });
}

function storeChildrenBase(children: T.ComponentMeasure[]) {
  children.forEach(addComponent);
}

function storeChildrenBreadcrumbs(parentComponentKey: string, children: T.Breadcrumb[]) {
  const parentBreadcrumbs = getComponentBreadcrumbs(parentComponentKey);
  if (parentBreadcrumbs) {
    children.forEach(child => {
      const breadcrumbs = [...parentBreadcrumbs, child];
      addComponentBreadcrumbs(child.key, breadcrumbs);
    });
  }
}

export function getCodeMetrics(qualifier: string, branchLike?: T.BranchLike) {
  if (['VW', 'SVW'].includes(qualifier)) {
    return [...PORTFOLIO_METRICS];
  }
  if (qualifier === 'APP') {
    return [...APPLICATION_METRICS];
  }
  if (showLeakMeasure(branchLike)) {
    return [...LEAK_METRICS];
  }
  return [...METRICS];
}

function retrieveComponentBase(componentKey: string, qualifier: string, branchLike?: T.BranchLike) {
  const existing = getComponentFromBucket(componentKey);
  if (existing) {
    return Promise.resolve(existing);
  }

  const metrics = getCodeMetrics(qualifier, branchLike);

  return getComponent({
    component: componentKey,
    metricKeys: metrics.join(),
    ...getBranchLikeQuery(branchLike)
  }).then(({ component }) => {
    addComponent(component);
    return component;
  });
}

export function retrieveComponentChildren(
  componentKey: string,
  qualifier: string,
  branchLike?: T.BranchLike
): Promise<{ components: T.ComponentMeasure[]; page: number; total: number }> {
  const existing = getComponentChildren(componentKey);
  if (existing) {
    return Promise.resolve({
      components: existing.children,
      total: existing.total,
      page: existing.page
    });
  }

  const metrics = getCodeMetrics(qualifier, branchLike);
  if (['VW', 'SVW'].includes(qualifier)) {
    metrics.push('alert_status');
  }

  return getChildren(componentKey, metrics, {
    ps: PAGE_SIZE,
    s: 'qualifier,name',
    ...getBranchLikeQuery(branchLike)
  })
    .then(prepareChildren)
    .then(r => {
      addComponentChildren(componentKey, r.components, r.total, r.page);
      storeChildrenBase(r.components);
      storeChildrenBreadcrumbs(componentKey, r.components);
      return r;
    });
}

function retrieveComponentBreadcrumbs(
  component: string,
  branchLike?: T.BranchLike
): Promise<T.Breadcrumb[]> {
  const existing = getComponentBreadcrumbs(component);
  if (existing) {
    return Promise.resolve(existing);
  }

  return getBreadcrumbs({ component, ...getBranchLikeQuery(branchLike) })
    .then(skipRootDir)
    .then(breadcrumbs => {
      addComponentBreadcrumbs(component, breadcrumbs);
      return breadcrumbs;
    });
}

export function retrieveComponent(
  componentKey: string,
  qualifier: string,
  branchLike?: T.BranchLike
): Promise<{
  breadcrumbs: T.Breadcrumb[];
  component: T.ComponentMeasure;
  components: T.ComponentMeasure[];
  page: number;
  total: number;
}> {
  return Promise.all([
    retrieveComponentBase(componentKey, qualifier, branchLike),
    retrieveComponentChildren(componentKey, qualifier, branchLike),
    retrieveComponentBreadcrumbs(componentKey, branchLike)
  ]).then(r => {
    return {
      breadcrumbs: r[2],
      component: r[0],
      components: r[1].components,
      page: r[1].page,
      total: r[1].total
    };
  });
}

export function loadMoreChildren(
  componentKey: string,
  page: number,
  qualifier: string,
  branchLike?: T.BranchLike
): Promise<Children> {
  const metrics = getCodeMetrics(qualifier, branchLike);

  return getChildren(componentKey, metrics, {
    ps: PAGE_SIZE,
    p: page,
    s: 'qualifier,name',
    ...getBranchLikeQuery(branchLike)
  })
    .then(prepareChildren)
    .then(r => {
      addComponentChildren(componentKey, r.components, r.total, r.page);
      storeChildrenBase(r.components);
      storeChildrenBreadcrumbs(componentKey, r.components);
      return r;
    });
}
