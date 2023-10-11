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
import { act, screen } from '@testing-library/react';

import userEvent from '@testing-library/user-event';
import * as React from 'react';
import selectEvent from 'react-select-event';
import { searchAzureRepositories } from '../../../../api/alm-integrations';
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
  azureCreateProjectButton: byText('onboarding.create_project.select_method.azure'),
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
  expect(await screen.findByText('onboarding.create_project.azure.title')).toBeInTheDocument();
  expect(screen.getByText('alm.configuration.selector.label.alm.azure.long')).toBeInTheDocument();

  expect(screen.getByText('onboarding.create_project.enter_pat')).toBeInTheDocument();
  expect(screen.getByText('onboarding.create_project.pat_form.title')).toBeInTheDocument();
  expect(screen.getByRole('button', { name: 'save' })).toBeInTheDocument();

  await user.click(ui.personalAccessTokenInput.get());
  await user.keyboard('secret');
  await user.click(screen.getByRole('button', { name: 'save' }));

  expect(screen.getByText('Azure project')).toBeInTheDocument();
  expect(screen.getByText('Azure project 2')).toBeInTheDocument();
  // eslint-disable-next-line jest-dom/prefer-in-document
  expect(screen.getAllByText('onboarding.create_project.repository_imported')).toHaveLength(1);
});

it('should show import project feature when PAT is already set', async () => {
  const user = userEvent.setup();

  renderCreateProject();
  expect(await screen.findByText('onboarding.create_project.azure.title')).toBeInTheDocument();

  await act(async () => {
    await selectEvent.select(ui.instanceSelector.get(), [/conf-azure-2/]);
  });

  expect(screen.getByText('Azure project')).toBeInTheDocument();
  expect(screen.getByText('Azure project 2')).toBeInTheDocument();

  expect(
    screen.getByRole('listitem', {
      name: 'Azure repo 1',
    }),
  ).toBeInTheDocument();

  expect(
    screen.getByRole('listitem', {
      name: 'Azure repo 2',
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
  expect(await screen.findByText('onboarding.create_project.azure.title')).toBeInTheDocument();

  await act(async () => {
    await selectEvent.select(ui.instanceSelector.get(), [/conf-azure-2/]);
  });

  // Should search with positive results
  const inputSearch = screen.getByPlaceholderText(
    'onboarding.create_project.search_projects_repositories',
  );
  await user.click(inputSearch);
  await user.keyboard('s');

  expect(searchAzureRepositories).toHaveBeenCalledWith('conf-azure-2', 's');

  // Should search with empty results
  almIntegrationHandler.setSearchAzureRepositories([]);
  await user.keyboard('f');
  expect(screen.getByText('onboarding.create_project.azure.no_results')).toBeInTheDocument();
});

function renderCreateProject() {
  renderApp('project/create', <CreateProjectPage />, {
    navigateTo: 'project/create?mode=azure',
  });
}
