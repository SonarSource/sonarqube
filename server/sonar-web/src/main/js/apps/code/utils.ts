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
import { getBreadcrumbs, getChildren, getComponent } from '../../api/components';
import { getBranchLikeQuery, isPullRequest } from '../../helpers/branch-like';
import { BranchLike } from '../../types/branch-like';
import { isPortfolioLike } from '../../types/component';
import { MetricKey } from '../../types/metrics';
import { Breadcrumb, ComponentMeasure } from '../../types/types';
import {
  addComponent,
  addComponentBreadcrumbs,
  addComponentChildren,
  getComponent as getComponentFromBucket,
  getComponentBreadcrumbs,
  getComponentChildren
} from './bucket';

const METRICS = [
  MetricKey.ncloc,
  MetricKey.bugs,
  MetricKey.vulnerabilities,
  MetricKey.code_smells,
  MetricKey.security_hotspots,
  MetricKey.coverage,
  MetricKey.duplicated_lines_density
];

const APPLICATION_METRICS = [MetricKey.alert_status, ...METRICS];

const PORTFOLIO_METRICS = [
  MetricKey.releasability_rating,
  MetricKey.reliability_rating,
  MetricKey.security_rating,
  MetricKey.security_review_rating,
  MetricKey.sqale_rating,
  MetricKey.ncloc
];

const NEW_PORTFOLIO_METRICS = [
  MetricKey.releasability_rating,
  MetricKey.new_reliability_rating,
  MetricKey.new_security_rating,
  MetricKey.new_security_review_rating,
  MetricKey.new_maintainability_rating,
  MetricKey.new_lines
];

const LEAK_METRICS = [
  MetricKey.new_lines,
  MetricKey.bugs,
  MetricKey.vulnerabilities,
  MetricKey.code_smells,
  MetricKey.security_hotspots,
  MetricKey.new_coverage,
  MetricKey.new_duplicated_lines_density
];

const PAGE_SIZE = 100;

interface Children {
  components: ComponentMeasure[];
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

export function showLeakMeasure(branchLike?: BranchLike) {
  return isPullRequest(branchLike);
}

function skipRootDir(breadcrumbs: ComponentMeasure[]) {
  return breadcrumbs.filter(component => {
    return !(component.qualifier === 'DIR' && component.name === '/');
  });
}

function storeChildrenBase(children: ComponentMeasure[]) {
  children.forEach(addComponent);
}

function storeChildrenBreadcrumbs(parentComponentKey: string, children: Breadcrumb[]) {
  const parentBreadcrumbs = getComponentBreadcrumbs(parentComponentKey);
  if (parentBreadcrumbs) {
    children.forEach(child => {
      const breadcrumbs = [...parentBreadcrumbs, child];
      addComponentBreadcrumbs(child.key, breadcrumbs);
    });
  }
}

export function getCodeMetrics(
  qualifier: string,
  branchLike?: BranchLike,
  options: { includeQGStatus?: boolean; newCode?: boolean } = {}
) {
  if (isPortfolioLike(qualifier)) {
    let metrics: MetricKey[] = [];
    if (options?.newCode === undefined) {
      metrics = [...NEW_PORTFOLIO_METRICS, ...PORTFOLIO_METRICS];
    } else if (options?.newCode) {
      metrics = [...NEW_PORTFOLIO_METRICS];
    } else {
      metrics = [...PORTFOLIO_METRICS];
    }
    return options.includeQGStatus ? metrics.concat(MetricKey.alert_status) : metrics;
  }
  if (qualifier === 'APP') {
    return [...APPLICATION_METRICS];
  }
  if (showLeakMeasure(branchLike)) {
    return [...LEAK_METRICS];
  }
  return [...METRICS];
}

function retrieveComponentBase(
  componentKey: string,
  qualifier: string,
  instance: { mounted: boolean },
  branchLike?: BranchLike
) {
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
    if (instance.mounted) {
      addComponent(component);
    }
    return component;
  });
}

export function retrieveComponentChildren(
  componentKey: string,
  qualifier: string,
  instance: { mounted: boolean },
  branchLike?: BranchLike
): Promise<{ components: ComponentMeasure[]; page: number; total: number }> {
  const existing = getComponentChildren(componentKey);
  if (existing) {
    return Promise.resolve({
      components: existing.children,
      total: existing.total,
      page: existing.page
    });
  }

  const metrics = getCodeMetrics(qualifier, branchLike, {
    includeQGStatus: true
  });

  return getChildren(componentKey, metrics, {
    ps: PAGE_SIZE,
    s: 'qualifier,name',
    ...getBranchLikeQuery(branchLike)
  })
    .then(prepareChildren)
    .then(r => {
      if (instance.mounted) {
        addComponentChildren(componentKey, r.components, r.total, r.page);
        storeChildrenBase(r.components);
        storeChildrenBreadcrumbs(componentKey, r.components);
      }
      return r;
    });
}

function retrieveComponentBreadcrumbs(
  component: string,
  instance: { mounted: boolean },
  branchLike?: BranchLike
): Promise<Breadcrumb[]> {
  const existing = getComponentBreadcrumbs(component);
  if (existing) {
    return Promise.resolve(existing);
  }

  return getBreadcrumbs({ component, ...getBranchLikeQuery(branchLike) })
    .then(skipRootDir)
    .then(breadcrumbs => {
      if (instance.mounted) {
        addComponentBreadcrumbs(component, breadcrumbs);
      }
      return breadcrumbs;
    });
}

export function retrieveComponent(
  componentKey: string,
  qualifier: string,
  instance: { mounted: boolean },
  branchLike?: BranchLike
): Promise<{
  breadcrumbs: Breadcrumb[];
  component: ComponentMeasure;
  components: ComponentMeasure[];
  page: number;
  total: number;
}> {
  return Promise.all([
    retrieveComponentBase(componentKey, qualifier, instance, branchLike),
    retrieveComponentChildren(componentKey, qualifier, instance, branchLike),
    retrieveComponentBreadcrumbs(componentKey, instance, branchLike)
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
  instance: { mounted: boolean },
  branchLike?: BranchLike
): Promise<Children> {
  const metrics = getCodeMetrics(qualifier, branchLike, {
    includeQGStatus: true
  });

  return getChildren(componentKey, metrics, {
    ps: PAGE_SIZE,
    p: page,
    s: 'qualifier,name',
    ...getBranchLikeQuery(branchLike)
  })
    .then(prepareChildren)
    .then(r => {
      if (instance.mounted) {
        addComponentChildren(componentKey, r.components, r.total, r.page);
        storeChildrenBase(r.components);
        storeChildrenBreadcrumbs(componentKey, r.components);
      }
      return r;
    });
}
