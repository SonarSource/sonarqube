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
import { waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import React from 'react';
import selectEvent from 'react-select-event';
import AlmIntegrationsServiceMock from '../../../../api/mocks/AlmIntegrationsServiceMock';
import AlmSettingsServiceMock from '../../../../api/mocks/AlmSettingsServiceMock';
import ComponentsServiceMock from '../../../../api/mocks/ComponentsServiceMock';
import DopTranslationServiceMock from '../../../../api/mocks/DopTranslationServiceMock';
import NewCodeDefinitionServiceMock from '../../../../api/mocks/NewCodeDefinitionServiceMock';
import ProjectManagementServiceMock from '../../../../api/mocks/ProjectsManagementServiceMock';
import SettingsServiceMock from '../../../../api/mocks/SettingsServiceMock';
import { mockProject } from '../../../../helpers/mocks/projects';
import { renderApp } from '../../../../helpers/testReactTestingUtils';
import { byRole, byText } from '../../../../helpers/testSelector';
import { AlmKeys } from '../../../../types/alm-settings';
import { Feature } from '../../../../types/features';
import CreateProjectPage from '../CreateProjectPage';
import { CreateProjectModes } from '../types';

jest.mock('../../../../api/alm-integrations');
jest.mock('../../../../api/alm-settings');

let almIntegrationHandler: AlmIntegrationsServiceMock;
let almSettingsHandler: AlmSettingsServiceMock;
let componentsHandler: ComponentsServiceMock;
let dopTranslationHandler: DopTranslationServiceMock;
let newCodePeriodHandler: NewCodeDefinitionServiceMock;
let projectManagementHandler: ProjectManagementServiceMock;
let settingsHandler: SettingsServiceMock;

const ui = {
  addButton: byRole('button', { name: 'onboarding.create_project.monorepo.add_project' }),
  cancelButton: byRole('button', { name: 'cancel' }),
  dopSettingSelector: byRole('combobox', {
    name: `onboarding.create_project.monorepo.choose_dop_setting.${AlmKeys.GitHub}`,
  }),
  gitHubOnboardingTitle: byRole('heading', { name: 'onboarding.create_project.github.title' }),
  monorepoProjectTitle: byRole('heading', {
    name: 'onboarding.create_project.monorepo.project_title',
  }),
  monorepoSetupLink: byRole('link', { name: 'onboarding.create_project.github.subtitle.link' }),
  monorepoTitle: byRole('heading', { name: 'onboarding.create_project.monorepo.titlealm.github' }),
  organizationSelector: byRole('combobox', {
    name: `onboarding.create_project.monorepo.choose_organization.${AlmKeys.GitHub}`,
  }),
  removeButton: byRole('button', { name: 'onboarding.create_project.monorepo.remove_project' }),
  repositorySelector: byRole('combobox', {
    name: `onboarding.create_project.monorepo.choose_repository.${AlmKeys.GitHub}`,
  }),
  notBoundRepositoryMessage: byText(
    'onboarding.create_project.monorepo.choose_repository.no_already_bound_projects',
  ),
  alreadyBoundRepositoryMessage: byText(
    /onboarding.create_project.monorepo.choose_repository.existing_already_bound_projects/,
  ),
  submitButton: byRole('button', { name: 'next' }),
};

beforeAll(() => {
  Object.defineProperty(window, 'location', {
    configurable: true,
    value: { replace: jest.fn() },
  });
  almIntegrationHandler = new AlmIntegrationsServiceMock();
  almSettingsHandler = new AlmSettingsServiceMock();
  componentsHandler = new ComponentsServiceMock();
  dopTranslationHandler = new DopTranslationServiceMock();
  newCodePeriodHandler = new NewCodeDefinitionServiceMock();
  settingsHandler = new SettingsServiceMock();
  projectManagementHandler = new ProjectManagementServiceMock(settingsHandler);
});

beforeEach(() => {
  jest.clearAllMocks();
  almIntegrationHandler.reset();
  almSettingsHandler.reset();
  componentsHandler.reset();
  dopTranslationHandler.reset();
  newCodePeriodHandler.reset();
  projectManagementHandler.reset();
  settingsHandler.reset();
});

describe('github monorepo project setup', () => {
  it('should be able to access monorepo setup page from GitHub project import page', async () => {
    const user = userEvent.setup();
    renderCreateProject({ isMonorepo: false });

    await ui.monorepoSetupLink.find();

    await user.click(await ui.monorepoSetupLink.find());

    expect(ui.monorepoTitle.get()).toBeInTheDocument();
  });

  it('should be able to go back to GitHub onboarding page from monorepo setup page', async () => {
    const user = userEvent.setup();
    renderCreateProject();

    await user.click(await ui.cancelButton.find());

    expect(ui.gitHubOnboardingTitle.get()).toBeInTheDocument();
  });

  it('should display that selected repository is not bound to any existing project', async () => {
    renderCreateProject({ code: '123', dopSetting: 'dop-setting-test-id', isMonorepo: true });

    expect(await ui.monorepoTitle.find()).toBeInTheDocument();

    expect(await ui.dopSettingSelector.find()).toBeInTheDocument();
    expect(ui.monorepoProjectTitle.query()).not.toBeInTheDocument();

    await waitFor(async () => {
      await selectEvent.select(await ui.organizationSelector.find(), 'org-1');
    });
    expect(ui.monorepoProjectTitle.query()).not.toBeInTheDocument();

    await selectEvent.select(await ui.repositorySelector.find(), 'Github repo 1');

    expect(await ui.notBoundRepositoryMessage.find()).toBeInTheDocument();
  });

  it('should display that selected repository is already bound to an existing project', async () => {
    projectManagementHandler.setProjects([
      mockProject({
        key: 'key123',
        name: 'Project GitHub 1',
      }),
    ]);
    renderCreateProject({ code: '123', dopSetting: 'dop-setting-test-id', isMonorepo: true });

    expect(await ui.monorepoTitle.find()).toBeInTheDocument();

    expect(await ui.dopSettingSelector.find()).toBeInTheDocument();
    expect(ui.monorepoProjectTitle.query()).not.toBeInTheDocument();

    await waitFor(async () => {
      await selectEvent.select(await ui.organizationSelector.find(), 'org-1');
    });
    expect(ui.monorepoProjectTitle.query()).not.toBeInTheDocument();

    await selectEvent.select(await ui.repositorySelector.find(), 'Github repo 1');

    expect(await ui.alreadyBoundRepositoryMessage.find()).toBeInTheDocument();
    expect(byRole('link', { name: 'Project GitHub 1' }).get()).toBeInTheDocument();
  });

  it('should be able to set a monorepo project', async () => {
    const user = userEvent.setup();
    renderCreateProject({ code: '123', dopSetting: 'dop-setting-test-id', isMonorepo: true });

    expect(await ui.monorepoTitle.find()).toBeInTheDocument();

    expect(await ui.dopSettingSelector.find()).toBeInTheDocument();
    expect(ui.monorepoProjectTitle.query()).not.toBeInTheDocument();

    await waitFor(async () => {
      await selectEvent.select(await ui.organizationSelector.find(), 'org-1');
    });
    expect(ui.monorepoProjectTitle.query()).not.toBeInTheDocument();

    await selectEvent.select(await ui.repositorySelector.find(), 'Github repo 1');
    expect(await ui.monorepoProjectTitle.find()).toBeInTheDocument();
    let projects = byRole('textbox', {
      name: /onboarding.create_project.project_key/,
    }).getAll();
    expect(projects).toHaveLength(1);
    expect(projects[0]).toHaveValue('org-1_Github-repo-1_add-your-reference');
    expect(ui.submitButton.get()).toBeEnabled();

    await user.click(ui.addButton.get());
    await waitFor(() => {
      projects = byRole('textbox', {
        name: /onboarding.create_project.project_key/,
      }).getAll();
      expect(projects).toHaveLength(2);
    });
    expect(projects[0]).toHaveValue('org-1_Github-repo-1_add-your-reference');
    expect(projects[1]).toHaveValue('org-1_Github-repo-1_add-your-reference-1');
    expect(ui.submitButton.get()).toBeEnabled();

    await user.type(projects[0], '-1');
    expect(ui.submitButton.get()).toBeDisabled();
    await user.clear(projects[1]);
    expect(ui.submitButton.get()).toBeDisabled();

    await user.click(ui.removeButton.getAll()[0]);
    await waitFor(() => {
      projects = byRole('textbox', {
        name: /onboarding.create_project.project_key/,
      }).getAll();
      expect(projects).toHaveLength(1);
    });
    expect(projects[0]).toHaveValue('');
    expect(ui.submitButton.get()).toBeDisabled();

    await user.type(projects[0], 'project-key');
    expect(ui.submitButton.get()).toBeEnabled();
  });
});

function renderCreateProject({
  code,
  dopSetting,
  isMonorepo = true,
}: {
  code?: string;
  dopSetting?: string;
  isMonorepo?: boolean;
} = {}) {
  let queryString = `mode=${CreateProjectModes.GitHub}`;
  if (isMonorepo) {
    queryString += '&mono=true';
  }
  if (dopSetting !== undefined) {
    queryString += `&dopSetting=${dopSetting}`;
  }
  if (code !== undefined) {
    queryString += `&code=${code}`;
  }

  renderApp('projects/create', <CreateProjectPage />, {
    navigateTo: `projects/create?${queryString}`,
    featureList: [Feature.MonoRepositoryPullRequestDecoration],
  });
}
