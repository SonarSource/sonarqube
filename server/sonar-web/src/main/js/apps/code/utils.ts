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
import { getBreadcrumbs, getChildren, getComponent, getComponentData } from '../../api/components';
import { getBranchLikeQuery, isPullRequest } from '../../helpers/branch-like';
import { CCT_SOFTWARE_QUALITY_METRICS, OLD_TAXONOMY_METRICS } from '../../helpers/constants';
import { BranchLike } from '../../types/branch-like';
import { ComponentQualifier, isPortfolioLike } from '../../types/component';
import { MetricKey } from '../../types/metrics';
import { Breadcrumb, ComponentMeasure } from '../../types/types';
import {
  addComponent,
  addComponentBreadcrumbs,
  addComponentChildren,
  getComponentBreadcrumbs,
  getComponentChildren,
  getComponent as getComponentFromBucket,
} from './bucket';

const METRICS = [
  MetricKey.ncloc,
  ...CCT_SOFTWARE_QUALITY_METRICS,
  ...OLD_TAXONOMY_METRICS,
  MetricKey.security_hotspots,
  MetricKey.coverage,
  MetricKey.duplicated_lines_density,
];

const APPLICATION_METRICS = [MetricKey.alert_status, ...METRICS];

const PORTFOLIO_METRICS = [
  MetricKey.releasability_rating,
  MetricKey.reliability_rating,
  MetricKey.security_rating,
  MetricKey.security_review_rating,
  MetricKey.sqale_rating,
  MetricKey.ncloc,
];

const NEW_PORTFOLIO_METRICS = [
  MetricKey.releasability_rating,
  MetricKey.new_reliability_rating,
  MetricKey.new_security_rating,
  MetricKey.new_security_review_rating,
  MetricKey.new_maintainability_rating,
  MetricKey.new_lines,
];

const LEAK_METRICS = [
  MetricKey.new_lines,
  ...CCT_SOFTWARE_QUALITY_METRICS,
  ...OLD_TAXONOMY_METRICS,
  MetricKey.security_hotspots,
  MetricKey.new_coverage,
  MetricKey.new_duplicated_lines_density,
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
    page: r.paging.pageIndex,
  };
}

function skipRootDir(breadcrumbs: ComponentMeasure[]) {
  return breadcrumbs.filter((component) => {
    return !(component.qualifier === ComponentQualifier.Directory && component.name === '/');
  });
}

function storeChildrenBase(children: ComponentMeasure[]) {
  children.forEach(addComponent);
}

function storeChildrenBreadcrumbs(parentComponentKey: string, children: Breadcrumb[]) {
  const parentBreadcrumbs = getComponentBreadcrumbs(parentComponentKey);
  if (parentBreadcrumbs) {
    children.forEach((child) => {
      const breadcrumbs = [...parentBreadcrumbs, child];
      addComponentBreadcrumbs(child.key, breadcrumbs);
    });
  }
}

export function getCodeMetrics(
  qualifier: string,
  branchLike?: BranchLike,
  options: { includeQGStatus?: boolean; newCode?: boolean } = {},
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
  if (qualifier === ComponentQualifier.Application) {
    return [...APPLICATION_METRICS];
  }
  if (isPullRequest(branchLike)) {
    return [...LEAK_METRICS];
  }
  return [...METRICS];
}

function retrieveComponentBase(
  componentKey: string,
  qualifier: string,
  instance: { mounted: boolean },
  branchLike?: BranchLike,
) {
  const existing = getComponentFromBucket(componentKey);
  if (existing) {
    return Promise.resolve(existing);
  }

  const metrics = getCodeMetrics(qualifier, branchLike);

  // eslint-disable-next-line local-rules/no-api-imports
  return getComponent({
    component: componentKey,
    metricKeys: metrics.join(),
    ...getBranchLikeQuery(branchLike),
  }).then(({ component }) => {
    if (instance.mounted) {
      addComponent(component);
    }
    return component;
  });
}

