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
import { screen, waitFor } from '@testing-library/react';

import userEvent from '@testing-library/user-event';
import * as React from 'react';
import selectEvent from 'react-select-event';
import { getGithubRepositories } from '../../../../api/alm-integrations';
import AlmIntegrationsServiceMock from '../../../../api/mocks/AlmIntegrationsServiceMock';
import AlmSettingsServiceMock from '../../../../api/mocks/AlmSettingsServiceMock';
import NewCodeDefinitionServiceMock from '../../../../api/mocks/NewCodeDefinitionServiceMock';
import { mockGitHubRepository } from '../../../../helpers/mocks/alm-integrations';
import { renderApp } from '../../../../helpers/testReactTestingUtils';
import { byLabelText, byRole, byText } from '../../../../helpers/testSelector';
import CreateProjectPage from '../CreateProjectPage';

jest.mock('../../../../api/alm-integrations');
jest.mock('../../../../api/alm-settings');

const original = window.location;

let almIntegrationHandler: AlmIntegrationsServiceMock;
let almSettingsHandler: AlmSettingsServiceMock;
let newCodePeriodHandler: NewCodeDefinitionServiceMock;

const ui = {
  githubCreateProjectButton: byText('onboarding.create_project.select_method.github'),
  instanceSelector: byLabelText(/alm.configuration.selector.label/),
  organizationSelector: byLabelText('onboarding.create_project.github.choose_organization'),
  project1: byRole('listitem', { name: 'Github repo 1' }),
  project1Checkbox: byRole('listitem', { name: 'Github repo 1' }).byRole('checkbox'),
  project2: byRole('listitem', { name: 'Github repo 2' }),
  project2Checkbox: byRole('listitem', { name: 'Github repo 2' }).byRole('checkbox'),
  project3: byRole('listitem', { name: 'Github repo 3' }),
  project3Checkbox: byRole('listitem', { name: 'Github repo 3' }).byRole('checkbox'),
  checkAll: byRole('checkbox', { name: 'onboarding.create_project.select_all_repositories' }),
  importButton: byRole('button', { name: 'onboarding.create_project.import' }),
  newCodeTitle: byRole('heading', { name: 'onboarding.create_project.new_code_definition.title' }),
  createProjectButton: byRole('button', {
    name: 'onboarding.create_project.new_code_definition.create_project',
  }),
  globalSettingRadio: byRole('radio', { name: 'new_code_definition.global_setting' }),
};

beforeAll(() => {
  Object.defineProperty(window, 'location', {
    configurable: true,
    value: { replace: jest.fn() },
  });
  almIntegrationHandler = new AlmIntegrationsServiceMock();
  almSettingsHandler = new AlmSettingsServiceMock();
  newCodePeriodHandler = new NewCodeDefinitionServiceMock();
});

beforeEach(() => {
  jest.clearAllMocks();
  almIntegrationHandler.reset();
  almSettingsHandler.reset();
  newCodePeriodHandler.reset();
});

afterAll(() => {
  Object.defineProperty(window, 'location', { configurable: true, value: original });
});

it('should redirect to github authorization page when not already authorized', async () => {
  renderCreateProject('project/create?mode=github');

  expect(await screen.findByText('onboarding.create_project.github.title')).toBeInTheDocument();
  expect(screen.getByText('alm.configuration.selector.placeholder')).toBeInTheDocument();
  expect(ui.instanceSelector.get()).toBeInTheDocument();

  await selectEvent.select(ui.instanceSelector.get(), [/conf-github-1/]);

  expect(window.location.replace).toHaveBeenCalled();
});

it('should not redirect to github when url is malformated', async () => {
  renderCreateProject('project/create?mode=github');

  expect(await screen.findByText('onboarding.create_project.github.title')).toBeInTheDocument();
  expect(screen.getByText('alm.configuration.selector.placeholder')).toBeInTheDocument();
  expect(ui.instanceSelector.get()).toBeInTheDocument();

  await selectEvent.select(ui.instanceSelector.get(), [/conf-github-3/]);

  expect(window.location.replace).not.toHaveBeenCalled();
});

