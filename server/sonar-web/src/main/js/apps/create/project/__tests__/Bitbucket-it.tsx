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
import { screen, within } from '@testing-library/react';

import userEvent from '@testing-library/user-event';
import * as React from 'react';
import selectEvent from 'react-select-event';
import { searchForBitbucketServerRepositories } from '../../../../api/alm-integrations';
import AlmIntegrationsServiceMock from '../../../../api/mocks/AlmIntegrationsServiceMock';
import AlmSettingsServiceMock from '../../../../api/mocks/AlmSettingsServiceMock';
import NewCodeDefinitionServiceMock from '../../../../api/mocks/NewCodeDefinitionServiceMock';
import { renderApp } from '../../../../helpers/testReactTestingUtils';
import { byLabelText, byRole, byText } from '../../../../helpers/testSelector';
import CreateProjectPage from '../CreateProjectPage';

jest.mock('../../../../api/alm-integrations');
jest.mock('../../../../api/alm-settings');

let almIntegrationHandler: AlmIntegrationsServiceMock;
let almSettingsHandler: AlmSettingsServiceMock;
let newCodePeriodHandler: NewCodeDefinitionServiceMock;

const ui = {
  bitbucketServerCreateProjectButton: byText('onboarding.create_project.select_method.bitbucket'),
  personalAccessTokenInput: byRole('textbox', {
    name: /onboarding.create_project.enter_pat/,
  }),
  instanceSelector: byLabelText(/alm.configuration.selector.label/),
};
const original = window.location;

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

it('should ask for PAT when it is not set yet and show the import project feature afterwards', async () => {
  const user = userEvent.setup();
  renderCreateProject();

  expect(screen.getByText('onboarding.create_project.bitbucket.title')).toBeInTheDocument();
  expect(await ui.instanceSelector.find()).toBeInTheDocument();

  expect(screen.getByText('onboarding.create_project.pat_form.title')).toBeInTheDocument();

  expect(screen.getByRole('button', { name: 'save' })).toBeDisabled();

  await user.click(
    screen.getByRole('textbox', {
      name: /onboarding.create_project.enter_pat/,
    }),
  );

  await user.keyboard('password');

  expect(screen.getByRole('button', { name: 'save' })).toBeEnabled();
  await user.click(screen.getByRole('button', { name: 'save' }));

  expect(screen.getByText('Bitbucket Project 1')).toBeInTheDocument();
  expect(screen.getByText('Bitbucket Project 2')).toBeInTheDocument();
});

it('should show import project feature when PAT is already set', async () => {
  const user = userEvent.setup();
  renderCreateProject();

  expect(screen.getByText('onboarding.create_project.bitbucket.title')).toBeInTheDocument();
  expect(await ui.instanceSelector.find()).toBeInTheDocument();

  await selectEvent.select(ui.instanceSelector.get(), [/conf-bitbucketserver-2/]);

  expect(await screen.findByText('Bitbucket Project 1')).toBeInTheDocument();

  const projectItem = screen.getByRole('region', { name: /Bitbucket Project 1/ });

  expect(
    within(projectItem).getByText('onboarding.create_project.repository_imported'),
  ).toBeInTheDocument();

  expect(within(projectItem).getByRole('link', { name: /Bitbucket Repo 1/ })).toBeInTheDocument();
  expect(within(projectItem).getByRole('link', { name: /Bitbucket Repo 1/ })).toHaveAttribute(
    'href',
    '/dashboard?id=key',
  );

  await user.click(projectItem);

  expect(
    screen.getByRole('listitem', {
      name: 'Bitbucket Repo 1',
    }),
  ).toBeInTheDocument();

  expect(
    screen.getByRole('listitem', {
      name: 'Bitbucket Repo 2',
    }),
  ).toBeInTheDocument();

  const importButton = screen.getByText('onboarding.create_project.import');
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

  expect(screen.getByText('onboarding.create_project.bitbucket.title')).toBeInTheDocument();
  expect(await ui.instanceSelector.find()).toBeInTheDocument();

  await selectEvent.select(ui.instanceSelector.get(), [/conf-bitbucketserver-2/]);

  const inputSearch = await screen.findByRole('searchbox', {
    name: 'onboarding.create_project.search_repositories_by_name',
  });
  await user.click(inputSearch);
  await user.keyboard('search');

  expect(searchForBitbucketServerRepositories).toHaveBeenLastCalledWith(
    'conf-bitbucketserver-2',
    'search',
  );
});

it('should show no result message when there are no projects', async () => {
  almIntegrationHandler.setBitbucketServerProjects([]);
  renderCreateProject();
  expect(screen.getByText('onboarding.create_project.bitbucket.title')).toBeInTheDocument();
  expect(await ui.instanceSelector.find()).toBeInTheDocument();

  await selectEvent.select(ui.instanceSelector.get(), [/conf-bitbucketserver-2/]);

  expect(await screen.findByText('onboarding.create_project.no_bbs_projects')).toBeInTheDocument();
});

function renderCreateProject() {
  renderApp('project/create', <CreateProjectPage />, {
    navigateTo: 'project/create?mode=bitbucket',
  });
}
