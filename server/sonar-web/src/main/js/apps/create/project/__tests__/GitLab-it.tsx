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
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import * as React from 'react';
import selectEvent from 'react-select-event';
import { getGitlabProjects } from '../../../../api/alm-integrations';
import AlmIntegrationsServiceMock from '../../../../api/mocks/AlmIntegrationsServiceMock';
import DopTranslationServiceMock from '../../../../api/mocks/DopTranslationServiceMock';
import NewCodeDefinitionServiceMock from '../../../../api/mocks/NewCodeDefinitionServiceMock';
import { renderApp } from '../../../../helpers/testReactTestingUtils';
import { byLabelText, byRole, byText } from '../../../../helpers/testSelector';
import { Feature } from '../../../../types/features';
import CreateProjectPage from '../CreateProjectPage';
import { CreateProjectModes } from '../types';

jest.mock('../../../../api/alm-integrations');
jest.mock('../../../../api/alm-settings');

let almIntegrationHandler: AlmIntegrationsServiceMock;
let dopTranslationHandler: DopTranslationServiceMock;
let newCodePeriodHandler: NewCodeDefinitionServiceMock;

const ui = {
  cancelButton: byRole('button', { name: 'cancel' }),
  gitlabCreateProjectButton: byText('onboarding.create_project.select_method.gitlab'),
  gitLabOnboardingTitle: byRole('heading', { name: 'onboarding.create_project.gitlab.title' }),
  instanceSelector: byLabelText(/alm.configuration.selector.label/),
  monorepoSetupLink: byRole('link', {
    name: 'onboarding.create_project.subtitle_monorepo_setup_link',
  }),
  monorepoTitle: byRole('heading', { name: 'onboarding.create_project.monorepo.titlealm.gitlab' }),

  personalAccessTokenInput: byRole('textbox', {
    name: /onboarding.create_project.enter_pat/,
  }),
};

const original = window.location;

beforeAll(() => {
  Object.defineProperty(window, 'location', {
    configurable: true,
    value: { replace: jest.fn() },
  });
  almIntegrationHandler = new AlmIntegrationsServiceMock();
  dopTranslationHandler = new DopTranslationServiceMock();
  newCodePeriodHandler = new NewCodeDefinitionServiceMock();
});

beforeEach(() => {
  jest.clearAllMocks();
  almIntegrationHandler.reset();
  dopTranslationHandler.reset();
  newCodePeriodHandler.reset();
});

afterAll(() => {
  Object.defineProperty(window, 'location', { configurable: true, value: original });
});

it('should ask for PAT when it is not set yet and show the import project feature afterwards', async () => {
  const user = userEvent.setup();
  renderCreateProject();

  expect(await screen.findByText('onboarding.create_project.gitlab.title')).toBeInTheDocument();
  expect(ui.instanceSelector.get()).toBeInTheDocument();

  expect(screen.getByText('onboarding.create_project.enter_pat')).toBeInTheDocument();
  expect(
    screen.getByText('onboarding.create_project.pat_help.instructions.gitlab'),
  ).toBeInTheDocument();
  expect(screen.getByRole('button', { name: 'save' })).toBeInTheDocument();

  await user.click(ui.personalAccessTokenInput.get());
  await user.keyboard('secret');
  await user.click(screen.getByRole('button', { name: 'save' }));

  expect(screen.getByText('Gitlab project 1')).toBeInTheDocument();
  expect(screen.getByText('Gitlab project 2')).toBeInTheDocument();
  expect(screen.getAllByText('onboarding.create_project.import')).toHaveLength(2);
  expect(screen.getByText('onboarding.create_project.repository_imported')).toBeInTheDocument();
});

