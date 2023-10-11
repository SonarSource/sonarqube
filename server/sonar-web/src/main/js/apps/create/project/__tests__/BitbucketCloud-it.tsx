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
import { act, screen, waitFor, within } from '@testing-library/react';

import userEvent from '@testing-library/user-event';
import * as React from 'react';
import selectEvent from 'react-select-event';
import { searchForBitbucketCloudRepositories } from '../../../../api/alm-integrations';
import AlmIntegrationsServiceMock from '../../../../api/mocks/AlmIntegrationsServiceMock';
import AlmSettingsServiceMock from '../../../../api/mocks/AlmSettingsServiceMock';
import NewCodeDefinitionServiceMock from '../../../../api/mocks/NewCodeDefinitionServiceMock';
import { renderApp } from '../../../../helpers/testReactTestingUtils';
import { byLabelText, byRole, byText } from '../../../../helpers/testSelector';
import CreateProjectPage from '../CreateProjectPage';
import { BITBUCKET_CLOUD_PROJECTS_PAGESIZE } from '../constants';

jest.mock('../../../../api/alm-integrations');
jest.mock('../../../../api/alm-settings');

let almIntegrationHandler: AlmIntegrationsServiceMock;
let almSettingsHandler: AlmSettingsServiceMock;
let newCodePeriodHandler: NewCodeDefinitionServiceMock;

const ui = {
  bitbucketCloudCreateProjectButton: byText(
    'onboarding.create_project.select_method.bitbucketcloud',
  ),
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

  expect(screen.getByText('onboarding.create_project.bitbucketcloud.title')).toBeInTheDocument();
  expect(await ui.instanceSelector.find()).toBeInTheDocument();

  expect(
    screen.getByText('onboarding.create_project.bitbucket_cloud.enter_password'),
  ).toBeInTheDocument();
  expect(
    screen.getByText('onboarding.create_project.enter_password.instructions.bitbucket_cloud'),
  ).toBeInTheDocument();

  expect(
    screen.getByText(
      'onboarding.create_project.pat.expired.info_message onboarding.create_project.pat.expired.info_message_contact',
    ),
  ).toBeInTheDocument();

  expect(screen.getByRole('button', { name: 'save' })).toBeDisabled();

  await user.click(
    screen.getByRole('textbox', {
      name: /onboarding.create_project.bitbucket_cloud.enter_username/,
    }),
  );

  await user.keyboard('username');

  await user.click(
    screen.getByRole('textbox', {
      name: /onboarding.create_project.bitbucket_cloud.enter_password/,
    }),
  );

  await user.keyboard('password');

  expect(screen.getByRole('button', { name: 'save' })).toBeEnabled();
  await user.click(screen.getByRole('button', { name: 'save' }));

  expect(screen.getByText('BitbucketCloud Repo 1')).toBeInTheDocument();
  expect(screen.getByText('BitbucketCloud Repo 2')).toBeInTheDocument();
});

it('should show import project feature when PAT is already set', async () => {
  const user = userEvent.setup();
  let projectItem;
  renderCreateProject();

  expect(screen.getByText('onboarding.create_project.bitbucketcloud.title')).toBeInTheDocument();
  expect(await ui.instanceSelector.find()).toBeInTheDocument();

  await act(async () => {
    await selectEvent.select(ui.instanceSelector.get(), [/conf-bitbucketcloud-2/]);
  });

  expect(screen.getByText('BitbucketCloud Repo 1')).toBeInTheDocument();
  expect(screen.getByText('BitbucketCloud Repo 2')).toBeInTheDocument();

  projectItem = screen.getByRole('listitem', { name: /BitbucketCloud Repo 1/ });
  expect(
    within(projectItem).getByText('onboarding.create_project.repository_imported'),
  ).toBeInTheDocument();

  expect(
    within(projectItem).getByRole('link', { name: /BitbucketCloud Repo 1/ }),
  ).toBeInTheDocument();
  expect(within(projectItem).getByRole('link', { name: /BitbucketCloud Repo 1/ })).toHaveAttribute(
    'href',
    '/dashboard?id=key',
  );

  projectItem = screen.getByRole('listitem', { name: /BitbucketCloud Repo 2/ });
  const setupButton = within(projectItem).getByRole('button', {
    name: 'onboarding.create_project.import',
  });

  await user.click(setupButton);

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

  expect(screen.getByText('onboarding.create_project.bitbucketcloud.title')).toBeInTheDocument();
  expect(await ui.instanceSelector.find()).toBeInTheDocument();

  await act(async () => {
    await selectEvent.select(ui.instanceSelector.get(), [/conf-bitbucketcloud-2/]);
  });

  expect(searchForBitbucketCloudRepositories).toHaveBeenLastCalledWith(
    'conf-bitbucketcloud-2',
    '',
    BITBUCKET_CLOUD_PROJECTS_PAGESIZE,
    1,
  );

  const inputSearch = screen.getByRole('searchbox', {
    name: 'onboarding.create_project.search_prompt',
  });
  await user.click(inputSearch);
  await user.keyboard('search');

  expect(searchForBitbucketCloudRepositories).toHaveBeenLastCalledWith(
    'conf-bitbucketcloud-2',
    'search',
    BITBUCKET_CLOUD_PROJECTS_PAGESIZE,
    1,
  );
});

it('should show no result message when there are no projects', async () => {
  almIntegrationHandler.setBitbucketCloudRepositories([]);
  renderCreateProject();

  expect(screen.getByText('onboarding.create_project.bitbucketcloud.title')).toBeInTheDocument();
  expect(await ui.instanceSelector.find()).toBeInTheDocument();

  await act(async () => {
    await selectEvent.select(ui.instanceSelector.get(), [/conf-bitbucketcloud-2/]);
  });

  expect(
    screen.getByText('onboarding.create_project.bitbucketcloud.no_projects'),
  ).toBeInTheDocument();
});

it('should have load more', async () => {
  const user = userEvent.setup();
  almIntegrationHandler.createRandomBitbucketCloudProjectsWithLoadMore(
    BITBUCKET_CLOUD_PROJECTS_PAGESIZE,
    BITBUCKET_CLOUD_PROJECTS_PAGESIZE + 1,
  );
  renderCreateProject();

  expect(screen.getByText('onboarding.create_project.bitbucketcloud.title')).toBeInTheDocument();
  expect(await ui.instanceSelector.find()).toBeInTheDocument();

  await act(async () => {
    await selectEvent.select(ui.instanceSelector.get(), [/conf-bitbucketcloud-2/]);
  });

  expect(screen.getByRole('button', { name: 'show_more' })).toBeInTheDocument();

  /*
   * Next api call response will simulate reaching the last page so we can test the
   * loadmore button disapperance.
   */
  almIntegrationHandler.createRandomBitbucketCloudProjectsWithLoadMore(
    BITBUCKET_CLOUD_PROJECTS_PAGESIZE + 1,
    BITBUCKET_CLOUD_PROJECTS_PAGESIZE + 1,
  );
  await user.click(screen.getByRole('button', { name: 'show_more' }));

  expect(searchForBitbucketCloudRepositories).toHaveBeenLastCalledWith(
    'conf-bitbucketcloud-2',
    '',
    BITBUCKET_CLOUD_PROJECTS_PAGESIZE,
    2,
  );

  await waitFor(() => {
    expect(screen.queryByRole('button', { name: 'show_more' })).not.toBeInTheDocument();
  });
});

function renderCreateProject() {
  renderApp('project/create', <CreateProjectPage />, {
    navigateTo: 'project/create?mode=bitbucketcloud',
  });
}
