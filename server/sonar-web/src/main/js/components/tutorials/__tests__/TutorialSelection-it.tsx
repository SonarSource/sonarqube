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
import { UserEvent } from '@testing-library/user-event/dist/types/setup/setup';
import * as React from 'react';
import { getScannableProjects } from '../../../api/components';
import AlmSettingsServiceMock from '../../../api/mocks/AlmSettingsServiceMock';
import SettingsServiceMock from '../../../api/mocks/SettingsServiceMock';
import UserTokensMock from '../../../api/mocks/UserTokensMock';
import { mockComponent } from '../../../helpers/mocks/component';
import { mockLoggedInUser } from '../../../helpers/testMocks';
import { renderApp } from '../../../helpers/testReactTestingUtils';
import { byLabelText, byRole, byText } from '../../../helpers/testSelector';
import { ComponentPropsType } from '../../../helpers/testUtils';
import { AlmKeys } from '../../../types/alm-settings';
import { Feature } from '../../../types/features';
import { Permissions } from '../../../types/permissions';
import { SettingsKey } from '../../../types/settings';
import TutorialSelection from '../TutorialSelection';
import { TutorialModes } from '../types';

jest.mock('../../../api/branches');

jest.mock('../../../helpers/urls', () => ({
  ...jest.requireActual('../../../helpers/urls'),
  getHostUrl: jest.fn().mockReturnValue('http://host.url'),
}));

jest.mock('../../../api/components', () => ({
  getScannableProjects: jest.fn().mockResolvedValue({ projects: [] }),
}));

let settingsMock: SettingsServiceMock;
let tokenMock: UserTokensMock;
let almMock: AlmSettingsServiceMock;

beforeAll(() => {
  settingsMock = new SettingsServiceMock();
  tokenMock = new UserTokensMock();
  almMock = new AlmSettingsServiceMock();
});

afterEach(() => {
  tokenMock.reset();
  settingsMock.reset();
  almMock.reset();
});

beforeEach(() => {
  jest.clearAllMocks();
});

const ui = {
  loading: byLabelText('loading'),
  noScanRights: byText('onboarding.tutorial.no_scan_rights'),
  chooseTutorialLink: (mode: TutorialModes) =>
    byRole('link', { name: `onboarding.tutorial.choose_method.${mode}` }),
};

it.each([
  [TutorialModes.Jenkins, 'onboarding.tutorial.with.jenkins.title'],
  [TutorialModes.AzurePipelines, 'onboarding.tutorial.with.azure_pipelines.title'],
  [
    TutorialModes.BitbucketPipelines,
    'onboarding.tutorial.with.bitbucket_pipelines.variables.title',
  ],
  [TutorialModes.GitHubActions, 'onboarding.tutorial.with.github_action.create_secret.title'],
  [TutorialModes.GitLabCI, 'onboarding.tutorial.with.gitlab_ci.title'],
  [TutorialModes.Local, 'onboarding.project_analysis.header'],
  [TutorialModes.OtherCI, 'onboarding.project_analysis.header'],
])('should properly click link for %s', async (mode, title) => {
  const user = userEvent.setup();
  const breadcrumbs = `onboarding.tutorial.breadcrumbs.${mode}`;
  renderTutorialSelection({});
  await waitOnDataLoaded();

  expect(screen.getByText('onboarding.tutorial.choose_method')).toBeInTheDocument();

  expect(screen.queryByText(breadcrumbs)).not.toBeInTheDocument();
  await user.click(ui.chooseTutorialLink(mode).get());
  expect(screen.getByText(title)).toBeInTheDocument();
  expect(screen.getByText(breadcrumbs)).toBeInTheDocument();
});

it.each([
  [
    AlmKeys.GitHub,
    [TutorialModes.GitHubActions, TutorialModes.Jenkins, TutorialModes.AzurePipelines],
  ],
  [AlmKeys.GitLab, [TutorialModes.GitLabCI, TutorialModes.Jenkins]],
  [AlmKeys.Azure, [TutorialModes.AzurePipelines]],
  [AlmKeys.BitbucketServer, [TutorialModes.Jenkins]],
  [AlmKeys.BitbucketCloud, [TutorialModes.BitbucketPipelines, TutorialModes.Jenkins]],
])('should show correct buttons if project is bound to %s', async (alm, modes) => {
  almMock.handleSetProjectBinding(alm, {
    project: 'foo',
    almSetting: 'foo',
    repository: 'repo',
    monorepo: false,
  });
  renderTutorialSelection();
  await waitOnDataLoaded();

  modes.forEach((mode) => expect(ui.chooseTutorialLink(mode).get()).toBeInTheDocument());
});

it('should correctly fetch the corresponding ALM setting', async () => {
  almMock.handleSetProjectBinding(AlmKeys.GitHub, {
    project: 'foo',
    almSetting: 'conf-github-1',
    repository: 'repo',
    monorepo: false,
  });
  renderTutorialSelection({}, `tutorials?selectedTutorial=${TutorialModes.Jenkins}&id=foo`);
  await waitOnDataLoaded();

  expect(await screen.findByText('http://url', { exact: false })).toBeInTheDocument();
});

it('should correctly fetch the instance URL', async () => {
  settingsMock.set(SettingsKey.ServerBaseUrl, 'http://sq.example.com');
  const user = userEvent.setup();
  renderTutorialSelection();
  await waitOnDataLoaded();

  await startLocalTutorial(user);
  expect(
    screen.getByText('-Dsonar.host.url=http://sq.example.com', { exact: false }),
  ).toBeInTheDocument();
});

it('should fallback on the host URL', async () => {
  const user = userEvent.setup();
  renderTutorialSelection();
  await waitOnDataLoaded();

  await startLocalTutorial(user);
  expect(
    screen.getByText('-Dsonar.host.url=http://host.url', { exact: false }),
  ).toBeInTheDocument();
});

it('should not display a warning if the user has no global scan permission, but can scan the project', async () => {
  jest
    .mocked(getScannableProjects)
    .mockResolvedValueOnce({ projects: [{ key: 'foo', name: 'foo' }] });
  renderTutorialSelection({ currentUser: mockLoggedInUser() });
  await waitOnDataLoaded();

  expect(ui.noScanRights.query()).not.toBeInTheDocument();
});

it('should correctly display a warning if the user has no scan permissions', async () => {
  renderTutorialSelection({ currentUser: mockLoggedInUser() });
  await waitOnDataLoaded();

  expect(ui.noScanRights.query()).toBeInTheDocument();
});

async function waitOnDataLoaded() {
  await waitFor(() => {
    expect(ui.loading.query()).not.toBeInTheDocument();
  });
}

async function startLocalTutorial(user: UserEvent) {
  await user.click(ui.chooseTutorialLink(TutorialModes.Local).get());
  await user.click(screen.getByRole('button', { name: 'onboarding.token.generate' }));
  await user.click(screen.getByRole('button', { name: 'continue' }));
  await user.click(screen.getByRole('radio', { name: 'onboarding.build.maven' }));
}

function renderTutorialSelection(
  props: Partial<ComponentPropsType<typeof TutorialSelection>> = {},
  navigateTo: string = 'tutorials?id=bar',
) {
  return renderApp(
    '/tutorials',
    <TutorialSelection
      component={mockComponent({ key: 'foo' })}
      currentUser={mockLoggedInUser({ permissions: { global: [Permissions.Scan] } })}
      {...props}
    />,
    { featureList: [Feature.BranchSupport], navigateTo },
  );
}
