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
import { searchForBitbucketCloudRepositories } from '../../../../api/alm-integrations';
import AlmIntegrationsServiceMock from '../../../../api/mocks/AlmIntegrationsServiceMock';
import AlmSettingsServiceMock from '../../../../api/mocks/AlmSettingsServiceMock';
import { renderApp } from '../../../../helpers/testReactTestingUtils';
import CreateProjectPage, { CreateProjectPageProps } from '../CreateProjectPage';

jest.mock('../../../../api/alm-integrations');
jest.mock('../../../../api/alm-settings');

let almIntegrationHandler: AlmIntegrationsServiceMock;
let almSettingsHandler: AlmSettingsServiceMock;

const ui = {
  bitbucketCloudCreateProjectButton: byText(
    'onboarding.create_project.select_method.bitbucketcloud'
  ),
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
  expect(ui.bitbucketCloudCreateProjectButton.get()).toBeInTheDocument();

  await user.click(ui.bitbucketCloudCreateProjectButton.get());
  expect(
    screen.getByRole('heading', { name: 'onboarding.create_project.bitbucketcloud.title' })
  ).toBeInTheDocument();
  expect(ui.instanceSelector.get()).toBeInTheDocument();

  expect(
    screen.getByText('onboarding.create_project.enter_pat.bitbucketcloud')
  ).toBeInTheDocument();
  expect(
    screen.getByText(
      'onboarding.create_project.pat_help.instructions_username.bitbucketcloud.title'
    )
  ).toBeInTheDocument();

  expect(
    screen.getByText('onboarding.create_project.pat.expired.info_message')
  ).toBeInTheDocument();
  expect(
    screen.getByText('onboarding.create_project.pat.expired.info_message_contact')
  ).toBeInTheDocument();

  expect(screen.getByRole('button', { name: 'save' })).toBeDisabled();

  await user.click(
    screen.getByRole('textbox', {
      name: /onboarding.create_project.enter_username/,
    })
  );

  await user.keyboard('username');

  await user.click(
    screen.getByRole('textbox', {
      name: /onboarding.create_project.enter_pat.bitbucketcloud/,
    })
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
  await act(async () => {
    await user.click(ui.bitbucketCloudCreateProjectButton.get());
    await selectEvent.select(ui.instanceSelector.get(), [/conf-bitbucketcloud-2/]);
  });

  expect(screen.getByText('BitbucketCloud Repo 1')).toBeInTheDocument();
  expect(screen.getByText('BitbucketCloud Repo 2')).toBeInTheDocument();

  projectItem = screen.getByRole('row', {
    name: 'qualifier.TRK BitbucketCloud Repo 1 project opens_in_new_window onboarding.create_project.bitbucketcloud.link onboarding.create_project.repository_imported',
  });
  expect(
    within(projectItem).getByText('onboarding.create_project.repository_imported')
  ).toBeInTheDocument();

  expect(
    within(projectItem).getByRole('link', { name: /BitbucketCloud Repo 1/ })
  ).toBeInTheDocument();
  expect(within(projectItem).getByRole('link', { name: /BitbucketCloud Repo 1/ })).toHaveAttribute(
    'href',
    '/dashboard?id=key'
  );

  projectItem = screen.getByRole('row', {
    name: 'BitbucketCloud Repo 2 project opens_in_new_window onboarding.create_project.bitbucketcloud.link onboarding.create_project.set_up',
  });
  const importProjectButton = within(projectItem).getByRole('button', {
    name: 'onboarding.create_project.set_up',
  });

  await user.click(importProjectButton);
  expect(await screen.findByText('/dashboard?id=key')).toBeInTheDocument();
});

it('should show search filter when PAT is already set', async () => {
  const user = userEvent.setup();
  renderCreateProject();

  await act(async () => {
    await user.click(ui.bitbucketCloudCreateProjectButton.get());
    await selectEvent.select(ui.instanceSelector.get(), [/conf-bitbucketcloud-2/]);
  });

  expect(searchForBitbucketCloudRepositories).toHaveBeenLastCalledWith(
    'conf-bitbucketcloud-2',
    '',
    30,
    1
  );

  const inputSearch = screen.getByRole('searchbox', {
    name: 'onboarding.create_project.search_prompt',
  });
  await user.click(inputSearch);
  await user.keyboard('search');

  expect(searchForBitbucketCloudRepositories).toHaveBeenLastCalledWith(
    'conf-bitbucketcloud-2',
    'search',
    30,
    1
  );
});

it('should show no result message when there are no projects', async () => {
  const user = userEvent.setup();
  almIntegrationHandler.setBitbucketCloudRepositories([]);
  renderCreateProject();
  await act(async () => {
    await user.click(ui.bitbucketCloudCreateProjectButton.get());
    await selectEvent.select(ui.instanceSelector.get(), [/conf-bitbucketcloud-2/]);
  });

  expect(screen.getByRole('alert')).toHaveTextContent(
    'onboarding.create_project.bitbucketcloud.no_projects'
  );
});

it('should have load more', async () => {
  const user = userEvent.setup();
  almIntegrationHandler.createRandomBitbucketCloudProjectsWithLoadMore(2, 4);
  renderCreateProject();
  await act(async () => {
    await user.click(ui.bitbucketCloudCreateProjectButton.get());
    await selectEvent.select(ui.instanceSelector.get(), [/conf-bitbucketcloud-2/]);
  });

  const loadMore = screen.getByRole('button', { name: 'show_more' });
  expect(loadMore).toBeInTheDocument();

  /*
   * Next api call response will simulate reaching the last page so we can test the
   * loadmore button disapperance.
   */
  almIntegrationHandler.createRandomBitbucketCloudProjectsWithLoadMore(4, 4);
  await user.click(loadMore);

  expect(searchForBitbucketCloudRepositories).toHaveBeenLastCalledWith(
    'conf-bitbucketcloud-2',
    '',
    30,
    2
  );
  expect(loadMore).not.toBeInTheDocument();
});

function renderCreateProject(props: Partial<CreateProjectPageProps> = {}) {
  renderApp('project/create', <CreateProjectPage {...props} />);
}
