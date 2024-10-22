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

import { byRole, byText } from '../../../sonar-aligned/helpers/testSelector';

import userEvent from '@testing-library/user-event';
import { DEBOUNCE_DELAY } from '~design-system';
import BranchesServiceMock from '../../../api/mocks/BranchesServiceMock';
import DependenciesServiceMock from '../../../api/mocks/DependenciesServiceMock';
import SettingsServiceMock from '../../../api/mocks/SettingsServiceMock';
import { mockComponent } from '../../../helpers/mocks/component';
import { renderAppWithComponentContext } from '../../../helpers/testReactTestingUtils';
import { DependenciesResponse } from '../../../types/dependencies';
import { Component } from '../../../types/types';
import routes from '../routes';

const depsHandler = new DependenciesServiceMock();
const branchesHandler = new BranchesServiceMock();
const settingsHandler = new SettingsServiceMock();
const MOCK_RESPONSE: DependenciesResponse = {
  page: {
    pageIndex: 1,
    pageSize: 100,
    total: 4,
  },
  dependencies: [
    {
      key: '1',
      name: 'jackson-databind',
      longName: 'com.fasterxml.jackson.core:jackson-databind',
      version: '2.10.0',
      fixVersion: '2.12.13',
      transitive: false,
      findingsCount: 16,
      findingsSeverities: { BLOCKER: 1, HIGH: 2, MEDIUM: 2, LOW: 2, INFO: 9 },
      findingsExploitableCount: 1,
      project: 'project1',
    },
    {
      key: '2',
      name: 'snappy-java',
      longName: 'org.xerial.snappy:snappy-java',
      version: '3.52',
      fixVersion: '4.6.1',
      transitive: true,
      findingsCount: 2,
      findingsSeverities: { LOW: 2 },
      findingsExploitableCount: 0,
      project: 'project1',
    },
    {
      key: '3',
      name: 'SnakeYAML',
      longName: 'org.yaml:SnakeYAML',
      version: '2.10.0',
      transitive: true,
      findingsCount: 3,
      findingsSeverities: { INFO: 3 },
      findingsExploitableCount: 0,
      project: 'project1',
    },
    {
      key: '4',
      name: 'random-lib',
      longName: 'com.random:random-lib',
      version: '2.10.0',
      transitive: true,
      findingsCount: 0,
      findingsSeverities: {},
      findingsExploitableCount: 0,
      project: 'project1',
    },
  ],
};

const MOCK_RESPONSE_NO_FINDINGS: DependenciesResponse = {
  page: {
    pageIndex: 1,
    pageSize: 100,
    total: 4,
  },
  dependencies: [
    {
      key: '1',
      name: 'jackson-databind',
      longName: 'com.fasterxml.jackson.core:jackson-databind',
      version: '2.10.0',
      transitive: false,
      project: 'project1',
    },
    {
      key: '2',
      name: 'snappy-java',
      longName: 'org.xerial.snappy:snappy-java',
      version: '3.52',
      transitive: true,
      project: 'project1',
    },
    {
      key: '3',
      name: 'SnakeYAML',
      longName: 'org.yaml:SnakeYAML',
      version: '2.10.0',
      transitive: true,
      project: 'project1',
    },
    {
      key: '4',
      name: 'random-lib',
      longName: 'com.random:random-lib',
      version: '2.10.0',
      transitive: true,
      project: 'project1',
    },
  ],
};

beforeEach(() => {
  branchesHandler.reset();
  depsHandler.reset();
  settingsHandler.reset();
});

it('should correctly show an empty state', async () => {
  const { ui } = getPageObject();
  renderDependenciesApp();

  expect(await ui.emptyStateTitle.find()).toBeInTheDocument();
  expect(await ui.emptyStateLink.find()).toBeInTheDocument();
});

it('should correctly render dependencies with findings', async () => {
  depsHandler.setDefaultDependencies(MOCK_RESPONSE);
  const { ui } = getPageObject();

  renderDependenciesApp();

  expect(await ui.dependencies.findAll()).toHaveLength(4);
});

it('should correctly render dependencies when no finding information is available', async () => {
  depsHandler.setDefaultDependencies(MOCK_RESPONSE_NO_FINDINGS);
  const { ui } = getPageObject();

  renderDependenciesApp();

  expect(await ui.dependencies.findAll()).toHaveLength(4);
  expect(byText('dependencies.dependency.no_findings.label').query()).not.toBeInTheDocument();
});

it('should correctly search for dependencies', async () => {
  depsHandler.setDefaultDependencies(MOCK_RESPONSE_NO_FINDINGS);
  const { ui, user } = getPageObject();

  renderDependenciesApp();

  expect(await ui.dependencies.findAll()).toHaveLength(4);

  user.type(ui.searchInput.get(), 'jackson');

  // Wait for input debounce
  await new Promise((resolve) => {
    setTimeout(resolve, DEBOUNCE_DELAY);
  });

  expect(await ui.dependencies.findAll()).toHaveLength(1);
});

it('should correctly show empty results state when no dependencies are found', async () => {
  depsHandler.setDefaultDependencies(MOCK_RESPONSE_NO_FINDINGS);
  const { ui, user } = getPageObject();

  renderDependenciesApp();

  expect(await ui.dependencies.findAll()).toHaveLength(4);

  user.type(ui.searchInput.get(), 'asd');

  // Wait for input debounce
  await new Promise((resolve) => {
    setTimeout(resolve, DEBOUNCE_DELAY);
  });

  expect(await ui.searchTitle.get()).toBeInTheDocument();
});

function getPageObject() {
  const user = userEvent.setup();
  const ui = {
    emptyStateTitle: byText('dependencies.empty_state.title'),
    emptyStateLink: byRole('link', {
      name: /dependencies.empty_state.link_text/,
    }),
    dependencies: byRole('listitem'),
    searchInput: byRole('searchbox'),
    searchTitle: byText('dependencies.list.name_search.title0'),
  };
  return { ui, user };
}

function renderDependenciesApp(
  { navigateTo, component }: { component: Component; navigateTo?: string } = {
    component: mockComponent(),
  },
) {
  return renderAppWithComponentContext('dependencies', routes, { navigateTo }, { component });
}
