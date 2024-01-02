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
import { act, screen, within } from '@testing-library/react';

import userEvent from '@testing-library/user-event';
import * as React from 'react';
import selectEvent from 'react-select-event';
import { byLabelText, byRole, byText } from 'testing-library-selector';
import { searchForBitbucketServerRepositories } from '../../../../api/alm-integrations';
import AlmIntegrationsServiceMock from '../../../../api/mocks/AlmIntegrationsServiceMock';
import AlmSettingsServiceMock from '../../../../api/mocks/AlmSettingsServiceMock';
import { renderApp } from '../../../../helpers/testReactTestingUtils';
import CreateProjectPage, { CreateProjectPageProps } from '../CreateProjectPage';

jest.mock('../../../../api/alm-integrations');
jest.mock('../../../../api/alm-settings');

let almIntegrationHandler: AlmIntegrationsServiceMock;
let almSettingsHandler: AlmSettingsServiceMock;

const ui = {
  bitbucketServerCreateProjectButton: byText('onboarding.create_project.select_method.bitbucket'),
  personalAccessTokenInput: byRole('textbox', {
    name: /onboarding.create_project.enter_pat/,
  }),
  instanceSelector: byLabelText(/alm.configuration.selector.label/),
};

beforeAll(() => {
  almIntegrationHandler = new AlmIntegrationsServiceMock();
  almSettingsHandler = new AlmSettingsServiceMock();
});

beforeEach(() => {
  jest.clearAllMocks();
  almIntegrationHandler.reset();
  almSettingsHandler.reset();
});

it('should ask for PAT when it is not set yet and show the import project feature afterwards', async () => {
  const user = userEvent.setup();
  renderCreateProject();
  expect(ui.bitbucketServerCreateProjectButton.get()).toBeInTheDocument();

  await user.click(ui.bitbucketServerCreateProjectButton.get());
  expect(screen.getByText('onboarding.create_project.from_bbs')).toBeInTheDocument();
  expect(ui.instanceSelector.get()).toBeInTheDocument();

  expect(
    screen.getByText('onboarding.create_project.pat_form.title.bitbucket')
  ).toBeInTheDocument();

  expect(screen.getByRole('button', { name: 'save' })).toBeDisabled();

  await user.click(
    screen.getByRole('textbox', {
      name: /onboarding.create_project.enter_pat/,
    })
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
  await act(async () => {
    await user.click(ui.bitbucketServerCreateProjectButton.get());
    await selectEvent.select(ui.instanceSelector.get(), [/conf-bitbucketserver-2/]);
  });

  expect(screen.getByText('Bitbucket Project 1')).toBeInTheDocument();

  await user.click(screen.getByRole('button', { name: 'expand_all' }));

  expect(screen.getByRole('button', { name: 'collapse_all' })).toBeInTheDocument();

  const projectItem = screen.getByRole('region', { name: /Bitbucket Project 1/ });

  expect(
    within(projectItem).getByText('onboarding.create_project.repository_imported')
  ).toBeInTheDocument();

  expect(within(projectItem).getByRole('link', { name: /Bitbucket Repo 1/ })).toBeInTheDocument();
  expect(within(projectItem).getByRole('link', { name: /Bitbucket Repo 1/ })).toHaveAttribute(
    'href',
    '/dashboard?id=key'
  );

  await user.click(projectItem);
  const radioButton = within(projectItem).getByRole('radio', {
    name: 'Bitbucket Repo 2',
  });
  const importButton = screen.getByText('onboarding.create_project.import_selected_repo');

  expect(radioButton).toBeInTheDocument();
  expect(importButton).toBeDisabled();
  await user.click(radioButton);
  expect(importButton).toBeEnabled();
  await user.click(importButton);
  expect(await screen.findByText('/dashboard?id=key')).toBeInTheDocument();
});

it('should show search filter when PAT is already set', async () => {
  const user = userEvent.setup();
  renderCreateProject();

  await act(async () => {
    await user.click(ui.bitbucketServerCreateProjectButton.get());
    await selectEvent.select(ui.instanceSelector.get(), [/conf-bitbucketserver-2/]);
  });

  const inputSearch = screen.getByRole('searchbox', {
    name: 'onboarding.create_project.search_repositories_by_name',
  });
  await user.click(inputSearch);
  await user.keyboard('search');

  expect(searchForBitbucketServerRepositories).toHaveBeenLastCalledWith(
    'conf-bitbucketserver-2',
    'search'
  );
});

it('should show no result message when there are no projects', async () => {
  const user = userEvent.setup();
  almIntegrationHandler.setBitbucketServerProjects([]);
  renderCreateProject();
  await act(async () => {
    await user.click(ui.bitbucketServerCreateProjectButton.get());
    await selectEvent.select(ui.instanceSelector.get(), [/conf-bitbucketserver-2/]);
  });

  expect(screen.getByRole('alert')).toHaveTextContent('onboarding.create_project.no_bbs_projects');
});

function renderCreateProject(props: Partial<CreateProjectPageProps> = {}) {
  renderApp('project/create', <CreateProjectPage {...props} />);
}
