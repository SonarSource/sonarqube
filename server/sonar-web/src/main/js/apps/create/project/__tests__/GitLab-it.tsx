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

import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { byLabelText, byRole, byText } from '~sonar-aligned/helpers/testSelector';
import { getGitlabProjects } from '../../../../api/alm-integrations';
import AlmIntegrationsServiceMock from '../../../../api/mocks/AlmIntegrationsServiceMock';
import DopTranslationServiceMock from '../../../../api/mocks/DopTranslationServiceMock';
import NewCodeDefinitionServiceMock from '../../../../api/mocks/NewCodeDefinitionServiceMock';
import { mockGitlabProject } from '../../../../helpers/mocks/alm-integrations';
import { renderApp } from '../../../../helpers/testReactTestingUtils';
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
  importProjectsTitle: byText('onboarding.create_project.gitlab.title'),
  monorepoSetupLink: byRole('link', {
    name: 'onboarding.create_project.subtitle_monorepo_setup_link',
  }),
  monorepoTitle: byRole('heading', { name: 'onboarding.create_project.monorepo.titlealm.gitlab' }),
  patHelpInstructions: byText('onboarding.create_project.pat_help.instructions.gitlab'),
  personalAccessTokenInput: byRole('textbox', {
    name: /onboarding.create_project.enter_pat/,
  }),

  // Bulk import
  checkAll: byRole('checkbox', { name: 'onboarding.create_project.select_all_repositories' }),
  project1: byRole('listitem', { name: 'Gitlab project 1' }),
  project1Checkbox: byRole('listitem', { name: 'Gitlab project 1' }).byRole('checkbox'),
  project1Link: byRole('listitem', { name: 'Gitlab project 1' }).byRole('link', {
    name: 'Gitlab project 1',
  }),
  project1GitlabLink: byRole('listitem', { name: 'Gitlab project 1' }).byRole('link', {
    name: 'onboarding.create_project.see_on.alm.gitlab',
  }),
  project2: byRole('listitem', { name: 'Gitlab project 2' }),
  project2Checkbox: byRole('listitem', { name: 'Gitlab project 2' }).byRole('checkbox'),
  project3: byRole('listitem', { name: 'Gitlab project 3' }),
  project3Checkbox: byRole('listitem', { name: 'Gitlab project 3' }).byRole('checkbox'),
  importButton: byRole('button', { name: 'onboarding.create_project.import' }),
  saveButton: byRole('button', { name: 'save' }),
  backButton: byRole('button', { name: 'back' }),
  newCodeMultipleProjectTitle: byRole('heading', {
    name: 'onboarding.create_x_project.new_code_definition.title2',
  }),
  changePeriodLaterInfo: byText('onboarding.create_projects.new_code_definition.change_info'),
  createProjectButton: byRole('button', {
    name: 'onboarding.create_project.new_code_definition.create_x_projects1',
  }),
  createProjectsButton: byRole('button', {
    name: 'onboarding.create_project.new_code_definition.create_x_projects2',
  }),
  globalSettingRadio: byRole('radio', { name: 'new_code_definition.global_setting' }),
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

  expect(await ui.importProjectsTitle.find()).toBeInTheDocument();
  expect(ui.instanceSelector.get()).toBeInTheDocument();
  await user.click(ui.instanceSelector.get());
  await user.click(byRole('option', { name: /conf-final-1/ }).get());

  expect(await screen.findByText('onboarding.create_project.enter_pat')).toBeInTheDocument();
  expect(ui.patHelpInstructions.get()).toBeInTheDocument();
  expect(ui.saveButton.get()).toBeInTheDocument();

  await user.click(ui.personalAccessTokenInput.get());
  await user.keyboard('secret');
  await user.click(ui.saveButton.get());

  expect(await ui.project1.find()).toBeInTheDocument();
});

it('should show import project feature when PAT is already set', async () => {
  const user = userEvent.setup();
  renderCreateProject();

  expect(await ui.importProjectsTitle.find()).toBeInTheDocument();
  await user.click(ui.instanceSelector.get());
  await user.click(byRole('option', { name: /conf-final-2/ }).get());

  expect(await ui.project1.find()).toBeInTheDocument();
  expect(ui.project1Link.get()).toHaveAttribute('href', '/dashboard?id=key');
  expect(ui.project1GitlabLink.get()).toHaveAttribute(
    'href',
    'https://gitlab.company.com/best-projects/awesome-project-exclamation',
  );
});

it('should show search filter when PAT is already set', async () => {
  const user = userEvent.setup();
  renderCreateProject();

  expect(await ui.importProjectsTitle.find()).toBeInTheDocument();

  await user.click(ui.instanceSelector.get());
  await user.click(byRole('option', { name: /conf-final-2/ }).get());

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

it('should import several projects', async () => {
  const user = userEvent.setup();

  almIntegrationHandler.setGitlabProjects([
    mockGitlabProject({ id: '1', name: 'Gitlab project 1' }),
    mockGitlabProject({ id: '2', name: 'Gitlab project 2' }),
    mockGitlabProject({ id: '3', name: 'Gitlab project 3' }),
  ]);

  renderCreateProject();

  expect(await ui.importProjectsTitle.find()).toBeInTheDocument();
  await user.click(ui.instanceSelector.get());
  await user.click(byRole('option', { name: /conf-final-2/ }).get());

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

  expect(await ui.newCodeMultipleProjectTitle.find()).toBeInTheDocument();
  expect(ui.changePeriodLaterInfo.get()).toBeInTheDocument();
  expect(ui.createProjectsButton.get()).toBeDisabled();

  await user.click(ui.backButton.get());
  expect(ui.project1Checkbox.get()).toBeChecked();
  expect(ui.project2Checkbox.get()).toBeChecked();
  expect(ui.project3Checkbox.get()).not.toBeChecked();
  expect(ui.importButton.get()).toBeInTheDocument();
  await user.click(ui.importButton.get());

  expect(await ui.newCodeMultipleProjectTitle.find()).toBeInTheDocument();

  await user.click(ui.globalSettingRadio.get());
  expect(ui.createProjectsButton.get()).toBeEnabled();
  await user.click(ui.createProjectsButton.get());

  expect(await screen.findByText('/projects?sort=-creation_date')).toBeInTheDocument();
});

it('should have load more', async () => {
  const user = userEvent.setup();
  almIntegrationHandler.createRandomGitlabProjectsWithLoadMore(50, 75);
  renderCreateProject();

  await user.click(await ui.instanceSelector.find());
  await user.click(byRole('option', { name: /conf-final-2/ }).get());

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
  const user = userEvent.setup();
  almIntegrationHandler.setGitlabProjects([]);
  renderCreateProject();

  expect(await ui.importProjectsTitle.find()).toBeInTheDocument();
  await user.click(ui.instanceSelector.get());
  await user.click(byRole('option', { name: /conf-final-2/ }).get());

  expect(await screen.findByText('no_results')).toBeInTheDocument();
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
