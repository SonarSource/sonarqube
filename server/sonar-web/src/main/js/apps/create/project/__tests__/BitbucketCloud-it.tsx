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
import { byLabelText, byRole, byText } from '~sonar-aligned/helpers/testSelector';
import { searchForBitbucketCloudRepositories } from '../../../../api/alm-integrations';
import AlmIntegrationsServiceMock from '../../../../api/mocks/AlmIntegrationsServiceMock';
import DopTranslationServiceMock from '../../../../api/mocks/DopTranslationServiceMock';
import NewCodeDefinitionServiceMock from '../../../../api/mocks/NewCodeDefinitionServiceMock';
import { renderApp } from '../../../../helpers/testReactTestingUtils';
import { Feature } from '../../../../types/features';
import CreateProjectPage from '../CreateProjectPage';
import { REPOSITORY_PAGE_SIZE } from '../constants';
import { CreateProjectModes } from '../types';

jest.mock('../../../../api/alm-integrations');
jest.mock('../../../../api/alm-settings');

let almIntegrationHandler: AlmIntegrationsServiceMock;
let dopTranslationHandler: DopTranslationServiceMock;
let newCodePeriodHandler: NewCodeDefinitionServiceMock;

const ui = {
  bitbucketCloudCreateProjectButton: byText(
    'onboarding.create_project.select_method.bitbucketcloud',
  ),
  bitbucketCloudOnboardingTitle: byRole('heading', {
    name: 'onboarding.create_project.bitbucketcloud.title',
  }),
  cancelButton: byRole('button', { name: 'cancel' }),
  instanceSelector: byLabelText(/alm.configuration.selector.label/),
  monorepoSetupLink: byRole('link', {
    name: 'onboarding.create_project.subtitle_monorepo_setup_link',
  }),
  monorepoTitle: byRole('heading', {
    name: 'onboarding.create_project.monorepo.titlealm.bitbucketcloud',
  }),
  password: byRole('textbox', {
    name: /onboarding\.create_project\.bitbucket_cloud\.enter_password/,
  }),
  personalAccessTokenInput: byRole('textbox', {
    name: /onboarding.create_project.enter_pat/,
  }),
  userName: byRole('textbox', {
    name: /onboarding\.create_project\.bitbucket_cloud\.enter_username/,
  }),
};

const original = window.location;

beforeAll(() => {
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

  expect(screen.getByText('onboarding.create_project.bitbucketcloud.title')).toBeInTheDocument();
  expect(await ui.instanceSelector.find()).toBeInTheDocument();

  await user.click(ui.instanceSelector.get());
  await user.click(byRole('option', { name: /conf-bitbucketcloud-1/ }).get());

  expect(
    await screen.findByText('onboarding.create_project.bitbucket_cloud.enter_password'),
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

  await user.click(ui.userName.get());
  await user.type(ui.userName.get(), 'username');

  expect(ui.userName.get()).toHaveValue('username');

  await user.click(ui.password.get());
  await user.type(ui.password.get(), 'password');

  expect(ui.password.get()).toHaveValue('password');

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

  await user.click(ui.instanceSelector.get());
  await user.click(byRole('option', { name: /conf-bitbucketcloud-2/ }).get());

  expect(await screen.findByText('BitbucketCloud Repo 1')).toBeInTheDocument();
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

  await user.click(ui.instanceSelector.get());
  await user.click(byRole('option', { name: /conf-bitbucketcloud-2/ }).get());

  await waitFor(() =>
    expect(searchForBitbucketCloudRepositories).toHaveBeenLastCalledWith(
      'conf-bitbucketcloud-2',
      '',
      REPOSITORY_PAGE_SIZE,
      1,
    ),
  );

  const inputSearch = screen.getByRole('searchbox', {
    name: 'onboarding.create_project.search_prompt',
  });
  await user.click(inputSearch);
  await user.type(inputSearch, 'search');

  await waitFor(() =>
    expect(searchForBitbucketCloudRepositories).toHaveBeenLastCalledWith(
      'conf-bitbucketcloud-2',
      'search',
      REPOSITORY_PAGE_SIZE,
      1,
    ),
  );
});

it('should show no result message when there are no projects', async () => {
  const user = userEvent.setup();
  almIntegrationHandler.setBitbucketCloudRepositories([]);
  renderCreateProject();

  expect(screen.getByText('onboarding.create_project.bitbucketcloud.title')).toBeInTheDocument();
  expect(await ui.instanceSelector.find()).toBeInTheDocument();

  await user.click(ui.instanceSelector.get());
  await user.click(byRole('option', { name: /conf-bitbucketcloud-2/ }).get());

  expect(
    await screen.findByText('onboarding.create_project.bitbucketcloud.no_projects'),
  ).toBeInTheDocument();
});

it('should have load more', async () => {
  const user = userEvent.setup();
  almIntegrationHandler.createRandomBitbucketCloudProjectsWithLoadMore(
    REPOSITORY_PAGE_SIZE,
    REPOSITORY_PAGE_SIZE + 1,
  );
  renderCreateProject();

  expect(screen.getByText('onboarding.create_project.bitbucketcloud.title')).toBeInTheDocument();
  expect(await ui.instanceSelector.find()).toBeInTheDocument();

  await user.click(ui.instanceSelector.get());
  await user.click(byRole('option', { name: /conf-bitbucketcloud-2/ }).get());

  expect(await screen.findByRole('button', { name: 'show_more' })).toBeInTheDocument();

  /*
   * Next api call response will simulate reaching the last page so we can test the
   * loadmore button disapperance.
   */
  almIntegrationHandler.createRandomBitbucketCloudProjectsWithLoadMore(
    REPOSITORY_PAGE_SIZE + 1,
    REPOSITORY_PAGE_SIZE + 1,
  );
  await user.click(screen.getByRole('button', { name: 'show_more' }));

  await waitFor(() =>
    expect(searchForBitbucketCloudRepositories).toHaveBeenLastCalledWith(
      'conf-bitbucketcloud-2',
      '',
      REPOSITORY_PAGE_SIZE,
      2,
    ),
  );

  await waitFor(() => {
    expect(screen.queryByRole('button', { name: 'show_more' })).not.toBeInTheDocument();
  });
});

describe('Bitbucket Cloud monorepo project navigation', () => {
  it('should be able to access monorepo setup page from Bitbucket Cloud project import page', async () => {
    const user = userEvent.setup();
    renderCreateProject();

    await user.click(await ui.monorepoSetupLink.find());

    expect(ui.monorepoTitle.get()).toBeInTheDocument();
  });

  it('should be able to go back to Bitbucket Cloud onboarding page from monorepo setup page', async () => {
    const user = userEvent.setup();
    renderCreateProject({ isMonorepo: true });

    await user.click(await ui.cancelButton.find());

    expect(ui.bitbucketCloudOnboardingTitle.get()).toBeInTheDocument();
  });
});

function renderCreateProject({
  isMonorepo = false,
}: {
  isMonorepo?: boolean;
} = {}) {
  let queryString = `mode=${CreateProjectModes.BitbucketCloud}`;
  if (isMonorepo) {
    queryString += '&mono=true';
  }

  renderApp('projects/create', <CreateProjectPage />, {
    navigateTo: `projects/create?${queryString}`,
    featureList: [Feature.MonoRepositoryPullRequestDecoration],
  });
}
