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
import { screen, within } from '@testing-library/react';

import userEvent from '@testing-library/user-event';
import * as React from 'react';
import selectEvent from 'react-select-event';
import { byLabelText, byText } from 'testing-library-selector';
import { getGithubRepositories } from '../../../../api/alm-integrations';
import AlmIntegrationsServiceMock from '../../../../api/mocks/AlmIntegrationsServiceMock';
import AlmSettingsServiceMock from '../../../../api/mocks/AlmSettingsServiceMock';
import { renderApp } from '../../../../helpers/testReactTestingUtils';
import CreateProjectPage from '../CreateProjectPage';

jest.mock('../../../../api/alm-integrations');
jest.mock('../../../../api/alm-settings');

const original = window.location;

let almIntegrationHandler: AlmIntegrationsServiceMock;
let almSettingsHandler: AlmSettingsServiceMock;

const ui = {
  githubCreateProjectButton: byText('onboarding.create_project.select_method.github'),
  instanceSelector: byLabelText(/alm.configuration.selector.label/),
  organizationSelector: byLabelText('onboarding.create_project.github.choose_organization'),
};

beforeAll(() => {
  Object.defineProperty(window, 'location', {
    configurable: true,
    value: { replace: jest.fn() },
  });
  almIntegrationHandler = new AlmIntegrationsServiceMock();
  almSettingsHandler = new AlmSettingsServiceMock();
});

beforeEach(() => {
  jest.clearAllMocks();
  almIntegrationHandler.reset();
  almSettingsHandler.reset();
});

afterAll(() => {
  Object.defineProperty(window, 'location', { configurable: true, value: original });
});

it('should redirect to github authorization page when not already authorized', async () => {
  const user = userEvent.setup();
  renderCreateProject();

  expect(ui.githubCreateProjectButton.get()).toBeInTheDocument();

  await user.click(ui.githubCreateProjectButton.get());
  expect(screen.getByText('onboarding.create_project.github.title')).toBeInTheDocument();
  expect(screen.getByText('alm.configuration.selector.placeholder')).toBeInTheDocument();
  expect(ui.instanceSelector.get()).toBeInTheDocument();

  await selectEvent.select(ui.instanceSelector.get(), [/conf-github-1/]);

  expect(window.location.replace).toHaveBeenCalled();
  expect(
    screen.getByText('onboarding.create_project.github.choose_organization')
  ).toBeInTheDocument();
});

it('should not redirect to github when url is malformated', async () => {
  const user = userEvent.setup();
  renderCreateProject();

  expect(ui.githubCreateProjectButton.get()).toBeInTheDocument();

  await user.click(ui.githubCreateProjectButton.get());
  expect(screen.getByText('onboarding.create_project.github.title')).toBeInTheDocument();
  expect(screen.getByText('alm.configuration.selector.placeholder')).toBeInTheDocument();
  expect(ui.instanceSelector.get()).toBeInTheDocument();

  await selectEvent.select(ui.instanceSelector.get(), [/conf-github-3/]);

  expect(window.location.replace).not.toHaveBeenCalled();
});

it('should show import project feature when the authentication is successfull', async () => {
  const user = userEvent.setup();
  let repoItem;

  renderCreateProject('project/create?mode=github&almInstance=conf-github-2&code=213321213');

  expect(await ui.instanceSelector.find()).toBeInTheDocument();

  await selectEvent.select(ui.organizationSelector.get(), [/org-1/]);

  expect(screen.getByText('Github repo 1')).toBeInTheDocument();
  expect(screen.getByText('Github repo 2')).toBeInTheDocument();

  repoItem = screen.getByRole('radio', {
    name: 'Github repo 1',
  });

  expect(
    within(repoItem).getByText('onboarding.create_project.repository_imported')
  ).toBeInTheDocument();

  expect(within(repoItem).getByRole('link', { name: /Github repo 1/ })).toBeInTheDocument();
  expect(within(repoItem).getByRole('link', { name: /Github repo 1/ })).toHaveAttribute(
    'href',
    '/dashboard?id=key123'
  );

  repoItem = screen.getByRole('radio', {
    name: 'Github repo 2',
  });

  const importButton = screen.getByText('onboarding.create_project.import_selected_repo');

  expect(repoItem).toBeInTheDocument();
  expect(importButton).toBeDisabled();
  await user.click(repoItem);
  expect(importButton).toBeEnabled();
  await user.click(importButton);

  expect(await screen.findByText('/dashboard?id=key')).toBeInTheDocument();
});

it('should show search filter when the authentication is successful', async () => {
  const user = userEvent.setup();
  renderCreateProject('project/create?mode=github&almInstance=conf-github-2&code=213321213');

  expect(await ui.instanceSelector.find()).toBeInTheDocument();

  await selectEvent.select(ui.organizationSelector.get(), [/org-1/]);

  const inputSearch = screen.getByRole('searchbox', {
    name: 'onboarding.create_project.search_repositories',
  });
  await user.click(inputSearch);
  await user.keyboard('search');

  expect(getGithubRepositories).toHaveBeenLastCalledWith({
    almSetting: 'conf-github-2',
    organization: 'org-1',
    page: 1,
    pageSize: 30,
    query: 'search',
  });
});

it('should have load more', async () => {
  const user = userEvent.setup();
  almIntegrationHandler.createRandomGithubRepositoriessWithLoadMore(10, 20);

  renderCreateProject('project/create?mode=github&almInstance=conf-github-2&code=213321213');

  expect(await ui.instanceSelector.find()).toBeInTheDocument();

  await selectEvent.select(ui.organizationSelector.get(), [/org-1/]);

  const loadMore = screen.getByRole('button', { name: 'show_more' });
  expect(loadMore).toBeInTheDocument();

  /*
   * Next api call response will simulate reaching the last page so we can test the
   * loadmore button disapperance.
   */
  almIntegrationHandler.createRandomGithubRepositoriessWithLoadMore(20, 20);
  await user.click(loadMore);
  expect(getGithubRepositories).toHaveBeenLastCalledWith({
    almSetting: 'conf-github-2',
    organization: 'org-1',
    page: 2,
    pageSize: 30,
    query: '',
  });
  expect(loadMore).not.toBeInTheDocument();
});

it('should show no result message when there are no projects', async () => {
  almIntegrationHandler.setGithubRepositories([]);

  renderCreateProject('project/create?mode=github&almInstance=conf-github-2&code=213321213');

  expect(await ui.instanceSelector.find()).toBeInTheDocument();

  await selectEvent.select(ui.organizationSelector.get(), [/org-1/]);

  expect(screen.getByText('no_results')).toBeInTheDocument();
});

function renderCreateProject(navigateTo?: string) {
  renderApp('project/create', <CreateProjectPage />, {
    navigateTo,
  });
}