export async function retrieveComponentChildren(
  componentKey: string,
  qualifier: string,
  instance: { mounted: boolean },
  branchLike?: BranchLike,
): Promise<{ components: ComponentMeasure[]; page: number; total: number }> {
  const existing = getComponentChildren(componentKey);
  if (existing) {
    return Promise.resolve({
      components: existing.children,
      total: existing.total,
      page: existing.page,
    });
  }

  const metrics = getCodeMetrics(qualifier, branchLike, {
    includeQGStatus: true,
  });

  // eslint-disable-next-line local-rules/no-api-imports
  const result = await getChildren(componentKey, metrics, {
    ps: PAGE_SIZE,
    s: 'qualifier,name',
    ...getBranchLikeQuery(branchLike),
  }).then(prepareChildren);

  if (instance.mounted && isPortfolioLike(qualifier)) {
    await Promise.all(
      // eslint-disable-next-line local-rules/no-api-imports
      result.components.map((c) =>
        getComponentData({ component: c.refKey ?? c.key, branch: c.branch }),
      ),
    ).then(
      (data) => {
        data.forEach(({ component: { analysisDate } }, i) => {
          result.components[i].analysisDate = analysisDate;
        });
      },
      () => {
        // noop
      },
    );
  }

  if (instance.mounted) {
    addComponentChildren(componentKey, result.components, result.total, result.page);
    storeChildrenBase(result.components);
    storeChildrenBreadcrumbs(componentKey, result.components);
  }

  return result;
}

function retrieveComponentBreadcrumbs(
  component: string,
  instance: { mounted: boolean },
  branchLike?: BranchLike,
): Promise<Breadcrumb[]> {
  const existing = getComponentBreadcrumbs(component);
  if (existing) {
    return Promise.resolve(existing);
  }

  // eslint-disable-next-line local-rules/no-api-imports
  return getBreadcrumbs({ component, ...getBranchLikeQuery(branchLike) })
    .then(skipRootDir)
    .then((breadcrumbs) => {
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
  branchLike?: BranchLike,
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
    retrieveComponentBreadcrumbs(componentKey, instance, branchLike),
  ]).then((r) => {
    return {
      breadcrumbs: r[2],
      component: r[0],
      components: r[1].components,
      page: r[1].page,
      total: r[1].total,
    };
  });
}

export function loadMoreChildren(
  componentKey: string,
  page: number,
  qualifier: string,
  instance: { mounted: boolean },
  branchLike?: BranchLike,
): Promise<Children> {
  const metrics = getCodeMetrics(qualifier, branchLike, {
    includeQGStatus: true,
  });

  // eslint-disable-next-line local-rules/no-api-imports
  return getChildren(componentKey, metrics, {
    ps: PAGE_SIZE,
    p: page,
    s: 'qualifier,name',
    ...getBranchLikeQuery(branchLike),
  })
    .then(prepareChildren)
    .then((r) => {
      if (instance.mounted) {
        addComponentChildren(componentKey, r.components, r.total, r.page);
        storeChildrenBase(r.components);
        storeChildrenBreadcrumbs(componentKey, r.components);
      }
      return r;
    });
}

export function mostCommonPrefix(strings: string[]) {
  const sortedStrings = strings.slice(0).sort((a, b) => a.localeCompare(b));
  const firstString = sortedStrings[0];
  const firstStringLength = firstString.length;
  const lastString = sortedStrings[sortedStrings.length - 1];
  let i = 0;
  while (i < firstStringLength && firstString.charAt(i) === lastString.charAt(i)) {
    i++;
  }
  const prefix = firstString.slice(0, i);
  const prefixTokens = prefix.split(/[\s\\/]/);
  const lastPrefixPart = prefixTokens[prefixTokens.length - 1];
  return prefix.slice(0, prefix.length - lastPrefixPart.length);
}