it('should show import project feature when PAT is already set', async () => {
  const user = userEvent.setup();
  let projectItem;
  renderCreateProject();

  expect(await screen.findByText('onboarding.create_project.gitlab.title')).toBeInTheDocument();
  await selectEvent.select(ui.instanceSelector.get(), [/conf-final-2/]);

  expect(await screen.findByText('Gitlab project 1')).toBeInTheDocument();
  expect(screen.getByText('Gitlab project 2')).toBeInTheDocument();

  projectItem = screen.getByRole('listitem', { name: /Gitlab project 1/ });
  expect(
    within(projectItem).getByText('onboarding.create_project.repository_imported'),
  ).toBeInTheDocument();
  expect(within(projectItem).getByRole('link', { name: /Gitlab project 1/ })).toBeInTheDocument();
  expect(within(projectItem).getByRole('link', { name: /Gitlab project 1/ })).toHaveAttribute(
    'href',
    '/dashboard?id=key',
  );

  projectItem = screen.getByRole('listitem', { name: /Gitlab project 2/ });
  const importButton = within(projectItem).getByRole('button', {
    name: 'onboarding.create_project.import',
  });

  await user.click(importButton);

  expect(
    screen.getByRole('heading', { name: 'onboarding.create_x_project.new_code_definition.title1' }),
  ).toBeInTheDocument();

  await user.click(screen.getByRole('radio', { name: 'new_code_definition.global_setting' }));
  await user.click(
    screen.getByRole('button', {
      name: 'onboarding.create_project.new_code_definition.create_x_projects1',
    }),
  );

  expect(await screen.findByText('/dashboard?id=key')).toBeInTheDocument();
});

it('should show search filter when PAT is already set', async () => {
  const user = userEvent.setup();
  renderCreateProject();

  expect(await screen.findByText('onboarding.create_project.gitlab.title')).toBeInTheDocument();

  await selectEvent.select(ui.instanceSelector.get(), [/conf-final-2/]);

  const inputSearch = await screen.findByRole('searchbox');
  await user.click(inputSearch);
  await user.keyboard('sea');

  await waitFor(() => expect(getGitlabProjects).toHaveBeenCalledTimes(2));
  expect(getGitlabProjects).toHaveBeenLastCalledWith({
    almSetting: 'conf-final-2',
    page: 1,
    pageSize: 50,
    query: 'sea',
  });
});

it('should have load more', async () => {
  const user = userEvent.setup();
  almIntegrationHandler.createRandomGitlabProjectsWithLoadMore(50, 75);
  renderCreateProject();

  expect(await screen.findByText('onboarding.create_project.gitlab.title')).toBeInTheDocument();
  await selectEvent.select(ui.instanceSelector.get(), [/conf-final-2/]);
  const loadMore = await screen.findByRole('button', { name: 'show_more' });
  expect(loadMore).toBeInTheDocument();

  /*
   * Next api call response will simulate reaching the last page so we can test the
   * loadmore button disapperance.
   */
  almIntegrationHandler.createRandomGitlabProjectsWithLoadMore(50, 50);
  await user.click(loadMore);
  expect(getGitlabProjects).toHaveBeenLastCalledWith({
    almSetting: 'conf-final-2',
    page: 2,
    pageSize: 50,
    query: '',
  });
  expect(loadMore).not.toBeInTheDocument();
});

it('should show no result message when there are no projects', async () => {
  almIntegrationHandler.setGitlabProjects([]);
  renderCreateProject();

  expect(await screen.findByText('onboarding.create_project.gitlab.title')).toBeInTheDocument();
  await selectEvent.select(ui.instanceSelector.get(), [/conf-final-2/]);

  expect(
    await screen.findByText('onboarding.create_project.gitlab.no_projects'),
  ).toBeInTheDocument();
});

describe('GitLab monorepo project navigation', () => {
  it('should be able to access monorepo setup page from GitLab project import page', async () => {
    const user = userEvent.setup();
    renderCreateProject();

    await user.click(await ui.monorepoSetupLink.find());

    expect(ui.monorepoTitle.get()).toBeInTheDocument();
  });

  it('should be able to go back to GitLab onboarding page from monorepo setup page', async () => {
    const user = userEvent.setup();
    renderCreateProject({ isMonorepo: true });

    await user.click(await ui.cancelButton.find());

    expect(ui.gitLabOnboardingTitle.get()).toBeInTheDocument();
  });
});

function renderCreateProject({
  isMonorepo = false,
}: {
  isMonorepo?: boolean;
} = {}) {
  let queryString = `mode=${CreateProjectModes.GitLab}`;
  if (isMonorepo) {
    queryString += '&mono=true';
  }

  renderApp('projects/create', <CreateProjectPage />, {
    navigateTo: `projects/create?${queryString}`,
    featureList: [Feature.MonoRepositoryPullRequestDecoration],
  });
}
