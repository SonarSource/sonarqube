/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import without from 'lodash/without';

import {
    addComponent,
    getComponent as getComponentFromBucket,
    addComponentChildren,
    getComponentChildren,
    addComponentBreadcrumbs,
    getComponentBreadcrumbs
} from './bucket';
import { getChildren, getComponent, getBreadcrumbs } from '../../api/components';
import { translate } from '../../helpers/l10n';

const METRICS = [
  'ncloc',
  'code_smells',
  'bugs',
  'vulnerabilities',
  'duplicated_lines_density',
  'alert_status'
];

const METRICS_WITH_COVERAGE = [
  ...METRICS,
  'coverage',
  'it_coverage',
  'overall_coverage'
];

const PAGE_SIZE = 100;

function expandRootDir ({ components, total, ...other }) {
  const rootDir = components.find(component => component.qualifier === 'DIR' && component.name === '/');
  if (rootDir) {
    return getChildren(rootDir.key, METRICS_WITH_COVERAGE).then(r => {
      const nextComponents = without([...r.components, ...components], rootDir);
      const nextTotal = total + r.components.length - /* root dir */ 1;
      return { components: nextComponents, total: nextTotal, ...other };
    });
  } else {
    return { components, total, ...other };
  }
}

function prepareChildren (r) {
  return {
    components: r.components,
    total: r.paging.total,
    page: r.paging.pageIndex
  };
}

function skipRootDir (breadcrumbs) {
  return breadcrumbs.filter(component => {
    return !(component.qualifier === 'DIR' && component.name === '/');
  });
}

function storeChildrenBase (children) {
  children.forEach(addComponent);
}

function storeChildrenBreadcrumbs (parentComponentKey, children) {
  const parentBreadcrumbs = getComponentBreadcrumbs(parentComponentKey);
  if (parentBreadcrumbs) {
    children.forEach(child => {
      const breadcrumbs = [...parentBreadcrumbs, child];
      addComponentBreadcrumbs(child.key, breadcrumbs);
    });
  }
}

export function retrieveComponentBase (componentKey) {
  const existing = getComponentFromBucket(componentKey);
  if (existing) {
    return Promise.resolve(existing);
  }

  return getComponent(componentKey, METRICS_WITH_COVERAGE).then(component => {
    addComponent(component);
    return component;
  });
}

function retrieveComponentChildren (componentKey) {
  const existing = getComponentChildren(componentKey);
  if (existing) {
    return Promise.resolve({
      components: existing.children,
      total: existing.total
    });
  }

  return getChildren(componentKey, METRICS_WITH_COVERAGE, { ps: PAGE_SIZE, s: 'name' })
      .then(prepareChildren)
      .then(expandRootDir)
      .then(r => {
        addComponentChildren(componentKey, r.components, r.total);
        storeChildrenBase(r.components);
        storeChildrenBreadcrumbs(componentKey, r.components);
        return r;
      });
}

function retrieveComponentBreadcrumbs (componentKey) {
  const existing = getComponentBreadcrumbs(componentKey);
  if (existing) {
    return Promise.resolve(existing);
  }

  return getBreadcrumbs({ key: componentKey })
      .then(skipRootDir)
      .then(breadcrumbs => {
        addComponentBreadcrumbs(componentKey, breadcrumbs);
        return breadcrumbs;
      });
}

export function retrieveComponent (componentKey) {
  return Promise.all([
    retrieveComponentBase(componentKey),
    retrieveComponentChildren(componentKey),
    retrieveComponentBreadcrumbs(componentKey)
  ]).then(r => {
    return {
      component: r[0],
      components: r[1].components,
      total: r[1].total,
      page: r[1].page,
      breadcrumbs: r[2]
    };
  });
}

export function loadMoreChildren (componentKey, page) {
  return getChildren(componentKey, METRICS_WITH_COVERAGE, { ps: PAGE_SIZE, p: page })
      .then(prepareChildren)
      .then(expandRootDir)
      .then(r => {
        addComponentChildren(componentKey, r.components, r.total);
        storeChildrenBase(r.components);
        storeChildrenBreadcrumbs(componentKey, r.components);
        return r;
      });
}

export function parseError (error) {
  const DEFAULT_MESSAGE = translate('default_error_message');

  try {
    return error.response.json()
        .then(r => r.errors.map(error => error.msg).join('. '))
        .catch(() => DEFAULT_MESSAGE);
  } catch (ex) {
    return Promise.resolve(DEFAULT_MESSAGE);
  }
}