it('should show import project feature when the authentication is successfull', async () => {
  const user = userEvent.setup();

  renderCreateProject('project/create?mode=github&almInstance=conf-github-2&code=213321213');

  expect(await ui.instanceSelector.find()).toBeInTheDocument();

  await selectEvent.select(ui.organizationSelector.get(), [/org-1/]);

  expect(await ui.project1.find()).toBeInTheDocument();
  expect(ui.project2.get()).toBeInTheDocument();
  expect(ui.checkAll.get()).not.toBeChecked();

  expect(ui.project1Checkbox.get()).toBeChecked();
  expect(ui.project1Checkbox.get()).toBeDisabled();

  expect(
    ui.project1.byText('onboarding.create_project.repository_imported').get(),
  ).toBeInTheDocument();

  expect(ui.project1.byRole('link', { name: /Github repo 1/ }).get()).toBeInTheDocument();
  expect(ui.project1.byRole('link', { name: /Github repo 1/ }).get()).toHaveAttribute(
    'href',
    '/dashboard?id=key123',
  );

  expect(ui.project2Checkbox.get()).not.toBeChecked();
  expect(ui.project2Checkbox.get()).toBeEnabled();

  expect(ui.importButton.query()).not.toBeInTheDocument();
  await user.click(ui.project2Checkbox.get());
  await waitFor(() => expect(ui.checkAll.get()).toBeChecked());

  expect(ui.importButton.get()).toBeInTheDocument();
  await user.click(ui.importButton.get());

  expect(await ui.newCodeTitle.find()).toBeInTheDocument();

  await user.click(ui.globalSettingRadio.get());

  expect(ui.createProjectButton.get()).toBeEnabled();
  await user.click(ui.createProjectButton.get());

  expect(await screen.findByText('/dashboard?id=key')).toBeInTheDocument();
});

it('should import several projects', async () => {
  const user = userEvent.setup();

  almIntegrationHandler.setGithubRepositories([
    mockGitHubRepository({ name: 'Github repo 1', key: 'key1' }),
    mockGitHubRepository({ name: 'Github repo 2', key: 'key2' }),
    mockGitHubRepository({ name: 'Github repo 3', key: 'key3' }),
  ]);

  renderCreateProject('project/create?mode=github&almInstance=conf-github-2&code=213321213');

  expect(await ui.instanceSelector.find()).toBeInTheDocument();

  await selectEvent.select(ui.organizationSelector.get(), [/org-1/]);

  expect(await ui.project1.find()).toBeInTheDocument();
  expect(ui.project1Checkbox.get()).not.toBeChecked();
  expect(ui.project2Checkbox.get()).not.toBeChecked();
  expect(ui.project3Checkbox.get()).not.toBeChecked();
  expect(ui.checkAll.get()).not.toBeChecked();
  expect(ui.importButton.query()).not.toBeInTheDocument();

  await user.click(ui.project1Checkbox.get());

  expect(ui.project1Checkbox.get()).toBeChecked();
  expect(ui.project2Checkbox.get()).not.toBeChecked();
  expect(ui.project3Checkbox.get()).not.toBeChecked();
  expect(ui.checkAll.get()).not.toBeChecked();
  expect(ui.importButton.get()).toBeInTheDocument();

  await user.click(ui.checkAll.get());

  expect(ui.project1Checkbox.get()).toBeChecked();
  expect(ui.project2Checkbox.get()).toBeChecked();
  expect(ui.project3Checkbox.get()).toBeChecked();
  expect(ui.checkAll.get()).toBeChecked();
  expect(ui.importButton.get()).toBeInTheDocument();

  await user.click(ui.checkAll.get());

  expect(ui.project1Checkbox.get()).not.toBeChecked();
  expect(ui.project2Checkbox.get()).not.toBeChecked();
  expect(ui.project3Checkbox.get()).not.toBeChecked();
  expect(ui.checkAll.get()).not.toBeChecked();
  expect(ui.importButton.query()).not.toBeInTheDocument();

  await user.click(ui.project1Checkbox.get());
  await user.click(ui.project2Checkbox.get());

  expect(ui.importButton.get()).toBeInTheDocument();
  await user.click(ui.importButton.get());

  expect(await ui.newCodeTitle.find()).toBeInTheDocument();

  // TBD
});

it('should show search filter when the authentication is successful', async () => {
  const user = userEvent.setup();
  renderCreateProject('project/create?mode=github&almInstance=conf-github-2&code=213321213');

  expect(await ui.instanceSelector.find()).toBeInTheDocument();

  await selectEvent.select(ui.organizationSelector.get(), [/org-1/]);

  const inputSearch = screen.getByRole('searchbox');
  await user.click(inputSearch);
  await user.keyboard('search');

  expect(getGithubRepositories).toHaveBeenLastCalledWith({
    almSetting: 'conf-github-2',
    organization: 'org-1',
    page: 1,
    pageSize: 20,
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
    pageSize: 20,
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
