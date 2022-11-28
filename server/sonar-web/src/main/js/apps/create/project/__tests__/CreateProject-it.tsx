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
import { act, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import * as React from 'react';
import selectEvent from 'react-select-event';
import { byLabelText, byRole, byText } from 'testing-library-selector';
import AlmIntegrationsServiceMock from '../../../../api/mocks/AlmIntegrationsServiceMock';
import AlmSettingsServiceMock from '../../../../api/mocks/AlmSettingsServiceMock';
import { renderApp } from '../../../../helpers/testReactTestingUtils';
import CreateProjectPage, { CreateProjectPageProps } from '../CreateProjectPage';

jest.mock('../../../../api/alm-integrations');
jest.mock('../../../../api/alm-settings');

const original = window.location;

let almIntegrationHandler: AlmIntegrationsServiceMock;
let almSettingsHandler: AlmSettingsServiceMock;

const ui = {
  gitlabCreateProjectButton: byText('onboarding.create_project.select_method.gitlab'),
  githubCreateProjectButton: byText('onboarding.create_project.select_method.github'),
  personalAccessTokenInput: byRole('textbox', {
    name: 'onboarding.create_project.enter_pat field_required',
  }),
  instanceSelector: byLabelText(/alm.configuration.selector.label/),
};

beforeAll(() => {
  Object.defineProperty(window, 'location', {
    configurable: true,
    value: { replace: jest.fn() },
  });
  almIntegrationHandler = new AlmIntegrationsServiceMock();
  almSettingsHandler = new AlmSettingsServiceMock();
});

afterEach(() => {
  almIntegrationHandler.reset();
  almSettingsHandler.reset();
});

afterAll(() => {
  Object.defineProperty(window, 'location', { configurable: true, value: original });
});

describe('Gitlab onboarding page', () => {
  it('should ask for PAT when it is not set yet and show the import project feature afterwards', async () => {
    const user = userEvent.setup();
    renderCreateProject();
    expect(ui.gitlabCreateProjectButton.get()).toBeInTheDocument();

    await user.click(ui.gitlabCreateProjectButton.get());
    expect(screen.getByText('onboarding.create_project.gitlab.title')).toBeInTheDocument();
    expect(ui.instanceSelector.get()).toBeInTheDocument();

    expect(screen.getByText('onboarding.create_project.enter_pat')).toBeInTheDocument();
    expect(screen.getByText('onboarding.create_project.pat_help.title')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'save' })).toBeInTheDocument();

    await user.click(ui.personalAccessTokenInput.get());
    await user.keyboard('secret');
    await user.click(screen.getByRole('button', { name: 'save' }));

    expect(screen.getByText('Gitlab project 1')).toBeInTheDocument();
    expect(screen.getByText('Gitlab project 2')).toBeInTheDocument();
    expect(screen.getAllByText('onboarding.create_project.set_up')).toHaveLength(2);
    expect(screen.getByText('onboarding.create_project.repository_imported')).toBeInTheDocument();
  });

  it('should show import project feature when PAT is already set', async () => {
    const user = userEvent.setup();
    renderCreateProject();
    await act(async () => {
      await user.click(ui.gitlabCreateProjectButton.get());
      await selectEvent.select(ui.instanceSelector.get(), [/conf-final-2/]);
    });

    expect(screen.getByText('Gitlab project 1')).toBeInTheDocument();
    expect(screen.getByText('Gitlab project 2')).toBeInTheDocument();
  });

  it('should show no result message when there are no projects', async () => {
    const user = userEvent.setup();
    almIntegrationHandler.setGitlabProjects([]);
    renderCreateProject();
    await act(async () => {
      await user.click(ui.gitlabCreateProjectButton.get());
      await selectEvent.select(ui.instanceSelector.get(), [/conf-final-2/]);
    });

    expect(screen.getByText('onboarding.create_project.gitlab.no_projects')).toBeInTheDocument();
  });
});

describe('Github onboarding page', () => {
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
});

function renderCreateProject(props: Partial<CreateProjectPageProps> = {}) {
  renderApp('project/create', <CreateProjectPage {...props} />);
}
